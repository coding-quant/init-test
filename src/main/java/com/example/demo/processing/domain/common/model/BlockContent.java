package com.example.demo.processing.domain.common.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity(name = "BLOCK_CONTENT")
@Table(name = "BLOCK_CONTENT")
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder
public class BlockContent {

   @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

   @Column
    @Enumerated(EnumType.STRING)
    BifKind kind;

   @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column
    private String content;

   @Column(updatable = false)
    @CreatedDate
   LocalDateTime createdDate;

}
