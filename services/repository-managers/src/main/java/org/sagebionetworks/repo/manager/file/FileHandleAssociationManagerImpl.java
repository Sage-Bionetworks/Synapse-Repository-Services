package org.sagebionetworks.repo.manager.file;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationManager;
import org.sagebionetworks.repo.model.file.FileHandleAssociationProvider;
import org.springframework.beans.factory.annotation.Autowired;

public class FileHandleAssociationManagerImpl implements FileHandleAssociationManager {

	Map<FileHandleAssociateType, FileHandleAssociationProvider> providerMap;

	private FileHandleDao fileHandleDao;

	@Autowired
	public FileHandleAssociationManagerImpl(FileHandleDao fileHandleDao) {
		this.fileHandleDao = fileHandleDao;
	}

	@Override
	public Set<String> getFileHandleIdsAssociatedWithObject(List<String> fileHandleIds, String objectId,
			FileHandleAssociateType associateType) {

		FileHandleAssociationProvider provider = getProvider(associateType);
		
		Set<String> allFileHandleIds = new HashSet<>();
		
		// Get the directly associated file handle ids
		Set<String> associatedFileHandleIds = provider.getFileHandleIdsDirectlyAssociatedWithObject(fileHandleIds, objectId);
		
		List<String> remainingFileHandleIds = fileHandleIds.stream().filter((id) -> !associatedFileHandleIds.contains(id)).collect(Collectors.toList());
		
		Set<String> associatedFileHandlePreviewIds = getFileHandlePreviewIdsAssociatedWithObject(remainingFileHandleIds, objectId, associateType);
		
		allFileHandleIds.addAll(associatedFileHandleIds);
		allFileHandleIds.addAll(associatedFileHandlePreviewIds);
		
		return allFileHandleIds;
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType(FileHandleAssociateType associateType) {
		FileHandleAssociationProvider provider = getProvider(associateType);
		return provider.getAuthorizationObjectTypeForAssociatedObjectType();
	}

	private Set<String> getFileHandlePreviewIdsAssociatedWithObject(final List<String> fileHandleIds, String objectId,
			FileHandleAssociateType associationType) {
		if (fileHandleIds.isEmpty()) {
			return Collections.emptySet();
		}
		// Gather the subset of file handles that are previews (Entry is <fileHandleId, fileHandlePreviewId>)
		final Map<String, String> fileHandlePreviewIds = fileHandleDao.getFileHandleIdsWithPreviewIds(fileHandleIds);
		// Get all the file handles that are actually associated with the object
		final Set<String> associatedFileHandleIds = getFileHandleIdsAssociatedWithObject(new ArrayList<>(fileHandlePreviewIds.keySet()), objectId, associationType);

		Set<String> results = new HashSet<>(associatedFileHandleIds.size());

		// Retain only the preview ids that are actually associated to the object
		associatedFileHandleIds.forEach(fileHandleId -> {
			results.add(fileHandlePreviewIds.get(fileHandleId));
		});

		return results;
	}

	/**
	 * Helper to get the correct provider for a given type.
	 * 
	 * @param type
	 * @return
	 */
	private FileHandleAssociationProvider getProvider(FileHandleAssociateType type) {
		if (type == null) {
			throw new IllegalArgumentException("FileHandleAssociationType cannot be null");
		}
		FileHandleAssociationProvider provider = providerMap.get(type);
		if (provider == null) {
			throw new UnsupportedOperationException(
					"Currently do not support this operation for FileHandleAssociationType = " + type);
		}
		return provider;
	}

	/**
	 * Injected.
	 * 
	 * @param providerMap
	 */
	public void setProviderMap(Map<FileHandleAssociateType, FileHandleAssociationProvider> providerMap) {
		this.providerMap = providerMap;
	}

}
