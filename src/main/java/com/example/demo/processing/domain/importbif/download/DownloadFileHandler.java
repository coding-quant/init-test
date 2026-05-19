package com.example.demo.processing.domain.importbif.download;

import com.example.demo.processing.domain.common.model.BifStatus;
import com.example.demo.processing.domain.importbif.ImportBifRepository;
import com.example.demo.processing.domain.importbif.model.ImportBif;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DownloadFileHandler {

    private final ImportBifRepository importBifRepository;
    private final ImportBifContentSaver importBifContentSaver;

    @Transactional(readOnly = true)
    public List<ImportBif> findNewFilesToDownload() {
        return importBifRepository.findAllByStatus(BifStatus.NEW);
    }

    public void saveDownloadedContent(ImportBif importBif, String fileContent) {
        importBifContentSaver.save(importBif, fileContent);
    }
}
