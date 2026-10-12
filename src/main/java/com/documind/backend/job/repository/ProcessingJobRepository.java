package com.documind.backend.job.repository;

import com.documind.backend.job.entity.JobStatus;
import com.documind.backend.job.entity.ProcessingJob;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;

public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
	@Query("select j from ProcessingJob j where j.status = com.documind.backend.job.entity.JobStatus.QUEUED order by j.createdAt")
	List<ProcessingJob> lockNextQueued(Pageable pageable);

}
