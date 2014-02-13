package org.sagebionetworks.repo.web.controller.metadata;

import java.util.List;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;

public interface MetadataProviderFactory {
	
	public List<TypeSpecificMetadataProvider<Entity>> getMetadataProvider(EntityType type);

}
