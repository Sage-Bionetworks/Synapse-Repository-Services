package org.sagebionetworks.repo.manager.agent.handler.grid;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlEvent;
import org.sagebionetworks.repo.manager.agent.parameter.Parameter;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.PatchBuilderPublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowData;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.QueryElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.CellValueFilterElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.CellValueOperatorElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.FilterElement;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.query.CellValueFilter;
import org.sagebionetworks.repo.model.grid.query.CellValueOperator;
import org.sagebionetworks.repo.model.grid.query.RowSelectionFilter;
import org.sagebionetworks.repo.model.grid.update.GridUpdateRequest;
import org.sagebionetworks.repo.model.grid.update.LiteralSetValue;
import org.sagebionetworks.repo.model.grid.update.SetValue;
import org.sagebionetworks.repo.model.grid.update.Update;
import org.sagebionetworks.repo.model.grid.update.UpdateBatch;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

@ExtendWith(MockitoExtension.class)
public class GridUpdateRequestHandlerTest {

	@Mock
	private GridManager mockGridManager;
	@Mock
	private GridReplicaViewManager mockGridViewManager;
	@Mock
	private PatchBuilderPublisher mockPatchBuilderPublisher;
	@Mock
	private IntendedChangePublisher mockIntendedChangePublisher;
	@Mock
	private SetValueProcessorFactory mockSetValueProcessorFactory;
	@InjectMocks
	@Spy
	private GridUpdateRequestHandler handler;

	private GridAgentSessionContext agentContext;
	private ReturnControlEvent event;
	private String gridSessionId;
	private Long usersReplicaId;
	private Long agentsReplicaId;
	private GridUpdateRequest updateRequest;
	private JSONObject updateRequestRaw;
	private GridConnectionInfo internalConnection;
	private GridConnectionInfo agentConnection;
	private GridHeader header;

	@BeforeEach
	public void before() {
		gridSessionId = "g123";
		usersReplicaId = 100L;
		agentsReplicaId = 200L;
		agentContext = new GridAgentSessionContext().setGridSessionId(gridSessionId).setUsersReplicaId(usersReplicaId)
				.setAgentsReplicaId(agentsReplicaId);
		event = new ReturnControlEvent(123L, "action", "function", List.<Parameter>of(), null, agentContext);
		updateRequest = new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(
				// one
				new Update().setSet(List.of(new LiteralSetValue().setColumnName("a").setValue(true)))
						.setFilters(List.of(new RowSelectionFilter().setIsSelected(true))).setLimit(10L),
				// two
				new Update().setSet(List.of(new LiteralSetValue().setColumnName("b").setValue(1))).setFilters(
						List.of(new CellValueFilter().setColumnName("b").setOperator(CellValueOperator.IS_UNDEFINED)))
		// end
		)));
		updateRequestRaw = JDOSecondaryPropertyUtils.createJSONObjectForEntity(updateRequest);
		internalConnection = new GridConnectionInfo().setConnectionId("internal");
		agentConnection = new GridConnectionInfo().setConnectionId("agent");
		header = new GridHeader().setClockSequenceMaximum(123L);

	}

	private RowView buildRow(long rep, long seq) {
		RowData data = new RowData().setVectorId(new LogicalTimestamp().setReplicaId(rep).setSequenceNumber(seq));
		RowObject ro = new RowObject().setData(data);
		return new RowView().setRowObject(ro);
	}

	private GridHeader buildHeader(List<Column> cols) {
		return new GridHeader().setOrderedColumns(cols).setClockSequenceMaximum(999L);
	}

	@Test
	public void testHandleEvent() throws Exception {
		doReturn(updateRequestRaw).when(handler).extractRequest(event);
		doReturn(agentContext).when(handler).getSessionContext(event);
		doReturn(internalConnection).when(handler).getInternalConnection(agentContext);
		doReturn(header).when(handler).getGridHeader(agentContext, internalConnection);
		doReturn(agentConnection).when(handler).getAgentConnection(agentContext);

		doReturn(2L).doReturn(3L).when(handler).executeUpdate(eq(header), eq(agentConnection), any(JSONObject.class));

		// call under test
		String result = handler.handleEvent(event);

		assertEquals("{\"updateResults\":[2,3],\"totalRowsUpdated\":5}", result);

		ArgumentCaptor<JSONObject> jsonCaptor = ArgumentCaptor.forClass(JSONObject.class);
		verify(handler, times(2)).executeUpdate(eq(header), eq(agentConnection), jsonCaptor.capture());

		List<JSONObject> capturedJsons = jsonCaptor.getAllValues();
		assertEquals(2, capturedJsons.size());

		assertEquals(updateRequest.getUpdate().getBatch().get(0),
				JDOSecondaryPropertyUtils.createObjectFromJSON(Update.class, capturedJsons.get(0).toString()));
		assertEquals(updateRequest.getUpdate().getBatch().get(1),
				JDOSecondaryPropertyUtils.createObjectFromJSON(Update.class, capturedJsons.get(1).toString()));
	}

	@Test
	public void testExecutUpdate() throws Exception {
		Update update = updateRequest.getUpdate().getBatch().get(0);
		update.setLimit(123L);
		JSONObject updateObj = JDOSecondaryPropertyUtils.createJSONObjectForEntity(update);
		doReturn(update).when(handler).extractUpdate(updateObj);
		doReturn(mockIntendedChangePublisher).when(handler).newIntendedChangePublisher(agentConnection,
				header.getClockSequenceMaximum(), mockPatchBuilderPublisher);
		Integer[] index = new Integer[] { 1, 2 };
		doReturn(index).when(handler).createIndexArray(update.getSet(), header);
		List<RowView> rows = List.of(new RowView().setRowIndex(1L), new RowView().setRowIndex(2L));
		List<FilterElement> filter = handler.getFilters(update);
		when(mockGridViewManager.getQueryIterator(header, new QueryElement().setWhere(filter).setLimit(123L)))
				.thenReturn(rows.iterator());

		IntendedChange one = Mockito.mock(IntendedChange.class);
		IntendedChange two = Mockito.mock(IntendedChange.class);
		doReturn(Optional.of(one)).when(handler).buildChange(eq(rows.get(0)), eq(update.getSet()), any(JSONArray.class), eq(index));
		doReturn(Optional.of(two)).when(handler).buildChange(eq(rows.get(1)), eq(update.getSet()), any(JSONArray.class), eq(index));

		// call under test
		long count = handler.executeUpdate(header, agentConnection, updateObj);
		assertEquals(2L, count);

		ArgumentCaptor<JSONArray> jsonCaptor = ArgumentCaptor.forClass(JSONArray.class);
		verify(handler, times(2)).buildChange(any(), eq(update.getSet()), jsonCaptor.capture(), eq(index));
		String arrayValue = "[{\"concreteType\":\"org.sagebionetworks.repo.model.grid.update.LiteralSetValue\",\"columnName\":\"a\",\"value\":true}]";
		assertEquals(arrayValue, jsonCaptor.getAllValues().get(0).toString());
		assertEquals(arrayValue, jsonCaptor.getAllValues().get(1).toString());
		System.out.println(jsonCaptor.getAllValues().get(0).toString());

		verify(mockIntendedChangePublisher, times(2)).publish(any());
		verify(mockIntendedChangePublisher).publish(one);
		verify(mockIntendedChangePublisher).publish(two);
		verify(mockIntendedChangePublisher).close();

	}
	

	@Test
	public void testExecutUpdateWithOptionalEmpty() throws Exception {
		Update update = updateRequest.getUpdate().getBatch().get(0);
		update.setLimit(123L);
		JSONObject updateObj = JDOSecondaryPropertyUtils.createJSONObjectForEntity(update);
		doReturn(update).when(handler).extractUpdate(updateObj);
		doReturn(mockIntendedChangePublisher).when(handler).newIntendedChangePublisher(agentConnection,
				header.getClockSequenceMaximum(), mockPatchBuilderPublisher);
		Integer[] index = new Integer[] { 1, 2 };
		doReturn(index).when(handler).createIndexArray(update.getSet(), header);
		List<RowView> rows = List.of(new RowView().setRowIndex(1L), new RowView().setRowIndex(2L));
		List<FilterElement> filter = handler.getFilters(update);
		when(mockGridViewManager.getQueryIterator(header, new QueryElement().setWhere(filter).setLimit(123L)))
				.thenReturn(rows.iterator());

		IntendedChange one = Mockito.mock(IntendedChange.class);
		IntendedChange two = Mockito.mock(IntendedChange.class);
		doReturn(Optional.of(one)).when(handler).buildChange(eq(rows.get(0)), eq(update.getSet()), any(JSONArray.class), eq(index));
		doReturn(Optional.empty()).when(handler).buildChange(eq(rows.get(1)), eq(update.getSet()), any(JSONArray.class), eq(index));

		// call under test
		long count = handler.executeUpdate(header, agentConnection, updateObj);
		assertEquals(1L, count);

		ArgumentCaptor<JSONArray> jsonCaptor = ArgumentCaptor.forClass(JSONArray.class);
		verify(handler, times(2)).buildChange(any(), eq(update.getSet()), jsonCaptor.capture(), eq(index));
		String arrayValue = "[{\"concreteType\":\"org.sagebionetworks.repo.model.grid.update.LiteralSetValue\",\"columnName\":\"a\",\"value\":true}]";
		assertEquals(arrayValue, jsonCaptor.getAllValues().get(0).toString());
		assertEquals(arrayValue, jsonCaptor.getAllValues().get(1).toString());
		System.out.println(jsonCaptor.getAllValues().get(0).toString());

		verify(mockIntendedChangePublisher, times(1)).publish(any());
		verify(mockIntendedChangePublisher).publish(one);
		verify(mockIntendedChangePublisher, never()).publish(two);
		verify(mockIntendedChangePublisher).close();

	}

	@Test
	public void testBuildChange() {
		LogicalTimestamp vectorId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L);
		RowView row = new RowView().setRowIndex(1L)
				.setRowObject(new RowObject().setData(new RowData().setVectorId(vectorId)));
		List<SetValue> set = List.of(new LiteralSetValue().setColumnName("a").setValue(123),
				new LiteralSetValue().setColumnName("b").setValue(false));
		JSONArray arraySet = new JSONArray(JDOSecondaryPropertyUtils.writeEntityListToJson(set));
		Integer[] index = new Integer[] { 1, 2 };
		when(mockSetValueProcessorFactory.createConValue(row, set.get(0), arraySet.getJSONObject(0)))
				.thenReturn(Optional.of(new ConValue(ConType.LONG, 123L)));
		when(mockSetValueProcessorFactory.createConValue(row, set.get(1), arraySet.getJSONObject(1)))
				.thenReturn(Optional.of(new ConValue(ConType.BOOLEAN, false)));

		// call under test
		Optional<IntendedChange> change = handler.buildChange(row, set, arraySet, index);
		UpdateRowChange expected = new UpdateRowChange(vectorId,
				List.of(new ConValue(ConType.LONG, 123L), new ConValue(ConType.BOOLEAN, false)), index);
		assertEquals(expected, change.get());
	}
	
	@Test
	public void testBuildChangeWithOneEmpty() {
		LogicalTimestamp vectorId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L);
		RowView row = new RowView().setRowIndex(1L)
				.setRowObject(new RowObject().setData(new RowData().setVectorId(vectorId)));
		List<SetValue> set = List.of(new LiteralSetValue().setColumnName("a").setValue(123),
				new LiteralSetValue().setColumnName("b").setValue(false));
		JSONArray arraySet = new JSONArray(JDOSecondaryPropertyUtils.writeEntityListToJson(set));
		Integer[] index = new Integer[] { 1, 2 };
		when(mockSetValueProcessorFactory.createConValue(row, set.get(0), arraySet.getJSONObject(0)))
				.thenReturn(Optional.of(new ConValue(ConType.LONG, 123L)));
		when(mockSetValueProcessorFactory.createConValue(row, set.get(1), arraySet.getJSONObject(1)))
				.thenReturn(Optional.empty());

		// call under test
		Optional<IntendedChange> change = handler.buildChange(row, set, arraySet, index);
		UpdateRowChange expected = new UpdateRowChange(vectorId, List.of(new ConValue(ConType.LONG, 123L)),
				new Integer[] { 1 });
		assertEquals(expected, change.get());
	}
	
	@Test
	public void testBuildChangeWithOtherEmpty() {
		LogicalTimestamp vectorId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L);
		RowView row = new RowView().setRowIndex(1L)
				.setRowObject(new RowObject().setData(new RowData().setVectorId(vectorId)));
		List<SetValue> set = List.of(new LiteralSetValue().setColumnName("a").setValue(123),
				new LiteralSetValue().setColumnName("b").setValue(false));
		JSONArray arraySet = new JSONArray(JDOSecondaryPropertyUtils.writeEntityListToJson(set));
		Integer[] index = new Integer[] { 1, 4 };
		when(mockSetValueProcessorFactory.createConValue(row, set.get(0), arraySet.getJSONObject(0)))
				.thenReturn(Optional.empty());
		when(mockSetValueProcessorFactory.createConValue(row, set.get(1), arraySet.getJSONObject(1)))
				.thenReturn(Optional.of(new ConValue(ConType.BOOLEAN, false)));

		// call under test
		Optional<IntendedChange> change = handler.buildChange(row, set, arraySet, index);
		UpdateRowChange expected = new UpdateRowChange(vectorId, List.of(new ConValue(ConType.BOOLEAN, false)),
				new Integer[] { 4 });
		assertEquals(expected, change.get());
	}
	
	@Test
	public void testBuildChangeWithAllEmpty() {
		LogicalTimestamp vectorId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L);
		RowView row = new RowView().setRowIndex(1L)
				.setRowObject(new RowObject().setData(new RowData().setVectorId(vectorId)));
		List<SetValue> set = List.of(new LiteralSetValue().setColumnName("a").setValue(123),
				new LiteralSetValue().setColumnName("b").setValue(false));
		JSONArray arraySet = new JSONArray(JDOSecondaryPropertyUtils.writeEntityListToJson(set));
		Integer[] index = new Integer[] { 1, 4 };
		when(mockSetValueProcessorFactory.createConValue(row, set.get(0), arraySet.getJSONObject(0)))
				.thenReturn(Optional.empty());
		when(mockSetValueProcessorFactory.createConValue(row, set.get(1), arraySet.getJSONObject(1)))
				.thenReturn(Optional.empty());

		// call under test
		Optional<IntendedChange> change = handler.buildChange(row, set, arraySet, index);
		assertEquals(Optional.empty(), change);
	}

	@Test
	public void testGetSessionContext() {
		// call under test
		GridAgentSessionContext context = handler.getSessionContext(event);
		assertEquals(agentContext, context);
	}

	@Test
	public void testGetSessionContextWithNoContext() {
		event = new ReturnControlEvent(123L, "action", "function", List.<Parameter>of(), null, null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			handler.getSessionContext(event);
		}).getMessage();

		assertEquals("GridAgentSessionContext cannot be null", message);
	}

	@Test
	public void testGetInternalConnection() {
		when(mockGridManager.getSingletonConnection(gridSessionId, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));

		// call under test
		GridConnectionInfo connection = handler.getInternalConnection(agentContext);
		assertEquals(internalConnection, connection);
	}

	@Test
	public void testGetInternalConnectionWithNoConnection() {
		when(mockGridManager.getSingletonConnection(gridSessionId, EventSource.INTERNAL)).thenReturn(Optional.empty());

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			handler.getInternalConnection(agentContext);
		}).getMessage();

		assertEquals("Cannot get an internal grid connection.", message);
	}

	@Test
	public void testGetAgentConnection() {
		when(mockGridManager.getConnection(gridSessionId, agentsReplicaId)).thenReturn(Optional.of(agentConnection));

		// call under test
		GridConnectionInfo connection = handler.getAgentConnection(agentContext);
		assertEquals(agentConnection, connection);
	}

	@Test
	public void testGetAgentConnectionWithNoConnection() {
		when(mockGridManager.getConnection(gridSessionId, agentsReplicaId)).thenReturn(Optional.empty());

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			handler.getAgentConnection(agentContext);
		}).getMessage();

		assertEquals("Cannot get an agent grid connection.", message);
	}

	@Test
	public void testGetGridHeader() {
		when(mockGridViewManager.readHeader(gridSessionId, internalConnection.getReplicaId(), usersReplicaId))
				.thenReturn(Optional.of(header));

		// call under test
		GridHeader result = handler.getGridHeader(agentContext, internalConnection);
		assertEquals(header, result);
	}

	@Test
	public void testGetGridHeaderWithNoHeader() {
		when(mockGridViewManager.readHeader(gridSessionId, internalConnection.getReplicaId(), usersReplicaId))
				.thenReturn(Optional.empty());

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			handler.getGridHeader(agentContext, internalConnection);
		}).getMessage();

		assertEquals("Cannot read the grid header.", message);
	}

	@Test
	public void testGetFilters() {
		Update update = new Update().setFilters(List.of(new RowSelectionFilter().setIsSelected(true),
				new CellValueFilter().setColumnName("a").setOperator(CellValueOperator.IS_UNDEFINED)));

		// call under test
		List<FilterElement> result = handler.getFilters(update);

		assertEquals(2, result.size());
	}

	@Test
	public void testGetFiltersWithNull() {
		Update update = new Update().setFilters(null);

		// call under test
		List<FilterElement> result = handler.getFilters(update);

		assertEquals(Collections.emptyList(), result);
	}

	@Test
	public void testCreateIndexArray() {
		List<SetValue> set = List.of(new LiteralSetValue().setColumnName("a").setValue("1"),
				new LiteralSetValue().setColumnName("b").setValue(3));
		GridHeader header = new GridHeader().setOrderedColumns(List.of(new Column().setName("a").setVectorIndex(2),
				new Column().setName("c").setVectorIndex(0), new Column().setName("b").setVectorIndex(1)));

		// call under test
		Integer[] results = handler.createIndexArray(set, header);
		assertArrayEquals(new Integer[] { 2, 1 }, results);
	}

	@Test
	public void testCreateIndexArrayWithNullSet() {
		List<SetValue> set = null;
		GridHeader header = new GridHeader().setOrderedColumns(List.of(new Column().setName("a").setVectorIndex(2),
				new Column().setName("c").setVectorIndex(0), new Column().setName("b").setVectorIndex(1)));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> handler.createIndexArray(set, header));
		assertEquals("set is required.", ex.getMessage());
	}

	@Test
	public void testCreateIndexArrayWithNullHeader() {
		List<SetValue> set = List.of(new LiteralSetValue().setColumnName("a").setValue("1"),
				new LiteralSetValue().setColumnName("b").setValue(3));
		GridHeader header = null;
		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> handler.createIndexArray(set, header));
		assertEquals("header is required.", ex.getMessage());
	}

	@Test
	public void testCreateIndexArrayWithHeaderColumnsNull() {
		List<SetValue> set = List.of(new LiteralSetValue().setColumnName("a").setValue("1"),
				new LiteralSetValue().setColumnName("b").setValue(3));
		GridHeader header = new GridHeader().setOrderedColumns(null);
		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> handler.createIndexArray(set, header));
		assertEquals("header.orderedColumns is required.", ex.getMessage());
	}

	@Test
	public void testCreateIndexArrayWithNotFound() {
		List<SetValue> set = List.of(new LiteralSetValue().setColumnName("a").setValue("1"),
				new LiteralSetValue().setColumnName("x").setValue(3));
		GridHeader header = new GridHeader().setOrderedColumns(List.of(new Column().setName("a").setVectorIndex(2),
				new Column().setName("c").setVectorIndex(0), new Column().setName("b").setVectorIndex(1)));
		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> handler.createIndexArray(set, header));
		assertEquals("Column name: x not found.", ex.getMessage());
	}

	@Test
	public void testExtractRequest() {
		GridUpdateRequest expected = new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(
				List.of(new Update().setSet(List.of(new LiteralSetValue().setColumnName("a").setValue(1))))));
		JSONObject rawExpected = JDOSecondaryPropertyUtils.createJSONObjectForEntity(expected);
		JSONObject update = rawExpected.getJSONObject("update");
		event = new ReturnControlEvent(1L, "group", "function", null,
				List.of(new Parameter("update", "object", update.toString())),
				new GridAgentSessionContext().setAgentsReplicaId(123L));
		// call under test
		JSONObject result = handler.extractRequest(event);
		assertTrue(rawExpected.similar(result));
	}

	@Test
	public void testExtractRequestWithJsonArrayValue() {
		GridUpdateRequest expected = new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(new Update()
				.setSet(List.of(new LiteralSetValue().setColumnName("a").setValue(new JSONArray("[1,2,3]")))))));
		JSONObject rawExpected = JDOSecondaryPropertyUtils.createJSONObjectForEntity(expected);
		JSONObject update = rawExpected.getJSONObject("update");
		event = new ReturnControlEvent(1L, "group", "function", null,
				List.of(new Parameter("update", "object", update.toString())),
				new GridAgentSessionContext().setAgentsReplicaId(123L));
		// call under test
		JSONObject result = handler.extractRequest(event);
		assertTrue(rawExpected.similar(result));
	}

	@Test
	public void testExtractRequestWithJsonObjectValue() {
		GridUpdateRequest expected = new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(new Update()
				.setSet(List.of(new LiteralSetValue().setColumnName("a").setValue(new JSONObject("{\"key\":true}")))))));
		JSONObject rawExpected = JDOSecondaryPropertyUtils.createJSONObjectForEntity(expected);
		JSONObject update = rawExpected.getJSONObject("update");
		event = new ReturnControlEvent(1L, "group", "function", null,
				List.of(new Parameter("update", "object", update.toString())),
				new GridAgentSessionContext().setAgentsReplicaId(123L));
		// call under test
		JSONObject result = handler.extractRequest(event);
		assertTrue(rawExpected.similar(result));
	}

	@Test
	public void testExtractRequestWithNullBody() {
		event = new ReturnControlEvent(1L, "group", "function", null, null,
				new GridAgentSessionContext().setAgentsReplicaId(123L));
		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> handler.extractRequest(event));
		assertEquals("Request body cannot be null.", ex.getMessage());
	}

	@Test
	public void testHandleEventWithCellValueFilterNullValue() throws Exception {
		String json = "{\"filters\":[{\"concreteType\":\"org.sagebionetworks.repo.model.grid.query.CellValueFilter\",\"columnName\":\"favoriteFoods\",\"operator\":\"IS_NOT_NULL\"}]}";
		Update update = JDOSecondaryPropertyUtils.createObjectFromJSON(Update.class, json);
		// call under test
		List<FilterElement> filters = handler.getFilters(update);
		assertEquals(List.of(new CellValueFilterElement().setColumnName("favoriteFoods").setOperator(CellValueOperatorElement.IS_NOT_NULL)), filters);
	}
}