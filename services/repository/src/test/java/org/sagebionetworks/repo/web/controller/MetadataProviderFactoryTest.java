package org.sagebionetworks.repo.web.controller;


import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.Code;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.web.controller.metadata.TypeSpecificMetadataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MetadataProviderFactoryTest {

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
		List<TypeSpecificMetadataProvider<Entity>> providers = metadataProviderFactory.getMetadataProvider(EntityType.getNodeTypeForClass(Project.class));
		assertNotNull(providers);
		assertEquals(1, providers.size());
	}


	@Test
	public void testGetDatasetMetadataProvider() {
		List<TypeSpecificMetadataProvider<Entity>> providers = metadataProviderFactory.getMetadataProvider(EntityType.getNodeTypeForClass(Dataset.class));
		assertNotNull(providers);
		assertEquals(2, providers.size());
	}

	@Test
	public void testGetLayerMetadataProvider() {
		List<TypeSpecificMetadataProvider<Entity>> providers = metadataProviderFactory.getMetadataProvider(EntityType.getNodeTypeForClass(Layer.class));
		assertNotNull(providers);
		assertEquals(2, providers.size());
	}

	@Test
	public void testGetCodeMetadataProvider() {
		List<TypeSpecificMetadataProvider<Entity>> providers = metadataProviderFactory.getMetadataProvider(EntityType.getNodeTypeForClass(Code.class));
		assertNotNull(providers);
		assertEquals(2, providers.size());
	}

}
