package com.example.demo.processing.domain.importbif.model;

import com.example.demo.processing.domain.common.model.BifType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(name = "BIF_METADATA")
@Table(name = "BIF_METADATA")
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BifMetadataEntity {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column
    String kirBifId;

    @Column
    @Enumerated(EnumType.STRING)
    BifType type;

    @Embedded
    SessionSlot sessionSlot;

    @Column
    LocalDateTime bifTimestamp;

    @Embedded
    Summary summary;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SessionSlot {

        @Column(name = "session_date")
        LocalDate date;

        @Column(name = "session_number")
        Integer number;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Summary {

        @Column(name = "num_ct_blk")
        Integer numCtBlk;

        @Column(name = "num_rfr_blk")
        Integer numRfrBlk;

        @Column(name = "num_rej_blk")
        Integer numRejBlk;

        @Column(name = "num_prc_blk")
        Integer numPrcBlk;

        @Column(name = "num_roi_blk")
        Integer numRoiBlk;

        @Column(name = "num_cnr_blk")
        Integer numCnrBlk;

        @Column(name = "num_rmp_blk")
        Integer numRmpBlk;

        @Column(name = "num_rog_blk")
        Integer numRogBlk;

        @Column(name = "num_sr_blk")
        Integer numSrBlk;

        @Column(name = "num_rep_blk")
        Integer numRepBlk;
    }
}
