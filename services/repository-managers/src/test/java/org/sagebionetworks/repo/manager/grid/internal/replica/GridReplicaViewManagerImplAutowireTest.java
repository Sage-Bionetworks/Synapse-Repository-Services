package org.sagebionetworks.repo.manager.grid.internal.replica;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.grid.db.GridIndexManager;
import org.sagebionetworks.repo.manager.grid.PatchRowHandler;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowData;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowMetadata;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowValidation;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.compact.PatchCompactSerializable;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertArray;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertObject;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertVector;
import org.sagebionetworks.repo.model.grid.patch.operation.NewConstant;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.semver4j.Semver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class GridReplicaViewManagerImplAutowireTest {

	private final Long MAX_ROW_SIZE_BYTES = 1000L;

	@Autowired
	private GridIndexManager gridIndexManger;

	@Autowired
	private GridReplicaViewManager gridViewManager;

	private String sessionId;
	private Long replicaId;
	private List<ColumnModel> schema;
	private List<Row> rows;

	@BeforeEach
	public void before() {
		gridIndexManger.truncateAll();
		sessionId = GridUtils.gridSessionIdAsString(123L);
		replicaId = 111L;
		schema = List.of(new ColumnModel().setName("a").setColumnType(ColumnType.STRING),
				new ColumnModel().setName("b").setColumnType(ColumnType.INTEGER));
		rows = TableModelTestUtils.createRows(schema, 10);
	}

	@Test
	public void testGetGridHeader() throws IOException {

		// call under test
		assertEquals(Optional.empty(), gridViewManager.readHeader(sessionId, replicaId));

		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);
		LogicalTimestamp expectedClock = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(115L);
		assertEquals(List.of(expectedClock), gridIndexManger.getClock(sessionId, replicaId));

		LogicalTimestamp columnOrderArrayId = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(4L);
		LogicalTimestamp columnNamesVecId = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(3L);

		GridHeader expected = new GridHeader().setSessionId(sessionId).setReplicaId(replicaId)
				.setDocumentVersion(new Semver("0.1.0"))
				.setOrderedColumns(List.of(new Column().setName("a").setVectorIndex(0),
						new Column().setName("b").setVectorIndex(1)))
				.setNodeId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(1L))
				.setRowsId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(5L))
				.setColumnOrderArrId(columnOrderArrayId).setColumnNamesVecId(columnNamesVecId);
		// call under test
		assertEquals(Optional.of(expected), gridViewManager.readHeader(sessionId, replicaId));

		// rename column b.
		Patch patch = new Patch()
				.setPatchId(LogicalTimestamp.newIncrement(gridIndexManger.getClock(sessionId, replicaId).get(0), 1));
		NewConstant b2 = patch.addNewOperation(NewConstant.class).setValue(new ConValue(ConType.STRING, "b2"));
		patch.addNewOperation(InsertVector.class).setVectorId(columnNamesVecId).setMap(Map.of(1, b2.getOperationId()));
		gridIndexManger.applyPatch(sessionId, replicaId, patch);

		expected.getOrderedColumns().get(1).setName("b2");
		// call under test
		assertEquals(Optional.of(expected), gridViewManager.readHeader(sessionId, replicaId));

		// insert a new column at zero
		patch = new Patch()
				.setPatchId(LogicalTimestamp.newIncrement(gridIndexManger.getClock(sessionId, replicaId).get(0), 1));
		NewConstant c = patch.addNewOperation(NewConstant.class).setValue(new ConValue(ConType.STRING, "c"));
		NewConstant two = patch.addNewOperation(NewConstant.class).setValue(new ConValue(ConType.LONG, 2L));
		patch.addNewOperation(InsertVector.class).setVectorId(columnNamesVecId).setMap(Map.of(2, c.getOperationId()));
		patch.addNewOperation(InsertArray.class).setArrayId(columnOrderArrayId).setReferenceId(columnOrderArrayId)
				.setElementIds(List.of(two.getOperationId()));
		gridIndexManger.applyPatch(sessionId, replicaId, patch);
		assertEquals(List.of(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(123L)),
				gridIndexManger.getClock(sessionId, replicaId));

		expected.setOrderedColumns(List.of(new Column().setName("c").setVectorIndex(2),
				new Column().setName("a").setVectorIndex(0), new Column().setName("b2").setVectorIndex(1)));
		// call under test
		assertEquals(Optional.of(expected), gridViewManager.readHeader(sessionId, replicaId));

	}

	@Test
	public void testQuerySinglePage() throws IOException {
		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);
		LogicalTimestamp expectedClock = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(115L);
		assertEquals(List.of(expectedClock), gridIndexManger.getClock(sessionId, replicaId));

		Long limit = 2L;
		Long offset = 3L;
		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();
		// call under test
		List<RowView> page = gridViewManager.querySinglePage(header, limit, offset);
		List<RowView> expected = List.of(
				new RowView().setArrNodeId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(54L))
						.setRowIndex(3L)
						.setRowObject(new RowObject()
								.setObjectId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(45L))
								.setData(new RowData().setData(new JSONArray("[\"string3\",103003]")))
								.setMetadata(new RowMetadata()
										.setObjectId(
												new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(50L))
										.setRowValiation(new RowValidation())
										.setSynapseRow(new SynapseRow().setObjectId(new LogicalTimestamp()
												.setReplicaId(replicaId).setSequenceNumber(51L))))),
				new RowView().setArrNodeId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(64L))
						.setRowIndex(4L)
						.setRowObject(new RowObject()
								.setObjectId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(55L))
								.setData(new RowData().setData(new JSONArray("[\"string4\",103004]")))
								.setMetadata(new RowMetadata()
										.setObjectId(
												new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(60L))
										.setRowValiation(new RowValidation())
										.setSynapseRow(new SynapseRow().setObjectId(new LogicalTimestamp()
												.setReplicaId(replicaId).setSequenceNumber(61L))))));
		assertEquals(expected, page);

		// add some metadata
		Patch patch = new Patch()
				.setPatchId(LogicalTimestamp.newIncrement(gridIndexManger.getClock(sessionId, replicaId).get(0), 1));
		SynapseRow toAdd = new SynapseRow().setRowId(111L).setVersionNumber(333L).setEtag("etag88");
		addSynapseMetadataToRow(patch, page.get(1), toAdd);
		gridIndexManger.applyPatch(sessionId, replicaId, patch);

		expected.get(1).getRowObject().getMetadata().getSynapseRow().setRowId(111L).setVersionNumber(333L)
				.setEtag("etag88");
		// call under test
		page = gridViewManager.querySinglePage(header, limit, offset);
		assertEquals(expected, page);

	}

	void addSynapseMetadataToRow(Patch toExtend, RowView row, SynapseRow toAdd) {
		toExtend.addNewOperation(InsertObject.class)
				.setObjectId(row.getRowObject().getMetadata().getSynapseRow().getObjectId()).setMap(Map.of(
						//
						"rowId", toExtend.addNewOperation(NewConstant.class)
								.setValue(new ConValue(ConType.LONG, toAdd.getRowId())).getOperationId()
						//
						, "versionNumber", toExtend.addNewOperation(NewConstant.class)
								.setValue(new ConValue(ConType.LONG, toAdd.getVersionNumber())).getOperationId()
						//
						, "etag", toExtend.addNewOperation(NewConstant.class)
								.setValue(new ConValue(ConType.STRING, toAdd.getEtag())).getOperationId()));

	}

	/**
	 * Helper function to apply the provided rows as a set of patches to the replica
	 * index.
	 * 
	 * @param rows
	 * @param sessionId
	 * @param replicaId
	 * @param schema
	 * @param maxRowSizeBytes
	 * @throws IOException
	 */
	void writeRowsAsPatches(List<Row> rows, String sessionId, Long replicaId, List<ColumnModel> schema,
			Long maxRowSizeBytes) throws IOException {
		try (PatchRowHandler patchRowHandler = new PatchRowHandler((s, pid, body) -> {
			Patch patch = PatchCompactSerializable.deserialize(new JSONArray(body));
			gridIndexManger.applyPatch(sessionId, pid.getReplicaId(), patch);
			return true;
		}, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES)) {
			rows.stream().forEach(r -> {
				patchRowHandler.nextRow(r);
			});
		}
	}

}
