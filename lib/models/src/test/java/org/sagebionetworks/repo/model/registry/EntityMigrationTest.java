package org.sagebionetworks.repo.model.registry;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

public class EntityMigrationTest {
	
	@Test
	public void testMigrationData() throws JSONObjectAdapterException{
		EntityMigration em = new EntityMigration();
		em.setToRename(new ArrayList<RenameData>());
		// Rename the preview blob to preview string
		RenameData renameData = new RenameData();
		renameData.setEntityTypeName(EntityType.preview.name());
		renameData.setOldFieldName("previewBlob");
		renameData.setNewFieldName("previewString");
		em.getToRename().add(renameData);
		
		renameData = new RenameData();
		renameData.setEntityTypeName(EntityType.eula.name());
		renameData.setOldFieldName("agreementBlob");
		renameData.setNewFieldName("agreement");
		em.getToRename().add(renameData);
		
		// Write out the datat to JSON
		String json = EntityFactory.createJSONStringForEntity(em);
		System.out.println(json);
		EntityMigration clone = EntityFactory.createEntityFromJSONString(json, EntityMigration.class);
		assertNotNull(clone);
		assertEquals(em, clone);
	}

}
