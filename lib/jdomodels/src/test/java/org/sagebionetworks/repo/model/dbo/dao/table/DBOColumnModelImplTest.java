package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
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
		List<ColumnModel> list = columnModelDao.getColumnModel(ids, false);
		assertNotNull(list);
		assertEquals(2, list.size());
		// The rows should be alphabetical order by name.
		assertEquals(one, list.get(0));
		assertEquals(three, list.get(1));
	}
	
	@Test
	public void testGetListInOrder() throws DatastoreException, NotFoundException {
		// Get the list
		List<String> ids = Lists.newArrayList(two.getId(), three.getId(), one.getId());
		List<ColumnModel> list = columnModelDao.getColumnModel(ids, true);
		assertNotNull(list);
		assertEquals(3, list.size());
		// The rows should be alphabetical order by name.
		assertEquals(two, list.get(0));
		assertEquals(three, list.get(1));
		assertEquals(one, list.get(2));
	}

	@Test
	public void testBindColumns() throws DatastoreException, NotFoundException{
		// Now bind one column
		List<String> toBind = new LinkedList<String>();
		toBind.add(two.getId());
		int count = columnModelDao.bindColumnToObject(toBind, "syn123");
		assertTrue(count > 0);
		String ownerEtag = columnModelDao.lockOnOwner("syn123");
		assertNotNull(ownerEtag);
		// Now bind the next two
		toBind.clear();
		toBind.add(one.getId());
		toBind.add(three.getId());
		count = columnModelDao.bindColumnToObject(toBind, "syn123");
		assertTrue(count > 0);
	}
	
	@Test
	public void testBindColumnsDoesNotExist() throws Exception {
		// Now bind one column
		List<String> toBind = new LinkedList<String>();
		// This should not exist
		String fakeId = "999999999999";
		toBind.add(fakeId);
		try{
			columnModelDao.bindColumnToObject(toBind, "syn123");
			fail("Should have thrown an exception");
		}catch(NotFoundException e){
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().contains(fakeId));
		}
	}
	
	@Test
	public void testUnbindColumnsAndDeleteOwner() throws DatastoreException, NotFoundException {
		// Now bind one column
		List<String> toBind = new LinkedList<String>();
		toBind.add(one.getId());
		toBind.add(two.getId());
		int count = columnModelDao.bindColumnToObject(toBind, "syn123");
		assertTrue(count > 0);
		columnModelDao.lockOnOwner("syn123");
		columnModelDao.unbindAllColumnsFromObject("syn123");
		columnModelDao.deleteOwner("syn123");
		try {
			columnModelDao.lockOnOwner("syn123");
			fail("owner should no longer exist");
		} catch (EmptyResultDataAccessException e) {
		}
		List<ColumnModel> columnModelsForObject = columnModelDao.getColumnModelsForObject("syn123");
		assertEquals(0, columnModelsForObject.size());
	}

	@Test
	public void testlistObjectsBoundToColumn() throws DatastoreException, NotFoundException{
		// bind two columns to two objects
		List<String> toBind = new LinkedList<String>();
		toBind.add(two.getId());
		toBind.add(one.getId());
		columnModelDao.bindColumnToObject(toBind, "syn123");
		// make only one the current
		toBind.clear();
		toBind.add(one.getId());
		columnModelDao.bindColumnToObject(toBind, "syn123");
		
		// bind to the other object
		toBind.clear();
		toBind.add(two.getId());
		toBind.add(three.getId());
		columnModelDao.bindColumnToObject(toBind, "syn456");
		// make only two the current
		toBind.clear();
		toBind.add(two.getId());
		columnModelDao.bindColumnToObject(toBind, "syn456");
		
		// None are bound to all three
		Set<String> filter = new HashSet<String>();
		filter.add(one.getId());
		filter.add(two.getId());
		filter.add(three.getId());
		List<String> results = columnModelDao.listObjectsBoundToColumn(filter, false, Long.MAX_VALUE, 0l);
		long count = columnModelDao.listObjectsBoundToColumnCount(filter, false);
		assertNotNull(results);
		assertEquals(0, results.size());
		assertEquals(0, count);
		results = columnModelDao.listObjectsBoundToColumn(filter, true, Long.MAX_VALUE, 0l);
		count = columnModelDao.listObjectsBoundToColumnCount(filter, false);
		assertEquals(0, count);
		assertNotNull(results);
		assertEquals(0, results.size());
		
		// Two is is used by both but is is only current for syn456;
		filter.clear();
		filter.add(two.getId());
		results = columnModelDao.listObjectsBoundToColumn(filter, false, Long.MAX_VALUE, 0l);
		count = columnModelDao.listObjectsBoundToColumnCount(filter, false);
		assertEquals(2, count);
		assertEquals(2, results.size());
		// The should be in order by object id.
		assertEquals("syn123", results.get(0));
		assertEquals("syn456", results.get(1));
		// Now for current only we should only get syn456
		results = columnModelDao.listObjectsBoundToColumn(filter, true, Long.MAX_VALUE, 0l);
		count = columnModelDao.listObjectsBoundToColumnCount(filter, true);
		assertEquals(1, count);
		assertEquals(1, results.size());
		assertEquals("syn456", results.get(0));
		
		// now bind one to syn456
		// make only two the current
		toBind.clear();
		toBind.add(one.getId());
		columnModelDao.bindColumnToObject(toBind, "syn456");
		
		// now filter one and two.
		filter.clear();
		filter.add(one.getId());
		filter.add(two.getId());
		results = columnModelDao.listObjectsBoundToColumn(filter, false, Long.MAX_VALUE, 0l);
		count = columnModelDao.listObjectsBoundToColumnCount(filter, false);
		assertEquals(2, count);
		assertEquals(2, results.size());
		// The should be in order by object id.
		assertEquals("syn123", results.get(0));
		assertEquals("syn456", results.get(1));
		
		// with limit and offset
		results = columnModelDao.listObjectsBoundToColumn(filter, false, 1, 0l);
		assertEquals(1, results.size());
		assertEquals("syn123", results.get(0));
		results = columnModelDao.listObjectsBoundToColumn(filter, false, 1, 1l);
		assertEquals(1, results.size());
		assertEquals("syn456", results.get(0));
		
		results = columnModelDao.listObjectsBoundToColumn(filter, true, Long.MAX_VALUE, 0l);
		assertEquals(0, results.size());
		count = columnModelDao.listObjectsBoundToColumnCount(filter, true);
		assertEquals(0, count);
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
		List<Long> ids = TableModelUtils.getIds(models);
		// Now bind them to the table in this shuffled order
		String tableId = "syn123";
		columnModelDao.bindColumnToObject(Lists.transform(ids, TableModelUtils.LONG_TO_STRING), tableId);
		// Now make sure we can fetch this back in the same order that we bound the columns
		List<ColumnModel> fetched = columnModelDao.getColumnModelsForObject(tableId);
		assertNotNull(fetched);
		assertEquals(models, fetched);
		// Now if we update the columns bound to the object the order should change
		models.remove(0);
		Collections.shuffle(models);
		ids = TableModelUtils.getIds(models);
		// Bind the new columns in a new order
		columnModelDao.bindColumnToObject(Lists.transform(ids, TableModelUtils.LONG_TO_STRING), tableId);
		// Get them back in the same order with the same columns
		fetched = columnModelDao.getColumnModelsForObject(tableId);
		assertNotNull(fetched);
		assertEquals(models, fetched);
	}
	
	@Test
	public void testBindNull() throws NotFoundException{
		String tableId = "syn123";
		int count = columnModelDao.bindColumnToObject(null, tableId);
		assertEquals(0, count);
		// Now bind some columns
		List<ColumnModel> raw = TableModelTestUtils.createOneOfEachType();
		// Create each one
		List<ColumnModel> models = new LinkedList<ColumnModel>();
		for(ColumnModel cm: raw){
			models.add(columnModelDao.createColumnModel(cm));
		}
		List<Long> ids = TableModelUtils.getIds(models);
		count = columnModelDao.bindColumnToObject(Lists.transform(ids, TableModelUtils.LONG_TO_STRING), tableId);
		assertEquals(ids.size(), count);
		// Now if we set it back to empty the update should include 5 rows
		List<String> empty = new LinkedList<String>();
		count = columnModelDao.bindColumnToObject(empty, tableId);
		assertEquals(ids.size(), count);
		
		// The bound list should not be null
		List<ColumnModel> results = columnModelDao.getColumnModelsForObject(tableId);
		assertNotNull(results);
		assertEquals(0,  results.size());
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
	
	/**
	 *  Should not be able to bind two columns with the same name to the same object
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	@Test
	public void testBindWithNameConflict() throws DatastoreException, NotFoundException{
		// Should not be able to bind two columns with the same name to the same object
		ColumnModel first = new ColumnModel();
		first.setName("AbbB");
		first.setColumnType(ColumnType.STRING);
		first.setMaximumSize(10L);
		first = columnModelDao.createColumnModel(first);
		// The second has the same name but different type.
		ColumnModel second = new ColumnModel();
		second.setName(first.getName());
		second.setColumnType(ColumnType.INTEGER);
		second = columnModelDao.createColumnModel(second);
		// They should not have the same id
		assertFalse("Two columns with the same name but different type should not have the same ID",first.getId().equals(second.getId()));
		// now try to bind both columns to an object
		try{
			columnModelDao.bindColumnToObject(Arrays.asList(first.getId(), second.getId()), "syn123");
			fail("Should not be able to bind two columns with the same name to the same object");
		}catch(IllegalArgumentException e){
			// expected
			assertTrue("The error should contain the duplicate name",e.getMessage().contains(first.getName()));
		}
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
		List<String> colBindings = columnModelDao.listObjectsBoundToColumn(boundColModelIds, true, 10L, 0);
		assertEquals(0, colBindings.size());
		// Delete the column model should succeed
		columnModelDao.deleteColumModel(m.getId());
		// Check that not found
		try {
			columnModelDao.getColumnModel(m.getId());
			fail("This model should have been deleted");
		} catch (NotFoundException e) {
			// expected
		}
		// Create another column model
		m = new ColumnModel();
		m.setName("col2");
		m.setColumnType(ColumnType.STRING);
		m.setMaximumSize(20L);
		m = columnModelDao.createColumnModel(m);
		// Bind it to a table
		List<String> colIds = new LinkedList<String>();
		colIds.add(m.getId());
		int count = columnModelDao.bindColumnToObject(colIds, "syn123");
		assertEquals(1, count);
		// Verify binding
		boundColModelIds = new HashSet<String>();
		boundColModelIds.add(m.getId());
		colBindings = columnModelDao.listObjectsBoundToColumn(boundColModelIds, true, 10L, 0);
		assertEquals(1, colBindings.size());
		assertEquals("syn123", colBindings.get(0));
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

}
