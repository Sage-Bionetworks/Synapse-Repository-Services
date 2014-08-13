package org.sagebionetworks.repo.web.controller.metadata;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Locationable;
import org.springframework.beans.factory.InitializingBean;

public class MetadataProviderFactoryImpl implements MetadataProviderFactory,
		InitializingBean {

	private Map<String, EntityProvider<Entity>> metadataProviderMap;
	private Map<String, List<EntityProvider<Entity>>> metadataProviders;
	private EntityProvider<Entity> locationableProvider;

	/**
	 * @param metadataProviderMap
	 *            the metadataProviderMap to set
	 */
	public void setMetadataProviderMap(Map<String, EntityProvider<Entity>> metadataProviderMap) {
		this.metadataProviderMap = metadataProviderMap;
	}

	@Override
	public List<EntityProvider<Entity>> getMetadataProvider(
			EntityType type) {

		List<EntityProvider<Entity>> providers = metadataProviders.get(type.name());
		if (null == providers) {
			if (Locationable.class.isAssignableFrom(type.getClassForType())) {
				providers = new LinkedList<EntityProvider<Entity>>();
				providers.add(locationableProvider);
				metadataProviders.put(type.name(), providers);
			}

		}
		return providers;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		metadataProviders = new HashMap<String, List<EntityProvider<Entity>>>();

		locationableProvider = this.metadataProviderMap.get("locationable");

		for (Entry<String, EntityProvider<Entity>> providerEntry : metadataProviderMap
				.entrySet()) {
			if (providerEntry.getValue() == locationableProvider) {
				continue;
			}

			List<EntityProvider<Entity>> allProvidersForType = new LinkedList<EntityProvider<Entity>>();
			allProvidersForType.add(providerEntry.getValue());

			EntityType type = EntityType.valueOf(providerEntry.getKey());
			if (Locationable.class.isAssignableFrom(type.getClassForType())) {
				allProvidersForType.add(locationableProvider);
			}

			metadataProviders.put(providerEntry.getKey(), allProvidersForType);
		}
	}

}
