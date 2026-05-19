package com.example.demo.processing.domain.importbif.download;

import com.example.demo.processing.domain.common.model.BifStatus;
import com.example.demo.processing.domain.common.model.BlockContent;
import com.example.demo.processing.domain.importbif.ImportBifRepository;
import com.example.demo.processing.domain.importbif.model.ImportBif;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DownloadFileHandler {

    private final ImportBifRepository importBifRepository;

    @Transactional(readOnly = true)
    public List<ImportBif> findNewFilesToDownload() {
        return importBifRepository.findAllByStatus(BifStatus.NEW);
    }

    @Transactional
    public void saveDownloadedContent(ImportBif importBif, String fileContent) {
        if (importBif.getId() == null) {
            throw new IllegalStateException("Downloaded importBif does not have id");
        }

        ImportBif downloadedImportBif = importBifRepository.findById(importBif.getId())
                .orElseThrow(() -> new IllegalStateException("ImportBif does not exist: " + importBif.getId()));

        downloadedImportBif.addBlockContent(BlockContent.builder()
                .kind(downloadedImportBif.getKind())
                .content(fileContent)
                .build());
        downloadedImportBif.setStatus(BifStatus.RECEIVED);
        importBifRepository.save(downloadedImportBif);

        log.info(
                "Saved remote file content for importBif {} to BLOCK_CONTENT ({} chars)",
                downloadedImportBif.getId(),
                fileContent.length()
        );
    }
}
