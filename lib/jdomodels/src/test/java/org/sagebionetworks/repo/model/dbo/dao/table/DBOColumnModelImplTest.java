package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOColumnModelImplTest {
	
	@Autowired
	ColumnModelDAO columnModelDao;
	
	ColumnModel one;
	ColumnModel two;
	ColumnModel three;
	
	IdAndVersion idAndVersion;
	
	@Before
	public void before() throws DatastoreException, NotFoundException{
		// One
		one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.STRING);
		one.setMaximumSize(10L);
		one = columnModelDao.createColumnModel(one);
		// two
		two = new ColumnModel();
		two.setName("two");
		two.setColumnType(ColumnType.STRING);
		// Get the default
		two.setMaximumSize(null);
		two = columnModelDao.createColumnModel(two);
		// three
		three = new ColumnModel();
		three.setName("three");
		three.setColumnType(ColumnType.STRING);
		three = columnModelDao.createColumnModel(three);
		
		idAndVersion  = IdAndVersion.parse("syn123");
	}
	
	@After
	public void after(){
		columnModelDao.truncateAllColumnData();
	}
	
	@Test
	public void testRoundTrip() throws Exception {
		// To save
		ColumnModel model = new ColumnModel();
		model.setName("column model dao test");
		model.setColumnType(ColumnType.STRING);
		model.setMaximumSize(16L);
		model.setDefaultValue("abc");
		model.setEnumValues(new LinkedList<String>());
		model.getEnumValues().add("xyz");
		model.getEnumValues().add("abc");
		// Create it.
		ColumnModel result = columnModelDao.createColumnModel(model);
		assertNotNull(result);
		assertNotNull(result.getId());
		assertEquals("column model dao test", result.getName());
		assertEquals(ColumnType.STRING, result.getColumnType());
		assertEquals(ColumnType.STRING, result.getColumnType());
		assertEquals(new Long(16), result.getMaximumSize());
		assertNotNull(result.getEnumValues());
		assertEquals(2, result.getEnumValues().size());
		assertEquals("abc", result.getEnumValues().get(0));
		assertEquals("xyz", result.getEnumValues().get(1));
		String originalId = result.getId();
		// If we save the same model again we should get the same ID as the two models will have the same hash
		result = columnModelDao.createColumnModel(model);
		assertNotNull(result);
		assertEquals("Creating the same column model with the same data should have returned same object as the original",originalId, result.getId());
		ColumnModel two = columnModelDao.getColumnModel(originalId);
		assertEquals(result, two);
		// Now delete the model
		columnModelDao.deleteColumModel(originalId);
		// Now it should be not found
		try {
			columnModelDao.getColumnModel(originalId);
			fail("This model should have been deleted");
		} catch (NotFoundException e) {
			// expected
		}
	}
	
	@Test
	public void testGetList() throws DatastoreException, NotFoundException{
		// Get the list
		List<String> ids = new LinkedList<String>();
		// ask for the rows out of order.
		ids.add(three.getId());
		ids.add(one.getId());
		List<ColumnModel> list = columnModelDao.getColumnModel(ids);
		assertNotNull(list);
		assertEquals(2, list.size());
		assertTrue(list.contains(one));
		assertTrue(list.contains(three));
	}
	
	@Test
	public void testGetListEmpty() throws DatastoreException, NotFoundException{
		List<ColumnModel> list = columnModelDao.getColumnModel(new LinkedList<String>());
		assertNotNull(list);
		assertTrue(list.isEmpty());
	}

	@Test
	public void testBindColumns() throws DatastoreException, NotFoundException{
		List<String> ids = columnModelDao.getColumnModelIdsForObject(idAndVersion);
		assertNotNull(ids);
		assertTrue(ids.isEmpty());
		// Now bind one column
		List<ColumnModel> toBind = Lists.newArrayList(two);
		columnModelDao.bindColumnToObject(toBind, idAndVersion);
		String ownerEtag = columnModelDao.lockOnOwner(idAndVersion.getId().toString());
		assertNotNull(ownerEtag);
		// Now bind the next two
		toBind.clear();
		toBind.add(one);
		toBind.add(three);
		columnModelDao.bindColumnToObject(toBind, idAndVersion);
		
		ids = columnModelDao.getColumnModelIdsForObject(idAndVersion);
		List<String> expected = Lists.newArrayList(one.getId(), three.getId());
		assertEquals(expected, ids);
	}
	
	@Test
	public void testBindColumnsMultipleVersion() {
		IdAndVersion defaultId = IdAndVersion.parse("syn123");
		IdAndVersion versionOne = IdAndVersion.parse("syn123.1");
		IdAndVersion versionTwo = IdAndVersion.parse("syn123.2");
		IdAndVersion versionThree = IdAndVersion.parse("syn123.3");
		
		List<ColumnModel> defaultSchema = Lists.newArrayList(two, three);
		List<ColumnModel> vOneSchema = Lists.newArrayList(one, three);
		List<ColumnModel> vTwoSchema = Lists.newArrayList(one, two);
		List<ColumnModel> vThreeSchema = Collections.emptyList();
		
		// call under test
		columnModelDao.bindColumnToObject(defaultSchema, defaultId);
		columnModelDao.bindColumnToObject(vOneSchema, versionOne);
		columnModelDao.bindColumnToObject(vTwoSchema, versionTwo);
		columnModelDao.bindColumnToObject(vThreeSchema, versionThree);
		
		// call under test
		List<ColumnModel> defaultResults = columnModelDao.getColumnModelsForObject(defaultId);
		List<ColumnModel> vOneResults = columnModelDao.getColumnModelsForObject(versionOne);
		List<ColumnModel> vTwoResults = columnModelDao.getColumnModelsForObject(versionTwo);
		List<ColumnModel> vThreeResults = columnModelDao.getColumnModelsForObject(versionThree);
		
		assertEquals(defaultSchema, defaultResults);
		assertEquals(vOneSchema, vOneResults);
		assertEquals(vTwoSchema, vTwoResults);
		assertEquals(vThreeSchema, vThreeResults);
		
	}
	
	@Test
	public void testGetColumnIdsMultipleVersion() {
		IdAndVersion defaultId = IdAndVersion.parse("syn123");
		IdAndVersion versionOne = IdAndVersion.parse("syn123.1");
		
		List<ColumnModel> defaultSchema = Lists.newArrayList(two, three);
		List<ColumnModel> vOneSchema = Lists.newArrayList(one, three);
	
		columnModelDao.bindColumnToObject(defaultSchema, defaultId);
		columnModelDao.bindColumnToObject(vOneSchema, versionOne);
		
		// call under test
		List<String> defaultResults = columnModelDao.getColumnModelIdsForObject(defaultId);
		List<String> vOneResults = columnModelDao.getColumnModelIdsForObject(versionOne);
		//
		List<String> defaultExpected = Lists.newArrayList(two.getId(), three.getId());
		assertEquals(defaultExpected, defaultResults);
		List<String> vOneExpected = Lists.newArrayList(one.getId(), three.getId());
		assertEquals(vOneExpected, vOneResults);
	}
	
	@Test
	public void testUnbindColumnsAndDeleteOwner() throws DatastoreException, NotFoundException {
		// Now bind one column
		List<ColumnModel> toBind = Lists.newArrayList(one, two);
		columnModelDao.bindColumnToObject(toBind, idAndVersion);
		columnModelDao.lockOnOwner("syn123");
		columnModelDao.deleteOwner("syn123");
		try {
			columnModelDao.lockOnOwner("syn123");
			fail("owner should no longer exist");
		} catch (EmptyResultDataAccessException e) {
		}
		List<ColumnModel> columnModelsForObject = columnModelDao.getColumnModelsForObject(idAndVersion);
		assertEquals(0, columnModelsForObject.size());
	}
	
	@Test
	public void testListColumnModels() throws DatastoreException, NotFoundException{
		List<ColumnModel> cols = createColumsWithName(new String[]{"aaa","aab","aac", "abb", "abc"});
		List<ColumnModel> results = columnModelDao.listColumnModels(null, Long.MAX_VALUE, 0l);
		assertNotNull(results);
		assertTrue(results.size() >= 5);
		// Now filter by 'a'
		results = columnModelDao.listColumnModels("A", Long.MAX_VALUE, 0l);
		long count = columnModelDao.listColumnModelsCount("A");
		assertNotNull(results);
		assertEquals(5, results.size());
		assertEquals(5, count);
		// The should be ordered by name
		assertEquals(cols.get(0).getId(), results.get(0).getId());
		assertEquals(cols.get(2).getId(), results.get(2).getId());
		// Now pagination
		results = columnModelDao.listColumnModels("A", 1, 2l);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(cols.get(2).getId(), results.get(0).getId());
		// more filtering
		results = columnModelDao.listColumnModels("Aa", Long.MAX_VALUE, 0l);
		count = columnModelDao.listColumnModelsCount("Aa");
		assertNotNull(results);
		assertEquals(3, results.size());
		assertEquals(3, count);
		assertEquals(cols.get(2).getId(), results.get(2).getId());
		
		// more filtering
		results = columnModelDao.listColumnModels("Ab", Long.MAX_VALUE, 0l);
		count = columnModelDao.listColumnModelsCount("Ab");
		assertNotNull(results);
		assertEquals(2, results.size());
		assertEquals(2, count);
		assertEquals(cols.get(4).getId(), results.get(1).getId());
	}
	
	@Test
	public void testGetColumnModelsForObject() throws NotFoundException{
		// Start with all types
		List<ColumnModel> raw = TableModelTestUtils.createOneOfEachType();
		// Create each one
		List<ColumnModel> models = new LinkedList<ColumnModel>();
		for(ColumnModel cm: raw){
			models.add(columnModelDao.createColumnModel(cm));
		}
		// Shuffle the results so the order they are bound to does not match the natural order
		Collections.shuffle(models);
		// Now bind them to the table in this shuffled order
		columnModelDao.bindColumnToObject(models, idAndVersion);
		// Now make sure we can fetch this back in the same order that we bound the columns
		List<ColumnModel> fetched = columnModelDao.getColumnModelsForObject(idAndVersion);
		assertNotNull(fetched);
		assertEquals(models, fetched);
		List<String> columnIds = columnModelDao.getColumnModelIdsForObject(idAndVersion);
		assertEquals(models.size(), columnIds.size());
		for(int i=0; i<models.size(); i++) {
			ColumnModel cm = models.get(i);
			String columnId = columnIds.get(i);
			assertEquals(cm.getId(), columnId);
		}
		
		// Now if we update the columns bound to the object the order should change
		models.remove(0);
		Collections.shuffle(models);
		// Bind the new columns in a new order
		columnModelDao.bindColumnToObject(models, idAndVersion);
		// Get them back in the same order with the same columns
		fetched = columnModelDao.getColumnModelsForObject(idAndVersion);
		assertNotNull(fetched);
		assertEquals(models, fetched);
	}
	
	@Test
	public void testBindNull() throws NotFoundException{
		List<ColumnModel> toBind = null;
		// call under test
		columnModelDao.bindColumnToObject(toBind, idAndVersion);
		List<ColumnModel> results = columnModelDao.getColumnModelsForObject(idAndVersion);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	@Test
	public void testBindEmpty() throws NotFoundException{
		List<ColumnModel> toBind = new LinkedList<>();
		// call under test
		columnModelDao.bindColumnToObject(toBind, idAndVersion);
		List<ColumnModel> results = columnModelDao.getColumnModelsForObject(idAndVersion);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	@Test
	public void testCaseSenstive() throws DatastoreException, NotFoundException{
		ColumnModel first = new ColumnModel();
		first.setName("AbbB");
		first.setColumnType(ColumnType.STRING);
		first.setMaximumSize(10L);
		String firstId = columnModelDao.createColumnModel(first).getId();
		// Create a second column that is the same.
		ColumnModel second = new ColumnModel();
		second.setName(first.getName());
		second.setColumnType(first.getColumnType());
		second.setMaximumSize(first.getMaximumSize());
		String secondId = columnModelDao.createColumnModel(second).getId();
		assertTrue("Two columns that are identical should have the same ID",firstId.equals(secondId));
		// Now change the case and try again
		second.setName(first.getName().toLowerCase());
		secondId = columnModelDao.createColumnModel(second).getId();
		assertFalse("Two columns that differ only by case should not get the same",firstId.equals(secondId));
	}

	
	@Test
	public void testDeleteColumnModel() throws DatastoreException, NotFoundException {
		// Create new column model
		ColumnModel m = new ColumnModel();
		m.setName("col1");
		m.setColumnType(ColumnType.STRING);
		m.setMaximumSize(10L);
		m = columnModelDao.createColumnModel(m);
		// Verify that not bound
		Set<String> boundColModelIds = new HashSet<String>();
		boundColModelIds.add(m.getId());
		// Delete the column model should succeed
		columnModelDao.deleteColumModel(m.getId());
		// Check that not found
		try {
			columnModelDao.getColumnModel(m.getId());
			fail("This model should have been deleted");
		} catch (NotFoundException e) {
			// expected
		}
	}
		
	@Test
	public void testGetColumnNames() {
		List<ColumnModel> raw = TableModelTestUtils.createOneOfEachType();
		// Create each one
		Set<Long> colIds = new HashSet<>();
		Map<Long, String> expected = new HashMap<>();
		for(ColumnModel cm: raw){
			cm = columnModelDao.createColumnModel(cm);
			Long id = Long.parseLong(cm.getId());
			colIds.add(id);
			expected.put(id, cm.getName());
		}
		// call under test
		Map<Long, String> results = columnModelDao.getColumnNames(colIds);
		assertEquals(expected, results);	
	}
	
	@Test
	public void testGetColumnNamesEmptyInput() {
		Set<Long> colIds = new HashSet<>();
		// call under test
		Map<Long, String> results = columnModelDao.getColumnNames(colIds);
		assertNotNull(results);
		assertEquals(0, results.size());
	}
	/**
	 * Helper to create columns by name
	 * @param names
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	private List<ColumnModel> createColumsWithName(String[] names) throws DatastoreException, NotFoundException{
		List<ColumnModel> results = new LinkedList<ColumnModel>();
		for(String name: names){
			ColumnModel cm  = new ColumnModel();
			cm.setName(name);
			cm.setColumnType(ColumnType.STRING);
			cm = columnModelDao.createColumnModel(cm);
			results.add(cm);
		}
		return results;
	}
	
	/**
	 * Ensure we can write a UTF-8 column name to the database.
	 * 
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testPLFM_5429() throws UnsupportedEncodingException {
		String name = "Aβ ELISA/WB";
		byte[] latin = name.getBytes("ISO-8859-1");
		System.out.println("Latin: "+Hex.encodeHexString(latin));
		byte[] utf8 = name.getBytes("UTF-8");
		System.out.println("utf-8: "+Hex.encodeHexString(utf8));
		
		ColumnModel column = new ColumnModel();
		column.setName(name);
		column.setColumnType(ColumnType.STRING);
		column.setMaximumSize(10L);
		columnModelDao.createColumnModel(column).getId();
	}

}
