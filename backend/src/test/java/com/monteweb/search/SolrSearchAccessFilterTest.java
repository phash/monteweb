package com.monteweb.search;

import com.monteweb.room.RoomInfo;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.search.internal.service.SolrSearchService;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for {@link SolrSearchService#buildAccessFilter(UUID)} — the global-search access
 * boundary. Validates that the generated Solr filter constrains room/parentOnly POST docs,
 * scoped EVENT docs, and audience-restricted FILE docs (regression guard for the search-authz
 * leak). The method is private, so it is invoked via reflection.
 */
@ExtendWith(MockitoExtension.class)
class SolrSearchAccessFilterTest {

    @Mock
    RoomModuleApi roomModuleApi;
    @Mock
    UserModuleApi userModuleApi;

    private static Method buildAccessFilterMethod() throws Exception {
        Method m = SolrSearchService.class.getDeclaredMethod("buildAccessFilter", UUID.class);
        m.setAccessible(true);
        return m;
    }

    private String buildFilter(UUID userId) throws Exception {
        SolrSearchService service = new SolrSearchService(null, roomModuleApi, userModuleApi);
        return (String) buildAccessFilterMethod().invoke(service, userId);
    }

    private RoomInfo room(UUID id, UUID sectionId) {
        return new RoomInfo(id, "Room", "desc", null, null, "CLASS", sectionId,
                false, 1, "OPEN", null, List.of(), null);
    }

    private UserInfo user(UUID id, UserRole role) {
        return new UserInfo(id, "u@x.de", "F", "L", "F L", null, null, role,
                Set.of(), Set.of(), true, null);
    }

    @Test
    void anonymousUser_onlyPublicAndSchoolEvents() throws Exception {
        String fq = buildFilter(null);
        // Public docs allowed
        assertTrue(fq.contains("doc_type:USER OR doc_type:ROOM"));
        // No room IDs => files/wiki/tasks pinned to a non-matching clause
        assertTrue(fq.contains("doc_type:FILE AND id:__none__"));
        // Events restricted to SCHOOL scope only
        assertTrue(fq.contains("doc_type:EVENT AND (scope:SCHOOL)"));
    }

    @Test
    void student_parentOnlyPostsExcluded() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        lenient().when(roomModuleApi.findByUserId(userId)).thenReturn(List.of(room(roomId, null)));
        lenient().when(userModuleApi.findById(userId)).thenReturn(Optional.of(user(userId, UserRole.STUDENT)));

        String fq = buildFilter(userId);

        // parentOnly posts must be excluded for students
        assertTrue(fq.contains("-parent_only:true"), "student filter must exclude parent_only posts");
        // ROOM-scoped posts constrained to the user's room
        assertTrue(fq.contains("room_id:(\"" + roomId + "\")"));
        // Students only see ALL + STUDENTS_ONLY files
        assertTrue(fq.contains("audience:(") && fq.contains("STUDENTS_ONLY"));
        assertFalse(fq.contains("PARENTS_ONLY"), "students must not be able to see PARENTS_ONLY files");
    }

    @Test
    void parent_parentOnlyPostsNotExcluded_andParentAudiences() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        lenient().when(roomModuleApi.findByUserId(userId)).thenReturn(List.of(room(roomId, null)));
        lenient().when(userModuleApi.findById(userId)).thenReturn(Optional.of(user(userId, UserRole.PARENT)));

        String fq = buildFilter(userId);

        assertFalse(fq.contains("-parent_only:true"), "parents see parentOnly posts");
        assertTrue(fq.contains("PARENTS_ONLY"));
        assertFalse(fq.contains("STUDENTS_ONLY"), "parents must not see STUDENTS_ONLY files");
    }

    @Test
    void teacher_seesAllAudiences() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        lenient().when(roomModuleApi.findByUserId(userId)).thenReturn(List.of(room(roomId, null)));
        lenient().when(userModuleApi.findById(userId)).thenReturn(Optional.of(user(userId, UserRole.TEACHER)));

        String fq = buildFilter(userId);

        assertTrue(fq.contains("ALL"));
        assertTrue(fq.contains("PARENTS_ONLY"));
        assertTrue(fq.contains("STUDENTS_ONLY"));
        assertFalse(fq.contains("-parent_only:true"));
    }

    @Test
    void sectionScopedEvents_constrainedToUserSections() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        lenient().when(roomModuleApi.findByUserId(userId)).thenReturn(List.of(room(roomId, sectionId)));
        lenient().when(userModuleApi.findById(userId)).thenReturn(Optional.of(user(userId, UserRole.PARENT)));

        String fq = buildFilter(userId);

        assertTrue(fq.contains("scope:SECTION AND section_id:(\"" + sectionId + "\")"));
        assertTrue(fq.contains("scope:ROOM AND room_id:(\"" + roomId + "\")"));
        assertTrue(fq.contains("scope:SCHOOL"));
    }
}
