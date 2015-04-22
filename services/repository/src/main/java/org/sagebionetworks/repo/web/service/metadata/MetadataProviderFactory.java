package org.sagebionetworks.repo.web.service.metadata;

import java.util.List;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;

public interface MetadataProviderFactory {
	
	public List<EntityProvider<Entity>> getMetadataProvider(EntityType type);

}
