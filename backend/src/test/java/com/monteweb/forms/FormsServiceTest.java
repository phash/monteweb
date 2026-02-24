package com.monteweb.forms;

import com.monteweb.forms.internal.model.*;
import com.monteweb.forms.internal.repository.*;
import com.monteweb.forms.internal.service.FormsService;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.school.SchoolModuleApi;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FormsService")
class FormsServiceTest {

    @Mock private FormRepository formRepository;
    @Mock private FormQuestionRepository questionRepository;
    @Mock private FormResponseRepository responseRepository;
    @Mock private FormAnswerRepository answerRepository;
    @Mock private FormResponseTrackingRepository trackingRepository;
    @Mock private RoomModuleApi roomModule;
    @Mock private SchoolModuleApi schoolModule;
    @Mock private UserModuleApi userModule;
    @Mock private ApplicationEventPublisher eventPublisher;

    private FormsService service;

    private static final UUID FORM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID QUESTION_ID_1 = UUID.randomUUID();
    private static final UUID QUESTION_ID_2 = UUID.randomUUID();
    private static final UUID RESPONSE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new FormsService(
                formRepository, questionRepository, responseRepository,
                answerRepository, trackingRepository,
                roomModule, schoolModule, userModule, eventPublisher
        );
    }

    // ── Helper methods ──────────────────────────────────────────────────

    private Form makeForm(FormStatus status, boolean anonymous, LocalDate deadline) {
        var form = new Form();
        form.setId(FORM_ID);
        form.setTitle("Test Survey");
        form.setDescription("A test survey");
        form.setType(FormType.SURVEY);
        form.setScope(FormScope.SCHOOL);
        form.setScopeId(null);
        form.setStatus(status);
        form.setAnonymous(anonymous);
        form.setDeadline(deadline);
        form.setCreatedBy(USER_ID);
        form.setCreatedAt(Instant.now());
        form.setUpdatedAt(Instant.now());
        return form;
    }

    private FormQuestion makeQuestion(UUID id, QuestionType type, String label, boolean required) {
        var q = new FormQuestion();
        q.setId(id);
        q.setFormId(FORM_ID);
        q.setType(type);
        q.setLabel(label);
        q.setRequired(required);
        q.setSortOrder(0);
        if (type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTIPLE_CHOICE) {
            q.setOptions(Map.of("choices", List.of("Option A", "Option B", "Option C")));
        }
        return q;
    }

    private FormAnswer makeAnswer(UUID questionId, String text, List<String> options, Integer rating) {
        var a = new FormAnswer();
        a.setId(UUID.randomUUID());
        a.setResponseId(RESPONSE_ID);
        a.setQuestionId(questionId);
        a.setAnswerText(text);
        a.setAnswerOptions(options);
        a.setAnswerRating(rating);
        return a;
    }

    private UserInfo makeUser(UUID id, UserRole role) {
        return new UserInfo(id, "test@monteweb.local", "Test", "User", "Test User",
                null, null, role, Set.of(), Set.of(), true, "SYSTEM");
    }

    private SubmitResponseRequest makeSubmitRequest(UUID questionId, String text) {
        return new SubmitResponseRequest(List.of(
                new AnswerRequest(questionId, text, null, null)
        ));
    }

    /**
     * Stubs the mocks required by {@code toFormInfo} which is called internally
     * by {@code getResults} and other public methods.
     */
    private void stubToFormInfo(Form form) {
        lenient().when(userModule.findById(form.getCreatedBy()))
                .thenReturn(Optional.of(makeUser(form.getCreatedBy(), UserRole.TEACHER)));
        lenient().when(questionRepository.countByFormId(form.getId())).thenReturn(1);
        lenient().when(responseRepository.countByFormId(form.getId())).thenReturn(0);
        // hasUserResponded path (non-anonymous default)
        lenient().when(responseRepository.existsByFormIdAndUserId(eq(form.getId()), any()))
                .thenReturn(false);
        lenient().when(trackingRepository.existsByFormIdAndUserId(eq(form.getId()), any()))
                .thenReturn(false);
    }

    // ── Submit Response ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Submit Response")
    class SubmitResponse {

        @Test
        @DisplayName("should throw when form is not published (DRAFT)")
        void submitResponse_notPublishedThrows() {
            var form = makeForm(FormStatus.DRAFT, false, null);
            when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

            assertThatThrownBy(() -> service.submitResponse(FORM_ID, makeSubmitRequest(QUESTION_ID_1, "answer"), USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not accepting responses");
        }

        @Test
        @DisplayName("should throw when deadline has passed")
        void submitResponse_pastDeadlineThrows() {
            var form = makeForm(FormStatus.PUBLISHED, false, LocalDate.now().minusDays(1));
            when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

            assertThatThrownBy(() -> service.submitResponse(FORM_ID, makeSubmitRequest(QUESTION_ID_1, "answer"), USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("deadline has passed");
        }

        @Test
        @DisplayName("should throw when non-anonymous user has already responded")
        void submitResponse_duplicateNonAnonymousThrows() {
            var form = makeForm(FormStatus.PUBLISHED, false, null);
            when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));
            when(responseRepository.existsByFormIdAndUserId(FORM_ID, USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.submitResponse(FORM_ID, makeSubmitRequest(QUESTION_ID_1, "answer"), USER_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already responded");
        }

        @Test
        @DisplayName("should throw when required question is not answered")
        void submitResponse_missingRequiredThrows() {
            var form = makeForm(FormStatus.PUBLISHED, false, null);
            when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));
            when(responseRepository.existsByFormIdAndUserId(FORM_ID, USER_ID)).thenReturn(false);

            var requiredQuestion = makeQuestion(QUESTION_ID_1, QuestionType.TEXT, "Required Q", true);
            var otherQuestion = makeQuestion(QUESTION_ID_2, QuestionType.TEXT, "Other Q", false);
            when(questionRepository.findByFormIdOrderBySortOrder(FORM_ID))
                    .thenReturn(List.of(requiredQuestion, otherQuestion));

            // Submit answer only for QUESTION_ID_2, skipping the required QUESTION_ID_1
            var request = new SubmitResponseRequest(List.of(
                    new AnswerRequest(QUESTION_ID_2, "some text", null, null)
            ));

            assertThatThrownBy(() -> service.submitResponse(FORM_ID, request, USER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Required question not answered");
        }

        @Test
        @DisplayName("should save response and answers on valid submission")
        void submitResponse_success() {
            var form = makeForm(FormStatus.PUBLISHED, false, null);
            when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));
            when(responseRepository.existsByFormIdAndUserId(FORM_ID, USER_ID)).thenReturn(false);

            var question = makeQuestion(QUESTION_ID_1, QuestionType.TEXT, "Tell us", true);
            when(questionRepository.findByFormIdOrderBySortOrder(FORM_ID))
                    .thenReturn(List.of(question));

            // responseRepository.save should return the response with an ID assigned
            when(responseRepository.save(any(FormResponse.class))).thenAnswer(invocation -> {
                FormResponse resp = invocation.getArgument(0);
                resp.setId(RESPONSE_ID);
                return resp;
            });
            when(answerRepository.save(any(FormAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            var request = makeSubmitRequest(QUESTION_ID_1, "My answer");

            assertThatCode(() -> service.submitResponse(FORM_ID, request, USER_ID))
                    .doesNotThrowAnyException();

            verify(responseRepository).save(argThat(r ->
                    r.getFormId().equals(FORM_ID) && USER_ID.equals(r.getUserId())));
            verify(answerRepository).save(argThat(a ->
                    a.getQuestionId().equals(QUESTION_ID_1) && "My answer".equals(a.getAnswerText())));
            // Non-anonymous: no tracking entry saved
            verify(trackingRepository, never()).save(any());
        }

        @Test
        @DisplayName("should save tracking entry for anonymous form submissions")
        void submitResponse_anonymousTracking() {
            var form = makeForm(FormStatus.PUBLISHED, true, null);
            when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));
            when(trackingRepository.existsByFormIdAndUserId(FORM_ID, USER_ID)).thenReturn(false);

            var question = makeQuestion(QUESTION_ID_1, QuestionType.TEXT, "Feedback", false);
            when(questionRepository.findByFormIdOrderBySortOrder(FORM_ID))
                    .thenReturn(List.of(question));

            when(responseRepository.save(any(FormResponse.class))).thenAnswer(invocation -> {
                FormResponse resp = invocation.getArgument(0);
                resp.setId(RESPONSE_ID);
                return resp;
            });
            when(answerRepository.save(any(FormAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            var request = makeSubmitRequest(QUESTION_ID_1, "Anonymous feedback");

            service.submitResponse(FORM_ID, request, USER_ID);

            // Anonymous: userId on response should be null
            verify(responseRepository).save(argThat(r ->
                    r.getFormId().equals(FORM_ID) && r.getUserId() == null));
            // Tracking entry saved for dedup
            verify(trackingRepository).save(argThat(t ->
                    t.getFormId().equals(FORM_ID) && t.getUserId().equals(USER_ID)));
        }
    }

    // ── Results Aggregation ─────────────────────────────────────────────

    @Nested
    @DisplayName("Results Aggregation")
    class ResultsAggregation {

        @Test
        @DisplayName("should count single-choice answers per option")
        void getResults_singleChoiceCounts() {
            var form = makeForm(FormStatus.PUBLISHED, false, null);
            when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

            var admin = makeUser(USER_ID, UserRole.SUPERADMIN);
            when(userModule.findById(USER_ID)).thenReturn(Optional.of(admin));

            stubToFormInfo(form);

            var question = makeQuestion(QUESTION_ID_1, QuestionType.SINGLE_CHOICE, "Favorite?", false);
            when(questionRepository.findByFormIdOrderBySortOrder(FORM_ID))
                    .thenReturn(List.of(question));

            var answer1 = makeAnswer(QUESTION_ID_1, null, List.of("Option A"), null);
            var answer2 = makeAnswer(QUESTION_ID_1, null, List.of("Option B"), null);
            var answer3 = makeAnswer(QUESTION_ID_1, null, List.of("Option A"), null);
            when(answerRepository.findByQuestionId(QUESTION_ID_1))
                    .thenReturn(List.of(answer1, answer2, answer3));

            var summary = service.getResults(FORM_ID, USER_ID);

            assertThat(summary.results()).hasSize(1);
            var result = summary.results().get(0);
            assertThat(result.type()).isEqualTo(QuestionType.SINGLE_CHOICE);
            assertThat(result.totalAnswers()).isEqualTo(3);
            assertThat(result.optionCounts())
                    .containsEntry("Option A", 2)
                    .containsEntry("Option B", 1)
                    .containsEntry("Option C", 0);
        }

        @Test
        @DisplayName("should calculate rating average and distribution")
        void getResults_ratingAverage() {
            var form = makeForm(FormStatus.PUBLISHED, false, null);
            when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

            var admin = makeUser(USER_ID, UserRole.SUPERADMIN);
            when(userModule.findById(USER_ID)).thenReturn(Optional.of(admin));

            stubToFormInfo(form);

            var question = makeQuestion(QUESTION_ID_1, QuestionType.RATING, "Rate us", false);
            when(questionRepository.findByFormIdOrderBySortOrder(FORM_ID))
                    .thenReturn(List.of(question));

            var answer1 = makeAnswer(QUESTION_ID_1, null, null, 4);
            var answer2 = makeAnswer(QUESTION_ID_1, null, null, 5);
            var answer3 = makeAnswer(QUESTION_ID_1, null, null, 3);
            when(answerRepository.findByQuestionId(QUESTION_ID_1))
                    .thenReturn(List.of(answer1, answer2, answer3));

            var summary = service.getResults(FORM_ID, USER_ID);

            assertThat(summary.results()).hasSize(1);
            var result = summary.results().get(0);
            assertThat(result.type()).isEqualTo(QuestionType.RATING);
            assertThat(result.averageRating()).isEqualTo(4.0); // (4+5+3)/3 = 4.0
            assertThat(result.ratingDistribution())
                    .containsEntry(3, 1)
                    .containsEntry(4, 1)
                    .containsEntry(5, 1);
        }

        @Test
        @DisplayName("should count yes and no answers for YES_NO questions")
        void getResults_yesNoCounts() {
            var form = makeForm(FormStatus.PUBLISHED, false, null);
            when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

            var admin = makeUser(USER_ID, UserRole.SUPERADMIN);
            when(userModule.findById(USER_ID)).thenReturn(Optional.of(admin));

            stubToFormInfo(form);

            var question = makeQuestion(QUESTION_ID_1, QuestionType.YES_NO, "Agree?", false);
            when(questionRepository.findByFormIdOrderBySortOrder(FORM_ID))
                    .thenReturn(List.of(question));

            var answer1 = makeAnswer(QUESTION_ID_1, "yes", null, null);
            var answer2 = makeAnswer(QUESTION_ID_1, "no", null, null);
            var answer3 = makeAnswer(QUESTION_ID_1, "yes", null, null);
            var answer4 = makeAnswer(QUESTION_ID_1, "yes", null, null);
            when(answerRepository.findByQuestionId(QUESTION_ID_1))
                    .thenReturn(List.of(answer1, answer2, answer3, answer4));

            var summary = service.getResults(FORM_ID, USER_ID);

            assertThat(summary.results()).hasSize(1);
            var result = summary.results().get(0);
            assertThat(result.type()).isEqualTo(QuestionType.YES_NO);
            assertThat(result.yesCount()).isEqualTo(3);
            assertThat(result.noCount()).isEqualTo(1);
        }
    }

    // ── Results Permissions ─────────────────────────────────────────────

    @Nested
    @DisplayName("Results Permissions")
    class ResultsPermissions {

        @Test
        @DisplayName("should allow SUPERADMIN to view results")
        void getResults_superadminAllowed() {
            var creatorId = UUID.randomUUID();
            var form = makeForm(FormStatus.PUBLISHED, false, null);
            form.setCreatedBy(creatorId);
            when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

            var admin = makeUser(USER_ID, UserRole.SUPERADMIN);
            when(userModule.findById(USER_ID)).thenReturn(Optional.of(admin));

            stubToFormInfo(form);
            // Override createdBy lookup for toFormInfo
            lenient().when(userModule.findById(creatorId))
                    .thenReturn(Optional.of(makeUser(creatorId, UserRole.TEACHER)));

            when(questionRepository.findByFormIdOrderBySortOrder(FORM_ID))
                    .thenReturn(List.of());

            assertThatCode(() -> service.getResults(FORM_ID, USER_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should allow creator to view results")
        void getResults_creatorAllowed() {
            var form = makeForm(FormStatus.PUBLISHED, false, null);
            // createdBy is already USER_ID
            when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

            var teacher = makeUser(USER_ID, UserRole.TEACHER);
            when(userModule.findById(USER_ID)).thenReturn(Optional.of(teacher));

            stubToFormInfo(form);

            when(questionRepository.findByFormIdOrderBySortOrder(FORM_ID))
                    .thenReturn(List.of());

            assertThatCode(() -> service.getResults(FORM_ID, USER_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should allow respondent to view results of closed form")
        void getResults_respondentAfterCloseAllowed() {
            var creatorId = UUID.randomUUID();
            var form = makeForm(FormStatus.CLOSED, false, null);
            form.setCreatedBy(creatorId);
            form.setClosedAt(Instant.now());
            when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

            // The requesting user is a PARENT (not admin, not creator)
            var parent = makeUser(USER_ID, UserRole.PARENT);
            when(userModule.findById(USER_ID)).thenReturn(Optional.of(parent));
            lenient().when(userModule.findById(creatorId))
                    .thenReturn(Optional.of(makeUser(creatorId, UserRole.TEACHER)));

            // User has responded to this form — must be set after stubToFormInfo
            // to take priority over the lenient false stub
            stubToFormInfo(form);
            when(responseRepository.existsByFormIdAndUserId(FORM_ID, USER_ID)).thenReturn(true);

            when(questionRepository.findByFormIdOrderBySortOrder(FORM_ID))
                    .thenReturn(List.of());

            assertThatCode(() -> service.getResults(FORM_ID, USER_ID))
                    .doesNotThrowAnyException();
        }
    }
}
