package org.sagebionetworks.repo.manager.table;

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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.replication.ReplicationManager;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProviderFactory;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.dao.table.InvalidStatusTokenException;
import org.sagebionetworks.repo.model.dbo.dao.table.TableSnapshot;
import org.sagebionetworks.repo.model.dbo.dao.table.TableSnapshotDao;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.semaphore.LockContext;
import org.sagebionetworks.repo.model.semaphore.LockContext.ContextType;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewScopeType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.table.cluster.UndefinedViewScopeException;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.ViewIndexDescription;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolver;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolverFactory;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolverImpl;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.cluster.view.filter.HierarchicaFilter;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockType;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;

import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class TableViewManagerImplTest {

	@Mock
	private ViewScopeDao viewScopeDao;
	@Mock
	private ColumnModelManager mockColumnModelManager;
	@Mock
	private TableManagerSupport mockTableManagerSupport;
	@Mock
	private ColumnModelDAO columnModelDao;
	@Mock
	private NodeManager mockNodeManager;
	@Mock
	private ReplicationManager mockReplicationManager;
	@Mock
	private ProgressCallback mockProgressCallback;
	@Mock
	private TableIndexConnectionFactory mockConnectionFactory;
	@Mock
	private TableIndexManager mockIndexManager;
	@Mock
	private StackConfiguration mockConfig;
	@Mock
	private TableSnapshotDao mockViewSnapshotDao;
	@Mock
	private MetadataIndexProviderFactory mockMetadataIndexProviderFactory;
	@Mock
	private ObjectFieldModelResolverFactory mockObjectFieldModelResolverFactory;
	@Mock
	private MetadataIndexProvider mockMetadataIndexProvider;
	@Mock
	private ViewFilter mockFilter;
	
	@Captor
	private ArgumentCaptor<PutObjectRequest> putRequestCaptor;
	@Captor
	private ArgumentCaptor<TableSnapshot> snapshotCaptor;
	@Captor
	private ArgumentCaptor<Annotations> annotationsCaptor;
	
	@InjectMocks
	private TableViewManagerImpl manager;
	
	private TableViewManagerImpl managerSpy;
	
	private UserInfo userInfo;
	private List<String> schema;
	private List<String> scope;
	private String viewId;
	private IdAndVersion idAndVersion;
	private Long viewType;
	
	private Set<Long> scopeIds;
	private ViewScope viewScope;
	private long rowCount;
	private List<Row> rows;
	private long viewCRC;
	private List<ColumnModel> viewSchema;
	private ColumnModel etagColumn;
	private ColumnModel anno1;
	private ColumnModel anno2;
	private ColumnModel dateColumn;
	private ColumnModel intListColumn;
	private SparseRowDto row;
	private SnapshotRequest snapshotOptions;
	private Set<Long> allContainersInScope;
	private Annotations annotationsV2;
	private IndexDescription indexDescription;
	private long pageSize;
	private ViewScopeType scopeType;
	
	private ObjectFieldModelResolver objectFieldModelResolver;
	private LockContext expectedLockContext;
	

	@BeforeEach
	public void before(){
		objectFieldModelResolver = new ObjectFieldModelResolverImpl(mockMetadataIndexProvider);
		
		userInfo = new UserInfo(false, 888L);
		schema = Lists.newArrayList("1","2","3");
		scope = Lists.newArrayList("syn123", "syn456");
		scopeIds = new HashSet<Long>(KeyFactory.stringToKey(scope));
		
		viewId = "syn555";
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
		
		indexDescription =  new ViewIndexDescription(idAndVersion, TableType.entityview);
		
		managerSpy = Mockito.spy(manager);
		
		pageSize = 2;
		
		expectedLockContext = new LockContext(ContextType.UpdatingViewIndex, idAndVersion);
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
		verify(mockReplicationManager).replicate(objectType.getMainType(), "syn111");
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
		verify(mockReplicationManager).replicate(objectType.getMainType(), "syn111");
		
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
		TableSnapshot snapshot = new TableSnapshot().withBucket("bucket").withKey("key").withSnapshotId(snapshotId);
		when(mockViewSnapshotDao.getSnapshot(idAndVersion)).thenReturn(Optional.of(snapshot));
		
		// call under test
		long id = manager.populateViewFromSnapshot(indexDescription, mockIndexManager);
		assertEquals(snapshotId, id);
		verify(mockViewSnapshotDao).getSnapshot(idAndVersion);
		verify(mockTableManagerSupport).restoreTableIndexFromS3(idAndVersion, "bucket", "key");
		verify(mockIndexManager).refreshViewBenefactors(idAndVersion);
	}
	
	@Test
	public void testPopulateViewFromSnapshotWithNotFound() throws IOException {
		when(mockViewSnapshotDao.getSnapshot(idAndVersion)).thenReturn(Optional.empty());
		
		String errorMessage = assertThrows(NotFoundException.class, () -> {
			// call under test			
			manager.populateViewFromSnapshot(indexDescription, mockIndexManager);
		}).getMessage();
		
		assertEquals("Snapshot not found for: " + idAndVersion.toString(), errorMessage);
		
		verify(mockViewSnapshotDao).getSnapshot(idAndVersion);
	}
		
	@Test
	public void testPopulateViewIndexFromReplication() {
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);

		viewCRC = 987L;
		when(mockIndexManager.populateViewFromEntityReplication(idAndVersion.getId(), scopeType, viewSchema)).thenReturn(viewCRC);
		// call under test
		long resultCRC32 = manager.populateViewIndexFromReplication(idAndVersion, mockIndexManager, viewSchema);
		assertEquals(viewCRC, resultCRC32);
		verify(mockIndexManager).populateViewFromEntityReplication(idAndVersion.getId(), scopeType, viewSchema);
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
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		when(mockIndexManager.resetTableIndex(any())).thenReturn(viewSchema);
		doNothing().when(mockIndexManager).buildTableIndexIndices(any(), any());

		viewCRC = 987L;
		when(mockIndexManager.populateViewFromEntityReplication(idAndVersion.getId(), scopeType, viewSchema)).thenReturn(viewCRC);
		
		// call under test
		manager.createOrRebuildViewHoldingLock(idAndVersion);

		verify(mockTableManagerSupport).isIndexWorkRequired(idAndVersion);
		verify(mockTableManagerSupport).startTableProcessing(idAndVersion);
		verify(mockTableManagerSupport).getViewScopeType(idAndVersion);
		verify(mockConnectionFactory).connectToTableIndex(idAndVersion);
		verify(mockTableManagerSupport).getIndexDescription(idAndVersion);
		verify(mockIndexManager).resetTableIndex(indexDescription);
		verify(mockTableManagerSupport).attemptToUpdateTableProgress(idAndVersion, token, "Copying data to view...", 0L, 1L);
		verify(mockIndexManager).populateViewFromEntityReplication(idAndVersion.getId(), scopeType, viewSchema);
		verify(mockTableManagerSupport, never()).restoreTableIndexFromS3(any(), any(), any());
		verify(mockIndexManager).buildTableIndexIndices(indexDescription, viewSchema);
		verify(mockIndexManager).setIndexVersion(idAndVersion, viewCRC);
		verify(mockTableManagerSupport).attemptToSetTableStatusToAvailable(idAndVersion, token,
				TableViewManagerImpl.DEFAULT_ETAG);
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class),
				any(Exception.class));
		verify(mockViewSnapshotDao, never()).getSnapshot(any(IdAndVersion.class));
	}
	
	/**
	 * Populate a view from a snapshot.
	 * @throws IOException
	 * @throws RecoverableMessageException 
	 */
	@Test
	public void testCreateOrRebuildViewHoldingLockWorkeRequiredWithVersion() throws IOException, RecoverableMessageException {
		idAndVersion = IdAndVersion.parse("syn123.45");
		indexDescription = new ViewIndexDescription(idAndVersion, TableType.entityview);
		when(mockTableManagerSupport.isIndexWorkRequired(idAndVersion)).thenReturn(true);
		String token = "the token";
		when(mockTableManagerSupport.startTableProcessing(idAndVersion)).thenReturn(token);
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		when(mockIndexManager.resetTableIndex(any())).thenReturn(viewSchema);
		doNothing().when(mockIndexManager).buildTableIndexIndices(any(), any());
		long snapshotId = 998L;
		TableSnapshot snapshot = new TableSnapshot().withBucket("bucket").withKey("key").withSnapshotId(snapshotId);
		when(mockViewSnapshotDao.getSnapshot(idAndVersion)).thenReturn(Optional.of(snapshot));
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		
		// call under test
		manager.createOrRebuildViewHoldingLock(idAndVersion);

		verify(mockTableManagerSupport).isIndexWorkRequired(idAndVersion);
		verify(mockTableManagerSupport).startTableProcessing(idAndVersion);
		verify(mockConnectionFactory).connectToTableIndex(idAndVersion);
		verify(mockTableManagerSupport).getIndexDescription(idAndVersion);
		verify(mockIndexManager).resetTableIndex(indexDescription);
		verify(mockTableManagerSupport).attemptToUpdateTableProgress(idAndVersion, token, "Copying data to view...", 0L,
				1L);
		verify(mockIndexManager, never()).populateViewFromEntityReplication(any(Long.class), any(), any());
		verify(mockViewSnapshotDao).getSnapshot(idAndVersion);
		verify(mockTableManagerSupport).restoreTableIndexFromS3(idAndVersion, "bucket", "key");
		verify(mockIndexManager).buildTableIndexIndices(indexDescription, viewSchema);
		verify(mockIndexManager).setIndexVersion(idAndVersion, snapshotId);
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
		when(mockIndexManager.resetTableIndex(any())).thenReturn(viewSchema);
		doNothing().when(mockIndexManager).buildTableIndexIndices(any(), any());
		viewCRC = 987L;
		when(mockIndexManager.populateViewFromEntityReplication(idAndVersion.getId(), scopeType, viewSchema))
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
	
	@Test
	public void testCreateOrRebuildViewHoldingLockWithRecoverableMessageException() throws IOException {
		when(mockTableManagerSupport.isIndexWorkRequired(idAndVersion)).thenReturn(true);
		String token = "the token";
		when(mockTableManagerSupport.startTableProcessing(idAndVersion)).thenReturn(token);
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		when(mockIndexManager.resetTableIndex(any())).thenReturn(viewSchema);
		
		RecoverableMessageException exception = new RecoverableMessageException();
		
		doThrow(exception).when(mockIndexManager).populateViewFromEntityReplication(any(), any(), any());
		
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			manager.createOrRebuildViewHoldingLock(idAndVersion);
		});
		
		assertEquals(exception, result);
		
		verify(mockIndexManager).populateViewFromEntityReplication(idAndVersion.getId(), scopeType, viewSchema);
		// conflict should not set the view to failed.
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(any(), any());
	}
		
	@Test
	public void testValidateViewForSnapshot() throws TableUnavailableException {
		when(mockTableManagerSupport.getTableStatusState(any())).thenReturn(Optional.of(TableState.AVAILABLE));
		when(mockTableManagerSupport.getViewScopeType(any())).thenReturn(scopeType);
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockMetadataIndexProvider.getViewFilter(any())).thenReturn(mockFilter);
		
		// Call under test
		manager.validateViewForSnapshot(idAndVersion);
		
		verify(mockTableManagerSupport).getTableStatusState(idAndVersion);
		verify(mockTableManagerSupport).getViewScopeType(idAndVersion);
		verify(mockMetadataIndexProviderFactory).getMetadataIndexProvider(scopeType.getObjectType());
		verify(mockMetadataIndexProvider).getViewFilter(idAndVersion.getId());
	}
	
	@Test
	public void testValidateViewForSnapshotWithNotAvailable() {
		for (TableState state : TableState.values()) {
			Mockito.reset(mockTableManagerSupport);
			
			if (TableState.AVAILABLE == state) {
				continue;
			}
			
			when(mockTableManagerSupport.getTableStatusState(any())).thenReturn(Optional.of(state));	
			
			String result = assertThrows(IllegalArgumentException.class, () -> {			
				// Call under test
				manager.validateViewForSnapshot(idAndVersion);
			}).getMessage();
			
			assertEquals("You cannot create a version of a view that is not available (Status: " + state + ").", result);
			
			verify(mockTableManagerSupport).getTableStatusState(idAndVersion);
		}
	}
	
	@Test
	public void testValidateViewForSnapshotWithEmptyStatus() throws TableUnavailableException {
		
		when(mockTableManagerSupport.getTableStatusState(any())).thenReturn(Optional.empty());	
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.validateViewForSnapshot(idAndVersion);
		}).getMessage();
		
		assertEquals("You cannot create a version of a view that is not available.", result);
		
		verify(mockTableManagerSupport).getTableStatusState(idAndVersion);	
		
	}
	
	@Test
	public void testValidateViewForSnapshotWithNoId() {
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.validateViewForSnapshot(null);
		}).getMessage();
		
		assertEquals("The view id is required.", result);
	}
	
	@Test
	public void testValidateViewForSnapshotWithEmptyFilter() throws TableUnavailableException {
		when(mockTableManagerSupport.getTableStatusState(any())).thenReturn(Optional.of(TableState.AVAILABLE));
		when(mockTableManagerSupport.getViewScopeType(any())).thenReturn(scopeType);
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockMetadataIndexProvider.getViewFilter(any())).thenReturn(mockFilter);
		when(mockFilter.isEmpty()).thenReturn(true);
		
		String result = assertThrows(UndefinedViewScopeException.class, () -> {			
			// Call under test
			manager.validateViewForSnapshot(idAndVersion);
		}).getMessage();
		
		assertEquals("You cannot create a version of a view that has no scope.", result);
		
		verify(mockTableManagerSupport).getTableStatusState(idAndVersion);
		verify(mockTableManagerSupport).getViewScopeType(idAndVersion);
		verify(mockMetadataIndexProviderFactory).getMetadataIndexProvider(scopeType.getObjectType());
		verify(mockMetadataIndexProvider).getViewFilter(idAndVersion.getId());
	}
	
	@Test
	public void testValidateViewForSnapshotWithEmptyFilterAndDataset() throws TableUnavailableException {
		when(mockTableManagerSupport.getTableStatusState(any())).thenReturn(Optional.of(TableState.AVAILABLE));
		when(mockTableManagerSupport.getViewScopeType(any())).thenReturn(new ViewScopeType(ViewObjectType.DATASET, ViewTypeMask.File.getMask()));
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockMetadataIndexProvider.getViewFilter(any())).thenReturn(mockFilter);
		when(mockFilter.isEmpty()).thenReturn(true);
		
		String result = assertThrows(UndefinedViewScopeException.class, () -> {			
			// Call under test
			manager.validateViewForSnapshot(idAndVersion);
		}).getMessage();
		
		assertEquals("You cannot create a version of an empty Dataset. Add files to this Dataset before creating a version.", result);
		
		verify(mockTableManagerSupport).getTableStatusState(idAndVersion);
		verify(mockTableManagerSupport).getViewScopeType(idAndVersion);
		verify(mockMetadataIndexProviderFactory).getMetadataIndexProvider(ViewObjectType.DATASET);
		verify(mockMetadataIndexProvider).getViewFilter(idAndVersion.getId());
	}
	
	@Test
	public void testValidateViewForSnapshotWithEmptyFilterAndDatasetCollection() throws TableUnavailableException {
		when(mockTableManagerSupport.getTableStatusState(any())).thenReturn(Optional.of(TableState.AVAILABLE));
		when(mockTableManagerSupport.getViewScopeType(any())).thenReturn(new ViewScopeType(ViewObjectType.DATASET_COLLECTION, ViewTypeMask.Dataset.getMask()));
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockMetadataIndexProvider.getViewFilter(any())).thenReturn(mockFilter);
		when(mockFilter.isEmpty()).thenReturn(true);
		
		String result = assertThrows(UndefinedViewScopeException.class, () -> {			
			// Call under test
			manager.validateViewForSnapshot(idAndVersion);
		}).getMessage();
		
		assertEquals("You cannot create a version of an empty Dataset Collection. Add Datasets to this Dataset Collection before creating a version.", result);
		
		verify(mockTableManagerSupport).getTableStatusState(idAndVersion);
		verify(mockTableManagerSupport).getViewScopeType(idAndVersion);
		verify(mockMetadataIndexProviderFactory).getMetadataIndexProvider(ViewObjectType.DATASET_COLLECTION);
		verify(mockMetadataIndexProvider).getViewFilter(idAndVersion.getId());
	}
		
	@Test
	public void testCreateSnapshot() throws Exception {
		setupNonExclusiveLockWithCustomKeyToForwardToCallack();
		
		doNothing().when(managerSpy).validateViewForSnapshot(any());
		when(mockTableManagerSupport.streamTableIndexToS3(any(), any(), any())).thenReturn(schema);
		
		String bucket = "snapshot.bucket";
		when(mockConfig.getViewSnapshotBucketName()).thenReturn(bucket);
		
		long snapshotVersion = 12L;
		
		when(mockNodeManager.createSnapshotAndVersion(any(), any(), any())).thenReturn(snapshotVersion);
		
		
		// call under test
		long result = managerSpy.createSnapshot(userInfo, idAndVersion.getId(), snapshotOptions, mockProgressCallback);
		
		verify(mockTableManagerSupport).tryRunWithTableNonExclusiveLock(
				eq(mockProgressCallback),
				eq(new LockContext(ContextType.ViewSnapshot, idAndVersion)),
				any(),
				eq(TableModelUtils.getTableSemaphoreKey(idAndVersion)),
				eq(TableModelUtils.getViewDeltaSemaphoreKey(idAndVersion)));
		
		assertEquals(snapshotVersion, result);
		
		verify(managerSpy).validateViewForSnapshot(idAndVersion);
		
		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		
		verify(mockTableManagerSupport).streamTableIndexToS3(eq(idAndVersion), eq(bucket), keyCaptor.capture());
		
		String key = keyCaptor.getValue();
		
		assertTrue(key.startsWith(""+idAndVersion.getId()));
		
		verify(mockNodeManager).createSnapshotAndVersion(userInfo, idAndVersion.getId().toString(), snapshotOptions);
		
		IdAndVersion expectedIdAndVersion = IdAndVersion.newBuilder().setId(idAndVersion.getId())
				.setVersion(snapshotVersion).build();
		
		verify(mockColumnModelManager).bindColumnsToVersionOfObject(schema, expectedIdAndVersion);
		
		TableSnapshot expectedSnapshot = new TableSnapshot()
			.withBucket(bucket)
			.withKey(key)
			.withCreatedBy(userInfo.getId())
			.withTableId(idAndVersion.getId())
			.withVersion(snapshotVersion);
		
		verify(mockViewSnapshotDao).createSnapshot(snapshotCaptor.capture());
		
		TableSnapshot snapshot = snapshotCaptor.getValue();
		expectedSnapshot.withCreatedOn(snapshot.getCreatedOn());
		
		assertEquals(expectedSnapshot, snapshot);
		
		// fix for PLFM-5957
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(expectedIdAndVersion);
		
	}
		
	@Test
	public void testApplyChangesToAvailableView() throws Exception {
		setupNonExclusiveLockToForwardToCallack();
		setupExclusiveLockToForwardToCallack();
		
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(viewSchema);
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockMetadataIndexProvider.getViewFilter(any())).thenReturn(mockFilter);
		
		// call under test
		managerSpy.applyChangesToAvailableView(idAndVersion, mockProgressCallback);
		verify(mockTableManagerSupport).tryRunWithTableNonExclusiveLock(eq(mockProgressCallback), eq(expectedLockContext), any(),
				eq(idAndVersion));
		String expectedKey = TableModelUtils.getViewDeltaSemaphoreKey(idAndVersion);
		verify(mockTableManagerSupport).tryRunWithTableExclusiveLock(eq(mockProgressCallback), eq(expectedLockContext), eq(expectedKey),
				any());
		verify(managerSpy).applyChangesToAvailableViewOrSnapshot(idAndVersion);
	}
	
	/**
	 * Setup the tryRunWithTableNonexclusiveLock() to forward the call to the passed callback.
	 * @throws Exception
	 */
	void setupNonExclusiveLockToForwardToCallack() throws Exception {
		doAnswer((InvocationOnMock invocation) -> {
			// Last argument is the callback
			Object[] args = invocation.getArguments();
			ProgressingCallable<?> callable = (ProgressingCallable<?>) args[args.length - 2];
			return callable.call(mockProgressCallback);
		}).when(mockTableManagerSupport).tryRunWithTableNonExclusiveLock(any(ProgressCallback.class), any(),
				any(), any(IdAndVersion.class));
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
			return callable.call(mockProgressCallback);
		}).when(mockTableManagerSupport).tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(),
				anyString(), any());
	}
	
	void setupNonExclusiveLockWithCustomKeyToForwardToCallack() throws Exception {
		doAnswer((InvocationOnMock invocation) -> {
			Object[] args = invocation.getArguments();
			ProgressingCallable<?> callable = (ProgressingCallable<?>) args[2];
			return callable.call(mockProgressCallback);
		}).when(mockTableManagerSupport).tryRunWithTableNonExclusiveLock(any(ProgressCallback.class), any(),
				any(), any(String.class));
	}
	
	@Test
	public void testApplyChangesToAvailableView_ExcluisveLockUnavailable() throws Exception {
		setupNonExclusiveLockToForwardToCallack();
		LockUnavilableException exception = new LockUnavilableException(LockType.Read, "key", "context");
		doThrow(exception).when(mockTableManagerSupport).tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(), any(String.class), any());
		String expectedKey = TableModelUtils.getViewDeltaSemaphoreKey(idAndVersion);
		// call under test
		managerSpy.applyChangesToAvailableView(idAndVersion, mockProgressCallback);
		verify(mockTableManagerSupport).tryRunWithTableExclusiveLock(eq(mockProgressCallback), eq(expectedLockContext), eq(expectedKey),
				any());
		verify(mockTableManagerSupport).tryRunWithTableNonExclusiveLock(eq(mockProgressCallback), eq(expectedLockContext), any(),
				eq(idAndVersion));
	}
	
	@Test
	public void testApplyChangesToAvailableView_NonExcluisveLockUnavailable() throws Exception {
		LockUnavilableException exception = new LockUnavilableException(LockType.Read, "key", "context");
		doThrow(exception).when(mockTableManagerSupport).tryRunWithTableNonExclusiveLock(any(ProgressCallback.class), any(),
				any(), any(IdAndVersion.class));
		// call under test
		manager.applyChangesToAvailableView(idAndVersion, mockProgressCallback);
		verify(mockTableManagerSupport).tryRunWithTableNonExclusiveLock(eq(mockProgressCallback), eq(expectedLockContext), any(),
				eq(idAndVersion));
		verifyNoMoreInteractions(mockTableManagerSupport);
	}
	
	@Test
	public void testApplyChangesToAvailableView_OtherException() throws Exception {
		setupNonExclusiveLockToForwardToCallack();
		IllegalArgumentException exception = new IllegalArgumentException("not now");
		doThrow(exception).when(mockTableManagerSupport).tryRunWithTableExclusiveLock(any(ProgressCallback.class), any(),
				anyString(), any());
		IllegalArgumentException result =assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.applyChangesToAvailableView(idAndVersion, mockProgressCallback);
		});
		assertEquals(exception, result);
	}
	
	@Test
	public void testApplyChangesToAvailableViewHoldingLock_NothingToDo() {

		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(viewSchema);
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);		
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.AVAILABLE));
		
		HierarchicaFilter filter = new HierarchicaFilter(ReplicationType.ENTITY, Sets.newHashSet(SubType.file),
				allContainersInScope);
		when(mockMetadataIndexProvider.getViewFilter(any())).thenReturn(filter);

		// call under test
		manager.applyChangesToAvailableView(idAndVersion, pageSize);
		verify(mockTableManagerSupport).getIndexDescription(idAndVersion);
		verify(mockTableManagerSupport).getTableStatusState(idAndVersion);
		verify(mockIndexManager, never()).updateViewRowsInTransaction(any(), any(), any(), any());
		verifyNoMoreInteractions(mockTableManagerSupport);
		verify(mockIndexManager, times(1)).getOutOfDateRowsForView(idAndVersion, filter, pageSize);
	}
	
	@Test
	public void testApplyChangesToAvailableViewHoldingLock_OnePage() {
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(viewSchema);
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);

		Set<Long> rowsToUpdate = Sets.newHashSet(101L, 102L);
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.AVAILABLE));
		when(mockIndexManager.getOutOfDateRowsForView(any(), any(), anyLong())).thenReturn(rowsToUpdate);
		when(mockIndexManager.updateViewRowsInTransaction(any(), any(), any(), any())).thenReturn(101L);
		HierarchicaFilter filter = new HierarchicaFilter(ReplicationType.ENTITY, Sets.newHashSet(SubType.file),
				allContainersInScope);
		when(mockMetadataIndexProvider.getViewFilter(any())).thenReturn(filter);
		

		// call under test
		manager.applyChangesToAvailableView(idAndVersion, pageSize);
		verify(mockIndexManager).updateViewRowsInTransaction(indexDescription, scopeType, viewSchema,
				filter.newBuilder().addLimitObjectids(rowsToUpdate).build());
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class),
				any(Exception.class));
		verify(mockTableManagerSupport).updateChangedOnIfAvailable(idAndVersion);
		
		verify(mockIndexManager, times(2)).getOutOfDateRowsForView(idAndVersion, filter, pageSize);
	}
	
	@Test
	public void testApplyChangesToAvailableViewHoldingLock_OnePageNotAvailable() {
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(viewSchema);
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockMetadataIndexProvider.getViewFilter(any())).thenReturn(mockFilter);
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);		
		// do no work when not available.
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.PROCESSING));
		// call under test
		manager.applyChangesToAvailableView(idAndVersion, pageSize);
		verify(mockTableManagerSupport).getIndexDescription(idAndVersion);
		verify(mockTableManagerSupport).getTableStatusState(idAndVersion);
		verifyNoMoreInteractions(mockTableManagerSupport);
		verifyNoMoreInteractions(mockIndexManager);
	}
	
	@Test
	public void testApplyChangesToAvailableViewHoldingLock_OnePageNoState() {
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(viewSchema);
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockMetadataIndexProvider.getViewFilter(any())).thenReturn(mockFilter);
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);		
		// do no work when not available.
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.empty());
		// call under test
		manager.applyChangesToAvailableView(idAndVersion, pageSize);
		verify(mockTableManagerSupport).getIndexDescription(idAndVersion);
		verify(mockTableManagerSupport).getTableStatusState(idAndVersion);
		verifyNoMoreInteractions(mockTableManagerSupport);
		verifyNoMoreInteractions(mockIndexManager);
	}
	
	
	@Test
	public void testapplyChangesToAvailableViewWithNull() {
		idAndVersion = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.applyChangesToAvailableView(idAndVersion, pageSize);
		});
	}
	
	@Test
	public void testapplyChangesToAvailableViewWithVersionNumber() {
		idAndVersion = IdAndVersion.parse("syn123.1");
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.applyChangesToAvailableView(idAndVersion, pageSize);
		}).getMessage();
		assertEquals("This method cannot be called on a view snapshot", message);
	}
	
	/**
	 * This is a test for PLFM-7548.
	 */
	@Test
	public void testApplyChangesToAvailableViewHoldingLockWithSinglePageUnderPageSize() {
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(viewSchema);
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);

		pageSize = 2;
		Set<Long> pageOne = Sets.newHashSet(101L);
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.AVAILABLE));
		when(mockIndexManager.getOutOfDateRowsForView(any(), any(), anyLong())).thenReturn(pageOne);
		when(mockIndexManager.updateViewRowsInTransaction(any(), any(), any(), any())).thenReturn(101L);
		
		HierarchicaFilter filter = new HierarchicaFilter(ReplicationType.ENTITY, Sets.newHashSet(SubType.file),
				allContainersInScope);
		when(mockMetadataIndexProvider.getViewFilter(any())).thenReturn(filter);
		
		// call under test
		manager.applyChangesToAvailableView(idAndVersion, pageSize);
		verify(mockIndexManager).updateViewRowsInTransaction(indexDescription, scopeType, viewSchema,
				filter.newBuilder().addLimitObjectids(pageOne).build());
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class),
				any(Exception.class));
		verify(mockIndexManager).setIndexVersion(idAndVersion, 101L);
		verify(mockTableManagerSupport, times(1)).updateChangedOnIfAvailable(idAndVersion);
		verify(mockIndexManager, times(1)).getOutOfDateRowsForView(idAndVersion, filter, pageSize);
	}
	
	/**
	 * This is a test for PLFM-7548.
	 */
	@Test
	public void testApplyChangesToAvailableViewHoldingLockWithSinglePageAtPageSize() {
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(viewSchema);
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);

		pageSize = 2;
		Set<Long> pageOne = Sets.newHashSet(101L, 102L);
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.AVAILABLE));
		when(mockIndexManager.getOutOfDateRowsForView(any(), any(), anyLong())).thenReturn(pageOne);
		when(mockIndexManager.updateViewRowsInTransaction(any(), any(), any(), any())).thenReturn(101L);

		HierarchicaFilter filter = new HierarchicaFilter(ReplicationType.ENTITY, Sets.newHashSet(SubType.file),
				allContainersInScope);
		when(mockMetadataIndexProvider.getViewFilter(any())).thenReturn(filter);
		
		// call under test
		manager.applyChangesToAvailableView(idAndVersion, pageSize);
		verify(mockIndexManager).updateViewRowsInTransaction(indexDescription, scopeType, viewSchema,
				filter.newBuilder().addLimitObjectids(pageOne).build());
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class),
				any(Exception.class));
		verify(mockIndexManager).setIndexVersion(idAndVersion, 101L);
		verify(mockTableManagerSupport, times(1)).updateChangedOnIfAvailable(idAndVersion);
		verify(mockIndexManager, times(2)).getOutOfDateRowsForView(idAndVersion, filter, pageSize);
	}
	
	/**
	 * This is a test for PLFM-7548.
	 */
	@Test
	public void testApplyChangesToAvailableViewHoldingLockWithSinglePageOverPageSize() {
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(viewSchema);
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);

		Set<Long> pageOne = Sets.newHashSet(101L, 102L, 103L);
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.AVAILABLE));
		when(mockIndexManager.getOutOfDateRowsForView(any(), any(), anyLong())).thenReturn(pageOne);
		when(mockIndexManager.updateViewRowsInTransaction(any(), any(), any(), any())).thenReturn(101L);
		
		HierarchicaFilter filter = new HierarchicaFilter(ReplicationType.ENTITY, Sets.newHashSet(SubType.file),
				allContainersInScope);
		when(mockMetadataIndexProvider.getViewFilter(any())).thenReturn(filter);
		
		// call under test
		manager.applyChangesToAvailableView(idAndVersion, pageSize);
		verify(mockIndexManager).updateViewRowsInTransaction(indexDescription, scopeType, viewSchema,
				filter.newBuilder().addLimitObjectids(pageOne).build());
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class),
				any(Exception.class));
		verify(mockIndexManager).setIndexVersion(idAndVersion, 101L);
		verify(mockTableManagerSupport, times(1)).updateChangedOnIfAvailable(idAndVersion);
		verify(mockIndexManager, times(2)).getOutOfDateRowsForView(idAndVersion, filter, pageSize);
	}
	
	/**
	 * If progress is made on a single page of changes, the IDs should not reappear in the next
	 * page, and all pages should be processed.
	 */
	@Test
	public void testApplyChangesToAvailableViewHoldingLockWithMultiplePagesNoOverlap() {
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(viewSchema);
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);

		Set<Long> pageOne = Sets.newHashSet(101L, 102L);
		Set<Long> pageTwo = Sets.newHashSet(103L, 104L);
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.AVAILABLE));
		when(mockIndexManager.getOutOfDateRowsForView(any(), any(), anyLong())).thenReturn(pageOne, pageTwo);
		when(mockIndexManager.updateViewRowsInTransaction(any(), any(), any(), any())).thenReturn(101L, 102L);

		HierarchicaFilter filter = new HierarchicaFilter(ReplicationType.ENTITY, Sets.newHashSet(SubType.file),
				allContainersInScope);
		when(mockMetadataIndexProvider.getViewFilter(any())).thenReturn(filter);
		
		// call under test
		manager.applyChangesToAvailableView(idAndVersion, pageSize);
		verify(mockIndexManager).updateViewRowsInTransaction(indexDescription, scopeType, viewSchema,
				filter.newBuilder().addLimitObjectids(pageOne).build());
		verify(mockIndexManager).updateViewRowsInTransaction(indexDescription, scopeType, viewSchema,
				filter.newBuilder().addLimitObjectids(pageTwo).build());
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class),
				any(Exception.class));
		verify(mockIndexManager).setIndexVersion(idAndVersion, 101L);
		verify(mockIndexManager).setIndexVersion(idAndVersion, 102L);
		verify(mockTableManagerSupport, times(2)).updateChangedOnIfAvailable(idAndVersion);
		verify(mockIndexManager, times(3)).getOutOfDateRowsForView(idAndVersion, filter, pageSize);
	}
	
	@Test
	public void testApplyChangesToAvailableViewHoldingLock_MultiplePagesNoOverlapStatusChanged() {
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(viewSchema);
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockIndexManager.updateViewRowsInTransaction(any(), any(), any(), any())).thenReturn(101L);
		
		Set<Long> pageOne = Sets.newHashSet(101L, 102L);
		Set<Long> pageTwo = Sets.newHashSet(103L, 104L);
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		// Status starts as available but changes to processing which stop the updates.
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.AVAILABLE),
				Optional.of(TableState.PROCESSING));
		when(mockIndexManager.getOutOfDateRowsForView(any(), any(), anyLong())).thenReturn(pageOne, pageTwo);

		HierarchicaFilter filter = new HierarchicaFilter(ReplicationType.ENTITY, Sets.newHashSet(SubType.file),
				allContainersInScope);
		when(mockMetadataIndexProvider.getViewFilter(any())).thenReturn(filter);
		

		// call under test
		manager.applyChangesToAvailableView(idAndVersion, pageSize);
		verify(mockIndexManager).updateViewRowsInTransaction(indexDescription, scopeType, viewSchema,
				filter.newBuilder().addLimitObjectids(pageOne).build());
		// page two should not be updated because of the switch to processing.
		verify(mockIndexManager, never()).updateViewRowsInTransaction(indexDescription, scopeType, viewSchema,
				filter.newBuilder().addLimitObjectids(pageTwo).build());
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class),
				any(Exception.class));
		verify(mockIndexManager).setIndexVersion(idAndVersion, 101L);
		verify(mockTableManagerSupport, times(1)).updateChangedOnIfAvailable(idAndVersion);
		verify(mockIndexManager, times(1)).getOutOfDateRowsForView(idAndVersion, filter, pageSize);
	}
	
	/**
	 * There are multiple page with overlap.  The overlap should terminate the process.
	 */
	@Test
	public void testApplyChangesToAvailableViewHoldingLock_MultiplePagesWithOverlap() {
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(viewSchema);
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		
		pageSize = 2;
		Set<Long> pageOne = Sets.newHashSet(101L,102L);
		Set<Long> pageTwo = Sets.newHashSet(102L,103L);
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.AVAILABLE));
		when(mockIndexManager.getOutOfDateRowsForView(any(), any(), anyLong())).thenReturn(pageOne, pageTwo);
		when(mockIndexManager.updateViewRowsInTransaction(any(), any(), any(), any())).thenReturn(101L);
		
		HierarchicaFilter filter = new HierarchicaFilter(ReplicationType.ENTITY, Sets.newHashSet(SubType.file),
				allContainersInScope);
		when(mockMetadataIndexProvider.getViewFilter(any())).thenReturn(filter);
		
		
		// call under test
		manager.applyChangesToAvailableView(idAndVersion, pageSize);
		verify(mockIndexManager).updateViewRowsInTransaction(indexDescription, scopeType, viewSchema,
				filter.newBuilder().addLimitObjectids(pageOne).build());
		// page two should be ignored since it overlaps with page one.
		verify(mockIndexManager, never()).updateViewRowsInTransaction(indexDescription, scopeType, viewSchema,
				filter.newBuilder().addLimitObjectids(pageTwo).build());
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(any(IdAndVersion.class),
				any(Exception.class));
		verify(mockIndexManager).setIndexVersion(idAndVersion, 101L);
		verify(mockTableManagerSupport, times(1)).updateChangedOnIfAvailable(idAndVersion);
		verify(mockIndexManager, times(2)).getOutOfDateRowsForView(idAndVersion, filter, pageSize);
	}
	
	/**
	 * Any failure should change change the table's status to failed.
	 */
	@Test
	public void testApplyChangesToAvailableViewHoldingLock_Failed() {
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(viewSchema);
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		
		Set<Long> pageOne = Sets.newHashSet(101L,102L);
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.AVAILABLE));
		when(mockIndexManager.getOutOfDateRowsForView(any(), any(), anyLong())).thenReturn(pageOne);
		
		HierarchicaFilter filter = new HierarchicaFilter(ReplicationType.ENTITY, Sets.newHashSet(SubType.file),
				allContainersInScope);
		when(mockMetadataIndexProvider.getViewFilter(any())).thenReturn(filter);
		
		
		// setup a failure
		IllegalArgumentException exception = new IllegalArgumentException("Something is wrong...");
		doThrow(exception).when(mockIndexManager).updateViewRowsInTransaction(any(), any(), anyList(), any());
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.applyChangesToAvailableView(idAndVersion, pageSize);
		});
		verify(mockIndexManager).updateViewRowsInTransaction(indexDescription, scopeType, viewSchema,
				filter.newBuilder().addLimitObjectids(pageOne).build());
		// should fail and change the table status.
		verify(mockTableManagerSupport).attemptToSetTableStatusToFailed(idAndVersion, exception);
		verify(mockIndexManager, times(1)).getOutOfDateRowsForView(idAndVersion, filter, pageSize);
	}
	
	@Test
	public void testApplyChangesToAvailableViewHoldingLockWithRecoverableMessageException() {
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(viewSchema);
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		
		Set<Long> pageOne = Sets.newHashSet(101L,102L);
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(indexDescription);
		when(mockTableManagerSupport.getTableStatusState(idAndVersion)).thenReturn(Optional.of(TableState.AVAILABLE));
		when(mockIndexManager.getOutOfDateRowsForView(any(), any(), anyLong())).thenReturn(pageOne);
		
		HierarchicaFilter filter = new HierarchicaFilter(ReplicationType.ENTITY, Sets.newHashSet(SubType.file),
				allContainersInScope);
		when(mockMetadataIndexProvider.getViewFilter(any())).thenReturn(filter);
		
		
		// setup a failure
		RecoverableMessageException exception = new RecoverableMessageException("Something is wrong...");
		doThrow(exception).when(mockIndexManager).updateViewRowsInTransaction(any(), any(), anyList(), any());
		assertThrows(RecoverableMessageException.class, ()->{
			// call under test
			manager.applyChangesToAvailableView(idAndVersion, pageSize);
		});
		verify(mockIndexManager).updateViewRowsInTransaction(indexDescription, scopeType, viewSchema,
				filter.newBuilder().addLimitObjectids(pageOne).build());
		// should not change the table status and will be retried later
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(any(), any());
		verify(mockIndexManager, times(1)).getOutOfDateRowsForView(idAndVersion, filter, pageSize);
	}
	
	@Test
	public void testApplyChangesToAvailableViewOrSnapshotWithoutVersion() {
		idAndVersion = IdAndVersion.parse("syn123");
		
		when(mockConnectionFactory.connectToTableIndex(idAndVersion)).thenReturn(mockIndexManager);
		when(mockTableManagerSupport.getViewScopeType(idAndVersion)).thenReturn(scopeType);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(viewSchema);
		when(mockMetadataIndexProviderFactory.getMetadataIndexProvider(any())).thenReturn(mockMetadataIndexProvider);
		when(mockMetadataIndexProvider.getViewFilter(any())).thenReturn(mockFilter);
		
		// call under test
		managerSpy.applyChangesToAvailableViewOrSnapshot(idAndVersion);
		verify(managerSpy).applyChangesToAvailableView(idAndVersion, TableViewManagerImpl.MAX_ROWS_PER_TRANSACTION);
		verify(managerSpy, never()).refreshBenefactorsForViewSnapshot(any());
	}
	
	@Test
	public void testApplyChangesToAvailableViewOrSnapshotWithVersion() {
		when(mockConnectionFactory.connectToTableIndex(any())).thenReturn(mockIndexManager);
		idAndVersion = IdAndVersion.parse("syn123.1");
		// call under test
		managerSpy.applyChangesToAvailableViewOrSnapshot(idAndVersion);
		verify(managerSpy, never()).applyChangesToAvailableView(any(), anyLong());
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
}
