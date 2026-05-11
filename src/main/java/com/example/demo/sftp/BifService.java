package com.example.demo.sftp;

import java.util.List;

import com.example.demo.sftp.model.BifFileCandidate;
import com.example.demo.sftp.model.BifFileContent;
import com.example.demo.sftp.model.BifsInfo;

public interface BifService {

	boolean exists(String memberId, String dirName, String fileName);

	void saveBifs(BifsInfo bifsInfo);

	void registerNewBif(BifFileCandidate candidate);

	List<BifFileCandidate> findNewBifsToDownload();

	void saveBifFile(BifFileContent bifFileContent);

	void markBifAsError(BifFileCandidate candidate);

	List<BifsInfo> findAll();
}
