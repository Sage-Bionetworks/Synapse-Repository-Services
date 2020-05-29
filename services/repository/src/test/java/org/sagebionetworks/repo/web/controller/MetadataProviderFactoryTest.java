package org.sagebionetworks.repo.web.controller;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.SubmissionView;
import org.sagebionetworks.repo.web.service.metadata.EntityProvider;
import org.sagebionetworks.repo.web.service.metadata.MetadataProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MetadataProviderFactoryTest {

	@Autowired
	private MetadataProviderFactory metadataProviderFactory;
	
	@Test
	public void testGetProjectMetadataProvider() {
		List<EntityProvider<? extends Entity>> providers = metadataProviderFactory.getMetadataProvider(EntityTypeUtils.getEntityTypeForClass(Project.class));
		assertNotNull(providers);
		assertEquals(1, providers.size());
	}

	@Test
	public void testGetEntityViewMetadataProvider() {
		List<EntityProvider<? extends Entity>> providers = metadataProviderFactory.getMetadataProvider(EntityTypeUtils.getEntityTypeForClass(EntityView.class));
		assertNotNull(providers);
		assertEquals(1, providers.size());
	}

	@Test
	public void testGetSubmissionViewMetadataProvider() {
		List<EntityProvider<? extends Entity>> providers = metadataProviderFactory.getMetadataProvider(EntityTypeUtils.getEntityTypeForClass(SubmissionView.class));
		assertNotNull(providers);
		assertEquals(1, providers.size());
	}


}
