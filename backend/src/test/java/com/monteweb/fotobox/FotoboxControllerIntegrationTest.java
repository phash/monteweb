package com.monteweb.fotobox;

import com.monteweb.TestContainerConfig;
import com.monteweb.TestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
class FotoboxControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String createRoomAndGetId(String token) throws Exception {
        var result = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Fotobox Room", "type": "GRUPPE"}
                                """))
                .andReturn();
        return TestHelper.parseResponse(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    private void enableFotobox(String token, String roomId) throws Exception {
        mockMvc.perform(put("/api/v1/rooms/" + roomId + "/fotobox/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled": true, "defaultPermission": "CREATE_THREADS"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void getSettings_unauthenticated_shouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/00000000-0000-0000-0000-000000000001/fotobox/settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSettings_authenticated_shouldReturn() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-settings@example.com", "Foto", "Settings");
        String roomId = createRoomAndGetId(token);

        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/fotobox/settings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void updateSettings_asLeader_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-update@example.com", "Foto", "Update");
        String roomId = createRoomAndGetId(token);

        mockMvc.perform(put("/api/v1/rooms/" + roomId + "/fotobox/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled": true, "defaultPermission": "POST_IMAGES", "maxFileSizeMb": 5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.defaultPermission").value("POST_IMAGES"))
                .andExpect(jsonPath("$.data.maxFileSizeMb").value(5));
    }

    @Test
    void getThreads_withFotoboxEnabled_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-threads@example.com", "Foto", "Threads");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/fotobox/threads")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void createThread_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-create@example.com", "Foto", "Creator");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/fotobox/threads")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Summer Party", "description": "Photos from the summer party"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Summer Party"))
                .andExpect(jsonPath("$.data.description").value("Photos from the summer party"))
                .andExpect(jsonPath("$.data.imageCount").value(0));
    }

    @Test
    void getThread_shouldReturnThread() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-getthread@example.com", "Foto", "GetThread");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        var threadResult = mockMvc.perform(post("/api/v1/rooms/" + roomId + "/fotobox/threads")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Thread to Get"}
                                """))
                .andReturn();
        String threadId = TestHelper.parseResponse(threadResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Thread to Get"));
    }

    @Test
    void deleteThread_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-delete@example.com", "Foto", "Delete");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        var threadResult = mockMvc.perform(post("/api/v1/rooms/" + roomId + "/fotobox/threads")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Thread to Delete"}
                                """))
                .andReturn();
        String threadId = TestHelper.parseResponse(threadResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(delete("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void uploadImages_shouldUploadFile() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-upload@example.com", "Foto", "Upload");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        var threadResult = mockMvc.perform(post("/api/v1/rooms/" + roomId + "/fotobox/threads")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Upload Thread"}
                                """))
                .andReturn();
        String threadId = TestHelper.parseResponse(threadResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Create a minimal JPEG (just magic bytes for validation)
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", jpegBytes);

        mockMvc.perform(multipart("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].originalFilename").value("test.jpg"));
    }
}
