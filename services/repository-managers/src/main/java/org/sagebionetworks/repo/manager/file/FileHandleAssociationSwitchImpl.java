package org.sagebionetworks.repo.manager.file;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.model.file.FileHandleAssociationSwitch;
import org.sagebionetworks.repo.model.file.FileHandleAssociationType;


public class FileHandleAssociationSwitchImpl implements
		FileHandleAssociationSwitch {

	Map<FileHandleAssociationType, FileHandleAssociationProvider> providerMap;
	
	@Override
	public Set<String> getDistinctAssociationsForFileHandleIds(
			List<String> fileHandleIds, String objectId,
			FileHandleAssociationType associationType) {

		FileHandleAssociationProvider provider = getProvider(associationType);
		return provider.getDistinctAssociationsForFileHandleIds(fileHandleIds, objectId);
	}
	
	/**
	 * Get a provider for a type.
	 * @param type
	 * @return
	 */
	private FileHandleAssociationProvider getProvider(FileHandleAssociationType type){
		if(type == null){
			throw new IllegalArgumentException("FileHandleAssociationType cannot be null");
		}
		FileHandleAssociationProvider provider = providerMap.get(type);
		if(provider == null){
			throw new UnsupportedOperationException("Currently do not support this opperation for FileHandleAssociationType = "+type);
		}
		return provider;
	}

	@Override
	public ObjectType getObjectTypeForAssociationType(
			FileHandleAssociationType associationType) {
		FileHandleAssociationProvider provider = getProvider(associationType);
		return provider.getObjectTypeForAssociationType();
	}

}
