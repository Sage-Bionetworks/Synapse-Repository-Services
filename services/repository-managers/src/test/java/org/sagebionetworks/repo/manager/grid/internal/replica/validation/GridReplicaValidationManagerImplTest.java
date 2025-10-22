package org.sagebionetworks.repo.manager.grid.internal.replica.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowMetadata;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowValidation;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.FilterElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.VectorIdFilterElement;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaValidationManager;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

@ExtendWith(MockitoExtension.class)
public class GridReplicaValidationManagerImplTest {

	@Mock
	private GridReplicaViewManager mockGridReplicaViewManager;
	@Mock
	private JsonSchemaManager mockJsonSchemaManager;
	@Mock
	private GridDao mockGridDao;
	@Mock
	private JsonSchemaValidationManager mockJsonSchemaValidationManager;
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
				new RowObject().setObjectId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L)).setMetadata(
						new RowMetadata().setObjectId(new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L))));
		rows = List.of(new RowView().setArrNodeId(new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L)),
				new RowView().setArrNodeId(new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L)));
		jsonSchema = new JsonSchema().set$id(schemaId);
		validationResult = new ValidationResults().setIsValid(true);
		intendedChange = new UpdateMetadataChange().setRowMetadataId(rows.get(0).getArrNodeId());
		validationConnection = new GridConnectionInfo().setConnectionId("con123").setSessionId(sessionId)
				.setReplicaId(replicaId);
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
		verifyZeroInteractions(mockGridReplicaViewManager, mockPatchBuilderPublisher);
	}

	@Test
	public void testValidateChangesWithNullChanges() {
		changedVectorIds = null;

		// call under test
		manager.validateChanges(sessionId, replicaId, changedVectorIds);
		verifyZeroInteractions(mockPatchBuilderPublisher);
	}

	@Test
	public void testValidateChangesWithEmptyChanges() {
		changedVectorIds = Collections.emptyList();

		// call under test
		manager.validateChanges(sessionId, replicaId, changedVectorIds);
		verifyZeroInteractions(mockPatchBuilderPublisher);
	}

	@Test
	public void testValidateChangesWithNoSesion() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.empty());

		// call under test
		manager.validateChanges(sessionId, replicaId, changedVectorIds);
		verifyZeroInteractions(mockPatchBuilderPublisher);
	}

	@Test
	public void testValidateChangesWithNullHeader() {
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));
		when(mockGridDao.getSingletonConnection(sessionId, EventSource.VALIDATION))
				.thenReturn(Optional.of(validationConnection));
		when(mockGridReplicaViewManager.readHeader(sessionId, replicaId)).thenReturn(Optional.empty());

		// call under test
		manager.validateChanges(sessionId, replicaId, changedVectorIds);
		verifyZeroInteractions(mockPatchBuilderPublisher);
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
		verifyZeroInteractions(mockPatchBuilderPublisher);
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
		verifyZeroInteractions(mockPatchBuilderPublisher);
	}

	@Test
	public void testValidateChangesWithNoSchema() {
		gridSession.setGridJsonSchema$Id(null);
		when(mockGridDao.getGridSession(sessionId)).thenReturn(Optional.of(gridSession));

		// call under test
		manager.validateChanges(sessionId, replicaId, changedVectorIds);
		verifyZeroInteractions(mockPatchBuilderPublisher);
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
		when(mockJsonSchemaManager.getValidationSchema(schemaId)).thenReturn(jsonSchema);

		// Sets an existing and equal validation result for the second row
		rows.get(1).setRowObject(new RowObject().setMetadata(
				new RowMetadata().setRowValidation(new RowValidation().setValidationResults(validationResult))));

		when(mockJsonSchemaValidationManager.validateBatch(jsonSchema,
				List.of(new RowJsonSubject(columns, rows.get(0)), new RowJsonSubject(columns, rows.get(1)))))
				.thenReturn(List.of(validationResult, validationResult));

		doNothing().when(manager).cleanupValidationResults(validationResult);

		// The change is created only for the first row that does not contain any
		// validation result yet
		doReturn(intendedChange).when(manager).createChange(rows.get(0), validationResult);

		// call under test
		List<IntendedChange> changes = manager.validateRows(gridHeader, schemaId, rows);

		assertEquals(List.of(intendedChange), changes);
	}

	@Test
	public void testCleanupValidation() {
		validationResult.setSchema$id(schemaId);
		validationResult.setValidatedOn(new Date());
		// call under test
		manager.cleanupValidationResults(validationResult);
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
}
