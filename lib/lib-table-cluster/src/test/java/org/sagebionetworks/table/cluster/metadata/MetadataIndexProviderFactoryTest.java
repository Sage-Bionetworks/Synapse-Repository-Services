package org.sagebionetworks.table.cluster.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableSet;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:table-cluster-spb.xml" })
public class MetadataIndexProviderFactoryTest {

	@Autowired
	private TestEntityMetadataIndexProvider testProvider;
	
	@Autowired
	private MetadataIndexProviderFactory factory;

	@Test
	public void testGetMetadataIndexProvider() {
		
		// Call under test
		MetadataIndexProvider provider = factory.getMetadataIndexProvider(ObjectType.ENTITY);
		
		assertEquals(testProvider, provider);
	}
	
	@Test
	public void testGetMetadataIndexProviderWithUnregistredType() {
		
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			factory.getMetadataIndexProvider(ObjectType.ACCESS_APPROVAL);
		});
	}
	
	@Test
	public void testGetObjectFieldModelResolver() {
		// Call under test
		ObjectFieldModelResolver resolver = factory.getObjectFieldModelResolver(ObjectType.ENTITY);
	
		assertNotNull(resolver);
		assertEquals(ObjectFieldModelResolverImpl.class, resolver.getClass());
	}
	
	@Test
	public void testSupportedObjectTypes() {
		Set<ObjectType> expected = ImmutableSet.of(ObjectType.ENTITY);
		assertEquals(expected, factory.supportedObjectTypes());
	}
	
	@Test
	public void testSupports() {
		for (ObjectType type : ObjectType.values()) {
			boolean result = factory.supports(type);
			// For testing we only have one provider in the classpath
			if (ObjectType.ENTITY == type) {
				assertTrue(result);
			} else {
				assertFalse(result);
			}
		}
	}
	
}
