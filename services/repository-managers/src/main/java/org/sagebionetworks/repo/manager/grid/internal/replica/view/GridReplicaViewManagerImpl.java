package org.sagebionetworks.repo.manager.grid.internal.replica.view;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.grid.db.GridTransaction;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowData;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowMetadata;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowValidation;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.Context;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.QueryElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.FilterElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.select.SelectItemElement;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.grid.ReplicaSelectionModel;
import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.query.result.QueryResult;
import org.sagebionetworks.repo.model.grid.query.result.Row;
import org.sagebionetworks.repo.model.grid.query.result.SelectColumn;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.util.PaginationIterator;
import org.sagebionetworks.util.ValidateArgument;
import org.semver4j.Semver;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
@GridTransaction(readOnly = true)
public class GridReplicaViewManagerImpl implements GridReplicaViewManager {

	private static final String GRID_INDEX_VIEW_TEMPLATE = loadStringFromClasspath("grid/grid-index-view-template.sql");

	private static RowMapper<RowView> ROW_VIEW_MAPPER = (ResultSet rs, int rowNum) -> {
		return new RowView().setArrNodeId(readNullableTimestamp(rs, "AN_REP", "AN_SEQ"))
				.setRowIndex(rs.getLong("INDEX"))
				.setRowObject(new RowObject().setObjectId(readNullableTimestamp(rs, "RO_REP", "RO_SEQ"))
						.setMetadata(new RowMetadata().setObjectId(readNullableTimestamp(rs, "MO_REP", "MO_SEQ"))
								.setRowValidation(new RowValidation()
										.setValidationResults(JDOSecondaryPropertyUtils
												.createObjectFromJSON(ValidationResults.class, rs.getString("VAL_RES")))
										.setConstantId(readNullableTimestamp(rs, "RVC_REP", "RVC_SEQ")))
								.setSynapseRow(new SynapseRow().setFromJSON(rs.getString("SYN_ROW"))
										.setConstantId(readNullableTimestamp(rs, "SRC_REP", "SRC_SEQ"))))
						.setData(new RowData().setVectorId(readNullableTimestamp(rs, "VEC_REP", "VEC_SEQ"))
								.setCells(new JSONArray(rs.getString("VALS")))));
	};

	private static RowMapper<RowView> ROW_VIEW_AGGREGATION_MAPPER = (ResultSet rs, int rowNum) -> {
		JSONArray cells = new JSONArray();
		cells.put(rs.getLong("C"));
		return new RowView().setRowObject(new RowObject().setData(new RowData().setCells(cells)));
	};

	/**
	 * Helper to read a nullable {@link LogicalTimestamp} given the rep and seq
	 * column names;
	 * 
	 * @param rs
	 * @param repName
	 * @param seqName
	 * @return
	 * @throws SQLException
	 */
	public static LogicalTimestamp readNullableTimestamp(ResultSet rs, String repName, String seqName)
			throws SQLException {
		Long rep = readNullableLong(rs, repName);
		Long seq = readNullableLong(rs, seqName);
		return rep == null || seq == null ? null : new LogicalTimestamp().setReplicaId(rep).setSequenceNumber(seq);
	}

	/**
	 * Helper to get nullable long from a result set.
	 * 
	 * @param rs
	 * @param columnName
	 * @return
	 * @throws SQLException
	 */
	public static Long readNullableLong(ResultSet rs, String columnName) throws SQLException {
		long val = rs.getLong(columnName);
		return rs.wasNull() ? null : val;
	}

	private final GridIndexDao gridIndexDao;

	public GridReplicaViewManagerImpl(GridIndexDao gridIndexDao) {
		super();
		this.gridIndexDao = gridIndexDao;
	}

	@Override
	public List<RowView> querySinglePage(GridHeader header, Long limit, Long offset) {
		return querySinglePage(header, Collections.emptyList(), limit, offset);
	}

	@Override
	public List<RowView> querySinglePage(GridHeader header, List<FilterElement> filters, Long limit, Long offset) {
		ValidateArgument.required(header, "header");
		ValidateArgument.required(filters, "filters");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		return querySinglePage(header, new QueryElement().setWhere(filters).setLimit(limit).setOffset(offset));
	}

	@Override
	public List<RowView> querySinglePage(GridHeader header, QueryElement query) {
		ValidateArgument.required(header, "header");
		ValidateArgument.required(query, "query");

		Map<String, Object> params = new HashMap<>();
		params.put("sessionId", GridUtils.gridSessionIdAsLong(header.getSessionId()));
		params.put("replicaId", header.getReplicaId());
		params.put("arrayRep", header.getRowsId().getReplicaId());
		params.put("arraySeq", header.getRowsId().getSequenceNumber());

		StringJoiner joiner = new StringJoiner(",");
		// read the values out of each array in the order defined in the header.
		header.getOrderedColumns().forEach(c -> {
			joiner.add(String.format("JSON_EXTRACT(V1.VEC_VAL, '$.c%d.v')", c.getVectorIndex()));
		});
		String select = joiner.toString();
		StringBuilder sqlBuilder = new StringBuilder();
		query.toSql(sqlBuilder, params, new Context(header));

		String sql = String.format(GRID_INDEX_VIEW_TEMPLATE, select, sqlBuilder.toString());

		RowMapper<RowView> mapper = query.isAggregate() ? ROW_VIEW_AGGREGATION_MAPPER : ROW_VIEW_MAPPER;
		return gridIndexDao.query(sql, new MapSqlParameterSource(params), mapper);
	}

	@Override
	public Iterator<RowView> getQueryIterator(GridHeader header, List<FilterElement> filters) {
		final long ROWS_PER_PAGE = 1_000L;
		return new PaginationIterator<>(
				(long limit, long offset) -> this.querySinglePage(header, filters, limit, offset), ROWS_PER_PAGE);
	}
	
	@Override
	public Optional<GridHeader> readHeader(String gridSessionId, Long replicaId) {
		return readHeader(gridSessionId, replicaId, null);
	}

	@Override
	public Optional<GridHeader> readHeader(String gridSessionId, Long replicaId, Long usersReplicaId) {
		Optional<ObjectNode> rootOpt = gridIndexDao.getRootObject(gridSessionId, replicaId);
		if (rootOpt.isEmpty()) {
			return Optional.empty();
		}
		ObjectNode root = rootOpt.get();
		List<LogicalTimestamp> constantIds = new ArrayList<>();
		LogicalTimestamp selectionId = root.getValue().get("selection");
		LogicalTimestamp selectionConId = null;
		if (selectionId != null && usersReplicaId != null) {
			List<ObjectNode> selections = gridIndexDao.getObjects(gridSessionId, replicaId, List.of(selectionId));
			if (selections.size() == 1) {
				ObjectNode selectionNode = selections.get(0);
				selectionConId = selectionNode.getValue().get(usersReplicaId.toString());
				if(selectionConId != null) {
					constantIds.add(selectionConId);
				}
			}
		}
		LogicalTimestamp docVersionConId = root.getValue().get("doc_version");
		constantIds.add(docVersionConId);

		Map<LogicalTimestamp, ConstantNode> constants = gridIndexDao.getConstants(gridSessionId, replicaId, constantIds)
				.stream().collect(Collectors.toMap(ConstantNode::getId, Function.identity()));

		ConstantNode selectionCon = constants.get(selectionConId);
		ReplicaSelectionModel selectionModel = null;
		if (selectionCon != null) {
			selectionModel = JDOSecondaryPropertyUtils.createEntityFromJSONObject((JSONObject) selectionCon.getValue(),
					ReplicaSelectionModel.class);
		}

		ConstantNode docVersion = constants.get(docVersionConId);
		Semver semver = new Semver((String) docVersion.getValue());
		if (semver.isGreaterThan(new Semver("0.1.0"))) {
			throw new IllegalArgumentException("Cannot read a document version of: " + semver.toString());
		}
		VectorNode columnNames = gridIndexDao
				.getVectors(gridSessionId, replicaId, List.of(root.getValue().get("columnNames"))).get(0);

		LogicalTimestamp columnOrderArrId = root.getValue().get("columnOrder");
		List<ArrayNode> columnOrder = gridIndexDao.getArrayNodesInOrder(gridSessionId, replicaId, columnOrderArrId,
				1000L, 0l);

		Map<LogicalTimestamp, Integer> columnOrderValues = gridIndexDao
				.getConstants(gridSessionId, replicaId,
						columnOrder.stream().map(ArrayNode::getDataId).collect(Collectors.toList()))
				.stream().collect(Collectors.toMap(ConstantNode::getId, (c) -> (Integer) c.getValue()));

		List<Column> columns = columnOrder.stream().map(a -> {
			Integer vectorIndex = columnOrderValues.get(a.getDataId());
			String columnName = (String) columnNames.getValues().get("c" + vectorIndex).getValue();
			return new Column().setVectorIndex(vectorIndex).setName(columnName);
		}).collect(Collectors.toList());

		Long clockSequenceMaximum = gridIndexDao.getClockSequenceMaximum(gridSessionId, replicaId);

		LogicalTimestamp rowsId = root.getValue().get("rows");
		return Optional.of(new GridHeader().setSessionId(gridSessionId).setReplicaId(replicaId).setRowsId(rowsId)
				.setDocumentVersion(semver).setNodeId(root.getId()).setOrderedColumns(columns)
				.setColumnOrderArrId(columnOrderArrId).setColumnNamesVecId(columnNames.getId())
				.setClockSequenceMaximum(clockSequenceMaximum).setReplicaSelectionModel(selectionModel));
	}

	/**
	 * Helper to load a string from a file on the classpath.
	 * 
	 * @param name
	 * @return
	 */
	private static String loadStringFromClasspath(String name) {
		try (InputStream in = GridReplicaViewManagerImpl.class.getClassLoader().getResourceAsStream(name)) {
			if (in == null) {
				throw new IllegalArgumentException("Cannot find file " + name + " on classpath.");
			}
			return IOUtils.toString(in, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public QueryResult querySinglePageAsQueryResult(GridHeader header, QueryElement query) {
		List<RowView> rowViews = querySinglePage(header, query);
		List<Row> rows = rowViews.stream()
				.map(v -> new Row().setValidationResults(translateValidation(v.getRowValidationResults()))
						.setCellValues(toList(v.getRowObject().getCells())))
				.collect(Collectors.toList());
		return new QueryResult().setRows(rows).setSelectColumns(translateSelect(header, query.getSelect()));
	}
	
	List<SelectColumn> translateSelect(GridHeader header, List<SelectItemElement> items){
		List<SelectColumn> cols = new ArrayList<>();
		for(int i=0; i<items.size(); i++) {
			SelectItemElement item = items.get(i);
			item.setSelect(header, Long.valueOf(i), cols);
		}
		return cols;
	}

	org.sagebionetworks.repo.model.grid.query.result.ValidationResults translateValidation(ValidationResults r) {
		if (r == null) {
			return null;
		}
		return new org.sagebionetworks.repo.model.grid.query.result.ValidationResults()
				.setValidationErrorMessage(r.getValidationErrorMessage())
				.setAllValidationMessages(r.getAllValidationMessages()).setIsValid(r.getIsValid());
	}

	List<Object> toList(JSONArray cells) {
		if (cells == null) {
			return Collections.emptyList();
		}
		ArrayList<Object> obs = new ArrayList<>();

		for (int i = 0; i < cells.length(); i++) {
			obs.add(cells.isNull(i) ? null : cells.get(i));
		}
		return obs;
	}



}
