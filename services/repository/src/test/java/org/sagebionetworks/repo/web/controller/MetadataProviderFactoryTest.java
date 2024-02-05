package org.sagebionetworks.repo.web.controller;


import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.table.Dataset;
import org.sagebionetworks.repo.model.table.DatasetCollection;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.MaterializedView;
import org.sagebionetworks.repo.model.table.SubmissionView;
import org.sagebionetworks.repo.model.table.VirtualTable;
import org.sagebionetworks.repo.web.service.metadata.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MetadataProviderFactoryTest {

	@Autowired
	private MetadataProviderFactory metadataProviderFactory;
	
	@Test
	public void testGetProjectMetadataProvider() {
		Optional<EntityProvider<? extends Entity>> provider = metadataProviderFactory.getMetadataProvider(EntityTypeUtils.getEntityTypeForClass(Project.class));

		assertTrue(provider.isPresent());
		assertTrue(provider.get() instanceof ProjectMetadataProvider);
	}

	@Test
	public void testGetEntityViewMetadataProvider() {
		Optional<EntityProvider<? extends Entity>> provider = metadataProviderFactory.getMetadataProvider(EntityTypeUtils.getEntityTypeForClass(EntityView.class));

		assertTrue(provider.isPresent());
		assertTrue(provider.get() instanceof EntityViewMetadataProvider);
	}

	@Test
	public void testGetSubmissionViewMetadataProvider() {
		Optional<EntityProvider<? extends Entity>> provider = metadataProviderFactory.getMetadataProvider(EntityTypeUtils.getEntityTypeForClass(SubmissionView.class));

		assertTrue(provider.isPresent());
		assertTrue(provider.get() instanceof SubmissionViewMetadataProvider);
	}
	
	@Test
	public void testGetMaterializedViewMetadataProvider() {
		Optional<EntityProvider<? extends Entity>> provider = metadataProviderFactory.getMetadataProvider(EntityTypeUtils.getEntityTypeForClass(MaterializedView.class));

		assertTrue(provider.isPresent());
		assertTrue(provider.get() instanceof MaterializedViewMetadataProvider);
	}
	
	@Test
	public void testGetVirtualTableMetadataProvider() {
		Optional<EntityProvider<? extends Entity>> provider = metadataProviderFactory.getMetadataProvider(EntityTypeUtils.getEntityTypeForClass(VirtualTable.class));

		assertTrue(provider.isPresent());
		assertTrue(provider.get() instanceof VirtualTableMetadataProvider);
	}
	
	@Test
	public void testGetDatasetMetadataProvider() {
		Optional<EntityProvider<? extends Entity>> provider = metadataProviderFactory.getMetadataProvider(EntityTypeUtils.getEntityTypeForClass(Dataset.class));

		assertTrue(provider.isPresent());
		assertTrue(provider.get() instanceof DatasetMetadataProvider);
	}
	
	@Test
	public void testGetDatasetCollectionMetadataProvider() {
		Optional<EntityProvider<? extends Entity>> provider = metadataProviderFactory.getMetadataProvider(EntityTypeUtils.getEntityTypeForClass(DatasetCollection.class));

		assertTrue(provider.isPresent());
		assertTrue(provider.get() instanceof DatasetCollectionMetadataProvider);
	}


}
