package com.monteweb.room.internal.controller;

import com.monteweb.room.internal.model.RoomChatChannel;
import com.monteweb.room.internal.service.RoomChatService;
import com.monteweb.room.internal.service.RoomChatService.RoomChatChannelInfo;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms/{roomId}/chat")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "monteweb.modules.messaging", name = "enabled", havingValue = "true")
public class RoomChatController {

    private final RoomChatService roomChatService;

    @GetMapping("/channels")
    public ResponseEntity<ApiResponse<List<RoomChatChannelInfo>>> getChannels(
            @PathVariable UUID roomId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(roomChatService.getChannels(roomId, userId)));
    }

    @PostMapping("/channels")
    public ResponseEntity<ApiResponse<RoomChatChannelInfo>> getOrCreateChannel(
            @PathVariable UUID roomId,
            @RequestBody CreateChannelRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        RoomChatChannel.ChannelType type = request.channelType() != null
                ? RoomChatChannel.ChannelType.valueOf(request.channelType())
                : RoomChatChannel.ChannelType.MAIN;
        return ResponseEntity.ok(ApiResponse.ok(
                roomChatService.getOrCreateChannel(roomId, userId, type)));
    }

    public record CreateChannelRequest(String channelType) {}
}
