package com.monteweb.profilefields.internal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monteweb.profilefields.ProfileFieldInfo;
import com.monteweb.profilefields.ProfileFieldsModuleApi;
import com.monteweb.profilefields.internal.dto.CreateProfileFieldRequest;
import com.monteweb.profilefields.internal.dto.UpdateProfileFieldRequest;
import com.monteweb.profilefields.internal.model.ProfileFieldDefinition;
import com.monteweb.profilefields.internal.model.ProfileFieldValue;
import com.monteweb.profilefields.internal.repository.ProfileFieldDefinitionRepository;
import com.monteweb.profilefields.internal.repository.ProfileFieldValueRepository;
import com.monteweb.shared.exception.BadRequestException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(prefix = "monteweb.modules", name = "profilefields.enabled", havingValue = "true")
public class ProfileFieldsService implements ProfileFieldsModuleApi {

    private final ProfileFieldDefinitionRepository definitionRepo;
    private final ProfileFieldValueRepository valueRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProfileFieldsService(ProfileFieldDefinitionRepository definitionRepo, ProfileFieldValueRepository valueRepo) {
        this.definitionRepo = definitionRepo;
        this.valueRepo = valueRepo;
    }

    // ---- Admin: definitions ----

    public List<ProfileFieldInfo> getAllDefinitions() {
        return definitionRepo.findAllByOrderByPositionAsc().stream()
                .map(this::toInfo)
                .toList();
    }

    @Override
    public List<ProfileFieldInfo> getFieldDefinitions() {
        return definitionRepo.findByActiveTrueOrderByPositionAsc().stream()
                .map(this::toInfo)
                .toList();
    }

    @Transactional
    public ProfileFieldInfo createDefinition(CreateProfileFieldRequest request) {
        if (definitionRepo.existsByFieldKey(request.fieldKey())) {
            throw new BadRequestException("Field key already exists: " + request.fieldKey());
        }

        var def = new ProfileFieldDefinition();
        def.setFieldKey(request.fieldKey());
        def.setLabelDe(request.labelDe());
        def.setLabelEn(request.labelEn());
        def.setFieldType(request.fieldType());
        def.setOptions(serializeOptions(request.options()));
        def.setRequired(request.required());
        def.setPosition(request.position());
        def.setActive(true);

        return toInfo(definitionRepo.save(def));
    }

    @Transactional
    public ProfileFieldInfo updateDefinition(UUID id, UpdateProfileFieldRequest request) {
        var def = requireDefinition(id);

        if (request.labelDe() != null) def.setLabelDe(request.labelDe());
        if (request.labelEn() != null) def.setLabelEn(request.labelEn());
        if (request.options() != null) def.setOptions(serializeOptions(request.options()));
        if (request.required() != null) def.setRequired(request.required());
        if (request.position() != null) def.setPosition(request.position());
        if (request.active() != null) def.setActive(request.active());

        return toInfo(definitionRepo.save(def));
    }

    @Transactional
    public void deleteDefinition(UUID id) {
        var def = requireDefinition(id);
        valueRepo.deleteByFieldId(id);
        definitionRepo.delete(def);
    }

    // ---- User: values ----

    @Override
    public Map<String, String> getUserFieldValues(UUID userId) {
        return valueRepo.findByUserId(userId).stream()
                .collect(Collectors.toMap(
                        v -> v.getFieldId().toString(),
                        v -> v.getValue() != null ? v.getValue() : ""
                ));
    }

    @Transactional
    public Map<String, String> updateUserValues(UUID userId, Map<String, String> values) {
        var activeFields = definitionRepo.findByActiveTrueOrderByPositionAsc().stream()
                .collect(Collectors.toMap(d -> d.getId().toString(), d -> d));

        // Validate required fields
        for (var entry : activeFields.entrySet()) {
            var def = entry.getValue();
            if (def.isRequired()) {
                String val = values.get(entry.getKey());
                if (val == null || val.isBlank()) {
                    throw new BadRequestException("Required field is missing: " + def.getLabelDe());
                }
            }
        }

        // Validate SELECT values
        for (var entry : values.entrySet()) {
            var def = activeFields.get(entry.getKey());
            if (def == null) continue;

            if ("SELECT".equals(def.getFieldType()) && entry.getValue() != null && !entry.getValue().isBlank()) {
                List<String> opts = parseOptions(def.getOptions());
                if (opts != null && !opts.contains(entry.getValue())) {
                    throw new BadRequestException("Invalid option for field " + def.getLabelDe() + ": " + entry.getValue());
                }
            }
        }

        var existing = valueRepo.findByUserId(userId).stream()
                .collect(Collectors.toMap(v -> v.getFieldId().toString(), v -> v));

        for (var entry : values.entrySet()) {
            if (!activeFields.containsKey(entry.getKey())) continue;

            var fieldValue = existing.get(entry.getKey());
            if (fieldValue != null) {
                fieldValue.setValue(entry.getValue());
                valueRepo.save(fieldValue);
            } else {
                var newVal = new ProfileFieldValue();
                newVal.setUserId(userId);
                newVal.setFieldId(UUID.fromString(entry.getKey()));
                newVal.setValue(entry.getValue());
                valueRepo.save(newVal);
            }
        }

        return getUserFieldValues(userId);
    }

    // ---- DSGVO ----

    @Override
    public Map<String, Object> exportUserData(UUID userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        var values = valueRepo.findByUserId(userId);
        var defs = definitionRepo.findByActiveTrueOrderByPositionAsc().stream()
                .collect(Collectors.toMap(d -> d.getId(), d -> d));

        data.put("customFields", values.stream().map(v -> {
            var def = defs.get(v.getFieldId());
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("fieldKey", def != null ? def.getFieldKey() : v.getFieldId().toString());
            entry.put("value", v.getValue());
            return entry;
        }).toList());

        return data;
    }

    // ---- Helpers ----

    private ProfileFieldDefinition requireDefinition(UUID id) {
        return definitionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile field not found: " + id));
    }

    private ProfileFieldInfo toInfo(ProfileFieldDefinition def) {
        return new ProfileFieldInfo(
                def.getId(),
                def.getFieldKey(),
                def.getLabelDe(),
                def.getLabelEn(),
                def.getFieldType(),
                parseOptions(def.getOptions()),
                def.isRequired(),
                def.getPosition()
        );
    }

    private String serializeOptions(List<String> options) {
        if (options == null || options.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid options format");
        }
    }

    private List<String> parseOptions(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
