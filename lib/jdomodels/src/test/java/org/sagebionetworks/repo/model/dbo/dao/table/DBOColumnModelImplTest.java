package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOColumnModelImplTest {
	
	@Autowired
	ColumnModelDAO columnModelDao;
	
	@Autowired
	DBOChangeDAO changeDAO;
	
	ColumnModel one;
	ColumnModel two;
	ColumnModel three;
	
	@Before
	public void before() throws DatastoreException, NotFoundException{
		// One
		one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.STRING);
		one = columnModelDao.createColumnModel(one);
		// two
		two = new ColumnModel();
		two.setName("two");
		two.setColumnType(ColumnType.STRING);
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
		model.setDefaultValue("someDefaultValue");
		model.setEnumValues(new LinkedList<String>());
		model.getEnumValues().add("xyz");
		model.getEnumValues().add("abc");
		// Create it.
		ColumnModel result = columnModelDao.createColumnModel(model);
		assertNotNull(result);
		assertNotNull(result.getId());
		assertEquals("column model dao test", result.getName());
		assertEquals(ColumnType.STRING, result.getColumnType());
		assertEquals("somedefaultvalue", result.getDefaultValue());
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
		columnModelDao.delete(originalId);
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
		// The rows should be alphabetical order by name.
		assertEquals(one, list.get(0));
		assertEquals(three, list.get(1));
	}
	
	@Test
	public void testBindColumns() throws DatastoreException, NotFoundException{
		// Now bind one column
		List<String> toBind = new LinkedList<String>();
		toBind.add(two.getId());
		int count = columnModelDao.bindColumnToObject(toBind, "syn123");
		assertTrue(count > 0);
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
			int count = columnModelDao.bindColumnToObject(toBind, "syn123");
			fail("Should have thrown an exception");
		}catch(NotFoundException e){
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().contains(fakeId));
		}
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
		List<ColumnModel> raw = TableModelUtils.createOneOfEachType();
		// Create each one
		List<ColumnModel> models = new LinkedList<ColumnModel>();
		for(ColumnModel cm: raw){
			models.add(columnModelDao.createColumnModel(cm));
		}
		// Shuffle the results so the order they are bound to does not match the natural order
		Collections.shuffle(models);
		List<String> headers = TableModelUtils.getHeaders(models);
		// Now bind them to the table in this shuffled order
		String tableId = "syn123";
		columnModelDao.bindColumnToObject(headers, tableId);
		// Now make sure we can fetch this back in the same order that we bound the columns
		List<ColumnModel> fetched = columnModelDao.getColumnModelsForObject(tableId);
		assertNotNull(fetched);
		assertEquals(models, fetched);
		// Now if we update the columns bound to the object the order should change
		models.remove(0);
		Collections.shuffle(models);
		headers = TableModelUtils.getHeaders(models);
		// Bind the new columns in a new order
		columnModelDao.bindColumnToObject(headers, tableId);
		// Get them back in the same order with the same columns
		fetched = columnModelDao.getColumnModelsForObject(tableId);
		assertNotNull(fetched);
		assertEquals(models, fetched);
	}
	
	/**
	 * Test that we send a change message when new columns are bound to a table.
	 * @throws NotFoundException 
	 */
	@Test
	public void testBindMessageSent() throws NotFoundException{
		// What is the current change number
		long startNumber = changeDAO.getCurrentChangeNumber();
		List<String> toBind = new LinkedList<String>();
		toBind.add(two.getId());
		toBind.add(one.getId());
		String tableId = "syn123";
		String changeId = KeyFactory.stringToKey(tableId).toString();
		columnModelDao.bindColumnToObject(toBind, tableId);
		// Did a message get sent?
		List<ChangeMessage> changes = changeDAO.listChanges(startNumber+1, ObjectType.TABLE, Long.MAX_VALUE);
		assertNotNull(changes);
		assertEquals("Changing the column binding of a table did not fire a change message",1, changes.size());
		ChangeMessage message = changes.get(0);
		assertNotNull(message);
		assertEquals(changeId, message.getObjectId());
		assertEquals(ChangeType.CREATE, message.getChangeType());
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
