package org.sagebionetworks.repo.web.controller;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.Code;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.web.controller.metadata.EntityProvider;
import org.sagebionetworks.repo.web.controller.metadata.MetadataProviderFactory;
import org.sagebionetworks.repo.web.controller.metadata.TypeSpecificMetadataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
		List<EntityProvider<Entity>> providers = metadataProviderFactory.getMetadataProvider(EntityType.getNodeTypeForClass(Project.class));
		assertNotNull(providers);
		assertEquals(1, providers.size());
	}


	@Test
	public void testGetDatasetMetadataProvider() {
		List<EntityProvider<Entity>> providers = metadataProviderFactory.getMetadataProvider(EntityType.getNodeTypeForClass(Study.class));
		assertNotNull(providers);
		assertEquals(2, providers.size());
	}

	@Test
	public void testGetLayerMetadataProvider() {
		List<EntityProvider<Entity>> providers = metadataProviderFactory.getMetadataProvider(EntityType.getNodeTypeForClass(Data.class));
		assertNotNull(providers);
		assertEquals(2, providers.size());
	}

	@Test
	public void testGetCodeMetadataProvider() {
		List<EntityProvider<Entity>> providers = metadataProviderFactory.getMetadataProvider(EntityType.getNodeTypeForClass(Code.class));
		assertNotNull(providers);
		assertEquals(2, providers.size());
	}

}
