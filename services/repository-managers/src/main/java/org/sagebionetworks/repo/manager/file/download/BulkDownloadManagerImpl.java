package org.sagebionetworks.repo.manager.file.download;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.download.BulkDownloadDAO;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

public class BulkDownloadManagerImpl implements BulkDownloadManager {

	public static final int MAX_FILES_PER_DOWNLOAD_LIST = 100;

	@Autowired
	EntityManager entityManager;

	@Autowired
	NodeDAO nodeDoa;

	@Autowired
	BulkDownloadDAO bulkDownloadDao;

	@WriteTransactionReadCommitted
	@Override
	public DownloadList addFilesFromFolder(UserInfo user, String folderId) {
		ValidateArgument.required(user, "UserInfo");
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.file);
		String nextPageToken = null;
		do {
			EntityChildrenRequest entityChildrenRequest = new EntityChildrenRequest();
			entityChildrenRequest.setIncludeTypes(includeTypes);
			entityChildrenRequest.setParentId(folderId);
			entityChildrenRequest.setNextPageToken(nextPageToken);
			// page through the children of the given container.
			EntityChildrenResponse entityChildrenResponse = entityManager.getChildren(user, entityChildrenRequest);
			List<FileHandleAssociation> toAdd = new LinkedList<>();
			// get the files handles for the resulting files
			for (EntityHeader header : entityChildrenResponse.getPage()) {
				String fileHandleId = nodeDoa.getFileHandleIdForVersion(header.getId(), header.getVersionNumber());
				FileHandleAssociation association = new FileHandleAssociation();
				association.setAssociateObjectId(header.getId());
				association.setAssociateObjectType(FileHandleAssociateType.FileEntity);
				association.setFileHandleId(fileHandleId);
				toAdd.add(association);
			}

			if (!toAdd.isEmpty()) {
				DownloadList list = bulkDownloadDao.addFilesToDownloadList("" + user.getId(), toAdd);
				if (list.getFilesToDownload().size() > MAX_FILES_PER_DOWNLOAD_LIST) {
					throw new IllegalArgumentException(
							"Exceeded the maximum number of " + MAX_FILES_PER_DOWNLOAD_LIST + " files.");
				}
			}

			// use the token to get the next page.
			nextPageToken = entityChildrenResponse.getNextPageToken();
			// continue as long as we have a next page token.
		} while (nextPageToken != null);
		// return the final state of the download list.
		return bulkDownloadDao.getUsersDownloadList("" + user.getId());
	}

}
