package org.sagebionetworks.table.cluster.metadata;

import org.sagebionetworks.repo.model.ObjectType;

public interface ObjectFieldModelProviderFactory {
	
	ObjectFieldModelProvider getObjectFieldModelProvider(ObjectType objectType);

}
