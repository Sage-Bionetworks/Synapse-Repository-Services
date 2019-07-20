package org.sagebionetworks.repo.manager.file;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationManager;
import org.sagebionetworks.repo.model.file.FileHandleAssociationProvider;
import org.springframework.beans.factory.annotation.Autowired;


public class FileHandleAssociationManagerImpl implements
		FileHandleAssociationManager {

	Map<FileHandleAssociateType, FileHandleAssociationProvider> providerMap;
	
	private FileHandleDao fileHandleDao;
	
	@Autowired
	public FileHandleAssociationManagerImpl(FileHandleDao fileHandleDao) {
		this.fileHandleDao = fileHandleDao;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.file.FileHandleAssociationSwitch#getFileHandleIdsAssociatedWithObject(java.util.List, java.lang.String, org.sagebionetworks.repo.model.file.FileHandleAssociationType)
	 */
	@Override
	public Set<String> getFileHandleIdsAssociatedWithObject(
			List<String> fileHandleIds, String objectId,
			FileHandleAssociateType associateType) {

		FileHandleAssociationProvider provider = getProvider(associateType);
		return provider.getFileHandleIdsAssociatedWithObject(fileHandleIds, objectId);
	}
	
	@Override
	public Set<String> getFileHandlePreviewIdsAssociatedWithObject(final List<String> fileHandleIds, String objectId,
			FileHandleAssociateType associationType) {
		// Gather the subset of file handles that are previews (Entry is <fileHandleId, fileHandlePreviewId>)
		final Map<String, String> fileHandlePreviewIds = fileHandleDao.getFileHandleIdsWithPreviewIds(fileHandleIds);
		// Get all the file handles that are actually associated with the object
		final Set<String> associatedFileHandleIds = getFileHandleIdsAssociatedWithObject(
				new ArrayList<>(fileHandlePreviewIds.keySet()), objectId, associationType);

		Set<String> results = new HashSet<>(associatedFileHandleIds.size());

		// Retain only the preview ids that are actually associated to the object
		associatedFileHandleIds.forEach(fileHandleId -> {
			results.add(fileHandlePreviewIds.get(fileHandleId));
		});

		return results;
	}
	
	
	/**
	 * Helper to get the correct provider for a given type.
	 * @param type
	 * @return
	 */
	private FileHandleAssociationProvider getProvider(FileHandleAssociateType type){
		if(type == null){
			throw new IllegalArgumentException("FileHandleAssociationType cannot be null");
		}
		FileHandleAssociationProvider provider = providerMap.get(type);
		if(provider == null){
			throw new UnsupportedOperationException("Currently do not support this operation for FileHandleAssociationType = "+type);
		}
		return provider;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.file.FileHandleAssociationSwitch#getObjectTypeForAssociationType(org.sagebionetworks.repo.model.file.FileHandleAssociationType)
	 */
	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType(
			FileHandleAssociateType associateType) {
		FileHandleAssociationProvider provider = getProvider(associateType);
		return provider.getAuthorizationObjectTypeForAssociatedObjectType();
	}

	/**
	 * Injected.
	 * @param providerMap
	 */
	public void setProviderMap(
			Map<FileHandleAssociateType, FileHandleAssociationProvider> providerMap) {
		this.providerMap = providerMap;
	}

}
