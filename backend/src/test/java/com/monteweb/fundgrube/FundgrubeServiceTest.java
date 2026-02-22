package com.monteweb.fundgrube;

import com.monteweb.fundgrube.internal.dto.ClaimItemRequest;
import com.monteweb.fundgrube.internal.model.FundgrubeItem;
import com.monteweb.fundgrube.internal.repository.FundgrubeImageRepository;
import com.monteweb.fundgrube.internal.repository.FundgrubeItemRepository;
import com.monteweb.fundgrube.internal.service.FundgrubeService;
import com.monteweb.school.SchoolModuleApi;
import com.monteweb.school.SchoolSectionInfo;
import com.monteweb.shared.exception.BadRequestException;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FundgrubeService covering the claim workflow.
 */
@ExtendWith(MockitoExtension.class)
class FundgrubeServiceTest {

    @Mock private FundgrubeItemRepository itemRepo;
    @Mock private FundgrubeImageRepository imageRepo;
    @Mock private UserModuleApi userModule;
    @Mock private SchoolModuleApi schoolModule;

    private FundgrubeService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();
    private static final UUID SECTION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Pass null for storageService — it is not used in claim tests, and the concrete class
        // cannot be mocked on Java 25 due to MinioClient module restrictions.
        service = new FundgrubeService(itemRepo, imageRepo, null, userModule, schoolModule);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private FundgrubeItem makeItem(UUID id, UUID createdBy, UUID sectionId) {
        var item = new FundgrubeItem();
        item.setId(id);
        item.setTitle("Lost keys");
        item.setDescription("Found near entrance");
        item.setSectionId(sectionId);
        item.setCreatedBy(createdBy);
        item.setCreatedAt(Instant.now());
        item.setUpdatedAt(Instant.now());
        return item;
    }

    private UserInfo makeUser(UUID id, UserRole role) {
        return new UserInfo(
                id, id + "@monteweb.local", "Max", "Mustermann",
                "Max Mustermann", null, null,
                role, Set.of(), Set.of(), true
        );
    }

    private SchoolSectionInfo makeSection(UUID id, String name) {
        return new SchoolSectionInfo(id, name, name.toLowerCase(), "Description", 1, true);
    }

    /**
     * Stubs the lookups that toInfo() performs (userModule.findById, schoolModule.findById, imageRepo).
     */
    private void stubToInfo(FundgrubeItem item) {
        lenient().when(userModule.findById(item.getCreatedBy()))
                .thenReturn(Optional.of(makeUser(item.getCreatedBy(), UserRole.PARENT)));
        if (item.getClaimedBy() != null) {
            lenient().when(userModule.findById(item.getClaimedBy()))
                    .thenReturn(Optional.of(makeUser(item.getClaimedBy(), UserRole.PARENT)));
        }
        if (item.getSectionId() != null) {
            lenient().when(schoolModule.findById(item.getSectionId()))
                    .thenReturn(Optional.of(makeSection(item.getSectionId(), "Grundstufe")));
        }
        lenient().when(imageRepo.findByItemIdOrderByCreatedAt(item.getId())).thenReturn(List.of());
    }

    // ── Claim Item ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Claim Item")
    class ClaimItem {

        @Test
        @DisplayName("Successfully claims unclaimed item with +24h expiry")
        void claimItem_success() {
            var item = makeItem(ITEM_ID, OTHER_USER_ID, SECTION_ID);
            when(itemRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(itemRepo.save(any(FundgrubeItem.class))).thenAnswer(inv -> inv.getArgument(0));
            stubToInfo(item);

            Instant beforeClaim = Instant.now();
            var result = service.claimItem(USER_ID, ITEM_ID, new ClaimItemRequest(null));

            assertThat(result).isNotNull();
            assertThat(result.claimed()).isTrue();
            assertThat(result.claimedBy()).isEqualTo(USER_ID);
            assertThat(item.getClaimedBy()).isEqualTo(USER_ID);
            assertThat(item.getClaimedAt()).isNotNull();
            assertThat(item.getClaimedAt()).isAfterOrEqualTo(beforeClaim);
            assertThat(item.getExpiresAt()).isNotNull();
            // Expiry should be approximately 24 hours after claimedAt
            assertThat(item.getExpiresAt())
                    .isCloseTo(item.getClaimedAt().plus(1, ChronoUnit.DAYS),
                            within(1, ChronoUnit.SECONDS));

            verify(itemRepo).save(item);
        }

        @Test
        @DisplayName("Throws BadRequestException when item is already claimed")
        void claimItem_alreadyClaimed() {
            var item = makeItem(ITEM_ID, OTHER_USER_ID, SECTION_ID);
            item.setClaimedBy(UUID.randomUUID());
            item.setClaimedAt(Instant.now());
            when(itemRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));

            assertThatThrownBy(() -> service.claimItem(USER_ID, ITEM_ID, new ClaimItemRequest(null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already been claimed");

            verify(itemRepo, never()).save(any());
        }

        @Test
        @DisplayName("Throws BadRequestException when user tries to claim own item")
        void claimItem_ownItemBlocked() {
            var item = makeItem(ITEM_ID, USER_ID, SECTION_ID);
            when(itemRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));

            assertThatThrownBy(() -> service.claimItem(USER_ID, ITEM_ID, new ClaimItemRequest(null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Cannot claim your own item");

            verify(itemRepo, never()).save(any());
        }
    }
}
