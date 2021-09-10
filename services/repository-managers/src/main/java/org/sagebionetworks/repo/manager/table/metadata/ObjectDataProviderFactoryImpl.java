package org.sagebionetworks.repo.manager.table.metadata;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.table.ReplicationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ObjectDataProviderFactoryImpl implements ObjectDataProviderFactory {
	
	private Map<ReplicationType, ObjectDataProvider> providersMap;
	
	@Autowired
	public ObjectDataProviderFactoryImpl(List<ObjectDataProvider> providers) {
		providersMap = providers.stream()
				.collect(Collectors.toMap(ObjectDataProvider::getReplicationType, provider -> provider));
	}

	@Override
	public ObjectDataProvider getObjectDataProvider(ReplicationType type) {
		ObjectDataProvider proivder =  providersMap.get(type);
		if(providersMap == null) {
			throw new IllegalArgumentException("Provider not found for: "+type);
		}
		return proivder;
	}

}
