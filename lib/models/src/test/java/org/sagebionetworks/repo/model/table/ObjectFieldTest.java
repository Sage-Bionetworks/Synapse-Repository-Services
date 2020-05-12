package org.sagebionetworks.repo.model.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

public class ObjectFieldTest {

	@Test
	public void testGetColumnModelNoSize(){
		ColumnModel model = ObjectField.benefactorId.getColumnModel();
		assertNull(model.getMaximumSize());
	}
	
	@Test
	public void testGetColumnModelWithSize(){
		ColumnModel model = ObjectField.name.getColumnModel();
		assertNotNull(model.getMaximumSize());
	}
	
	@Test
	public void testFindMatchNotFound(){
		ColumnModel model = new ColumnModel();
		model.setName("noMatch");
		model.setColumnType(ColumnType.BOOLEAN);
		model.setDefaultValue("someDefault");
		ObjectField match = ObjectField.findMatch(model);
		assertEquals(null, match);
	}
	
	@Test
	public void testFindMatchAll(){
		// should find a match for all EntityFields
		for(ObjectField field: ObjectField.values()){
			ColumnModel model = field.getColumnModel();
			ObjectField match = ObjectField.findMatch(model);
			assertEquals(field, match);
		}
	}
	
	@Test
	public void testFindMatchList(){
		ColumnModel idColumn = ObjectField.id.getColumnModel();
		ColumnModel etagColumn = ObjectField.etag.getColumnModel();
		List<ColumnModel> list = Lists.newArrayList(idColumn, etagColumn);
		
		ColumnModel result = ObjectField.findMatch(list, ObjectField.id);
		assertEquals(idColumn, result);
		result = ObjectField.findMatch(list, ObjectField.etag);
		assertEquals(etagColumn, result);
		result = ObjectField.findMatch(list, ObjectField.name);
		assertEquals(null, result);
	}
}
