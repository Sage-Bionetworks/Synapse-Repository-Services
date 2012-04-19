package org.sagebionetworks.repo.web.controller;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.repo.web.controller.metadata.TypeSpecificMetadataProvider;
import org.springframework.beans.factory.InitializingBean;

public class MetadataProviderFactoryImpl implements MetadataProviderFactory,
		InitializingBean {

	private Map<String, TypeSpecificMetadataProvider<Entity>> metadataProviderMap;
	private Map<String, List<TypeSpecificMetadataProvider<Entity>>> metadataProviders;
	private TypeSpecificMetadataProvider<Entity> locationableProvider;

	/**
	 * @param metadataProviderMap
	 *            the metadataProviderMap to set
	 */
	public void setMetadataProviderMap(
			Map<String, TypeSpecificMetadataProvider<Entity>> metadataProviderMap) {
		this.metadataProviderMap = metadataProviderMap;
	}

	@Override
	public List<TypeSpecificMetadataProvider<Entity>> getMetadataProvider(
			EntityType type) {

		List<TypeSpecificMetadataProvider<Entity>> providers = metadataProviders
				.get(type.name());
		if (null == providers) {
			if (Locationable.class.isAssignableFrom(type.getClassForType())) {
				providers = new LinkedList<TypeSpecificMetadataProvider<Entity>>();
				providers.add(locationableProvider);
				metadataProviders.put(type.name(), providers);
			}

		}
		return providers;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		metadataProviders = new HashMap<String, List<TypeSpecificMetadataProvider<Entity>>>();

		locationableProvider = this.metadataProviderMap.get("locationable");

		for (Entry<String, TypeSpecificMetadataProvider<Entity>> providerEntry : metadataProviderMap
				.entrySet()) {
			if (providerEntry.getValue() == locationableProvider) {
				continue;
			}

			List<TypeSpecificMetadataProvider<Entity>> allProvidersForType = new LinkedList<TypeSpecificMetadataProvider<Entity>>();
			allProvidersForType.add(providerEntry.getValue());

			EntityType type = EntityType.getFirstTypeInUrl("/"
					+ providerEntry.getKey());
			if (Locationable.class.isAssignableFrom(type.getClassForType())) {
				allProvidersForType.add(locationableProvider);
			}

			metadataProviders.put(providerEntry.getKey(), allProvidersForType);
		}
	}

}
