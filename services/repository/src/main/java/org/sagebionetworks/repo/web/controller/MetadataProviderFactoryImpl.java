package org.sagebionetworks.repo.web.controller;

import java.util.Map;

import org.sagebionetworks.repo.model.Nodeable;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.web.controller.metadata.TypeSpecificMetadataProvider;

public class MetadataProviderFactoryImpl implements MetadataProviderFactory {

	private Map<String, TypeSpecificMetadataProvider<Nodeable>>  metadataProviderMap;
	
	/**
	 * @param metadataProviderMap the metadataProviderMap to set
	 */
	public void setMetadataProviderMap(
			Map<String, TypeSpecificMetadataProvider<Nodeable>> metadataProviderMap) {
		this.metadataProviderMap = metadataProviderMap;
	}

	@Override
	public TypeSpecificMetadataProvider<Nodeable> getMetadataProvider(
			ObjectType type) {
		return metadataProviderMap.get(type.name());
	}

	
}
