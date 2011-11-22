package org.sagebionetworks.repo.model.registry;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.registry.EntityMigrationData.RenameFieldData;
import org.sagebionetworks.schema.ENCODING;
import org.sagebionetworks.schema.TYPE;

public class MigrationDataLoaderImplTest {

	@Test
	public void testLoad(){
		MigrationDataLoaderImpl loader = new MigrationDataLoaderImpl();
		EntityMigrationData migrationData = loader.loadMigrationData();
		assertNotNull(migrationData);
		 List<RenameFieldData> list = migrationData.getRenameDataForEntity(EntityType.preview);
		 assertNotNull(list);
		 assertNotNull(list.size() > 0);
		 RenameFieldData first = list.get(0);
		 assertNotNull(first);
		 assertEquals("previewBlob", first.getOldFieldName());
		 assertEquals("previewString", first.getNewFieldName());
		 assertNotNull(first.getFieldSchema());
		 assertEquals(TYPE.STRING ,first.getFieldSchema().getType());
		 assertEquals(ENCODING.BINARY ,first.getFieldSchema().getContentEncoding());
	}
	
}
