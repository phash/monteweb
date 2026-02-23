package com.monteweb.admin.internal.service;

import com.monteweb.admin.internal.dto.CsvImportResult;
import com.monteweb.admin.internal.dto.CsvImportResult.CsvRowError;
import com.monteweb.admin.internal.dto.CsvImportResult.CsvRowPreview;
import com.monteweb.family.FamilyModuleApi;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class CsvImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);
    private static final String DEFAULT_PASSWORD = "changeme123";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
    private static final Set<String> VALID_ROLES = Set.of("PARENT", "STUDENT", "TEACHER", "SECTION_ADMIN");
    private static final Set<String> VALID_FAMILY_ROLES = Set.of("PARENT", "CHILD");

    private final UserModuleApi userModuleApi;
    private final FamilyModuleApi familyModuleApi;
    private final PasswordEncoder passwordEncoder;

    public CsvImportService(UserModuleApi userModuleApi,
                            FamilyModuleApi familyModuleApi,
                            PasswordEncoder passwordEncoder) {
        this.userModuleApi = userModuleApi;
        this.familyModuleApi = familyModuleApi;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Process a CSV file: parse, validate, and optionally import users and families.
     *
     * @param file   the uploaded CSV file
     * @param dryRun if true, only validate without persisting
     * @return the import result with statistics and errors
     */
    @Transactional
    public CsvImportResult processCsv(MultipartFile file, boolean dryRun) {
        List<String[]> rows = parseCsv(file);
        List<CsvRowError> errors = new ArrayList<>();
        List<CsvRowPreview> previews = new ArrayList<>();

        int usersCreated = 0;
        int familiesCreated = 0;

        // Track families created during this import (name -> familyId)
        Map<String, UUID> importedFamilies = new HashMap<>();
        // Track emails seen in this import to detect duplicates within the file
        Set<String> seenEmails = new HashSet<>();

        String hashedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);

        for (int i = 0; i < rows.size(); i++) {
            int rowNum = i + 2; // +2 because row 1 is header, data starts at row 2
            String[] cols = rows.get(i);

            String email = col(cols, 0).trim().toLowerCase();
            String firstName = col(cols, 1).trim();
            String lastName = col(cols, 2).trim();
            String roleStr = col(cols, 3).trim().toUpperCase();
            String familyName = col(cols, 4).trim();
            String familyRoleStr = col(cols, 5).trim().toUpperCase();
            String sectionSlug = col(cols, 6).trim();

            // Validate required fields
            List<String> rowErrors = new ArrayList<>();

            if (email.isEmpty()) {
                rowErrors.add("E-Mail ist erforderlich");
            } else if (!EMAIL_PATTERN.matcher(email).matches()) {
                rowErrors.add("Ungültiges E-Mail-Format: " + email);
            } else if (seenEmails.contains(email)) {
                rowErrors.add("Doppelte E-Mail in der CSV-Datei: " + email);
            } else if (userModuleApi.existsByEmail(email)) {
                rowErrors.add("E-Mail existiert bereits: " + email);
            }

            if (firstName.isEmpty()) {
                rowErrors.add("Vorname ist erforderlich");
            }
            if (lastName.isEmpty()) {
                rowErrors.add("Nachname ist erforderlich");
            }

            if (roleStr.isEmpty()) {
                rowErrors.add("Rolle ist erforderlich");
            } else if (!VALID_ROLES.contains(roleStr)) {
                rowErrors.add("Ungültige Rolle: " + roleStr + " (erlaubt: PARENT, STUDENT, TEACHER, SECTION_ADMIN)");
            }

            if (!familyName.isEmpty() && !familyRoleStr.isEmpty() && !VALID_FAMILY_ROLES.contains(familyRoleStr)) {
                rowErrors.add("Ungültige Familienrolle: " + familyRoleStr + " (erlaubt: PARENT, CHILD)");
            }

            if (!familyName.isEmpty() && familyRoleStr.isEmpty()) {
                rowErrors.add("Familienrolle ist erforderlich wenn Familienname angegeben ist");
            }

            boolean valid = rowErrors.isEmpty();
            String errorMsg = valid ? null : String.join("; ", rowErrors);

            previews.add(new CsvRowPreview(
                    rowNum, email, firstName + " " + lastName, roleStr,
                    familyName, familyRoleStr, sectionSlug, valid, errorMsg));

            if (!valid) {
                for (String err : rowErrors) {
                    errors.add(new CsvRowError(rowNum, "", err));
                }
                continue;
            }

            seenEmails.add(email);

            if (!dryRun) {
                // Create user
                UserRole role = UserRole.valueOf(roleStr);
                var userInfo = userModuleApi.createUser(email, hashedPassword, firstName, lastName, null, role);
                // Activate user immediately
                userModuleApi.setActive(userInfo.id(), true);
                usersCreated++;

                // Handle family assignment
                if (!familyName.isEmpty()) {
                    String familyKey = familyName.toLowerCase().trim();
                    UUID familyId = importedFamilies.get(familyKey);

                    if (familyId == null) {
                        // Check if family already exists in DB
                        var existingFamily = familyModuleApi.findByNameIgnoreCase(familyName);
                        if (existingFamily.isPresent()) {
                            familyId = existingFamily.get().id();
                        } else {
                            // Create new family
                            var newFamily = familyModuleApi.adminCreateFamily(familyName);
                            familyId = newFamily.id();
                            familiesCreated++;
                        }
                        importedFamilies.put(familyKey, familyId);
                    }

                    // Add user to family
                    try {
                        familyModuleApi.adminAddMember(familyId, userInfo.id(), familyRoleStr.isEmpty() ? "PARENT" : familyRoleStr);
                    } catch (Exception e) {
                        log.warn("Failed to add user {} to family {}: {}", email, familyName, e.getMessage());
                    }
                }
            } else {
                // In dry-run mode, count what would happen
                usersCreated++;
                if (!familyName.isEmpty()) {
                    String familyKey = familyName.toLowerCase().trim();
                    if (!importedFamilies.containsKey(familyKey)) {
                        var existingFamily = familyModuleApi.findByNameIgnoreCase(familyName);
                        if (existingFamily.isEmpty()) {
                            importedFamilies.put(familyKey, UUID.randomUUID()); // placeholder
                            familiesCreated++;
                        } else {
                            importedFamilies.put(familyKey, existingFamily.get().id());
                        }
                    }
                }
            }
        }

        return new CsvImportResult(
                rows.size(),
                usersCreated,
                familiesCreated,
                errors.size(),
                errors,
                previews
        );
    }

    /**
     * Generate an example CSV file content.
     */
    public byte[] generateExampleCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("email;firstName;lastName;role;familyName;familyRole;sectionSlug\n");
        sb.append("maria.mueller@schule.de;Maria;Müller;PARENT;Familie Müller;PARENT;grundstufe\n");
        sb.append("thomas.mueller@schule.de;Thomas;Müller;PARENT;Familie Müller;PARENT;grundstufe\n");
        sb.append("leon.mueller@schule.de;Leon;Müller;STUDENT;Familie Müller;CHILD;grundstufe\n");
        sb.append("emma.mueller@schule.de;Emma;Müller;STUDENT;Familie Müller;CHILD;kindergarten\n");
        sb.append("sandra.schmidt@schule.de;Sandra;Schmidt;PARENT;Familie Schmidt;PARENT;mittelstufe\n");
        sb.append("finn.schmidt@schule.de;Finn;Schmidt;STUDENT;Familie Schmidt;CHILD;mittelstufe\n");
        sb.append("julia.weber@schule.de;Julia;Weber;TEACHER;;;grundstufe\n");
        sb.append("markus.hoffmann@schule.de;Markus;Hoffmann;TEACHER;;;mittelstufe\n");
        sb.append("petra.bauer@schule.de;Petra;Bauer;SECTION_ADMIN;;;oberstufe\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<String[]> parseCsv(MultipartFile file) {
        List<String[]> rows = new ArrayList<>();
        try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                // Skip BOM if present
                if (firstLine) {
                    if (line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    firstLine = false;
                    // Skip header row
                    if (line.toLowerCase().contains("email") || line.toLowerCase().contains("e-mail")) {
                        continue;
                    }
                }

                line = line.trim();
                if (line.isEmpty()) continue;

                // Support both semicolon and tab as separator
                String[] cols;
                if (line.contains(";")) {
                    cols = line.split(";", -1);
                } else if (line.contains("\t")) {
                    cols = line.split("\t", -1);
                } else {
                    cols = line.split(",", -1);
                }

                rows.add(cols);
            }
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Lesen der CSV-Datei: " + e.getMessage(), e);
        }
        return rows;
    }

    private String col(String[] cols, int index) {
        if (index >= cols.length) return "";
        return cols[index] != null ? cols[index] : "";
    }
}
