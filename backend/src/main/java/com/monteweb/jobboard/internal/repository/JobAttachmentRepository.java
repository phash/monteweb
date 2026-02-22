package com.monteweb.jobboard.internal.repository;

import com.monteweb.jobboard.internal.model.JobAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobAttachmentRepository extends JpaRepository<JobAttachment, UUID> {

    List<JobAttachment> findByJobIdOrderByCreatedAtAsc(UUID jobId);

    int countByJobId(UUID jobId);

    void deleteByJobId(UUID jobId);
}
