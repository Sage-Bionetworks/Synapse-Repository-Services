package org.sagebionetworks.grid.workers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.java_websocket.WebSocket;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlRequest;
import org.sagebionetworks.repo.model.grid.GridConstants;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessage;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.compact.PatchCompactSerializable;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;
import org.sagebionetworks.repo.service.GridService;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Shared polling and CRDT-patch helpers for grid synchronization integration
 * tests. Wraps a grid session's internal replica view
 * ({@link GridReplicaViewManager}) to wait for expected row state, and drives
 * user-attributed cell edits over the grid's websocket protocol so tests can
 * simulate an in-grid user edit.
 * <p>
 * Spring-managed (registered as a bean in {@code test-context.xml}); autowire
 * this into an integration test rather than constructing it directly.
 */
public class GridIntegrationTestUtils {

	public static final long MAX_WAIT_MS = 1000L * 60 * 2;

	private static final long PAGE_SIZE = 100L;
	private static final long POLL_INTERVAL_MS = 1000L;

	@Autowired
	private GridReplicaViewManager gridViewManager;
	@Autowired
	private GridService gridService;
	@Autowired
	private AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;


	/**
	 * Wait until the internal replica's current page of rows satisfies
	 * {@code predicate}. Returns the matching rows so callers can use them (e.g.
	 * to locate a row's vector id for a subsequent patch).
	 */
	public List<RowView> waitForRows(String sessionId, Long replicaId, Predicate<List<RowView>> predicate)
			throws Exception {
		return TimeUtils.waitFor(MAX_WAIT_MS, POLL_INTERVAL_MS, () -> {
			Optional<GridHeader> header = gridViewManager.readHeader(sessionId, replicaId);
			if (header.isEmpty()) {
				return Pair.create(false, null);
			}
			List<RowView> rows = gridViewManager.querySinglePage(header.get(), PAGE_SIZE, 0L);
			return Pair.create(predicate.test(rows), rows);
		});
	}

	/**
	 * Wait until the internal replica reports exactly {@code expectedCount} rows.
	 */
	public void waitForRowCount(String sessionId, Long replicaId, int expectedCount) throws Exception {
		waitForRows(sessionId, replicaId, rows -> rows.size() == expectedCount);
	}

	/**
	 * Wait until the internal replica's rows match the expected set, keyed by
	 * the "a" column value. Each expected entry maps column name to expected
	 * value; comparison is tolerant of JSON number/string representation.
	 */
	public void waitForRows(String sessionId, Long replicaId, Map<String, Map<String, String>> expectedByKey)
			throws Exception {
		waitForRows(sessionId, replicaId, (List<RowView> rows) -> {
			Map<String, Map<String, String>> actualByKey = new HashMap<>();
			for (RowView row : rows) {
				var doc = row.getRowObject().getData().getRowJsonDocument();
				String key = doc.has("a") ? String.valueOf(doc.get("a")) : null;
				if (key == null) {
					continue;
				}
				Map<String, String> cells = new HashMap<>();
				for (String name : expectedByKey.getOrDefault(key, Map.of()).keySet()) {
					cells.put(name, doc.has(name) ? String.valueOf(doc.get(name)) : null);
				}
				actualByKey.put(key, cells);
			}
			return expectedByKey.equals(actualByKey);
		});
	}

	/**
	 * Wait until the internal replica's rows, rendered as JSON, exactly match
	 * {@code expectedRowJsonStrings} as an unordered set.
	 */
	public List<RowView> waitForRowJsonSet(String sessionId, Long replicaId, Set<String> expectedRowJsonStrings)
			throws Exception {
		return waitForRows(sessionId, replicaId, (List<RowView> rows) -> {
			Set<String> actual = rows.stream().map(r -> r.getRowObject().getData().getRowJsonDocument().toString())
					.collect(Collectors.toSet());
			return expectedRowJsonStrings.equals(actual);
		});
	}

	/**
	 * Apply a user-attributed cell edit to a single grid row (identified by its
	 * "a" key value) over a websocket connection, so the resulting CRDT nodes are
	 * owned by the given user replica.
	 */
	public void applyUserCellEdits(UserInfo admin, String sessionId, Long internalReplicaId, Long userReplicaId,
			String rowKey, Map<String, ConValue> edits) throws Exception {
		String url = gridService.createPresignedUrl(admin.getId(),
				new CreateGridPresignedUrlRequest().setGridSessionId(sessionId).setReplicaId(userReplicaId))
				.getPresignedUrl();
		BlockingQueue<String> incomingMessages = new LinkedBlockingQueue<>();
		WebSocket websocket = asynchronousJobWorkerHelper.createConnection(url, incomingMessages);

		GridHeader header = gridViewManager.readHeader(sessionId, internalReplicaId).get();
		RowView row = getRowByKey(header, rowKey);
		LogicalTimestamp rowVectorId = row.getRowObject().getData().getVectorId();

		Patch patch = new Patch()
				.setPatchId(new LogicalTimestamp().setReplicaId(userReplicaId).setSequenceNumber(101L));
		Map<Integer, LogicalTimestamp> vectorMap = new HashMap<>();
		for (Map.Entry<String, ConValue> edit : edits.entrySet()) {
			int vectorIndex = columnVectorIndex(header, edit.getKey());
			vectorMap.put(vectorIndex, patch.addNewOperation(Operations.newConstant().setValue(edit.getValue())));
		}
		patch.addNewOperation(Operations.insertVector().setVectorId(rowVectorId).setMap(vectorMap));

		JsonRxMessage message = new JsonRxMessage(JsonRxMessageType.RequestData).setId(102).setMethod("patch")
				.setBody(PatchCompactSerializable.serialize(patch));
		websocket.send(message.toJson());
		asynchronousJobWorkerHelper.waitForMessage((a) -> a.optInt(0) == 5 && a.optInt(1) == message.getId().get(),
				incomingMessages);
	}

	/**
	 * Wait until a cell's CRDT node attribution (user-owned vs service-owned)
	 * matches {@code expectedIsUserReplica}. A cell's attribution can change
	 * without its rendered value changing, so {@link #waitForRows} alone cannot
	 * detect this — callers that assert on attribution must poll separately
	 * rather than reading the header once immediately after a value-based wait.
	 */
	public void waitForCellAttribution(String sessionId, Long replicaId, String rowKey, String columnName,
			boolean expectedIsUserReplica) throws Exception {
		TimeUtils.waitFor(MAX_WAIT_MS, POLL_INTERVAL_MS, () -> {
			Optional<GridHeader> header = gridViewManager.readHeader(sessionId, replicaId);
			if (header.isEmpty()) {
				return Pair.create(false, null);
			}
			RowView row;
			try {
				row = getRowByKey(header.get(), rowKey);
			} catch (IllegalStateException e) {
				// row not (yet) visible
				return Pair.create(false, null);
			}
			boolean isUserReplica = GridConstants
					.isUserReplica(cellNode(header.get(), row, columnName).getId().getReplicaId());
			return Pair.create(isUserReplica == expectedIsUserReplica, null);
		});
	}

	/**
	 * Wait until every row keyed by {@code keys} (via column "a") has a
	 * validation result, and return each row's validation constant id — the CRDT
	 * logical timestamp identifying the current validation result, which
	 * advances on every revalidation regardless of whether the validation
	 * content itself changed.
	 */
	public Map<String, LogicalTimestamp> waitForValidationConstantIds(String sessionId, Long replicaId,
			Set<String> keys) throws Exception {
		return TimeUtils.waitFor(MAX_WAIT_MS, POLL_INTERVAL_MS, () -> {
			Optional<GridHeader> header = gridViewManager.readHeader(sessionId, replicaId);
			if (header.isEmpty()) {
				return Pair.create(false, null);
			}
			Map<String, LogicalTimestamp> found = new HashMap<>();
			for (RowView row : gridViewManager.querySinglePage(header.get(), PAGE_SIZE, 0L)) {
				var doc = row.getRowObject().getData().getRowJsonDocument();
				if (!doc.has("a")) {
					continue;
				}
				String key = String.valueOf(doc.get("a"));
				if (!keys.contains(key)) {
					continue;
				}
				LogicalTimestamp constantId = row.getRowMetadata() == null
						|| row.getRowMetadata().getRowValidation() == null ? null
								: row.getRowMetadata().getRowValidation().getConstantId();
				if (constantId == null) {
					return Pair.create(false, null);
				}
				found.put(key, constantId);
			}
			boolean done = found.keySet().containsAll(keys);
			return Pair.create(done, done ? found : null);
		});
	}

	/**
	 * Wait until every row in {@code baseline} has a validation constant id
	 * strictly greater than its baseline value — i.e. it has been re-validated
	 * since the baseline was captured.
	 */
	public void waitForValidationConstantIdsAdvanced(String sessionId, Long replicaId,
			Map<String, LogicalTimestamp> baseline) throws Exception {
		TimeUtils.waitFor(MAX_WAIT_MS, POLL_INTERVAL_MS, () -> {
			Optional<GridHeader> header = gridViewManager.readHeader(sessionId, replicaId);
			if (header.isEmpty()) {
				return Pair.create(false, null);
			}
			Map<String, LogicalTimestamp> current = new HashMap<>();
			for (RowView row : gridViewManager.querySinglePage(header.get(), PAGE_SIZE, 0L)) {
				var doc = row.getRowObject().getData().getRowJsonDocument();
				if (!doc.has("a")) {
					continue;
				}
				String key = String.valueOf(doc.get("a"));
				if (!baseline.containsKey(key)) {
					continue;
				}
				LogicalTimestamp constantId = row.getRowMetadata() == null
						|| row.getRowMetadata().getRowValidation() == null ? null
								: row.getRowMetadata().getRowValidation().getConstantId();
				current.put(key, constantId);
			}
			for (Map.Entry<String, LogicalTimestamp> entry : baseline.entrySet()) {
				LogicalTimestamp newId = current.get(entry.getKey());
				if (newId == null || newId.compareTo(entry.getValue()) <= 0) {
					return Pair.create(false, null);
				}
			}
			return Pair.create(true, null);
		});
	}

	private int columnVectorIndex(GridHeader header, String columnName) {
		return header.getOrderedColumns().stream().filter(c -> columnName.equals(c.getName())).findFirst()
				.map(Column::getVectorIndex).orElseThrow(() -> new IllegalStateException("No column: " + columnName));
	}

	private int columnPosition(GridHeader header, String columnName) {
		List<Column> columns = header.getOrderedColumns();
		for (int i = 0; i < columns.size(); i++) {
			if (columnName.equals(columns.get(i).getName())) {
				return i;
			}
		}
		throw new IllegalStateException("No column: " + columnName);
	}

	/** The CRDT cell node for a column in a row, located by the column's position in the header. */
	private ConstantNode cellNode(GridHeader header, RowView row, String columnName) {
		return row.getRowObject().getData().getNodes()[columnPosition(header, columnName)];
	}

	/** Find the (current) row whose "a" key column equals the given value. */
	private RowView getRowByKey(GridHeader header, String key) {
		for (RowView row : gridViewManager.querySinglePage(header, PAGE_SIZE, 0L)) {
			var doc = row.getRowObject().getData().getRowJsonDocument();
			if (doc.has("a") && key.equals(String.valueOf(doc.get("a")))) {
				return row;
			}
		}
		throw new IllegalStateException("No row with key: " + key);
	}
}
