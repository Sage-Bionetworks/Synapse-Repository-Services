package org.sagebionetworks.table.cluster.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectField;

@ExtendWith(MockitoExtension.class)
public class ObjectFieldModelResolverImplTest {

	@Mock
	private ObjectFieldTypeMapper mockFieldTypeMapper;

	@InjectMocks
	private ObjectFieldModelResolverImpl resolver;

	@Test
	public void testResolveColumnTypeWithFieldHavingColumnType() {
		ObjectField field = ObjectField.etag;

		// call under test
		ColumnType type = resolver.resolveColumnType(field);

		assertEquals(field.getColumnType(), type);

		verifyZeroInteractions(mockFieldTypeMapper);
	}

	@Test
	public void testResolveColumnTypeWithFieldHavingNullColumnType() {

		ColumnType expectedType = ColumnType.ENTITYID;

		when(mockFieldTypeMapper.getIdColumnType()).thenReturn(expectedType);
		when(mockFieldTypeMapper.getParentIdColumnType()).thenReturn(expectedType);
		when(mockFieldTypeMapper.getBenefactorIdColumnType()).thenReturn(expectedType);

		for (ObjectField field : ObjectField.values()) {

			if (field.getColumnType() == null) {
				// Call under test
				ColumnType type = resolver.resolveColumnType(field);

				assertEquals(expectedType, type);

				switch (field) {
				case id:
					verify(mockFieldTypeMapper).getIdColumnType();
					break;
				case parentId:
					verify(mockFieldTypeMapper).getParentIdColumnType();
					break;
				case benefactorId:
					verify(mockFieldTypeMapper).getBenefactorIdColumnType();
					break;

				default:
					fail("There should not be any call to the fieldTypeMapper for field " + field.name());
				}
			}
		}

	}

	@Test
	public void testResolveColumnTypeWithIdField() {

		ColumnType expectedType = ColumnType.ENTITYID;

		when(mockFieldTypeMapper.getIdColumnType()).thenReturn(expectedType);

		ObjectField field = ObjectField.id;

		// call under test
		ColumnType type = resolver.resolveColumnType(field);

		assertEquals(expectedType, type);

		verify(mockFieldTypeMapper).getIdColumnType();

		verifyNoMoreInteractions(mockFieldTypeMapper);
	}

	@Test
	public void testResolveColumnTypeWithParentIdField() {

		ColumnType expectedType = ColumnType.ENTITYID;

		when(mockFieldTypeMapper.getParentIdColumnType()).thenReturn(expectedType);

		ObjectField field = ObjectField.parentId;

		// call under test
		ColumnType type = resolver.resolveColumnType(field);

		assertEquals(expectedType, type);

		verify(mockFieldTypeMapper).getParentIdColumnType();

		verifyNoMoreInteractions(mockFieldTypeMapper);
	}

	@Test
	public void testResolveColumnTypeWithBenefactorIdField() {

		ColumnType expectedType = ColumnType.ENTITYID;

		when(mockFieldTypeMapper.getBenefactorIdColumnType()).thenReturn(expectedType);

		ObjectField field = ObjectField.benefactorId;

		// call under test
		ColumnType type = resolver.resolveColumnType(field);

		assertEquals(expectedType, type);

		verify(mockFieldTypeMapper).getBenefactorIdColumnType();

		verifyNoMoreInteractions(mockFieldTypeMapper);
	}

	@Test
	public void testGetColumnModelWithFieldHavingNullType() {

		ColumnModel expectedModel = new ColumnModel();

		ObjectField field = ObjectField.id;

		expectedModel.setName(field.name());
		expectedModel.setMaximumSize(field.getSize());
		expectedModel.setColumnType(ColumnType.ENTITYID);
		expectedModel.setFacetType(field.getFacetType());

		when(mockFieldTypeMapper.getIdColumnType()).thenReturn(expectedModel.getColumnType());

		// call under test
		ColumnModel model = resolver.getColumnModel(field);

		assertEquals(expectedModel, model);

		verify(mockFieldTypeMapper).getIdColumnType();
	}

	@Test
	public void testGetColumnModelWithFieldHavingType() {

		ColumnModel expectedModel = new ColumnModel();

		ObjectField field = ObjectField.etag;

		expectedModel.setName(field.name());
		expectedModel.setMaximumSize(field.getSize());
		expectedModel.setColumnType(field.getColumnType());
		expectedModel.setFacetType(field.getFacetType());

		// call under test
		ColumnModel model = resolver.getColumnModel(field);

		assertEquals(expectedModel, model);

		verifyZeroInteractions(mockFieldTypeMapper);
	}

	@Test
	public void testIsMatchForAll() {
		ColumnType expectedType = ColumnType.ENTITYID;

		when(mockFieldTypeMapper.getIdColumnType()).thenReturn(expectedType);
		when(mockFieldTypeMapper.getParentIdColumnType()).thenReturn(expectedType);
		when(mockFieldTypeMapper.getBenefactorIdColumnType()).thenReturn(expectedType);

		for (ObjectField field : ObjectField.values()) {
			ColumnModel model = resolver.getColumnModel(field);

			// Call under test
			assertTrue(resolver.isMatch(field, model));
		}
	}

	@Test
	public void testIsMatchForAllWithId() {
		ColumnType expectedType = ColumnType.ENTITYID;

		when(mockFieldTypeMapper.getIdColumnType()).thenReturn(expectedType);
		when(mockFieldTypeMapper.getParentIdColumnType()).thenReturn(expectedType);
		when(mockFieldTypeMapper.getBenefactorIdColumnType()).thenReturn(expectedType);

		for (ObjectField field : ObjectField.values()) {
			ColumnModel model = resolver.getColumnModel(field);
			model.setId("123");

			// Call under test
			assertTrue(resolver.isMatch(field, model));
		}
	}

	@Test
	public void testIsMatchSizeGreater() {
		ObjectField field = ObjectField.name;

		ColumnModel model = new ColumnModel();
		model.setId("123");
		model.setColumnType(field.getColumnType());
		model.setName(field.name());
		// size greater
		model.setMaximumSize(field.getSize() + 1);

		// Call under test
		assertTrue(resolver.isMatch(field, model));
	}

	@Test
	public void testIsMatchSizeEqual() {
		ObjectField field = ObjectField.name;

		ColumnModel model = new ColumnModel();
		model.setId("123");
		model.setColumnType(field.getColumnType());
		model.setName(field.name());
		// size greater
		model.setMaximumSize(field.getSize());

		// Call under test
		assertTrue(resolver.isMatch(field, model));
	}

	@Test
	public void testIsMatchSizeLess() {
		ObjectField field = ObjectField.name;

		ColumnModel model = new ColumnModel();
		model.setId("123");
		model.setColumnType(field.getColumnType());
		model.setName(field.name());
		// size greater
		model.setMaximumSize(field.getSize() - 1);

		// Call under test
		assertFalse(resolver.isMatch(field, model));
	}

	@Test
	public void testIsMatchSizeNull() {
		ObjectField field = ObjectField.name;

		ColumnModel model = new ColumnModel();
		model.setId("123");
		model.setColumnType(field.getColumnType());
		model.setName(field.name());
		// size greater
		model.setMaximumSize(null);

		// Call under test
		assertFalse(resolver.isMatch(field, model));

	}

	@Test
	public void testIsMatchNoFacets() {

		ObjectField field = ObjectField.type;

		// this models is not faceted but it should still match.
		ColumnModel model = new ColumnModel();
		model.setId("123");
		model.setColumnType(field.getColumnType());
		model.setName(field.name());
		model.setMaximumSize(field.getSize());
		model.setFacetType(null);

		// Call under test
		assertTrue(resolver.isMatch(field, model));
	}
	

	@Test
	public void testIsMatchFalseSize(){
		ObjectField field = ObjectField.name;
		ColumnModel model = resolver.getColumnModel(field);
		
		model.setMaximumSize(null);
		
		// Call under test
		assertFalse(resolver.isMatch(field, model));
	}
	
	/**
	 * Test a default column that does not have a maxSize,
	 * but the test column does have a maxSize.
	 * 
	 */
	@Test
	public void testIsMatchSizeNotNull(){
		ObjectField field = ObjectField.createdOn;
		ColumnModel model = resolver.getColumnModel(field);
		
		model.setMaximumSize(123L);
		
		// Call under test
		assertTrue(resolver.isMatch(field, model));
	}
	
	@Test
	public void testIsMatchDifferentType(){
		ObjectField field = ObjectField.name;
		ColumnModel model = resolver.getColumnModel(field);
		model.setColumnType(ColumnType.BOOLEAN);
		
		// Call under test
		assertFalse(resolver.isMatch(field, model));
	}
	
	@Test
	public void testIsMatchDifferentName(){
		ObjectField field = ObjectField.name;
		ColumnModel model = resolver.getColumnModel(field);
		model.setName("foo");
		
		// Call under test
		assertFalse(resolver.isMatch(field, model));
	}
	
	@Test
	public void testIsMatchNameNull(){
		ObjectField field = ObjectField.name;
		ColumnModel model = new ColumnModel();
		
		// Call under test
		assertFalse(resolver.isMatch(field, model));
	}
	
	@Test
	public void testIsMatchTypeNull(){
		ObjectField field = ObjectField.name;
		ColumnModel model = resolver.getColumnModel(field);
		model.setColumnType(null);
		
		// Call under test
		assertFalse(resolver.isMatch(field, model));
	}
	
	@Test
	public void testFindMatchNotFound(){
		ColumnModel model = new ColumnModel();
		model.setName("noMatch");
		model.setColumnType(ColumnType.BOOLEAN);
		model.setDefaultValue("someDefault");
		
		// Call under test
		assertFalse(resolver.findMatch(model).isPresent());
	}
	
	@Test
	public void testFindMatchAll(){
		
		ColumnType expectedType = ColumnType.ENTITYID;
		
		when(mockFieldTypeMapper.getIdColumnType()).thenReturn(expectedType);
		when(mockFieldTypeMapper.getParentIdColumnType()).thenReturn(expectedType);
		when(mockFieldTypeMapper.getBenefactorIdColumnType()).thenReturn(expectedType);
		
		// should find a match for all EntityFields
		for(ObjectField field: ObjectField.values()){
			ColumnModel model = resolver.getColumnModel(field);
			
			// Call under test
			assertTrue(resolver.findMatch(model).isPresent());
		}
	}

}
