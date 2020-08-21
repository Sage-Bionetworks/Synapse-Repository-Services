package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.replication.ReplicationManager;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProviderFactory;
import org.sagebionetworks.repo.model.BucketAndKey;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.InvalidStatusTokenException;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewSnapshot;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewSnapshotDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewScopeType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolver;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolverFactory;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolverImpl;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class TableViewManagerImplTest {

	@Mock
	ViewScopeDao viewScopeDao;
	@Mock
	ColumnModelManager mockColumnModelManager;
	@Mock
	TableManagerSupport mockTableManagerSupport;
	@Mock
	ColumnModelDAO columnModelDao;
	@Mock
	NodeManager mockNodeManager;
	@Mock
	ReplicationManager mockReplicationManager;
	@Mock
	ProgressCallback mockProgressCallback;
	@Mock
	TableIndexConnectionFactory mockConnectionFactory;
	@Mock
	TableIndexManager mockIndexManager;
	@Mock
	FileProvider mockFileProvider;
	@Mock
	SynapseS3Client mockS3Client;
	@Mock
	StackConfiguration mockConfig;
	@Mock
	ViewSnapshotDao mockViewSnapshotDao;
	@Mock
	MetadataIndexProviderFactory mockMetadataIndexProviderFactory;
	@Mock
	ObjectFieldModelResolverFactory mockObjectFieldModelResolverFactory;
	
	@Mock
	File mockFile;
	@Mock
	OutputStream mockOutStream;
	@Mock
	GZIPOutputStream mockGzipOutStream;
	@Mock
	InputStream mockInputStream;
	@Mock
	GZIPInputStream mockGzipInputStream;
	@Mock
	MetadataIndexProvider mockMetadataIndexProvider;
	
	@Captor
	ArgumentCaptor<PutObjectRequest> putRequestCaptor;
	@Captor
	ArgumentCaptor<ViewSnapshot> snapshotCaptor;
	@Captor
	ArgumentCaptor<Annotations> annotationsCaptor;
	
	@InjectMocks
	TableViewManagerImpl manager;
	
	TableViewManagerImpl managerSpy;
	
	UserInfo userInfo;
	List<String> schema;
	List<String> scope;
	String viewId;
	Long viewIdLong;
	IdAndVersion idAndVersion;
	Long viewType;
	
	Set<Long> scopeIds;
	ViewScope viewScope;
	long rowCount;
	List<Row> rows;
	long viewCRC;
	List<ColumnModel> viewSchema;
	ColumnModel etagColumn;
	ColumnModel anno1;
	ColumnModel anno2;
	ColumnModel dateColumn;
	ColumnModel intListColumn;
	SparseRowDto row;
	SnapshotRequest snapshotOptions;
	Set<Long> allContainersInScope;
	org.sagebionetworks.repo.model.Annotations annotations;
	Annotations annotationsV2;
	
	ViewScopeType scopeType;
	
	ObjectFieldModelResolver objectFieldModelResolver;

	@BeforeEach
	public void before(){
		objectFieldModelResolver = new ObjectFieldModelResolverImpl(mockMetadataIndexProvider);
		
		userInfo = new UserInfo(false, 888L);
		schema = Lists.newArrayList("1","2","3");
		scope = Lists.newArrayList("syn123", "syn456");
		scopeIds = new HashSet<Long>(KeyFactory.stringToKey(scope));
		
		viewId = "syn555";
		viewIdLong = KeyFactory.stringToKey(viewId);
		idAndVersion = IdAndVersion.parse(viewId);
		viewType =ViewTypeMask.File.getMask();
		
		viewScope = new ViewScope();
		viewScope.setViewEntityType(ViewEntityType.entityview);
		viewScope.setScope(scope);
		viewScope.setViewTypeMask(viewType);
		
		rowCount = 13;
		rows = new LinkedList<Row>();
		for(long i=0; i<rowCount; i++){
			Row row = new Row();
			row.setRowId(i);
			rows.add(row);
		}
		
		annotationsV2= AnnotationsV2TestUtils.newEmptyAnnotationsV2();

		anno1 = new ColumnModel();
		anno1.setColumnType(ColumnType.STRING);
		anno1.setName("foo");
		anno1.setMaximumSize(50L);
		anno1.setId("1");
		
		anno2 = new ColumnModel();
		anno2.setColumnType(ColumnType.INTEGER);
		anno2.setName("bar");
		anno2.setId("2");
		
		dateColumn = new ColumnModel();
		dateColumn.setName("aDate");
		dateColumn.setColumnType(ColumnType.DATE);
		dateColumn.setId("3");

		intListColumn = new ColumnModel();
		intListColumn.setColumnType(ColumnType.INTEGER_LIST);
		intListColumn.setName("stringList");
		intListColumn.setId("4");

		
		etagColumn = objectFieldModelResolver.getColumnModel(ObjectField.etag);
		etagColumn.setId("3");
		
		viewSchema = new LinkedList<ColumnModel>();
		viewSchema.add(etagColumn);
		viewSchema.add(anno1);
		viewSchema.add(anno2);
		viewSchema.add(intListColumn);
		
		Map<String, String> values = new HashMap<>();
		values.put(etagColumn.getId(), "anEtag");
		values.put(anno1.getId(), "aString");
		values.put(anno2.getId(), "123");
		row = new SparseRowDto();
		row.setRowId(111L);
		row.setValues(values);
		
		snapshotOptions = new SnapshotRequest();
		snapshotOptions.setSnapshotComment("a comment");
		allContainersInScope = Sets.newHashSet(123L, 456L);;
		
		scopeType = new ViewScopeType(ViewObjectType.ENTITY, ViewTypeMask.File.getMask());
		
		managerSpy = Mockito.spy(manager);
	}
	
	@Test
	public void testSetViewSchemaAndScopeOverLimit(){
		IllegalArgumentException overLimit = new IllegalArgumentException("Over limit");
		doThrow(overLimit).when(mockTableManagerSupport).validateScope(any(), anySet());
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.setViewSchemaAndScope(userInfo, schema, viewScope, viewId);
		});
	}
	
	@Test
	public void testSetViewSchemaAndScope(){
		// call under test
		manager.setViewSchemaAndScope(userInfo, schema, viewScope, viewId);
		// the size should be validated
		verify(mockTableManagerSupport).validateScope(scopeType, scopeIds);
		verify(viewScopeDao).setViewScopeAndType(555L, Sets.newHashSet(123L, 456L), scopeType);
		verify(mockColumnModelManager).bindColumnsToDefaultVersionOfObject(schema, viewId);
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);
	}
	
	@Test
	public void testSetViewSchemaAndScopeTooManyColumns(){
		this.schema = new LinkedList<>();
		int columnCount = TableViewManagerImpl.MAX_COLUMNS_PER_VIEW+1;
		for(int i=0; i<columnCount; i++) {
			schema.add(""+i);
		}
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.setViewSchemaAndScope(userInfo, schema, viewScope, viewId);
		}).getMessage();
		// expected
		assertTrue(message.contains(""+TableViewManagerImpl.MAX_COLUMNS_PER_VIEW));
	}
	
	
	@Test
	public void testSetViewSchemaAndScopeWithNullSchema(){
		schema = null;
		// call under test
		manager.setViewSchemaAndScope(userInfo, schema, viewScope, viewId);
		verify(viewScopeDao).setViewScopeAndType(555L, Sets.newHashSet(123L, 456L), scopeType);
		verify(mockColumnModelManager).bindColumnsToDefaultVersionOfObject(null, viewId);
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);
	}
	
	@Test
	public void testSetViewSchemaAndScopeWithNullScope(){
		viewScope.setScope(null);
		// call under test
		manager.setViewSchemaAndScope(userInfo, schema, viewScope, viewId);
		verify(viewScopeDao).setViewScopeAndType(555L, null, scopeType);
		verify(mockColumnModelManager).bindColumnsToDefaultVersionOfObject(schema, viewId);
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);
	}
	
	@Test
	public void testSetViewSchemaAndScopeWithNullType(){
		viewScope.setViewType(null);
		viewScope.setViewTypeMask(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.setViewSchemaAndScope(userInfo, schema, viewScope, viewId);
		});		
	}
	
	/**
	 * Project cannot be combined with anything else.
	 */
	@Test
	public void testSetViewSchemaAndScopeWithProjectOnly(){
		long mask = ViewTypeMask.Project.getMask();
		viewScope.setViewTypeMask(mask);
		ViewScopeType expectedScopeType = new ViewScopeType(ViewObjectType.ENTITY, mask);
		// call under test
		manager.setViewSchemaAndScope(userInfo, schema, viewScope, viewId);
		verify(viewScopeDao).setViewScopeAndType(555L, Sets.newHashSet(123L, 456L), expectedScopeType);
	}
	
	@Test
	public void testSetViewSchemaAndScopeWithNullObjectType(){
		viewScope.setViewEntityType(null);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.setViewSchemaAndScope(userInfo, schema, viewScope, viewId);
		});
		
		assertEquals("The scope entity type is required.", ex.getMessage());
	}
	
	@Test
	public void testGetViewSchemaWithRequiredColumnsNoAdditions(){
		List<ColumnModel> rawSchema = Lists.newArrayList(
				objectFieldModelResolver.getColumnModel(ObjectField.benefactorId),
				objectFieldModelResolver.getColumnModel(ObjectField.createdBy),
				objectFieldModelResolver.getColumnModel(ObjectField.etag)
				);
		when(mockColumnModelManager.getColumnModelsForObject(idAndVersion)).thenReturn(rawSchema);
		// call under test
		List<ColumnModel> result = manager.getViewSchema(idAndVersion);
		assertEquals(rawSchema, result);
	}
	
	@Test
	public void testGetViewSchema(){
		List<ColumnModel> rawSchema = Lists.newArrayList(
				objectFieldModelResolver.getColumnModel(ObjectField.benefactorId),
				objectFieldModelResolver.getColumnModel(ObjectField.createdBy),
				objectFieldModelResolver.getColumnModel(ObjectField.etag)
				);
		when(mockColumnModelManager.getColumnModelsForObject(idAndVersion)).thenReturn(rawSchema);
		// call under test
		List<ColumnModel> result = manager.getViewSchema(idAndVersion);
		
		List<ColumnModel> expected = Lists.newArrayList(
				objectFieldModelResolver.getColumnModel(ObjectField.benefactorId),
				objectFieldModelResolver.getColumnModel(ObjectField.createdBy),
				objectFieldModelResolver.getColumnModel(ObjectField.etag)
				);
		assertEquals(expected, result);
	}
	
	@Test
	public void testApplySchemaChange(){
		ColumnChange change = new ColumnChange();
		change.setOldColumnId(null);
		change.setNewColumnId("456");
		List<ColumnChange> changes = Lists.newArrayList(change);
		ColumnModel model = objectFieldModelResolver.getColumnModel(ObjectField.benefactorId);
		model.setId(change.getNewColumnId());
		List<ColumnModel> schema = Lists.newArrayList(model);
		List<String> newColumnIds = Lists.newArrayList(change.getNewColumnId());
		when(mockColumnModelManager.calculateNewSchemaIdsAndValidate(viewId, changes, newColumnIds)).thenReturn(newColumnIds);
		when(mockColumnModelManager.bindColumnsToDefaultVersionOfObject(newColumnIds, viewId)).thenReturn(schema);
		
		// call under test
		List<ColumnModel> newSchema = manager.applySchemaChange(userInfo, viewId, changes, newColumnIds);
		assertEquals(schema, newSchema);
		verify(mockColumnModelManager).calculateNewSchemaIdsAndValidate(viewId, changes, newColumnIds);
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);
	}
	
	/**
	 * Test for PLFM-4733. Schema change on a view should enforce the view max.
	 * 
	 */
	@Test 
	public void testApplySchemaChangeOverLimit(){
		String viewId = "syn123";
		ColumnChange change = new ColumnChange();
		change.setOldColumnId(null);
		change.setNewColumnId("456");
		List<ColumnChange> changes = Lists.newArrayList(change);
		ColumnModel model = objectFieldModelResolver.getColumnModel(ObjectField.benefactorId);
		model.setId(change.getNewColumnId());
		// the new schema should be over the limit
		List<String> newSchemaColumnIds = new LinkedList<>();
		int columnCount = TableViewManagerImpl.MAX_COLUMNS_PER_VIEW+1;
		for(int i=0; i<columnCount; i++) {
			newSchemaColumnIds.add(""+i);
		}
		List<String> newColumnIds = Lists.newArrayList(change.getNewColumnId());
		when(mockColumnModelManager.calculateNewSchemaIdsAndValidate(viewId, changes, newColumnIds)).thenReturn(newSchemaColumnIds);
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.applySchemaChange(userInfo, viewId, changes, newColumnIds);
		}).getMessage();
		// expected
		assertTrue(message.contains(""+TableViewManagerImpl.MAX_COLUMNS_PER_VIEW));
	}

	@Test
	public void testUpdateAnnotationsFromValues(){
		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any())).thenReturn(objectFieldModelResolver);
		when(mockMetadataIndexProvider.canUpdateAnnotation(any())).thenReturn(true);
		
		Annotations annos = AnnotationsV2TestUtils.newEmptyAnnotationsV2();
		Map<String, String> values = new HashMap<>();
		values.put(ObjectField.etag.name(), "anEtag");
		values.put(anno1.getId(), "aString");
		values.put(anno2.getId(), "123");
		// call under test
		boolean updated = manager.updateAnnotationsFromValues(annos, viewSchema, values, mockMetadataIndexProvider);
		assertTrue(updated);
		AnnotationsValue anno1Value = annos.getAnnotations().get(anno1.getName());
		assertEquals("aString",AnnotationsV2Utils.getSingleValue(anno1Value));
		assertEquals(AnnotationsValueType.STRING, anno1Value.getType());
		AnnotationsValue anno2Value = annos.getAnnotations().get(anno2.getName());
		assertEquals("123",AnnotationsV2Utils.getSingleValue(anno2Value));
		assertEquals(AnnotationsValueType.LONG, anno2Value.getType());
		// etag should not be included.
		assertNull(AnnotationsV2Utils.getSingleValue(annos, ObjectField.etag.name()));
	}

	@Test
	public void testUpdateAnnotations_ListValues(){
		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any())).thenReturn(objectFieldModelResolver);
		when(mockMetadataIndexProvider.canUpdateAnnotation(any())).thenReturn(true);
		
		//make anno1 a list
		anno1.setColumnType(ColumnType.STRING_LIST);


		Annotations annos = AnnotationsV2TestUtils.newEmptyAnnotationsV2();
		Map<String, String> values = new HashMap<>();
		values.put(ObjectField.etag.name(), "anEtag");
		values.put(anno1.getId(), "[\"asdf\", \"qwerty\"]");
		values.put(intListColumn.getId(), "[123, 456, 789]");
		// call under test
		boolean updated = manager.updateAnnotationsFromValues(annos, viewSchema, values, mockMetadataIndexProvider);
		
		assertTrue(updated);
		AnnotationsValue anno1Value = annos.getAnnotations().get(anno1.getName());
		assertEquals(Arrays.asList("asdf", "qwerty"), anno1Value.getValue());
		assertEquals(AnnotationsValueType.STRING, anno1Value.getType());
		AnnotationsValue intListValue = annos.getAnnotations().get(intListColumn.getName());
		assertEquals(Arrays.asList("123", "456", "789"),intListValue.getValue());
		assertEquals(AnnotationsValueType.LONG, intListValue.getType());
		// etag should not be included.
		assertNull(AnnotationsV2Utils.getSingleValue(annos, ObjectField.etag.name()));
	}

	@Test
	public void testToAnnotationValuesList_ValueNotJSONArrayFormat(){
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			 TableViewManagerImpl.toAnnotationValuesList(intListColumn, "not a JSON ARRAY!!");
		});

		assertEquals("Value is not correctly formatted as a JSON Array: not a JSON ARRAY!!" , e.getMessage());
	}

	@Test
	public void testToAnnotationValuesList_ListContainsNull(){
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			TableViewManagerImpl.toAnnotationValuesList(intListColumn, "[123, \456, null, 789]");
		});

		assertEquals("null value is not allowed" , e.getMessage());
	}

	@Test
	public void testToAnnotationValuesList_HappyCase(){
		// call under test
		List<String> values = TableViewManagerImpl.toAnnotationValuesList(intListColumn, "[123, 456, 789]");

		assertEquals(Arrays.asList("123", "456", "789") , values);
	}
	
	@Test
	public void testUpdateAnnotationsFromValuesSameNameDifferntType(){
		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any())).thenReturn(objectFieldModelResolver);
		when(mockMetadataIndexProvider.canUpdateAnnotation(any())).thenReturn(true);
		
		Annotations annos = AnnotationsV2TestUtils.newEmptyAnnotationsV2();
		AnnotationsV2TestUtils.putAnnotations(annos, anno1.getName(), "456", AnnotationsValueType.LONG);
		AnnotationsV2TestUtils.putAnnotations(annos, anno2.getName(), "not a long", AnnotationsValueType.STRING);
		// update the values.
		Map<String, String> values = new HashMap<>();
		values.put(ObjectField.etag.name(), "anEtag");
		values.put(anno1.getId(), "aString");
		values.put(anno2.getId(), "123");
		// call under test
		boolean updated = manager.updateAnnotationsFromValues(annos, viewSchema, values, mockMetadataIndexProvider);
		// the resulting annotations must be valid.
		AnnotationsV2Utils.validateAnnotations(annos);
		assertTrue(updated);
		AnnotationsValue anno1Value = annos.getAnnotations().get(anno1.getName());
		assertEquals("aString",AnnotationsV2Utils.getSingleValue(anno1Value));
		assertEquals(AnnotationsValueType.STRING, anno1Value.getType());
		AnnotationsValue anno2Value = annos.getAnnotations().get(anno2.getName());
		assertEquals("123",AnnotationsV2Utils.getSingleValue(anno2Value));
		assertEquals(AnnotationsValueType.LONG, anno2Value.getType());

		// etag should not be included.
		assertNull(AnnotationsV2Utils.getSingleValue(annos, ObjectField.etag.name()));
	}
	
	/**
	 * This test was added for PLFM-4706
	 * 
	 */
	@Test
	public void testUpdateAnnotationsDate() throws IOException, JSONObjectAdapterException {
		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any())).thenReturn(objectFieldModelResolver);
		when(mockMetadataIndexProvider.canUpdateAnnotation(any())).thenReturn(true);
		
		String date = "1509744902000";
		viewSchema = Lists.newArrayList(dateColumn);
		Annotations annos = AnnotationsV2TestUtils.newEmptyAnnotationsV2();
		// update the values.
		Map<String, String> values = new HashMap<>();
		values.put(dateColumn.getId(), date);
		// call under test
		boolean updated = manager.updateAnnotationsFromValues(annos, viewSchema, values, mockMetadataIndexProvider);
		// the resulting annotations must be valid.
		AnnotationsV2Utils.validateAnnotations(annos);
		assertTrue(updated);
		/*
		 * Copy the annotations
		 * Note: With PLFM-4706 this is where the date gets 
		 * converted to the same day at time 0.
		 */
		Annotations annotationCopy = EntityFactory.createEntityFromJSONObject(EntityFactory.createJSONObjectForEntity(annos), Annotations.class);

		AnnotationsValue annotationV2Value = annotationCopy.getAnnotations().get(dateColumn.getName());
		assertEquals(date, AnnotationsV2Utils.getSingleValue(annotationV2Value));
		assertEquals(AnnotationsValueType.TIMESTAMP_MS, annotationV2Value.getType());
	}
	
	@Test
	public void testUpdateAnnotationsFromValuesEmptyScheam(){
		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any())).thenReturn(objectFieldModelResolver);
		
		Annotations annos = AnnotationsV2TestUtils.newEmptyAnnotationsV2();
		Map<String, String> values = new HashMap<>();
		// call under test
		boolean updated = manager.updateAnnotationsFromValues(annos, new LinkedList<ColumnModel>(), values, mockMetadataIndexProvider);
		assertFalse(updated);
	}
	
	@Test
	public void testUpdateAnnotationsFromValuesEmptyValues(){
		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any())).thenReturn(objectFieldModelResolver);
		when(mockMetadataIndexProvider.canUpdateAnnotation(any())).thenReturn(true);
		
		Annotations annos = AnnotationsV2TestUtils.newEmptyAnnotationsV2();
		Map<String, String> values = new HashMap<>();
		// call under test
		boolean updated = manager.updateAnnotationsFromValues(annos, viewSchema, values, mockMetadataIndexProvider);
		assertFalse(updated);
	}
	
	@Test
	public void testUpdateAnnotationsFromValuesDelete(){
		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any())).thenReturn(objectFieldModelResolver);
		when(mockMetadataIndexProvider.canUpdateAnnotation(any())).thenReturn(true);
		
		Annotations annos = AnnotationsV2TestUtils.newEmptyAnnotationsV2();
		// start with an annotation.
		AnnotationsV2TestUtils.putAnnotations(annos, anno1.getName(), "startValue", AnnotationsValueType.STRING);
		Map<String, String> values = new HashMap<>();
		values.put(anno1.getId(), null);
		// call under test
		boolean updated = manager.updateAnnotationsFromValues(annos, viewSchema, values, mockMetadataIndexProvider);
		assertTrue(updated);
		assertFalse(annos.getAnnotations().containsKey(anno1.getName()));
		assertEquals(null, AnnotationsV2Utils.getSingleValue(annos, anno1.getName()));
	}
	
	@Test
	public void testGetEtagColumn(){
		ColumnModel etag = TableViewManagerImpl.getEtagColumn(viewSchema);
		assertEquals(etagColumn, etag);
	}
	
	@Test 
	public void testGetEtagColumnMissing(){
		try {
			TableViewManagerImpl.getEtagColumn(new LinkedList<ColumnModel>());
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals(TableViewManagerImpl.ETG_COLUMN_MISSING, e.getMessage());
		}
	}
	
	@Test
	public void testUpdateEntityInView(){
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any())).thenReturn(objectFieldModelResolver);
		when(mockMetadataIndexProvider.getAnnotations(any(), any())).thenReturn(Optional.of(annotationsV2));
		when(mockMetadataIndexProvider.canUpdateAnnotation(any())).thenReturn(true);
		
		ViewObjectType objectType = scopeType.getObjectType();
		
		// call under test
		manager.updateRowInView(userInfo, viewSchema, objectType, row);
		
		// this should trigger an update
		verify(mockMetadataIndexProvider).updateAnnotations(eq(userInfo), eq("syn111"), any(Annotations.class));
		verify(mockReplicationManager).replicate(objectType, "syn111");
	}
	
	@Test
	public void testUpdateEntityInViewNullRow(){
		ViewObjectType objectType = scopeType.getObjectType();
		row = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.updateRowInView(userInfo, viewSchema, objectType, row);
		});
	}
	
	@Test
	public void testUpdateEntityInViewNullRowId(){
		ViewObjectType objectType = scopeType.getObjectType();
		row.setRowId(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.updateRowInView(userInfo, viewSchema, objectType, row);
		});
	}
	
	@Test
	public void testUpdateEntityInViewNullValues(){
		ViewObjectType objectType = scopeType.getObjectType();
		row.setValues(null);
		// call under test
		manager.updateRowInView(userInfo, viewSchema, objectType, row);
		// this should not trigger an update
		verify(mockNodeManager, never()).updateUserAnnotations(any(UserInfo.class), anyString(), any(Annotations.class));
		verify(mockReplicationManager, never()).replicate(any(), anyString());
	}
	
	@Test
	public void testUpdateEntityInViewEmptyValues(){
		ViewObjectType objectType = scopeType.getObjectType();
		row.setValues(new HashMap<String, String>());
		// call under test
		manager.updateRowInView(userInfo, viewSchema, objectType, row);
		// this should not trigger an update
		verify(mockNodeManager, never()).updateUserAnnotations(any(UserInfo.class), anyString(), any(Annotations.class));
		verify(mockReplicationManager, never()).replicate(any(), anyString());
	}
	
	@Test
	public void testUpdateEntityInViewNoChanges(){
		ViewObjectType objectType = scopeType.getObjectType();
		row.getValues().remove(anno1.getId());
		row.getValues().remove(anno2.getId());

		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any())).thenReturn(objectFieldModelResolver);
		when(mockMetadataIndexProvider.getAnnotations(any(), any())).thenReturn(Optional.of(annotationsV2));
		
		// call under test
		manager.updateRowInView(userInfo, viewSchema, objectType, row);
		
		// this should not trigger an update
		verify(mockNodeManager, never()).updateUserAnnotations(any(UserInfo.class), anyString(), any(Annotations.class));
		verify(mockReplicationManager, never()).replicate(any(), anyString());
	}
	
	@Test
	public void testUpdateEntityInViewMissingEtag(){
		ViewObjectType objectType = scopeType.getObjectType();
		// Each row must include the etag.
		row.getValues().remove(etagColumn.getId());
		// call under test
		try {
			manager.updateRowInView(userInfo, viewSchema, objectType, row);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals(TableViewManagerImpl.ETAG_MISSING_MESSAGE, e.getMessage());
		}
	}
	
	@Test
	public void testUpdateEntityInViewSkipAnnotation(){
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockObjectFieldModelResolverFactory.getObjectFieldModelResolver(any())).thenReturn(objectFieldModelResolver);
		when(mockMetadataIndexProvider.getAnnotations(any(), any())).thenReturn(Optional.of(annotationsV2));
		
		ColumnModel skipModel = new ColumnModel();
		skipModel.setName("customField");
		skipModel.setColumnType(ColumnType.STRING);
		
		when(mockMetadataIndexProvider.canUpdateAnnotation(any())).thenReturn(true);
		when(mockMetadataIndexProvider.canUpdateAnnotation(skipModel)).thenReturn(false);
		
		ViewObjectType objectType = scopeType.getObjectType();
		
		viewSchema.add(skipModel);
		
		// call under test
		manager.updateRowInView(userInfo, viewSchema, objectType, row);
		
		// this should trigger an update
		verify(mockMetadataIndexProvider).canUpdateAnnotation(skipModel);
		verify(mockMetadataIndexProvider).updateAnnotations(eq(userInfo), eq("syn111"), annotationsCaptor.capture());
		verify(mockReplicationManager).replicate(objectType, "syn111");
		
		Map<String, AnnotationsValue> annotations = annotationsCaptor.getValue().getAnnotations();
		
		assertEquals(2, annotations.size());
		assertTrue(annotations.containsKey("foo"));
		assertTrue(annotations.containsKey("bar"));
		assertFalse(annotations.containsKey("customField"));
	}

	@Test
	public void testDeleteViewIndex() {
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		// call under test
		manager.deleteViewIndex(idAndVersion);
		verify(mockIndexManager).deleteTableIndex(idAndVersion);
	}
	
	@Test
	public void testCreateOrUpdateViewIndex_AvailableNoVersion() throws Exception {
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.AVAILABLE));
		// call under test
		managerSpy.createOrUpdateViewIndex(idAndVersion, mockProgressCallback);
		verify(managerSpy).applyChangesToAvailableView(idAndVersion, mockProgressCallback);
		verify(managerSpy, never()).createOrRebuildView(any(IdAndVersion.class), any(ProgressCallback.class));
	}
	
	@Test
	public void testCreateOrUpdateViewIndex_AvailableWithVersion() throws Exception {
		idAndVersion = IdAndVersion.parse("syn123.456");
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.AVAILABLE));
		// call under test
		managerSpy.createOrUpdateViewIndex(idAndVersion, mockProgressCallback);
		verify(managerSpy).applyChangesToAvailableView(any(IdAndVersion.class), any(ProgressCallback.class));
		verify(managerSpy, never()).createOrRebuildView(idAndVersion, mockProgressCallback);
	}
	
	@Test
	public void testCreateOrUpdateViewIndex_ProcessingFailed() throws Exception {
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.PROCESSING_FAILED));
		// call under test
		managerSpy.createOrUpdateViewIndex(idAndVersion, mockProgressCallback);
		verify(managerSpy, never()).applyChangesToAvailableView(any(IdAndVersion.class), any(ProgressCallback.class));
		verify(managerSpy).createOrRebuildView(idAndVersion, mockProgressCallback);
	}
	
	@Test
	public void testCreateOrUpdateViewProcessing() throws Exception {
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.PROCESSING));
		// call under test
		managerSpy.createOrUpdateViewIndex(idAndVersion, mockProgressCallback);
		verify(managerSpy, never()).applyChangesToAvailableView(any(IdAndVersion.class), any(ProgressCallback.class));
		verify(managerSpy).createOrRebuildView(idAndVersion, mockProgressCallback);
	}
	
	@Test
	public void testCreateOrUpdateViewStateDoesNotExist() throws Exception {
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.empty());
		// call under test
		managerSpy.createOrUpdateViewIndex(idAndVersion, mockProgressCallback);
		verify(managerSpy, never()).applyChangesToAvailableView(any(IdAndVersion.class), any(ProgressCallback.class));
		verify(managerSpy).createOrRebuildView(idAndVersion, mockProgressCallback);
	}
	
	@Test
	public void testPopulateViewFromSnapshot() throws IOException {
		long snapshotId = 998L;
		ViewSnapshot snapshot = new ViewSnapshot().withBucket("bucket").withKey("key").withSnapshotId(snapshotId);
		when(mockViewSnapshotDao.getSnapshot(idAndVersion)).thenReturn(snapshot);
		when(mockFileProvider.createTempFile(anyString(), anyString())).thenReturn(mockFile);
		setupReader("foo,bar");
		
		// call under test
		long id = manager.populateViewFromSnapshot(idAndVersion, mockIndexManager);
		assertEquals(snapshotId, id);
		verify(mockViewSnapshotDao).getSnapshot(idAndVersion);
		verify(mockS3Client).getObject(new GetObjectRequest("bucket", "key"), mockFile);
		verify(mockIndexManager).populateViewFromSnapshot(eq(idAndVersion), any());
		verify(mockIndexManager).refreshViewBenefactors(idAndVersion);
		verify(mockFile).delete();
	}
	
	@Test
	public void testPopulateViewFromSnapshotError() throws IOException {
		long snapshotId = 998L;
		ViewSnapshot snapshot = new ViewSnapshot().withBucket("bucket").withKey("key").withSnapshotId(snapshotId);
		when(mockViewSnapshotDao.getSnapshot(idAndVersion)).thenReturn(snapshot);
		when(mockFileProvider.createTempFile(anyString(), anyString())).thenReturn(mockFile);
		AmazonServiceException error = new AmazonServiceException("not correct");
		when(mockS3Client.getObject(any(GetObjectRequest.class), any(File.class))).thenThrow(error);
		
		assertThrows(AmazonServiceException.class, ()->{
			// call under test
			manager.populateViewFromSnapshot(idAndVersion, mockIndexManager);
		});
		verify(mockViewSnapshotDao).getSnapshot(idAndVersion);
		verify(mockS3Client).getObject(new GetObjectRequest("bucket", "key"), mockFile);
		verify(mockIndexManager, never()).populateViewFromSnapshot(any(IdAndVersion.class), any());
		// file should still be deleted
		verify(mockFile).delete();
	}
	
	@Test
	public void testPopulateViewFromSnapshotFileCreateError() throws IOException {
		long snapshotId = 998L;
		ViewSnapshot snapshot = new ViewSnapshot().withBucket("bucket").withKey("key").withSnapshotId(snapshotId);
		when(mockViewSnapshotDao.getSnapshot(idAndVersion)).thenReturn(snapshot);
		IOException error = new IOException("some IO error");
		when(mockFileProvider.createTempFile(anyString(), anyString())).thenThrow(error);
		
		Throwable cause = assertThrows(RuntimeException.class, ()->{
			// call under test
			manager.populateViewFromSnapshot(idAndVersion, mockIndexManager);
		}).getCause();
		assertEquals(error, cause);
		
		verify(mockViewSnapshotDao).getSnapshot(idAndVersion);
		verify(mockS3Client, never()).getObject(any(GetObjectRequest.class), any(File.class));
		verify(mockIndexManager, never()).populateViewFromSnapshot(any(IdAndVersion.class), any());
		verify(mockFile, never()).delete();
	}
	
	@Test
	public void testPopulateViewIndexFromReplication() {
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);
		Set<Long> scope = Sets.newHashSet(124L, 455L);
		when(mockTableManagerSupport.getAllContainerIdsForViewScope(idAndVersion, scopeType)).thenReturn(scope);
		viewCRC = 987L;
		when(mockIndexManager.populateViewFromEntityReplication(idAndVersion.getId(), scopeType, scope, viewSchema)).thenReturn(viewCRC);
		// call under test
		long resultCRC32 = manager.populateViewIndexFromReplication(idAndVersion, mockIndexManager, viewSchema);
		assertEquals(viewCRC, resultCRC32);
		verify(mockIndexManager).populateViewFromEntityReplication(idAndVersion.getId(), scopeType, scope,
				viewSchema);
	}
	
	@Test
	public void testCreateOrRebuildViewHoldingLockNoWorkRequired() throws RecoverableMessageException {
		when(mockTableManagerSupport.isIndexWorkRequired(idAndVersion)).thenReturn(false);
		// call under test
		manager.createOrRebuildViewHoldingLock(idAndVersion);
		verify(mockTableManagerSupport).isIndexWorkRequired(idAndVersion);
		verifyNoMoreInteractions(mockTableManagerSupport);
		verifyNoMoreInteractions(mockConnectionFactory);
	}
	
	/**
	 * Populate a view from entity replication.
	 * @throws RecoverableMessageException 
	 */
	@Test
	public void testCreateOrRebuildViewHoldingLockWorkeRequired() throws RecoverableMessageException {
		when(mockTableManagerSupport.isIndexWorkRequired(idAndVersion)).thenReturn(true);
		String token = "the token";
		when(mockTableManagerSupport.startTableProcessing(idAndVersion)).thenReturn(token);
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		String originalSchemaMD5Hex = "startMD5";
		when(mockTableManagerSupport.getSchemaMD5Hex(idAndVersion)).thenReturn(originalSchemaMD5Hex);
		when(mockColumnModelManager.getColumnModelsForObject(idAndVersion)).thenReturn(viewSchema);
		Set<Long> scope = Sets.newHashSet(124L, 455L);
		when(mockTableManagerSupport.getAllContainerIdsForViewScope(idAndVersion, scopeType)).thenReturn(scope);
		viewCRC = 987L;
		when(mockIndexManager.populateViewFromEntityReplication(idAndVersion.getId(), scopeType, scope, viewSchema)).thenReturn(viewCRC);
		
		// call under test
		manager.createOrRebuildViewHoldingLock(idAndVersion);

		verify(mockTableManagerSupport).isIndexWorkRequired(idAndVersion);
		verify(mockTableManagerSupport).startTableProcessing(idAndVersion);
		verify(mockTableManagerSupport).getViewScopeType(idAndVersion);
		verify(mockConnectionFactory).connectToTableIndex(idAndVersion);
		verify(mockIndexManager).deleteTableIndex(idAndVersion);
		verify(mockTableManagerSupport).getSchemaMD5Hex(idAndVersion);
		verify(mockColumnModelManager).getColumnModelsForObject(idAndVersion);
		verify(mockTableManagerSupport).getAllContainerIdsForViewScope(idAndVersion, scopeType);
		boolean isTableView = true;
		verify(mockIndexManager).setIndexSchema(idAndVersion, isTableView, viewSchema);
		verify(mockTableManagerSupport).attemptToUpdateTableProgress(idAndVersion, token, "Copying data to view...", 0L,
				1L);
		verify(mockIndexManager).populateViewFromEntityReplication(idAndVersion.getId(), scopeType, scope,
				viewSchema);
		verify(mockIndexManager, never()).populateViewFromSnapshot(any(IdAndVersion.class), any());
		verify(mockIndexManager).optimizeTableIndices(idAndVersion);
		verify(mockIndexManager).populateListColumnIndexTables(idAndVersion, viewSchema);
		verify(mockIndexManager).setIndexVersionAndSchemaMD5Hex(idAndVersion, viewCRC, originalSchemaMD5Hex);
		verify(mockTableManagerSupport).attemptToSetTableStatusToAvailable(idAndVersion, token,
				TableViewManagerImpl.DEFAULT_ETAG);
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class),
				any(Exception.class));
		verify(mockViewSnapshotDao, never()).getSnapshot(any(IdAndVersion.class));
		verifyNoMoreInteractions(mockS3Client);
	}
	
	/**
	 * Populate a view from a snapshot.
	 * @throws IOException
	 * @throws RecoverableMessageException 
	 */
	@Test
	public void testCreateOrRebuildViewHoldingLockWorkeRequiredWithVersion() throws IOException, RecoverableMessageException {
		idAndVersion = IdAndVersion.parse("syn123.45");
		when(mockTableManagerSupport.isIndexWorkRequired(idAndVersion)).thenReturn(true);
		String token = "the token";
		when(mockTableManagerSupport.startTableProcessing(idAndVersion)).thenReturn(token);
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		String originalSchemaMD5Hex = "startMD5";
		when(mockTableManagerSupport.getSchemaMD5Hex(idAndVersion)).thenReturn(originalSchemaMD5Hex);
		when(mockColumnModelManager.getColumnModelsForObject(idAndVersion)).thenReturn(viewSchema);
		long snapshotId = 998L;
		ViewSnapshot snapshot = new ViewSnapshot().withBucket("bucket").withKey("key").withSnapshotId(snapshotId);
		when(mockViewSnapshotDao.getSnapshot(idAndVersion)).thenReturn(snapshot);
		when(mockFileProvider.createTempFile(anyString(), anyString())).thenReturn(mockFile);
		setupReader("foo,bar");
		
		// call under test
		manager.createOrRebuildViewHoldingLock(idAndVersion);

		verify(mockTableManagerSupport).isIndexWorkRequired(idAndVersion);
		verify(mockTableManagerSupport).startTableProcessing(idAndVersion);
		verify(mockConnectionFactory).connectToTableIndex(idAndVersion);
		verify(mockIndexManager).deleteTableIndex(idAndVersion);
		verify(mockTableManagerSupport).getSchemaMD5Hex(idAndVersion);
		verify(mockColumnModelManager).getColumnModelsForObject(idAndVersion);
		boolean isTableView = true;
		verify(mockIndexManager).setIndexSchema(idAndVersion, isTableView, viewSchema);
		verify(mockTableManagerSupport).attemptToUpdateTableProgress(idAndVersion, token, "Copying data to view...", 0L,
				1L);
		verify(mockIndexManager, never()).populateViewFromEntityReplication(any(Long.class), any(), any(), any());
		verify(mockViewSnapshotDao).getSnapshot(idAndVersion);
		verify(mockS3Client).getObject(new GetObjectRequest("bucket", "key"), mockFile);
		verify(mockIndexManager).populateViewFromSnapshot(eq(idAndVersion), any());
		verify(mockFile).delete();
		verify(mockIndexManager).optimizeTableIndices(idAndVersion);
		verify(mockIndexManager).populateListColumnIndexTables(idAndVersion, viewSchema);
		verify(mockIndexManager).setIndexVersionAndSchemaMD5Hex(idAndVersion, snapshotId, originalSchemaMD5Hex);
		verify(mockTableManagerSupport).attemptToSetTableStatusToAvailable(idAndVersion, token,
				TableViewManagerImpl.DEFAULT_ETAG);
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class),
				any(Exception.class));
	}
	
	/**
	 * For PLFM-5939 an exception thrown at
	 * tableMangerSupport.isIndexWorkerRequired() did not the table status to
	 * failed.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCreateOrRebuildViewHoldingLockPLFM_5939() throws IOException {
		idAndVersion = IdAndVersion.parse("syn123.45");
		IllegalArgumentException exception = new IllegalArgumentException("nope");
		when(mockTableManagerSupport.isIndexWorkRequired(idAndVersion)).thenThrow(exception);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.createOrRebuildViewHoldingLock(idAndVersion);
		});
		verify(mockTableManagerSupport).attemptToSetTableStatusToFailed(idAndVersion, exception);
	}
	
	@Test
	public void testCreateOrRebuildViewHoldingLockError() {
		when(mockTableManagerSupport.isIndexWorkRequired(idAndVersion)).thenReturn(true);
		String token = "the token";
		when(mockTableManagerSupport.startTableProcessing(idAndVersion)).thenReturn(token);
		IllegalStateException exception = new IllegalStateException("something is wrong");
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenThrow(exception);
		
		assertThrows(IllegalStateException.class, () -> {
			// call under test
			manager.createOrRebuildViewHoldingLock(idAndVersion);
		});
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToAvailable(any(IdAndVersion.class),
				anyString(), anyString());
		verify(mockTableManagerSupport).attemptToSetTableStatusToFailed(idAndVersion, exception);
	}
	
	/**
	 * A InvalidStatusTokenException thrown while attempting to set the view to available should not
	 * result in setting the view's state to failed.  Instead, the message should returned to the queue
	 * for a retry at a later time.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCreateOrRebuildViewHoldingLockInvalidStatusTokenException() throws IOException {
		when(mockTableManagerSupport.isIndexWorkRequired(idAndVersion)).thenReturn(true);
		String token = "the token";
		when(mockTableManagerSupport.startTableProcessing(idAndVersion)).thenReturn(token);
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		String originalSchemaMD5Hex = "startMD5";
		when(mockTableManagerSupport.getSchemaMD5Hex(idAndVersion)).thenReturn(originalSchemaMD5Hex);
		when(mockColumnModelManager.getColumnModelsForObject(idAndVersion)).thenReturn(viewSchema);
		Set<Long> scope = Sets.newHashSet(124L, 455L);
		when(mockTableManagerSupport.getAllContainerIdsForViewScope(idAndVersion, scopeType)).thenReturn(scope);
		viewCRC = 987L;
		when(mockIndexManager.populateViewFromEntityReplication(idAndVersion.getId(), scopeType, scope, viewSchema))
				.thenReturn(viewCRC);
		
		// conflict is thrown if the state has changed since the process was started.
		InvalidStatusTokenException conflictException = new InvalidStatusTokenException();
		doThrow(conflictException).when(mockTableManagerSupport).attemptToSetTableStatusToAvailable(idAndVersion, token,
				TableViewManagerImpl.DEFAULT_ETAG);
		
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			manager.createOrRebuildViewHoldingLock(idAndVersion);
		});
		assertEquals(conflictException, result.getCause());
		verify(mockTableManagerSupport).attemptToSetTableStatusToAvailable(idAndVersion, token,
				TableViewManagerImpl.DEFAULT_ETAG);
		// conflict should not set the view to failed.
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(any(), any());
	}
	
	/**
	 * Setup the chain of file->gzip->writer.
	 * 
	 * @return
	 * @throws IOException
	 */
	public StringWriter setupWriter() throws IOException {
		StringWriter writer = new StringWriter();
		when(mockFileProvider.createFileOutputStream(mockFile)).thenReturn(mockOutStream);
		when(mockFileProvider.createGZIPOutputStream(mockOutStream)).thenReturn(mockGzipOutStream);
		when(mockFileProvider.createWriter(mockGzipOutStream, StandardCharsets.UTF_8)).thenReturn(writer);
		return writer;
	}
	
	/**
	 * Setup chain of file->gzip->reader
	 * @param toRead
	 * @return
	 * @throws IOException
	 */
	public StringReader setupReader(String toRead) throws IOException {
		StringReader reader = new StringReader(toRead);
		when(mockFileProvider.createFileInputStream(mockFile)).thenReturn(mockInputStream);
		when(mockFileProvider.createGZIPInputStream(mockInputStream)).thenReturn(mockGzipInputStream);
		when(mockFileProvider.createReader(mockGzipInputStream, StandardCharsets.UTF_8)).thenReturn(reader);
		return reader;
	}
	
	
	@Test
	public void testCreateViewSnapshotAndUploadToS3() throws IOException {
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		when(mockFileProvider.createTempFile(anyString(), anyString())).thenReturn(mockFile);
		setupWriter();
		String bucket = "snapshot.bucket";
		when(mockConfig.getViewSnapshotBucketName()).thenReturn(bucket);
		
		// call under test
		BucketAndKey bucketAndKey = manager.createViewSnapshotAndUploadToS3(idAndVersion, scopeType, viewSchema, scopeIds);
		
		verify(mockFileProvider).createTempFile("ViewSnapshot",	".csv");
		verify(mockFileProvider).createFileOutputStream(mockFile);
		verify(mockFileProvider).createGZIPOutputStream(mockOutStream);
		verify(mockFileProvider).createWriter(mockGzipOutStream, StandardCharsets.UTF_8);
		verify(mockIndexManager).createViewSnapshot(eq(idAndVersion.getId()), eq(scopeType), eq(scopeIds), eq(viewSchema), any(CSVWriterStream.class));
		assertNotNull(bucketAndKey);
		verify(mockS3Client).putObject(putRequestCaptor.capture());
		PutObjectRequest putRequest = putRequestCaptor.getValue();
		assertNotNull(putRequest);
		assertEquals(bucket, putRequest.getBucketName());
		assertNotNull(putRequest.getKey());
		assertTrue(putRequest.getKey().startsWith(""+idAndVersion.getId()));
		assertEquals(mockFile, putRequest.getFile());
		verify(mockFile).delete();
		assertEquals(bucket, bucketAndKey.getBucket());
		assertEquals(putRequest.getKey(), bucketAndKey.getKey());
	}
	
	@Test
	public void testCreateViewSnapshotAndUploadToS3Error() throws IOException {
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		when(mockFileProvider.createTempFile(anyString(), anyString())).thenReturn(mockFile);
		FileNotFoundException exception = new FileNotFoundException("no");
		doThrow(exception).when(mockFileProvider).createFileOutputStream(mockFile);
	
		Throwable cause = assertThrows(RuntimeException.class, ()->{
			// call under test
			manager.createViewSnapshotAndUploadToS3(idAndVersion, scopeType, viewSchema, scopeIds);
		}).getCause();
		assertEquals(exception, cause);
		
		verify(mockIndexManager, never()).createViewSnapshot(anyLong(), any(), any(), any(), any(CSVWriterStream.class));
		// the temp file must be deleted even if there is an error
		verify(mockFile).delete();	
	}
	
	@Test
	public void testCreateViewSnapshotAndUploadToS3NullId() throws IOException {
		idAndVersion = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.createViewSnapshotAndUploadToS3(idAndVersion, scopeType, viewSchema, scopeIds);
		});
	}
	
	@Test
	public void testCreateViewSnapshotAndUploadToS3NullViewType() throws IOException {
		scopeType = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.createViewSnapshotAndUploadToS3(idAndVersion, scopeType, viewSchema, scopeIds);
		});
	}
	
	@Test
	public void testCreateViewSnapshotAndUploadToS3NullSchema() throws IOException {
		viewSchema = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.createViewSnapshotAndUploadToS3(idAndVersion, scopeType, viewSchema, scopeIds);
		});
	}
	
	@Test
	public void testCreateViewSnapshotAndUploadToS3NullScope() throws IOException {
		scopeIds = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.createViewSnapshotAndUploadToS3(idAndVersion, scopeType, viewSchema, scopeIds);
		});
	}
	
	@Test
	public void testCreateSnapshot() throws IOException {
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);
		when(mockColumnModelManager.getColumnModelsForObject(idAndVersion)).thenReturn(viewSchema);
		when(mockTableManagerSupport.getAllContainerIdsForViewScope(idAndVersion, scopeType)).thenReturn(scopeIds);
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		when(mockFileProvider.createTempFile(anyString(), anyString())).thenReturn(mockFile);
		setupWriter();
		String bucket = "snapshot.bucket";
		when(mockConfig.getViewSnapshotBucketName()).thenReturn(bucket);
		long snapshotVersion = 12L;
		when(mockNodeManager.createSnapshotAndVersion(userInfo, viewId, snapshotOptions)).thenReturn(snapshotVersion);
		
		// call under test
		long result = manager.createSnapshot(userInfo, viewId, snapshotOptions);
		assertEquals(snapshotVersion, result);
		
		IdAndVersion expectedIdAndVersion = IdAndVersion.newBuilder().setId(idAndVersion.getId())
				.setVersion(snapshotVersion).build();
		verify(mockColumnModelManager).bindCurrentColumnsToVersion(expectedIdAndVersion);
		verify(mockViewSnapshotDao).createSnapshot(snapshotCaptor.capture());
		// fix for PLFM-5957
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(expectedIdAndVersion);
		ViewSnapshot snapshot = snapshotCaptor.getValue();
		assertNotNull(snapshot);
		assertEquals(bucket, snapshot.getBucket());
		assertNotNull(snapshot.getKey());
		assertTrue(snapshot.getKey().startsWith(""+idAndVersion.getId()));
		assertEquals(userInfo.getId(), snapshot.getCreatedBy());
		assertNotNull(snapshot.getCreatedOn());
		assertEquals(snapshotVersion, snapshot.getVersion());
		assertEquals(idAndVersion.getId(), snapshot.getViewId());
		
	}
	
	@Test
	public void testApplyChangesToAvailableView() throws Exception {
		setupNonexclusiveLockToForwardToCallack();
		setupExclusiveLockToForwardToCallack();
		// call under test
		managerSpy.applyChangesToAvailableView(idAndVersion, mockProgressCallback);
		verify(mockTableManagerSupport).tryRunWithTableNonexclusiveLock(eq(mockProgressCallback), eq(idAndVersion),
				any());
		String expectedKey = TableViewManagerImpl.VIEW_DELTA_KEY_PREFIX+idAndVersion.toString();
		verify(mockTableManagerSupport).tryRunWithTableExclusiveLock(eq(mockProgressCallback), eq(expectedKey),
				any());
		verify(managerSpy).applyChangesToAvailableViewOrSnapshot(idAndVersion);
	}
	
	/**
	 * Setup the tryRunWithTableNonexclusiveLock() to forward the call to the passed callback.
	 * @throws Exception
	 */
	void setupNonexclusiveLockToForwardToCallack() throws Exception {
		doAnswer((InvocationOnMock invocation) -> {
			// Last argument is the callback
			Object[] args = invocation.getArguments();
			ProgressingCallable<?> callable = (ProgressingCallable<?>) args[args.length - 1];
			callable.call(mockProgressCallback);
			return null;
		}).when(mockTableManagerSupport).tryRunWithTableNonexclusiveLock(any(ProgressCallback.class),
				any(IdAndVersion.class), any());
	}
	
	/**
	 * Setup the tryRunWithTableExclusiveLock() to forward the call to the passed callback.
	 * @throws Exception
	 */
	void setupExclusiveLockToForwardToCallack() throws Exception {
		doAnswer((InvocationOnMock invocation) -> {
			// Last argument is the callback
			Object[] args = invocation.getArguments();
			ProgressingCallable<?> callable = (ProgressingCallable<?>) args[args.length - 1];
			callable.call(mockProgressCallback);
			return null;
		}).when(mockTableManagerSupport).tryRunWithTableExclusiveLock(any(ProgressCallback.class),
				anyString(), any());
	}
	
	@Test
	public void testApplyChangesToAvailableView_ExcluisveLockUnavailable() throws Exception {
		setupNonexclusiveLockToForwardToCallack();
		LockUnavilableException exception = new LockUnavilableException("not now");
		doThrow(exception).when(mockTableManagerSupport).tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(String.class), any());
		String expectedKey = TableViewManagerImpl.VIEW_DELTA_KEY_PREFIX+idAndVersion.toString();
		// call under test
		managerSpy.applyChangesToAvailableView(idAndVersion, mockProgressCallback);
		verify(mockTableManagerSupport).tryRunWithTableExclusiveLock(eq(mockProgressCallback), eq(expectedKey),
				any());
		verify(mockTableManagerSupport).tryRunWithTableNonexclusiveLock(eq(mockProgressCallback), eq(idAndVersion),
				any());
	}
	
	@Test
	public void testApplyChangesToAvailableView_NonExcluisveLockUnavailable() throws Exception {
		LockUnavilableException exception = new LockUnavilableException("not now");
		doThrow(exception).when(mockTableManagerSupport).tryRunWithTableNonexclusiveLock(any(ProgressCallback.class),
				any(IdAndVersion.class), any());
		// call under test
		manager.applyChangesToAvailableView(idAndVersion, mockProgressCallback);
		verify(mockTableManagerSupport).tryRunWithTableNonexclusiveLock(eq(mockProgressCallback), eq(idAndVersion),
				any());
		verifyNoMoreInteractions(mockTableManagerSupport);
	}
	
	@Test
	public void testApplyChangesToAvailableView_OtherException() throws Exception {
		setupNonexclusiveLockToForwardToCallack();
		IllegalArgumentException exception = new IllegalArgumentException("not now");
		doThrow(exception).when(mockTableManagerSupport).tryRunWithTableExclusiveLock(any(ProgressCallback.class),
				anyString(), any());
		IllegalArgumentException result =assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.applyChangesToAvailableView(idAndVersion, mockProgressCallback);
		});
		assertEquals(exception, result);
	}
	
	@Test
	public void testApplyChangesToAvailableViewHoldingLock_NothingToDo() {
		setupApplyChanges();
		Set<Long> rowsToUpdate = Collections.emptySet();
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.AVAILABLE));
		when(mockIndexManager.getOutOfDateRowsForView(idAndVersion, scopeType, allContainersInScope,
				TableViewManagerImpl.MAX_ROWS_PER_TRANSACTION)).thenReturn(rowsToUpdate);
		// call under test
		manager.applyChangesToAvailableView(idAndVersion);
		verify(mockTableManagerSupport).getTableStatusState(idAndVersion);
		verify(mockIndexManager, never()).updateViewRowsInTransaction(any(IdAndVersion.class), anySet(), any(),
				anySet(), anyList());
		verifyNoMoreInteractions(mockTableManagerSupport);
	}
	
	@Test
	public void testApplyChangesToAvailableViewHoldingLock_OnePage() {
		setupApplyChanges();
		Set<Long> rowsToUpdate = Sets.newHashSet(101L,102L);
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.AVAILABLE));
		when(mockIndexManager.getOutOfDateRowsForView(idAndVersion, scopeType, allContainersInScope,
				TableViewManagerImpl.MAX_ROWS_PER_TRANSACTION)).thenReturn(rowsToUpdate);
		// call under test
		manager.applyChangesToAvailableView(idAndVersion);
		verify(mockIndexManager).updateViewRowsInTransaction(idAndVersion, rowsToUpdate, scopeType,
				allContainersInScope, viewSchema);
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class),
				any(Exception.class));
		verify(mockTableManagerSupport).updateChangedOnIfAvailable(idAndVersion);
	}
	
	@Test
	public void testApplyChangesToAvailableViewHoldingLock_OnePageNotAvailable() {
		setupApplyChanges();
		// do no work when not available.
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.PROCESSING));
		// call under test
		manager.applyChangesToAvailableView(idAndVersion);
		verify(mockTableManagerSupport).getTableStatusState(idAndVersion);
		verifyNoMoreInteractions(mockTableManagerSupport);
		verifyNoMoreInteractions(mockIndexManager);
	}
	
	@Test
	public void testApplyChangesToAvailableViewHoldingLock_OnePageNoState() {
		setupApplyChanges();
		// do no work when not available.
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.empty());
		// call under test
		manager.applyChangesToAvailableView(idAndVersion);
		verify(mockTableManagerSupport).getTableStatusState(idAndVersion);
		verifyNoMoreInteractions(mockTableManagerSupport);
		verifyNoMoreInteractions(mockIndexManager);
	}
	
	
	@Test
	public void testapplyChangesToAvailableViewWithNull() {
		idAndVersion = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.applyChangesToAvailableView(idAndVersion);
		});
	}
	
	@Test
	public void testapplyChangesToAvailableViewWithVersionNumber() {
		idAndVersion = IdAndVersion.parse("syn123.1");
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.applyChangesToAvailableView(idAndVersion);
		}).getMessage();
		assertEquals("This method cannot be called on a view snapshot", message);
	}
	
	/**
	 * If progress is made on a single page of changes, the IDs should not reappear in the next
	 * page, and all pages should be processed.
	 */
	@Test
	public void testApplyChangesToAvailableViewHoldingLock_MultiplePagesNoOverlap() {
		setupApplyChanges();
		Set<Long> pageOne = Sets.newHashSet(101L,102L);
		Set<Long> pageTwo = Sets.newHashSet(103L,104L);
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.AVAILABLE));
		when(mockIndexManager.getOutOfDateRowsForView(idAndVersion, scopeType, allContainersInScope,
				TableViewManagerImpl.MAX_ROWS_PER_TRANSACTION)).thenReturn(pageOne, pageTwo);
		// call under test
		manager.applyChangesToAvailableView(idAndVersion);
		verify(mockIndexManager).updateViewRowsInTransaction(idAndVersion, pageOne, scopeType,
				allContainersInScope, viewSchema);
		verify(mockIndexManager).updateViewRowsInTransaction(idAndVersion, pageTwo, scopeType,
				allContainersInScope, viewSchema);
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class),
				any(Exception.class));
		verify(mockTableManagerSupport, times(2)).updateChangedOnIfAvailable(idAndVersion);
	}
	
	@Test
	public void testApplyChangesToAvailableViewHoldingLock_MultiplePagesNoOverlapStatusChanged() {
		setupApplyChanges();
		Set<Long> pageOne = Sets.newHashSet(101L,102L);
		Set<Long> pageTwo = Sets.newHashSet(103L,104L);
		// Status starts as available but changes to processing which stop the updates.
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.AVAILABLE), Optional.of(TableState.PROCESSING));
		when(mockIndexManager.getOutOfDateRowsForView(idAndVersion, scopeType, allContainersInScope,
				TableViewManagerImpl.MAX_ROWS_PER_TRANSACTION)).thenReturn(pageOne, pageTwo);
		// call under test
		manager.applyChangesToAvailableView(idAndVersion);
		verify(mockIndexManager).updateViewRowsInTransaction(idAndVersion, pageOne, scopeType,
				allContainersInScope, viewSchema);
		// page two should not be updated becuase of the swith to processing.
		verify(mockIndexManager, never()).updateViewRowsInTransaction(idAndVersion, pageTwo, scopeType,
				allContainersInScope, viewSchema);
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class),
				any(Exception.class));
		verify(mockTableManagerSupport, times(1)).updateChangedOnIfAvailable(idAndVersion);
	}
	
	/**
	 * There are multiple page with overlap.  The overlap should terminate the process.
	 */
	@Test
	public void testApplyChangesToAvailableViewHoldingLock_MultiplePagesWithOverlap() {
		setupApplyChanges();
		Set<Long> pageOne = Sets.newHashSet(101L,102L);
		Set<Long> pageTwo = Sets.newHashSet(102L,103L);
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.AVAILABLE));
		when(mockIndexManager.getOutOfDateRowsForView(idAndVersion, scopeType, allContainersInScope,
				TableViewManagerImpl.MAX_ROWS_PER_TRANSACTION)).thenReturn(pageOne, pageTwo);
		// call under test
		manager.applyChangesToAvailableView(idAndVersion);
		verify(mockIndexManager).updateViewRowsInTransaction(idAndVersion, pageOne, scopeType,
				allContainersInScope, viewSchema);
		// page two should be ignored since it overlaps with page one.
		verify(mockIndexManager, never()).updateViewRowsInTransaction(idAndVersion, pageTwo, scopeType,
				allContainersInScope, viewSchema);
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class),
				any(Exception.class));
		verify(mockTableManagerSupport, times(1)).updateChangedOnIfAvailable(idAndVersion);
	}
	
	/**
	 * Any failure should change change the table's status to failed.
	 */
	@Test
	public void testApplyChangesToAvailableViewHoldingLock_Failed() {
		setupApplyChanges();
		Set<Long> pageOne = Sets.newHashSet(101L,102L);
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.AVAILABLE));
		when(mockIndexManager.getOutOfDateRowsForView(idAndVersion, scopeType, allContainersInScope,
				TableViewManagerImpl.MAX_ROWS_PER_TRANSACTION)).thenReturn(pageOne);
		// setup a failure
		IllegalArgumentException exception = new IllegalArgumentException("Something is wrong...");
		doThrow(exception).when(mockIndexManager).updateViewRowsInTransaction(any(IdAndVersion.class), anySet(), any(),
				anySet(), anyList());
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.applyChangesToAvailableView(idAndVersion);
		});
		verify(mockIndexManager).updateViewRowsInTransaction(idAndVersion, pageOne, scopeType,
				allContainersInScope, viewSchema);
		// should fail and change the table status.
		verify(mockTableManagerSupport).attemptToSetTableStatusToFailed(idAndVersion, exception);
	}
	
	@Test
	public void testApplyChangesToAvailableViewOrSnapshotWithoutVersion() {
		idAndVersion = IdAndVersion.parse("syn123");
		// call under test
		managerSpy.applyChangesToAvailableViewOrSnapshot(idAndVersion);
		verify(managerSpy).applyChangesToAvailableView(idAndVersion);
		verify(managerSpy, never()).refreshBenefactorsForViewSnapshot(any());
	}
	
	@Test
	public void testApplyChangesToAvailableViewOrSnapshotWithVersion() {
		when(mockConnectionFactory.connectToTableIndex(any())).thenReturn(mockIndexManager);
		idAndVersion = IdAndVersion.parse("syn123.1");
		// call under test
		managerSpy.applyChangesToAvailableViewOrSnapshot(idAndVersion);
		verify(managerSpy, never()).applyChangesToAvailableView(any());
		verify(managerSpy).refreshBenefactorsForViewSnapshot(idAndVersion);
	}
	
	@Test
	public void testApplyChangesToAvailableViewOrSnapshotWithoutNull() {
		idAndVersion = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			managerSpy.applyChangesToAvailableViewOrSnapshot(idAndVersion);
		});
	}
	
	@Test
	public void testRefreshBenefactorsForViewSnapshot() {
		when(mockConnectionFactory.connectToTableIndex(any())).thenReturn(mockIndexManager);
		// call under test
		manager.refreshBenefactorsForViewSnapshot(idAndVersion);
		verify(mockIndexManager).refreshViewBenefactors(idAndVersion);
	}
	
	@Test
	public void testRefreshBenefactorsForViewSnapshotWithNull() {
		idAndVersion = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.refreshBenefactorsForViewSnapshot(idAndVersion);
		});
	}

	/**
	 * Helper to setup a call to applyChangesToAvailableViewHoldingLock()
	 */
	void setupApplyChanges() {
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);
		when(mockTableManagerSupport.getAllContainerIdsForViewScope(idAndVersion, scopeType))
				.thenReturn(allContainersInScope);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(viewSchema);
	}
}
