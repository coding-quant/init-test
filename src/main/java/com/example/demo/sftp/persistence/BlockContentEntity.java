package com.example.demo.sftp.persistence;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.demo.sftp.model.BlockContentType;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "block_content")
public class BlockContentEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private BlockContentType type;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Column(nullable = false)
	private String content;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "import_bif_id", nullable = false)
	private BifFileEntity importBif;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}

	public UUID getId() {
		return id;
	}

	public BlockContentType getType() {
		return type;
	}

	public void setType(BlockContentType type) {
		this.type = type;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public BifFileEntity getImportBif() {
		return importBif;
	}

	public void setImportBif(BifFileEntity importBif) {
		this.importBif = importBif;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
