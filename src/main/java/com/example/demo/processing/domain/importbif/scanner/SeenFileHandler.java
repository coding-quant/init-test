package com.example.demo.processing.domain.importbif.scanner;

import com.example.demo.processing.domain.importbif.ImportBifRepository;
import com.example.demo.processing.domain.importbif.model.ImportBif;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeenFileHandler {

    private final ImportBifRepository repository;

    public boolean matchesAnyOfAcceptedRegexes(List<String> acceptedFilesRegexes, SftpFileInfo fileInfo) {
        return acceptedFilesRegexes.stream().anyMatch(regex -> fileInfo.getFilename().matches(regex));
    }

    public boolean doesNotExist(String memberId, String fileName) {
        return !repository.existsByMemberIdAndFileName(memberId, fileName);
    }

    public boolean doesNotExist(String memberId, String dirName, String fileName) {
        return !repository.existsByMemberIdAndDirNameAndFileName(memberId, dirName, fileName);
    }

    @Transactional
    public void save(ImportBif importBif) {
        log.info("Saving importBif {}", importBif);
        repository.save(importBif);

    }
}
