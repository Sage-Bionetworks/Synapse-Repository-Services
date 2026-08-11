package org.sagebionetworks.repo.manager.grid.internal.replica.view;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sagebionetworks.grid.db.GridIndexManager;
import org.sagebionetworks.repo.manager.grid.DocumentConstants;
import org.sagebionetworks.repo.manager.grid.PatchRowHandler;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowData;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowMetadata;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowValidation;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.QueryElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.CellValueFilterElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.CellValueOperatorElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.FilterElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.RowIsValidFilterElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.RowSelectionFilterElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.RowValidationResultFilterElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.VectorIdFilterElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.select.CountStartElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.select.SelectAllElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.select.SelectByNameElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.select.SelectSelectionElement;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.grid.CrdtId;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.grid.ReplicaSelectionModel;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.Timespan;
import org.sagebionetworks.repo.model.grid.patch.compact.PatchCompactSerializable;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.InsertObjectBuilder;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.NewConstantBuilder;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.NewObjectBuilder;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;
import org.sagebionetworks.repo.model.grid.query.CellValueFilter;
import org.sagebionetworks.repo.model.grid.query.CellValueOperator;
import org.sagebionetworks.repo.model.grid.query.Query;
import org.sagebionetworks.repo.model.grid.query.SelectAll;
import org.sagebionetworks.repo.model.grid.query.SelectByName;
import org.sagebionetworks.repo.model.grid.query.ValidationOperator;
import org.sagebionetworks.repo.model.grid.query.result.QueryResult;
import org.sagebionetworks.repo.model.grid.query.result.SelectColumn;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONArrayAdapterImpl;
import org.sagebionetworks.util.ClasspathUtil;
import org.semver4j.Semver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * 
 */
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
		LogicalTimestamp expectedClock = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(85L);
		assertEquals(List.of(expectedClock), gridIndexManger.getClock(sessionId, replicaId));

		LogicalTimestamp columnOrderArrayId = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(4L);
		LogicalTimestamp columnNamesVecId = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(3L);

		GridHeader expected = new GridHeader().setSessionId(sessionId).setReplicaId(replicaId)
				.setDocumentVersion(new Semver("0.1.0"))
				.setOrderedColumns(List.of(
					new Column().setName("a").setVectorIndex(0).setColumnOrderNodeId(new CrdtId().setRep(replicaId).setSeq(13L)),
					new Column().setName("b").setVectorIndex(1).setColumnOrderNodeId(new CrdtId().setRep(replicaId).setSeq(14L))
				))
				.setNodeId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(1L))
				.setRowsId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(5L))
				.setColumnOrderArrId(columnOrderArrayId).setColumnNamesVecId(columnNamesVecId)
				.setClockSequenceMaximum(expectedClock.getSequenceNumber());
		// call under test
		assertEquals(Optional.of(expected), gridViewManager.readHeader(sessionId, replicaId));

		// rename column b.
		Patch patch = new Patch()
				.setPatchId(LogicalTimestamp.newIncrement(gridIndexManger.getClock(sessionId, replicaId).get(0), 1));
		LogicalTimestamp b2Ref = patch
				.addNewOperation(Operations.newConstant().setValue(new ConValue(ConType.STRING, "b2")));
		patch.addNewOperation(Operations.insertVector().setVectorId(columnNamesVecId).setMap(Map.of(1, b2Ref)));
		gridIndexManger.applyPatch(sessionId, replicaId, patch);

		expected.getOrderedColumns().get(1).setName("b2");
		expected.setClockSequenceMaximum(patch.getPatchId().getSequenceNumber() + patch.getSpan());
		// call under test
		assertEquals(Optional.of(expected), gridViewManager.readHeader(sessionId, replicaId));

		// insert a new column at zero
		patch = new Patch()
				.setPatchId(LogicalTimestamp.newIncrement(gridIndexManger.getClock(sessionId, replicaId).get(0), 1));
		LogicalTimestamp cRef = patch
				.addNewOperation(Operations.newConstant().setValue(new ConValue(ConType.STRING, "c")));
		LogicalTimestamp twoRef = patch
				.addNewOperation(Operations.newConstant().setValue(new ConValue(ConType.LONG, 2L)));
		patch.addNewOperation(Operations.insertVector().setVectorId(columnNamesVecId).setMap(Map.of(2, cRef)));
		patch.addNewOperation(Operations.insertArray().setArrayId(columnOrderArrayId).setReferenceId(columnOrderArrayId)
				.setElementIds(List.of(twoRef)));
		gridIndexManger.applyPatch(sessionId, replicaId, patch);
		assertEquals(List.of(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(93L)),
				gridIndexManger.getClock(sessionId, replicaId));

		expected.setOrderedColumns(List.of(
			new Column().setName("c").setVectorIndex(2).setColumnOrderNodeId(new CrdtId().setRep(replicaId).setSeq(92L)),
			new Column().setName("a").setVectorIndex(0).setColumnOrderNodeId(new CrdtId().setRep(replicaId).setSeq(13L)), 
			new Column().setName("b2").setVectorIndex(1).setColumnOrderNodeId(new CrdtId().setRep(replicaId).setSeq(14L))
		));
		expected.setClockSequenceMaximum(patch.getPatchId().getSequenceNumber() + patch.getSpan());
		// call under test
		assertEquals(Optional.of(expected), gridViewManager.readHeader(sessionId, replicaId));

		// add a selection model to the gird
		ReplicaSelectionModel selection = new ReplicaSelectionModel().setRowSelectAll(true).setColumnSelectAll(false);
		Long otherReplica = 765L;
		Long maxSeq = setSelection(expected.getNodeId(), selection, otherReplica);

		expected.setReplicaSelectionModel(selection);
		expected.setClockSequenceMaximum(maxSeq);
		// call under test
		assertEquals(Optional.of(expected), gridViewManager.readHeader(sessionId, replicaId, otherReplica));
		
		// set a different replica to be selected.
		maxSeq = setSelection(expected.getNodeId(), selection, 333L);

		expected.setReplicaSelectionModel(null);
		expected.setClockSequenceMaximum(maxSeq);
		// call under test
		assertEquals(Optional.of(expected), gridViewManager.readHeader(sessionId, replicaId));

	}

	/**
	 * Helper to set the selection model of the grid session.
	 * 
	 * @param rootObjectId
	 * @param selection
	 * @return
	 */
	public Long setSelection(LogicalTimestamp rootObjectId, ReplicaSelectionModel selection, Long otherReplica) {
		Patch patch = new Patch()
				.setPatchId(LogicalTimestamp.newIncrement(gridIndexManger.getClock(sessionId, replicaId).get(0), 1));
		LogicalTimestamp selectionObId = patch.addNewOperation(new NewObjectBuilder());
		LogicalTimestamp selectionConId = patch.addNewOperation(new NewConstantBuilder().setValue(
			new ConValue(ConType.JSON_OBJECT, JDOSecondaryPropertyUtils.createJSONObjectForEntity(selection))));
		patch.addNewOperation(new InsertObjectBuilder().setMap(Map.of(otherReplica.toString(), selectionConId))
				.setObjectId(selectionObId));
		patch.addNewOperation(
				new InsertObjectBuilder().setObjectId(rootObjectId).setMap(Map.of("selection", selectionObId)));
		gridIndexManger.applyPatch(sessionId, replicaId, patch);
		return patch.getPatchId().getSequenceNumber() + patch.getSpan();
	}

	@Test
	public void testQuerySinglePage() throws IOException {
		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);
		LogicalTimestamp expectedClock = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(85L);
		assertEquals(List.of(expectedClock), gridIndexManger.getClock(sessionId, replicaId));

		Long limit = 2L;
		Long offset = 3L;
		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();
		// call under test
		List<RowView> page = gridViewManager.querySinglePage(header, limit, offset);
		List<RowView> expected = List.of(
				new RowView().setArrNodeId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(42L))
						.setRowIndex(3L)
						.setRowObject(new RowObject()
								.setObjectId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(36L))
								.setData(new RowData()
										.setVectorId(
												new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(37L))
										.setNodes(Arrays.asList(
												new ConstantNode().setId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(38L)).setValue(new ConValue(ConType.STRING, "string3")),
												new ConstantNode().setId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(39L)).setValue(new ConValue(ConType.LONG, 103003L))
										))
										.setRowJsonDocument(new JSONObject(Map.of("a", "string3", "b", 103003L))))
								.setMetadata(new RowMetadata().setRowValidation(new RowValidation())
										.setSynapseRow(new SynapseRow()))),
				new RowView().setArrNodeId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(49L))
						.setRowIndex(4L)
						.setRowObject(new RowObject()
								.setObjectId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(43L))
								.setData(new RowData()
										.setVectorId(
												new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(44L))
										.setNodes(Arrays.asList(
												new ConstantNode().setId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(45L)).setValue(new ConValue(ConType.STRING, "string4")),
												new ConstantNode().setId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(46L)).setValue(new ConValue(ConType.LONG, 103004L))
										))
										.setRowJsonDocument(new JSONObject(Map.of("a", "string4", "b", 103004L))))
								.setMetadata(new RowMetadata().setRowValidation(new RowValidation())
										.setSynapseRow(new SynapseRow()))));
		assertEquals(expected, page);

		// Add some metadata

		Patch newPatch = new Patch()
				.setPatchId(LogicalTimestamp.newIncrement(gridIndexManger.getClock(sessionId, replicaId).get(0), 1));


		// Since the row doesn't have any metadata yet we need to create the object
		LogicalTimestamp metadataRef = newPatch.addNewOperation(Operations.newObject());

		LogicalTimestamp synapseRowRef = newPatch.addNewOperation(Operations.newConstant()
			.setValue(new ConValue(ConType.JSON_ARRAY, new JSONArray().put(111L).put(333L).put("etag88"))));
		
		newPatch.addNewOperation(
				Operations.insertObject().setObjectId(metadataRef).setMap(Map.of("synapseRow", synapseRowRef)));

		// We also need to update the row object with the metadata now
		LogicalTimestamp rowObjectRef = expected.get(1).getRowObject().getObjectId();

		newPatch.addNewOperation(
				Operations.insertObject().setObjectId(rowObjectRef).setMap(Map.of("metadata", metadataRef)));

		gridIndexManger.applyPatch(sessionId, replicaId, newPatch);

		expected.get(1).getRowMetadata().setObjectId(metadataRef);
		expected.get(1).getSynapseRow().setConstantId(synapseRowRef).setRowId(111L).setVersionNumber(333L)
				.setEtag("etag88");

		// call under test
		page = gridViewManager.querySinglePage(header, limit, offset);
		assertEquals(expected, page);
	}

	@Test
	public void testQuerySinglePageWithDeletedRows() throws IOException {
		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);
		LogicalTimestamp expectedClock = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(85L);
		assertEquals(List.of(expectedClock), gridIndexManger.getClock(sessionId, replicaId));

		Long limit = 100L;
		Long offset = 0L;

		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();

		List<RowView> allRows = gridViewManager.querySinglePage(header, limit, offset);

		Patch patch = new Patch()
				.setPatchId(LogicalTimestamp.newIncrement(gridIndexManger.getClock(sessionId, replicaId).get(0), 1));

		patch.addNewOperation(Operations.delete().setNodeId(header.getRowsId()).setTimespans(List.of(
				// First three rows
				new Timespan(allRows.get(0).getArrNodeId(),
						allRows.get(2).getArrNodeId().getSequenceNumber()
								- allRows.get(0).getArrNodeId().getSequenceNumber() + 1),
				// 6th row
				new Timespan(allRows.get(5).getArrNodeId(), 1L),
				// Last two rows
				new Timespan(allRows.get(allRows.size() - 2).getArrNodeId(),
						allRows.get(allRows.size() - 1).getArrNodeId().getSequenceNumber()
								- allRows.get(allRows.size() - 2).getArrNodeId().getSequenceNumber() + 1))));

		gridIndexManger.applyPatch(sessionId, replicaId, patch);

		List<RowView> expected = new ArrayList<>();

		expected.addAll(allRows.subList(3, 5));
		expected.addAll(allRows.subList(6, allRows.size() - 2));

		// call under test
		List<RowView> page = gridViewManager.querySinglePage(header, limit, offset);

		assertEquals(expected, page);
	}

	@Test
	public void testQuerySinglePageWithNullOrUndefinedValues() throws IOException {
		writeRowsAsPatches(rows.subList(0, 1), sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);

		Long limit = 100L;
		Long offset = 0L;

		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();

		// Update the first row to include an undefined value and a null value
		Patch newPatch = new Patch()
				.setPatchId(LogicalTimestamp.newIncrement(gridIndexManger.getClock(sessionId, replicaId).get(0), 1));
		LogicalTimestamp undefinedConst = newPatch.addNewOperation(Operations.newConstant().setValue(new ConValue(ConType.UNDEFINED, null)));
		LogicalTimestamp nullConst = newPatch.addNewOperation(Operations.newConstant().setValue(new ConValue(ConType.NULL, null)));
		LogicalTimestamp vectorToUpdate = new LogicalTimestamp().setReplicaId(111L).setSequenceNumber(16L);
		newPatch.addNewOperation(Operations.insertVector().setVectorId(vectorToUpdate)
				.setMap(Map.of(0, undefinedConst, 1, nullConst)));

		gridIndexManger.applyPatch(sessionId, replicaId, newPatch);

		// Call under test -- verify that `null`/`undefined` return expected results
		List<RowView> allRows = gridViewManager.querySinglePage(header, limit, offset);

		assertEquals(allRows.size(), 1);
		// "a" is undefined, so it is omitted from the JSON document
		assertEquals(allRows.get(0).getRowObject().getData().getRowJsonDocument().toString(), "{\"b\":null}");
		assertEquals(allRows.get(0).getRowObject().getCells(), Arrays.asList(new ConValue(ConType.UNDEFINED, null), new ConValue(ConType.NULL, null)));
	}

	@Test
	public void testQuerySinglePageWithFiltersOne() throws IOException {
		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);
		LogicalTimestamp expectedClock = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(85L);
		assertEquals(List.of(expectedClock), gridIndexManger.getClock(sessionId, replicaId));

		Long limit = 100L;
		Long offset = 0L;
		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();

		List<RowView> allRows = gridViewManager.querySinglePage(header, limit, offset);

		List<FilterElement> filters = List
				.of(new VectorIdFilterElement(List.of(allRows.get(1).getRowObject().getData().getVectorId(),
						allRows.get(4).getRowObject().getData().getVectorId())));

		// call under test
		List<RowView> page = gridViewManager.querySinglePage(header, filters, limit, offset);
		List<RowView> expected = List.of(allRows.get(1), allRows.get(4));
		assertEquals(expected, page);
	}

	@Test
	public void testQuerySinglePageWithFiltersMultiple() throws IOException {
		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);
		LogicalTimestamp expectedClock = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(85L);
		assertEquals(List.of(expectedClock), gridIndexManger.getClock(sessionId, replicaId));

		Long limit = 100L;
		Long offset = 0L;
		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();

		List<RowView> allRows = gridViewManager.querySinglePage(header, limit, offset);

		List<FilterElement> filters = List.of(
				// filter 4 rows by their vector id.
				new VectorIdFilterElement(List.of(allRows.get(1).getRowObject().getData().getVectorId(),
						allRows.get(4).getRowObject().getData().getVectorId(),
						allRows.get(5).getRowObject().getData().getVectorId(),
						allRows.get(9).getRowObject().getData().getVectorId())),
				// of those four rows only include rows with a cell value greater than 103004.
				new CellValueFilterElement().setColumnName(header.getOrderedColumns().get(1).getName())
						.setOperator(CellValueOperatorElement.GREATER_THAN).setValue(103004L));

		// call under test
		List<RowView> page = gridViewManager.querySinglePage(header, filters, limit, offset);
		List<RowView> expected = List.of(allRows.get(5), allRows.get(9));
		assertEquals(expected, page);

	}

	@Test
	public void testQuerySinglePageWithRowValidation() throws IOException, JSONObjectAdapterException {
		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);
		LogicalTimestamp expectedClock = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(85L);
		assertEquals(List.of(expectedClock), gridIndexManger.getClock(sessionId, replicaId));

		Long limit = 100L;
		Long offset = 0L;
		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();

		List<RowView> allRows = gridViewManager.querySinglePage(header, limit, offset);

		RowView four = allRows.get(4);
		ValidationResults validation = new ValidationResults().setIsValid(true);
		setValidationResult(four, validation);

		List<FilterElement> filters = List
				.of(new VectorIdFilterElement(List.of(four.getRowObject().getData().getVectorId())));

		// call under test
		List<RowView> page = gridViewManager.querySinglePage(header, filters, limit, offset);
		assertEquals(1, page.size());
		RowView fourUpdated = page.get(0);
		assertEquals(validation, fourUpdated.getRowValidationResults());
	}

	/**
	 * Helper to set the validation results for a row.
	 * 
	 * @param row
	 * @param validation
	 */
	void setValidationResult(RowView row, ValidationResults validation) {
		Patch patch = new Patch()
				.setPatchId(LogicalTimestamp.newIncrement(gridIndexManger.getClock(sessionId, replicaId).get(0), 1));

		JSONObject validationJson = JDOSecondaryPropertyUtils.createJSONObjectForEntity(validation);
		LogicalTimestamp metadataRef = patch.addNewOperation(Operations.newObject());
		LogicalTimestamp rowObjectRef = row.getRowObject().getObjectId();
		patch.addNewOperation(
				Operations.insertObject().setObjectId(rowObjectRef).setMap(Map.of("metadata", metadataRef)));
		LogicalTimestamp conId = patch
				.addNewOperation(Operations.newConstant().setValue(new ConValue(ConType.JSON_OBJECT, validationJson)));
		patch.addNewOperation(
				Operations.insertObject().setObjectId(metadataRef).setMap(Map.of("rowValidation", conId)));
		gridIndexManger.applyPatch(sessionId, replicaId, patch);
	}

	@Test
	public void testFilterWithEachColumnTypeAndEquals() throws IOException {
		schema = TableModelTestUtils.createOneOfEachType();

		rows = TableModelTestUtils.createRows(schema, 10);
		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);

		Long limit = 100L;
		Long offset = 0L;
		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();

		List<RowView> allRows = gridViewManager.querySinglePage(header,
				new QueryElement().setLimit(limit).setOffset(offset));

		RowView rowToFind = allRows.get(4);
		List<RowView> expected = List.of(rowToFind);

		for (int i = 0; i < schema.size(); i++) {
			ColumnModel cm = schema.get(i);
			Object value = rowToFind.getRowObject().getCells().get(i).getValue();
			// call under test
			List<RowView> filtered = gridViewManager.querySinglePage(header,
					new QueryElement()
							.setWhere(List.of(new CellValueFilterElement().setColumnName(cm.getName())
									.setOperator(CellValueOperatorElement.EQUALS).setValue(value)))
							.setLimit(limit).setOffset(offset));
			if (ColumnType.BOOLEAN.equals(cm.getColumnType()) || ColumnType.BOOLEAN_LIST.equals(cm.getColumnType())) {
				// half the the rows match this case.
				assertEquals(5, filtered.size());
			} else {
				assertEquals(expected, filtered, String.format("For: columnName: '%s', type: '%s',  value: %s",
						cm.getName(), cm.getColumnType().name(), value));
			}
		}
	}

	@Test
	public void testFilterWithEachColumnTypeAndIn() throws IOException {
		schema = TableModelTestUtils.createOneOfEachType();

		rows = TableModelTestUtils.createRows(schema, 10);
		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);

		Long limit = 100L;
		Long offset = 0L;
		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();

		List<RowView> allRows = gridViewManager.querySinglePage(header,
				new QueryElement().setLimit(limit).setOffset(offset));

		RowView rowToFindOne = allRows.get(4);
		RowView rowToFindTwo = allRows.get(7);
		List<RowView> expected = List.of(rowToFindOne, rowToFindTwo);

		for (int i = 0; i < schema.size(); i++) {
			ColumnModel cm = schema.get(i);
			Object v1 = rowToFindOne.getRowObject().getCells().get(i).getValue();
			Object v2 = rowToFindTwo.getRowObject().getCells().get(i).getValue();
			// call under test
			List<RowView> filtered = gridViewManager.querySinglePage(header,
					new QueryElement()
							.setWhere(List.of(new CellValueFilterElement().setColumnName(cm.getName())
									.setOperator(CellValueOperatorElement.IN).setValue(List.of(v1, v2))))
							.setLimit(limit).setOffset(offset));
			if (ColumnType.BOOLEAN.equals(cm.getColumnType()) || ColumnType.BOOLEAN_LIST.equals(cm.getColumnType())
					|| ColumnType.STRING_LIST.equals(cm.getColumnType())
					|| ColumnType.INTEGER_LIST.equals(cm.getColumnType())
					|| ColumnType.DATE_LIST.equals(cm.getColumnType())
					|| ColumnType.ENTITYID_LIST.equals(cm.getColumnType())
					|| ColumnType.USERID_LIST.equals(cm.getColumnType())
					|| ColumnType.JSON.equals(cm.getColumnType())) {
				// this does not currently work for these types
				assertEquals(0, filtered.size());
			} else {
				assertEquals(expected, filtered,
						String.format("For: columnName: '%s', type: '%s',  values: ['%s','%s']", cm.getName(),
								cm.getColumnType().name(), v1, v2));
			}
		}
	}

	@Test
	public void testFilterWithEachOperationAndNumeric() throws IOException {
		schema = List.of(new ColumnModel().setName("anInt").setColumnType(ColumnType.INTEGER));

		rows = List.of(
				new Row().setValues(Collections.emptyList()),
				new Row().setValues(List.of("0")),
				new Row().setValues(List.of("1")),
				new Row().setValues(List.of("2")),
				new Row().setValues(List.of("3")));
		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);
		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();

		// Also write a row with a `null` value, which we cannot express via the Row model.
		Patch patch = new Patch().setPatchId(LogicalTimestamp.newIncrement(gridIndexManger.getClock(sessionId, replicaId).get(0), 1));
		LogicalTimestamp objectWithNullRef = patch.addNewOperation(Operations.newObject());
		LogicalTimestamp vecWithNullRef = patch.addNewOperation(Operations.newVector());
		patch.addNewOperation(Operations.insertObject().setObjectId(objectWithNullRef).setMap(Map.of("data", vecWithNullRef)));
		LogicalTimestamp nullConRef = patch.addNewOperation(Operations.newConstant().setValue(new ConValue(ConType.NULL, null)));
		patch.addNewOperation(Operations.insertVector().setVectorId(vecWithNullRef).setMap(Map.of(0, nullConRef)));
		LogicalTimestamp nullRowInsRef = patch.addNewOperation(Operations.insertArray().setArrayId(header.getRowsId()).setReferenceId(new LogicalTimestamp().setReplicaId(111L).setSequenceNumber(39L)).setElementIds(List.of(objectWithNullRef)));

		// Add a row with an `undefined` value
		LogicalTimestamp objectWithUndefinedRef = patch.addNewOperation(Operations.newObject());
		LogicalTimestamp vecWithUndefinedRef = patch.addNewOperation(Operations.newVector());
		patch.addNewOperation(Operations.insertObject().setObjectId(objectWithUndefinedRef).setMap(Map.of("data", vecWithUndefinedRef)));
		LogicalTimestamp undefinedConRef = patch.addNewOperation(Operations.newConstant().setValue(new ConValue(ConType.UNDEFINED, null)));
		patch.addNewOperation(Operations.insertVector().setVectorId(vecWithUndefinedRef).setMap(Map.of(0, undefinedConRef)));
		patch.addNewOperation(Operations.insertArray().setArrayId(header.getRowsId()).setReferenceId(nullRowInsRef).setElementIds(List.of(objectWithUndefinedRef)));

		// Apply patches
		gridIndexManger.applyPatch(sessionId, replicaId, patch);


		List<RowView> allRows = gridViewManager.querySinglePage(header,
				new QueryElement().setLimit(100L).setOffset(0L));
		assertEquals(7, allRows.size());

		RowView rowWithNoCells = allRows.get(0);
		RowView rowWithZero = allRows.get(1);
		RowView rowWithOne = allRows.get(2);
		RowView rowWithTwo = allRows.get(3);
		RowView rowWithThree = allRows.get(4);
		RowView rowWithNull = allRows.get(5);
		RowView rowWithUndefined = allRows.get(6);

		// call under test
		assertEquals(List.of(rowWithZero),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("anInt")
										.setOperator(CellValueOperatorElement.EQUALS).setValue(0L)))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithZero, rowWithNull),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("anInt")
										.setOperator(CellValueOperatorElement.LESS_THAN).setValue(1L)))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithZero, rowWithOne, rowWithNull),
				gridViewManager.querySinglePage(header,
						new QueryElement().setWhere(List.of(new CellValueFilterElement().setColumnName("anInt")
								.setOperator(CellValueOperatorElement.LESS_THAN_OR_EQUALS).setValue(1L)))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithTwo, rowWithThree),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("anInt")
										.setOperator(CellValueOperatorElement.GREATER_THAN).setValue(1L)))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithOne, rowWithTwo, rowWithThree),
				gridViewManager.querySinglePage(header,
						new QueryElement().setWhere(List.of(new CellValueFilterElement().setColumnName("anInt")
								.setOperator(CellValueOperatorElement.GREATER_THAN_OR_EQUALS).setValue(1L)))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithZero, rowWithTwo, rowWithThree, rowWithNull, rowWithUndefined),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("anInt")
										.setOperator(CellValueOperatorElement.NOT_EQUALS).setValue(1L)))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithNull),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("anInt")
										.setOperator(CellValueOperatorElement.IS_NULL)))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithZero, rowWithOne, rowWithTwo, rowWithThree, rowWithUndefined),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("anInt")
										.setOperator(CellValueOperatorElement.IS_NOT_NULL)))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithZero, rowWithThree),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("anInt")
										.setOperator(CellValueOperatorElement.IN).setValue(List.of(0L, 3L))))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithOne, rowWithTwo, rowWithNull, rowWithUndefined),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("anInt")
										.setOperator(CellValueOperatorElement.NOT_IN).setValue(List.of(0L, 3L))))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithUndefined),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("anInt")
										.setOperator(CellValueOperatorElement.IS_UNDEFINED)))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithZero, rowWithOne, rowWithTwo, rowWithThree, rowWithNull),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("anInt")
										.setOperator(CellValueOperatorElement.IS_DEFINED)))
								.setLimit(100L).setOffset(0L)));
	}

	@Test
	public void testFilterWithEachOperationAndString() throws IOException {
		schema = List.of(new ColumnModel().setName("aString").setColumnType(ColumnType.STRING).setMaximumSize(100L));

		rows = List.of(
				new Row().setValues(Collections.emptyList()),
				new Row().setValues(List.of("a")),
				new Row().setValues(List.of("b")),
				new Row().setValues(List.of("c")),
				new Row().setValues(List.of("d")));
		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);
		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();

		// Also write a row with a `null` value, which we cannot express via the Row model.
		Patch patch = new Patch().setPatchId(LogicalTimestamp.newIncrement(gridIndexManger.getClock(sessionId, replicaId).get(0), 1));
		LogicalTimestamp objectWithNullRef = patch.addNewOperation(Operations.newObject());
		LogicalTimestamp vecWithNullRef = patch.addNewOperation(Operations.newVector());
		patch.addNewOperation(Operations.insertObject().setObjectId(objectWithNullRef).setMap(Map.of("data", vecWithNullRef)));
		LogicalTimestamp nullConRef = patch.addNewOperation(Operations.newConstant().setValue(new ConValue(ConType.NULL, null)));
		patch.addNewOperation(Operations.insertVector().setVectorId(vecWithNullRef).setMap(Map.of(0, nullConRef)));
		LogicalTimestamp nullRowInsRef = patch.addNewOperation(Operations.insertArray().setArrayId(header.getRowsId()).setReferenceId(new LogicalTimestamp().setReplicaId(111L).setSequenceNumber(39L)).setElementIds(List.of(objectWithNullRef)));

		// Add a row with an `undefined` value
		LogicalTimestamp objectWithUndefinedRef = patch.addNewOperation(Operations.newObject());
		LogicalTimestamp vecWithUndefinedRef = patch.addNewOperation(Operations.newVector());
		patch.addNewOperation(Operations.insertObject().setObjectId(objectWithUndefinedRef).setMap(Map.of("data", vecWithUndefinedRef)));
		LogicalTimestamp undefinedConRef = patch.addNewOperation(Operations.newConstant().setValue(new ConValue(ConType.UNDEFINED, null)));
		patch.addNewOperation(Operations.insertVector().setVectorId(vecWithUndefinedRef).setMap(Map.of(0, undefinedConRef)));
		patch.addNewOperation(Operations.insertArray().setArrayId(header.getRowsId()).setReferenceId(nullRowInsRef).setElementIds(List.of(objectWithUndefinedRef)));

		// Apply patches
		gridIndexManger.applyPatch(sessionId, replicaId, patch);


		List<RowView> allRows = gridViewManager.querySinglePage(header,
				new QueryElement().setLimit(100L).setOffset(0L));
		assertEquals(7, allRows.size());

		RowView rowWithNoCells = allRows.get(0);
		RowView rowWithA = allRows.get(1);
		RowView rowWithB = allRows.get(2);
		RowView rowWithC = allRows.get(3);
		RowView rowWithD = allRows.get(4);
		RowView rowWithNull = allRows.get(5);
		RowView rowWithUndefined = allRows.get(6);


		// call under test
		assertEquals(List.of(rowWithA),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("aString")
										.setOperator(CellValueOperatorElement.EQUALS).setValue("a")))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithA),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("aString")
										.setOperator(CellValueOperatorElement.LESS_THAN).setValue("b")))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithA, rowWithB),
				gridViewManager.querySinglePage(header,
						new QueryElement().setWhere(List.of(new CellValueFilterElement().setColumnName("aString")
								.setOperator(CellValueOperatorElement.LESS_THAN_OR_EQUALS).setValue("b")))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithC, rowWithD, rowWithNull),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("aString")
										.setOperator(CellValueOperatorElement.GREATER_THAN).setValue("b")))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithB, rowWithC, rowWithD, rowWithNull),
				gridViewManager.querySinglePage(header,
						new QueryElement().setWhere(List.of(new CellValueFilterElement().setColumnName("aString")
								.setOperator(CellValueOperatorElement.GREATER_THAN_OR_EQUALS).setValue("b")))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithA, rowWithC, rowWithD, rowWithNull, rowWithUndefined),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("aString")
										.setOperator(CellValueOperatorElement.NOT_EQUALS).setValue("b")))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithNull),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("aString")
										.setOperator(CellValueOperatorElement.IS_NULL)))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithA, rowWithB, rowWithC, rowWithD, rowWithUndefined),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("aString")
										.setOperator(CellValueOperatorElement.IS_NOT_NULL)))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithA, rowWithC),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("aString")
										.setOperator(CellValueOperatorElement.IN).setValue(List.of("a", "c"))))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithB,rowWithD, rowWithNull, rowWithUndefined),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("aString")
										.setOperator(CellValueOperatorElement.NOT_IN).setValue(List.of("a", "c"))))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithUndefined),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("aString")
										.setOperator(CellValueOperatorElement.IS_UNDEFINED)))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithA, rowWithB, rowWithC, rowWithD, rowWithNull),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("aString")
										.setOperator(CellValueOperatorElement.IS_DEFINED)))
								.setLimit(100L).setOffset(0L)));
	}

	@Test
	public void testFilterWithEachLikeOperation() throws IOException {
		schema = List.of(new ColumnModel().setName("aString").setColumnType(ColumnType.STRING).setMaximumSize(100L));

		rows = List.of(new Row().setValues(Collections.emptyList()), new Row().setValues(List.of("this is one")),
				new Row().setValues(List.of("this is two")), new Row().setValues(Collections.emptyList()),
				new Row().setValues(List.of("three this is")));
		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);
		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();
		List<RowView> allRows = gridViewManager.querySinglePage(header,
				new QueryElement().setLimit(100L).setOffset(0L));

		RowView rowWithNoCells = allRows.get(0);
		RowView rowWithThisIsOne = allRows.get(1);
		RowView rowWithThisIsTwo = allRows.get(2);
		RowView rowWithNoCells2 = allRows.get(3);
		RowView rowWithThreeThisIs = allRows.get(4);

		// call under test
		assertEquals(List.of(rowWithThisIsOne, rowWithThisIsTwo),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("aString")
										.setOperator(CellValueOperatorElement.LIKE).setValue("this is%")))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithThisIsOne, rowWithThisIsTwo, rowWithThreeThisIs),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("aString")
										.setOperator(CellValueOperatorElement.LIKE).setValue("%this%")))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(rowWithThreeThisIs),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new CellValueFilterElement().setColumnName("aString")
										.setOperator(CellValueOperatorElement.NOT_LIKE).setValue("this is%")))
								.setLimit(100L).setOffset(0L)));

	}

	@Test
	public void testFilterValidationResults() throws IOException {
		schema = List.of(new ColumnModel().setName("aString").setColumnType(ColumnType.STRING).setMaximumSize(100L));

		rows = List.of(new Row().setValues(Collections.emptyList()), new Row().setValues(List.of("a")),
				new Row().setValues(List.of("b")), new Row().setValues(List.of("c")),
				new Row().setValues(List.of("d")));
		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);
		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();
		List<RowView> allRows = gridViewManager.querySinglePage(header,
				new QueryElement().setLimit(100L).setOffset(0L));

		setValidationResult(allRows.get(0),
				new ValidationResults().setIsValid(false)
						.setValidationErrorMessage("#: only 1 subschema matches out of 2")
						.setAllValidationMessages(List.of("aString cannot be null", "another value")));
		setValidationResult(allRows.get(1), new ValidationResults().setIsValid(true));
		setValidationResult(allRows.get(2),
				new ValidationResults().setIsValid(false)
						.setValidationErrorMessage("#: only 2 subschema matches out of 3")
						.setAllValidationMessages(List.of("aString cannot be 'b'", "nore can it be c")));
		setValidationResult(allRows.get(3), new ValidationResults().setIsValid(true));
		allRows = gridViewManager.querySinglePage(header, new QueryElement().setLimit(100L).setOffset(0L));

		// call under test
		assertEquals(List.of(allRows.get(1), allRows.get(3)), gridViewManager.querySinglePage(header, new QueryElement()
				.setWhere(List.of(new RowIsValidFilterElement().setValue(true))).setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(allRows.get(0), allRows.get(2)), gridViewManager.querySinglePage(header, new QueryElement()
				.setWhere(List.of(new RowIsValidFilterElement().setValue(false))).setLimit(100L).setOffset(0L)));

		// call under test
		assertEquals(List.of(allRows.get(0), allRows.get(2)), gridViewManager.querySinglePage(header,
				new QueryElement().setWhere(List.of(new RowValidationResultFilterElement()
						.setOperator(ValidationOperator.LIKE).setValidationResultValue("aString cannot be%")))
						.setLimit(100L).setOffset(0L)));

		// call under test
		assertEquals(List.of(allRows.get(0)),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new RowValidationResultFilterElement()
										.setOperator(ValidationOperator.LIKE).setValidationResultValue("%another%")))
								.setLimit(100L).setOffset(0L)));

		// call under test
		assertEquals(List.of(allRows.get(1), allRows.get(2), allRows.get(3), allRows.get(4)),
				gridViewManager.querySinglePage(header,
						new QueryElement().setWhere(List.of(new RowValidationResultFilterElement()
								.setOperator(ValidationOperator.NOT_LIKE).setValidationResultValue("%another%")))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(allRows.get(2)),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new RowValidationResultFilterElement()
										.setOperator(ValidationOperator.LIKE).setValidationResultValue("#: only 2%")))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(List.of(allRows.get(0)),
				gridViewManager.querySinglePage(header,
						new QueryElement()
								.setWhere(List.of(new RowValidationResultFilterElement()
										.setOperator(ValidationOperator.LIKE).setValidationResultValue("#: only 1%")))
								.setLimit(100L).setOffset(0L)));
	}

	@Test
	public void testFilterSelected() throws IOException {
		schema = List.of(new ColumnModel().setName("aString").setColumnType(ColumnType.STRING).setMaximumSize(100L));

		rows = List.of(new Row().setValues(Collections.emptyList()), new Row().setValues(List.of("a")),
				new Row().setValues(List.of("b")), new Row().setValues(List.of("c")),
				new Row().setValues(List.of("d")));
		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);
		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();
		List<RowView> allRows = gridViewManager.querySinglePage(header,
				new QueryElement().setLimit(100L).setOffset(0L));
		// no selection set at this point
		// call under test
		assertEquals(Collections.emptyList(),
				gridViewManager.querySinglePage(header,
						new QueryElement().setWhere(List.of(new RowSelectionFilterElement().setFilterSelected(true)))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(allRows,
				gridViewManager.querySinglePage(header,
						new QueryElement().setWhere(List.of(new RowSelectionFilterElement().setFilterSelected(false)))
								.setLimit(100L).setOffset(0L)));

		ReplicaSelectionModel selection = new ReplicaSelectionModel()
				.setRowSelection(List.of(createCrdtIdFromLogical(allRows.get(1).getArrNodeId()),
						createCrdtIdFromLogical(allRows.get(3).getArrNodeId())));
		Long otherReplica = 987L;
		setSelection(header.getNodeId(), selection, otherReplica);
		header = gridViewManager.readHeader(sessionId, replicaId, otherReplica).get();

		// call under test
		assertEquals(List.of(allRows.get(1), allRows.get(3)),
				gridViewManager.querySinglePage(header,
						new QueryElement().setWhere(List.of(new RowSelectionFilterElement().setFilterSelected(true)))
								.setLimit(100L).setOffset(0L)));

		// call under test
		assertEquals(List.of(allRows.get(0), allRows.get(2), allRows.get(4)),
				gridViewManager.querySinglePage(header,
						new QueryElement().setWhere(List.of(new RowSelectionFilterElement().setFilterSelected(false)))
								.setLimit(100L).setOffset(0L)));

		selection = new ReplicaSelectionModel().setRowSelectAll(true);
		setSelection(header.getNodeId(), selection, otherReplica);
		header = gridViewManager.readHeader(sessionId, replicaId, otherReplica).get();

		assertEquals(allRows,
				gridViewManager.querySinglePage(header,
						new QueryElement().setWhere(List.of(new RowSelectionFilterElement().setFilterSelected(true)))
								.setLimit(100L).setOffset(0L)));

		assertEquals(Collections.emptyList(),
				gridViewManager.querySinglePage(header,
						new QueryElement().setWhere(List.of(new RowSelectionFilterElement().setFilterSelected(false)))
								.setLimit(100L).setOffset(0L)));

		// null selection
		header.setReplicaSelectionModel(new ReplicaSelectionModel());
		// call under test
		assertEquals(Collections.emptyList(),
				gridViewManager.querySinglePage(header,
						new QueryElement().setWhere(List.of(new RowSelectionFilterElement().setFilterSelected(true)))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(allRows,
				gridViewManager.querySinglePage(header,
						new QueryElement().setWhere(List.of(new RowSelectionFilterElement().setFilterSelected(false)))
								.setLimit(100L).setOffset(0L)));

		// empty selection.
		header.setReplicaSelectionModel(new ReplicaSelectionModel().setRowSelection(Collections.emptyList()));
		// call under test
		assertEquals(Collections.emptyList(),
				gridViewManager.querySinglePage(header,
						new QueryElement().setWhere(List.of(new RowSelectionFilterElement().setFilterSelected(true)))
								.setLimit(100L).setOffset(0L)));
		// call under test
		assertEquals(allRows,
				gridViewManager.querySinglePage(header,
						new QueryElement().setWhere(List.of(new RowSelectionFilterElement().setFilterSelected(false)))
								.setLimit(100L).setOffset(0L)));

	}

	@Test
	public void testQueryWithCount() throws IOException {
		schema = List.of(new ColumnModel().setName("aString").setColumnType(ColumnType.STRING).setMaximumSize(100L));

		rows = List.of(new Row().setValues(Collections.emptyList()), new Row().setValues(List.of("a")),
				new Row().setValues(List.of("b")), new Row().setValues(List.of("c")),
				new Row().setValues(List.of("d")));
		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);
		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();

		// call under test
		List<RowView> r = gridViewManager.querySinglePage(header,
				new QueryElement().setSelect(new CountStartElement()));
		assertEquals(1, r.size());
		assertNull(r.get(0).getRowObject().getData().getCells());
		assertEquals("{\"count\":5}", r.get(0).getRowObject().getData().getRowJsonDocument().toString());
	}
	
	@Test
	public void testQueryWithColumnsSubset() throws IOException {
		schema = List.of(
			new ColumnModel().setName("aString").setColumnType(ColumnType.STRING).setMaximumSize(100L),
			new ColumnModel().setName("anInt").setColumnType(ColumnType.INTEGER)
		);

		rows = List.of(
			new Row().setValues(Arrays.asList(null, null)),
			new Row().setValues(List.of("a", "1")),
			new Row().setValues(List.of("b", "2"))
		);
		
		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);
		
		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();
		
		// call under test		
		assertEquals("Column name not found: invalid", assertThrows(IllegalArgumentException.class, () -> {
			gridViewManager.querySinglePage(header, new QueryElement().setSelect(
				new SelectByNameElement(new SelectByName().setColumnName("invalid"))
			));
		}).getMessage());
		
		List<RowView> allRows = gridViewManager.querySinglePage(header, new QueryElement().setLimit(100L).setOffset(0L));

		// call under test
		List<RowView> subsetResult = gridViewManager.querySinglePage(header, new QueryElement().setSelect(
			new SelectByNameElement(new SelectByName().setColumnName("anInt"))
		));
		assertEquals(allRows.size(), subsetResult.size());
		assertEquals(List.of(new ConValue(ConType.UNDEFINED, null)), subsetResult.get(0).getRowObject().getData().getCells());
		assertEquals("{}", subsetResult.get(0).getRowObject().getData().getRowJsonDocument().toString());
		assertEquals(List.of(new ConValue(ConType.LONG, 1)), subsetResult.get(1).getRowObject().getData().getCells());
		assertEquals("{\"anInt\":1}", subsetResult.get(1).getRowObject().getData().getRowJsonDocument().toString());
		assertEquals(List.of(new ConValue(ConType.LONG, 2)), subsetResult.get(2).getRowObject().getData().getCells());
		assertEquals("{\"anInt\":2}", subsetResult.get(2).getRowObject().getData().getRowJsonDocument().toString());

		// call under test		
		subsetResult = gridViewManager.querySinglePage(header, new QueryElement().setSelect(
			new SelectByNameElement(new SelectByName().setColumnName("aString"))
		));

		assertEquals(List.of(new ConValue(ConType.UNDEFINED, null)), subsetResult.get(0).getRowObject().getData().getCells());
		assertEquals("{}", subsetResult.get(0).getRowObject().getData().getRowJsonDocument().toString());
		assertEquals(List.of(new ConValue(ConType.STRING, "a")), subsetResult.get(1).getRowObject().getData().getCells());
		assertEquals("{\"aString\":\"a\"}", subsetResult.get(1).getRowObject().getData().getRowJsonDocument().toString());
		assertEquals(List.of(new ConValue(ConType.STRING, "b")), subsetResult.get(2).getRowObject().getData().getCells());
		assertEquals("{\"aString\":\"b\"}", subsetResult.get(2).getRowObject().getData().getRowJsonDocument().toString());


		// call under test		
		subsetResult = gridViewManager.querySinglePage(header, new QueryElement().setSelect(
			new SelectByNameElement(new SelectByName().setColumnName("anInt")),
			new SelectByNameElement(new SelectByName().setColumnName("aString"))
		));

		assertEquals(List.of(new ConValue(ConType.UNDEFINED, null), new ConValue(ConType.UNDEFINED, null)), subsetResult.get(0).getRowObject().getData().getCells());
		assertEquals("{}", subsetResult.get(0).getRowObject().getData().getRowJsonDocument().toString());
		assertEquals(List.of(new ConValue(ConType.LONG, 1), new ConValue(ConType.STRING, "a")), subsetResult.get(1).getRowObject().getData().getCells());
		assertEquals("{\"anInt\":1,\"aString\":\"a\"}", subsetResult.get(1).getRowObject().getData().getRowJsonDocument().toString());
		assertEquals(List.of(new ConValue(ConType.LONG, 2), new ConValue(ConType.STRING, "b")), subsetResult.get(2).getRowObject().getData().getCells());
		assertEquals("{\"anInt\":2,\"aString\":\"b\"}", subsetResult.get(2).getRowObject().getData().getRowJsonDocument().toString());
	}
	
	@Test
	public void testQueryWithSelectedColumns() throws IOException {
		schema = List.of(
			new ColumnModel().setName("aString").setColumnType(ColumnType.STRING).setMaximumSize(100L),
			new ColumnModel().setName("anInt").setColumnType(ColumnType.INTEGER)
		);

		rows = List.of(
				new Row().setValues(Arrays.asList(null, null)),
			new Row().setValues(List.of("a", "1")),
			new Row().setValues(List.of("b", "2"))
		);
		
		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);
		
		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).orElseThrow();
		
		List<RowView> allRows = gridViewManager.querySinglePage(header, new QueryElement());


		// call under test
		List<RowView> result = gridViewManager.querySinglePage(header, new QueryElement().setSelect(
			new SelectSelectionElement()
		));

		assertEquals(allRows.size(), result.size());
		// Nothing selected, we expect empty cells
		for (int i = 0; i < allRows.size(); i++) {
			assertEquals(Collections.emptyList(), result.get(i).getRowObject().getData().getCells());
			assertEquals("{}", result.get(i).getRowObject().getData().getRowJsonDocument().toString());
		}

		// Add a "all columns selected" model to the gird
		ReplicaSelectionModel selection = new ReplicaSelectionModel().setColumnSelectAll(true);
		
		Long otherReplica = 987L;
		
		setSelection(header.getNodeId(), selection, otherReplica);
		
		header = gridViewManager.readHeader(sessionId, replicaId, otherReplica).orElseThrow();
		
		// All columns selected, we expect all cells

		// call under test	
		result = gridViewManager.querySinglePage(header, new QueryElement().setSelect(
			new SelectSelectionElement()
		));

		assertEquals(List.of(new ConValue(ConType.UNDEFINED, null), new ConValue(ConType.UNDEFINED, null)), result.get(0).getRowObject().getData().getCells());
		assertEquals("{}", result.get(0).getRowObject().getData().getRowJsonDocument().toString());
		assertEquals(List.of(new ConValue(ConType.STRING, "a"), new ConValue(ConType.LONG, 1)), result.get(1).getRowObject().getData().getCells());
		assertEquals("{\"aString\":\"a\",\"anInt\":1}", result.get(1).getRowObject().getData().getRowJsonDocument().toString());
		assertEquals(List.of(new ConValue(ConType.STRING, "b"), new ConValue(ConType.LONG, 2)), result.get(2).getRowObject().getData().getCells());
		assertEquals("{\"aString\":\"b\",\"anInt\":2}", result.get(2).getRowObject().getData().getRowJsonDocument().toString());

		// Add a "column anInt selected" model to the grid
		selection = new ReplicaSelectionModel().setColumnSelection(List.of(
			header.getOrderedColumns().get(1).getColumnOrderNodeId()
		));
		
		setSelection(header.getNodeId(), selection, otherReplica);
		
		header = gridViewManager.readHeader(sessionId, replicaId, otherReplica).orElseThrow();
		
		// call under test
		result = gridViewManager.querySinglePage(header, new QueryElement().setSelect(
			new SelectSelectionElement()
		));

		// Only the second column selected
		assertEquals(List.of(new ConValue(ConType.UNDEFINED, null)), result.get(0).getRowObject().getData().getCells());
		assertEquals("{}", result.get(0).getRowObject().getData().getRowJsonDocument().toString());
		assertEquals(List.of(new ConValue(ConType.LONG, 1)), result.get(1).getRowObject().getData().getCells());
		assertEquals("{\"anInt\":1}", result.get(1).getRowObject().getData().getRowJsonDocument().toString());
		assertEquals(List.of(new ConValue(ConType.LONG, 2)), result.get(2).getRowObject().getData().getCells());
		assertEquals("{\"anInt\":2}", result.get(2).getRowObject().getData().getRowJsonDocument().toString());

		// Add a "column anInt,aString selected" model to the grid
		selection = new ReplicaSelectionModel().setColumnSelection(List.of(
			header.getOrderedColumns().get(1).getColumnOrderNodeId(), header.getOrderedColumns().get(0).getColumnOrderNodeId()
		));
		
		setSelection(header.getNodeId(), selection, otherReplica);
		
		header = gridViewManager.readHeader(sessionId, replicaId, otherReplica).orElseThrow();

		// call under test
		result = gridViewManager.querySinglePage(header, new QueryElement().setSelect(
				new SelectSelectionElement()
		));

		// both columns selected in reverse order
		assertEquals(List.of(new ConValue(ConType.UNDEFINED, null), new ConValue(ConType.UNDEFINED, null)), result.get(0).getRowObject().getData().getCells());
		assertEquals("{}", result.get(0).getRowObject().getData().getRowJsonDocument().toString());
		assertEquals(List.of(new ConValue(ConType.LONG, 1), new ConValue(ConType.STRING, "a")), result.get(1).getRowObject().getData().getCells());
		assertEquals("{\"anInt\":1,\"aString\":\"a\"}", result.get(1).getRowObject().getData().getRowJsonDocument().toString());
		assertEquals(List.of(new ConValue(ConType.LONG, 2), new ConValue(ConType.STRING, "b")), result.get(2).getRowObject().getData().getCells());
		assertEquals("{\"anInt\":2,\"aString\":\"b\"}", result.get(2).getRowObject().getData().getRowJsonDocument().toString());
	}

	@Test
	public void testQueryAsResult() throws IOException {
		schema = List.of(new ColumnModel().setName("aString").setColumnType(ColumnType.STRING).setMaximumSize(100L),
				new ColumnModel().setName("anInt").setColumnType(ColumnType.INTEGER));

		rows = List.of(new Row().setValues(Collections.emptyList()), new Row().setValues(List.of("a", "123")),
				new Row().setValues(List.of("b", "456")), new Row().setValues(List.of("c", "789")),
				new Row().setValues(List.of("d", "333")));
		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);
		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();
		List<RowView> allRows = gridViewManager.querySinglePage(header,
				new QueryElement().setLimit(100L).setOffset(0L));

		setValidationResult(allRows.get(0), new ValidationResults().setIsValid(false)
				.setValidationErrorMessage("required").setAllValidationMessages(List.of("abc", "efg")));
		setValidationResult(allRows.get(1), new ValidationResults().setIsValid(true));
		setValidationResult(allRows.get(2), new ValidationResults().setIsValid(false).setValidationErrorMessage("naw"));
		setValidationResult(allRows.get(3), new ValidationResults().setIsValid(true));
		allRows = gridViewManager.querySinglePage(header, new QueryElement().setLimit(100L).setOffset(0L));

		// call under test
		QueryResult actual = gridViewManager.querySinglePageAsQueryResult(header, new QueryElement().setSelect(new CountStartElement()));
		assertEquals(List.of(new SelectColumn().setColumnName("count")), actual.getSelectColumns());
		assertEquals(1, actual.getRows().size());
		assertEquals("{\"count\":5}", actual.getRows().get(0).getData().toString());

		List<Object> nullList = new ArrayList<>();
		nullList.add(null);
		nullList.add(null);

		// call under test
		actual = gridViewManager.querySinglePageAsQueryResult(header, new QueryElement().setSelect(new SelectAllElement()));
		assertEquals(List.of(new SelectColumn().setColumnName("aString"), new SelectColumn().setColumnName("anInt")), actual.getSelectColumns());
		assertEquals(5, actual.getRows().size());

		assertEquals("{}", actual.getRows().get(0).getData().toString());
		assertEquals(allRows.get(0).getRowId(), actual.getRows().get(0).getRowId());
		assertEquals(false, actual.getRows().get(0).getValidationResults().getIsValid());

		assertEquals("{\"aString\":\"a\",\"anInt\":123}", actual.getRows().get(1).getData().toString());
		assertEquals(allRows.get(1).getRowId(), actual.getRows().get(1).getRowId());
		assertEquals(true, actual.getRows().get(1).getValidationResults().getIsValid());

		assertEquals("{\"aString\":\"b\",\"anInt\":456}", actual.getRows().get(2).getData().toString());
		assertEquals(allRows.get(2).getRowId(), actual.getRows().get(2).getRowId());
		assertEquals(false, actual.getRows().get(2).getValidationResults().getIsValid());

		assertEquals("{\"aString\":\"c\",\"anInt\":789}", actual.getRows().get(3).getData().toString());
		assertEquals(allRows.get(3).getRowId(), actual.getRows().get(3).getRowId());
		assertEquals(true, actual.getRows().get(3).getValidationResults().getIsValid());

		assertEquals("{\"aString\":\"d\",\"anInt\":333}", actual.getRows().get(4).getData().toString());
		assertEquals(allRows.get(4).getRowId(), actual.getRows().get(4).getRowId());
		assertEquals(null, actual.getRows().get(4).getValidationResults());
	}
	
	
	@Test
	public void testQueryAsResultWithArraysAndObjects() throws IOException {
		schema = List.of(new ColumnModel().setName("anObject").setColumnType(ColumnType.JSON).setMaximumSize(100L),
				new ColumnModel().setName("anArray").setColumnType(ColumnType.INTEGER_LIST));

		rows = List.of(
				//
				new Row().setValues(Collections.emptyList()),
				//
				new Row().setValues(List.of("{\"arr\":[1,2,[4,5]]}", "[6,7]")),
				//
				new Row().setValues(List.of("{\"simple\":true}", "[]")));
		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);

		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();
		QueryResult qr = gridViewManager.querySinglePageAsQueryResult(header, new QueryElement().setLimit(100L).setOffset(0L));
		// The agent will receive a JSON sting of this result.
		String json = JDOSecondaryPropertyUtils.createJSONFromObject(qr);
		JSONObject resultJson = new JSONObject(json);
		JSONArray rows= resultJson.getJSONArray("rows");
		assertEquals(3, rows.length());
		assertEquals("{}", rows.getJSONObject(0).get("data").toString());
		assertEquals("{\"anObject\":{\"arr\":[1,2,[4,5]]},\"anArray\":[6,7]}", rows.getJSONObject(1).get("data").toString());
		assertEquals("{\"anObject\":{\"simple\":true},\"anArray\":[]}", rows.getJSONObject(2).get("data").toString());

	}

	public static CrdtId createCrdtIdFromLogical(LogicalTimestamp timestamp) {
		return new CrdtId().setRep(timestamp.getReplicaId()).setSeq(timestamp.getSequenceNumber());
	}

	/**
	 * This is a test for
	 * <a href=" https://sagebionetworks.jira.com/browse/PLFM-9220">PLFM-9220</a>
	 */
	@Test
	public void testMissingRows() {
		replicaId = 66534L;
		List.of("patches/b6e3e983-e918-48fa-9460-35eec7a6e953.json",
				"patches/d2cc8633-db04-45e1-8ef8-ce6cee4daa59.json",
				"patches/1579ca83-f9bb-45bb-88e9-7cc84aebfa28.json",
				"patches/a41969be-d0a0-4f83-b6db-4ab5eb0ab947.json").stream().map(f -> {
					try {
						return PatchCompactSerializable.deserialize(new JSONArray(ClasspathUtil.loadFromClasspath(f)));
					} catch (JSONException | IOException e) {
						throw new RuntimeException(e);
					}
				}).forEach(p -> {
					gridIndexManger.applyPatch(sessionId, replicaId, p);
				});

		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();
		List<RowView> page = gridViewManager.querySinglePage(header, List.of(), 100L, 0L);
		assertEquals(7, page.size());
	}
	
	@ParameterizedTest
	@MethodSource("provideValidationQueries")
	void testQueryWithIncludeValidationMessages(QueryElement query) throws IOException {

		schema = List.of(new ColumnModel().setName("aString").setColumnType(ColumnType.STRING).setMaximumSize(100L));

		rows = List.of(new Row().setValues(List.of("a")));
		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);

		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();
		List<RowView> rowViews = gridViewManager.querySinglePage(header, new QueryElement());
		rowViews.forEach((r) -> {
			LogicalTimestamp clock = gridIndexManger.getClock(sessionId, replicaId).get(0);
			writeValidationState(r, clock,
					new ValidationResults().setIsValid(false).setValidationErrorMessage("baseMessage")
							.setAllValidationMessages(List.of("messageOne", "messageTwo")));
		});

		// call under test
		QueryResult results = gridViewManager.querySinglePageAsQueryResult(header, query);
		assertNotNull(results);
		assertNotNull(results.getRows());
		assertEquals(1, results.getRows().size());
		org.sagebionetworks.repo.model.grid.query.result.Row row = results.getRows().get(0);
		assertNotNull(row);
		if (query.getIncludeValidationMessages() == null || query.getIncludeValidationMessages() == false) {
			assertEquals(
					new org.sagebionetworks.repo.model.grid.query.result.ValidationResults().setIsValid(false)
							.setValidationErrorMessage("baseMessage").setAllValidationMessages(null),
					row.getValidationResults());
		} else {
			assertEquals(new org.sagebionetworks.repo.model.grid.query.result.ValidationResults().setIsValid(false)
					.setValidationErrorMessage("baseMessage")
					.setAllValidationMessages(List.of("messageOne", "messageTwo")), row.getValidationResults());
		}
	}

	private static Stream<Arguments> provideValidationQueries() {
		return Stream.of(Arguments.of(new QueryElement().setIncludeValidationMessages(null)),
				Arguments.of(new QueryElement().setIncludeValidationMessages(true)),
				Arguments.of(new QueryElement().setIncludeValidationMessages(false)));
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
		}, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES, Collections.emptyList())) {
			rows.stream().forEach(r -> {
				patchRowHandler.nextRow(r);
			});
		}
	}
	
	void writeValidationState(RowView row, LogicalTimestamp clock, ValidationResults newValidationResults) {
		RowObject rowObject = row.getRowObject();
		RowMetadata rowMetadata = row.getRowMetadata();
		JSONObject validationState;
		try {
			validationState = EntityFactory.createJSONObjectForEntity(newValidationResults);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		LogicalTimestamp rowObjectId = rowObject != null ? rowObject.getObjectId() : null;
		LogicalTimestamp metadataObjectId = rowMetadata != null ? rowMetadata.getObjectId() : null;

		Patch patch = new Patch().setPatchId(LogicalTimestamp.newIncrement(clock, 1));

		if (metadataObjectId == null) {
			metadataObjectId = patch.addNewOperation(Operations.newObject());
			patch.addNewOperation(Operations.insertObject().setObjectId(rowObjectId)
					.setMap(Map.of(DocumentConstants.METADATA, metadataObjectId)));
		}
		LogicalTimestamp conId = patch
				.addNewOperation(Operations.newConstant().setValue(new ConValue(ConType.JSON_OBJECT, validationState)));
		patch.addNewOperation(Operations.insertObject().setObjectId(metadataObjectId)
				.setMap(Map.of(DocumentConstants.ROW_VALIDATION, conId)));

		gridIndexManger.applyPatch(sessionId, patch.getPatchId().getReplicaId(), patch);
	}

	@Test
	public void testQueryWithInOperatorAndArrayValuesFromJSON() throws IOException, JSONObjectAdapterException {
		// Setup: Create rows with string column "a"
		schema = List.of(new ColumnModel().setName("a").setColumnType(ColumnType.STRING).setMaximumSize(100L),
				new ColumnModel().setName("b").setColumnType(ColumnType.INTEGER));
		rows = List.of(
				new Row().setValues(List.of("alpha", "1")),
				new Row().setValues(List.of("beta", "2")),
				new Row().setValues(List.of("gamma", "3")));

		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);
		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();

		// Build a Query object with IN operator and array values wrapped in JSONArrayAdapterImpl
		// This simulates what happens during schema-to-pojo deserialization from a controller
		Query query = new Query()
				.setColumnSelection(List.of(new SelectAll()))
				.setFilters(List.of(new CellValueFilter()
						.setColumnName("a")
						.setOperator(CellValueOperator.IN)
						.setValue(new JSONArrayAdapterImpl(new JSONArray(List.of("alpha", "beta"))))))
				.setLimit(10L)
				.setOffset(0L);

		// call under test - CellValueFilterElement should handle JSONArrayAdapterImpl
		QueryResult result = gridViewManager.querySinglePageAsQueryResult(header,
				new QueryElement(query));

		// Verify the query returned the expected rows
		assertNotNull(result);
		assertEquals(2, result.getRows().size());
		// Rows should be "alpha" and "beta"
		List<String> actualValues = result.getRows().stream()
				.map(r -> {
					try {
						return ((JSONObject) r.getData()).getString("a");
					} catch (JSONException e) {
						throw new RuntimeException(e);
					}
				})
				.collect(java.util.stream.Collectors.toList());
		assertEquals(List.of("alpha", "beta"), actualValues);
	}

	@Test
	public void testQueryWithEqualsOperatorAndArrayValuesFromJSON() throws IOException, JSONObjectAdapterException {
		// Setup: Create rows - one with a JSON array value that matches our query
		schema = List.of(new ColumnModel().setName("a").setColumnType(ColumnType.STRING_LIST).setMaximumSize(100L),
				new ColumnModel().setName("b").setColumnType(ColumnType.INTEGER));

		rows = List.of(
				new Row().setValues(List.of("[\"alpha\",\"beta\"]", "1")),  // This row has the array as its value
				new Row().setValues(List.of("[\"gamma\",\"beta\"]", "2")),          // This row has just "alpha"
				new Row().setValues(List.of("[\"delta\",\"gamma\"]", "3")));

		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);
		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();

		// Build a Query object with EQUALS operator and array values wrapped in JSONArrayAdapterImpl
		// This simulates what happens during schema-to-pojo deserialization from a controller
		Query query = new Query()
				.setColumnSelection(List.of(new SelectAll()))
				.setFilters(List.of(new CellValueFilter()
						.setColumnName("a")
						.setOperator(CellValueOperator.EQUALS)
						.setValue(new JSONArrayAdapterImpl(new JSONArray(List.of("alpha", "beta"))))))
				.setLimit(10L)
				.setOffset(0L);

		// call under test - CellValueFilterElement should handle JSONArrayAdapterImpl
		QueryResult result = gridViewManager.querySinglePageAsQueryResult(header,
				new QueryElement(query));

		// Verify the query returned the expected row (only the one with the array value)
		assertNotNull(result);
		assertEquals(1, result.getRows().size());
		// The row should have the JSON array as its value
		String actualValue = ((JSONObject) result.getRows().get(0).getData()).getString("a");
		assertEquals("[\"alpha\",\"beta\"]", actualValue);
	}

	/**
	 * PLFM-9831/PLFM-9831.2: a filter value is matched strictly by its JSON type. On a scalar column a
	 * scalar value "A" matches the scalar cells, while a single-element array value ["A"] is a genuine
	 * JSON array and therefore matches no scalar cell. This is the half of the reviewer's mixed-column
	 * scenario that proves a wrapped array cannot leak into a scalar cell.
	 */
	@Test
	public void testQueryWithEqualsOperatorAndScalarColumn() throws IOException, JSONObjectAdapterException {
		schema = List.of(new ColumnModel().setName("a").setColumnType(ColumnType.STRING).setMaximumSize(100L),
				new ColumnModel().setName("b").setColumnType(ColumnType.INTEGER));
		// Two rows in each category so a correct EQUALS filter returns exactly the "A" subset.
		rows = List.of(
				new Row().setValues(List.of("A", "1")),
				new Row().setValues(List.of("A", "2")),
				new Row().setValues(List.of("B", "3")),
				new Row().setValues(List.of("B", "4")));

		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);
		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();

		// A scalar value matches the scalar cells.
		Query scalarQuery = new Query()
				.setColumnSelection(List.of(new SelectAll()))
				.setFilters(List.of(new CellValueFilter()
						.setColumnName("a")
						.setOperator(CellValueOperator.EQUALS)
						.setValue("A")))
				.setLimit(10L)
				.setOffset(0L);

		// call under test
		QueryResult scalarResult = gridViewManager.querySinglePageAsQueryResult(header, new QueryElement(scalarQuery));

		assertNotNull(scalarResult);
		assertEquals(2, scalarResult.getRows().size());
		scalarResult.getRows().forEach(r -> {
			try {
				assertEquals("A", ((JSONObject) r.getData()).getString("a"));
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
		});

		// A single-element array value is a JSON array and matches no scalar cell.
		Query arrayQuery = new Query()
				.setColumnSelection(List.of(new SelectAll()))
				.setFilters(List.of(new CellValueFilter()
						.setColumnName("a")
						.setOperator(CellValueOperator.EQUALS)
						.setValue(new JSONArrayAdapterImpl(new JSONArray(List.of("A"))))))
				.setLimit(10L)
				.setOffset(0L);

		// call under test
		QueryResult arrayResult = gridViewManager.querySinglePageAsQueryResult(header, new QueryElement(arrayQuery));

		assertNotNull(arrayResult);
		assertEquals(0, arrayResult.getRows().size());
	}

	/**
	 * PLFM-9831.2: on a LIST column a single-element array value ["A"] matches the single-element list
	 * cells exactly. This is the other half of the reviewer's mixed-column scenario: the same ["A"]
	 * filter that misses scalar cells (see {@link #testQueryWithEqualsOperatorAndScalarColumn()}) does
	 * match a genuine list cell.
	 */
	@Test
	public void testQueryWithEqualsOperatorAndSingleElementArrayOnListColumn()
			throws IOException, JSONObjectAdapterException {
		schema = List.of(new ColumnModel().setName("a").setColumnType(ColumnType.STRING_LIST).setMaximumSize(100L),
				new ColumnModel().setName("b").setColumnType(ColumnType.INTEGER));
		// Two single-element list cells in each category so the ["A"] filter returns exactly that subset.
		rows = List.of(
				new Row().setValues(List.of("[\"A\"]", "1")),
				new Row().setValues(List.of("[\"A\"]", "2")),
				new Row().setValues(List.of("[\"B\"]", "3")),
				new Row().setValues(List.of("[\"B\"]", "4")));

		writeRowsAsPatches(rows, sessionId, replicaId, schema, MAX_ROW_SIZE_BYTES);
		GridHeader header = gridViewManager.readHeader(sessionId, replicaId).get();

		Query query = new Query()
				.setColumnSelection(List.of(new SelectAll()))
				.setFilters(List.of(new CellValueFilter()
						.setColumnName("a")
						.setOperator(CellValueOperator.EQUALS)
						.setValue(new JSONArrayAdapterImpl(new JSONArray(List.of("A"))))))
				.setLimit(10L)
				.setOffset(0L);

		// call under test
		QueryResult result = gridViewManager.querySinglePageAsQueryResult(header, new QueryElement(query));

		assertNotNull(result);
		assertEquals(2, result.getRows().size());
		result.getRows().forEach(r -> {
			try {
				assertEquals("[\"A\"]", ((JSONObject) r.getData()).getString("a"));
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
		});
	}

}
