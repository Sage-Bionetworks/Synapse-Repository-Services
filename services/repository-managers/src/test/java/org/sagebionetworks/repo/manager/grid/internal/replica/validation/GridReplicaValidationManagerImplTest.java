package org.sagebionetworks.repo.manager.grid.internal.replica.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangeSet;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.PatchBuilderPublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateMetadataChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowData;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowMetadata;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowValidation;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.FilterElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.VectorIdFilterElement;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
public class GridReplicaValidationManagerImplTest {

	@Mock
	private GridReplicaViewManager mockGridReplicaViewManager;
	@Mock
	private GridRowValidator mockGridRowValidator;
	@Mock
	private GridDao mockGridDao;
	@Mock
	private PatchBuilderPublisher mockPatchBuilderPublisher;

	private String sessionId;
	private Long replicaId;
	private List<LogicalTimestamp> changedVectorIds;
	private GridSession gridSession;
	private String schemaId;
	private List<Column> columns;
	private GridHeader gridHeader;
	private RowView row;
	private List<RowView> rows;
	private JsonSchema jsonSchema;
	private ValidationResults validationResult;
	private IntendedChange intendedChange;
	private GridConnectionInfo validationConnection;
	private GridConnectionInfo internalConnection;

	@BeforeEach
	public void before() {
		sessionId = "session123";
		replicaId = 111L;
		changedVectorIds = List.of(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
				new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L));
		schemaId = "some-schema";
		gridSession = new GridSession().setGridJsonSchema$Id(schemaId).setSessionId(sessionId);
		gridHeader = new GridHeader().setReplicaId(replicaId).setSessionId(sessionId).setOrderedColumns(columns);
		row = new RowView().setRowObject(
				new RowObject().setObjectId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L))
						.setData(new RowData().setRowJsonDocument(new JSONObject("{\"key\":\"value\"}")))
						.setMetadata(new RowMetadata().setObjectId(new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L))));
		rows = List.of(new RowView().setArrNodeId(new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L)),
				new RowView().setArrNodeId(new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L)));
		jsonSchema = new JsonSchema().set$id(schemaId);
		validationResult = new ValidationResults().setIsValid(true);
		intendedChange = new UpdateMetadataChange().setRowMetadataId(rows.get(0).getArrNodeId());
		validationConnection = new GridConnectionInfo().setConnectionId("con123").setSessionId(sessionId)
				.setReplicaId(replicaId);
		internalConnection = new GridConnectionInfo().setConnectionId("internalCon456").setSessionId(sessionId)
				.setReplicaId(999L);
	}

	@Spy
	@InjectMocks
	private GridReplicaValidationManagerImpl manager;

	@Test
	public void testValidateChanges() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.VALIDATION))
				.thenReturn(Optional.of(validationConnection));
		when(mockGridReplicaViewManager.readHeader(sessionId, replicaId)).thenReturn(Optional.of(gridHeader));
		List<RowView> rows = List
				.of(new RowView().setArrNodeId(new LogicalTimestamp().setReplicaId(9L).setSequenceNumber(10L)));
		doReturn(rows).when(manager).getRowsToValidate(gridHeader, changedVectorIds);
		List<IntendedChange> changes = List.of(new UpdateMetadataChange()
				.setRowMetadataId(new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L)));
		doReturn(changes).when(manager).validateRows(gridHeader, schemaId, rows);

		// call under test
		manager.validateChanges(sessionId, replicaId, changedVectorIds);

		verify(mockPatchBuilderPublisher).sendChangesToPatchBuilder(
				new IntendedChangeSet().setConnectionId(validationConnection.getConnectionId()).setChanges(changes)
						.setReplicaId(replicaId).setSessionId(sessionId));
	}
	
	@Test
	public void testValidateChangesWithNoConnection() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.VALIDATION))
				.thenReturn(Optional.empty());

		// call under test
		manager.validateChanges(sessionId, replicaId, changedVectorIds);
		verifyNoMoreInteractions(mockGridReplicaViewManager, mockPatchBuilderPublisher);
	}

	@Test
	public void testValidateChangesWithNullChanges() {
		changedVectorIds = null;

		// call under test
		manager.validateChanges(sessionId, replicaId, changedVectorIds);
		verifyNoMoreInteractions(mockPatchBuilderPublisher);
	}

	@Test
	public void testValidateChangesWithEmptyChanges() {
		changedVectorIds = Collections.emptyList();

		// call under test
		manager.validateChanges(sessionId, replicaId, changedVectorIds);
		verifyNoMoreInteractions(mockPatchBuilderPublisher);
	}

	@Test
	public void testValidateChangesWithNoSesion() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.empty());

		// call under test
		manager.validateChanges(sessionId, replicaId, changedVectorIds);
		verifyNoMoreInteractions(mockPatchBuilderPublisher);
	}

	@Test
	public void testValidateChangesWithNullHeader() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.VALIDATION))
				.thenReturn(Optional.of(validationConnection));
		when(mockGridReplicaViewManager.readHeader(sessionId, replicaId)).thenReturn(Optional.empty());

		// call under test
		manager.validateChanges(sessionId, replicaId, changedVectorIds);
		verifyNoMoreInteractions(mockPatchBuilderPublisher);
	}

	@Test
	public void testValidateChangesWithNoRows() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.VALIDATION))
				.thenReturn(Optional.of(validationConnection));
		when(mockGridReplicaViewManager.readHeader(sessionId, replicaId)).thenReturn(Optional.of(gridHeader));
		doReturn(Collections.emptyList()).when(manager).getRowsToValidate(gridHeader, changedVectorIds);

		// call under test
		manager.validateChanges(sessionId, replicaId, changedVectorIds);
		verifyNoMoreInteractions(mockPatchBuilderPublisher);
	}

	@Test
	public void testValidateChangesWithNoChanges() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.VALIDATION))
				.thenReturn(Optional.of(validationConnection));
		when(mockGridReplicaViewManager.readHeader(sessionId, replicaId)).thenReturn(Optional.of(gridHeader));
		doReturn(rows).when(manager).getRowsToValidate(gridHeader, changedVectorIds);
		doReturn(Collections.emptyList()).when(manager).validateRows(gridHeader, schemaId, rows);

		// call under test
		manager.validateChanges(sessionId, replicaId, changedVectorIds);
		verifyNoMoreInteractions(mockPatchBuilderPublisher);
	}

	@Test
	public void testValidateChangesWithNoSchema() {
		gridSession.setGridJsonSchema$Id(null);
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));

		// call under test
		manager.validateChanges(sessionId, replicaId, changedVectorIds);
		verifyNoMoreInteractions(mockPatchBuilderPublisher);
	}

	@Test
	public void testValidateAllRows() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.VALIDATION))
				.thenReturn(Optional.of(validationConnection));
		when(mockGridReplicaViewManager.readHeader(sessionId, replicaId)).thenReturn(Optional.of(gridHeader));

		RowView rowToValidate = new RowView()
				.setArrNodeId(new LogicalTimestamp().setReplicaId(9L).setSequenceNumber(10L));
		Iterator<RowView> rowIterator = List.of(rowToValidate).iterator();
		when(mockGridReplicaViewManager.getQueryIterator(eq(gridHeader), anyList())).thenReturn(rowIterator);

		doReturn(true).when(manager).isDataNewerThanValidationResult(rowToValidate);
		List<IntendedChange> changes = List.of(new UpdateMetadataChange()
				.setRowMetadataId(new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L)));
		doReturn(changes).when(manager).validateRows(eq(gridHeader), eq(schemaId), any());

		// call under test
		manager.validateAllRows(sessionId, replicaId);

		verify(mockPatchBuilderPublisher).sendChangesToPatchBuilder(
				new IntendedChangeSet().setConnectionId(validationConnection.getConnectionId()).setChanges(changes)
						.setReplicaId(replicaId).setSessionId(sessionId));
	}

	@Test
	public void testValidateAllRowsWithNoSession() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.empty());

		// call under test
		manager.validateAllRows(sessionId, replicaId);
		verifyNoMoreInteractions(mockPatchBuilderPublisher, mockGridReplicaViewManager);
	}

	@Test
	public void testValidateAllRowsWithNoSchema() {
		gridSession.setGridJsonSchema$Id(null);
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));

		// call under test
		manager.validateAllRows(sessionId, replicaId);
		verifyNoMoreInteractions(mockPatchBuilderPublisher, mockGridReplicaViewManager);
	}

	@Test
	public void testValidateAllRowsWithNoConnection() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.VALIDATION))
				.thenReturn(Optional.empty());

		// call under test
		manager.validateAllRows(sessionId, replicaId);
		verifyNoMoreInteractions(mockGridReplicaViewManager, mockPatchBuilderPublisher);
	}

	@Test
	public void testValidateAllRowsWithNullHeader() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.VALIDATION))
				.thenReturn(Optional.of(validationConnection));
		when(mockGridReplicaViewManager.readHeader(sessionId, replicaId)).thenReturn(Optional.empty());

		// call under test
		manager.validateAllRows(sessionId, replicaId);
		verifyNoMoreInteractions(mockPatchBuilderPublisher);
	}

	@Test
	public void testValidateAllRowsWithEmptyRows() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.VALIDATION))
				.thenReturn(Optional.of(validationConnection));
		when(mockGridReplicaViewManager.readHeader(sessionId, replicaId)).thenReturn(Optional.of(gridHeader));

		Iterator<RowView> emptyIterator = Collections.emptyIterator();
		when(mockGridReplicaViewManager.getQueryIterator(eq(gridHeader), anyList())).thenReturn(emptyIterator);

		// call under test
		manager.validateAllRows(sessionId, replicaId);
		verifyNoMoreInteractions(mockPatchBuilderPublisher);
	}

	@Test
	public void testValidateAllRowsFiltersByDataNewerThanValidation() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.VALIDATION))
				.thenReturn(Optional.of(validationConnection));
		when(mockGridReplicaViewManager.readHeader(sessionId, replicaId)).thenReturn(Optional.of(gridHeader));

		RowView newerRow = new RowView()
				.setArrNodeId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L));
		RowView olderRow = new RowView()
				.setArrNodeId(new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(2L));
		Iterator<RowView> rowIterator = List.of(newerRow, olderRow).iterator();
		when(mockGridReplicaViewManager.getQueryIterator(eq(gridHeader), anyList())).thenReturn(rowIterator);

		doReturn(true).when(manager).isDataNewerThanValidationResult(newerRow);
		doReturn(false).when(manager).isDataNewerThanValidationResult(olderRow);

		List<IntendedChange> changes = List.of(new UpdateMetadataChange()
				.setRowMetadataId(newerRow.getArrNodeId()));
		doReturn(changes).when(manager).validateRows(eq(gridHeader), eq(schemaId), eq(List.of(newerRow)));

		// call under test
		manager.validateAllRows(sessionId, replicaId);

		verify(mockPatchBuilderPublisher).sendChangesToPatchBuilder(
				new IntendedChangeSet().setConnectionId(validationConnection.getConnectionId()).setChanges(changes)
						.setReplicaId(replicaId).setSessionId(sessionId));
	}

	@Test
	public void testValidateAllRowsSkipsBatchWhenAllRowsFiltered() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.VALIDATION))
				.thenReturn(Optional.of(validationConnection));
		when(mockGridReplicaViewManager.readHeader(sessionId, replicaId)).thenReturn(Optional.of(gridHeader));

		RowView olderRow = new RowView()
				.setArrNodeId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L));
		Iterator<RowView> rowIterator = List.of(olderRow).iterator();
		when(mockGridReplicaViewManager.getQueryIterator(eq(gridHeader), anyList())).thenReturn(rowIterator);

		doReturn(false).when(manager).isDataNewerThanValidationResult(olderRow);

		// call under test
		manager.validateAllRows(sessionId, replicaId);

		verifyNoMoreInteractions(mockPatchBuilderPublisher, mockGridRowValidator);
	}

	@Test
	public void testGetRowsToValidate() {
		// call under test
		manager.getRowsToValidate(gridHeader, changedVectorIds);
		List<FilterElement> filter = List.of(new VectorIdFilterElement(changedVectorIds));
		verify(mockGridReplicaViewManager).querySinglePage(gridHeader, filter, 3L, 0L);
	}

	@Test
	public void testValidateRows() {
		when(mockGridRowValidator.getValidationSchema(schemaId)).thenReturn(jsonSchema);
		rows.get(0).setRowObject(new RowObject()
				.setData(new RowData().setRowJsonDocument(new JSONObject("{\"key\":\"value1\"}"))));
		// Sets an existing and equal validation result for the second row
		rows.get(1).setRowObject(new RowObject()
						.setData(new RowData().setRowJsonDocument(new JSONObject("{\"key\":\"value2\"}")))
				.setMetadata(
				new RowMetadata().setRowValidation(new RowValidation().setValidationResults(validationResult))));
		IntendedChange intendedChange2 = new UpdateMetadataChange().setRowMetadataId(rows.get(1).getArrNodeId());

		when(mockGridRowValidator.validateBatch(jsonSchema,
				List.of(new JsonObjectSubject(rows.get(0).getRowObject().getData().getRowJsonDocument()),
						new JsonObjectSubject(rows.get(1).getRowObject().getData().getRowJsonDocument())
				)))
				.thenReturn(List.of(validationResult, validationResult));

		doReturn(intendedChange).when(manager).createChange(rows.get(0), validationResult);
		doReturn(intendedChange2).when(manager).createChange(rows.get(1), validationResult);

		// call under test
		List<IntendedChange> changes = manager.validateRows(gridHeader, schemaId, rows);

		assertEquals(List.of(intendedChange, intendedChange2), changes);
	}

	@Test
	public void testCleanupValidation() {
		validationResult.setSchema$id(schemaId);
		validationResult.setValidatedOn(new Date());
		// call under test — cleanup lives on GridRowValidator
		GridRowValidator.cleanupValidationResults(validationResult);
		assertNull(validationResult.getSchema$id());
		assertNull(validationResult.getValidatedOn());
	}

	@Test
	public void testCreateChange() throws JSONObjectAdapterException {
		// call under test
		IntendedChange change = manager.createChange(row, validationResult);
		IntendedChange expected = new UpdateMetadataChange().setRowObjectId(row.getRowObject().getObjectId())
				.setRowMetadataId(row.getRowMetadata().getObjectId())
				.setValidationState(EntityFactory.createJSONObjectForEntity(validationResult));
		assertEquals(change, expected);
	}

	@Test
	public void testCreateChangeWithNullMeta() throws JSONObjectAdapterException {
		row.getRowObject().setMetadata(null);
		// call under test
		IntendedChange change = manager.createChange(row, validationResult);
		IntendedChange expected = new UpdateMetadataChange().setRowObjectId(row.getRowObject().getObjectId())
				.setRowMetadataId(null).setValidationState(EntityFactory.createJSONObjectForEntity(validationResult));
		assertEquals(change, expected);
	}

	@Test
	public void testCreateChangeNullObject() throws JSONObjectAdapterException {
		row.setRowObject(null);
		// call under test
		IntendedChange change = manager.createChange(row, validationResult);
		IntendedChange expected = new UpdateMetadataChange()
				.setValidationState(EntityFactory.createJSONObjectForEntity(validationResult));
		assertEquals(change, expected);
	}

	@Test
	public void testIsDataNewerThanValidationResultWithNullMetadata() {
		RowView rowView = new RowView().setRowObject(new RowObject().setData(new RowData()));
		rowView.getRowObject().setMetadata(null);

		// call under test
		boolean result = manager.isDataNewerThanValidationResult(rowView);

		assertEquals(true, result);
	}

	@Test
	public void testIsDataNewerThanValidationResultWithNullRowValidation() {
		RowView rowView = new RowView().setRowObject(
				new RowObject()
						.setData(new RowData())
						.setMetadata(new RowMetadata()));

		// call under test
		boolean result = manager.isDataNewerThanValidationResult(rowView);

		assertEquals(true, result);
	}

	@Test
	public void testIsDataNewerThanValidationResultWithNullValidationConstantId() {
		RowView rowView = new RowView().setRowObject(
				new RowObject()
						.setData(new RowData())
						.setMetadata(new RowMetadata().setRowValidation(new RowValidation())));

		// call under test
		boolean result = manager.isDataNewerThanValidationResult(rowView);

		assertEquals(true, result);
	}

	@Test
	public void testIsDataNewerThanValidationResultWithNewerData() {
		LogicalTimestamp validationTimestamp = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(10L);
		LogicalTimestamp newerDataTimestamp = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(20L);

		RowView rowView = new RowView().setRowObject(
				new RowObject()
						.setData(new RowData().setNodes(new ConstantNode[] {
								new org.sagebionetworks.repo.model.grid.node.ConstantNode()
										.setId(newerDataTimestamp) }))
						.setMetadata(new RowMetadata()
								.setRowValidation(new RowValidation().setConstantId(validationTimestamp))));

		// call under test
		boolean result = manager.isDataNewerThanValidationResult(rowView);

		assertEquals(true, result);
	}

	@Test
	public void testIsDataNewerThanValidationResultWithOlderData() {
		LogicalTimestamp validationTimestamp = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(20L);
		LogicalTimestamp olderDataTimestamp = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(10L);

		RowView rowView = new RowView().setRowObject(
				new RowObject()
						.setData(new RowData().setNodes(new ConstantNode[] {
								new org.sagebionetworks.repo.model.grid.node.ConstantNode()
										.setId(olderDataTimestamp) }))
						.setMetadata(new RowMetadata()
								.setRowValidation(new RowValidation().setConstantId(validationTimestamp))));

		// call under test
		boolean result = manager.isDataNewerThanValidationResult(rowView);

		assertEquals(false, result);
	}

	@Test
	public void testIsDataNewerThanValidationResultWithNullConstantId() {
		LogicalTimestamp validationTimestamp = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(10L);

		RowView rowView = new RowView().setRowObject(
				new RowObject()
						.setData(new RowData().setNodes(new ConstantNode[] {
								new org.sagebionetworks.repo.model.grid.node.ConstantNode()
										.setId(null) }))
						.setMetadata(new RowMetadata()
								.setRowValidation(new RowValidation().setConstantId(validationTimestamp))));

		// call under test - should return false because null IDs are filtered out
		boolean result = manager.isDataNewerThanValidationResult(rowView);

		assertEquals(false, result);
	}

	@Test
	public void testValidateAfterSchemaChangeWithNoSession() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.empty());

		// call under test
		assertDoesNotThrow(() -> manager.validateAfterSchemaChange(sessionId));
		verifyNoMoreInteractions(mockGridReplicaViewManager, mockPatchBuilderPublisher);
	}

	@Test
	public void testValidateSchemaChangeWithNoAfterSchema() {
		gridSession.setGridJsonSchema$Id(null);
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));

		// call under test
		assertDoesNotThrow(() -> manager.validateAfterSchemaChange(sessionId));
		verifyNoMoreInteractions(mockGridReplicaViewManager, mockPatchBuilderPublisher);
	}

	@Test
	public void testValidateAfterSchemaChangeWithNoInternalConnection() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.INTERNAL)).thenReturn(Optional.empty());

		// call under test
		assertDoesNotThrow(() -> manager.validateAfterSchemaChange(sessionId));
		verifyNoMoreInteractions(mockGridReplicaViewManager, mockPatchBuilderPublisher);
	}

	/**
	 * No VALIDATION replica/connection exists for this session yet (e.g. a
	 * schema was bound after creation, with no schema present at creation time).
	 * The pending bootstrap means this call must be retried once the replica has
	 * connected.
	 */
	@Test
	public void testValidateAfterSchemaChangeWithPendingValidationConnection() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.VALIDATION)).thenReturn(Optional.empty());

		// call under test
		assertThrows(RecoverableMessageException.class, () -> manager.validateAfterSchemaChange(sessionId));
		verifyNoMoreInteractions(mockGridReplicaViewManager, mockPatchBuilderPublisher);
	}

	@Test
	public void testValidateAfterSchemaChangeWithNullHeader() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.VALIDATION)).thenReturn(Optional.of(validationConnection));
		when(mockGridReplicaViewManager.readHeader(sessionId, internalConnection.getReplicaId()))
				.thenReturn(Optional.empty());

		// call under test
		assertDoesNotThrow(() -> manager.validateAfterSchemaChange(sessionId));
		verifyNoMoreInteractions(mockPatchBuilderPublisher);
	}

	@Test
	public void testValidateAfterSchemaChangeWithEmptyRows() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.VALIDATION)).thenReturn(Optional.of(validationConnection));
		when(mockGridReplicaViewManager.readHeader(sessionId, internalConnection.getReplicaId()))
				.thenReturn(Optional.of(gridHeader));
		when(mockGridReplicaViewManager.getQueryIterator(eq(gridHeader), anyList()))
				.thenReturn(Collections.emptyIterator());

		// call under test
		assertDoesNotThrow(() -> manager.validateAfterSchemaChange(sessionId));
		verifyNoMoreInteractions(mockPatchBuilderPublisher);
	}

	/**
	 * The defining behavior of validateSchemaChange: a row whose data is OLDER
	 * than its existing validation result (i.e. one that validateAllRows would
	 * skip via isDataNewerThanValidationResult) is still re-validated, because
	 * the schema itself changed, not the row's data.
	 */
	@Test
	public void testValidateAfterSchemaChangeRevalidatesRowsRegardlessOfStaleness() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.VALIDATION)).thenReturn(Optional.of(validationConnection));
		when(mockGridReplicaViewManager.readHeader(sessionId, internalConnection.getReplicaId()))
				.thenReturn(Optional.of(gridHeader));

		LogicalTimestamp validationTimestamp = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(20L);
		LogicalTimestamp olderDataTimestamp = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(10L);
		RowView notStaleRow = new RowView().setRowObject(new RowObject()
				.setData(new RowData().setRowJsonDocument(new JSONObject("{\"key\":\"value\"}"))
						.setNodes(new ConstantNode[] { new ConstantNode().setId(olderDataTimestamp) }))
				.setMetadata(new RowMetadata().setRowValidation(new RowValidation().setConstantId(validationTimestamp))));
		// sanity check: this row would be skipped by the data-changed filter
        assertFalse(manager.isDataNewerThanValidationResult(notStaleRow));

		Iterator<RowView> rowIterator = List.of(notStaleRow).iterator();
		when(mockGridReplicaViewManager.getQueryIterator(eq(gridHeader), anyList())).thenReturn(rowIterator);

		List<IntendedChange> changes = List
				.of(new UpdateMetadataChange().setRowMetadataId(new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L)));
		doReturn(changes).when(manager).validateRows(eq(gridHeader), eq(schemaId), eq(List.of(notStaleRow)));

		// call under test
		manager.validateAfterSchemaChange(sessionId);

		verify(mockPatchBuilderPublisher).sendChangesToPatchBuilder(
				new IntendedChangeSet().setConnectionId(validationConnection.getConnectionId()).setChanges(changes)
						.setReplicaId(validationConnection.getReplicaId()).setSessionId(sessionId));
	}
}
