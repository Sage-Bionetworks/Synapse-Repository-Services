package org.sagebionetworks.repo.manager.file;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.IdRange;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileHandleAssociationManagerImpl implements FileHandleAssociationManager {

	private Map<FileHandleAssociateType, FileHandleAssociationProvider> providerMap;
	private Map<FileHandleAssociateType, FileHandleAssociationScanner> scannerMap;

	private FileHandleDao fileHandleDao;

	@Autowired
	public FileHandleAssociationManagerImpl(FileHandleDao fileHandleDao) {
		this.fileHandleDao = fileHandleDao;
	}

	@Autowired
	public void configureProviderMap(Map<FileHandleAssociateType, FileHandleAssociationProvider> providersMap) {
		this.providerMap = providersMap;
	}
	
	@Autowired
	public void configureScannerMap(Map<FileHandleAssociateType, FileHandleAssociationScanner> scannerMap) {
		this.scannerMap = scannerMap;
	}
	
	@Override
	public Set<String> getFileHandleIdsAssociatedWithObject(List<String> fileHandleIds, String objectId,
			FileHandleAssociateType associateType) {

		ValidateArgument.required(fileHandleIds, "fileHandleIds");
		ValidateArgument.required(associateType, "associateType");
		ValidateArgument.required(objectId, "objectId");

		if (fileHandleIds.isEmpty()) {
			return Collections.emptySet();
		}
		
		Set<String> allFileHandleIds = new HashSet<>(fileHandleIds);

		// Gather all the file handle ids that are previews (each entry is <FileHandlePreviewId, FileHandleId>)
		Map<String, String> fileHandlePreviewIds = fileHandleDao.getFileHandlePreviewIds(fileHandleIds);

		// Expand the initial set with the file handles ids that reference any preview
		allFileHandleIds.addAll(fileHandlePreviewIds.values());

		FileHandleAssociationProvider provider = getProvider(associateType);

		// Get the directly associated file handle ids
		Set<String> associatedFileHandleIds = provider.getFileHandleIdsDirectlyAssociatedWithObject(new ArrayList<>(allFileHandleIds), objectId);

		// Builds the result filtering the ids that are not associated
		Set<String> result = fileHandleIds.stream().filter(id -> {
			// Retain the given id or the id of the file handle mapped with the preview if it exist
			String mappedId = fileHandlePreviewIds.getOrDefault(id, id);
			return associatedFileHandleIds.contains(mappedId);
		}).collect(Collectors.toSet());

		return result;
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType(FileHandleAssociateType associateType) {
		ValidateArgument.required(associateType, "associateType");
		FileHandleAssociationProvider provider = getProvider(associateType);
		return provider.getAuthorizationObjectTypeForAssociatedObjectType();
	}

	@Override
	public IdRange getIdRange(FileHandleAssociateType associationType) {
		FileHandleAssociationScanner scanner = getScanner(associationType);
		
		return scanner.getIdRange();
	}
	
	@Override
	public long getMaxIdRangeSize(FileHandleAssociateType associationType) {
		FileHandleAssociationScanner scanner = getScanner(associationType);
		
		return scanner.getMaxIdRangeSize();
	}
	
	@Override
	public Iterable<ScannedFileHandleAssociation> scanRange(FileHandleAssociateType associationType, IdRange range) {
		FileHandleAssociationScanner scanner = getScanner(associationType);
		
		return scanner.scanRange(range);
	}
	
	private FileHandleAssociationScanner getScanner(FileHandleAssociateType associationType) {
		ValidateArgument.required(associationType, "associationType");
		FileHandleAssociationScanner scanner = scannerMap.get(associationType);
		if (scanner == null) {
			throw new UnsupportedOperationException("Currently do not support this operation for FileHandleAssociationType = " + associationType);
		}
		return scanner;
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
			throw new UnsupportedOperationException("Currently do not support this operation for FileHandleAssociationType = " + type);
		}
		return provider;
	}

}
