package com.monteweb.feed.internal.repository;

import com.monteweb.feed.internal.model.FeedPostAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FeedPostAttachmentRepository extends JpaRepository<FeedPostAttachment, UUID> {
}
