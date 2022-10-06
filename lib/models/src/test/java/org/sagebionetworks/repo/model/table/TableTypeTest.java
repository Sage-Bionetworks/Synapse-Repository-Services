package org.sagebionetworks.repo.model.table;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.TableType;

public class TableTypeTest {

	@Test
	public void testGetEntityType() {
		// each type should have a matching entity type
		for (TableType type : TableType.values()) {
			assertNotNull(type.getEntityType());
			assertEquals(type.name(), type.getEntityType().name());
		}
	}

	@Test
	public void testObjectTypes() {
		assertEquals(TableType.table.getObjectType(), ObjectType.TABLE);
		assertEquals(TableType.entityview.getObjectType(), ObjectType.ENTITY_VIEW);
		assertEquals(TableType.submissionview.getObjectType(), ObjectType.ENTITY_VIEW);
		assertEquals(TableType.dataset.getObjectType(), ObjectType.ENTITY_VIEW);
		assertEquals(TableType.datasetcollection.getObjectType(), ObjectType.ENTITY_VIEW);
		assertEquals(TableType.materializedview.getObjectType(), ObjectType.MATERIALIZED_VIEW);
	}
	
	@Test
	public void testLookupByEntityType() {
		for (TableType type : TableType.values()) {
			assertNotNull(type.getEntityType());
			assertEquals(Optional.of(type), TableType.lookupByEntityType(type.getEntityType()));
		}
	}

	
	@Test
	public void testLookupByEntityTypeWithNonTable() {
		assertEquals(Optional.empty(), TableType.lookupByEntityType(EntityType.project));
		assertEquals(Optional.empty(), TableType.lookupByEntityType(EntityType.folder));
		assertEquals(Optional.empty(), TableType.lookupByEntityType(EntityType.file));
	}
	
	@Test
	public void testGetViewEntityType() {
		for (ViewEntityType viewType : ViewEntityType.values()) {
			TableType type = TableType.valueOf(viewType.name());
			assertEquals(viewType.name(), type.name());
			assertTrue(type.isViewEntityType());
		}
	}
	
	@Test
	public void testIsViewEntityType() {
		assertTrue(TableType.entityview.isViewEntityType());
		assertFalse(TableType.table.isViewEntityType());
		assertFalse(TableType.materializedview.isViewEntityType());
	}
}
