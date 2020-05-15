package org.sagebionetworks.repo.manager.table.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolver;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolverFactory;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class DefaultColumnModelMapperTest {

	@Mock
	private MetadataIndexProviderFactory mockMetadataIndexProviderFactory;
	@Mock
	private ObjectFieldModelResolverFactory mockObjectFieldModelResolverFactory;
	@Mock
	private ColumnModelManager mockColumnModelManager;

	@InjectMocks
	private DefaultColumnModelMapperImpl mapper;

	@Mock
	private MetadataIndexProvider mockMetadataIndexProvider;

	@Mock
	private ObjectFieldModelResolver mockObjectFieldModelResolver;

	@Mock
	private DefaultColumnModel mockDefaultModel;

	@Mock
	private ColumnModel mockModel;

	private ViewObjectType objectType;

	@BeforeEach
	public void before() {
		objectType = ViewObjectType.ENTITY;
	}

	@Test
	public void testMapObjectFields() {

		List<ObjectField> fields = ImmutableList.of(ObjectField.id, ObjectField.etag);

		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any()))
				.thenReturn(mockObjectFieldModelResolver);
		when(mockObjectFieldModelResolver.getColumnModel(any())).thenReturn(mockModel);
		when(mockColumnModelManager.createColumnModel(any())).thenReturn(mockModel);
		when(mockDefaultModel.getObjectType()).thenReturn(objectType);
		when(mockDefaultModel.getDefaultFields()).thenReturn(fields);

		List<ColumnModel> expected = ImmutableList.of(mockModel, mockModel);

		// Call under test
		List<ColumnModel> result = mapper.map(mockDefaultModel);

		verify(mockMetadataIndexProviderFactory).getMetadataIndexProvider(objectType);
		verify(mockObjectFieldModelResolverFactory).getObjectFieldModelResolver(mockMetadataIndexProvider);
		verify(mockObjectFieldModelResolver, times(fields.size())).getColumnModel(any());
		verify(mockColumnModelManager, times(fields.size())).createColumnModel(any());

		assertEquals(expected, result);
	}

	@Test
	public void testMapWithCustomFields() {

		List<ObjectField> fields = ImmutableList.of(ObjectField.id, ObjectField.etag);
		List<ColumnModel> customFields = ImmutableList.of(mockModel);

		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any()))
				.thenReturn(mockObjectFieldModelResolver);
		when(mockObjectFieldModelResolver.getColumnModel(any())).thenReturn(mockModel);
		when(mockColumnModelManager.createColumnModel(any())).thenReturn(mockModel);
		when(mockDefaultModel.getObjectType()).thenReturn(objectType);
		when(mockDefaultModel.getDefaultFields()).thenReturn(fields);
		when(mockDefaultModel.getCustomFields()).thenReturn(customFields);

		List<ColumnModel> expected = ImmutableList.of(mockModel, mockModel, mockModel);

		// Call under test
		List<ColumnModel> result = mapper.map(mockDefaultModel);

		verify(mockMetadataIndexProviderFactory).getMetadataIndexProvider(objectType);
		verify(mockObjectFieldModelResolverFactory).getObjectFieldModelResolver(mockMetadataIndexProvider);
		verify(mockObjectFieldModelResolver, times(fields.size())).getColumnModel(any());
		verify(mockColumnModelManager, times(fields.size() + customFields.size())).createColumnModel(any());

		assertEquals(expected, result);
	}

	@Test
	public void testMapWithNullInput() {

		DefaultColumnModel columnModel = null;

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			mapper.map(columnModel);
		});

		assertEquals("defaultColumns is required.", ex.getMessage());
	}

	@Test
	public void testMapWithNoObjectType() {

		objectType = null;

		when(mockDefaultModel.getObjectType()).thenReturn(objectType);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			mapper.map(mockDefaultModel);
		});

		assertEquals("defaultColumns.objectType is required.", ex.getMessage());
	}

	@Test
	public void testMapWithNoDefaultFields() {
		List<ObjectField> fields = null;

		when(mockDefaultModel.getObjectType()).thenReturn(objectType);
		when(mockDefaultModel.getDefaultFields()).thenReturn(fields);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			mapper.map(mockDefaultModel);
		});

		assertEquals("defaultColumns.defaultFields is required and must not be empty.", ex.getMessage());
	}

	@Test
	public void testMapWithEmptyDefaultFields() {
		List<ObjectField> fields = Collections.emptyList();

		when(mockDefaultModel.getObjectType()).thenReturn(objectType);
		when(mockDefaultModel.getDefaultFields()).thenReturn(fields);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			mapper.map(mockDefaultModel);
		});

		assertEquals("defaultColumns.defaultFields is required and must not be empty.", ex.getMessage());
	}

	@Test
	public void testGetColumnModels() {
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any()))
				.thenReturn(mockObjectFieldModelResolver);
		when(mockObjectFieldModelResolver.getColumnModel(any())).thenReturn(mockModel);
		when(mockColumnModelManager.createColumnModel(any())).thenReturn(mockModel);

		ObjectField[] fields = new ObjectField[] { ObjectField.id, ObjectField.name };

		List<ColumnModel> expected = ImmutableList.of(mockModel, mockModel);
		// Call under test
		List<ColumnModel> result = mapper.getColumnModels(objectType, fields);

		verify(mockMetadataIndexProviderFactory).getMetadataIndexProvider(objectType);
		verify(mockObjectFieldModelResolverFactory).getObjectFieldModelResolver(mockMetadataIndexProvider);
		for (ObjectField field : fields) {
			verify(mockObjectFieldModelResolver).getColumnModel(field);
		}
		verify(mockColumnModelManager, times(fields.length)).createColumnModel(mockModel);
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetColumnModelsWithNullObjectType() {
		objectType = null;
		ObjectField[] fields = new ObjectField[] { ObjectField.id, ObjectField.name };

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			mapper.getColumnModels(objectType, fields);
		});
		
		assertEquals("objectType is required.", ex.getMessage());

	}
	
	@Test
	public void testGetColumnModelsWithNullFields() {
		ObjectField[] fields = null;

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			mapper.getColumnModels(objectType, fields);
		});
		
		assertEquals("fields is required.", ex.getMessage());

	}
}
