package org.sagebionetworks.repo.manager.file;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Service
public class FileHandleAuthorizationManagerImpl implements FileHandleAuthorizationManager {
	
	
	private AuthorizationManager authorizationManager;

	@Autowired
	public FileHandleAuthorizationManagerImpl(AuthorizationManager authorizationManager) {
		this.authorizationManager = authorizationManager;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.AuthorizationManager#canDownLoadFile(org.sagebionetworks.repo.model.UserInfo, java.util.List)
	 */
	@Override
	public List<FileHandleAssociationAuthorizationStatus> canDownLoadFile(UserInfo user,
			List<FileHandleAssociation> associations) {
		if(user == null){
			throw new IllegalArgumentException("User cannot be null");
		}
		// validate the input
		validate(associations);
		/* A separate authorization check must be made for each associated object so the
		 * first step is to group by the associated object.
		 */
		Map<FileAssociateObject, List<String>> objectGroups =  Maps.newHashMap();
		for(FileHandleAssociation fha: associations){
			FileAssociateObject key = new FileAssociateObject(fha.getAssociateObjectId(), fha.getAssociateObjectType());
			List<String> fileHandleIds = objectGroups.get(key);
			if(fileHandleIds == null){
				fileHandleIds = Lists.newLinkedList();
				objectGroups.put(key, fileHandleIds);
			}
			fileHandleIds.add(fha.getFileHandleId());
		}
		// used to track the results.
		Map<FileHandleAssociation, FileHandleAssociationAuthorizationStatus> resultMap = Maps.newHashMap();
		// execute a canDownloadFile() check for each object group.
		for(FileAssociateObject object: objectGroups.keySet()){
			List<String> fileHandleIds = objectGroups.get(object);
			// The authorization check for this group.
			List<FileHandleAuthorizationStatus> groupResults = authorizationManager.canDownloadFile(user, fileHandleIds, object.getObjectId(), object.getObjectType());
			// Build the results for this group
			for(FileHandleAuthorizationStatus fileStatus: groupResults){
				FileHandleAssociation fileAssociation = new FileHandleAssociation();
				fileAssociation.setFileHandleId(fileStatus.getFileHandleId());
				fileAssociation.setAssociateObjectId(object.getObjectId());
				fileAssociation.setAssociateObjectType(object.getObjectType());
				FileHandleAssociationAuthorizationStatus result = new FileHandleAssociationAuthorizationStatus(fileAssociation, fileStatus.getStatus());
				resultMap.put(fileAssociation, result);
			}
		}
		
		// put the results in the same order as the request
		List<FileHandleAssociationAuthorizationStatus> results = Lists.newLinkedList();
		for(FileHandleAssociation association: associations){
			results.add(resultMap.get(association));
		}
		return results;
	}
	
	public void validate(List<FileHandleAssociation> associations){
		if(associations == null){
			throw new IllegalArgumentException("FileHandleAssociations cannot be null");
		}
		if(associations.isEmpty()){
			throw new IllegalArgumentException("FileHandleAssociations is empty.  Must include at least one FileHandleAssociation");
		}
		for(FileHandleAssociation fha: associations){
			validate(fha);
		}
	}
	
	public void validate(FileHandleAssociation fha){
		if(fha == null){
			throw new IllegalArgumentException("FileHandleAssociation cannot be null");
		}
		if(fha.getFileHandleId() == null){
			throw new IllegalArgumentException("FileHandleAssociation.fileHandleId cannot be null");
		}
		if(fha.getAssociateObjectId() == null){
			throw new IllegalArgumentException("FileHandleAssociation.associateObjectId cannot be null");
		}
		if(fha.getAssociateObjectType() == null){
			throw new IllegalArgumentException("FileHandleAssociation.associateObjectType cannot be null");
		}
	}
}
