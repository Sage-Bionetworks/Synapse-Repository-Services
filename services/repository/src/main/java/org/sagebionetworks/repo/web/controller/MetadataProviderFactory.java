package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.Nodeable;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.web.controller.metadata.TypeSpecificMetadataProvider;

public interface MetadataProviderFactory {
	
	public TypeSpecificMetadataProvider<Nodeable> getMetadataProvider(ObjectType type);

}
