package com.example.demo.processing.domain.importbif;

import com.example.demo.processing.domain.importbif.model.ImportBif;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImportBifRepository extends JpaRepository<ImportBif, UUID> {
    boolean existsByMemberIdAndFileName(String memberId, String fileName);
}
