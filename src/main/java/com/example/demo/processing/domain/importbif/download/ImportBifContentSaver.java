package com.example.demo.processing.domain.importbif.download;

import com.example.demo.processing.domain.common.model.BifStatus;
import com.example.demo.processing.domain.common.model.BlockContent;
import com.example.demo.processing.domain.importbif.ImportBifRepository;
import com.example.demo.processing.domain.importbif.model.ImportBif;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
class ImportBifContentSaver {

    private final ImportBifRepository importBifRepository;

    @Transactional
    void save(ImportBif downloadedImportBif, String fileContent) {
        if (downloadedImportBif.getId() == null) {
            throw new IllegalStateException("Downloaded importBif does not have id");
        }

        ImportBif importBif = importBifRepository.findById(downloadedImportBif.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "ImportBif does not exist: " + downloadedImportBif.getId()));

        importBif.addBlockContent(BlockContent.builder()
                .kind(importBif.getKind())
                .content(fileContent)
                .build());
        importBif.setStatus(BifStatus.RECEIVED);
        importBifRepository.save(importBif);

        log.info(
                "Saved remote file content for importBif {} to BLOCK_CONTENT ({} chars)",
                importBif.getId(),
                fileContent.length()
        );
    }
}
