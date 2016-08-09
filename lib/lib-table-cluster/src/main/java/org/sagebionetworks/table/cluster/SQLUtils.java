package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.repo.model.table.TableConstants.FILE_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;
import static org.sagebionetworks.repo.model.table.TableConstants.SCHEMA_HASH;
import static org.sagebionetworks.repo.model.table.TableConstants.SINGLE_KEY;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Utilities for generating Table SQL, DML, and DDL.
 * 
 * @author jmhill
 * 
 */
public class SQLUtils {


	private static final String DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS %1$S";
	private static final String SELECT_COUNT_FROM_TEMP = "SELECT COUNT(*) FROM ";
	private static final String SQL_COPY_TABLE_TO_TEMP = "INSERT INTO %1$S SELECT * FROM %2$S ORDER BY "+ROW_ID;
	private static final String CREATE_TABLE_LIKE = "CREATE TABLE %1$S LIKE %2$S";
	private static final String TEMP = "TEMP";
	private static final String IDX = "idx_";
	public static final String CHARACTER_SET_UTF8_COLLATE_UTF8_GENERAL_CI = "CHARACTER SET utf8 COLLATE utf8_general_ci";
	public static final String FILE_ID_BIND = "bFIds";
	public static final String ROW_ID_BIND = "bRI";
	public static final String ROW_VERSION_BIND = "bRV";
	public static final String DEFAULT = "DEFAULT";
	public static final String TABLE_PREFIX = "T";
	private static final String COLUMN_PREFIX = "_C";
	private static final String COLUMN_POSTFIX = "_";

	private static final String DOUBLE_NAN = Double.toString(Double.NaN);
	private static final String DOUBLE_POSITIVE_INFINITY = Double.toString(Double.POSITIVE_INFINITY);
	private static final String DOUBLE_NEGATIVE_INFINITY = Double.toString(Double.NEGATIVE_INFINITY);
	private static final String DOUBLE_ENUM_CLAUSE = " ENUM ('" + DOUBLE_NAN + "', '" + DOUBLE_POSITIVE_INFINITY + "', '"
			+ DOUBLE_NEGATIVE_INFINITY + "') DEFAULT null";

	public enum TableType {
		/**
		 * The index tables
		 */
		INDEX(""),
		/**
		 * The status table that tracks the current state of the index table
		 */
		STATUS("S"),
		/**
		 * Table tracking filehandles bound to a given table.
		 */
		FILE_IDS("F");

		private final String tablePostFix;
		private final Pattern tableNamePattern;

		private TableType(String tablePostFix) {
			this.tablePostFix = tablePostFix;
			this.tableNamePattern = Pattern.compile(TABLE_PREFIX + "\\d+" + tablePostFix);
		}

		public String getTablePostFix() {
			return tablePostFix;
		}

		public Pattern getTableNamePattern() {
			return tableNamePattern;
		}
	}
	
	/**
	 * Secondary tables are additional tables used to support a table's index.
	 */
	public static final List<TableType> SECONDARY_TYPES = ImmutableList.of(TableType.STATUS, TableType.FILE_IDS);
	
	/**
	 * Given a new schema generate the create table DDL.
	 * 
	 * @param newSchema
	 * @return
	 */
	public static String createTableSQL(String tableId, TableType type) {
		ValidateArgument.required(tableId, "tableId");
		StringBuilder columnDefinitions = new StringBuilder();
		switch (type) {
		case STATUS:
			columnDefinitions.append("single_key ENUM('1') NOT NULL PRIMARY KEY, ");
			columnDefinitions.append(ROW_VERSION).append(" bigint(20) NOT NULL,");
			columnDefinitions.append(SCHEMA_HASH).append(" CHAR(35) NOT NULL");
			break;
		case FILE_IDS:
			columnDefinitions.append(FILE_ID).append(" bigint(20) NOT NULL PRIMARY KEY");
			break;
		default:
			throw new IllegalArgumentException("Cannot handle type " + type);
		}
		return createTableSQL(tableId, type, columnDefinitions.toString());
	}

	public static String createTableSQL(Long tableId, TableType type) {
		return createTableSQL(tableId.toString(), type);
	}

	private static String createTableSQL(String tableId, TableType type, String columnDefinitions) {
		return "CREATE TABLE IF NOT EXISTS `" + getTableNameForId(tableId, type) + "` ( " + columnDefinitions + " )";
	}

	/**
	 * Pares the value for insertion into the database.
	 * @param value
	 * @param type
	 * @return
	 */
	public static Object parseValueForDB(ColumnType type, String value){
		if(value == null) return null;
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		ColumnTypeInfo info = ColumnTypeInfo.getInfoForType(type);
		return info.parseValueForDB(value);
	}

	/**
	 * Build up a set of column Ids from the passed schema.
	 * 
	 * @param schema
	 * @return
	 */
	static Set<String> createColumnIdSet(List<ColumnModel> schema) {
		HashSet<String> set = new HashSet<String>(schema.size());
		for (ColumnModel cm : schema) {
			if (cm.getId() == null)
				throw new IllegalArgumentException("ColumnId cannot be null");
			set.add(cm.getId());
		}
		return set;
	}

	/**
	 * Get the list of ColumnModels that do not have their IDs in the passed
	 * set.
	 * 
	 * @param set
	 * @param schema
	 * @return
	 */
	static List<ColumnModel> listNotInSet(Set<String> set, List<ColumnModel> schema) {
		List<ColumnModel> list = new LinkedList<ColumnModel>();
		for (ColumnModel cm : schema) {
			if (!set.contains(cm.getId())) {
				list.add(cm);
			}
		}
		return list;
	}
	

	/**
	 * Get the Table Name for a given table ID.
	 * 
	 * @param tableId
	 * @return
	 */
	public static String getTableNameForId(String tableId, TableType type) {
		if (tableId == null)
			throw new IllegalArgumentException("Table ID cannot be null");
		return TABLE_PREFIX + KeyFactory.stringToKey(tableId) + type.getTablePostFix();
	}

	public static String getTableNameForId(Long tableId, TableType type) {
		ValidateArgument.required(tableId, "Table ID");
		return TABLE_PREFIX + tableId + type.getTablePostFix();
	}

	/**
	 * Get the Column name for a given column ID.
	 * 
	 * @param columnId
	 * @return
	 */
	public static String getColumnNameForId(String columnId) {
		if (columnId == null)
			throw new IllegalArgumentException("Column ID cannot be null");
		return COLUMN_PREFIX + columnId.toString() + COLUMN_POSTFIX;
	}

	public static void appendColumnName(String subName, String columnId, StringBuilder builder) {
		appendColumnName(null, subName, columnId, builder);
	}

	private static void appendColumnName(String prefix, String subName, String columnId, StringBuilder builder) {
		if (prefix != null) {
			builder.append(prefix);
		}
		builder.append(subName).append(COLUMN_PREFIX).append(columnId).append(COLUMN_POSTFIX);
	}

	/**
	 * Append case statement for doubles, like:
	 * 
	 * <pre>
	 * CASE
	 * 	WHEN _DBL_C1_ IS NULL THEN _C1_
	 * 	ELSE _DBL_C1_
	 * END AS _C1_
	 * </pre>
	 * 
	 * @param column
	 * @param subName
	 * @param builder
	 */
	public static void appendDoubleCase(String columnId, StringBuilder builder) {
		String subName = "";
		builder.append("CASE WHEN ");
		appendColumnName(TableConstants.DOUBLE_PREFIX, subName, columnId, builder);
		builder.append(" IS NULL THEN ");
		appendColumnName(subName, columnId, builder);
		builder.append(" ELSE ");
		appendColumnName(TableConstants.DOUBLE_PREFIX, subName, columnId, builder);
		builder.append(" END");
	}
	
	/**
	 * Create a double clause.
	 * @param columnId
	 * @return
	 */
	public static String createDoubleCluase(String columnId){
		StringBuilder builder = new StringBuilder();
		appendDoubleCase(columnId, builder);
		return builder.toString();
	}


		/**
	 * Compare doubles to NaN
	 * 
	 * <pre>
	 * _DBL_C1_ = 'NaN'
	 * </pre>
	 * 
	 * 
	 * @param column
	 * @param subName
	 * @param builder
	 */
	public static void appendIsNan(String columnId, String subName, StringBuilder builder) {
		builder.append("(");
		appendColumnName(TableConstants.DOUBLE_PREFIX, subName, columnId, builder);
		builder.append(" IS NOT NULL AND ");
		appendColumnName(TableConstants.DOUBLE_PREFIX, subName, columnId, builder);
		builder.append(" = '").append(DOUBLE_NAN).append("')");
	}

	/**
	 * Compare doubles to infinity
	 * 
	 * <pre>
	 * _DBL_C1_ IN ('Infinity', '-Infinity')
	 * </pre>
	 * 
	 * @param column
	 * @param subName
	 * @param builder
	 */
	public static void appendIsInfinity(String columnId, String subName, StringBuilder builder) {
		builder.append("(");
		appendColumnName(TableConstants.DOUBLE_PREFIX, subName, columnId, builder);
		builder.append(" IS NOT NULL AND ");
		appendColumnName(TableConstants.DOUBLE_PREFIX, subName, columnId, builder);
		builder.append(" IN ('").append(DOUBLE_NEGATIVE_INFINITY).append("', '").append(DOUBLE_POSITIVE_INFINITY).append("'))");
	}

	/**
	 * Create the DROP table SQL.
	 * @param tableId
	 * @return
	 */
	public static String dropTableSQL(String tableId, TableType type) {
		String tableName = getTableNameForId(tableId, type);
		return "DROP TABLE " + tableName;
	}

	public static String dropTableSQL(Long tableId, TableType type) {
		String tableName = getTableNameForId(tableId, type);
		return "DROP TABLE " + tableName;
	}

	/**
	 * Create the delete a batch of row Ids SQL.
	 * 
	 * @param tableId
	 * @return
	 */
	public static String deleteBatchFromTableSQL(Long tableId, TableType type) {
		String tableName = getTableNameForId(tableId, type);
		return "DELETE FROM " + tableName + " WHERE " + ROW_ID + " IN ( :" + ROW_ID_BIND + " )";
	}

	/**
	 * Create the delete a row Ids SQL.
	 * 
	 * @param tableId
	 * @return
	 */
	public static String deleteFromTableSQL(Long tableId, TableType type) {
		String tableName = getTableNameForId(tableId, type);
		return "DELETE FROM " + tableName + " WHERE " + ROW_ID + " = ?";
	}

	/**
	 * Build the create or update statement for inserting rows into a table.
	 * @param schema
	 * @param tableId
	 * @return
	 */
	public static String buildCreateOrUpdateRowSQL(List<ColumnModel> schema, String tableId){
		if(schema == null) throw new IllegalArgumentException("Schema cannot be null");
		if(schema.size() < 1) throw new IllegalArgumentException("Schema must include at least on column");
		if(tableId == null) throw new IllegalArgumentException("TableID cannot be null");
 		StringBuilder builder = new StringBuilder();
		builder.append("INSERT INTO ");
		builder.append(getTableNameForId(tableId, TableType.INDEX));
		builder.append(" (");
		// Unconditionally set these two columns
		builder.append(ROW_ID);
		builder.append(", ").append(ROW_VERSION);
		List<String> columnNames = getColumnNames(schema);
		for (String columnName : columnNames) {
			builder.append(", ");
			builder.append(columnName);
		}
		builder.append(") VALUES ( :").append(ROW_ID_BIND).append(", :").append(ROW_VERSION_BIND);
		for (String columnName : columnNames) {
			builder.append(", :");
			builder.append(columnName);
		}
		builder.append(") ON DUPLICATE KEY UPDATE " + ROW_VERSION + " = VALUES(" + ROW_VERSION + ")");
		for (String columnName : columnNames) {
			builder.append(", ");
			builder.append(columnName);
			builder.append(" = VALUES(").append(columnName).append(")");
		}
		return builder.toString();
	}
	
	/**
	 * Get all of the column names for the given schema.
	 * @param schema
	 * @return
	 */
	public static List<String> getColumnNames(List<ColumnModel> schema){
		List<String> names = new LinkedList<String>();
		for(ColumnModel cm: schema){
			String columnName = getColumnNameForId(cm.getId());
			names.add(columnName);
			if(ColumnType.DOUBLE.equals(cm.getColumnType())){
				names.add(TableConstants.DOUBLE_PREFIX + columnName);
			}
		}
		return names;
	}

	/**
	 * Build the create or update statement for inserting rows into a table.
	 * 
	 * @param schema
	 * @param tableId
	 * @return
	 */
	public static String buildCreateOrUpdateStatusSQL(String tableId) {
		if (tableId == null)
			throw new IllegalArgumentException("TableID cannot be null");
		StringBuilder builder = new StringBuilder();
		builder.append("INSERT INTO ");
		builder.append(getTableNameForId(tableId, TableType.STATUS));
		builder.append(" ( ");
		builder.append(SINGLE_KEY);
		builder.append(",");
		builder.append(ROW_VERSION);
		builder.append(",");
		builder.append(SCHEMA_HASH);
		builder.append(" ) VALUES ('1', ?, 'DEFAULT' ) ON DUPLICATE KEY UPDATE "+ROW_VERSION+" = ? ");
		return builder.toString();
	}
	
	public static String buildCreateOrUpdateStatusHashSQL(String tableId) {
		if (tableId == null)
			throw new IllegalArgumentException("TableID cannot be null");
		StringBuilder builder = new StringBuilder();
		builder.append("INSERT INTO ");
		builder.append(getTableNameForId(tableId, TableType.STATUS));
		builder.append(" ( ");
		builder.append(SINGLE_KEY);
		builder.append(",");
		builder.append(ROW_VERSION);
		builder.append(",");
		builder.append(SCHEMA_HASH);
		builder.append(" ) VALUES ('1', -1, ? ) ON DUPLICATE KEY UPDATE "+SCHEMA_HASH+" = ? ");
		return builder.toString();
	}

	/**
	 * Build the delete statement for inserting rows into a table.
	 * 
	 * @param schema
	 * @param tableId
	 * @return
	 */
	public static String buildDeleteSQL(List<ColumnModel> schema, String tableId){
		if(schema == null) throw new IllegalArgumentException("Schema cannot be null");
		if(schema.size() < 1) throw new IllegalArgumentException("Schema must include at least on column");
		if(tableId == null) throw new IllegalArgumentException("TableID cannot be null");
 		StringBuilder builder = new StringBuilder();
		builder.append("DELETE FROM ");
		builder.append(getTableNameForId(tableId, TableType.INDEX));
		builder.append(" WHERE ");
		builder.append(ROW_ID);
		builder.append(" IN ( :").append(ROW_ID_BIND).append(" )");
		return builder.toString();
	}

	/**
	 * Build the parameters that will bind the passed RowSet to a SQL statement.
	 * 
	 * @param toBind
	 * @param schema
	 * @return
	 */
	public static SqlParameterSource[] bindParametersForCreateOrUpdate(RowSet toBind, List<ColumnModel> schema){
		// First we need a mapping from the the schema to the RowSet
		Map<String, Integer> columnIndexMap = new HashMap<String, Integer>();
		int index = 0;
		for (SelectColumn header : toBind.getHeaders()) {
			// columns might no longer exists
			if (header != null) {
				columnIndexMap.put(header.getId(), index);
			}
			index++;
		}

		// We will need a binding for every row
		List<MapSqlParameterSource> results = Lists.newArrayListWithExpectedSize(toBind.getRows().size() * 2);
		for(Row row: toBind.getRows()){
			if (!TableModelUtils.isDeletedRow(row)) {
				Map<String, Object> rowMap = new HashMap<String, Object>(schema.size() + 2);
				// Always bind the row ID and version
				if (row.getRowId() == null)
					throw new IllegalArgumentException("RowID cannot be null");
				if (row.getVersionNumber() == null)
					throw new IllegalArgumentException("RowVersionNumber cannot be null");
				rowMap.put(ROW_ID_BIND, row.getRowId());
				rowMap.put(ROW_VERSION_BIND, row.getVersionNumber());
				// Bind each column
				for (ColumnModel cm : schema) {
					// Lookup the index of this column
					Integer columnIndex = columnIndexMap.get(cm.getId());

					String stringValue = (columnIndex == null) ? cm.getDefaultValue() : row.getValues().get(columnIndex);
					Object value = parseValueForDB(cm.getColumnType(), stringValue);

					String columnName = getColumnNameForId(cm.getId());
					switch (cm.getColumnType()) {
					case DOUBLE:
						Double doubleValue = (Double) value;
						String extraColumnName = TableConstants.DOUBLE_PREFIX + columnName;
						if (doubleValue != null && doubleValue.isNaN()) {
							rowMap.put(columnName, null);
							rowMap.put(extraColumnName, DOUBLE_NAN);
						} else if (doubleValue != null && doubleValue.isInfinite()) {
							if (doubleValue < 0) {
								rowMap.put(columnName, "-1.7976931348623157E+308");
								rowMap.put(extraColumnName, DOUBLE_NEGATIVE_INFINITY);
							} else {
								rowMap.put(columnName, "1.7976931348623157E+308");
								rowMap.put(extraColumnName, DOUBLE_POSITIVE_INFINITY);
							}
						} else {
							rowMap.put(columnName, value);
							rowMap.put(extraColumnName, null);
						}
						break;
					default:
						rowMap.put(columnName, value);
						break;
					}
				}
				results.add(new MapSqlParameterSource(rowMap));
			}
		}
		return results.toArray(new MapSqlParameterSource[results.size()]);
	}
	
	/**
	 * Build the parameters that will bind the passed RowSet to a SQL statement.
	 * 
	 * @param toBind
	 * @param schema
	 * @return
	 */
	public static SqlParameterSource bindParameterForDelete(RowSet toBind, List<ColumnModel> schema) {
		List<Long> rowIds = Lists.newArrayList();
		for (Row row : toBind.getRows()) {
			if (TableModelUtils.isDeletedRow(row)) {
				rowIds.add(row.getRowId());
			}
		}
		if (rowIds.isEmpty()) {
			return null;
		} else {
			return new MapSqlParameterSource(Collections.singletonMap(ROW_ID_BIND, rowIds));
		}
	}

	/**
	 * Create the SQL used to get the number of rows in an index table.
	 * 
	 * @return
	 */
	public static String getCountSQL(String tableId){
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT COUNT(").append(ROW_ID).append(") FROM ").append(getTableNameForId(tableId, TableType.INDEX));
		return builder.toString();
	}
	
	/**
	 * Create the SQL used to get the max version number from a table.
	 * @return
	 */
	public static String getStatusMaxVersionSQL(String tableId) {
		return "SELECT " + ROW_VERSION + " FROM " + getTableNameForId(tableId, TableType.STATUS);
	}

	/**
	 * Create SQL used to get the current schema hash of a table.
	 * @param tableId
	 * @return
	 */
	public static String getSchemaHashSQL(String tableId) {
		return "SELECT " + SCHEMA_HASH + " FROM " + getTableNameForId(tableId, TableType.STATUS);
	}
	
	public static String selectRowValuesForRowId(Long tableId) {
		return "SELECT * FROM " + getTableNameForId(tableId, TableType.INDEX) + " WHERE " + ROW_ID + " IN ( :" + ROW_ID_BIND + " )";
	}
	/**
	 * Insert ignore file handle ids into a table's secondary file index.
	 * @param tableId
	 * @return
	 */
	public static String createSQLInsertIgnoreFileHandleId(String tableId){
		return "INSERT IGNORE INTO "+getTableNameForId(tableId, TableType.FILE_IDS)+" ("+FILE_ID+") VALUES(?)";
	}
	
	/**
	 * SQL for finding all file handle ids bound to a table that are included in the provided set.
	 * @param tableId
	 * @return
	 */
	public static String createSQLGetBoundFileHandleId(String tableId){
		return "SELECT "+FILE_ID+" FROM "+getTableNameForId(tableId, TableType.FILE_IDS)+" WHERE "+FILE_ID+" IN( :"+FILE_ID_BIND+")";
	}
	
	/**
	 * Select distinct values from the given column ID.
	 * 
	 * @param tableId
	 * @param columnId
	 * @return
	 */
	public static String createSQLGetDistinctValues(String tableId, String columnId){
		return "SELECT DISTINCT "+getColumnNameForId(columnId)+" FROM "+getTableNameForId(tableId, TableType.INDEX);
	}
	
	/**
	 * SQL to create a table if it does not exist.
	 * @param tableId
	 * @return
	 */
	public static String createTableIfDoesNotExistSQL(String tableId){
		StringBuilder builder = new StringBuilder();
		builder.append("CREATE TABLE IF NOT EXISTS ");
		builder.append(getTableNameForId(tableId, TableType.INDEX));
		builder.append("( ");
		builder.append(ROW_ID).append(" bigint(20) NOT NULL, ");
		builder.append(ROW_VERSION).append(" bigint(20) NOT NULL, ");
		builder.append("PRIMARY KEY (").append("ROW_ID").append(")");
		builder.append(")");
		return builder.toString();
	}

	/**
	 * Create an alter table SQL statement for the given set of column changes.
	 * 
	 * @param changes
	 * @return
	 */
	public static String createAlterTableSql(List<ColumnChange> changes, String tableId){
		StringBuilder builder = new StringBuilder();
		builder.append("ALTER TABLE ");
		builder.append(getTableNameForId(tableId, TableType.INDEX));
		builder.append(" ");
		boolean isFirst = true;
		boolean hasChanges = false;
		for(ColumnChange change: changes){
			if(!isFirst){
				builder.append(", ");
			}
			boolean hasChange = appendAlterTableSql(builder, change);
			if(hasChange){
				hasChanges = true;
			}
			isFirst = false;
		}
		// return null if there is nothing to do.
		if(!hasChanges){
			return null;
		}
		return builder.toString();
	}
	
	/**
	 * Alter a single column for a given column change.
	 * @param builder
	 * @param change
	 */
	public static boolean appendAlterTableSql(StringBuilder builder,
			ColumnChange change) {
		if(change.getOldColumn() == null && change.getNewColumn() == null){
			// nothing to do
			return false;
		}
		if(change.getOldColumn() == null){
			// add
			appendAddColumn(builder, change.getNewColumn());
			// change was added.
			return true;
		}

		if(change.getNewColumn() == null){
			// delete
			appendDeleteColumn(builder, change.getOldColumn());
			// change was added.
			return true;
		}

		if (change.getNewColumn().equals(change.getOldColumn())) {
			// both columns are the same so do nothing.
			return false;
		}
		// update
		appendUpdateColumn(builder, change);
		// change was added.
		return true;

	}
	
	/**
	 * Append an add column statement to the passed builder.
	 * @param builder
	 * @param newColumn
	 */
	public static void appendAddColumn(StringBuilder builder,
			ColumnModel newColumn) {
		ValidateArgument.required(newColumn, "newColumn");
		builder.append("ADD COLUMN ");
		appendColumnDefinition(builder, newColumn);
		// doubles use two columns.
		if(ColumnType.DOUBLE.equals(newColumn.getColumnType())){
			appendAddDoubleEnum(builder, newColumn.getId());
		}
	}
	
	/**
	 * Append a delete column statement to the passed builder.
	 * @param builder
	 * @param oldColumn
	 */
	public static void appendDeleteColumn(StringBuilder builder,
			ColumnModel oldColumn) {
		ValidateArgument.required(oldColumn, "oldColumn");
		builder.append("DROP COLUMN ");
		builder.append(getColumnNameForId(oldColumn.getId()));
		// doubles use two columns.
		if(ColumnType.DOUBLE.equals(oldColumn.getColumnType())){
			appendDropDoubleEnum(builder, oldColumn.getId());
		}
	}
	
	/**
	 * Append an update column statement to the passed builder.
	 * @param builder
	 * @param change
	 */
	public static void appendUpdateColumn(StringBuilder builder,
			ColumnChange change) {
		ValidateArgument.required(change, "change");
		ValidateArgument.required(change.getOldColumn(), "change.getOldColumn()");
		ValidateArgument.required(change.getNewColumn(), "change.getNewColumn()");
		builder.append("CHANGE COLUMN ");
		builder.append(getColumnNameForId(change.getOldColumn().getId()));
		builder.append(" ");
		appendColumnDefinition(builder, change.getNewColumn());
		// Is this a type change?
		if(!change.getOldColumn().getColumnType().equals(change.getNewColumn().getColumnType())){
			if(ColumnType.DOUBLE.equals(change.getOldColumn().getColumnType())){
				// The old type is a double so remove the double enumeration column
				appendDropDoubleEnum(builder, change.getOldColumn().getId());
			}else if(ColumnType.DOUBLE.equals(change.getNewColumn().getColumnType())){
				// the new type is a double so add the double enumeration column
				appendAddDoubleEnum(builder, change.getNewColumn().getId());
			}
		}
		// are both columns a double?
		if(ColumnType.DOUBLE.equals(change.getOldColumn().getColumnType())
				&& ColumnType.DOUBLE.equals(change.getNewColumn().getColumnType())){
			appendRenameDoubleEnum(builder, change.getOldColumn().getId(), change.getNewColumn().getId());
		}
	}
	
	/**
	 * Append a column type definition to the passed builder.
	 * 
	 * @param builder
	 * @param column
	 */
	public static void appendColumnDefinition(StringBuilder builder, ColumnModel column){
		builder.append(getColumnNameForId(column.getId()));
		builder.append(" ");
		ColumnTypeInfo info = ColumnTypeInfo.getInfoForType(column.getColumnType());
		builder.append(info.toSql(column.getMaximumSize(), column.getDefaultValue()));
	}

	/**
	 * Append the SQL to add a double enumeration column for the given column.
	 * @param builder
	 * @param newColumn
	 */
	public static void appendAddDoubleEnum(StringBuilder builder,
			String columnId) {
		builder.append(", ADD COLUMN ");
		builder.append(TableConstants.DOUBLE_PREFIX );
		builder.append(getColumnNameForId(columnId));
		builder.append(DOUBLE_ENUM_CLAUSE);
	}

	/**
	 * Append drop double enumeration column for the given column id.
	 * @param builder
	 * @param oldColumn
	 */
	public static void appendDropDoubleEnum(StringBuilder builder,
			String columnId) {
		builder.append(", DROP COLUMN ");
		builder.append(TableConstants.DOUBLE_PREFIX );
		builder.append(getColumnNameForId(columnId));
	}
	
	/**
	 * Append a the rename of a double enumeration column.
	 * @param builder
	 * @param oldId
	 * @param newId
	 */
	public static void appendRenameDoubleEnum(StringBuilder builder, String oldId, String newId){
		builder.append(", CHANGE COLUMN ");
		builder.append(TableConstants.DOUBLE_PREFIX );
		builder.append(getColumnNameForId(oldId));
		builder.append(" ");
		builder.append(TableConstants.DOUBLE_PREFIX );
		builder.append(getColumnNameForId(newId));
		builder.append(DOUBLE_ENUM_CLAUSE);
	}

	/**
	 * Create the SQL to truncate the given table.
	 * @param tableId
	 * @return
	 */
	public static String createTruncateSql(String tableId) {
		return "TRUNCATE TABLE "+getTableNameForId(tableId, TableType.INDEX);
	}

	
	/**
	 * A single SQL statement to get the cardinality of each column as a single call.
	 * 
	 * @param list
	 * @param tableId
	 * @return
	 */
	public static String createCardinalitySql(List<DatabaseColumnInfo> list, String tableId){
		if(list.isEmpty()){
			return null;
		}
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT ");
		boolean isFirst = true;
		for(DatabaseColumnInfo info: list){
			if(!isFirst){
				builder.append(", ");
			}
			builder.append("COUNT(DISTINCT ");
			builder.append(info.getColumnName());
			builder.append(") AS ");
			builder.append(info.getColumnName());
			isFirst = false;
		}
		builder.append(" FROM ");
		builder.append(getTableNameForId(tableId, TableType.INDEX));
		return builder.toString();
	}
	
	/**
	 * Create SQL to alter the table to add, remove, and rename column indices.  This method
	 * will insure that columns with high cardinality are given an index before columns with
	 * a low cardinality while ensuring the maximum number of indices is respected for each table.
	 * 
	 * @param list
	 * @param tableId
	 * @return
	 */
	public static String createOptimizedAlterIndices(List<DatabaseColumnInfo> list, String tableId, int maxNumberOfIndex){
		IndexChange change = calculateIndexOptimization(list, tableId, maxNumberOfIndex);
		return createAlterIndices(change, tableId);
	}
	
	/**
	 * Create an IndexChange to add, remove, and rename column indices.  This method
	 * will insure that columns with high cardinality are given an index before columns with
	 * a low cardinality while ensuring the ma
	 * ximum number of indices is respected for each table.
	 * 
	 * @param list
	 * @param tableId
	 * @return
	 */
	public static IndexChange calculateIndexOptimization(List<DatabaseColumnInfo> list, String tableId, int maxNumberOfIndex){
		// us a copy of the list
		list = new LinkedList<DatabaseColumnInfo>(list);
		// sort by cardinality descending		
		Collections.sort(list, Collections.reverseOrder(DatabaseColumnInfo.CARDINALITY_COMPARATOR));
		List<DatabaseColumnInfo> toAdd = new LinkedList<DatabaseColumnInfo>();
		List<DatabaseColumnInfo> toRemove = new LinkedList<DatabaseColumnInfo>();
		List<DatabaseColumnInfo> toRename = new LinkedList<DatabaseColumnInfo>();
		
		int indexCount = 1;
		for(DatabaseColumnInfo info: list){
			// ignore row_id and version
			if(info.isRowIdOrVersion()){
				continue;
			}
			if(indexCount < maxNumberOfIndex){
				// Still under the max.
				indexCount++;
				if(!info.hasIndex()){
					toAdd.add(info);
				}else{
					// does the index need to be renamed?
					String expectedIndexName = getIndexName(info.getColumnName());
					if(!expectedIndexName.equals(info.getIndexName())){
						toRename.add(info);
					}
				}
			}else{
				// over the max
				if(info.hasIndex()){
					toRemove.add(info);
				}
			}
		}
		return new IndexChange(toAdd, toRemove, toRename);
	}
	
	
	/**
	 * Create the alter table SQL for the given index change.
	 * 
	 * @param change
	 * @param tableId
	 * @return
	 */
	public static String createAlterIndices(IndexChange change, String tableId){
		ValidateArgument.required(change, "change");
		ValidateArgument.required(tableId, "tableId");
		
		if(change.getToAdd().isEmpty()
				&& change.getToRemove().isEmpty()
				&& change.getToRename().isEmpty()){
			// nothing to do.
			return null;
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append("ALTER TABLE ");
		builder.append(getTableNameForId(tableId, TableType.INDEX));
		builder.append(" ");
		boolean isFirst = true;
		//deletes first
		for(DatabaseColumnInfo info: change.getToRemove()){
			if(!isFirst){
				builder.append(", ");
			}
			appendDropIndex(builder, info);		
			isFirst = false;
		}
		// renames
		for(DatabaseColumnInfo info: change.getToRename()){
			if(!isFirst){
				builder.append(", ");
			}
			// for MySQL 5.6 rename index is not supported so drop and add.
			appendDropIndex(builder, info);		
			builder.append(", ");
			appendAddIndex(builder, info);
			isFirst = false;
		}
		// adds
		for(DatabaseColumnInfo info: change.getToAdd()){
			if(!isFirst){
				builder.append(", ");
			}
			appendAddIndex(builder, info);
			isFirst = false;
		}
		return builder.toString();
	}
	
	private static void appendDropIndex(StringBuilder builder, DatabaseColumnInfo info){
		builder.append("DROP INDEX ");
		builder.append(info.getIndexName());	
	}
	
	private static void appendAddIndex(StringBuilder builder, DatabaseColumnInfo info){
		builder.append("ADD INDEX ");
		info.setIndexName(getIndexName(info.getColumnName()));
		builder.append(info.createIndexDefinition());	
	}
	
	/**
	 * Get the name of an index from the columnId.
	 * @param columnId
	 * @return
	 */
	public static String getIndexName(String columnId){
		return columnId+IDX;
	}
	
	/**
	 * Create a list of ColumnChanges to replace the current schema represented by the given
	 * list of Database with a new schema represented by the given ColumnModels.
	 * @param infoList
	 * @param newSchema
	 * @return
	 */
	public static List<ColumnChange> createReplaceSchemaChange(List<DatabaseColumnInfo> infoList, List<ColumnModel> newSchema){
		List<ColumnModel> oldColumnIds = extractSchemaFromInfo(infoList);
		return createReplaceSchemaChangeIds(oldColumnIds, newSchema);
	}
	
	/**
	 * Create a replace schema change.
	 * Any column in the current schema that is not in the schema will be removed.
	 * Any column in the new schema that is not in the old schema will be added.
	 * Any column in both the current and new will be left unchanged.
	 * 
	 * @param currentInfo
	 * @param newSchema
	 * @return
	 */
	public static List<ColumnChange> createReplaceSchemaChangeIds(List<ColumnModel> currentColunm, List<ColumnModel> newSchema){
		Set<String> oldSet = createColumnIdSet(currentColunm);
		Set<String> newSet = createColumnIdSet(newSchema);
		List<ColumnChange> changes = new LinkedList<ColumnChange>();
		// remove any column in the current that is not in the new.
		for(ColumnModel oldColumn: currentColunm){
			if(!newSet.contains(oldColumn.getId())){
				// Remove this column
				ColumnModel newColumn = null;
				changes.add(new ColumnChange(oldColumn, newColumn));
			}
		}
		// Add any column in the current that is not in the old.
		for(ColumnModel newColumn: newSchema){
			if(!oldSet.contains(newColumn.getId())){
				ColumnModel oldColumn = null;
				changes.add(new ColumnChange(oldColumn, newColumn));
			}
		}
		return changes;
	}
	
	/**
	 * Extract the list of columnIds from a list of DatabaseColumnInfo.
	 * 
	 * @param infoList
	 * @return
	 */
	public static List<ColumnModel> extractSchemaFromInfo(List<DatabaseColumnInfo> infoList){
		List<ColumnModel> results = new LinkedList<ColumnModel>();
		if(infoList != null){
			for(DatabaseColumnInfo info: infoList){
				if(!info.isRowIdOrVersion()){
					if(info.getColumnType() != null){
						long columnId = getColumnId(info);
						ColumnModel cm = new ColumnModel();
						cm.setId(""+columnId);
						cm.setColumnType(info.getColumnType());
						if(info.getMaxSize() != null){
							cm.setMaximumSize(info.getMaxSize().longValue());
						}
						results.add(cm);
					}
				}
			}
		}
		return results;
	}
	
	/**
	 * Extract the columnId from the columnName of the given DatabaseColumnInfo.
	 * 
	 * @param info
	 * @return
	 */
	public static long getColumnId(DatabaseColumnInfo info){
		ValidateArgument.required(info, "DatabaseColumnInfo");
		ValidateArgument.required(info.getColumnName(), "DatabaseColumnInfo.columnName()");
		String columnName = info.getColumnName();
		try {
			return Long.parseLong(columnName.substring(COLUMN_PREFIX.length(), columnName.length()-COLUMN_POSTFIX.length()));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Unexpected columnName: "+info.getColumnName());
		}
	}
	
	/**
	 * The name of the temporary table for the given table Id.
	 * 
	 * @param tableId
	 * @return
	 */
	public static String getTemporaryTableName(String tableId){
		return TEMP+KeyFactory.stringToKey(tableId);
	}

	/**
	 * Create the SQL used to create a temporary table 
	 * @param tableId
	 * @return
	 */
	public static String createTempTableSql(String tableId) {
		String tableName = getTableNameForId(tableId, TableType.INDEX);
		String tempName = getTemporaryTableName(tableId);
		return String.format(CREATE_TABLE_LIKE, tempName, tableName);
	}
	
	
	/**
	 * Create the SQL used to copy all of the data from a table to the temp table.
	 * 
	 * @param tableId
	 * @return
	 */
	public static String copyTableToTempSql(String tableId){
		String tableName = getTableNameForId(tableId, TableType.INDEX);
		String tempName = getTemporaryTableName(tableId);
		return String.format(SQL_COPY_TABLE_TO_TEMP, tempName, tableName);
	}
	
	/**
	 * SQL to count the rows in temp table.
	 * @param tableId
	 * @return
	 */
	public static String countTempRowsSql(String tableId){
		String tempName = getTemporaryTableName(tableId);
		return SELECT_COUNT_FROM_TEMP+tempName;
	}
	
	/**
	 *SQL to delete a temp table.
	 * @param tableId
	 * @return
	 */
	public static String deleteTempTableSql(String tableId){
		String tempName = getTemporaryTableName(tableId);
		return String.format(DROP_TABLE_IF_EXISTS, tempName);
	}
}
