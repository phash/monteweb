package com.monteweb.family.internal.service;

import com.monteweb.family.FamilyInfo;
import com.monteweb.family.FamilyModuleApi;
import com.monteweb.family.internal.model.Family;
import com.monteweb.family.internal.model.FamilyMember;
import com.monteweb.family.internal.model.FamilyMemberRole;
import com.monteweb.family.internal.repository.FamilyRepository;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FamilyService implements FamilyModuleApi {

    private final FamilyRepository familyRepository;
    private final InviteCodeService inviteCodeService;
    private final UserModuleApi userModuleApi;

    public FamilyService(FamilyRepository familyRepository,
                         InviteCodeService inviteCodeService,
                         UserModuleApi userModuleApi) {
        this.familyRepository = familyRepository;
        this.inviteCodeService = inviteCodeService;
        this.userModuleApi = userModuleApi;
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
    public void updateAvatarUrl(UUID familyId, String avatarUrl) {
        var family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family", familyId));
        family.setAvatarUrl(avatarUrl);
        familyRepository.save(family);
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
        return new FamilyInfo(family.getId(), family.getName(), family.getAvatarUrl(), members);
    }
}
