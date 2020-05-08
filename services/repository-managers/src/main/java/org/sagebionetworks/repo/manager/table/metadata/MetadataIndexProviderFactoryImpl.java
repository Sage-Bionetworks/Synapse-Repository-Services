package org.sagebionetworks.repo.manager.table.metadata;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableSet;

@Service
public class MetadataIndexProviderFactoryImpl implements MetadataIndexProviderFactory {

	private Map<ViewObjectType, MetadataIndexProvider> providersMap;

	@Autowired
	public MetadataIndexProviderFactoryImpl(List<MetadataIndexProvider> fieldTypeProviders) {
		providersMap = fieldTypeProviders.stream()
				.collect(Collectors.toMap(MetadataIndexProvider::getObjectType, provider -> provider));
	}

	@Override
	public Set<ViewObjectType> supportedObjectTypes() {
		return ImmutableSet.copyOf(providersMap.keySet());
	}

	@Override
	public boolean supports(ViewObjectType objectType) {
		return providersMap.containsKey(objectType);
	}

	@Override
	public MetadataIndexProvider getMetadataIndexProvider(ViewObjectType objectType) {
		MetadataIndexProvider provider = providersMap.get(objectType);

		if (provider == null) {
			throw new IllegalArgumentException(
					"MetadataIndexProvider not found for object type: " + objectType.name());
		}

		return provider;
	}

}
