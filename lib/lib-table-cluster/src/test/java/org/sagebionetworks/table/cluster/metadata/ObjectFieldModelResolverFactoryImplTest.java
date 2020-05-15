package org.sagebionetworks.table.cluster.metadata;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ObjectFieldModelResolverFactoryImplTest {
	
	@Mock
	private ObjectFieldTypeMapper typeMapper;

	@InjectMocks
	private ObjectFieldModelResolverFactoryImpl factory;
	
	@Test
	public void testGetInstance() {
		
		ObjectFieldModelResolver instance = factory.getObjectFieldModelResolver(typeMapper);
		
		assertNotNull(instance);
	}
	
}
