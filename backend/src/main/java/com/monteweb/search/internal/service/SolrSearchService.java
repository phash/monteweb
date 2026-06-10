package com.monteweb.search.internal.service;

import com.monteweb.room.RoomInfo;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.search.SearchResult;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@ConditionalOnProperty(prefix = "monteweb.modules.solr", name = "enabled", havingValue = "true")
public class SolrSearchService {

    private static final Logger log = LoggerFactory.getLogger(SolrSearchService.class);

    private static final Set<String> VALID_DOC_TYPES = Set.of(
        "USER", "ROOM", "POST", "EVENT", "FILE", "WIKI", "TASK"
    );

    private final SolrClient solrClient;
    private final RoomModuleApi roomModuleApi;
    private final UserModuleApi userModuleApi;

    public SolrSearchService(SolrClient solrClient, RoomModuleApi roomModuleApi,
                             UserModuleApi userModuleApi) {
        this.solrClient = solrClient;
        this.roomModuleApi = roomModuleApi;
        this.userModuleApi = userModuleApi;
    }

    public List<SearchResult> search(String query, String type, int limit, UUID userId) {
        try {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(escapeQuery(query));
            solrQuery.setRows(limit);

            // Filter by doc_type if not ALL
            if (type != null && !type.equalsIgnoreCase("ALL")) {
                String upperType = type.toUpperCase();
                if (!VALID_DOC_TYPES.contains(upperType)) {
                    return List.of();
                }
                solrQuery.addFilterQuery("doc_type:" + upperType);
            }

            // Access control: each doc_type is constrained by its own visibility rules.
            // USER/ROOM are public; POST docs are constrained by room membership and
            // parent_only; EVENT docs by their scope (ROOM/SECTION/SCHOOL); FILE docs by
            // room membership AND audience; WIKI/TASK by room membership. See buildAccessFilter.
            solrQuery.addFilterQuery(buildAccessFilter(userId));

            // Highlighting
            solrQuery.setHighlight(true);
            solrQuery.setHighlightFragsize(150);
            solrQuery.setHighlightSnippets(2);
            solrQuery.addHighlightField("title");
            solrQuery.addHighlightField("content");
            solrQuery.addHighlightField("file_content");

            // Sort by score then created_at desc
            solrQuery.addSort("score", SolrQuery.ORDER.desc);
            solrQuery.addSort("created_at", SolrQuery.ORDER.desc);

            // Use POST: the room-access filter can list many room_ids, and a GET URI
            // would exceed Solr/Jetty's max header size (HTTP 414) for users in many rooms.
            QueryResponse response = solrClient.query(solrQuery, SolrRequest.METHOD.POST);
            Map<String, Map<String, List<String>>> highlighting = response.getHighlighting();

            List<SearchResult> results = new ArrayList<>();
            for (SolrDocument doc : response.getResults()) {
                String solrId = (String) doc.getFieldValue("id");
                String docType = (String) doc.getFieldValue("doc_type");
                String entityId = (String) doc.getFieldValue("entity_id");
                String title = getStringField(doc, "title");
                String url = getStringField(doc, "url");
                String roomName = getStringField(doc, "room_name");
                String contentType = getStringField(doc, "content_type");

                // Build subtitle
                String subtitle = buildSubtitle(docType, roomName, contentType, doc);

                // Build snippet from highlights
                String snippet = buildSnippet(highlighting, solrId);
                if (snippet == null) {
                    snippet = truncate(getStringField(doc, "content"), 150);
                }

                // Timestamp
                Date createdAt = (Date) doc.getFieldValue("created_at");
                Date updatedAt = (Date) doc.getFieldValue("updated_at");
                Instant timestamp = updatedAt != null ? updatedAt.toInstant()
                        : createdAt != null ? createdAt.toInstant() : null;

                results.add(new SearchResult(
                        entityId != null ? UUID.fromString(entityId) : null,
                        docType,
                        title,
                        subtitle,
                        snippet,
                        url,
                        timestamp
                ));
            }
            return results;
        } catch (Exception e) {
            log.error("Solr search failed for query '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    /**
     * Builds a per-doc-type Solr access filter. The previous implementation let every document
     * without a {@code room_id} pass unconditionally, which leaked ROOM-scoped / parentOnly feed
     * posts, ROOM/SECTION calendar events, and restricted files. Each doc_type now carries its
     * own constraint:
     *
     * <ul>
     *   <li><b>USER, ROOM</b> — public, always visible.</li>
     *   <li><b>POST</b> — ROOM-scoped posts (room_id set) require room membership; parentOnly posts
     *       are hidden from students. (Per-user targeted posts are excluded at index time —
     *       they carry no target_user_ids field; see FLAG.)</li>
     *   <li><b>EVENT</b> — SCHOOL events are visible to all; ROOM events require room membership;
     *       SECTION events require the user to belong to that section.</li>
     *   <li><b>FILE</b> — requires room membership AND an audience the user may see
     *       (mirrors {@code FileService.getAllowedAudiences}).</li>
     *   <li><b>WIKI, TASK</b> — require room membership.</li>
     * </ul>
     *
     * <p>Fails closed: an anonymous user or a lookup failure restricts results to public
     * (USER/ROOM) and SCHOOL-scoped EVENT documents only.</p>
     */
    private String buildAccessFilter(UUID userId) {
        List<UUID> accessibleRoomIds = List.of();
        Set<UUID> accessibleSectionIds = Set.of();
        Set<String> allowedAudiences = Set.of("ALL");

        if (userId != null) {
            try {
                List<RoomInfo> rooms = roomModuleApi.findByUserId(userId);
                accessibleRoomIds = rooms.stream().map(RoomInfo::id).toList();
                accessibleSectionIds = rooms.stream()
                        .map(RoomInfo::sectionId)
                        .filter(Objects::nonNull)
                        .collect(java.util.stream.Collectors.toSet());
            } catch (Exception e) {
                log.error("Failed to resolve accessible rooms for user {}: {}", userId, e.getMessage());
                // Fall through with empty room/section sets (fail closed).
            }
            allowedAudiences = allowedAudiencesForUser(userId);
        }

        boolean isStudent = isStudent(userId);
        String roomIn = roomIdInClause(accessibleRoomIds);   // null when no accessible rooms
        String sectionIn = sectionIdInClause(accessibleSectionIds);

        // POST: ROOM-scoped posts (room_id set) need membership; non-room posts pass. parentOnly
        // posts are hidden from students regardless of source.
        StringBuilder postClause = new StringBuilder("doc_type:POST");
        // Use the (*:* -room_id:...) form so the negative sub-clause is not a pure-negative query
        // (a parenthesized pure-negative matches nothing in Lucene).
        String roomScopeForPost = roomIn != null
                ? "((*:* -room_id:[* TO *]) OR room_id:(" + roomIn + "))"
                : "(*:* -room_id:[* TO *])";
        postClause.append(" AND ").append(roomScopeForPost);
        if (isStudent) {
            postClause.append(" AND -parent_only:true");
        }

        // EVENT: SCHOOL always; ROOM needs membership; SECTION needs section membership.
        StringBuilder eventClause = new StringBuilder("doc_type:EVENT AND (scope:SCHOOL");
        if (roomIn != null) {
            eventClause.append(" OR (scope:ROOM AND room_id:(").append(roomIn).append("))");
        }
        if (sectionIn != null) {
            eventClause.append(" OR (scope:SECTION AND section_id:(").append(sectionIn).append("))");
        }
        eventClause.append(")");

        // FILE: room membership AND an audience the user may see.
        String fileClause;
        if (roomIn != null) {
            String audienceIn = allowedAudiences.stream()
                    .reduce((a, b) -> a + " OR " + b)
                    .orElse("ALL");
            fileClause = "doc_type:FILE AND room_id:(" + roomIn + ") AND audience:(" + audienceIn + ")";
        } else {
            fileClause = "doc_type:FILE AND id:__none__"; // no accessible rooms -> no files
        }

        // WIKI / TASK: room membership.
        String roomScopedClause = roomIn != null
                ? "(doc_type:WIKI OR doc_type:TASK) AND room_id:(" + roomIn + ")"
                : "(doc_type:WIKI OR doc_type:TASK) AND id:__none__";

        return "("
                + "doc_type:USER OR doc_type:ROOM"
                + " OR (" + postClause + ")"
                + " OR (" + eventClause + ")"
                + " OR (" + fileClause + ")"
                + " OR (" + roomScopedClause + ")"
                + ")";
    }

    /** Returns a quoted {@code OR}-joined list of room IDs, or {@code null} if the set is empty. */
    private String roomIdInClause(List<UUID> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) return null;
        return roomIds.stream()
                .map(id -> "\"" + id + "\"")
                .reduce((a, b) -> a + " OR " + b)
                .orElse(null);
    }

    private String sectionIdInClause(Set<UUID> sectionIds) {
        if (sectionIds == null || sectionIds.isEmpty()) return null;
        return sectionIds.stream()
                .map(id -> "\"" + id + "\"")
                .reduce((a, b) -> a + " OR " + b)
                .orElse(null);
    }

    private boolean isStudent(UUID userId) {
        if (userId == null) return false;
        try {
            return userModuleApi.findById(userId)
                    .map(UserInfo::role)
                    .map(r -> r == UserRole.STUDENT)
                    .orElse(false);
        } catch (Exception e) {
            // Fail closed: treat as student (most restrictive) so parentOnly posts stay hidden.
            return true;
        }
    }

    /**
     * Resolves the set of file audiences a user may see, mirroring
     * {@code FileService.getAllowedAudiences} but using only the global role (per-room LEADER /
     * PARENT_MEMBER room roles are not resolved here — see FLAG). TEACHER / SUPERADMIN /
     * SECTION_ADMIN see everything; PARENT sees ALL + PARENTS_ONLY; STUDENT sees ALL +
     * STUDENTS_ONLY; everyone else sees ALL only.
     */
    private Set<String> allowedAudiencesForUser(UUID userId) {
        UserRole role;
        try {
            role = userModuleApi.findById(userId).map(UserInfo::role).orElse(null);
        } catch (Exception e) {
            return Set.of("ALL");
        }
        if (role == UserRole.TEACHER || role == UserRole.SUPERADMIN || role == UserRole.SECTION_ADMIN) {
            return Set.of("ALL", "PARENTS_ONLY", "STUDENTS_ONLY");
        }
        if (role == UserRole.PARENT) {
            return Set.of("ALL", "PARENTS_ONLY");
        }
        if (role == UserRole.STUDENT) {
            return Set.of("ALL", "STUDENTS_ONLY");
        }
        return Set.of("ALL");
    }

    private String buildSubtitle(String docType, String roomName, String contentType, SolrDocument doc) {
        return switch (docType) {
            case "FILE" -> {
                String name = roomName != null ? roomName : "";
                String ct = contentType != null ? " (" + friendlyContentType(contentType) + ")" : "";
                Long size = (Long) doc.getFieldValue("file_size");
                String sizeStr = size != null ? " - " + formatFileSize(size) : "";
                yield name + ct + sizeStr;
            }
            case "WIKI" -> roomName != null ? "Wiki - " + roomName : "Wiki";
            case "TASK" -> roomName != null ? "Aufgabe - " + roomName : "Aufgabe";
            case "POST" -> {
                String authorName = getStringField(doc, "author_name");
                yield authorName != null ? authorName : "";
            }
            case "USER" -> getStringField(doc, "content"); // email
            case "ROOM" -> getStringField(doc, "content"); // description
            case "EVENT" -> getStringField(doc, "content"); // description
            default -> null;
        };
    }

    private String buildSnippet(Map<String, Map<String, List<String>>> highlighting, String solrId) {
        if (highlighting == null || !highlighting.containsKey(solrId)) return null;
        var docHighlights = highlighting.get(solrId);
        // Prefer file_content, then content, then title
        for (String field : List.of("file_content", "content", "title")) {
            if (docHighlights.containsKey(field) && !docHighlights.get(field).isEmpty()) {
                return String.join(" ... ", docHighlights.get(field));
            }
        }
        return null;
    }

    private String escapeQuery(String query) {
        // Escape Solr special characters but allow basic queries
        return query.replace("\\", "\\\\")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("!", "\\!")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("^", "\\^")
                .replace("~", "\\~")
                .replace(":", "\\:")
                .replace("\"", "\\\"")
                .replace("*", "\\*")
                .replace("?", "\\?")
                .replace("/", "\\/");
    }

    private String getStringField(SolrDocument doc, String field) {
        Object val = doc.getFieldValue(field);
        return val != null ? val.toString() : null;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    private String friendlyContentType(String contentType) {
        if (contentType == null) return "Datei";
        if (contentType.contains("pdf")) return "PDF";
        if (contentType.contains("word") || contentType.contains("docx")) return "Word";
        if (contentType.contains("excel") || contentType.contains("spreadsheet") || contentType.contains("xlsx")) return "Excel";
        if (contentType.contains("powerpoint") || contentType.contains("presentation")) return "PowerPoint";
        if (contentType.contains("image")) return "Bild";
        if (contentType.contains("video")) return "Video";
        if (contentType.contains("audio")) return "Audio";
        if (contentType.contains("text/plain")) return "Text";
        if (contentType.contains("text/csv")) return "CSV";
        if (contentType.contains("zip") || contentType.contains("archive")) return "Archiv";
        return "Datei";
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1048576) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1073741824) return String.format("%.1f MB", bytes / 1048576.0);
        return String.format("%.1f GB", bytes / 1073741824.0);
    }
}
