package org.sagebionetworks.repo.manager.table.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.table.metadata.providers.EntityMetadataIndexProvider;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableSet;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MetadataIndexProviderFactoryTest {

	@Autowired
	private EntityMetadataIndexProvider testProvider;
	
	@Autowired
	private MetadataIndexProviderFactory factory;

	@Test
	public void testGetMetadataIndexProvider() {
		
		// Call under test
		MetadataIndexProvider provider = factory.getMetadataIndexProvider(ViewObjectType.ENTITY);
		
		assertEquals(testProvider, provider);
	}
	
	@Test
	public void testGetMetadataIndexProviderWithUnregistredType() {
		
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			factory.getMetadataIndexProvider(ViewObjectType.EVALUATION_SUBMISSIONS);
		});
	}
	
	@Test
	public void testSupportedObjectTypes() {
		Set<ViewObjectType> expected = ImmutableSet.of(ViewObjectType.ENTITY);
		assertEquals(expected, factory.supportedObjectTypes());
	}
	
	@Test
	public void testSupports() {
		for (ViewObjectType type : ViewObjectType.values()) {
			boolean result = factory.supports(type);
			// For testing we only have one provider in the classpath
			if (ViewObjectType.ENTITY == type) {
				assertTrue(result);
			} else {
				assertFalse(result);
			}
		}
	}
	
}
