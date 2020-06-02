package org.sagebionetworks.repo.manager.table.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.DefaultField;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.ViewObjectType;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class DefaultColumnModelTest {
	
	@Mock
	private DefaultField mockDefaultField;
	
	@Test
	public void testDefaultColumnModelBuilderWithObjectFields() {
		ViewObjectType objectType = ViewObjectType.ENTITY;
		
		List<ObjectField> defaultFields = ImmutableList.of(ObjectField.id, ObjectField.etag);
		List<ColumnModel> customFields = Collections.emptyList();
		
		DefaultColumnModel expected = new DefaultColumnModel(objectType, defaultFields, customFields);
		
		DefaultColumnModel model = DefaultColumnModel.builder(objectType)
				.withObjectField(defaultFields.toArray(new ObjectField[defaultFields.size()]))
				.build();
		
		assertEquals(expected, model);
		
	}
	
	@Test
	public void testDefaultColumnModelBuilderWithCustomFields() {
		ViewObjectType objectType = ViewObjectType.ENTITY;
		
		String columnName = "myColumn";
		ColumnType columnType = ColumnType.STRING;
		
		when(mockDefaultField.getColumnName()).thenReturn(columnName);
		when(mockDefaultField.getColumnType()).thenReturn(columnType);
		
		// This two are needed because by default the mock initializes the values
		when(mockDefaultField.getMaximumSize()).thenReturn(null);
		when(mockDefaultField.getEnumValues()).thenReturn(null);
		
		ColumnModel column = new ColumnModel();
		column.setName(columnName);
		column.setColumnType(columnType);
		
		List<ObjectField> defaultFields = ImmutableList.of(ObjectField.id, ObjectField.etag);
		List<ColumnModel> customFields = ImmutableList.of(column);
		
		DefaultColumnModel expected = new DefaultColumnModel(objectType, defaultFields, customFields);
		
		DefaultColumnModel model = DefaultColumnModel.builder(objectType)
				.withObjectField(defaultFields.toArray(new ObjectField[defaultFields.size()]))
				.withCustomField(mockDefaultField)
				.build();
		
		assertEquals(expected, model);
		
	}
	
	@Test
	public void testDefaultColumnModelBuilderWithCustomFieldsAndNullName() {
		ViewObjectType objectType = ViewObjectType.ENTITY;
		
		String columnName = null;
		
		when(mockDefaultField.getColumnName()).thenReturn(columnName);
		
		List<ObjectField> defaultFields = ImmutableList.of(ObjectField.id, ObjectField.etag);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			DefaultColumnModel.builder(objectType)
					.withObjectField(defaultFields.toArray(new ObjectField[defaultFields.size()]))
					.withCustomField(mockDefaultField)
					.build();
		}).getMessage();
		
		assertEquals("columnName is required and must not be the empty string.", errorMessage);
		
	}

	@Test
	public void testDefaultColumnModelBuilderWithCustomFieldsAndNullType() {
		ViewObjectType objectType = ViewObjectType.ENTITY;
		
		String columnName = "myColumn";
		ColumnType columnType = null;
		
		when(mockDefaultField.getColumnName()).thenReturn(columnName);
		when(mockDefaultField.getColumnType()).thenReturn(columnType);
		
		List<ObjectField> defaultFields = ImmutableList.of(ObjectField.id, ObjectField.etag);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			DefaultColumnModel.builder(objectType)
					.withObjectField(defaultFields.toArray(new ObjectField[defaultFields.size()]))
					.withCustomField(mockDefaultField)
					.build();
		}).getMessage();
		
		assertEquals("columnType is required.", errorMessage);
		
	}
	
	@Test
	public void testDefaultColumnModelBuilderWithNullObjectType() {
		ViewObjectType objectType = null;
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			DefaultColumnModel.builder(objectType);
		});
		
		assertEquals("objectType is required.", ex.getMessage());
	}
	
	@Test
	public void testDefaultColumnModelBuilderWithNoDefaultField() {
		ViewObjectType objectType = ViewObjectType.ENTITY;
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			DefaultColumnModel.builder(objectType).build();
		});
		
		assertEquals("At least one objectField must be defined", ex.getMessage());
	}
	
	@Test
	public void testFindCustomFieldByName() {
		ViewObjectType objectType = ViewObjectType.ENTITY;
		String columnName = "myColumn";
		ColumnType columnType = ColumnType.STRING;
		
		when(mockDefaultField.getColumnName()).thenReturn(columnName);
		when(mockDefaultField.getColumnType()).thenReturn(columnType);
		
		// This two are needed because by default the mock initializes the values
		when(mockDefaultField.getMaximumSize()).thenReturn(null);
		when(mockDefaultField.getEnumValues()).thenReturn(null);
		
		DefaultColumnModel model = DefaultColumnModel.builder(objectType)
				.withObjectField(ObjectField.id)
				.withCustomField(mockDefaultField)
				.build();
		
		ColumnModel expected = new ColumnModel();
		expected.setName(columnName);
		expected.setColumnType(columnType);
		
		// Call under test
		Optional<ColumnModel> match = model.findCustomFieldByColumnName(columnName);
		
		assertTrue(match.isPresent());
		assertEquals(expected, match.get());
	}
	
	@Test
	public void testFindCustomFieldByNameWithNoCustomFields() {
		ViewObjectType objectType = ViewObjectType.ENTITY;
		
		DefaultColumnModel model = DefaultColumnModel.builder(objectType)
				.withObjectField(ObjectField.id)
				.build();
		
		String findColumn = "id";
		
		// Call under test
		Optional<ColumnModel> match = model.findCustomFieldByColumnName(findColumn);
		
		assertFalse(match.isPresent());
	}
	
	@Test
	public void testFindCustomFieldByNameWithEmptyColumn() {
		ViewObjectType objectType = ViewObjectType.ENTITY;
		
		String columnName = "myColumn";
		ColumnType columnType = ColumnType.STRING;
		
		when(mockDefaultField.getColumnName()).thenReturn(columnName);
		when(mockDefaultField.getColumnType()).thenReturn(columnType);
		
		// This two are needed because by default the mock initializes the values
		when(mockDefaultField.getMaximumSize()).thenReturn(null);
		when(mockDefaultField.getEnumValues()).thenReturn(null);
		
		DefaultColumnModel model = DefaultColumnModel.builder(objectType)
				.withObjectField(ObjectField.id)
				.withCustomField(mockDefaultField)
				.build();
		
		String findColumn = " ";

		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			model.findCustomFieldByColumnName(findColumn);
		});
		

	}
	
	@Test
	public void testFindCustomFieldByNameWithNullColumn() {
		ViewObjectType objectType = ViewObjectType.ENTITY;
		
		String columnName = "myColumn";
		ColumnType columnType = ColumnType.STRING;
		
		when(mockDefaultField.getColumnName()).thenReturn(columnName);
		when(mockDefaultField.getColumnType()).thenReturn(columnType);
		
		// This two are needed because by default the mock initializes the values
		when(mockDefaultField.getMaximumSize()).thenReturn(null);
		when(mockDefaultField.getEnumValues()).thenReturn(null);
		
		DefaultColumnModel model = DefaultColumnModel.builder(objectType)
				.withObjectField(ObjectField.id)
				.withCustomField(mockDefaultField)
				.build();
		
		String findColumn = " ";

		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			model.findCustomFieldByColumnName(findColumn);
		});
		

	}

}
