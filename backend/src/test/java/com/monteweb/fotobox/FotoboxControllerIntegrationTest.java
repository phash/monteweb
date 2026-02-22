package com.monteweb.fotobox;

import com.monteweb.TestContainerConfig;
import com.monteweb.TestHelper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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

    private void enableFotoboxWithPermission(String token, String roomId, String permission) throws Exception {
        mockMvc.perform(put("/api/v1/rooms/" + roomId + "/fotobox/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled": true, "defaultPermission": "%s"}
                                """.formatted(permission)))
                .andExpect(status().isOk());
    }

    private String createThreadAndGetId(String token, String roomId, String title) throws Exception {
        var result = mockMvc.perform(post("/api/v1/rooms/" + roomId + "/fotobox/threads")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "%s", "description": "A test thread"}
                                """.formatted(title)))
                .andReturn();
        return TestHelper.parseResponse(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    private byte[] createMinimalJpeg() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    }

    private void addMemberToRoom(String leaderToken, String roomId, String memberId) throws Exception {
        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/members")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": \"%s\", \"role\": \"MEMBER\"}".formatted(memberId)))
                .andExpect(status().isOk());
    }

    // ==================== Settings Tests ====================

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
    void getSettings_defaultValues_shouldReturn() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-defaults@example.com", "Foto", "Defaults");
        String roomId = createRoomAndGetId(token);

        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/fotobox/settings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.defaultPermission").value("VIEW_ONLY"))
                .andExpect(jsonPath("$.data.maxFileSizeMb").value(10));
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
    void updateSettings_partialUpdate_shouldPreserveOtherValues() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-partial@example.com", "Foto", "Partial");
        String roomId = createRoomAndGetId(token);

        // First set full settings
        mockMvc.perform(put("/api/v1/rooms/" + roomId + "/fotobox/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled": true, "defaultPermission": "CREATE_THREADS", "maxFileSizeMb": 20}
                                """))
                .andExpect(status().isOk());

        // Then partial update — only change enabled
        mockMvc.perform(put("/api/v1/rooms/" + roomId + "/fotobox/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled": false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.defaultPermission").value("CREATE_THREADS"))
                .andExpect(jsonPath("$.data.maxFileSizeMb").value(20));
    }

    @Test
    void updateSettings_maxImagesZero_shouldSetNull() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-maxi0@example.com", "Foto", "MaxI0");
        String roomId = createRoomAndGetId(token);

        mockMvc.perform(put("/api/v1/rooms/" + roomId + "/fotobox/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"maxImagesPerThread": 0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maxImagesPerThread").doesNotExist());
    }

    // ==================== Thread Tests ====================

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
    void getThreads_fotoboxNotEnabled_shouldFail() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-notenabled@example.com", "Foto", "NotEnabled");
        String roomId = createRoomAndGetId(token);

        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/fotobox/threads")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
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
    void createThread_withoutDescription_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-nodesc@example.com", "Foto", "NoDesc");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/fotobox/threads")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Only Title"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Only Title"))
                .andExpect(jsonPath("$.data.description").doesNotExist());
    }

    @Test
    void createThread_blankTitle_shouldFail() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-blanktitle@example.com", "Foto", "BlankTitle");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/fotobox/threads")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": ""}
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getThread_shouldReturnThread() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-getthread@example.com", "Foto", "GetThread");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        String threadId = createThreadAndGetId(token, roomId, "Thread to Get");

        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Thread to Get"))
                .andExpect(jsonPath("$.data.imageCount").value(0))
                .andExpect(jsonPath("$.data.createdByName").isNotEmpty());
    }

    @Test
    void getThread_wrongRoom_shouldFail() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-wrongroom@example.com", "Foto", "WrongRoom");
        String roomId1 = createRoomAndGetId(token);
        String roomId2 = createRoomAndGetId(token);
        enableFotobox(token, roomId1);
        enableFotobox(token, roomId2);

        String threadId = createThreadAndGetId(token, roomId1, "Room 1 Thread");

        // Try to get thread from room1 via room2 endpoint
        mockMvc.perform(get("/api/v1/rooms/" + roomId2 + "/fotobox/threads/" + threadId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateThread_asOwner_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-updatethread@example.com", "Foto", "UpdateThread");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        String threadId = createThreadAndGetId(token, roomId, "Original Title");

        mockMvc.perform(put("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Updated Title"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Title"));
    }

    @Test
    void deleteThread_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-delete@example.com", "Foto", "Delete");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        String threadId = createThreadAndGetId(token, roomId, "Thread to Delete");

        mockMvc.perform(delete("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Verify it's gone
        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteThread_nonExistent_shouldFail() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-delnone@example.com", "Foto", "DelNone");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        mockMvc.perform(delete("/api/v1/rooms/" + roomId + "/fotobox/threads/00000000-0000-0000-0000-000000000099")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void createMultipleThreads_shouldAllAppear() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-multi@example.com", "Foto", "Multi");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        createThreadAndGetId(token, roomId, "Thread A");
        createThreadAndGetId(token, roomId, "Thread B");
        createThreadAndGetId(token, roomId, "Thread C");

        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/fotobox/threads")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    // ==================== Image Upload Tests ====================

    @Disabled("Requires MinIO")
    @Test
    void uploadImages_shouldUploadFile() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-upload@example.com", "Foto", "Upload");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        String threadId = createThreadAndGetId(token, roomId, "Upload Thread");

        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", createMinimalJpeg());

        mockMvc.perform(multipart("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].originalFilename").value("test.jpg"));
    }

    @Disabled("Requires MinIO")
    @Test
    void uploadImages_withCaption_shouldStoreCaption() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-caption@example.com", "Foto", "Caption");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        String threadId = createThreadAndGetId(token, roomId, "Caption Thread");

        MockMultipartFile file = new MockMultipartFile(
                "files", "photo.jpg", "image/jpeg", createMinimalJpeg());

        mockMvc.perform(multipart("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId + "/images")
                        .file(file)
                        .param("caption", "Beautiful sunset")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].caption").value("Beautiful sunset"));
    }

    @Disabled("Requires MinIO")
    @Test
    void uploadImages_shouldSetCoverImage() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-cover@example.com", "Foto", "Cover");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        String threadId = createThreadAndGetId(token, roomId, "Cover Thread");

        MockMultipartFile file = new MockMultipartFile(
                "files", "cover.jpg", "image/jpeg", createMinimalJpeg());

        mockMvc.perform(multipart("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Verify thread now has cover image
        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coverImageId").isNotEmpty())
                .andExpect(jsonPath("$.data.coverImageThumbnailUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.imageCount").value(1));
    }

    @Disabled("Requires MinIO")
    @Test
    void uploadImages_multipleFiles_shouldUploadAll() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-multiup@example.com", "Foto", "MultiUp");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        String threadId = createThreadAndGetId(token, roomId, "Multi Upload Thread");

        MockMultipartFile file1 = new MockMultipartFile(
                "files", "img1.jpg", "image/jpeg", createMinimalJpeg());
        MockMultipartFile file2 = new MockMultipartFile(
                "files", "img2.jpg", "image/jpeg", createMinimalJpeg());

        mockMvc.perform(multipart("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId + "/images")
                        .file(file1)
                        .file(file2)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].originalFilename").value("img1.jpg"))
                .andExpect(jsonPath("$.data[1].originalFilename").value("img2.jpg"));
    }

    @Disabled("Requires MinIO")
    @Test
    void getThreadImages_shouldReturnImages() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-getimgs@example.com", "Foto", "GetImgs");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        String threadId = createThreadAndGetId(token, roomId, "Images Thread");

        MockMultipartFile file = new MockMultipartFile(
                "files", "photo.jpg", "image/jpeg", createMinimalJpeg());

        mockMvc.perform(multipart("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId + "/images")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].imageUrl").isNotEmpty())
                .andExpect(jsonPath("$.data[0].thumbnailUrl").isNotEmpty());
    }

    @Test
    void getThreadImages_emptyThread_shouldReturnEmpty() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-emptyimgs@example.com", "Foto", "EmptyImgs");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        String threadId = createThreadAndGetId(token, roomId, "Empty Thread");

        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId + "/images")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ==================== Image Deletion Tests ====================

    @Disabled("Requires MinIO")
    @Test
    void deleteImage_asUploader_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-delimg@example.com", "Foto", "DelImg");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        String threadId = createThreadAndGetId(token, roomId, "Del Image Thread");

        MockMultipartFile file = new MockMultipartFile(
                "files", "delete-me.jpg", "image/jpeg", createMinimalJpeg());

        var uploadResult = mockMvc.perform(multipart("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        String imageId = TestHelper.parseResponse(uploadResult.getResponse().getContentAsString())
                .path("data").path(0).path("id").asText();

        mockMvc.perform(delete("/api/v1/fotobox/images/" + imageId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Verify thread image count is 0
        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageCount").value(0));
    }

    @Disabled("Requires MinIO")
    @Test
    void deleteImage_coverImage_shouldClearCover() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-delcover@example.com", "Foto", "DelCover");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        String threadId = createThreadAndGetId(token, roomId, "Cover Delete Thread");

        MockMultipartFile file = new MockMultipartFile(
                "files", "cover-del.jpg", "image/jpeg", createMinimalJpeg());

        var uploadResult = mockMvc.perform(multipart("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        String imageId = TestHelper.parseResponse(uploadResult.getResponse().getContentAsString())
                .path("data").path(0).path("id").asText();

        // Verify cover is set
        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.coverImageId").value(imageId));

        // Delete the cover image
        mockMvc.perform(delete("/api/v1/fotobox/images/" + imageId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Verify cover is cleared
        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coverImageId").isEmpty());
    }

    // ==================== Image Update Tests ====================

    @Disabled("Requires MinIO")
    @Test
    void updateImage_caption_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-updimg@example.com", "Foto", "UpdImg");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        String threadId = createThreadAndGetId(token, roomId, "Upd Image Thread");

        MockMultipartFile file = new MockMultipartFile(
                "files", "update-me.jpg", "image/jpeg", createMinimalJpeg());

        var uploadResult = mockMvc.perform(multipart("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        String imageId = TestHelper.parseResponse(uploadResult.getResponse().getContentAsString())
                .path("data").path(0).path("id").asText();

        mockMvc.perform(put("/api/v1/fotobox/images/" + imageId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"caption": "New caption", "sortOrder": 5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.caption").value("New caption"))
                .andExpect(jsonPath("$.data.sortOrder").value(5));
    }

    // ==================== Thread Deletion with Images ====================

    @Disabled("Requires MinIO")
    @Test
    void deleteThread_withImages_shouldDeleteAll() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-delcasc@example.com", "Foto", "DelCasc");
        String roomId = createRoomAndGetId(token);
        enableFotobox(token, roomId);

        String threadId = createThreadAndGetId(token, roomId, "Cascade Delete Thread");

        MockMultipartFile file1 = new MockMultipartFile(
                "files", "cas1.jpg", "image/jpeg", createMinimalJpeg());
        MockMultipartFile file2 = new MockMultipartFile(
                "files", "cas2.jpg", "image/jpeg", createMinimalJpeg());

        mockMvc.perform(multipart("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId + "/images")
                        .file(file1)
                        .file(file2)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Delete thread
        mockMvc.perform(delete("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Thread and images should be gone
        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ==================== Permission Tests ====================

    @Test
    void createThread_withViewOnlyPermission_shouldFail() throws Exception {
        String leaderToken = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-leader-perm@example.com", "Foto", "LeaderPerm");
        String roomId = createRoomAndGetId(leaderToken);
        enableFotoboxWithPermission(leaderToken, roomId, "VIEW_ONLY");

        // Register a second user and add to room
        var memberResponse = TestHelper.registerAndGetResponse(mockMvc,
                "fotobox-viewonly@example.com", "Foto", "ViewOnly");
        String memberToken = memberResponse.path("data").path("accessToken").asText();
        String memberId = memberResponse.path("data").path("userId").asText();
        addMemberToRoom(leaderToken, roomId, memberId);

        // Member with VIEW_ONLY should not be able to create threads
        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/fotobox/threads")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Should Fail"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadImages_withViewOnlyPermission_shouldFail() throws Exception {
        String leaderToken = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-leader-upl@example.com", "Foto", "LeaderUpl");
        String roomId = createRoomAndGetId(leaderToken);
        enableFotoboxWithPermission(leaderToken, roomId, "VIEW_ONLY");

        String threadId = createThreadAndGetId(leaderToken, roomId, "Perm Thread");

        // Register a second user and add to room
        var memberResponse = TestHelper.registerAndGetResponse(mockMvc,
                "fotobox-viewonly-upl@example.com", "Foto", "ViewOnlyUpl");
        String memberToken = memberResponse.path("data").path("accessToken").asText();
        String memberId = memberResponse.path("data").path("userId").asText();
        addMemberToRoom(leaderToken, roomId, memberId);

        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", createMinimalJpeg());

        // Member with VIEW_ONLY should not be able to upload
        mockMvc.perform(multipart("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Disabled("Requires MinIO")
    @Test
    void uploadImages_withPostImagesPermission_shouldSucceed() throws Exception {
        String leaderToken = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-leader-post@example.com", "Foto", "LeaderPost");
        String roomId = createRoomAndGetId(leaderToken);
        enableFotoboxWithPermission(leaderToken, roomId, "POST_IMAGES");

        String threadId = createThreadAndGetId(leaderToken, roomId, "Post Perm Thread");

        // Register a second user and add to room
        var memberResponse = TestHelper.registerAndGetResponse(mockMvc,
                "fotobox-poster@example.com", "Foto", "Poster");
        String memberToken = memberResponse.path("data").path("accessToken").asText();
        String memberId = memberResponse.path("data").path("userId").asText();
        addMemberToRoom(leaderToken, roomId, memberId);

        MockMultipartFile file = new MockMultipartFile(
                "files", "post.jpg", "image/jpeg", createMinimalJpeg());

        // Member with POST_IMAGES should be able to upload
        mockMvc.perform(multipart("/api/v1/rooms/" + roomId + "/fotobox/threads/" + threadId + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());
    }

    @Test
    void getThreads_asViewOnlyMember_shouldSucceed() throws Exception {
        String leaderToken = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-leader-view@example.com", "Foto", "LeaderView");
        String roomId = createRoomAndGetId(leaderToken);
        enableFotoboxWithPermission(leaderToken, roomId, "VIEW_ONLY");

        createThreadAndGetId(leaderToken, roomId, "Visible Thread");

        // Register a second user and add to room
        var memberResponse = TestHelper.registerAndGetResponse(mockMvc,
                "fotobox-viewer@example.com", "Foto", "Viewer");
        String memberToken = memberResponse.path("data").path("accessToken").asText();
        String memberId = memberResponse.path("data").path("userId").asText();
        addMemberToRoom(leaderToken, roomId, memberId);

        // VIEW_ONLY member should see threads
        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/fotobox/threads")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void getSettings_asNonMember_shouldFail() throws Exception {
        String leaderToken = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-leader-nm@example.com", "Foto", "LeaderNm");
        String roomId = createRoomAndGetId(leaderToken);

        String nonMemberToken = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-nonmember@example.com", "Foto", "NonMember");

        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/fotobox/settings")
                        .header("Authorization", "Bearer " + nonMemberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateSettings_asNonLeader_shouldFail() throws Exception {
        String leaderToken = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-leader-nl@example.com", "Foto", "LeaderNl");
        String roomId = createRoomAndGetId(leaderToken);

        // Add a regular member
        var memberResponse = TestHelper.registerAndGetResponse(mockMvc,
                "fotobox-nonleader@example.com", "Foto", "NonLeader");
        String memberToken = memberResponse.path("data").path("accessToken").asText();
        String memberId = memberResponse.path("data").path("userId").asText();
        addMemberToRoom(leaderToken, roomId, memberId);

        mockMvc.perform(put("/api/v1/rooms/" + roomId + "/fotobox/settings")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled": true}
                                """))
                .andExpect(status().isForbidden());
    }

    // ==================== Cross-Room Isolation ====================

    @Test
    void threads_shouldBeIsolatedPerRoom() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fotobox-isolation@example.com", "Foto", "Isolation");
        String roomId1 = createRoomAndGetId(token);
        String roomId2 = createRoomAndGetId(token);
        enableFotobox(token, roomId1);
        enableFotobox(token, roomId2);

        createThreadAndGetId(token, roomId1, "Room 1 Thread A");
        createThreadAndGetId(token, roomId1, "Room 1 Thread B");
        createThreadAndGetId(token, roomId2, "Room 2 Thread");

        mockMvc.perform(get("/api/v1/rooms/" + roomId1 + "/fotobox/threads")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/v1/rooms/" + roomId2 + "/fotobox/threads")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }
}
