package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.web.controller.metadata.TypeSpecificMetadataProvider;

public interface MetadataProviderFactory {
	
	public TypeSpecificMetadataProvider<Entity> getMetadataProvider(EntityType type);

}
