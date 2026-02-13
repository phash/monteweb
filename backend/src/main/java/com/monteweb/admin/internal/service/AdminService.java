package com.monteweb.admin.internal.service;

import com.monteweb.admin.AdminModuleApi;
import com.monteweb.admin.TenantConfigInfo;
import com.monteweb.admin.internal.model.TenantConfig;
import com.monteweb.admin.internal.repository.TenantConfigRepository;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AdminService implements AdminModuleApi {

    private final TenantConfigRepository configRepository;

    public AdminService(TenantConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    public TenantConfigInfo getTenantConfig() {
        return toInfo(getConfig());
    }

    @Override
    public boolean isModuleEnabled(String moduleName) {
        var config = getConfig();
        return config.getModules().getOrDefault(moduleName, false);
    }

    @Transactional
    public TenantConfigInfo updateConfig(String schoolName, String logoUrl, BigDecimal targetHours, BigDecimal targetCleaningHours,
                                          String bundesland, java.util.List<java.util.Map<String, String>> schoolVacations) {
        var config = getConfig();
        if (schoolName != null) config.setSchoolName(schoolName);
        if (logoUrl != null) config.setLogoUrl(logoUrl);
        if (targetHours != null) config.setTargetHoursPerFamily(targetHours);
        if (targetCleaningHours != null) config.setTargetCleaningHours(targetCleaningHours);
        if (bundesland != null) config.setBundesland(bundesland);
        if (schoolVacations != null) config.setSchoolVacations(schoolVacations);
        return toInfo(configRepository.save(config));
    }

    @Transactional
    public TenantConfigInfo updateTheme(Map<String, Object> theme) {
        var config = getConfig();
        config.setTheme(theme);
        return toInfo(configRepository.save(config));
    }

    @Transactional
    public TenantConfigInfo updateModules(Map<String, Boolean> modules) {
        var config = getConfig();
        config.setModules(modules);
        return toInfo(configRepository.save(config));
    }

    /**
     * Upload logo as base64 data URL (stored directly in tenant config).
     * This avoids a MinIO dependency for the admin module.
     * For production, consider a dedicated file service.
     */
    @Transactional
    public TenantConfigInfo uploadLogo(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("Logo file is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("File must be an image");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException("Logo must be smaller than 2MB");
        }
        try {
            byte[] bytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String dataUrl = "data:" + contentType + ";base64," + base64;

            var config = getConfig();
            config.setLogoUrl(dataUrl);
            return toInfo(configRepository.save(config));
        } catch (java.io.IOException e) {
            throw new BusinessException("Failed to read logo file");
        }
    }

    private TenantConfig getConfig() {
        return configRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Tenant configuration not found"));
    }

    private TenantConfigInfo toInfo(TenantConfig config) {
        return new TenantConfigInfo(
                config.getId(),
                config.getSchoolName(),
                config.getLogoUrl(),
                config.getTheme(),
                config.getModules(),
                config.getTargetHoursPerFamily(),
                config.getTargetCleaningHours(),
                config.isParentToParentMessaging(),
                config.isStudentToStudentMessaging(),
                config.getBundesland(),
                config.getSchoolVacations(),
                config.getGithubRepo(),
                config.getGithubPat() != null && !config.getGithubPat().isBlank()
        );
    }
}
