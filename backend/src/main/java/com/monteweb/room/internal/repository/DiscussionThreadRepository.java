package com.monteweb.room.internal.repository;

import com.monteweb.room.internal.model.DiscussionThread;
import com.monteweb.room.internal.model.ThreadAudience;
import com.monteweb.room.internal.model.ThreadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiscussionThreadRepository extends JpaRepository<DiscussionThread, UUID> {

    Page<DiscussionThread> findByRoomIdOrderByCreatedAtDesc(UUID roomId, Pageable pageable);

    Page<DiscussionThread> findByRoomIdAndStatusOrderByCreatedAtDesc(UUID roomId, ThreadStatus status, Pageable pageable);

    Page<DiscussionThread> findByRoomIdAndAudienceInOrderByCreatedAtDesc(UUID roomId, List<ThreadAudience> audiences, Pageable pageable);

    Page<DiscussionThread> findByRoomIdAndStatusAndAudienceInOrderByCreatedAtDesc(UUID roomId, ThreadStatus status, List<ThreadAudience> audiences, Pageable pageable);
}
