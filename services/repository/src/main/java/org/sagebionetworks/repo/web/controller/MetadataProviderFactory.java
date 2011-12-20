package org.sagebionetworks.repo.web.controller;

import java.util.List;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.web.controller.metadata.TypeSpecificMetadataProvider;

public interface MetadataProviderFactory {
	
	public List<TypeSpecificMetadataProvider<Entity>> getMetadataProvider(EntityType type);

}
