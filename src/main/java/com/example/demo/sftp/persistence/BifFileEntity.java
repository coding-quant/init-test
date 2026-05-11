package com.example.demo.sftp.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.demo.sftp.model.BifFileCandidate;
import com.example.demo.sftp.model.BifKind;
import com.example.demo.sftp.model.BifMetadata;
import com.example.demo.sftp.model.BifStatus;
import com.example.demo.sftp.model.BifType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "import_bif",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_import_bif_member_file",
				columnNames = { "member_id", "file_name" }))
public class BifFileEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "member_id", nullable = false)
	private String memberId;

	@Column(name = "dir_name", nullable = false)
	private String dirName;

	@Column(name = "file_name", nullable = false)
	private String fileName;

	@Column(name = "remote_path", nullable = false)
	private String remotePath;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BifStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BifKind kind;

	@Column(name = "kir_bif_id")
	private String kirBifId;

	@Enumerated(EnumType.STRING)
	@Column(name = "bif_type")
	private BifType bifType;

	@Column(name = "session_date")
	private LocalDate sessionDate;

	@Column(name = "session_number")
	private Integer sessionNumber;

	@Column(name = "bif_timestamp")
	private LocalDateTime bifTimestamp;

	@Column(name = "num_ct_blk")
	private Integer numCtBlk;

	@Column(name = "num_rfr_blk")
	private Integer numRfrBlk;

	@Column(name = "num_rej_blk")
	private Integer numRejBlk;

	@Column(name = "num_prc_blk")
	private Integer numPrcBlk;

	@Column(name = "num_roi_blk")
	private Integer numRoiBlk;

	@Column(name = "num_cnr_blk")
	private Integer numCnrBlk;

	@Column(name = "num_rmp_blk")
	private Integer numRmpBlk;

	@Column(name = "num_rog_blk")
	private Integer numRogBlk;

	@Column(name = "num_sr_blk")
	private Integer numSrBlk;

	@Column(name = "num_rep_blk")
	private Integer numRepBlk;

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

	public String getMemberId() {
		return memberId;
	}

	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}

	public String getDirName() {
		return dirName;
	}

	public void setDirName(String dirName) {
		this.dirName = dirName;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getRemotePath() {
		return remotePath;
	}

	public void setRemotePath(String remotePath) {
		this.remotePath = remotePath;
	}

	public BifStatus getStatus() {
		return status;
	}

	public void setStatus(BifStatus status) {
		this.status = status;
	}

	public BifKind getKind() {
		return kind;
	}

	public void setKind(BifKind kind) {
		this.kind = kind;
	}

	public String getKirBifId() {
		return kirBifId;
	}

	public void setKirBifId(String kirBifId) {
		this.kirBifId = kirBifId;
	}

	public BifType getBifType() {
		return bifType;
	}

	public void setBifType(BifType bifType) {
		this.bifType = bifType;
	}

	public LocalDate getSessionDate() {
		return sessionDate;
	}

	public void setSessionDate(LocalDate sessionDate) {
		this.sessionDate = sessionDate;
	}

	public Integer getSessionNumber() {
		return sessionNumber;
	}

	public void setSessionNumber(Integer sessionNumber) {
		this.sessionNumber = sessionNumber;
	}

	public LocalDateTime getBifTimestamp() {
		return bifTimestamp;
	}

	public void setBifTimestamp(LocalDateTime bifTimestamp) {
		this.bifTimestamp = bifTimestamp;
	}

	public Integer getNumCtBlk() {
		return numCtBlk;
	}

	public void setNumCtBlk(Integer numCtBlk) {
		this.numCtBlk = numCtBlk;
	}

	public Integer getNumRfrBlk() {
		return numRfrBlk;
	}

	public void setNumRfrBlk(Integer numRfrBlk) {
		this.numRfrBlk = numRfrBlk;
	}

	public Integer getNumRejBlk() {
		return numRejBlk;
	}

	public void setNumRejBlk(Integer numRejBlk) {
		this.numRejBlk = numRejBlk;
	}

	public Integer getNumPrcBlk() {
		return numPrcBlk;
	}

	public void setNumPrcBlk(Integer numPrcBlk) {
		this.numPrcBlk = numPrcBlk;
	}

	public Integer getNumRoiBlk() {
		return numRoiBlk;
	}

	public void setNumRoiBlk(Integer numRoiBlk) {
		this.numRoiBlk = numRoiBlk;
	}

	public Integer getNumCnrBlk() {
		return numCnrBlk;
	}

	public void setNumCnrBlk(Integer numCnrBlk) {
		this.numCnrBlk = numCnrBlk;
	}

	public Integer getNumRmpBlk() {
		return numRmpBlk;
	}

	public void setNumRmpBlk(Integer numRmpBlk) {
		this.numRmpBlk = numRmpBlk;
	}

	public Integer getNumRogBlk() {
		return numRogBlk;
	}

	public void setNumRogBlk(Integer numRogBlk) {
		this.numRogBlk = numRogBlk;
	}

	public Integer getNumSrBlk() {
		return numSrBlk;
	}

	public void setNumSrBlk(Integer numSrBlk) {
		this.numSrBlk = numSrBlk;
	}

	public Integer getNumRepBlk() {
		return numRepBlk;
	}

	public void setNumRepBlk(Integer numRepBlk) {
		this.numRepBlk = numRepBlk;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public BifFileCandidate toCandidate() {
		return new BifFileCandidate(memberId, dirName, fileName, remotePath, status, kind, toMetadata());
	}

	public BifMetadata toMetadata() {
		return new BifMetadata(
				kirBifId,
				bifType,
				new BifMetadata.SessionSlot(sessionDate, sessionNumber),
				bifTimestamp,
				new BifMetadata.Summary(
						numCtBlk,
						numRfrBlk,
						numRejBlk,
						numPrcBlk,
						numRoiBlk,
						numCnrBlk,
						numRmpBlk,
						numRogBlk,
						numSrBlk,
						numRepBlk));
	}
}
