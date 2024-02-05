package org.sagebionetworks.repo.web.service.metadata;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;

public interface MetadataProviderFactory {
	
	public Optional<EntityProvider<? extends Entity>> getMetadataProvider(EntityType type);

}
