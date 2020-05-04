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
	public void testIsMatchTrue(){
		for(ObjectField field: ObjectField.values()){
			// each field should match the column created by the field.
			ColumnModel cm = field.getColumnModel();
			assertTrue("name: "+field.name(),field.isMatch(cm));
		}
	}
	
	@Test
	public void testIsMatchTrueWithId(){
		for(ObjectField field: ObjectField.values()){
			// each field should match the column created by the field.
			ColumnModel cm = field.getColumnModel();
			cm.setId("123");
			assertTrue("name: "+field.name(),field.isMatch(cm));
		}
	}
	
	@Test
	public void testIsMatchSizeGreater(){
		ColumnModel model = new ColumnModel();
		model.setId("123");
		model.setColumnType(ColumnType.STRING);
		model.setName(ObjectField.name.name());
		// size greater
		model.setMaximumSize(ObjectField.name.getColumnModel().getMaximumSize()+1);
		assertTrue(ObjectField.name.isMatch(model));
	}
	
	@Test
	public void testIsMatchSizeEqual(){
		ColumnModel model = new ColumnModel();
		model.setId("123");
		model.setColumnType(ColumnType.STRING);
		model.setName(ObjectField.name.name());
		// size equals
		model.setMaximumSize(ObjectField.name.getColumnModel().getMaximumSize());
		assertTrue(ObjectField.name.isMatch(model));
	}
	
	@Test
	public void testIsMatchSizeLess(){
		ColumnModel model = new ColumnModel();
		model.setId("123");
		model.setColumnType(ColumnType.STRING);
		model.setName(ObjectField.name.name());
		// size less
		model.setMaximumSize(ObjectField.name.getColumnModel().getMaximumSize()-1);
		assertFalse(ObjectField.name.isMatch(model));
	}
	
	@Test
	public void testIsMatchSizeNull(){
		ColumnModel model = new ColumnModel();
		model.setId("123");
		model.setColumnType(ColumnType.STRING);
		model.setName(ObjectField.name.name());
		// size null
		model.setMaximumSize(null);
		assertFalse(ObjectField.name.isMatch(model));
	}
	
	@Test
	public void testIsMatchNoFacets(){
		// this models is not faceted but it should still match.
		ColumnModel model = new ColumnModel();
		model.setId("123");
		model.setColumnType(ColumnType.ENTITYID);
		model.setName(ObjectField.parentId.name());
		model.setFacetType(null);
		assertTrue(ObjectField.parentId.isMatch(model));
	}
	
	@Test
	public void testIsMatchFalseSize(){
		ColumnModel model = ObjectField.name.getColumnModel();
		model.setMaximumSize(null);
		assertFalse(ObjectField.name.isMatch(model));
	}
	
	/**
	 * Test a default column that does not have a maxSize,
	 * but the test column does have a maxSize.
	 * 
	 */
	@Test
	public void testIsMatchSizeNotNull(){
		ColumnModel model = ObjectField.id.getColumnModel();
		model.setMaximumSize(123L);
		assertTrue(ObjectField.id.isMatch(model));
	}
	
	@Test
	public void testIsMatchDifferentType(){
		ColumnModel model = ObjectField.name.getColumnModel();
		model.setColumnType(ColumnType.BOOLEAN);
		assertFalse(ObjectField.name.isMatch(model));
	}
	
	@Test
	public void testIsMatchDifferentName(){
		ColumnModel model = ObjectField.name.getColumnModel();
		model.setName("foo");
		assertFalse(ObjectField.name.isMatch(model));
	}
	
	@Test
	public void testIsMatchNameNull(){
		ColumnModel model = new ColumnModel();
		assertFalse(ObjectField.name.isMatch(model));
	}
	
	@Test
	public void testIsMatchTypeNull(){
		ColumnModel model = ObjectField.name.getColumnModel();
		model.setColumnType(null);
		assertFalse(ObjectField.name.isMatch(model));
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
