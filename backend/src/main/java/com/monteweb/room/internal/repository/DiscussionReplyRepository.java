package com.monteweb.room.internal.repository;

import com.monteweb.room.internal.model.DiscussionReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DiscussionReplyRepository extends JpaRepository<DiscussionReply, UUID> {

    Page<DiscussionReply> findByThreadIdOrderByCreatedAtAsc(UUID threadId, Pageable pageable);

    long countByThreadId(UUID threadId);
}
