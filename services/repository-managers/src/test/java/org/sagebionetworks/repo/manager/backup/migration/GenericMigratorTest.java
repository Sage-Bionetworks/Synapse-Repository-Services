package org.sagebionetworks.repo.manager.backup.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.registry.EntityTypeMigrationSpec;
import org.sagebionetworks.repo.model.registry.FieldDescription;
import org.sagebionetworks.repo.model.registry.FieldMigrationSpec;
import org.sagebionetworks.repo.model.registry.MigrationSpec;
import org.sagebionetworks.repo.model.registry.MigrationSpecData;

public class GenericMigratorTest {
	
	GenericMigrator genericMigrator;;
	NodeRevisionBackup toMigrate;
	
	@Before
	public void before(){
//		mockType = Mockito.mock(EntityType.class);
		genericMigrator = new GenericMigrator();
		toMigrate = new NodeRevisionBackup();
	}
	
	@Test
	public void testMigratePrimaryToPrimaryString() {
		for (EntityType type: EntityType.values()) {
			// Setup migration spec
//			MigrationSpec ms = MigratorTestHelper.setupMigrationSpec(type.name(), "primary", "primary");
			MigrationSpec ms = new MigrationSpec();
			List<EntityTypeMigrationSpec> listEms = new ArrayList<EntityTypeMigrationSpec>();
			EntityTypeMigrationSpec ems = new EntityTypeMigrationSpec();
			ems.setEntityType(type.name());
			List<FieldMigrationSpec> listFms = new ArrayList<FieldMigrationSpec>();
			FieldMigrationSpec fms = new FieldMigrationSpec();
			FieldDescription source = new FieldDescription();
			FieldDescription dest = new FieldDescription();
			source.setName("old_name");
			source.setType("string");
			source.setBucket("primary");
			dest.setName("name");
			dest.setType("string");
			dest.setBucket("primary");
			fms.setSource(source);
			fms.setDestination(dest);
			listFms.add(fms);
			listEms.add(ems);
			ems.setFields(listFms);
			ms.setMigrationMetadata(listEms);			
			MigrationSpecData msd = new MigrationSpecData();
			msd.setData(ms);
			genericMigrator.setMigrationSpecData(msd);
			
			// Setup node to migrate
			
//			toMigrate = MigratorTestHelper.setupNodeRevisionToMigrate();
			
			String oldKey = "old_name";
			String newKey = "name";
			String valueToMigrate = "String to migrate";
			toMigrate.setNamedAnnotations(new NamedAnnotations());
			Annotations primaryAnnotations = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
			primaryAnnotations.addAnnotation(oldKey, valueToMigrate);
			
			Map<String, List<String>> stringAnnos = primaryAnnotations.getStringAnnotations();
			assertNotNull(stringAnnos.get(oldKey));
			genericMigrator.migrateOneStep(toMigrate, type);
			assertNull(stringAnnos.get(oldKey));
			assertNotNull(stringAnnos.get(newKey));
			assertEquals(1, stringAnnos.get(newKey).size());
			assertEquals(valueToMigrate, stringAnnos.get(newKey).iterator().next());
		}
		return;
	}
	
	@Test
	public void testMigratePrimaryToAdditionalString() {
		for (EntityType type: EntityType.values()) {
			toMigrate.setNamedAnnotations(new NamedAnnotations());
			MigrationSpec ms = new MigrationSpec();
			List<EntityTypeMigrationSpec> listEms = new ArrayList<EntityTypeMigrationSpec>();
			EntityTypeMigrationSpec ems = new EntityTypeMigrationSpec();
			ems.setEntityType(type.name());
			List<FieldMigrationSpec> listFms = new ArrayList<FieldMigrationSpec>();
			FieldMigrationSpec fms = new FieldMigrationSpec();
			FieldDescription source = new FieldDescription();
			FieldDescription dest = new FieldDescription();
			source.setName("old_name");
			source.setType("string");
			source.setBucket("primary");
			dest.setName("new_name");
			dest.setType("string");
			dest.setBucket("additional");
			fms.setSource(source);
			fms.setDestination(dest);
			listFms.add(fms);
			listEms.add(ems);
			ems.setFields(listFms);
			ms.setMigrationMetadata(listEms);
			MigrationSpecData msd = new MigrationSpecData();
			msd.setData(ms);
			genericMigrator.setMigrationSpecData(msd);
			String oldKey = "old_name";
			String newKey = "new_name";
			String valueToMigrate = "Value to be migrated";
			Annotations primaryAnnotations = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
			Annotations additionalAnnotations = toMigrate.getNamedAnnotations().getAdditionalAnnotations();
			primaryAnnotations.addAnnotation(oldKey, valueToMigrate);
			Map<String, List<String>> primaryStringAnnos = primaryAnnotations.getStringAnnotations();
			Map<String, List<String>> additionalStringAnnos = additionalAnnotations.getStringAnnotations();
			assertNotNull(primaryStringAnnos.get(oldKey));
			genericMigrator.migrateOneStep(toMigrate, type);
			assertNull(primaryStringAnnos.get(oldKey));
			assertNotNull(additionalStringAnnos.get(newKey));
			assertEquals(1, additionalStringAnnos.get(newKey).size());
			assertEquals(valueToMigrate, additionalStringAnnos.get(newKey).iterator().next());
		}
		return;
	}
	
	@Test
	@Ignore
	public void testMigratePrimaryToPrimaryArrayString() {
		for (EntityType type: EntityType.values()) {
			toMigrate.setNamedAnnotations(new NamedAnnotations());
			MigrationSpec ms = new MigrationSpec();
			List<EntityTypeMigrationSpec> listEms = new ArrayList<EntityTypeMigrationSpec>();
			EntityTypeMigrationSpec ems = new EntityTypeMigrationSpec();
			ems.setEntityType(type.name());
			List<FieldMigrationSpec> listFms = new ArrayList<FieldMigrationSpec>();
			FieldMigrationSpec fms = new FieldMigrationSpec();
			FieldDescription source = new FieldDescription();
			FieldDescription dest = new FieldDescription();
			source.setName("old_name");
			source.setType("string");
			source.setBucket("primary");
			dest.setName("name");
			dest.setType("string");
			dest.setBucket("primary");
			fms.setSource(source);
			fms.setDestination(dest);
			listFms.add(fms);
			listEms.add(ems);
			ems.setFields(listFms);
			ms.setMigrationMetadata(listEms);
			MigrationSpecData msd = new MigrationSpecData();
			msd.setData(ms);
			genericMigrator.setMigrationSpecData(msd);
			String oldKey = "old_name";
			String newKey = "name";
			List<String> valuesToMigrate = new ArrayList<String>();
			valuesToMigrate.add("Value1 to be migrated");
			valuesToMigrate.add("Value2 to be migrated");
			valuesToMigrate.add("Value3 to be migrated");
			Annotations primaryAnnotations = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
			primaryAnnotations.addAnnotation(oldKey, valuesToMigrate);
			Map<String, List<String>> primaryStringAnnos = primaryAnnotations.getStringAnnotations();
			assertNotNull(primaryStringAnnos.get(oldKey));
			genericMigrator.migrateOneStep(toMigrate, type);
			assertNull(primaryStringAnnos.get(oldKey));
			assertNotNull(primaryStringAnnos.get(newKey));
			assertEquals(3, primaryStringAnnos.get(newKey).size());
			assertEquals(valuesToMigrate.iterator().next(), primaryStringAnnos.get(newKey).iterator().next());
		}
	}
	
	@Test
	@Ignore
	public void testMigrateAdditionalToAdditionalArrayString() {
		for (EntityType type: EntityType.values()) {
			toMigrate.setNamedAnnotations(new NamedAnnotations());
			MigrationSpec ms = new MigrationSpec();
			List<EntityTypeMigrationSpec> listEms = new ArrayList<EntityTypeMigrationSpec>();
			EntityTypeMigrationSpec ems = new EntityTypeMigrationSpec();
			ems.setEntityType(type.name());
			List<FieldMigrationSpec> listFms = new ArrayList<FieldMigrationSpec>();
			FieldMigrationSpec fms = new FieldMigrationSpec();
			FieldDescription source = new FieldDescription();
			FieldDescription dest = new FieldDescription();
			source.setName("old_name");
			source.setType("string");
			source.setBucket("additional");
			dest.setName("new_name");
			dest.setType("string");
			dest.setBucket("additional");
			fms.setSource(source);
			fms.setDestination(dest);
			listFms.add(fms);
			listEms.add(ems);
			ems.setFields(listFms);
			ms.setMigrationMetadata(listEms);
			MigrationSpecData msd = new MigrationSpecData();
			msd.setData(ms);
			genericMigrator.setMigrationSpecData(msd);
			String oldKey = "old_name";
			String newKey = "new_name";
			List<String> valuesToMigrate = new ArrayList<String>();
			valuesToMigrate.add("Value1 to be migrated");
			valuesToMigrate.add("Value2 to be migrated");
			valuesToMigrate.add("Value3 to be migrated");
			Annotations additionalAnnotations = toMigrate.getNamedAnnotations().getAdditionalAnnotations();
			additionalAnnotations.addAnnotation(oldKey, valuesToMigrate);
			Map<String, List<String>> additionalStringAnnos = additionalAnnotations.getStringAnnotations();
			assertNotNull(additionalStringAnnos.get(oldKey));
			genericMigrator.migrateOneStep(toMigrate, type);
			assertNull(additionalStringAnnos.get(oldKey));
			assertNotNull(additionalStringAnnos.get(newKey));
			assertEquals(3, additionalStringAnnos.get(newKey).size());
			assertEquals(valuesToMigrate.iterator().next(), additionalStringAnnos.get(newKey).iterator().next());
		}
		return;
	}
	
	@Test
	@Ignore
	public void testMigrateDatasetAdditionalToPrimaryArrayString() {
		toMigrate.setNamedAnnotations(new NamedAnnotations());
		EntityType type = EntityType.dataset;
		MigrationSpec ms = new MigrationSpec();
		List<EntityTypeMigrationSpec> listEms = new ArrayList<EntityTypeMigrationSpec>();
		EntityTypeMigrationSpec ems = new EntityTypeMigrationSpec();
		ems.setEntityType(type.name());
		List<FieldMigrationSpec> listFms = new ArrayList<FieldMigrationSpec>();
		FieldMigrationSpec fms = new FieldMigrationSpec();
		FieldDescription source = new FieldDescription();
		FieldDescription dest = new FieldDescription();
		source.setName("Disease");
		source.setType("string");
		source.setBucket("additional");
		dest.setName("diseases");
		dest.setType("string");
		dest.setBucket("primary");
		fms.setSource(source);
		fms.setDestination(dest);
		listFms.add(fms);
		listEms.add(ems);
		ems.setFields(listFms);
		ms.setMigrationMetadata(listEms);
		MigrationSpecData msd = new MigrationSpecData();
		msd.setData(ms);
		genericMigrator.setMigrationSpecData(msd);
		String oldKey = "Disease";
		String newKey = "diseases";
		List<String> valuesToMigrate = new ArrayList<String>();
		valuesToMigrate.add("Value1 to be migrated");
		valuesToMigrate.add("Value2 to be migrated");
		valuesToMigrate.add("Value3 to be migrated");
		Annotations additionalAnnotations = toMigrate.getNamedAnnotations().getAdditionalAnnotations();
		Annotations primaryAnnotations = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
		Map<String, List<String>> primaryStringAnnos = primaryAnnotations.getStringAnnotations();
		additionalAnnotations.addAnnotation(oldKey, valuesToMigrate);
		assertNotNull(additionalAnnotations.getAllValues(oldKey));
		genericMigrator.migrateOneStep(toMigrate, type);
		assertNull(additionalAnnotations.getAllValues(oldKey));
		assertNotNull(primaryAnnotations.getAllValues(newKey));
		assertEquals(3, primaryStringAnnos.get(newKey).size());
		assertEquals(valuesToMigrate.iterator().next(), primaryStringAnnos.get(newKey).iterator().next());
		return;
	}
	
	@Test
	@Ignore
	public void testMigrateDatasetAdditionalToPrimarySingleLong() {
		toMigrate.setNamedAnnotations(new NamedAnnotations());
		EntityType type = EntityType.dataset;
		MigrationSpec ms = new MigrationSpec();
		List<EntityTypeMigrationSpec> listEms = new ArrayList<EntityTypeMigrationSpec>();
		EntityTypeMigrationSpec ems = new EntityTypeMigrationSpec();
		ems.setEntityType(type.name());
		List<FieldMigrationSpec> listFms = new ArrayList<FieldMigrationSpec>();
		FieldMigrationSpec fms = new FieldMigrationSpec();
		FieldDescription source = new FieldDescription();
		FieldDescription dest = new FieldDescription();
		source.setName("Number_of_Samples");
		source.setType("long");
		source.setBucket("additional");
		dest.setName("numberOfSamples");
		dest.setType("long");
		dest.setBucket("primary");
		fms.setSource(source);
		fms.setDestination(dest);
		listFms.add(fms);
		listEms.add(ems);
		ems.setFields(listFms);
		ms.setMigrationMetadata(listEms);
		MigrationSpecData msd = new MigrationSpecData();
		msd.setData(ms);
		genericMigrator.setMigrationSpecData(msd);
		String oldKey = "Number_of_Samples";
		String newKey = "numberOfSamples";
		List<Long> valuesToMigrate = new ArrayList<Long>();
		valuesToMigrate.add(100L);
		Annotations additionalAnnotations = toMigrate.getNamedAnnotations().getAdditionalAnnotations();
		Annotations primaryAnnotations = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
		Map<String, List<Long>> primaryLongAnnos = primaryAnnotations.getLongAnnotations();
		additionalAnnotations.addAnnotation(oldKey, valuesToMigrate);
		assertNotNull(additionalAnnotations.getAllValues(oldKey));
		genericMigrator.migrateOneStep(toMigrate, type);
		assertNull(additionalAnnotations.getAllValues(oldKey));
		assertNotNull(primaryAnnotations.getAllValues(newKey));
		assertEquals(1, primaryLongAnnos.get(newKey).size());
		assertEquals(valuesToMigrate.iterator().next(), primaryLongAnnos.get(newKey).iterator().next());
		return;
	}
	
	@Test
	public void testMigrateType() {
		for (EntityType type: EntityType.values()) {
			// Setup migration spec
//			MigrationSpec ms = MigratorTestHelper.setupMigrationSpec(type.name(), "primary", "primary");
			MigrationSpec ms = new MigrationSpec();
			List<EntityTypeMigrationSpec> listEms = new ArrayList<EntityTypeMigrationSpec>();
			EntityTypeMigrationSpec ems = new EntityTypeMigrationSpec();
			ems.setEntityType(type.name());
			List<FieldMigrationSpec> listFms = new ArrayList<FieldMigrationSpec>();
			FieldMigrationSpec fms = new FieldMigrationSpec();
			FieldDescription source = new FieldDescription();
			FieldDescription dest = new FieldDescription();
			source.setName("old_name");
			source.setType("string");
			source.setBucket("additional");
			dest.setName("new_name");
			dest.setType("integer");
			dest.setBucket("additional");
			fms.setSource(source);
			fms.setDestination(dest);
			listFms.add(fms);
			listEms.add(ems);
			ems.setFields(listFms);
			ms.setMigrationMetadata(listEms);			
			MigrationSpecData msd = new MigrationSpecData();
			msd.setData(ms);
			genericMigrator.setMigrationSpecData(msd);
			
			// Setup node to migrate
			
//			toMigrate = MigratorTestHelper.setupNodeRevisionToMigrate();
			
			String oldKey = "old_name";
			String newKey = "new_name";
			String valueToMigrate = "1000";
			toMigrate.setNamedAnnotations(new NamedAnnotations());
//			Annotations primaryAnnotations = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
			Annotations additionalAnnotations = toMigrate.getNamedAnnotations().getAdditionalAnnotations();
			additionalAnnotations.addAnnotation(oldKey, valueToMigrate);
			
			Map<String, List<String>> stringAnnos = additionalAnnotations.getStringAnnotations();
			assertNotNull(stringAnnos.get(oldKey));
			genericMigrator.migrateOneStep(toMigrate, type);
			assertNull(stringAnnos.get(oldKey));
			Map<String, List<Long>> longAnnos = additionalAnnotations.getLongAnnotations();
			assertNotNull(longAnnos.get(newKey));
			assertEquals(1, longAnnos.get(newKey).size());
			assertEquals(Long.valueOf(valueToMigrate), longAnnos.get(newKey).iterator().next());
		}
		return;
	}
	
	@Test
	public void testMigrateTypeInvalid() {
		for (EntityType type: EntityType.values()) {
			// Setup migration spec
//			MigrationSpec ms = MigratorTestHelper.setupMigrationSpec(type.name(), "primary", "primary");
			MigrationSpec ms = new MigrationSpec();
			List<EntityTypeMigrationSpec> listEms = new ArrayList<EntityTypeMigrationSpec>();
			EntityTypeMigrationSpec ems = new EntityTypeMigrationSpec();
			ems.setEntityType(type.name());
			List<FieldMigrationSpec> listFms = new ArrayList<FieldMigrationSpec>();
			FieldMigrationSpec fms = new FieldMigrationSpec();
			FieldDescription source = new FieldDescription();
			FieldDescription dest = new FieldDescription();
			source.setName("old_name");
			source.setType("string");
			source.setBucket("additional");
			dest.setName("new_name");
			dest.setType("integer");
			dest.setBucket("additional");
			fms.setSource(source);
			fms.setDestination(dest);
			listFms.add(fms);
			listEms.add(ems);
			ems.setFields(listFms);
			ms.setMigrationMetadata(listEms);			
			MigrationSpecData msd = new MigrationSpecData();
			msd.setData(ms);
			genericMigrator.setMigrationSpecData(msd);
			
			// Setup node to migrate
			
//			toMigrate = MigratorTestHelper.setupNodeRevisionToMigrate();
			
			String oldKey = "old_name";
			String newKey = "new_name";
			String valueToMigrate = "abc";
			toMigrate.setNamedAnnotations(new NamedAnnotations());
//			Annotations primaryAnnotations = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
			Annotations additionalAnnotations = toMigrate.getNamedAnnotations().getAdditionalAnnotations();
			additionalAnnotations.addAnnotation(oldKey, valueToMigrate);
			
			Map<String, List<String>> stringAnnos = additionalAnnotations.getStringAnnotations();
			assertNotNull(stringAnnos.get(oldKey));
			genericMigrator.migrateOneStep(toMigrate, type);
			assertNull(stringAnnos.get(oldKey));
			Map<String, List<Long>> longAnnos = additionalAnnotations.getLongAnnotations();
			assertNotNull(longAnnos.get(newKey));
			assertEquals(1, longAnnos.get(newKey).size());
			assertEquals(new Long(-1), (Long)longAnnos.get(newKey).iterator().next());
		}
		return;
	}
	
	@Test
	public void TestMigrateTypeAnnotExistsButNotString() {
		for (EntityType type: EntityType.values()) {
			// Setup migration spec
//			MigrationSpec ms = MigratorTestHelper.setupMigrationSpec(type.name(), "primary", "primary");
			MigrationSpec ms = new MigrationSpec();
			List<EntityTypeMigrationSpec> listEms = new ArrayList<EntityTypeMigrationSpec>();
			EntityTypeMigrationSpec ems = new EntityTypeMigrationSpec();
			ems.setEntityType(type.name());
			List<FieldMigrationSpec> listFms = new ArrayList<FieldMigrationSpec>();
			FieldMigrationSpec fms = new FieldMigrationSpec();
			FieldDescription source = new FieldDescription();
			FieldDescription dest = new FieldDescription();
			source.setName("old_name");
			source.setType("string");
			source.setBucket("additional");
			dest.setName("new_name");
			dest.setType("integer");
			dest.setBucket("additional");
			fms.setSource(source);
			fms.setDestination(dest);
			listFms.add(fms);
			listEms.add(ems);
			ems.setFields(listFms);
			ms.setMigrationMetadata(listEms);			
			MigrationSpecData msd = new MigrationSpecData();
			msd.setData(ms);
			genericMigrator.setMigrationSpecData(msd);
			
			// Setup node to migrate
			
//			toMigrate = MigratorTestHelper.setupNodeRevisionToMigrate();
			
			String oldKey = "old_name";
			String newKey = "new_name";
			Long valueToMigrate = 100L;
			toMigrate.setNamedAnnotations(new NamedAnnotations());
			Annotations additionalAnnotations = toMigrate.getNamedAnnotations().getAdditionalAnnotations();
			// Create a long annotation with name expected for string annotation
			additionalAnnotations.addAnnotation(oldKey, valueToMigrate);
			
			Map<String, List<String>> stringAnnos = additionalAnnotations.getStringAnnotations();
			assertNull(stringAnnos.get(oldKey));
			Map<String, List<Long>> longAnnos = additionalAnnotations.getLongAnnotations();
			assertNotNull(longAnnos.get(oldKey));
			
			genericMigrator.migrateOneStep(toMigrate, type);
			
			assertNull(stringAnnos.get(oldKey));
			assertNull(longAnnos.get(oldKey));
			assertNotNull(longAnnos.get(newKey));
			assertEquals(1, longAnnos.get(newKey).size());
			assertEquals(new Long(100), (Long)longAnnos.get(newKey).iterator().next());
		}
		return;
	}
}
