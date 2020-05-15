package org.sagebionetworks.table.cluster.metadata;

import org.springframework.stereotype.Service;

@Service
public class ObjectFieldModelResolverFactoryImpl implements ObjectFieldModelResolverFactory {

	@Override
	public ObjectFieldModelResolver getObjectFieldModelResolver(ObjectFieldTypeMapper fieldTypeMapper) {
		return new ObjectFieldModelResolverImpl(fieldTypeMapper);
	}

}
