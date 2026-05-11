package com.example.demo.sftp.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface BlockContentRepository extends JpaRepository<BlockContentEntity, UUID> {

	boolean existsByImportBif(BifFileEntity importBif);
}
