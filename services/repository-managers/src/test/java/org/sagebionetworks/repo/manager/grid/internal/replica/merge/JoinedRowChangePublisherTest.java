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
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateRowChange;
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
			new JoinedRow(List.of(new ConValue(ConType.LONG, 1), new ConValue(ConType.LONG, 2), new ConValue(ConType.LONG, 3)), new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(1L)),
			new JoinedRow(List.of(new ConValue(ConType.LONG, 2), new ConValue(ConType.LONG, 3), new ConValue(ConType.LONG, 4)), new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(2L)),
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
			if (row.getGridRowVecId() != null) {
				return new UpdateRowChange(row.getGridRowVecId(), row.getCsvData(), vecIndex);
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
		
		JoinedRow insertOne = new JoinedRow(List.of(new ConValue(ConType.LONG, 9), new ConValue(ConType.STRING, "active"), new ConValue(ConType.LONG, 1)), null);
		JoinedRow update = new JoinedRow(List.of(new ConValue(ConType.LONG, 8), new ConValue(ConType.STRING, "unknown"), new ConValue(ConType.LONG, 2)), new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(1L));
		JoinedRow insertTwo = new JoinedRow(List.of(new ConValue(ConType.LONG, 7), new ConValue(ConType.STRING, "unknown"), new ConValue(ConType.LONG, 3)), null);
		JoinedRow insertThree = new JoinedRow(List.of(new ConValue(ConType.LONG, 6), new ConValue(ConType.STRING, "pending"), new ConValue(ConType.LONG, 4)), null);
		
		ValidationResults valid = new ValidationResults().setIsValid(true);
		ValidationResults invalid = new ValidationResults().setIsValid(false).setValidationErrorMessage("#/b: unknown is not a valid enum value")
			.setAllValidationMessages(List.of("#/b: unknown is not a valid enum value"));
		
		when(mockGridRowValidator.validateBatch(mockSchema, List.of(
			new JsonObjectSubject(new JSONObject().put("a", 9).put("b", "active").put("c", 1)),
			new JsonObjectSubject(new JSONObject().put("a", 7).put("b", "unknown").put("c", 3))
		))).thenReturn(List.of(valid, invalid));
		
		when(mockGridRowValidator.validateBatch(mockSchema, List.of(
			new JsonObjectSubject(new JSONObject().put("a", 6).put("b", "pending").put("c", 4))
		))).thenReturn(List.of(valid));
		
		// Call under test
		GridCsvImportResponse result = changePublisher.processJoinedRows(header, mockConnInfo, List.of(insertOne, update, insertTwo, insertThree).iterator(), mapping, Optional.of(mockSchema));
		
		assertEquals(new GridCsvImportResponse()
			.setSessionId(header.getSessionId())
			.setTotalCount(4L)
			.setUpdatedCount(1L)
			.setCreatedCount(3L), 
			result
		);
		
		// The update is published as read and never validated; the inserts are published once their batch is
		// validated, in the order they were read.
		List<IntendedChange> changes = List.of(
			new UpdateRowChange(update.getGridRowVecId(), update.getCsvData(), vecIndex),
			new InsertRowChange(header.getRowsId(), mockTimestamp, insertOne.getCsvData(), vecIndex)
				.setValidationState(EntityFactory.createJSONObjectForEntity(valid)),
			new InsertRowChange(header.getRowsId(), mockTimestamp, insertTwo.getCsvData(), vecIndex)
				.setValidationState(EntityFactory.createJSONObjectForEntity(invalid)),
			new InsertRowChange(header.getRowsId(), mockTimestamp, insertThree.getCsvData(), vecIndex)
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
}