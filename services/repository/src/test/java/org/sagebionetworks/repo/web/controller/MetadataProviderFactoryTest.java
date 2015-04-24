package org.sagebionetworks.repo.web.controller;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.web.service.metadata.EntityProvider;
import org.sagebionetworks.repo.web.service.metadata.MetadataProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class MetadataProviderFactoryTest extends AbstractAutowiredControllerTestBase {

	@Autowired
	private MetadataProviderFactory metadataProviderFactory;
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testGetProjectMetadataProvider() {
		List<EntityProvider<Entity>> providers = metadataProviderFactory.getMetadataProvider(EntityType.getEntityTypeForClass(Project.class));
		assertNotNull(providers);
		assertEquals(1, providers.size());
	}


}
