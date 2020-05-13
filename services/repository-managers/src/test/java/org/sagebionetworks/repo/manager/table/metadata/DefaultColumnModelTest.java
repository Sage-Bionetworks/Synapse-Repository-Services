package org.sagebionetworks.repo.manager.table.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.ViewObjectType;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class DefaultColumnModelTest {
	
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
		
		ColumnModel column1 = new ColumnModel();
		column1.setName("foo");
		column1.setColumnType(ColumnType.STRING);
		
		ColumnModel column2 = new ColumnModel();
		column2.setName("bar");
		column2.setColumnType(ColumnType.INTEGER);
		
		List<ObjectField> defaultFields = ImmutableList.of(ObjectField.id, ObjectField.etag);
		List<ColumnModel> customFields = ImmutableList.of(column1, column2);
		
		DefaultColumnModel expected = new DefaultColumnModel(objectType, defaultFields, customFields);
		
		DefaultColumnModel model = DefaultColumnModel.builder(objectType)
				.withObjectField(defaultFields.toArray(new ObjectField[defaultFields.size()]))
				.withColumnModel(customFields.toArray(new ColumnModel[customFields.size()]))
				.build();
		
		assertEquals(expected, model);
		
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

}
