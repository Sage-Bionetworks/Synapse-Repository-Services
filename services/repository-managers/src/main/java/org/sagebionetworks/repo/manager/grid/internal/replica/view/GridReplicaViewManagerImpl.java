package org.sagebionetworks.repo.manager.grid.internal.replica.view;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowData;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowMetadata;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowValidation;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.filter.ViewFilter;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.PaginationIterator;
import org.sagebionetworks.util.ValidateArgument;
import org.semver4j.Semver;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class GridReplicaViewManagerImpl implements GridReplicaViewManager {

	private static final String GRID_INDEX_VIEW_TEMPLATE = loadStringFromClasspath("grid/grid-index-view-template.sql");

	private static RowMapper<RowView> ROW_VIEW_MAPPER = (ResultSet rs, int rowNum) -> {
		return new RowView().setArrNodeId(readNullableTimestamp(rs, "D.NODE_REP", "D.NODE_SEQ"))
				.setRowIndex(rs.getLong("INDEX"))
				.setRowObject(new RowObject().setObjectId(readNullableTimestamp(rs, "O1.OBJ_REP", "O1.OBJ_SEQ"))
						.setMetadata(new RowMetadata()
								.setObjectId(readNullableTimestamp(rs, "O2.OBJ_REP", "O2.OBJ_SEQ"))
								.setRowValidation(new RowValidation()
										.setConstantId(readNullableTimestamp(rs, "RVC_REP", "RVC_SEQ")))
								.setSynapseRow(new SynapseRow()
										.setConstantId(readNullableTimestamp(rs, "SRC_REP", "SRC_SEQ"))))
						.setData(new RowData().setVectorId(readNullableTimestamp(rs, "V1.VEC_REP", "V1.VEC_SEQ"))
								.setCells(new JSONArray(rs.getString("VALS")))));
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
	public List<RowView> querySinglePage(GridHeader header, List<ViewFilter> filters, Long limit, Long offset) {
		ValidateArgument.required(header, "header");
		ValidateArgument.required(filters, "filters");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("sessionId", GridUtils.gridSessionIdAsLong(header.getSessionId()));
		params.addValue("replicaId", header.getReplicaId());
		params.addValue("arrayRep", header.getRowsId().getReplicaId());
		params.addValue("arraySeq", header.getRowsId().getSequenceNumber());
		params.addValue("limit", limit);
		params.addValue("offset", offset);

		StringJoiner joiner = new StringJoiner(",");
		// read the values out of each array in the order defined in the header.
		header.getOrderedColumns().forEach(c -> {
			joiner.add(String.format("JSON_EXTRACT(V1.VEC_VAL, '$.c%d.v')", c.getVectorIndex()));
		});
		String select = joiner.toString();
		
		StringBuilder whereBuilder = new StringBuilder(" WHERE IS_DELETED IS FALSE");
		
		if (!filters.isEmpty()) {
			for (int i = 0; i < filters.size(); i++) {
				ViewFilter f = filters.get(i);
				whereBuilder.append(" AND ").append(f.getConditionSql(i));
				params.addValue(f.getParameterKey(i), f.getParameterValue());
			}
		}
		String sql = String.format(GRID_INDEX_VIEW_TEMPLATE, select, whereBuilder.toString());

		List<RowView> page = gridIndexDao.query(sql, params, ROW_VIEW_MAPPER);

		List<LogicalTimestamp> conId = page.stream().flatMap(r -> {
			return r.getConstantIds().stream();
		}).collect(Collectors.toList());

		Map<LogicalTimestamp, ConstantNode> constantMap = gridIndexDao
				.getConstants(header.getSessionId(), header.getReplicaId(), conId).stream()
				.collect(Collectors.toMap(ConstantNode::getId, Function.identity()));

		page.stream().forEach(r -> {
			r.applyConstants(constantMap);
		});

		return page;
	}

    @Override
    public Iterator<RowView> getQueryIterator(GridHeader header, List<ViewFilter> filters) {
        final long ROWS_PER_PAGE = 1_000L;
        return new PaginationIterator<>(
                (long limit, long offset) -> this.querySinglePage(header, filters, limit, offset),
                ROWS_PER_PAGE
        );
    }

    @Override
	public Optional<GridHeader> readHeader(String gridSessionId, Long replicaId) {
		Optional<ObjectNode> rootOpt = gridIndexDao.getRootObject(gridSessionId, replicaId);
		if (rootOpt.isEmpty()) {
			return Optional.empty();
		}
		ObjectNode root = rootOpt.get();
		ConstantNode docVersion = gridIndexDao
				.getConstants(gridSessionId, replicaId, List.of(root.getValue().get("doc_version"))).get(0);
		docVersion.getValue();

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

		LogicalTimestamp rowsId = root.getValue().get("rows");
		return Optional.of(new GridHeader().setSessionId(gridSessionId).setReplicaId(replicaId).setRowsId(rowsId)
				.setDocumentVersion(semver).setNodeId(root.getId()).setOrderedColumns(columns)
				.setColumnOrderArrId(columnOrderArrId).setColumnNamesVecId(columnNames.getId()));
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

}
