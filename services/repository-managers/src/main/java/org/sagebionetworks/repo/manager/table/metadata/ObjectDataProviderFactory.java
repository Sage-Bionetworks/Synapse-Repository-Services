package org.sagebionetworks.repo.manager.table.metadata;

import org.sagebionetworks.repo.model.table.ReplicationType;

public interface ObjectDataProviderFactory {

	/**
	 * Get the ObjectDataProvider for the given ObjectType
	 * @param type
	 * @return
	 */
	ObjectDataProvider getObjectDataProvider(ReplicationType type);
	
}
