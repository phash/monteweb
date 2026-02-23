package com.monteweb.wiki.internal.service;

import com.monteweb.room.RoomModuleApi;
import com.monteweb.shared.exception.BadRequestException;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.wiki.WikiModuleApi;
import com.monteweb.wiki.internal.dto.*;
import com.monteweb.wiki.internal.model.WikiPage;
import com.monteweb.wiki.internal.model.WikiPageVersion;
import com.monteweb.wiki.internal.repository.WikiPageRepository;
import com.monteweb.wiki.internal.repository.WikiPageVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(prefix = "monteweb.modules", name = "wiki.enabled", havingValue = "true")
@RequiredArgsConstructor
public class WikiService implements WikiModuleApi {

    private final WikiPageRepository pageRepo;
    private final WikiPageVersionRepository versionRepo;
    private final UserModuleApi userModule;
    private final RoomModuleApi roomModule;

    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9-]");
    private static final Pattern MULTI_DASH = Pattern.compile("-{2,}");

    // ---- Page Tree ----

    public List<WikiPageSummary> getPageTree(UUID roomId) {
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

    public WikiPageResponse getPage(UUID roomId, String slug) {
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
        pageRepo.save(page);

        // Create new version
        var version = new WikiPageVersion();
        version.setPageId(page.getId());
        version.setTitle(page.getTitle());
        version.setContent(page.getContent());
        version.setEditedBy(userId);
        versionRepo.save(version);

        return getPage(page.getRoomId(), page.getSlug());
    }

    // ---- Delete Page ----

    @Transactional
    public void deletePage(UUID pageId, UUID userId) {
        var page = requirePage(pageId);
        requireRoomMembership(userId, page.getRoomId());

        // Children become root pages (parent_id set to NULL by ON DELETE SET NULL)
        pageRepo.delete(page);
    }

    // ---- Version History ----

    public List<WikiPageVersionResponse> getVersions(UUID pageId) {
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

    public WikiPageVersionResponse getVersion(UUID versionId) {
        var version = versionRepo.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found: " + versionId));

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

    public List<WikiPageSummary> searchPages(UUID roomId, String query) {
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

    // ---- Helpers ----

    private WikiPage requirePage(UUID pageId) {
        return pageRepo.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Wiki page not found: " + pageId));
    }

    private void requireRoomMembership(UUID userId, UUID roomId) {
        if (!roomModule.isUserInRoom(userId, roomId)) {
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
        // Normalize unicode, lowercase, replace spaces with hyphens, remove special chars
        String normalized = Normalizer.normalize(title.trim().toLowerCase(), Normalizer.Form.NFD);
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

        // Ensure uniqueness
        String baseSlug = slug;
        int counter = 1;
        while (pageRepo.existsByRoomIdAndSlug(roomId, slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }
}
