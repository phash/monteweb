package com.monteweb.forms.internal.service;

import com.monteweb.forms.*;
import com.monteweb.forms.internal.model.*;
import com.monteweb.forms.internal.repository.*;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.room.RoomRole;
import com.monteweb.school.SchoolModuleApi;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@ConditionalOnProperty(prefix = "monteweb.modules", name = "forms.enabled", havingValue = "true")
public class FormsService implements FormsModuleApi {

    private final FormRepository formRepository;
    private final FormQuestionRepository questionRepository;
    private final FormResponseRepository responseRepository;
    private final FormAnswerRepository answerRepository;
    private final FormResponseTrackingRepository trackingRepository;
    private final RoomModuleApi roomModule;
    private final SchoolModuleApi schoolModule;
    private final UserModuleApi userModule;
    private final ApplicationEventPublisher eventPublisher;

    public FormsService(FormRepository formRepository,
                        FormQuestionRepository questionRepository,
                        FormResponseRepository responseRepository,
                        FormAnswerRepository answerRepository,
                        FormResponseTrackingRepository trackingRepository,
                        RoomModuleApi roomModule,
                        SchoolModuleApi schoolModule,
                        UserModuleApi userModule,
                        ApplicationEventPublisher eventPublisher) {
        this.formRepository = formRepository;
        this.questionRepository = questionRepository;
        this.responseRepository = responseRepository;
        this.answerRepository = answerRepository;
        this.trackingRepository = trackingRepository;
        this.roomModule = roomModule;
        this.schoolModule = schoolModule;
        this.userModule = userModule;
        this.eventPublisher = eventPublisher;
    }

    public Page<FormInfo> getAvailableForms(UUID userId, Pageable pageable) {
        var rooms = roomModule.findByUserId(userId);
        var roomIds = rooms.stream().map(r -> r.id()).toList();
        var sectionIds = rooms.stream()
                .filter(r -> r.sectionId() != null)
                .map(r -> r.sectionId())
                .distinct()
                .toList();

        if (roomIds.isEmpty()) roomIds = List.of(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        if (sectionIds.isEmpty()) sectionIds = List.of(UUID.fromString("00000000-0000-0000-0000-000000000000"));

        return formRepository.findAvailableForms(roomIds, sectionIds, pageable)
                .map(f -> toFormInfo(f, userId));
    }

    public Page<FormInfo> getMyForms(UUID userId, Pageable pageable) {
        return formRepository.findByCreatedByOrderByCreatedAtDesc(userId, pageable)
                .map(f -> toFormInfo(f, userId));
    }

    public FormDetailInfo getForm(UUID formId, UUID userId) {
        var form = formRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found"));
        var questions = questionRepository.findByFormIdOrderBySortOrder(formId);
        return new FormDetailInfo(
                toFormInfo(form, userId),
                questions.stream().map(this::toQuestionInfo).toList()
        );
    }

    public FormDetailInfo createForm(CreateFormRequest request, UUID userId) {
        checkCreatePermission(request.scope(), request.scopeId(), userId);

        var form = new Form();
        form.setTitle(request.title());
        form.setDescription(request.description());
        form.setType(request.type());
        form.setScope(request.scope());
        form.setScopeId(request.scopeId());
        if (request.scope() == FormScope.SECTION && request.sectionIds() != null && !request.sectionIds().isEmpty()) {
            form.setSectionIds(request.sectionIds().toArray(new UUID[0]));
            form.setScopeId(request.sectionIds().get(0));
        }
        form.setAnonymous(request.anonymous());
        form.setDeadline(request.deadline());
        form.setCreatedBy(userId);
        form = formRepository.save(form);

        List<FormQuestion> savedQuestions = saveQuestions(form.getId(), request.questions());

        return new FormDetailInfo(
                toFormInfo(form, userId),
                savedQuestions.stream().map(this::toQuestionInfo).toList()
        );
    }

    public FormDetailInfo updateForm(UUID formId, UpdateFormRequest request, UUID userId) {
        var form = formRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found"));

        if (form.getStatus() != FormStatus.DRAFT) {
            throw new IllegalStateException("Only draft forms can be edited");
        }

        checkCreatePermission(form.getScope(), form.getScopeId(), userId);

        if (request.title() != null) form.setTitle(request.title());
        if (request.description() != null) form.setDescription(request.description());
        if (request.deadline() != null) form.setDeadline(request.deadline());

        form = formRepository.save(form);

        List<FormQuestion> savedQuestions;
        if (request.questions() != null) {
            questionRepository.deleteByFormId(formId);
            savedQuestions = saveQuestions(formId, request.questions());
        } else {
            savedQuestions = questionRepository.findByFormIdOrderBySortOrder(formId);
        }

        return new FormDetailInfo(
                toFormInfo(form, userId),
                savedQuestions.stream().map(this::toQuestionInfo).toList()
        );
    }

    public void deleteForm(UUID formId, UUID userId) {
        var form = formRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found"));

        var user = userModule.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // SUPERADMIN can delete any form regardless of status
        if (user.role() == UserRole.SUPERADMIN) {
            answerRepository.deleteByResponseFormId(formId);
            responseRepository.deleteByFormId(formId);
            trackingRepository.deleteByFormId(formId);
            questionRepository.deleteByFormId(formId);
            formRepository.delete(form);
            return;
        }

        // Creator can delete their own forms regardless of status
        if (form.getCreatedBy().equals(userId)) {
            answerRepository.deleteByResponseFormId(formId);
            responseRepository.deleteByFormId(formId);
            trackingRepository.deleteByFormId(formId);
            questionRepository.deleteByFormId(formId);
            formRepository.delete(form);
            return;
        }

        if (form.getStatus() != FormStatus.DRAFT) {
            throw new IllegalStateException("Only draft forms can be deleted");
        }

        checkCreatePermission(form.getScope(), form.getScopeId(), userId);
        formRepository.delete(form);
    }

    public FormInfo publishForm(UUID formId, UUID userId) {
        var form = formRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found"));

        if (form.getStatus() != FormStatus.DRAFT) {
            throw new IllegalStateException("Only draft forms can be published");
        }

        checkManagePermission(form, userId);

        int qCount = questionRepository.countByFormId(formId);
        if (qCount == 0) {
            throw new IllegalStateException("Cannot publish a form without questions");
        }

        form.setStatus(FormStatus.PUBLISHED);
        form.setPublishedAt(Instant.now());
        form = formRepository.save(form);

        var user = userModule.findById(userId).orElse(null);
        String publisherName = user != null ? user.displayName() : "Unknown";

        eventPublisher.publishEvent(new FormPublishedEvent(
                form.getId(), form.getTitle(), form.getType(),
                form.getScope(), form.getScopeId(),
                form.getSectionIds() != null ? List.of(form.getSectionIds()) : List.of(),
                userId, publisherName
        ));

        return toFormInfo(form, userId);
    }

    public FormInfo closeForm(UUID formId, UUID userId) {
        var form = formRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found"));

        if (form.getStatus() != FormStatus.PUBLISHED) {
            throw new IllegalStateException("Only published forms can be closed");
        }

        checkManagePermission(form, userId);

        form.setStatus(FormStatus.CLOSED);
        form.setClosedAt(Instant.now());
        form = formRepository.save(form);

        eventPublisher.publishEvent(new FormClosedEvent(
                form.getId(), form.getTitle(), form.getScope(), form.getScopeId(),
                form.getSectionIds() != null ? List.of(form.getSectionIds()) : List.of()
        ));

        return toFormInfo(form, userId);
    }

    public void submitResponse(UUID formId, SubmitResponseRequest request, UUID userId) {
        var form = formRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found"));

        if (form.getStatus() != FormStatus.PUBLISHED) {
            throw new IllegalStateException("Form is not accepting responses");
        }

        if (form.getDeadline() != null && java.time.LocalDate.now().isAfter(form.getDeadline())) {
            throw new IllegalStateException("Form deadline has passed");
        }

        // Check for duplicate responses
        if (form.isAnonymous()) {
            if (trackingRepository.existsByFormIdAndUserId(formId, userId)) {
                throw new IllegalStateException("You have already responded to this form");
            }
        } else {
            if (responseRepository.existsByFormIdAndUserId(formId, userId)) {
                throw new IllegalStateException("You have already responded to this form");
            }
        }

        var questions = questionRepository.findByFormIdOrderBySortOrder(formId);
        var questionMap = questions.stream().collect(Collectors.toMap(FormQuestion::getId, q -> q));

        // Validate required questions
        var answeredIds = request.answers().stream()
                .map(AnswerRequest::questionId)
                .collect(Collectors.toSet());

        for (var q : questions) {
            if (q.isRequired() && !answeredIds.contains(q.getId())) {
                throw new IllegalArgumentException("Required question not answered: " + q.getLabel());
            }
        }

        // Save response
        var response = new FormResponse();
        response.setFormId(formId);
        response.setUserId(form.isAnonymous() ? null : userId);
        response = responseRepository.save(response);

        // Save answers
        for (var answerReq : request.answers()) {
            var question = questionMap.get(answerReq.questionId());
            if (question == null) continue;

            var answer = new FormAnswer();
            answer.setResponseId(response.getId());
            answer.setQuestionId(answerReq.questionId());

            switch (question.getType()) {
                case TEXT -> answer.setAnswerText(answerReq.text());
                case SINGLE_CHOICE -> answer.setAnswerOptions(
                        answerReq.selectedOptions() != null ? answerReq.selectedOptions() : List.of());
                case MULTIPLE_CHOICE -> answer.setAnswerOptions(
                        answerReq.selectedOptions() != null ? answerReq.selectedOptions() : List.of());
                case RATING -> answer.setAnswerRating(answerReq.rating());
                case YES_NO -> answer.setAnswerText(answerReq.text());
            }

            answerRepository.save(answer);
        }

        // Track anonymous responses
        if (form.isAnonymous()) {
            var tracking = new FormResponseTracking();
            tracking.setFormId(formId);
            tracking.setUserId(userId);
            trackingRepository.save(tracking);
        }
    }

    public void updateResponse(UUID formId, SubmitResponseRequest request, UUID userId) {
        var form = formRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found"));

        if (form.getStatus() != FormStatus.PUBLISHED) {
            throw new IllegalStateException("Form is not accepting responses");
        }

        if (form.getDeadline() != null && java.time.LocalDate.now().isAfter(form.getDeadline())) {
            throw new IllegalStateException("Form deadline has passed");
        }

        if (form.isAnonymous()) {
            throw new IllegalStateException("Anonymous responses cannot be edited");
        }

        var existingResponse = responseRepository.findByFormIdAndUserId(formId, userId)
                .orElseThrow(() -> new IllegalArgumentException("No existing response found"));

        var questions = questionRepository.findByFormIdOrderBySortOrder(formId);
        var questionMap = questions.stream().collect(Collectors.toMap(FormQuestion::getId, q -> q));

        // Delete old answers
        answerRepository.deleteByResponseId(existingResponse.getId());

        // Save new answers
        for (var answerReq : request.answers()) {
            var question = questionMap.get(answerReq.questionId());
            if (question == null) continue;

            var answer = new FormAnswer();
            answer.setResponseId(existingResponse.getId());
            answer.setQuestionId(answerReq.questionId());

            switch (question.getType()) {
                case TEXT -> answer.setAnswerText(answerReq.text());
                case SINGLE_CHOICE -> answer.setAnswerOptions(
                        answerReq.selectedOptions() != null ? answerReq.selectedOptions() : List.of());
                case MULTIPLE_CHOICE -> answer.setAnswerOptions(
                        answerReq.selectedOptions() != null ? answerReq.selectedOptions() : List.of());
                case RATING -> answer.setAnswerRating(answerReq.rating());
                case YES_NO -> answer.setAnswerText(answerReq.text());
            }

            answerRepository.save(answer);
        }

        // Update submitted timestamp
        existingResponse.setSubmittedAt(Instant.now());
        responseRepository.save(existingResponse);
    }

    public MyResponseInfo getMyResponse(UUID formId, UUID userId) {
        var form = formRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found"));

        if (form.isAnonymous()) {
            return null;
        }

        var response = responseRepository.findByFormIdAndUserId(formId, userId).orElse(null);
        if (response == null) return null;

        var answers = answerRepository.findByResponseId(response.getId());
        var answerInfos = answers.stream()
                .map(a -> new MyAnswerInfo(a.getQuestionId(), a.getAnswerText(), a.getAnswerOptions(), a.getAnswerRating()))
                .toList();

        return new MyResponseInfo(response.getId(), answerInfos);
    }

    public FormInfo archiveForm(UUID formId, UUID userId) {
        var form = formRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found"));

        if (form.getStatus() != FormStatus.CLOSED) {
            throw new IllegalStateException("Only closed forms can be archived");
        }

        checkManagePermission(form, userId);

        form.setStatus(FormStatus.ARCHIVED);
        form = formRepository.save(form);

        return toFormInfo(form, userId);
    }

    public FormResultsSummary getResults(UUID formId, UUID userId) {
        var form = formRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found"));

        checkResultsPermission(form, userId);

        var questions = questionRepository.findByFormIdOrderBySortOrder(formId);
        var questionResults = new ArrayList<QuestionResult>();

        for (var question : questions) {
            var answers = answerRepository.findByQuestionId(question.getId());
            questionResults.add(aggregateQuestionResult(question, answers));
        }

        return new FormResultsSummary(toFormInfo(form, userId), questionResults);
    }

    public List<IndividualResponse> getIndividualResponses(UUID formId, UUID userId) {
        var form = formRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found"));

        if (form.isAnonymous()) {
            throw new IllegalStateException("Individual responses not available for anonymous forms");
        }

        checkResultsPermission(form, userId);

        var questions = questionRepository.findByFormIdOrderBySortOrder(formId);
        var questionMap = questions.stream().collect(Collectors.toMap(FormQuestion::getId, q -> q));

        var responses = responseRepository.findByFormIdOrderBySubmittedAtDesc(formId);
        var result = new ArrayList<IndividualResponse>();

        for (var response : responses) {
            var user = response.getUserId() != null
                    ? userModule.findById(response.getUserId()).orElse(null) : null;
            String userName = user != null ? user.displayName() : "Anonymous";

            var answers = answerRepository.findByResponseId(response.getId());
            var individualAnswers = answers.stream().map(a -> {
                var question = questionMap.get(a.getQuestionId());
                return new IndividualAnswer(
                        a.getQuestionId(),
                        question != null ? question.getLabel() : "",
                        question != null ? question.getType() : null,
                        a.getAnswerText(),
                        a.getAnswerOptions(),
                        a.getAnswerRating()
                );
            }).toList();

            result.add(new IndividualResponse(
                    response.getId(), response.getUserId(), userName,
                    response.getSubmittedAt(), individualAnswers
            ));
        }

        return result;
    }

    // FormsModuleApi implementations

    @Override
    @Transactional(readOnly = true)
    public List<FormInfo> getPublishedFormsForRoom(UUID roomId) {
        return formRepository.findByScopeAndScopeIdAndStatus(FormScope.ROOM, roomId, FormStatus.PUBLISHED)
                .stream().map(f -> toFormInfo(f, null)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormInfo> getPublishedFormsForSection(UUID sectionId) {
        return formRepository.findPublishedForSection(sectionId)
                .stream().map(f -> toFormInfo(f, null)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormInfo> getPublishedSchoolForms() {
        return formRepository.findByScopeAndStatus(FormScope.SCHOOL, FormStatus.PUBLISHED)
                .stream().map(f -> toFormInfo(f, null)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormInfo> findById(UUID formId, UUID currentUserId) {
        return formRepository.findById(formId)
                .map(f -> toFormInfo(f, currentUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserResponded(UUID formId, UUID userId) {
        var form = formRepository.findById(formId).orElse(null);
        if (form == null) return false;
        if (form.isAnonymous()) {
            return trackingRepository.existsByFormIdAndUserId(formId, userId);
        }
        return responseRepository.existsByFormIdAndUserId(formId, userId);
    }

    // CSV export helper

    public String generateCsv(UUID formId, UUID userId) {
        var form = formRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found"));

        checkResultsPermission(form, userId);

        var questions = questionRepository.findByFormIdOrderBySortOrder(formId);
        var responses = responseRepository.findByFormIdOrderBySubmittedAtDesc(formId);

        var sb = new StringBuilder();

        // Header
        if (!form.isAnonymous()) {
            sb.append("User;");
        }
        sb.append("Submitted;");
        for (var q : questions) {
            sb.append(escapeCsv(q.getLabel())).append(";");
        }
        sb.append("\n");

        // Rows
        for (var response : responses) {
            if (!form.isAnonymous()) {
                var user = response.getUserId() != null
                        ? userModule.findById(response.getUserId()).orElse(null) : null;
                sb.append(escapeCsv(user != null ? user.displayName() : "")).append(";");
            }
            sb.append(response.getSubmittedAt()).append(";");

            var answers = answerRepository.findByResponseId(response.getId());
            var answerMap = answers.stream().collect(Collectors.toMap(FormAnswer::getQuestionId, a -> a));

            for (var q : questions) {
                var answer = answerMap.get(q.getId());
                if (answer != null) {
                    sb.append(escapeCsv(formatAnswer(answer))).append(";");
                } else {
                    sb.append(";");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // PDF export helper

    public String generateResultsHtml(UUID formId, UUID userId) {
        var summary = getResults(formId, userId);
        var form = summary.form();

        var sb = new StringBuilder();
        sb.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head>
                    <style>
                        body { font-family: sans-serif; font-size: 12px; margin: 2cm; }
                        h1 { font-size: 18px; margin-bottom: 0.3cm; }
                        h2 { font-size: 14px; color: #555; margin-bottom: 0.5cm; }
                        h3 { font-size: 13px; margin-top: 0.8cm; margin-bottom: 0.3cm; }
                        .meta { color: #777; margin-bottom: 1cm; font-size: 11px; }
                        .bar-container { background: #eee; height: 20px; margin: 2px 0; position: relative; }
                        .bar { background: #4a90d9; height: 100%%; }
                        .bar-label { font-size: 11px; margin-left: 4px; }
                        .stat { font-size: 12px; margin: 4px 0; }
                        .footer { margin-top: 1cm; font-size: 10px; color: #999; }
                    </style>
                </head>
                <body>
                """);
        sb.append("<h1>").append(escapeXml(form.title())).append("</h1>\n");
        sb.append("<div class=\"meta\">");
        sb.append("Typ: ").append(form.type()).append(" | ");
        sb.append("Antworten: ").append(form.responseCount());
        if (form.targetCount() > 0) {
            sb.append(" / ").append(form.targetCount());
        }
        sb.append("</div>\n");

        for (var result : summary.results()) {
            sb.append("<h3>").append(escapeXml(result.label())).append("</h3>\n");

            switch (result.type()) {
                case SINGLE_CHOICE, MULTIPLE_CHOICE -> {
                    int maxCount = result.optionCounts().values().stream()
                            .mapToInt(Integer::intValue).max().orElse(1);
                    for (var entry : result.optionCounts().entrySet()) {
                        int pct = maxCount > 0 ? (entry.getValue() * 100 / maxCount) : 0;
                        sb.append("<div class=\"bar-container\">");
                        sb.append("<div class=\"bar\" style=\"width: ").append(pct).append("%%;\"></div>");
                        sb.append("</div>");
                        sb.append("<div class=\"bar-label\">").append(escapeXml(entry.getKey()));
                        sb.append(": ").append(entry.getValue()).append("</div>\n");
                    }
                }
                case RATING -> {
                    sb.append("<div class=\"stat\">Durchschnitt: ");
                    sb.append(result.averageRating() != null ? String.format("%.1f", result.averageRating()) : "-");
                    sb.append(" (").append(result.totalAnswers()).append(" Bewertungen)</div>\n");
                    if (result.ratingDistribution() != null) {
                        for (var entry : result.ratingDistribution().entrySet()) {
                            sb.append("<div class=\"bar-label\">").append(entry.getKey()).append(" Sterne: ");
                            sb.append(entry.getValue()).append("</div>\n");
                        }
                    }
                }
                case YES_NO -> {
                    sb.append("<div class=\"stat\">Ja: ").append(result.yesCount());
                    sb.append(" | Nein: ").append(result.noCount()).append("</div>\n");
                }
                case TEXT -> {
                    sb.append("<div class=\"stat\">").append(result.totalAnswers()).append(" Antworten</div>\n");
                    for (var text : result.textAnswers()) {
                        sb.append("<div style=\"margin: 2px 0; padding: 4px; background: #f9f9f9;\">")
                                .append(escapeXml(text)).append("</div>\n");
                    }
                }
            }
        }

        sb.append("<p class=\"footer\">Generiert am ").append(java.time.LocalDate.now()).append("</p>\n");
        sb.append("</body></html>");

        return sb.toString();
    }

    // Private helpers

    private List<FormQuestion> saveQuestions(UUID formId, List<QuestionRequest> requests) {
        if (requests == null || requests.isEmpty()) return List.of();

        var saved = new ArrayList<FormQuestion>();
        for (int i = 0; i < requests.size(); i++) {
            var req = requests.get(i);
            var question = new FormQuestion();
            question.setFormId(formId);
            question.setType(req.type());
            question.setLabel(req.label());
            question.setDescription(req.description());
            question.setRequired(req.required());
            question.setSortOrder(i);

            // Store options as JSONB map
            var optionsMap = new HashMap<String, Object>();
            if (req.options() != null) {
                optionsMap.put("choices", req.options());
            }
            if (req.ratingConfig() != null) {
                optionsMap.putAll(req.ratingConfig());
            }
            if (!optionsMap.isEmpty()) {
                question.setOptions(optionsMap);
            }

            saved.add(questionRepository.save(question));
        }
        return saved;
    }

    private void checkCreatePermission(FormScope scope, UUID scopeId, UUID userId) {
        var user = userModule.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.role() == UserRole.SUPERADMIN) return;

        // ELTERNBEIRAT special role can create forms
        if (hasSpecialRole(user, "ELTERNBEIRAT", scopeId)) {
            return;
        }

        switch (scope) {
            case ROOM -> {
                if (scopeId == null) throw new IllegalArgumentException("Room ID required for ROOM scope");
                var role = roomModule.getUserRoleInRoom(userId, scopeId)
                        .orElseThrow(() -> new IllegalArgumentException("User is not a member of this room"));
                if (role != RoomRole.LEADER) {
                    throw new IllegalArgumentException("Only room leaders can create room forms");
                }
            }
            case SECTION -> {
                if (user.role() != UserRole.TEACHER && user.role() != UserRole.SECTION_ADMIN) {
                    throw new IllegalArgumentException("Only teachers or Elternbeirat can create section forms");
                }
            }
            case SCHOOL -> {
                throw new IllegalArgumentException("Only admins can create school-wide forms");
            }
        }
    }

    private void checkManagePermission(Form form, UUID userId) {
        var user = userModule.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.role() == UserRole.SUPERADMIN) return;
        if (form.getCreatedBy().equals(userId)) return;
        checkCreatePermission(form.getScope(), form.getScopeId(), userId);
    }

    private boolean hasSpecialRole(com.monteweb.user.UserInfo user, String roleName, UUID scopeId) {
        if (user.specialRoles() == null) return false;
        if (user.specialRoles().contains(roleName)) return true;
        return scopeId != null && user.specialRoles().contains(roleName + ":" + scopeId);
    }

    private void checkResultsPermission(Form form, UUID userId) {
        var user = userModule.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.role() == UserRole.SUPERADMIN) return;
        if (form.getCreatedBy().equals(userId)) return;

        // Allow respondents to see results of closed/archived forms
        if (form.getStatus() == FormStatus.CLOSED || form.getStatus() == FormStatus.ARCHIVED) {
            boolean hasResponded = form.isAnonymous()
                ? trackingRepository.existsByFormIdAndUserId(form.getId(), userId)
                : responseRepository.existsByFormIdAndUserId(form.getId(), userId);
            if (hasResponded) return;
        }

        throw new IllegalArgumentException("Only the form creator or an admin can view results");
    }

    private int calculateTargetCount(Form form) {
        if (form.getScope() == FormScope.ROOM && form.getScopeId() != null) {
            return roomModule.getMemberUserIds(form.getScopeId()).size();
        }
        return 0;
    }

    private FormInfo toFormInfo(Form form, UUID currentUserId) {
        var creator = userModule.findById(form.getCreatedBy()).orElse(null);
        String creatorName = creator != null ? creator.displayName() : "Unknown";
        String scopeName = resolveScopeName(form.getScope(), form.getScopeId());

        List<UUID> sectionIdList = form.getSectionIds() != null ? List.of(form.getSectionIds()) : List.of();
        List<String> sectionNameList = sectionIdList.stream()
                .map(id -> schoolModule.findById(id).map(s -> s.name()).orElse(null))
                .filter(Objects::nonNull)
                .toList();

        int questionCount = questionRepository.countByFormId(form.getId());
        int responseCount = responseRepository.countByFormId(form.getId());
        int targetCount = calculateTargetCount(form);

        boolean hasResponded = false;
        if (currentUserId != null) {
            if (form.isAnonymous()) {
                hasResponded = trackingRepository.existsByFormIdAndUserId(form.getId(), currentUserId);
            } else {
                hasResponded = responseRepository.existsByFormIdAndUserId(form.getId(), currentUserId);
            }
        }

        return new FormInfo(
                form.getId(),
                form.getTitle(),
                form.getDescription(),
                form.getType(),
                form.getScope(),
                form.getScopeId(),
                scopeName,
                sectionIdList,
                sectionNameList,
                form.getStatus(),
                form.isAnonymous(),
                form.getDeadline(),
                questionCount,
                responseCount,
                targetCount,
                form.getCreatedBy(),
                creatorName,
                hasResponded,
                form.getCreatedAt(),
                form.getUpdatedAt(),
                form.getPublishedAt(),
                form.getClosedAt()
        );
    }

    private String resolveScopeName(FormScope scope, UUID scopeId) {
        return switch (scope) {
            case ROOM -> scopeId != null ? roomModule.findById(scopeId).map(r -> r.name()).orElse(null) : null;
            case SECTION -> scopeId != null ? schoolModule.findById(scopeId).map(s -> s.name()).orElse(null) : null;
            case SCHOOL -> null;
        };
    }

    private QuestionInfo toQuestionInfo(FormQuestion question) {
        List<String> choices = null;
        Map<String, Object> ratingConfig = null;

        if (question.getOptions() != null) {
            var opts = question.getOptions();
            if (opts.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                var rawChoices = (List<String>) opts.get("choices");
                choices = rawChoices;
            }
            if (opts.containsKey("min") || opts.containsKey("max")) {
                ratingConfig = new HashMap<>();
                if (opts.containsKey("min")) ratingConfig.put("min", opts.get("min"));
                if (opts.containsKey("max")) ratingConfig.put("max", opts.get("max"));
            }
        }

        return new QuestionInfo(
                question.getId(),
                question.getType(),
                question.getLabel(),
                question.getDescription(),
                question.isRequired(),
                question.getSortOrder(),
                choices,
                ratingConfig
        );
    }

    private QuestionResult aggregateQuestionResult(FormQuestion question, List<FormAnswer> answers) {
        Map<String, Integer> optionCounts = null;
        Double averageRating = null;
        Map<Integer, Integer> ratingDistribution = null;
        List<String> textAnswers = null;
        int yesCount = 0;
        int noCount = 0;

        switch (question.getType()) {
            case SINGLE_CHOICE, MULTIPLE_CHOICE -> {
                optionCounts = new LinkedHashMap<>();
                // Initialize with all options
                if (question.getOptions() != null && question.getOptions().containsKey("choices")) {
                    @SuppressWarnings("unchecked")
                    var choices = (List<String>) question.getOptions().get("choices");
                    for (var choice : choices) {
                        optionCounts.put(choice, 0);
                    }
                }
                for (var answer : answers) {
                    if (answer.getAnswerOptions() != null) {
                        for (var opt : answer.getAnswerOptions()) {
                            optionCounts.merge(opt, 1, Integer::sum);
                        }
                    }
                }
            }
            case RATING -> {
                ratingDistribution = new TreeMap<>();
                int sum = 0;
                int count = 0;
                for (var answer : answers) {
                    if (answer.getAnswerRating() != null) {
                        ratingDistribution.merge(answer.getAnswerRating(), 1, Integer::sum);
                        sum += answer.getAnswerRating();
                        count++;
                    }
                }
                averageRating = count > 0 ? (double) sum / count : null;
            }
            case YES_NO -> {
                for (var answer : answers) {
                    if ("yes".equalsIgnoreCase(answer.getAnswerText())) {
                        yesCount++;
                    } else if ("no".equalsIgnoreCase(answer.getAnswerText())) {
                        noCount++;
                    }
                }
            }
            case TEXT -> {
                textAnswers = answers.stream()
                        .map(FormAnswer::getAnswerText)
                        .filter(Objects::nonNull)
                        .filter(t -> !t.isBlank())
                        .toList();
            }
        }

        return new QuestionResult(
                question.getId(),
                question.getLabel(),
                question.getType(),
                answers.size(),
                optionCounts,
                averageRating,
                ratingDistribution,
                textAnswers,
                yesCount,
                noCount
        );
    }

    private String formatAnswer(FormAnswer answer) {
        if (answer.getAnswerText() != null) return answer.getAnswerText();
        if (answer.getAnswerOptions() != null) return String.join(", ", answer.getAnswerOptions());
        if (answer.getAnswerRating() != null) return answer.getAnswerRating().toString();
        return "";
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
