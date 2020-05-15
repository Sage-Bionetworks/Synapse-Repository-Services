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
	
	// We use setter injection here since we have a circular dependency:
	//
	// NodeManager -> AuthorizationManager -> FileHandleAssociationManager -> 
	// TableFileHandleAssociationProvider -> TableEntityManager -> NodeManager
	//
	// Note that other FileHandleAssociationProvider in the FileHandleAssociationManager
	// have dependencies that circle back to the NodeManager.
	// 
	// Also note that this introduces other circular dependencies, for example:
	//
	// MetadataIndexProviderFactory -> EntityMetadataIndexProvider -> NodeManager -> 
	// AuthorizationManager -> FileHandleAssociationManager ->  TableFileHandleAssociationProvider -> 
	// TableEntityManager -> TableManagerSupport -> MetadataIndexProviderFactory
	//
	// This used to work since most of this components used field injection which often hide the circular dependencies, 
	// components should in general use constructor injection which also help in discovering circular dependencies 
	// (since spring will throw a BeanCurrentlyInCreationException during initialization).
	// 
	// Note that spring bean resolution is not deterministic so it might work locally and fail in the build
	// environment, the setter injection below allows to bypass the above circular dependency.
	@Autowired
	public void setProvidersMap(List<MetadataIndexProvider> fieldTypeProviders) {
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
