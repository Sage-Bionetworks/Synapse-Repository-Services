package org.sagebionetworks.repo.manager.table.metadata.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProviderFactory;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EntityMetadataIndexProviderTest {

	@Autowired
	private EntityMetadataIndexProvider entityProvider;
	
	@Autowired
	private MetadataIndexProviderFactory factory;
	
	@Test
	public void testWiring() {
		MetadataIndexProvider provider = factory.getMetadataIndexProvider(ViewObjectType.ENTITY);
		
		assertEquals(entityProvider, provider);
	}
	
}
