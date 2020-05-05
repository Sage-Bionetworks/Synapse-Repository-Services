package org.sagebionetworks.table.cluster.metadata;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ObjectFieldModelProviderFactoryImpl implements ObjectFieldModelProviderFactory {

	private Map<ObjectType, ObjectFieldModelProvider> providersMap;

	@Autowired
	public ObjectFieldModelProviderFactoryImpl(List<ObjectFieldTypeMapper> fieldTypeProviders) {
		providersMap = fieldTypeProviders.stream()
				.collect(Collectors.toMap(ObjectFieldTypeMapper::getObjectType, this::buildObjectFieldModelProvider));
	}

	@Override
	public ObjectFieldModelProvider getObjectFieldModelProvider(ObjectType objectType) {
		ObjectFieldModelProvider provider = providersMap.get(objectType);

		if (provider == null) {
			throw new IllegalArgumentException(
					"ObjectFieldModelProvider not found for object type: " + objectType.name());
		}

		return provider;
	}

	private ObjectFieldModelProvider buildObjectFieldModelProvider(ObjectFieldTypeMapper fieldTypeProvider) {
		return new ObjectFieldModelProviderImpl(fieldTypeProvider);
	}

}
