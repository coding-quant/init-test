package com.example.demo.processing.domain.importbif.scanner.filter;

import lombok.RequiredArgsConstructor;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OnlyFilesFilter implements FileListFilter<SftpClient.DirEntry> {

    @Override
    public List<SftpClient.DirEntry> filterFiles(SftpClient.DirEntry[] dirEntries) {
        if(dirEntries == null) List.of();
        return Arrays.stream(dirEntries).filter(e -> !e.getAttributes().isDirectory())
                .toList();
    }
}
