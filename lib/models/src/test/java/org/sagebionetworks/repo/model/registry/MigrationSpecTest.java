package org.sagebionetworks.repo.model.registry;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

public class MigrationSpecTest {
	
	@Test
	public void testMigrationSpecData() throws JSONObjectAdapterException {
		MigrationSpec ms = new MigrationSpec();
		List<EntityTypeMigrationSpec> listEms = new ArrayList<EntityTypeMigrationSpec>();
		EntityTypeMigrationSpec ems = new EntityTypeMigrationSpec();
		ems.setEntityType("project");
		List<FieldMigrationSpec> listFms = new ArrayList<FieldMigrationSpec>();
		FieldMigrationSpec fms = new FieldMigrationSpec();
		FieldDescription source = new FieldDescription();
		FieldDescription dest = new FieldDescription();
		source.setName("num_samples");
		source.setType("long");
		source.setBucket("primary");
		dest.setName("numSamples");
		dest.setType("long");
		dest.setBucket("primary");
		fms.setSource(source);
		fms.setDestination(dest);
		listFms.add(fms);
		listEms.add(ems);
		ems.setFields(listFms);
		ms.setMigrationMetadata(listEms);

		String json = EntityFactory.createJSONStringForEntity(ms);
		System.out.println(json);
		MigrationSpec clone = EntityFactory.createEntityFromJSONString(json, MigrationSpec.class);
		assertNotNull(clone);
		assertEquals(ms, clone);

		return;
	}
	
	@Test
	public void testCreateFromString() throws JSONObjectAdapterException {
		String json = "{\"migrationMetadata\":[{\"entityType\":\"project\",\"fields\":[{\"source\":{\"name\":\"num_samples\",\"type\":\"long\",\"bucket\":\"primary\"},\"destination\":{\"name\":\"numSamples\",\"type\":\"long\",\"bucket\":\"primary\"}}]}]}";
		System.out.print(json);
		MigrationSpec ms = EntityFactory.createEntityFromJSONString(json, MigrationSpec.class);
		assertNotNull(ms);
		List<EntityTypeMigrationSpec> l = ms.getMigrationMetadata();
		assertEquals(l.size(), 1);
	}
	
	@Test
	public void testEmptyFieldList() throws JSONObjectAdapterException {
		
		MigrationSpec ms = new MigrationSpec();
		List<EntityTypeMigrationSpec> listEms = new ArrayList<EntityTypeMigrationSpec>();
		EntityTypeMigrationSpec ems = new EntityTypeMigrationSpec();
		ems.setEntityType("project");
		List<FieldMigrationSpec> listFms = new ArrayList<FieldMigrationSpec>();
		ems.setFields(listFms);
		listEms.add(ems);
		ms.setMigrationMetadata(listEms);

		String json = EntityFactory.createJSONStringForEntity(ms);
		System.out.println(json);
		MigrationSpec clone = EntityFactory.createEntityFromJSONString(json, MigrationSpec.class);
		assertNotNull(clone);
		assertEquals(ms, clone);

		return;
	}
}
