package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
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
	List<String> toDelete;
	
	@Before
	public void before(){
		toDelete = new LinkedList<String>();
	}
	
	@After
	public void after(){
		if(toDelete != null){
			for(String id: toDelete){
				columnModelDao.delete(id);
			}
		}
	}
	
	@Test
	public void testRoundTrip() throws Exception {
		// To save
		ColumnModel model = new ColumnModel();
		model.setName("column model dao test");
		model.setColumnType(ColumnType.ENUM);
		model.setDefaultValue("someDefaultValue");
		model.setEnumValues(new LinkedList<String>());
		model.getEnumValues().add("xyz");
		model.getEnumValues().add("abc");
		// Create it.
		ColumnModel result = columnModelDao.createColumnModel(model);
		assertNotNull(result);
		assertNotNull(result.getId());
		toDelete.add(result.getId());
		assertEquals("column model dao test", result.getName());
		assertEquals(ColumnType.ENUM, result.getColumnType());
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

}
