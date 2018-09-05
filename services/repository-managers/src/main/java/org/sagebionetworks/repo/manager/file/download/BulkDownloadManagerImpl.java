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

	
	@Autowired
	EntityManager entityManager;
	
	@Autowired
	NodeDAO nodeDoa;
	
	@Autowired
	BulkDownloadDAO bulkDownloadDao;
	
	@WriteTransactionReadCommitted
	@Override	
	public void addFilesFromFolder(UserInfo user, String folderId) {
		ValidateArgument.required(user, "UserInfo");
		EntityChildrenRequest entityChildrenRequest = new EntityChildrenRequest();
		entityChildrenRequest.setIncludeTypes(Lists.newArrayList(EntityType.file));
		entityChildrenRequest.setParentId(folderId);
		
		EntityChildrenResponse entityChildrenResponse = null;
		do {
			// page through the children of the given container.
			entityChildrenResponse = entityManager.getChildren(user, entityChildrenRequest);
			List<FileHandleAssociation> toAdd = new LinkedList<>();
			// get the files handles for the resulting files
			if(entityChildrenResponse.getPage() != null) {
				for(EntityHeader header: entityChildrenResponse.getPage()) {
					String fileHandleId = nodeDoa.getFileHandleIdForVersion(header.getId(), header.getVersionNumber());
					FileHandleAssociation association = new FileHandleAssociation();
					association.setAssociateObjectId(header.getId());
					association.setAssociateObjectType(FileHandleAssociateType.FileEntity);
					association.setFileHandleId(fileHandleId);
					toAdd.add(association);
				}
			}
			if(!toAdd.isEmpty()) {
				DownloadList list = bulkDownloadDao.addFilesToDownloadList(""+user.getId(), toAdd);
			}
		
			// use the token to get the next page.
			entityChildrenRequest.setNextPageToken(entityChildrenResponse.getNextPageToken());
		}while(entityChildrenRequest.getNextPageToken() != null);
		
	}

}
