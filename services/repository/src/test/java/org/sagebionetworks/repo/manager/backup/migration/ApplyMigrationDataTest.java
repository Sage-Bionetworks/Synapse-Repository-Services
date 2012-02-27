package org.sagebionetworks.repo.manager.backup.migration;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.registry.EntityMigrationData;
import org.sagebionetworks.repo.model.registry.EntityMigrationData.RenameFieldData;
import org.sagebionetworks.schema.ENCODING;
import org.sagebionetworks.schema.FORMAT;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.TYPE;

/**
 * Test for the ApplyMigrationData.
 * @author John
 *
 */
public class ApplyMigrationDataTest {
	
	EntityMigrationData mockMigrationData = null;
	ApplyMigrationData apply;
	EntityType mockType;
	NodeRevisionBackup toMigrate;
	
	@Before
	public void before(){
		mockMigrationData = Mockito.mock(EntityMigrationData.class);
		mockType = Mockito.mock(EntityType.class);
		apply = new ApplyMigrationData(mockMigrationData);
		toMigrate = new NodeRevisionBackup();
		toMigrate.setNamedAnnotations(new NamedAnnotations());
	}
	

	@Test
	public void testRenameString(){
		// Rename a string.
		String oldKey = "oldStringKey";
		String newKey = "newStringKey";
		Annotations primaryAnnos = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
		String valueToMigrate = "This value should be migrated";
		primaryAnnos.addAnnotation(oldKey, valueToMigrate);
		List<RenameFieldData> renameList = new ArrayList<RenameFieldData>();
		
		RenameFieldData rename = new RenameFieldData(oldKey, newKey, new ObjectSchema(TYPE.STRING));
		renameList.add(rename);
		when(mockMigrationData.getRenameDataForEntity(mockType)).thenReturn(renameList);
		// Now validate the rename
		apply.migrateOneStep(toMigrate, mockType);
		// Validate that the rename occured.
		Map<String, List<String>> stringAnnos = primaryAnnos.getStringAnnotations();
		assertNull(stringAnnos.get(oldKey));
		assertNotNull(stringAnnos.get(newKey));
		assertEquals(1, stringAnnos.get(newKey).size());
		assertEquals(valueToMigrate, stringAnnos.get(newKey).iterator().next());
		
	}
	
//	@Test
//	public void testRenameAdditionalString(){
//		// Rename a string.
//		String oldKey = "oldStringKey";
//		String newKey = "newStringKey";
//		Annotations additionalAnnos = toMigrate.getNamedAnnotations().getAdditionalAnnotations();
//		String valueToMigrate = "This value should be migrated";
//		additionalAnnos.addAnnotation(oldKey, valueToMigrate);
//		List<RenameFieldData> renameList = new ArrayList<RenameFieldData>();
//		
//		RenameFieldData rename = new RenameFieldData(oldKey, newKey, new ObjectSchema(TYPE.STRING));
//		renameList.add(rename);
//		when(mockMigrationData.getRenameDataForEntity(mockType)).thenReturn(renameList);
//		// Now validate the rename
//		apply.migrateOneStep(toMigrate, mockType);
//		// Validate that the rename occured.
//		Map<String, List<String>> stringAnnos = additionalAnnos.getStringAnnotations();
//		assertNull(stringAnnos.get(oldKey));
//		assertNotNull(stringAnnos.get(newKey));
//		assertEquals(1, stringAnnos.get(newKey).size());
//		assertEquals(valueToMigrate, stringAnnos.get(newKey).iterator().next());
//		
//	}
//	
//	@Test
//	public void testRenameBothString(){
//		// Rename a string.
//		String oldPrimaryKey = "oldPrimaryStringKey";
//		String newPrimaryKey = "newPrimaryStringKey";
//		String oldAdditionalKey = "oldAdditionalKey";
//		String newAdditionalKey = "newAdditionalKey";
//		Annotations primaryAnnos = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
//		Annotations additionalAnnos = toMigrate.getNamedAnnotations().getAdditionalAnnotations();
//		String additionalValueToMigrate = "This additional value should be migrated";
//		String primaryValueToMigrate = "This primary value should be migrated";
//		additionalAnnos.addAnnotation(oldAdditionalKey, additionalValueToMigrate);
//		primaryAnnos.addAnnotation(oldPrimaryKey, primaryValueToMigrate);
//		List<RenameFieldData> renameList = new ArrayList<RenameFieldData>();
//		
//		RenameFieldData rename = new RenameFieldData(oldPrimaryKey, newPrimaryKey, new ObjectSchema(TYPE.STRING));
//		renameList.add(rename);
//		rename = new RenameFieldData(oldAdditionalKey, newAdditionalKey, new ObjectSchema(TYPE.STRING));
//		renameList.add(rename);
//		
//		when(mockMigrationData.getRenameDataForEntity(mockType)).thenReturn(renameList);
//		// Now validate the rename
//		apply.migrateOneStep(toMigrate, mockType);
//		// Validate that the rename occured.
//		Map<String, List<String>> stringAnnos = primaryAnnos.getStringAnnotations();
//		assertNull(stringAnnos.get(oldPrimaryKey));
//		assertNotNull(stringAnnos.get(newPrimaryKey));
//		assertEquals(1, stringAnnos.get(newPrimaryKey).size());
//		assertEquals(primaryValueToMigrate, stringAnnos.get(newPrimaryKey).iterator().next());
//		stringAnnos = additionalAnnos.getStringAnnotations();
//		assertNull(stringAnnos.get(oldAdditionalKey));
//		assertNotNull(stringAnnos.get(newAdditionalKey));
//		assertEquals(1, stringAnnos.get(newAdditionalKey).size());
//		assertEquals(additionalValueToMigrate, stringAnnos.get(newAdditionalKey).iterator().next());
//		
//	}
	/**
	 * Cannot rename any since it is not supported.
	 */
	@Test (expected=IllegalArgumentException.class)
	public void testRenameAny(){
		Annotations primaryAnnos = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
		String valueToMigrate = "This value should be migrated";
		primaryAnnos.addAnnotation("oldStringKey", valueToMigrate);
		List<RenameFieldData> renameList = new ArrayList<RenameFieldData>();
		// Rename a string.
		String oldKey = "oldStringKey";
		String newKey = "newStringKey";
		RenameFieldData rename = new RenameFieldData(oldKey, newKey, new ObjectSchema(TYPE.ANY));
		renameList.add(rename);
		when(mockMigrationData.getRenameDataForEntity(mockType)).thenReturn(renameList);
		// Now validate the rename
		apply.migrateOneStep(toMigrate, mockType);	
	}
	
	@Test
	public void testRenameLong(){
		// Rename a string.
		String oldKey = "oldLongKey";
		String newKey = "newLongKey";
		Annotations primaryAnnos = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
		List<Long> valuesToRename = new ArrayList<Long>();
		valuesToRename.add(new Long(123));
		valuesToRename.add(new Long(456));
		primaryAnnos.addAnnotation(oldKey, valuesToRename);
		List<RenameFieldData> renameList = new ArrayList<RenameFieldData>();
		
		RenameFieldData rename = new RenameFieldData(oldKey, newKey, new ObjectSchema(TYPE.INTEGER));
		renameList.add(rename);
		when(mockMigrationData.getRenameDataForEntity(mockType)).thenReturn(renameList);
		// Now validate the rename
		apply.migrateOneStep(toMigrate, mockType);
		// Validate that the rename occured.
		Map<String, List<Long>> annos = primaryAnnos.getLongAnnotations();
		assertNull(annos.get(oldKey));
		assertNotNull(annos.get(newKey));
		assertEquals(2, annos.get(newKey).size());
		assertEquals(valuesToRename, annos.get(newKey));
		
	}
	
//	@Test
//	public void testRenameAdditionalLong(){
//		// Rename a string.
//		String oldKey = "oldStringKey";
//		String newKey = "newStringKey";
//		Annotations additionalAnnos = toMigrate.getNamedAnnotations().getAdditionalAnnotations();
//		List<Long> valuesToRename = new ArrayList<Long>();
//		valuesToRename.add(new Long(123));
//		valuesToRename.add(new Long(456));
//		additionalAnnos.addAnnotation(oldKey, valuesToRename);
//		List<RenameFieldData> renameList = new ArrayList<RenameFieldData>();
//		
//		RenameFieldData rename = new RenameFieldData(oldKey, newKey, new ObjectSchema(TYPE.INTEGER));
//		renameList.add(rename);
//		when(mockMigrationData.getRenameDataForEntity(mockType)).thenReturn(renameList);
//		// Now validate the rename
//		apply.migrateOneStep(toMigrate, mockType);
//		// Validate that the rename occured.
//		Map<String, List<Long>> longAnnos = additionalAnnos.getLongAnnotations();
//		assertNull(longAnnos.get(oldKey));
//		assertNotNull(longAnnos.get(newKey));
//		assertEquals(2, longAnnos.get(newKey).size());
//		assertEquals(valuesToRename, longAnnos.get(newKey));
//		
//	}
//	
//	@Test
//	public void testRenameBothLong(){
//		// Rename a string.
//		String oldPrimaryKey = "oldPrimaryStringKey";
//		String newPrimaryKey = "newPrimaryStringKey";
//		String oldAdditionalKey = "oldAdditionalKey";
//		String newAdditionalKey = "newAdditionalKey";
//		Annotations primaryAnnos = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
//		Annotations additionalAnnos = toMigrate.getNamedAnnotations().getAdditionalAnnotations();
//		List<Long> valuesToMigrate1 = new ArrayList<Long>();
//		valuesToMigrate1.add(new Long(12));
//		valuesToMigrate1.add(new Long(34));
//		List<Long> valuesToMigrate2 = new ArrayList<Long>();
//		valuesToMigrate2.add(new Long(56));
//		valuesToMigrate2.add(new Long(78));
//		additionalAnnos.addAnnotation(oldAdditionalKey, valuesToMigrate1);
//		primaryAnnos.addAnnotation(oldPrimaryKey, valuesToMigrate2);
//		List<RenameFieldData> renameList = new ArrayList<RenameFieldData>();
//		
//		RenameFieldData rename = new RenameFieldData(oldPrimaryKey, newPrimaryKey, new ObjectSchema(TYPE.INTEGER));
//		renameList.add(rename);
//		rename = new RenameFieldData(oldAdditionalKey, newAdditionalKey, new ObjectSchema(TYPE.INTEGER));
//		renameList.add(rename);
//		
//		when(mockMigrationData.getRenameDataForEntity(mockType)).thenReturn(renameList);
//		// Now validate the rename
//		apply.migrateOneStep(toMigrate, mockType);
//		// Validate that the rename occured.
//		Map<String, List<Long>> longAnnos = additionalAnnos.getLongAnnotations();
//		assertNull(longAnnos.get(oldPrimaryKey));
//		assertNotNull(longAnnos.get(newAdditionalKey));
//		assertEquals(2, longAnnos.get(newAdditionalKey).size());
//		assertEquals(valuesToMigrate1, longAnnos.get(newAdditionalKey));
//		longAnnos = primaryAnnos.getLongAnnotations();
//		assertNull(longAnnos.get(oldPrimaryKey));
//		assertNotNull(longAnnos.get(newPrimaryKey));
//		assertEquals(2, longAnnos.get(newPrimaryKey).size());
//		assertEquals(valuesToMigrate2, longAnnos.get(newPrimaryKey));
//		
//	}
	
	@Test
	public void testRenameDouble(){
		// Rename a string.
		String oldKey = "oldDoubleKey";
		String newKey = "newDoubleKey";
		Annotations primaryAnnos = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
		List<Double> valuesToRename = new ArrayList<Double>();
		valuesToRename.add(new Double(1.33));
		valuesToRename.add(new Double(2.1));
		primaryAnnos.addAnnotation(oldKey, valuesToRename);
		List<RenameFieldData> renameList = new ArrayList<RenameFieldData>();
		
		RenameFieldData rename = new RenameFieldData(oldKey, newKey, new ObjectSchema(TYPE.NUMBER));
		renameList.add(rename);
		when(mockMigrationData.getRenameDataForEntity(mockType)).thenReturn(renameList);
		// Now validate the rename
		apply.migrateOneStep(toMigrate, mockType);
		// Validate that the rename occured.
		Map<String, List<Double>> annos = primaryAnnos.getDoubleAnnotations();
		assertNull(annos.get(oldKey));
		assertNotNull(annos.get(newKey));
		assertEquals(2, annos.get(newKey).size());
		assertEquals(valuesToRename, annos.get(newKey));

	}
	
	@Test
	public void testRenameDate(){
		// Rename a string.
		String oldKey = "oldDateKey";
		String newKey = "newDateKey";
		Annotations primaryAnnos = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
		List<Date> valuesToRename = new ArrayList<Date>();
		valuesToRename.add(new Date());
		valuesToRename.add(new Date(0));
		primaryAnnos.addAnnotation(oldKey, valuesToRename);
		List<RenameFieldData> renameList = new ArrayList<RenameFieldData>();
		ObjectSchema schema = new ObjectSchema(TYPE.STRING);
		schema.setFormat(FORMAT.DATE_TIME);
		RenameFieldData rename = new RenameFieldData(oldKey, newKey, schema);
		renameList.add(rename);
		when(mockMigrationData.getRenameDataForEntity(mockType)).thenReturn(renameList);
		// Now validate the rename
		apply.migrateOneStep(toMigrate, mockType);
		// Validate that the rename occured.
		Map<String, List<Date>> annos = primaryAnnos.getDateAnnotations();
		assertNull(annos.get(oldKey));
		assertNotNull(annos.get(newKey));
		assertEquals(2, annos.get(newKey).size());
		assertEquals(valuesToRename, annos.get(newKey));
	}
	
	@Test
	public void testRenameBlob() throws UnsupportedEncodingException{
		// Rename a string.
		String oldKey = "oldBlobKey";
		String newKey = "newBlobKey";
		Annotations primaryAnnos = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
		List<byte[]> valuesToRename = new ArrayList<byte[]>();
		valuesToRename.add("This should be converted to a blob number one".getBytes("UTF-8"));
		valuesToRename.add("This should be converted to a blob number two".getBytes("UTF-8"));
		primaryAnnos.addAnnotation(oldKey, valuesToRename);
		List<RenameFieldData> renameList = new ArrayList<RenameFieldData>();
		// This is a string but it is stored as a blob.
		ObjectSchema schema = new ObjectSchema(TYPE.STRING);
		schema.setContentEncoding(ENCODING.BINARY);
		RenameFieldData rename = new RenameFieldData(oldKey, newKey, schema);
		renameList.add(rename);
		when(mockMigrationData.getRenameDataForEntity(mockType)).thenReturn(renameList);
		// Now validate the rename
		apply.migrateOneStep(toMigrate, mockType);
		// Validate that the rename occured.
		Map<String, List<byte[]>> annos = primaryAnnos.getBlobAnnotations();
		assertNull(annos.get(oldKey));
		assertNotNull(annos.get(newKey));
		assertEquals(2, annos.get(newKey).size());
		assertEquals(valuesToRename, annos.get(newKey));

	}
}
