package org.sagebionetworks.repo.manager.table.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MetadataIndexProviderFactoryTest {

	@Autowired
	private MetadataIndexProviderFactory factory;

	@Test
	public void testGetMetadataIndexProvider() {

		for (ViewObjectType type : ViewObjectType.values()) {
			// Call under test
			MetadataIndexProvider provider = factory.getMetadataIndexProvider(type);
			assertEquals(type, provider.getObjectType());
		}
	}

	@Test
	public void testSupportedObjectTypes() {
		Set<ViewObjectType> expected = Stream.of(ViewObjectType.values()).collect(Collectors.toSet());
		assertEquals(expected, factory.supportedObjectTypes());
	}

	@Test
	public void testSupports() {
		for (ViewObjectType type : ViewObjectType.values()) {
			boolean result = factory.supports(type);
			assertTrue(result);
		}
	}

}
