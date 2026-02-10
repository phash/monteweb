package com.monteweb.school.internal.service;

import com.monteweb.school.SchoolModuleApi;
import com.monteweb.school.SchoolSectionInfo;
import com.monteweb.school.internal.model.SchoolSection;
import com.monteweb.school.internal.repository.SchoolSectionRepository;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SchoolSectionService implements SchoolModuleApi {

    private final SchoolSectionRepository repository;

    public SchoolSectionService(SchoolSectionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<SchoolSectionInfo> findById(UUID id) {
        return repository.findById(id).map(this::toInfo);
    }

    @Override
    public Optional<SchoolSectionInfo> findBySlug(String slug) {
        return repository.findBySlug(slug).map(this::toInfo);
    }

    @Override
    public List<SchoolSectionInfo> findAllActive() {
        return repository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(this::toInfo)
                .toList();
    }

    @Transactional
    public SchoolSectionInfo create(String name, String description, int sortOrder) {
        String slug = toSlug(name);
        if (repository.existsBySlug(slug)) {
            throw new BusinessException("A section with this name already exists");
        }

        var section = new SchoolSection();
        section.setName(name);
        section.setSlug(slug);
        section.setDescription(description);
        section.setSortOrder(sortOrder);

        return toInfo(repository.save(section));
    }

    @Transactional
    public SchoolSectionInfo update(UUID id, String name, String description, Integer sortOrder) {
        var section = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolSection", id));

        if (name != null) {
            String newSlug = toSlug(name);
            if (!newSlug.equals(section.getSlug()) && repository.existsBySlug(newSlug)) {
                throw new BusinessException("A section with this name already exists");
            }
            section.setName(name);
            section.setSlug(newSlug);
        }
        if (description != null) section.setDescription(description);
        if (sortOrder != null) section.setSortOrder(sortOrder);

        return toInfo(repository.save(section));
    }

    @Transactional
    public void deactivate(UUID id) {
        var section = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolSection", id));
        section.setActive(false);
        repository.save(section);
    }

    private String toSlug(String name) {
        return name.toLowerCase(Locale.GERMAN)
                .replaceAll("[äÄ]", "ae")
                .replaceAll("[öÖ]", "oe")
                .replaceAll("[üÜ]", "ue")
                .replaceAll("ß", "ss")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private SchoolSectionInfo toInfo(SchoolSection section) {
        return new SchoolSectionInfo(
                section.getId(),
                section.getName(),
                section.getSlug(),
                section.getDescription(),
                section.getSortOrder(),
                section.isActive()
        );
    }
}
