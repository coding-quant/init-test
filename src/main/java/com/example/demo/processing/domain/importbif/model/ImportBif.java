package com.example.demo.processing.domain.importbif.model;

import com.example.demo.processing.domain.common.model.BifKind;
import com.example.demo.processing.domain.common.model.BifStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@Data
@Entity(name = "IMPORT_BIF")
@Table(name = "IMPORT_BIF")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Builder
public class ImportBif {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column
    @Enumerated(EnumType.STRING)
    BifStatus status;

    @Column
    @Enumerated(EnumType.STRING)
    BifKind kind;

    @Column
    String memberId;

    @Column
    String dirName;

    @Column
    String fileName;

    @CreatedDate
    @Column(updatable = false)
    LocalDateTime createdAt;

    @LastModifiedDate
    @Column
    LocalDateTime modifiedAt;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "BIF_METADATA_ID")
    BifMetadataEntity bifMetadata;

}
