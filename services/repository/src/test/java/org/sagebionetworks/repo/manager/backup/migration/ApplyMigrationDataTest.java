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
		Map<String, Collection<String>> stringAnnos = primaryAnnos.getStringAnnotations();
		assertNull(stringAnnos.get(oldKey));
		assertNotNull(stringAnnos.get(newKey));
		assertEquals(1, stringAnnos.get(newKey).size());
		assertEquals(valueToMigrate, stringAnnos.get(newKey).iterator().next());
		
	}
	
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
		Map<String, Collection<Long>> annos = primaryAnnos.getLongAnnotations();
		assertNull(annos.get(oldKey));
		assertNotNull(annos.get(newKey));
		assertEquals(2, annos.get(newKey).size());
		assertEquals(valuesToRename, annos.get(newKey));
		
	}
	
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
		Map<String, Collection<Double>> annos = primaryAnnos.getDoubleAnnotations();
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
		Map<String, Collection<Date>> annos = primaryAnnos.getDateAnnotations();
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
		Map<String, Collection<byte[]>> annos = primaryAnnos.getBlobAnnotations();
		assertNull(annos.get(oldKey));
		assertNotNull(annos.get(newKey));
		assertEquals(2, annos.get(newKey).size());
		assertEquals(valuesToRename, annos.get(newKey));

	}
}
