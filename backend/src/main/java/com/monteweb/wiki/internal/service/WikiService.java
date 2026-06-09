package com.monteweb.wiki.internal.service;

import com.monteweb.room.RoomModuleApi;
import com.monteweb.shared.exception.BadRequestException;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import com.monteweb.wiki.WikiModuleApi;
import com.monteweb.wiki.WikiPageDeletedEvent;
import com.monteweb.wiki.WikiPageSavedEvent;
import com.monteweb.wiki.internal.dto.*;
import com.monteweb.wiki.internal.model.WikiPage;
import com.monteweb.wiki.internal.model.WikiPageVersion;
import com.monteweb.wiki.internal.repository.WikiPageRepository;
import com.monteweb.wiki.internal.repository.WikiPageVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "monteweb.modules", name = "wiki.enabled", havingValue = "true")
@RequiredArgsConstructor
public class WikiService implements WikiModuleApi {

    private final WikiPageRepository pageRepo;
    private final WikiPageVersionRepository versionRepo;
    private final UserModuleApi userModule;
    private final RoomModuleApi roomModule;
    private final ApplicationEventPublisher eventPublisher;

    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9-]");
    private static final Pattern MULTI_DASH = Pattern.compile("-{2,}");

    // ---- Page Tree ----

    public List<WikiPageSummary> getPageTree(UUID userId, UUID roomId) {
        requireRoomMembership(userId, roomId);
        var pages = pageRepo.findByRoomIdOrderByTitleAsc(roomId);
        // Build set of IDs that have children
        Set<UUID> parentIds = new HashSet<>();
        for (var page : pages) {
            if (page.getParentId() != null) {
                parentIds.add(page.getParentId());
            }
        }
        return pages.stream()
                .map(p -> new WikiPageSummary(
                        p.getId(),
                        p.getTitle(),
                        p.getSlug(),
                        p.getParentId(),
                        parentIds.contains(p.getId()),
                        p.getUpdatedAt()
                ))
                .toList();
    }

    // ---- Get Page ----

    public WikiPageResponse getPage(UUID userId, UUID roomId, String slug) {
        requireRoomMembership(userId, roomId);
        var page = pageRepo.findByRoomIdAndSlug(roomId, slug)
                .orElseThrow(() -> new ResourceNotFoundException("Wiki page not found: " + slug));

        var children = pageRepo.findByParentIdOrderByTitleAsc(page.getId());
        Set<UUID> grandparentIds = new HashSet<>();
        for (var child : children) {
            // check if child has children
            if (!pageRepo.findByParentIdOrderByTitleAsc(child.getId()).isEmpty()) {
                grandparentIds.add(child.getId());
            }
        }

        var childSummaries = children.stream()
                .map(c -> new WikiPageSummary(
                        c.getId(),
                        c.getTitle(),
                        c.getSlug(),
                        c.getParentId(),
                        grandparentIds.contains(c.getId()),
                        c.getUpdatedAt()
                ))
                .toList();

        // Resolve user names
        Set<UUID> userIds = new HashSet<>();
        userIds.add(page.getCreatedBy());
        if (page.getLastEditedBy() != null) userIds.add(page.getLastEditedBy());
        Map<UUID, String> userNames = resolveUserNames(userIds);

        return new WikiPageResponse(
                page.getId(),
                page.getRoomId(),
                page.getParentId(),
                page.getTitle(),
                page.getSlug(),
                page.getContent(),
                page.getCreatedBy(),
                userNames.getOrDefault(page.getCreatedBy(), "Unbekannt"),
                page.getLastEditedBy(),
                page.getLastEditedBy() != null ? userNames.getOrDefault(page.getLastEditedBy(), "Unbekannt") : null,
                childSummaries,
                page.getCreatedAt(),
                page.getUpdatedAt()
        );
    }

    // ---- Create Page ----

    @Transactional
    public WikiPageResponse createPage(UUID roomId, UUID userId, CreatePageRequest request) {
        requireRoomMembership(userId, roomId);

        // Validate parent if specified
        if (request.parentId() != null) {
            var parent = pageRepo.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent page not found"));
            if (!parent.getRoomId().equals(roomId)) {
                throw new BadRequestException("Parent page does not belong to this room");
            }
        }

        String slug = generateSlug(roomId, request.title());

        var page = new WikiPage();
        page.setRoomId(roomId);
        page.setParentId(request.parentId());
        page.setTitle(request.title().trim());
        page.setSlug(slug);
        page.setContent(request.content() != null ? request.content() : "");
        page.setCreatedBy(userId);
        page.setLastEditedBy(userId);
        pageRepo.save(page);

        // Create initial version
        var version = new WikiPageVersion();
        version.setPageId(page.getId());
        version.setTitle(page.getTitle());
        version.setContent(page.getContent());
        version.setEditedBy(userId);
        versionRepo.save(version);

        String userName = userModule.findById(userId).map(UserInfo::displayName).orElse("Unbekannt");

        eventPublisher.publishEvent(new WikiPageSavedEvent(
                page.getId(), page.getRoomId(), page.getTitle(), page.getContent(), page.getSlug()));

        return new WikiPageResponse(
                page.getId(),
                page.getRoomId(),
                page.getParentId(),
                page.getTitle(),
                page.getSlug(),
                page.getContent(),
                page.getCreatedBy(),
                userName,
                page.getLastEditedBy(),
                userName,
                List.of(),
                page.getCreatedAt(),
                page.getUpdatedAt()
        );
    }

    // ---- Update Page ----

    @Transactional
    public WikiPageResponse updatePage(UUID pageId, UUID userId, UpdatePageRequest request) {
        var page = requirePage(pageId);
        requireRoomMembership(userId, page.getRoomId());

        page.setTitle(request.title().trim());
        page.setContent(request.content());
        page.setLastEditedBy(userId);

        // Slug may optionally be changed. When the client supplies a (non-blank) slug
        // it is sanitized and made unique within the room (ignoring this page's own
        // current slug). When omitted, the existing slug is preserved.
        if (request.slug() != null && !request.slug().isBlank()) {
            String newSlug = uniqueSlug(page.getRoomId(), slugify(request.slug()), page.getId());
            page.setSlug(newSlug);
        }

        pageRepo.save(page);

        // Create new version
        var version = new WikiPageVersion();
        version.setPageId(page.getId());
        version.setTitle(page.getTitle());
        version.setContent(page.getContent());
        version.setEditedBy(userId);
        versionRepo.save(version);

        eventPublisher.publishEvent(new WikiPageSavedEvent(
                page.getId(), page.getRoomId(), page.getTitle(), page.getContent(), page.getSlug()));

        return getPage(userId, page.getRoomId(), page.getSlug());
    }

    // ---- Delete Page ----

    @Transactional
    public void deletePage(UUID pageId, UUID userId) {
        var page = requirePage(pageId);
        requireRoomMembership(userId, page.getRoomId());

        // Children become root pages (parent_id set to NULL by ON DELETE SET NULL)
        UUID deletedPageId = page.getId();
        pageRepo.delete(page);
        eventPublisher.publishEvent(new WikiPageDeletedEvent(deletedPageId));
    }

    // ---- Version History ----

    public List<WikiPageVersionResponse> getVersions(UUID userId, UUID pageId) {
        var page = requirePage(pageId);
        requireRoomMembership(userId, page.getRoomId());
        var versions = versionRepo.findByPageIdOrderByCreatedAtDesc(pageId);

        Set<UUID> userIds = new HashSet<>();
        for (var v : versions) {
            userIds.add(v.getEditedBy());
        }
        Map<UUID, String> userNames = resolveUserNames(userIds);

        return versions.stream()
                .map(v -> new WikiPageVersionResponse(
                        v.getId(),
                        v.getTitle(),
                        v.getContent(),
                        v.getEditedBy(),
                        userNames.getOrDefault(v.getEditedBy(), "Unbekannt"),
                        v.getCreatedAt()
                ))
                .toList();
    }

    public WikiPageVersionResponse getVersion(UUID userId, UUID roomId, UUID versionId) {
        var version = versionRepo.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found: " + versionId));

        // Resolve the owning page to determine the room, and ensure the version
        // actually belongs to the room in the request path (prevents cross-room reads).
        var page = requirePage(version.getPageId());
        if (!page.getRoomId().equals(roomId)) {
            throw new ResourceNotFoundException("Version not found: " + versionId);
        }
        requireRoomMembership(userId, page.getRoomId());

        String userName = userModule.findById(version.getEditedBy())
                .map(UserInfo::displayName).orElse("Unbekannt");

        return new WikiPageVersionResponse(
                version.getId(),
                version.getTitle(),
                version.getContent(),
                version.getEditedBy(),
                userName,
                version.getCreatedAt()
        );
    }

    // ---- Search ----

    public List<WikiPageSummary> searchPages(UUID userId, UUID roomId, String query) {
        requireRoomMembership(userId, roomId);
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        var pages = pageRepo.searchByRoomIdAndQuery(roomId, query.trim());

        // Build parent ID set for hasChildren
        var allPages = pageRepo.findByRoomIdOrderByTitleAsc(roomId);
        Set<UUID> parentIds = new HashSet<>();
        for (var p : allPages) {
            if (p.getParentId() != null) {
                parentIds.add(p.getParentId());
            }
        }

        return pages.stream()
                .map(p -> new WikiPageSummary(
                        p.getId(),
                        p.getTitle(),
                        p.getSlug(),
                        p.getParentId(),
                        parentIds.contains(p.getId()),
                        p.getUpdatedAt()
                ))
                .toList();
    }

    // ---- Re-indexing ----

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findAllPagesForIndexing() {
        return pageRepo.findAll().stream()
                .map(p -> {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("id", p.getId());
                    data.put("roomId", p.getRoomId());
                    data.put("title", p.getTitle());
                    data.put("content", p.getContent());
                    data.put("slug", p.getSlug());
                    return data;
                })
                .toList();
    }

    // ---- DSGVO ----

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> exportUserData(UUID userId) {
        Map<String, Object> data = new LinkedHashMap<>();

        var createdPages = pageRepo.findByCreatedBy(userId);
        data.put("createdPages", createdPages.stream().map(p -> Map.of(
                "id", p.getId(),
                "title", p.getTitle(),
                "slug", p.getSlug(),
                "createdAt", p.getCreatedAt()
        )).toList());

        var editedPages = pageRepo.findByLastEditedBy(userId);
        data.put("editedPages", editedPages.stream().map(p -> Map.of(
                "id", p.getId(),
                "title", p.getTitle()
        )).toList());

        var editedVersions = versionRepo.findByEditedBy(userId);
        data.put("editedVersions", editedVersions.stream().map(v -> Map.of(
                "id", v.getId(),
                "title", v.getTitle(),
                "createdAt", v.getCreatedAt()
        )).toList());

        return data;
    }

    @Transactional
    public void cleanupUserData(UUID userId) {
        pageRepo.anonymizeCreator(userId);
        pageRepo.anonymizeLastEditor(userId);
        versionRepo.anonymizeEditor(userId);
        log.info("Anonymized wiki data for deleted user {}", userId);
    }

    // ---- Helpers ----

    private WikiPage requirePage(UUID pageId) {
        return pageRepo.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Wiki page not found: " + pageId));
    }

    /**
     * Room-scoped admin check: SUPERADMIN is a global admin for every room,
     * SECTION_ADMIN only counts as admin for rooms whose section the user actually
     * administers (special role "SECTION_ADMIN:&lt;sectionId&gt;").
     */
    private boolean hasAdminRoleForRoom(UUID userId, UUID roomId) {
        var user = userModule.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        if (user.role() == UserRole.SUPERADMIN) {
            return true;
        }
        if (user.role() == UserRole.SECTION_ADMIN) {
            var sectionId = roomModule.findById(roomId).map(r -> r.sectionId()).orElse(null);
            return sectionId != null && user.specialRoles() != null
                    && user.specialRoles().contains("SECTION_ADMIN:" + sectionId);
        }
        return false;
    }

    private void requireRoomMembership(UUID userId, UUID roomId) {
        if (!hasAdminRoleForRoom(userId, roomId) && !roomModule.isUserInRoom(userId, roomId)) {
            throw new ForbiddenException("Not a member of this room");
        }
    }

    private Map<UUID, String> resolveUserNames(Set<UUID> userIds) {
        if (userIds.isEmpty()) return Map.of();
        var users = userModule.findByIds(new ArrayList<>(userIds));
        Map<UUID, String> names = new HashMap<>();
        for (var user : users) {
            names.put(user.id(), user.displayName());
        }
        return names;
    }

    /**
     * Generate a URL-safe slug from the title. Ensures uniqueness within the room.
     */
    String generateSlug(UUID roomId, String title) {
        return uniqueSlug(roomId, slugify(title), null);
    }

    /**
     * Normalize an arbitrary input into a URL-safe slug (lowercase, hyphenated,
     * special chars stripped). Never returns an empty string ("page" fallback).
     */
    private String slugify(String input) {
        // Normalize unicode, lowercase, replace spaces with hyphens, remove special chars
        String normalized = Normalizer.normalize(input.trim().toLowerCase(), Normalizer.Form.NFD);
        // Replace umlauts
        normalized = normalized
                .replaceAll("\\p{M}", "")
                .replace("ae", "ae")
                .replace("oe", "oe")
                .replace("ue", "ue")
                .replace("ss", "ss");
        String slug = normalized
                .replaceAll("\\s+", "-");
        slug = NON_SLUG.matcher(slug).replaceAll("");
        slug = MULTI_DASH.matcher(slug).replaceAll("-");
        slug = slug.replaceAll("^-|-$", "");

        if (slug.isEmpty()) {
            slug = "page";
        }
        return slug;
    }

    /**
     * Ensure the slug is unique within the room, appending -1, -2, ... on collision.
     * When {@code excludePageId} is non-null, a collision with that page is ignored
     * (used when updating a page's own slug).
     */
    private String uniqueSlug(UUID roomId, String baseSlug, UUID excludePageId) {
        String slug = baseSlug;
        int counter = 1;
        while (slugTaken(roomId, slug, excludePageId)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        return slug;
    }

    private boolean slugTaken(UUID roomId, String slug, UUID excludePageId) {
        if (excludePageId == null) {
            return pageRepo.existsByRoomIdAndSlug(roomId, slug);
        }
        return pageRepo.existsByRoomIdAndSlugAndIdNot(roomId, slug, excludePageId);
    }
}
