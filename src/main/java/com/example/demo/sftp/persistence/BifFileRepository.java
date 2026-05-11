package com.example.demo.sftp.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.sftp.model.BifStatus;

interface BifFileRepository extends JpaRepository<BifFileEntity, UUID> {

	boolean existsByMemberIdAndFileName(String memberId, String fileName);

	Optional<BifFileEntity> findByMemberIdAndFileName(String memberId, String fileName);

	List<BifFileEntity> findAllByStatusOrderByCreatedAtAsc(BifStatus status);

	List<BifFileEntity> findAllByOrderByCreatedAtAsc();
}
