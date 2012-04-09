package org.sagebionetworks.repo.manager.backup.migration;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.*;
import org.junit.Ignore;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.registry.MigrationSpec;
import org.sagebionetworks.repo.model.registry.EntityTypeMigrationSpec;
import org.sagebionetworks.repo.model.registry.FieldMigrationSpec;
import org.sagebionetworks.repo.model.registry.FieldDescription;
import org.sagebionetworks.repo.model.registry.MigrationSpecData;

public class DataTypeMigratorTest {
	
	DataTypeMigrator dataTypeMigrator;;
	NodeRevisionBackup toMigrate;
	
	@Before
	public void before(){
//		mockType = Mockito.mock(EntityType.class);
		dataTypeMigrator = new DataTypeMigrator();
		toMigrate = new NodeRevisionBackup();
	}
	
	@Test
	public void testMigrateNonDataEntities() {
		EntityType t;
		for (EntityType type: EntityType.values()) {
			if (EntityType.layer == type)
				continue;
			// Setup node to migrate
			toMigrate.setNamedAnnotations(new NamedAnnotations());
			Annotations primaryAnnotations = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
			primaryAnnotations.addAnnotation("name", "entityName");
			primaryAnnotations.addAnnotation("id", "entityId");
			
			Map<String, List<String>> stringAnnos = primaryAnnotations.getStringAnnotations();
			assertNotNull(stringAnnos.get("name"));
			assertNotNull(stringAnnos.get("id"));

			t = EntityType.valueOf(type.toString());
			dataTypeMigrator.migrateOneStep(toMigrate, type);

			assertNotNull(stringAnnos.get("name"));
			assertNotNull(stringAnnos.get("id"));
			assertEquals(t, type);
		}
		return;
	}
	
	@Test
	public void testMigrateClinicalData() {
		EntityType type, t;
		type = EntityType.layer;
		toMigrate.setNamedAnnotations(new NamedAnnotations());
		Annotations primaryAnnotations = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
		primaryAnnotations.addAnnotation("name", "entityName");
		primaryAnnotations.addAnnotation("id", "entityId");
		primaryAnnotations.addAnnotation("type", "C");
		primaryAnnotations.addAnnotation("species", "entitySpecies");
		primaryAnnotations.addAnnotation("tissueType", "entityTissue");
		primaryAnnotations.addAnnotation("disease", "entityDisease");
		primaryAnnotations.addAnnotation("platform", "entityPlatform");
		

		t = EntityType.valueOf(type.toString());
		type = dataTypeMigrator.migrateOneStep(toMigrate, type);
		
		assertFalse(t.equals(type));
		assertEquals(type, EntityType.phenotypedata);
		assertNull(primaryAnnotations.getSingleValue("tissueType"));
		assertNull(primaryAnnotations.getSingleValue("platform"));
		assertNull(primaryAnnotations.getSingleValue("type"));
	}
	
	@Test
	public void testMigrateGenotypeData() {
		EntityType type, t;
		type = EntityType.layer;
		toMigrate.setNamedAnnotations(new NamedAnnotations());
		Annotations primaryAnnotations = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
		primaryAnnotations.addAnnotation("name", "entityName");
		primaryAnnotations.addAnnotation("id", "entityId");
		primaryAnnotations.addAnnotation("type", "G");
		primaryAnnotations.addAnnotation("species", "entitySpecies");
		primaryAnnotations.addAnnotation("tissueType", "entityTissue");
		primaryAnnotations.addAnnotation("disease", "entityDisease");
		primaryAnnotations.addAnnotation("platform", "entityPlatform");
		

		t = EntityType.valueOf(type.toString());
		type = dataTypeMigrator.migrateOneStep(toMigrate, type);
		
		assertFalse(t.equals(type));
		assertEquals(type, EntityType.genotypedata);
		assertNotNull(primaryAnnotations.getSingleValue("species"));
		assertNotNull(primaryAnnotations.getSingleValue("disease"));
		assertNotNull(primaryAnnotations.getSingleValue("platform"));
		assertNull(primaryAnnotations.getSingleValue("tissueType"));
		assertNull(primaryAnnotations.getSingleValue("type"));
	}
	
	@Test
	public void testMigrateExpressionData() {
		EntityType type, t;
		type = EntityType.layer;
		toMigrate.setNamedAnnotations(new NamedAnnotations());
		Annotations primaryAnnotations = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
		primaryAnnotations.addAnnotation("name", "entityName");
		primaryAnnotations.addAnnotation("id", "entityId");
		primaryAnnotations.addAnnotation("type", "E");
		primaryAnnotations.addAnnotation("species", "entitySpecies");
		primaryAnnotations.addAnnotation("tissueType", "entityTissue");
		primaryAnnotations.addAnnotation("disease", "entityDisease");
		primaryAnnotations.addAnnotation("platform", "entityPlatform");
		

		t = EntityType.valueOf(type.toString());
		type = dataTypeMigrator.migrateOneStep(toMigrate, type);
		
		assertFalse(t.equals(type));
		assertEquals(type, EntityType.expressiondata);
		assertNotNull(primaryAnnotations.getSingleValue("species"));
		assertNotNull(primaryAnnotations.getSingleValue("disease"));
		assertNotNull(primaryAnnotations.getSingleValue("platform"));
		assertNotNull(primaryAnnotations.getSingleValue("tissueType"));
		assertNull(primaryAnnotations.getSingleValue("type"));
	}
	

}
