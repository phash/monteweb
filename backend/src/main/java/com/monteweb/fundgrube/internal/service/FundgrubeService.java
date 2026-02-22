package com.monteweb.fundgrube.internal.service;

import com.monteweb.fundgrube.FundgrubeImageInfo;
import com.monteweb.fundgrube.FundgrubeItemInfo;
import com.monteweb.fundgrube.FundgrubeModuleApi;
import com.monteweb.fundgrube.internal.dto.ClaimItemRequest;
import com.monteweb.fundgrube.internal.dto.CreateItemRequest;
import com.monteweb.fundgrube.internal.dto.UpdateItemRequest;
import com.monteweb.fundgrube.internal.model.FundgrubeImage;
import com.monteweb.fundgrube.internal.model.FundgrubeItem;
import com.monteweb.fundgrube.internal.repository.FundgrubeImageRepository;
import com.monteweb.fundgrube.internal.repository.FundgrubeItemRepository;
import com.monteweb.school.SchoolModuleApi;
import com.monteweb.shared.exception.BadRequestException;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@ConditionalOnProperty(prefix = "monteweb.modules", name = "fundgrube.enabled", havingValue = "true")
@RequiredArgsConstructor
public class FundgrubeService implements FundgrubeModuleApi {

    private final FundgrubeItemRepository itemRepo;
    private final FundgrubeImageRepository imageRepo;
    private final FundgrubeStorageService storageService;
    private final UserModuleApi userModule;
    private final SchoolModuleApi schoolModule;

    // ---- List ----

    public List<FundgrubeItemInfo> listItems(UUID sectionId) {
        var now = Instant.now();
        List<FundgrubeItem> items = sectionId != null
                ? itemRepo.findActiveBySectionId(sectionId, now)
                : itemRepo.findAllActive(now);
        return items.stream().map(this::toInfo).toList();
    }

    // ---- Get ----

    public FundgrubeItemInfo getItem(UUID itemId) {
        return toInfo(requireItem(itemId));
    }

    // ---- Create ----

    @Transactional
    public FundgrubeItemInfo createItem(UUID userId, CreateItemRequest request) {
        if (request.sectionId() != null) {
            schoolModule.findById(request.sectionId())
                    .orElseThrow(() -> new BadRequestException("Section not found: " + request.sectionId()));
        }
        var item = new FundgrubeItem();
        item.setTitle(request.title());
        item.setDescription(request.description());
        item.setSectionId(request.sectionId());
        item.setCreatedBy(userId);
        itemRepo.save(item);
        return toInfo(item);
    }

    // ---- Update ----

    @Transactional
    public FundgrubeItemInfo updateItem(UUID userId, UUID itemId, UpdateItemRequest request) {
        var item = requireItem(itemId);
        requireEditPermission(userId, item);
        if (request.title() != null && !request.title().isBlank()) item.setTitle(request.title());
        if (request.description() != null) item.setDescription(request.description());
        if (request.sectionId() != null) {
            schoolModule.findById(request.sectionId())
                    .orElseThrow(() -> new BadRequestException("Section not found: " + request.sectionId()));
            item.setSectionId(request.sectionId());
        }
        itemRepo.save(item);
        return toInfo(item);
    }

    // ---- Delete ----

    @Transactional
    public void deleteItem(UUID userId, UUID itemId) {
        var item = requireItem(itemId);
        requireEditPermission(userId, item);
        // Delete images from storage
        imageRepo.findByItemIdOrderByCreatedAt(itemId).forEach(img -> {
            storageService.delete(img.getStoragePath());
            storageService.delete(img.getThumbnailPath());
        });
        itemRepo.delete(item);
    }

    // ---- Claim ----

    @Transactional
    public FundgrubeItemInfo claimItem(UUID userId, UUID itemId, ClaimItemRequest request) {
        var item = requireItem(itemId);
        if (item.isClaimed()) {
            throw new BadRequestException("Item has already been claimed");
        }
        if (item.getCreatedBy().equals(userId)) {
            throw new BadRequestException("Cannot claim your own item");
        }
        var now = Instant.now();
        item.setClaimedBy(userId);
        item.setClaimedAt(now);
        item.setExpiresAt(now.plus(1, ChronoUnit.DAYS));
        itemRepo.save(item);
        return toInfo(item);
    }

    // ---- Images ----

    @Transactional
    public List<FundgrubeImageInfo> uploadImages(UUID userId, UUID itemId, List<MultipartFile> files) {
        var item = requireItem(itemId);
        requireEditPermission(userId, item);
        if (files.size() > 10) throw new BadRequestException("Maximum 10 images per item");

        List<FundgrubeImageInfo> result = new ArrayList<>();
        for (MultipartFile file : files) {
            String contentType = storageService.validateAndDetectContentType(file);
            String extension = FundgrubeStorageService.extensionFromContentType(contentType);
            UUID imageId = UUID.randomUUID();

            String storagePath = storageService.uploadOriginal(itemId, imageId, extension, file, contentType);
            String thumbnailPath = storageService.uploadThumbnail(itemId, imageId, file);

            var image = new FundgrubeImage();
            image.setId(imageId);
            image.setItemId(itemId);
            image.setStoragePath(storagePath);
            image.setThumbnailPath(thumbnailPath);
            image.setOriginalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image");
            image.setContentType(contentType);
            image.setFileSize(file.getSize());
            imageRepo.save(image);

            result.add(toImageInfo(image));
        }
        return result;
    }

    @Transactional
    public void deleteImage(UUID userId, UUID imageId) {
        var image = imageRepo.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
        var item = requireItem(image.getItemId());
        requireEditPermission(userId, item);
        storageService.delete(image.getStoragePath());
        storageService.delete(image.getThumbnailPath());
        imageRepo.delete(image);
    }

    public FundgrubeImage requireImageForDownload(UUID imageId) {
        return imageRepo.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
    }

    // ---- Helpers ----

    private FundgrubeItem requireItem(UUID itemId) {
        return itemRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Fundgrube item not found: " + itemId));
    }

    /**
     * Edit/delete is allowed for: creator, SECTION_ADMIN of the item's section, SUPERADMIN.
     */
    private void requireEditPermission(UUID userId, FundgrubeItem item) {
        if (item.getCreatedBy().equals(userId)) return;
        UserInfo user = userModule.findById(userId)
                .orElseThrow(() -> new ForbiddenException("User not found"));
        if (user.role() == UserRole.SUPERADMIN) return;
        if (user.role() == UserRole.SECTION_ADMIN && item.getSectionId() != null) {
            boolean adminOfSection = user.specialRoles().stream()
                    .filter(r -> r.startsWith("SECTION_ADMIN:"))
                    .map(r -> UUID.fromString(r.substring("SECTION_ADMIN:".length())))
                    .anyMatch(sId -> sId.equals(item.getSectionId()));
            if (adminOfSection) return;
        }
        throw new ForbiddenException("No permission to edit this item");
    }

    private FundgrubeItemInfo toInfo(FundgrubeItem item) {
        String createdByName = userModule.findById(item.getCreatedBy())
                .map(UserInfo::displayName).orElse("Unbekannt");
        String claimedByName = item.getClaimedBy() != null
                ? userModule.findById(item.getClaimedBy()).map(UserInfo::displayName).orElse(null)
                : null;
        String sectionName = item.getSectionId() != null
                ? schoolModule.findById(item.getSectionId()).map(s -> s.name()).orElse(null)
                : null;
        List<FundgrubeImageInfo> images = imageRepo.findByItemIdOrderByCreatedAt(item.getId())
                .stream().map(this::toImageInfo).toList();
        return new FundgrubeItemInfo(
                item.getId(), item.getTitle(), item.getDescription(),
                item.getSectionId(), sectionName,
                item.getCreatedBy(), createdByName,
                item.getCreatedAt(), item.getUpdatedAt(),
                item.getClaimedBy(), claimedByName, item.getClaimedAt(), item.getExpiresAt(),
                item.isClaimed(),
                images
        );
    }

    private FundgrubeImageInfo toImageInfo(FundgrubeImage image) {
        return new FundgrubeImageInfo(
                image.getId(), image.getItemId(),
                image.getOriginalFilename(),
                "/api/v1/fundgrube/images/" + image.getId(),
                "/api/v1/fundgrube/images/" + image.getId() + "/thumbnail",
                image.getFileSize(), image.getContentType()
        );
    }

    /**
     * DSGVO: Clean up all fundgrube data for a deleted user.
     */
    @Transactional
    public void cleanupUserData(UUID userId) {
        var items = itemRepo.findByCreatedBy(userId);
        var itemIds = items.stream().map(FundgrubeItem::getId).toList();
        if (!itemIds.isEmpty()) {
            var images = imageRepo.findByItemIdIn(itemIds);
            for (var img : images) {
                storageService.delete(img.getStoragePath());
                storageService.delete(img.getThumbnailPath());
            }
            imageRepo.deleteAll(images);
        }
        itemRepo.deleteAll(items);
    }

    /**
     * DSGVO: Export all fundgrube data for a user.
     */
    @Override
    public Map<String, Object> exportUserData(UUID userId) {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        var items = itemRepo.findByCreatedBy(userId);
        data.put("items", items.stream().map(i -> Map.of(
                "id", i.getId(),
                "title", i.getTitle(),
                "createdAt", i.getCreatedAt()
        )).toList());
        return data;
    }
}
