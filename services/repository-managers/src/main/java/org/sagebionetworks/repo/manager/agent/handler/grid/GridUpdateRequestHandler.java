package org.sagebionetworks.repo.manager.agent.handler.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.manager.agent.handler.HttpCode;
import org.sagebionetworks.repo.manager.agent.handler.HttpMethod;
import org.sagebionetworks.repo.manager.agent.handler.OpenApiReturnControlHandler;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlEvent;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.PatchUtils;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.PatchBuilderPublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.QueryElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.FilterElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.FilterTranslation;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.update.GridUpdateResponse;
import org.sagebionetworks.repo.model.grid.update.SetValue;
import org.sagebionetworks.repo.model.grid.update.Update;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class GridUpdateRequestHandler implements OpenApiReturnControlHandler {

	private static final Logger log = LogManager.getLogger(GridUpdateRequestHandler.class);

	private final GridManager gridManager;
	private final GridReplicaViewManager gridViewManager;
	private final PatchBuilderPublisher patchBuilderPublisher;
	private final SetValueProcessorFactory factory;

	public GridUpdateRequestHandler(GridManager gridManager, GridReplicaViewManager gridViewManager,
			PatchBuilderPublisher patchBuilderPublisher, SetValueProcessorFactory factory) {
		this.gridManager = gridManager;
		this.gridViewManager = gridViewManager;
		this.patchBuilderPublisher = patchBuilderPublisher;
		this.factory = factory;
	}

	@Override
	public String getActionGroup() {
		return "org_sage_grid_one";
	}

	@Override
	public boolean needsWriteAccess() {
		return false;
	}

	@Override
	public String handleEvent(ReturnControlEvent event) throws Exception {
		JSONObject updateRequestRaw = extractRequest(event);
		GridAgentSessionContext context = getSessionContext(event);
		GridConnectionInfo internalConnection = getInternalConnection(context);
		GridHeader header = getGridHeader(context, internalConnection);
		GridConnectionInfo agentConnection = getAgentConnection(context);

		JSONArray updateBatch = updateRequestRaw.getJSONObject("update").getJSONArray("batch");
		List<Long> updateCounts = new ArrayList<>();
		for (int i = 0; i < updateBatch.length(); i++) {
			updateCounts.add(executeUpdate(header, agentConnection, updateBatch.getJSONObject(i)));
		}
		return buildResponseJSON(updateCounts);
	}

	long executeUpdate(GridHeader header, GridConnectionInfo agentConnection, JSONObject updateObject)
			throws Exception {
		Update update = extractUpdate(updateObject);
		JSONArray rawSetValueArray = updateObject.getJSONArray("set");
		List<SetValue> set = update.getSet();
		List<FilterElement> filters = getFilters(update);
		Integer[] indexArray = createIndexArray(set, header);

		long updateCount = 0;
		Iterator<RowView> rows = gridViewManager.getQueryIterator(header,
				new QueryElement().setWhere(filters).setLimit(update.getLimit()));

		try (IntendedChangePublisher icp = newIntendedChangePublisher(agentConnection, header.getClockSequenceMaximum(),
				patchBuilderPublisher)) {
			while (rows.hasNext()) {
				Optional<IntendedChange> change = buildChange(rows.next(), set, rawSetValueArray, indexArray);
				if (change.isPresent()) {
					icp.publish(change.get());
					updateCount++;
				}
			}
		}
		return updateCount;
	}
	
	Optional<IntendedChange> buildChange(RowView row, List<SetValue> set, JSONArray rawSetValueArray,
			Integer[] indexArray) {
		List<ConValue> updates = new ArrayList<>();
		List<Integer> finalIndex = new ArrayList<>();
		for (int i = 0; i < set.size(); i++) {
			SetValue sv = set.get(i);
			JSONObject rawSetValue = rawSetValueArray.optJSONObject(i);
			Optional<ConValue> op = factory.createConValue(row, sv, rawSetValue);
			if (op.isPresent()) {
				finalIndex.add(indexArray[i]);
				updates.add(op.get());
			}
		}
		if (updates.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new UpdateRowChange(row.getRowObject().getData().getVectorId(), updates,
				finalIndex.toArray(new Integer[finalIndex.size()])));
	}

	GridAgentSessionContext getSessionContext(ReturnControlEvent event) {
		return event.getSessionContext(GridAgentSessionContext.class)
				.orElseThrow(() -> new IllegalArgumentException("GridAgentSessionContext cannot be null"));
	}

	GridConnectionInfo getInternalConnection(GridAgentSessionContext context) {
		return gridManager.getSingletonConnection(context.getGridSessionId(), EventSource.INTERNAL)
				.orElseThrow(() -> new IllegalArgumentException("Cannot get an internal grid connection."));
	}

	GridHeader getGridHeader(GridAgentSessionContext context, GridConnectionInfo internalConnection) {
		return gridViewManager
				.readHeader(context.getGridSessionId(), internalConnection.getReplicaId(), context.getUsersReplicaId())
				.orElseThrow(() -> new IllegalArgumentException("Cannot read the grid header."));
	}

	GridConnectionInfo getAgentConnection(GridAgentSessionContext context) {
		return gridManager.getConnection(context.getGridSessionId(), context.getAgentsReplicaId()).orElseThrow(
				() -> new IllegalArgumentException("Cannot get an agent grid connection."));
	}

	List<FilterElement> getFilters(Update update) {
		return update.getFilters() == null ? Collections.emptyList()
				: update.getFilters().stream().map(FilterTranslation::translate).collect(Collectors.toList());
	}

	IntendedChangePublisher newIntendedChangePublisher(GridConnectionInfo connInfo, Long maxClockSeq,
			PatchBuilderPublisher publisher) {
		return new IntendedChangePublisher(connInfo, maxClockSeq, publisher, PatchUtils.MAX_CHANGE_SET_SIZE);
	}

	Update extractUpdate(JSONObject updateObject) {
		return JDOSecondaryPropertyUtils.createEntityFromJSONObject(updateObject, Update.class);
	}

	String buildResponseJSON(List<Long> updateCount) {
		String json = JDOSecondaryPropertyUtils
				.createJSONFromObject(new GridUpdateResponse().setUpdateResults(updateCount)
						.setTotalRowsUpdated(updateCount.stream().mapToLong(Long::longValue).sum()));
		log.info("response JSON: {}", json);
		return json;
	}

	JSONObject extractRequest(ReturnControlEvent event) {
		ValidateArgument.required(event, "event");
		String body = event.getRequestBody()
				.orElseThrow(() -> new IllegalArgumentException("Request body cannot be null."));
		log.info("request body: {}", body);
		return new JSONObject(body);
	}

	Integer[] createIndexArray(List<SetValue> set, GridHeader header) {
		ValidateArgument.required(set, "set");
		ValidateArgument.required(header, "header");
		ValidateArgument.required(header.getOrderedColumns(), "header.orderedColumns");
		Map<String, Integer> indexByName = header.getOrderedColumns().stream()
				.collect(Collectors.toMap(Column::getName, Column::getVectorIndex));
		return set.stream().map(s -> {
			Integer idx = indexByName.get(s.getColumnName());
			if (idx == null) {
				throw new IllegalArgumentException("Column name: " + s.getColumnName() + " not found.");
			}
			return idx;
		}).toArray(Integer[]::new);
	}

	@Override
	public String getPath() {
		return "/repo/v1/grid/update";
	}

	@Override
	public HttpMethod getHttpMethod() {
		return HttpMethod.put;
	}

	@Override
	public HttpCode getSuccessHttpCode() {
		return HttpCode.ok;
	}
}
