package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONArray;
import org.sagebionetworks.grid.db.GridTransaction;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.table.cluster.ColumnTypeInfo;
import org.sagebionetworks.table.cluster.MySqlColumnType;
import org.sagebionetworks.util.PaginationIterator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GridCsvImportDaoImpl implements GridCsvImportDao {
	
	private static enum TempTableType {
		CSV,
		GRID;
		
		String tableName(String sessionId) {
			return "T_" + name() + "_" + GridUtils.gridSessionIdAsLong(sessionId);
		}
	}
	
	private static final String COL_EXTRA = "EXTRA";

	private static String getUpsertKeyColumnName(int index) {
		return "C" + index;
	}

	private static final int BATCH_SIZE = 1000;

	private JdbcTemplate jdbcTemplate;

	public GridCsvImportDaoImpl(JdbcTemplate gridDatabaseJdbcTemplate) {
		this.jdbcTemplate = gridDatabaseJdbcTemplate;
	}

	@Override
	@GridTransaction(readOnly = false)
	public void streamToCsvTempTable(String sessionId, DataStream dataStream, ColumnMapping[] columnMapping) {
		streamToTempTable(TempTableType.CSV.tableName(sessionId), dataStream, columnMapping);
	}

	@Override
	@GridTransaction(readOnly = false)
	public void streamToGridTempTable(String sessionId, DataStream dataStream, ColumnMapping[] columnMapping) {
		streamToTempTable(TempTableType.GRID.tableName(sessionId), dataStream, columnMapping);
	}

	@Override
	@GridTransaction(readOnly = true)
	public PaginationIterator<Object[]> getCsvTempTableIterator(String sessionId) {
		return getTempTableIterator(TempTableType.CSV.tableName(sessionId));
	}

	@Override
	@GridTransaction(readOnly = true)
	public PaginationIterator<Object[]> getGridTempTableIterator(String sessionId) {
		return getTempTableIterator(TempTableType.GRID.tableName(sessionId));
	}
	
	@Override
	@GridTransaction(readOnly = true)
	public PaginationIterator<JoinedRow> getJoinedTempTableIterator(String sessionId, ColumnMapping[] columnMapping) {
		List<ColumnMapping> csvUpsertColumns = Arrays.stream(columnMapping).filter(ColumnMapping::isUpsertColumn).collect(Collectors.toList());

		StringJoiner joinConditions = new StringJoiner(" AND ");
		StringJoiner orderByColumns = new StringJoiner(" DESC,", "", " DESC");

		IntStream.range(0, csvUpsertColumns.size()).forEach( columnIndex -> {
			String columnName = getUpsertKeyColumnName(columnIndex);
			joinConditions.add("C." + columnName + " = G." + columnName);
			// We order by descending so that we can create rows in reverse order without having to
			// get the last node of the array for each insert
			orderByColumns.add("C." + columnName);
		});
		
		String sql = String.format("SELECT C.*, G." + COL_EXTRA + " FROM " 
				+ TempTableType.CSV.tableName(sessionId) + " C LEFT JOIN " + TempTableType.GRID.tableName(sessionId) + " G ON (%s) ORDER BY %s"
				+ " LIMIT ? OFFSET ?", joinConditions.toString(), orderByColumns.toString());

		return new PaginationIterator<>((limit, offset) -> jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
			JSONArray csvExtraArray = new JSONArray(rs.getString(csvUpsertColumns.size() + 1));
			ConValue[] csvData = new ConValue[csvUpsertColumns.size() + csvExtraArray.length()];

			// Add the upsert columns first
			for (int i = 0; i < csvUpsertColumns.size(); i++) {
				Object value = rs.getObject(i + 1);
				csvData[i] = new ConValue(ConType.fromValue(value), value);
			}

			// Unpack the remaining CSV columns from the extra column
			for (int i = 0; i < csvExtraArray.length(); i++) {
				Object value = csvExtraArray.get(i);
				csvData[i + csvUpsertColumns.size()] = new ConValue(ConType.fromValue(value), value);
			}

			LogicalTimestamp gridRowVecId = null;

			// The grid data can be null if there is no match
			String gridExtraStr = rs.getString(csvUpsertColumns.size() + 2);

			if (gridExtraStr != null) {
				// We only need the row vector id from the grid data
				gridRowVecId = LogicalTimestampCompactSerializable.deserialize(new JSONArray(gridExtraStr).getJSONArray(0));
			}

			return new JoinedRow(Arrays.asList(csvData), gridRowVecId);
		}, limit, offset), BATCH_SIZE);
	}
	
	@Override
	@GridTransaction(readOnly = false)
	public void dropTemporaryTables(String sessionId) {
		for (TempTableType type : TempTableType.values()) {
			dropTemporaryTable(type.tableName(sessionId));
		}
	}

	void streamToTempTable(String tableName, DataStream dataStream, ColumnMapping[] columnMapping) {
		List<ColumnMapping> upsertKey = Arrays.stream(columnMapping)
			.filter(ColumnMapping::isUpsertColumn)
			.collect(Collectors.toList());

		createTemporaryTable(tableName, upsertKey);

		List<Object[]> batch = new ArrayList<>(BATCH_SIZE);

		while (dataStream.hasNext()) {
			batch.add(mapRow(dataStream.next(), upsertKey.size()));
			if (batch.size() >= BATCH_SIZE) {
				flushBatch(tableName, upsertKey, batch);
			}
		}
		
		flushBatch(tableName, upsertKey, batch);
	}

	Object[] mapRow(Object[] row, int upsertKeyLength) {
		Object[] mappedRow = new Object[upsertKeyLength + 1];

		int i = 0;

		// We normalize the upsert key columns first
		for (; i < upsertKeyLength; i++) {
			mappedRow[i] = row[i];
		}

		// Pack the remaining columns into a single extra JSON array column
		JSONArray extraData = new JSONArray();

		for (; i < row.length; i++) {
			extraData.put(row[i]);
		}

		mappedRow[mappedRow.length - 1] = extraData.toString();

		return mappedRow;
	}

	PaginationIterator<Object[]> getTempTableIterator(String tableName) {
		return new PaginationIterator<>((limit, offset) -> {
			String sql = "SELECT * FROM " + tableName + " LIMIT ? OFFSET ?";
			return jdbcTemplate.query(sql, (rs, rowNum) -> {
				Object[] row = new Object[rs.getMetaData().getColumnCount()];
				for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
					row[i] = rs.getObject(i + 1);
				}
				return row;
			}, limit, offset);
		}, BATCH_SIZE);
	}

	void flushBatch(String tableName, List<ColumnMapping> upsertKey, List<Object[]> batch) {
		if (batch.isEmpty()) {
			return;
		}

		StringJoiner columnNames = new StringJoiner(",");
		StringJoiner valuePlaceholders = new StringJoiner(",");

		for (int i = 0; i < upsertKey.size(); i++) {
			columnNames.add(getUpsertKeyColumnName(i));
			valuePlaceholders.add("?");
		}

		columnNames.add(COL_EXTRA);
		valuePlaceholders.add("?");

		String sql = "INSERT INTO " + tableName + " (" + columnNames.toString() + ") VALUES (" + valuePlaceholders.toString() + ")";

		jdbcTemplate.batchUpdate(sql, batch);

		batch.clear();
	}

	void createTemporaryTable(String tableName, List<ColumnMapping> upsertKey) {
		StringJoiner columnDefinitions = new StringJoiner(",");
		StringJoiner upsertKeyColumns = new StringJoiner(",");

		int index = 0;

		for (ColumnMapping mapping : upsertKey) {

			String columnName = getUpsertKeyColumnName(index);

			StringBuilder columnDefinition = new StringBuilder();

			columnDefinition.append(columnName);

			MySqlColumnType sqlType;

			sqlType = ColumnTypeInfo.getInfoForType(mapping.getType()).getMySqlType();

			columnDefinition.append(" ");
			columnDefinition.append(sqlType.name());

			if (sqlType.hasSize()) {
				columnDefinition.append("(");
				columnDefinition.append(ColumnConstants.MAX_MYSQL_VARCHAR_INDEX_LENGTH);
				columnDefinition.append(")");
			}

			columnDefinition.append(" NOT NULL");

			columnDefinitions.add(columnDefinition.toString());

			upsertKeyColumns.add(columnName);
			index++;
		}

		// Add extra TEXT column to hold any additional data
		columnDefinitions.add(COL_EXTRA + " " + MySqlColumnType.TEXT.name() + " NOT NULL");

		// Add an index on the upsert key columns
		columnDefinitions.add("INDEX upsertKeyIndex(" + upsertKeyColumns.toString() + ")");

		String sql = "CREATE TEMPORARY TABLE " + tableName + " (" + columnDefinitions.toString() + ")";

		jdbcTemplate.update(sql);
	}
	
	void dropTemporaryTable(String tableName) {
		jdbcTemplate.update("DROP TEMPORARY TABLE IF EXISTS " + tableName);
	}

}
