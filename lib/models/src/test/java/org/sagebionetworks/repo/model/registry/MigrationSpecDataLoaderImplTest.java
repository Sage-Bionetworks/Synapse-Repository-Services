package org.sagebionetworks.repo.model.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.registry.MigrationSpecData.FieldMigrationSpecData;

public class MigrationSpecDataLoaderImplTest {
	
	@Test
	public void testLoader() {
		MigrationSpecDataLoaderImpl loader = new MigrationSpecDataLoaderImpl();
		MigrationSpecData msd = loader.loadMigrationSpecData();
		assertNotNull(msd);
		List<FieldMigrationSpecData> migrationSpecData = msd.getData(EntityType.project);
		assertEquals(2, migrationSpecData.size());
		migrationSpecData = msd.getData(EntityType.dataset);
		assertEquals(25, migrationSpecData.size());
		migrationSpecData = msd.getData(EntityType.layer);
		assertEquals(18, migrationSpecData.size());
		migrationSpecData = msd.getData(EntityType.preview);
		assertEquals(1, migrationSpecData.size());
		migrationSpecData = msd.getData(EntityType.analysis);
		assertEquals(1, migrationSpecData.size());
		migrationSpecData = msd.getData(EntityType.code);
		assertEquals(1, migrationSpecData.size());
		migrationSpecData = msd.getData(EntityType.step);
		assertEquals(1, migrationSpecData.size());
		return;
	}
	
	@Test
	public void testPrimaryToDelete() {
		MigrationSpecDataLoaderImpl loader = new MigrationSpecDataLoaderImpl();
		MigrationSpecData msd = loader.loadMigrationSpecData();
		assertNotNull(msd);
		List<FieldMigrationSpecData> migrationSpecData = msd.getData(EntityType.project);
		assertEquals(2, migrationSpecData.size());
		migrationSpecData = msd.getData(EntityType.project);
		List<String> ld = msd.getPrimaryFieldsToDelete(EntityType.project);
		assertEquals(2, ld.size());
		return;
	}
}
