package org.sagebionetworks.repo.model.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

public class EntityFieldTest {

	@Test
	public void testGetColumnModelNoSize(){
		ColumnModel model = EntityField.benefactorId.getColumnModel();
		assertEquals(EntityField.benefactorId.name(), model.getName());
		assertEquals(EntityField.benefactorId.colunmType, model.getColumnType());
		assertEquals(null, model.getMaximumSize());
	}
	
	@Test
	public void testGetColumnModelWithSize(){
		ColumnModel model = EntityField.name.getColumnModel();
		assertEquals(EntityField.name.name(), model.getName());
		assertEquals(EntityField.name.colunmType, model.getColumnType());
		assertEquals(EntityField.name.size, model.getMaximumSize());
	}

	@Test
	public void testIsMatchTrue(){
		for(EntityField field: EntityField.values()){
			// each field should match the column created by the field.
			ColumnModel cm = field.getColumnModel();
			assertTrue("name: "+field.name(),field.isMatch(cm));
		}
	}
	
	@Test
	public void testIsMatchTrueWithId(){
		for(EntityField field: EntityField.values()){
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
		model.setName(EntityField.name.name());
		// size greater
		model.setMaximumSize(EntityField.name.getColumnModel().getMaximumSize()+1);
		assertTrue(EntityField.name.isMatch(model));
	}
	
	@Test
	public void testIsMatchSizeEqual(){
		ColumnModel model = new ColumnModel();
		model.setId("123");
		model.setColumnType(ColumnType.STRING);
		model.setName(EntityField.name.name());
		// size equals
		model.setMaximumSize(EntityField.name.getColumnModel().getMaximumSize());
		assertTrue(EntityField.name.isMatch(model));
	}
	
	@Test
	public void testIsMatchSizeLess(){
		ColumnModel model = new ColumnModel();
		model.setId("123");
		model.setColumnType(ColumnType.STRING);
		model.setName(EntityField.name.name());
		// size less
		model.setMaximumSize(EntityField.name.getColumnModel().getMaximumSize()-1);
		assertFalse(EntityField.name.isMatch(model));
	}
	
	@Test
	public void testIsMatchSizeNull(){
		ColumnModel model = new ColumnModel();
		model.setId("123");
		model.setColumnType(ColumnType.STRING);
		model.setName(EntityField.name.name());
		// size null
		model.setMaximumSize(null);
		assertFalse(EntityField.name.isMatch(model));
	}
	
	@Test
	public void testIsMatchNoFacets(){
		// this models is not faceted but it should still match.
		ColumnModel model = new ColumnModel();
		model.setId("123");
		model.setColumnType(ColumnType.ENTITYID);
		model.setName(EntityField.parentId.name());
		model.setFacetType(null);
		assertTrue(EntityField.parentId.isMatch(model));
	}
	
	@Test
	public void testIsMatchFalseSize(){
		ColumnModel model = EntityField.name.getColumnModel();
		model.setMaximumSize(null);
		assertFalse(EntityField.name.isMatch(model));
	}
	
	/**
	 * Test a default column that does not have a maxSize,
	 * but the test column does have a maxSize.
	 * 
	 */
	@Test
	public void testIsMatchSizeNotNull(){
		ColumnModel model = EntityField.id.getColumnModel();
		model.setMaximumSize(123L);
		assertTrue(EntityField.id.isMatch(model));
	}
	
	@Test
	public void testIsMatchDifferentType(){
		ColumnModel model = EntityField.name.getColumnModel();
		model.setColumnType(ColumnType.BOOLEAN);
		assertFalse(EntityField.name.isMatch(model));
	}
	
	@Test
	public void testIsMatchDifferentName(){
		ColumnModel model = EntityField.name.getColumnModel();
		model.setName("foo");
		assertFalse(EntityField.name.isMatch(model));
	}
	
	@Test
	public void testIsMatchNameNull(){
		ColumnModel model = new ColumnModel();
		assertFalse(EntityField.name.isMatch(model));
	}
	
	@Test
	public void testIsMatchTypeNull(){
		ColumnModel model = EntityField.name.getColumnModel();
		model.setColumnType(null);
		assertFalse(EntityField.name.isMatch(model));
	}
	
	@Test
	public void testFindMatchNotFound(){
		ColumnModel model = new ColumnModel();
		model.setName("noMatch");
		model.setColumnType(ColumnType.BOOLEAN);
		model.setDefaultValue("someDefault");
		EntityField match = EntityField.findMatch(model);
		assertEquals(null, match);
	}
	
	@Test
	public void testFindMatchAll(){
		// should find a match for all EntityFields
		for(EntityField field: EntityField.values()){
			ColumnModel model = field.getColumnModel();
			EntityField match = EntityField.findMatch(model);
			assertEquals(field, match);
		}
	}
	
	@Test
	public void testFindMatchList(){
		ColumnModel idColumn = EntityField.id.getColumnModel();
		ColumnModel etagColumn = EntityField.etag.getColumnModel();
		List<ColumnModel> list = Lists.newArrayList(idColumn, etagColumn);
		
		ColumnModel result = EntityField.findMatch(list, EntityField.id);
		assertEquals(idColumn, result);
		result = EntityField.findMatch(list, EntityField.etag);
		assertEquals(etagColumn, result);
		result = EntityField.findMatch(list, EntityField.name);
		assertEquals(null, result);
	}
}
