package org.sagebionetworks.repo.web.controller;

import java.util.Map;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.web.controller.metadata.TypeSpecificMetadataProvider;

public class MetadataProviderFactoryImpl implements MetadataProviderFactory {

	private Map<String, TypeSpecificMetadataProvider<Entity>>  metadataProviderMap;
	
	/**
	 * @param metadataProviderMap the metadataProviderMap to set
	 */
	public void setMetadataProviderMap(
			Map<String, TypeSpecificMetadataProvider<Entity>> metadataProviderMap) {
		this.metadataProviderMap = metadataProviderMap;
	}

	@Override
	public TypeSpecificMetadataProvider<Entity> getMetadataProvider(
			EntityType type) {
		return metadataProviderMap.get(type.name());
	}

	
}
