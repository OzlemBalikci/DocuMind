package com.documind.backend.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, Long> {

	List<ProcessingJob> findByStatusOrderByCreatedAtAsc(JobStatus status);

	List<ProcessingJob> findByStatusInOrderByCreatedAtAsc(Collection<JobStatus> statuses);

	List<ProcessingJob> findByDocument_IdOrderByCreatedAtDesc(Long documentId);

	List<ProcessingJob> findByMessage_IdOrderByCreatedAtDesc(Long messageId);

	Optional<ProcessingJob> findFirstByDocument_IdAndTypeOrderByCreatedAtDesc(Long documentId, JobType type);

	Optional<ProcessingJob> findFirstByMessage_IdAndTypeOrderByCreatedAtDesc(Long messageId, JobType type);

	boolean existsByDocument_IdAndTypeAndStatusIn(Long documentId, JobType type, Collection<JobStatus> statuses);

	boolean existsByMessage_IdAndTypeAndStatusIn(Long messageId, JobType type, Collection<JobStatus> statuses);

}
