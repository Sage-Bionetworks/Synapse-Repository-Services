package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.download.DownloadListManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListResponse;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListRequest;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListResponse;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DownloadListServiceImpl implements DownloadListService {
	
	UserManager userManager;
	DownloadListManager downloadListManager;
	
	@Autowired
	public DownloadListServiceImpl(UserManager userManager, DownloadListManager downloadListManager) {
		super();
		this.userManager = userManager;
		this.downloadListManager = downloadListManager;
	}

	@Override
	public AddBatchOfFilesToDownloadListResponse addBatchOfFilesToDownloadList(Long userId,
			AddBatchOfFilesToDownloadListRequest toAdd) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return downloadListManager.addBatchOfFilesToDownloadList(userInfo, toAdd);
	}

	@Override
	public RemoveBatchOfFilesFromDownloadListResponse removeBatchOfFilesFromDownloadList(Long userId,
			RemoveBatchOfFilesFromDownloadListRequest toRemove) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return downloadListManager.removeBatchOfFilesFromDownloadList(userInfo, toRemove);
	}

	@Override
	public void clearDownloadList(Long userId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		downloadListManager.clearDownloadList(userInfo);
	}

}
