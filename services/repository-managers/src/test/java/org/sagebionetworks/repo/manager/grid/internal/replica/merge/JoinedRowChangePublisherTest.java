package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.InsertRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangeSet;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.PatchBuilderPublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateMetadataChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.merge.JoinedRow.GridRow;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.GridRowValidator;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.JsonObjectSubject;
import org.sagebionetworks.repo.manager.schema.JsonSubject;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridCsvImportResponse;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

@ExtendWith(MockitoExtension.class)
public class JoinedRowChangePublisherTest {

	@Mock
	private PatchBuilderPublisher mockPatchBuilderPublisher;
	@Mock
	private GridIndexDao mockGridIndexDao;	
	@Mock
	private GridConnectionInfo mockConnInfo;
	@Mock
	private LogicalTimestamp mockTimestamp;
	
	@Mock
	private GridRowValidator mockGridRowValidator;
	@Mock
	private JsonSchema mockSchema;
	
	private JoinedRowChangePublisher changePublisher;
	
	private GridHeader header;
	private Long repId = 123L;
	private List<JoinedRow> rows;
	private ColumnMapping[] mapping;
	private Integer[] vecIndex;

	@BeforeEach
	void setUp() {
		changePublisher = new JoinedRowChangePublisher(mockPatchBuilderPublisher, mockGridIndexDao, mockGridRowValidator);
		
		header = new GridHeader()
			.setSessionId("sessionId")
			.setReplicaId(repId)
			.setClockSequenceMaximum(234L)
			.setRowsId(new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(33L))
			.setOrderedColumns(List.of(
				new Column().setName("b").setVectorIndex(3),
				new Column().setName("a").setVectorIndex(4),
				new Column().setName("c").setVectorIndex(5)
			));
		
		rows = List.of(
			new JoinedRow(List.of(new ConValue(ConType.LONG, 1), new ConValue(ConType.LONG, 2), new ConValue(ConType.LONG, 3)),
				new GridRow(new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(1L), new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(10L), null, "{\"a\":1,\"b\":2,\"c\":3}")),
			new JoinedRow(List.of(new ConValue(ConType.LONG, 2), new ConValue(ConType.LONG, 3), new ConValue(ConType.LONG, 4)),
				new GridRow(new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(2L), new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(20L), null, "{\"a\":2,\"b\":3,\"c\":4}")),
			new JoinedRow(List.of(new ConValue(ConType.LONG, 5), new ConValue(ConType.LONG, 6), new ConValue(ConType.LONG, 7)), null)
		);
		
		mapping = new ColumnMapping[] {
			new ColumnMapping("a", ColumnType.INTEGER, 0, 1, true),
			new ColumnMapping("b", ColumnType.INTEGER, 1, 0, false),
			new ColumnMapping("c", ColumnType.INTEGER, 2, 2, false)
		};
		
		vecIndex = new Integer[] { 4, 3, 5 };
	}

	@Test
	public void testProcessJoinedRows() {
		when(mockGridIndexDao.getArrayLastNodeId(header.getSessionId(), header.getReplicaId(), header.getRowsId()))
			.thenReturn(mockTimestamp);
		
		when(mockConnInfo.getSessionId()).thenReturn(header.getSessionId());
		when(mockConnInfo.getConnectionId()).thenReturn("connId");
		when(mockConnInfo.getReplicaId()).thenReturn(789L);
		
		
		// Call under test
		GridCsvImportResponse result = changePublisher.processJoinedRows(header, mockConnInfo, rows.iterator(), mapping, Optional.empty());
	
		assertEquals(new GridCsvImportResponse()
			.setSessionId(header.getSessionId())
			.setTotalCount(3L)
			.setUpdatedCount(2L)
			.setCreatedCount(1L), 
			result
		);
		
		List<IntendedChange> changes = rows.stream().map(row -> {
			if (row.getGridRow() != null) {
				return new UpdateRowChange(row.getGridRow().vectorId(), row.getCsvData(), vecIndex);
			} else {
				return new InsertRowChange(header.getRowsId(), mockTimestamp, row.getCsvData(), vecIndex);
			}
		}).collect(Collectors.toList());
		
		verify(mockPatchBuilderPublisher).sendChangesToPatchBuilder(new IntendedChangeSet()
			.setSessionId(header.getSessionId())
			.setReplicaId(789L)
			.setConnectionId("connId")
			.setClockSequenceMaximum(header.getClockSequenceMaximum())
			.setChanges(changes)
		);
		verifyNoInteractions(mockGridRowValidator);
	}
	
	@Test
	public void testProcessJoinedRowsWithValidationSchema() throws Exception {
		when(mockGridIndexDao.getArrayLastNodeId(header.getSessionId(), header.getReplicaId(), header.getRowsId()))
			.thenReturn(mockTimestamp);
		
		when(mockConnInfo.getSessionId()).thenReturn(header.getSessionId());
		when(mockConnInfo.getConnectionId()).thenReturn("connId");
		when(mockConnInfo.getReplicaId()).thenReturn(789L);
		
		List<JoinedRow> insertRows = List.of(
			new JoinedRow(List.of(new ConValue(ConType.LONG, 5), new ConValue(ConType.STRING, "active"), new ConValue(ConType.LONG, 7)), null),
			new JoinedRow(List.of(new ConValue(ConType.LONG, 8), new ConValue(ConType.STRING, "unknown"), new ConValue(ConType.UNDEFINED, null)), null)
		);
		
		List<ValidationResults> results = List.of(
			new ValidationResults().setIsValid(true),
			new ValidationResults().setIsValid(false).setValidationErrorMessage("#/b: unknown is not a valid enum value")
				.setAllValidationMessages(List.of("#/b: unknown is not a valid enum value"))
		);
		
		// The undefined cell is omitted from the validated row
		List<JsonSubject> expectedSubjects = List.of(
			new JsonObjectSubject(new JSONObject().put("a", 5).put("b", "active").put("c", 7)),
			new JsonObjectSubject(new JSONObject().put("a", 8).put("b", "unknown"))
		);
		
		when(mockGridRowValidator.validateBatch(mockSchema, expectedSubjects)).thenReturn(results);
		
		// Call under test
		GridCsvImportResponse result = changePublisher.processJoinedRows(header, mockConnInfo, insertRows.iterator(), mapping, Optional.of(mockSchema));
	
		assertEquals(new GridCsvImportResponse()
			.setSessionId(header.getSessionId())
			.setTotalCount(2L)
			.setUpdatedCount(0L)
			.setCreatedCount(2L), 
			result
		);
		
		List<IntendedChange> changes = List.of(
			new InsertRowChange(header.getRowsId(), mockTimestamp, insertRows.get(0).getCsvData(), vecIndex)
				.setValidationState(EntityFactory.createJSONObjectForEntity(results.get(0))),
			new InsertRowChange(header.getRowsId(), mockTimestamp, insertRows.get(1).getCsvData(), vecIndex)
				.setValidationState(EntityFactory.createJSONObjectForEntity(results.get(1)))
		);
		
		verify(mockPatchBuilderPublisher).sendChangesToPatchBuilder(new IntendedChangeSet()
			.setSessionId(header.getSessionId())
			.setReplicaId(789L)
			.setConnectionId("connId")
			.setClockSequenceMaximum(header.getClockSequenceMaximum())
			.setChanges(changes)
		);
	}
	
	@Test
	public void testProcessJoinedRowsWithValidationSchemaAndMixedInsertsAndUpdates() throws Exception {
		changePublisher = new JoinedRowChangePublisher(mockPatchBuilderPublisher, mockGridIndexDao, mockGridRowValidator, 2);
		
		when(mockGridIndexDao.getArrayLastNodeId(header.getSessionId(), header.getReplicaId(), header.getRowsId()))
			.thenReturn(mockTimestamp);
		
		when(mockConnInfo.getSessionId()).thenReturn(header.getSessionId());
		when(mockConnInfo.getConnectionId()).thenReturn("connId");
		when(mockConnInfo.getReplicaId()).thenReturn(789L);
		
		LogicalTimestamp updateOneVecId = new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(1L);
		LogicalTimestamp updateOneObjId = new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(2L);
		LogicalTimestamp updateOneMetaId = new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(3L);
		LogicalTimestamp updateTwoVecId = new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(4L);
		LogicalTimestamp updateTwoObjId = new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(5L);
		
		JoinedRow insertOne = new JoinedRow(List.of(new ConValue(ConType.LONG, 9), new ConValue(ConType.STRING, "active"), new ConValue(ConType.LONG, 1)), null);
		// The grid row has a column ("status") that is not in the CSV, and a value for "c" that the CSV clears
		JoinedRow updateOne = new JoinedRow(List.of(new ConValue(ConType.LONG, 8), new ConValue(ConType.STRING, "unknown"), new ConValue(ConType.UNDEFINED, null)),
			new GridRow(updateOneVecId, updateOneObjId, updateOneMetaId, "{\"a\":8,\"b\":\"active\",\"c\":2,\"status\":\"pending\"}"));
		JoinedRow insertTwo = new JoinedRow(List.of(new ConValue(ConType.LONG, 7), new ConValue(ConType.STRING, "unknown"), new ConValue(ConType.LONG, 3)), null);
		// A grid row without a metadata object
		JoinedRow updateTwo = new JoinedRow(List.of(new ConValue(ConType.LONG, 6), new ConValue(ConType.STRING, "pending"), new ConValue(ConType.LONG, 4)),
			new GridRow(updateTwoVecId, updateTwoObjId, null, "{\"a\":6,\"b\":\"unknown\",\"c\":0,\"status\":\"active\"}"));
		
		ValidationResults valid = new ValidationResults().setIsValid(true);
		ValidationResults invalid = new ValidationResults().setIsValid(false).setValidationErrorMessage("#/b: unknown is not a valid enum value")
			.setAllValidationMessages(List.of("#/b: unknown is not a valid enum value"));
		
		// Each updated row is validated with its grid cells and the CSV cells written over them
		when(mockGridRowValidator.validateBatch(mockSchema, List.of(
			new JsonObjectSubject(new JSONObject().put("a", 9).put("b", "active").put("c", 1)),
			new JsonObjectSubject(new JSONObject().put("a", 8).put("b", "unknown").put("status", "pending"))
		))).thenReturn(List.of(valid, invalid));
		
		when(mockGridRowValidator.validateBatch(mockSchema, List.of(
			new JsonObjectSubject(new JSONObject().put("a", 7).put("b", "unknown").put("c", 3)),
			new JsonObjectSubject(new JSONObject().put("a", 6).put("b", "pending").put("c", 4).put("status", "active"))
		))).thenReturn(List.of(invalid, valid));
		
		// Call under test
		GridCsvImportResponse result = changePublisher.processJoinedRows(header, mockConnInfo, List.of(insertOne, updateOne, insertTwo, updateTwo).iterator(), mapping, Optional.of(mockSchema));
		
		assertEquals(new GridCsvImportResponse()
			.setSessionId(header.getSessionId())
			.setTotalCount(4L)
			.setUpdatedCount(2L)
			.setCreatedCount(2L), 
			result
		);
		
		// Rows are published in the order they were read, each update followed by its validation state
		List<IntendedChange> changes = List.of(
			new InsertRowChange(header.getRowsId(), mockTimestamp, insertOne.getCsvData(), vecIndex)
				.setValidationState(EntityFactory.createJSONObjectForEntity(valid)),
			new UpdateRowChange(updateOneVecId, updateOne.getCsvData(), vecIndex),
			new UpdateMetadataChange().setRowObjectId(updateOneObjId).setRowMetadataId(updateOneMetaId)
				.setValidationState(EntityFactory.createJSONObjectForEntity(invalid)),
			new InsertRowChange(header.getRowsId(), mockTimestamp, insertTwo.getCsvData(), vecIndex)
				.setValidationState(EntityFactory.createJSONObjectForEntity(invalid)),
			new UpdateRowChange(updateTwoVecId, updateTwo.getCsvData(), vecIndex),
			new UpdateMetadataChange().setRowObjectId(updateTwoObjId)
				.setValidationState(EntityFactory.createJSONObjectForEntity(valid))
		);
		
		verify(mockPatchBuilderPublisher).sendChangesToPatchBuilder(new IntendedChangeSet()
			.setSessionId(header.getSessionId())
			.setReplicaId(789L)
			.setConnectionId("connId")
			.setClockSequenceMaximum(header.getClockSequenceMaximum())
			.setChanges(changes)
		);
	}
	
	@Test
	public void testToChangesWithUpdateAndNoRowOrMetadataObject() {
		LogicalTimestamp vecId = new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(1L);
		JoinedRow row = new JoinedRow(rows.get(0).getCsvData(), new GridRow(vecId, null, null, "{\"a\":1}"));
		
		// call under test
		List<IntendedChange> changes = JoinedRowChangePublisher.toChanges(row, new JSONObject().put("isValid", true), header.getRowsId(), mockTimestamp, vecIndex);
		
		// The validation state has nowhere to go, so the validation worker validates the row
		assertEquals(List.of(new UpdateRowChange(vecId, row.getCsvData(), vecIndex)), changes);
	}
	
	@Test
	public void testToChangesWithUpdateAndNoValidationState() {
		LogicalTimestamp vecId = new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(1L);
		LogicalTimestamp objId = new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(2L);
		JoinedRow row = new JoinedRow(rows.get(0).getCsvData(), new GridRow(vecId, objId, null, null));
		
		// call under test
		List<IntendedChange> changes = JoinedRowChangePublisher.toChanges(row, null, header.getRowsId(), mockTimestamp, vecIndex);
		
		assertEquals(List.of(new UpdateRowChange(vecId, row.getCsvData(), vecIndex)), changes);
	}
}