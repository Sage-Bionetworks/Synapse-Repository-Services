package org.sagebionetworks.repo.manager.table.metadata;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolver;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolverImpl;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableSet;

@Service
public class MetadataIndexProviderFactoryImpl implements MetadataIndexProviderFactory {

	private Map<ObjectType, MetadataIndexProvider> providersMap;

	@Autowired
	public MetadataIndexProviderFactoryImpl(List<MetadataIndexProvider> fieldTypeProviders) {
		providersMap = fieldTypeProviders.stream()
				.collect(Collectors.toMap(MetadataIndexProvider::getObjectType, provider -> provider));
	}

	@Override
	public Set<ObjectType> supportedObjectTypes() {
		return ImmutableSet.copyOf(providersMap.keySet());
	}

	@Override
	public boolean supports(ObjectType objectType) {
		return providersMap.containsKey(objectType);
	}

	@Override
	public MetadataIndexProvider getMetadataIndexProvider(ObjectType objectType) {
		MetadataIndexProvider provider = providersMap.get(objectType);

		if (provider == null) {
			throw new IllegalArgumentException(
					"MetadataIndexProvider not found for object type: " + objectType.name());
		}

		return provider;
	}
	
	@Override
	public ObjectFieldModelResolver getObjectFieldModelResolver(ObjectType objectType) {
		ObjectFieldTypeMapper fieldTypeMapper = getMetadataIndexProvider(objectType);
		return new ObjectFieldModelResolverImpl(fieldTypeMapper);
	}

}
