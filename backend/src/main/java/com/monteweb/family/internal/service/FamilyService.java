package com.monteweb.family.internal.service;

import com.monteweb.family.FamilyInfo;
import com.monteweb.family.FamilyInvitationEvent;
import com.monteweb.family.FamilyModuleApi;
import com.monteweb.family.internal.dto.FamilyInvitationInfo;
import com.monteweb.family.internal.model.*;
import com.monteweb.family.internal.repository.FamilyInvitationRepository;
import com.monteweb.family.internal.repository.FamilyRepository;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FamilyService implements FamilyModuleApi {

    private final FamilyRepository familyRepository;
    private final FamilyInvitationRepository invitationRepository;
    private final InviteCodeService inviteCodeService;
    private final UserModuleApi userModuleApi;
    private final ApplicationEventPublisher eventPublisher;

    public FamilyService(FamilyRepository familyRepository,
                         FamilyInvitationRepository invitationRepository,
                         InviteCodeService inviteCodeService,
                         UserModuleApi userModuleApi,
                         ApplicationEventPublisher eventPublisher) {
        this.familyRepository = familyRepository;
        this.invitationRepository = invitationRepository;
        this.inviteCodeService = inviteCodeService;
        this.userModuleApi = userModuleApi;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Optional<FamilyInfo> findById(UUID familyId) {
        return familyRepository.findById(familyId).map(this::toFamilyInfo);
    }

    @Override
    public List<FamilyInfo> findAll() {
        return familyRepository.findAll().stream()
                .map(this::toFamilyInfo)
                .toList();
    }

    @Override
    public List<FamilyInfo> findByUserId(UUID userId) {
        return familyRepository.findByMemberUserId(userId).stream()
                .map(this::toFamilyInfo)
                .toList();
    }

    @Override
    public boolean isUserInFamily(UUID userId, UUID familyId) {
        return familyRepository.isMember(userId, familyId);
    }

    @Transactional
    public FamilyInfo create(String name, UUID creatorUserId) {
        // Only PARENT and SUPERADMIN can create/manage families
        var user = userModuleApi.findById(creatorUserId);
        if (user.isPresent()) {
            var role = user.get().role();
            if (role != com.monteweb.user.UserRole.PARENT && role != com.monteweb.user.UserRole.SUPERADMIN) {
                throw new BusinessException("Only parents and administrators can create families");
            }
        }
        // Check if parent already has a family
        List<Family> existingFamilies = familyRepository.findByMemberUserId(creatorUserId);
        if (!existingFamilies.isEmpty()) {
            throw new BusinessException("User already belongs to a family");
        }

        var family = new Family();
        family.setName(name);
        family = familyRepository.save(family);

        var member = new FamilyMember(family, creatorUserId, FamilyMemberRole.PARENT);
        family.getMembers().add(member);
        family = familyRepository.save(family);

        return toFamilyInfo(family);
    }

    @Transactional
    public String generateInviteCode(UUID familyId, UUID requestingUserId) {
        var family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family", familyId));

        if (!familyRepository.isMember(requestingUserId, familyId)) {
            throw new BusinessException("Not a member of this family");
        }

        String code = inviteCodeService.generateCode();
        family.setInviteCode(code);
        family.setInviteExpires(Instant.now().plus(7, ChronoUnit.DAYS));
        familyRepository.save(family);

        return code;
    }

    @Transactional
    public FamilyInfo joinByInviteCode(String inviteCode, UUID userId) {
        assertCanJoinFamily(userId);

        var family = familyRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException("Invalid invite code"));

        if (family.getInviteExpires() != null && family.getInviteExpires().isBefore(Instant.now())) {
            throw new BusinessException("Invite code has expired");
        }

        if (familyRepository.isMember(userId, family.getId())) {
            throw new BusinessException("Already a member of this family");
        }

        var member = new FamilyMember(family, userId, FamilyMemberRole.PARENT);
        family.getMembers().add(member);
        familyRepository.save(family);

        return toFamilyInfo(family);
    }

    @Transactional
    public FamilyInfo addChild(UUID familyId, UUID childUserId, UUID requestingUserId) {
        var family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family", familyId));

        if (!familyRepository.isMember(requestingUserId, familyId)) {
            throw new BusinessException("Not a member of this family");
        }

        if (familyRepository.isMember(childUserId, familyId)) {
            throw new BusinessException("User is already a member of this family");
        }

        var member = new FamilyMember(family, childUserId, FamilyMemberRole.CHILD);
        family.getMembers().add(member);
        familyRepository.save(family);

        return toFamilyInfo(family);
    }

    @Transactional
    public void removeMember(UUID familyId, UUID memberUserId, UUID requestingUserId) {
        var family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family", familyId));

        if (!familyRepository.isMember(requestingUserId, familyId)) {
            throw new BusinessException("Not a member of this family");
        }

        family.getMembers().removeIf(m -> m.getUserId().equals(memberUserId));
        familyRepository.save(family);
    }

    @Override
    @Transactional
    public void adminAddMember(UUID familyId, UUID userId, String role) {
        var family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family", familyId));

        if (familyRepository.isMember(userId, familyId)) {
            throw new BusinessException("User is already a member of this family");
        }

        var memberRole = FamilyMemberRole.valueOf(role);
        var member = new FamilyMember(family, userId, memberRole);
        family.getMembers().add(member);
        familyRepository.save(family);
    }

    @Override
    @Transactional
    public void adminRemoveMember(UUID familyId, UUID userId) {
        var family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family", familyId));

        family.getMembers().removeIf(m -> m.getUserId().equals(userId));
        familyRepository.save(family);
    }

    @Transactional
    public void leaveFamily(UUID familyId, UUID userId) {
        var family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family", familyId));

        if (!familyRepository.isMember(userId, familyId)) {
            throw new BusinessException("Not a member of this family");
        }

        // Check if this is the last PARENT and there are children
        var member = family.getMembers().stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Member not found"));

        if (member.getRole() == FamilyMemberRole.PARENT) {
            long parentCount = family.getMembers().stream()
                    .filter(m -> m.getRole() == FamilyMemberRole.PARENT)
                    .count();
            boolean hasChildren = family.getMembers().stream()
                    .anyMatch(m -> m.getRole() == FamilyMemberRole.CHILD);
            if (parentCount <= 1 && hasChildren) {
                throw new BusinessException("Cannot leave: you are the last parent with children in this family");
            }
        }

        family.getMembers().removeIf(m -> m.getUserId().equals(userId));

        // If no members left, delete the family
        if (family.getMembers().isEmpty()) {
            invitationRepository.deleteByFamilyId(familyId);
            familyRepository.delete(family);
        } else {
            familyRepository.save(family);
        }
    }

    @Transactional
    public FamilyInfo updateFamily(UUID familyId, String name, UUID requestingUserId) {
        assertAdminOrSectionAdmin(requestingUserId);
        var family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family", familyId));
        family.setName(name);
        family = familyRepository.save(family);
        return toFamilyInfo(family);
    }

    @Transactional
    public void deleteFamily(UUID familyId, UUID requestingUserId) {
        assertAdminOrSectionAdmin(requestingUserId);
        var family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family", familyId));
        invitationRepository.deleteByFamilyId(familyId);
        familyRepository.delete(family);
    }

    @Transactional
    public FamilyInfo setActive(UUID familyId, boolean active, UUID requestingUserId) {
        assertAdminOrSectionAdmin(requestingUserId);
        var family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family", familyId));
        family.setActive(active);
        family = familyRepository.save(family);
        return toFamilyInfo(family);
    }

    @Transactional
    public void updateAvatarUrl(UUID familyId, String avatarUrl) {
        var family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family", familyId));
        family.setAvatarUrl(avatarUrl);
        familyRepository.save(family);
    }

    // --- Invitations ---

    @Transactional
    public FamilyInvitationInfo inviteMember(UUID familyId, UUID inviteeId, String role, UUID inviterId) {
        var family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family", familyId));

        if (!familyRepository.isMember(inviterId, familyId)) {
            throw new BusinessException("Not a member of this family");
        }
        if (familyRepository.isMember(inviteeId, familyId)) {
            throw new BusinessException("User is already a member of this family");
        }
        if (invitationRepository.existsByFamilyIdAndInviteeIdAndStatus(familyId, inviteeId, FamilyInvitationStatus.PENDING)) {
            throw new BusinessException("A pending invitation already exists");
        }

        var memberRole = FamilyMemberRole.valueOf(role);
        var invitation = new FamilyInvitation(familyId, inviterId, inviteeId, memberRole);
        invitation = invitationRepository.save(invitation);

        String inviterName = userModuleApi.findById(inviterId).map(UserInfo::displayName).orElse("Unknown");
        eventPublisher.publishEvent(new FamilyInvitationEvent(
                invitation.getId(), familyId, family.getName(),
                inviterId, inviterName, inviteeId, false));

        return toInvitationInfo(invitation);
    }

    public List<FamilyInvitationInfo> getMyPendingInvitations(UUID userId) {
        return invitationRepository.findByInviteeIdAndStatusOrderByCreatedAtDesc(userId, FamilyInvitationStatus.PENDING)
                .stream().map(this::toInvitationInfo).toList();
    }

    public List<FamilyInvitationInfo> getFamilyInvitations(UUID familyId) {
        return invitationRepository.findByFamilyIdAndStatusOrderByCreatedAtDesc(familyId, FamilyInvitationStatus.PENDING)
                .stream().map(this::toInvitationInfo).toList();
    }

    @Transactional
    public FamilyInvitationInfo acceptInvitation(UUID invitationId, UUID userId) {
        assertCanJoinFamily(userId);

        var invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation", invitationId));
        if (!invitation.getInviteeId().equals(userId)) {
            throw new BusinessException("Not your invitation");
        }
        if (invitation.getStatus() != FamilyInvitationStatus.PENDING) {
            throw new BusinessException("Invitation is not pending");
        }

        invitation.setStatus(FamilyInvitationStatus.ACCEPTED);
        invitation.setResolvedAt(Instant.now());
        invitationRepository.save(invitation);

        // Add user to family
        var family = familyRepository.findById(invitation.getFamilyId())
                .orElseThrow(() -> new ResourceNotFoundException("Family", invitation.getFamilyId()));
        if (!familyRepository.isMember(userId, family.getId())) {
            var member = new FamilyMember(family, userId, invitation.getRole());
            family.getMembers().add(member);
            familyRepository.save(family);
        }

        String inviterName = userModuleApi.findById(invitation.getInviterId())
                .map(UserInfo::displayName).orElse("Unknown");
        eventPublisher.publishEvent(new FamilyInvitationEvent(
                invitationId, invitation.getFamilyId(), family.getName(),
                invitation.getInviterId(), inviterName, userId, true));

        return toInvitationInfo(invitation);
    }

    @Transactional
    public FamilyInvitationInfo declineInvitation(UUID invitationId, UUID userId) {
        var invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation", invitationId));
        if (!invitation.getInviteeId().equals(userId)) {
            throw new BusinessException("Not your invitation");
        }
        if (invitation.getStatus() != FamilyInvitationStatus.PENDING) {
            throw new BusinessException("Invitation is not pending");
        }

        invitation.setStatus(FamilyInvitationStatus.DECLINED);
        invitation.setResolvedAt(Instant.now());
        invitationRepository.save(invitation);

        return toInvitationInfo(invitation);
    }

    private void assertCanJoinFamily(UUID userId) {
        var user = userModuleApi.findById(userId);
        if (user.isPresent()) {
            var role = user.get().role();
            if (role == com.monteweb.user.UserRole.TEACHER
                    || role == com.monteweb.user.UserRole.SECTION_ADMIN) {
                throw new BusinessException("Teachers and section admins cannot join families");
            }
        }
    }

    private FamilyInvitationInfo toInvitationInfo(FamilyInvitation invitation) {
        String familyName = familyRepository.findById(invitation.getFamilyId())
                .map(Family::getName).orElse("Unknown");
        String inviterName = userModuleApi.findById(invitation.getInviterId())
                .map(UserInfo::displayName).orElse("Unknown");
        String inviteeName = userModuleApi.findById(invitation.getInviteeId())
                .map(UserInfo::displayName).orElse("Unknown");
        return new FamilyInvitationInfo(
                invitation.getId(), invitation.getFamilyId(), familyName,
                invitation.getInviterId(), inviterName,
                invitation.getInviteeId(), inviteeName,
                invitation.getRole().name(), invitation.getStatus().name(),
                invitation.getCreatedAt(), invitation.getResolvedAt());
    }

    @Transactional
    public FamilyInfo setHoursExempt(UUID familyId, boolean exempt, UUID requestingUserId) {
        assertAdminOrSectionAdmin(requestingUserId);
        var family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family", familyId));
        family.setHoursExempt(exempt);
        family = familyRepository.save(family);
        return toFamilyInfo(family);
    }

    private void assertAdminOrSectionAdmin(UUID userId) {
        var user = userModuleApi.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
        if (user.role() != com.monteweb.user.UserRole.SUPERADMIN
                && user.role() != com.monteweb.user.UserRole.SECTION_ADMIN) {
            throw new BusinessException("Only SUPERADMIN or SECTION_ADMIN can manage families");
        }
    }

    private FamilyInfo toFamilyInfo(Family family) {
        var members = family.getMembers().stream()
                .map(m -> {
                    String displayName = userModuleApi.findById(m.getUserId())
                            .map(UserInfo::displayName)
                            .orElse("Unknown");
                    return new FamilyInfo.FamilyMemberInfo(m.getUserId(), displayName, m.getRole().name());
                })
                .toList();
        return new FamilyInfo(family.getId(), family.getName(), family.getAvatarUrl(), family.isHoursExempt(), family.isActive(), members);
    }

    /**
     * DSGVO: Clean up all family data for a deleted user.
     */
    @Transactional
    public void cleanupUserData(UUID userId) {
        // Delete invitations involving the user
        invitationRepository.deleteByInviteeId(userId);
        invitationRepository.deleteByInviterId(userId);
        // Remove user from all families they belong to
        var families = familyRepository.findByMemberUserId(userId);
        for (var family : families) {
            family.getMembers().removeIf(m -> userId.equals(m.getUserId()));
            familyRepository.save(family);
        }
    }

    @Override
    public Optional<FamilyInfo> findByNameIgnoreCase(String name) {
        return familyRepository.findByNameIgnoreCase(name).map(this::toFamilyInfo);
    }

    @Override
    @Transactional
    public FamilyInfo adminCreateFamily(String name) {
        var family = new Family();
        family.setName(name);
        family = familyRepository.save(family);
        return toFamilyInfo(family);
    }

    /**
     * DSGVO: Export all family data for a user.
     */
    @Override
    public Map<String, Object> exportUserData(UUID userId) {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        var families = familyRepository.findByMemberUserId(userId);
        data.put("families", families.stream().map(f -> Map.of(
                "id", f.getId(),
                "name", f.getName()
        )).toList());
        return data;
    }
}
