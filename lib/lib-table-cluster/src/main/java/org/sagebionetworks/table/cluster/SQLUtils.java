package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_ALIAS;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_DOUBLE_ABSTRACT;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_ENTITY_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_KEY;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_TABLE;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_ALIAS;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_BENEFACTOR_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_ETAG;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_VERSION;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_TABLE;
import static org.sagebionetworks.repo.model.table.TableConstants.FILE_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_BENEFACTOR;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ETAG;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;
import static org.sagebionetworks.repo.model.table.TableConstants.SCHEMA_HASH;
import static org.sagebionetworks.repo.model.table.TableConstants.SINGLE_KEY;
import static org.sagebionetworks.table.cluster.utils.ColumnConstants.isTableTooLargeForFourByteUtf8;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.AbstractDouble;
import org.sagebionetworks.repo.model.table.AnnotationDTO;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.EntityField;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.repo.model.table.parser.AllLongTypeParser;
import org.sagebionetworks.repo.model.table.parser.BooleanParser;
import org.sagebionetworks.repo.model.table.parser.DoubleParser;
import org.sagebionetworks.table.model.Grouping;
import org.sagebionetworks.table.model.SparseRow;
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


	private static final String EMPTY_STRING = "";
	private static final String ABSTRACT_DOUBLE_ALIAS_PREFIX = "_DBL";
	private static final String TEMPLATE_MAX_ANNOTATION_SELECT = ", MAX(IF(%1$s.%2$s ='%3$s', %1$s.%4$s, NULL)) AS %5$s%6$s";
	private static final String TEMPLATE_MAX_ENTITY_SELECT = ", MAX(%1$s.%2$s) AS %2$s";
	private static final String DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS %1$S";
	private static final String SELECT_COUNT_FROM_TEMP = "SELECT COUNT(*) FROM ";
	private static final String SQL_COPY_TABLE_TO_TEMP = "INSERT INTO %1$S SELECT * FROM %2$S ORDER BY "+ROW_ID;
	private static final String CREATE_TABLE_LIKE = "CREATE TABLE %1$S LIKE %2$S";
	private static final String TEMP = "TEMP";
	private static final String IDX = "idx_";
	public static final String FILE_ID_BIND = "bFIds";
	public static final String ROW_ID_BIND = "bRI";
	public static final String ROW_VERSION_BIND = "bRV";
	public static final String DEFAULT = "DEFAULT";
	public static final String TABLE_PREFIX = "T";
	public static final String COLUMN_PREFIX = "_C";
	public static final String COLUMN_POSTFIX = "_";

	private static final String DOUBLE_NAN = Double.toString(Double.NaN);
	private static final String DOUBLE_POSITIVE_INFINITY = Double.toString(Double.POSITIVE_INFINITY);
	private static final String DOUBLE_NEGATIVE_INFINITY = Double.toString(Double.NEGATIVE_INFINITY);
	private static final String DOUBLE_ENUM_CLAUSE = " ENUM ('" + DOUBLE_NAN + "', '" + DOUBLE_POSITIVE_INFINITY + "', '"
			+ DOUBLE_NEGATIVE_INFINITY + "') DEFAULT null";
	
	
	public enum TableType {
		/**
		 * The index tables
		 */
		INDEX(EMPTY_STRING),
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
	public static String createTableSQL(IdAndVersion tableId, TableType type) {
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

	private static String createTableSQL(IdAndVersion tableId, TableType type, String columnDefinitions) {
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
		return info.parseValueForDatabaseWrite(value);
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
	public static String getTableNameForId(IdAndVersion id, TableType type) {
		if (id == null) {
			throw new IllegalArgumentException("Table ID cannot be null");			
		}
		StringBuilder builder = new StringBuilder(TABLE_PREFIX);
		builder.append(id.getId());
		if(id.getVersion().isPresent()) {
			builder.append("_").append(id.getVersion().get());
		}
		builder.append(type.getTablePostFix());
		return builder.toString();
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
		String subName = EMPTY_STRING;
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
	public static String dropTableSQL(IdAndVersion tableId, TableType type) {
		String tableName = getTableNameForId(tableId, type);
		return "DROP TABLE " + tableName;
	}

	/**
	 * Build the create or update statement for inserting rows into a table.
	 * @param schema
	 * @param tableId
	 * @return
	 */
	public static String buildCreateOrUpdateRowSQL(List<ColumnModel> schema, IdAndVersion tableId){
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
	public static String buildCreateOrUpdateStatusSQL(IdAndVersion tableId) {
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
	
	public static String buildCreateOrUpdateStatusHashSQL(IdAndVersion tableId) {
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
	
	public static String buildCreateOrUpdateStatusVersionAndHashSQL(IdAndVersion tableId){
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
		builder.append(" ) VALUES ('1', ?, ? ) ON DUPLICATE KEY UPDATE "+ROW_VERSION+" = ?, "+SCHEMA_HASH+" = ? ");
		return builder.toString();
	}

	/**
	 * Build the delete statement for inserting rows into a table.
	 * 
	 * @param schema
	 * @param tableId
	 * @return
	 */
	public static String buildDeleteSQL(IdAndVersion tableId){
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
	public static SqlParameterSource[] bindParametersForCreateOrUpdate(Grouping grouping){
		// We will need a binding for every row
		List<MapSqlParameterSource> results = new LinkedList<MapSqlParameterSource>();
		for(SparseRow row: grouping.getRows()){
			if (!row.isDelete()) {
				Map<String, Object> rowMap = new HashMap<String, Object>(grouping.getColumnsWithValues().size() + 2);
				// Always bind the row ID and version
				if (row.getRowId() == null)
					throw new IllegalArgumentException("RowID cannot be null");
				if (row.getVersionNumber() == null)
					throw new IllegalArgumentException("RowVersionNumber cannot be null");
				rowMap.put(ROW_ID_BIND, row.getRowId());
				rowMap.put(ROW_VERSION_BIND, row.getVersionNumber());
				// Bind each column
				for (ColumnModel cm : grouping.getColumnsWithValues()) {
					String stringValue = row.getCellValue(cm.getId());
					Object value = parseValueForDB(cm.getColumnType(), stringValue);

					String columnName = getColumnNameForId(cm.getId());
					switch (cm.getColumnType()) {
					case DOUBLE:
						String doubleEnumerationName = TableConstants.DOUBLE_PREFIX + columnName;
						Double doubleValue = (Double) value;
						if(AbstractDouble.isAbstractValue(doubleValue)){
							// Abstract double include NaN and +/- Infinity.
							AbstractDouble type = AbstractDouble.lookupType(doubleValue);
							// an approximation is used for the double column.
							rowMap.put(columnName, type.getApproximateValue());
							// Each abstract value has its own enumeration value.
							rowMap.put(doubleEnumerationName, type.getEnumerationValue());
						}else{
							// Non-abstract doubles are used as-is.
							rowMap.put(columnName, value);
							// Non-abstract doubles have a null value for the double enumeration column.
							rowMap.put(doubleEnumerationName, null);
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
	public static SqlParameterSource bindParameterForDelete(List<SparseRow> toBind) {
		List<Long> rowIds = Lists.newArrayList();
		for (SparseRow row : toBind) {
			ValidateArgument.requirement(row.isDelete(), "Expected only a delete row");
			if (row.isDelete()) {
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
	public static String getCountSQL(IdAndVersion tableId){
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT COUNT(").append(ROW_ID).append(") FROM ").append(getTableNameForId(tableId, TableType.INDEX));
		return builder.toString();
	}
	
	/**
	 * Create the SQL used to get the max version number from a table.
	 * @return
	 */
	public static String getStatusMaxVersionSQL(IdAndVersion tableId) {
		return "SELECT " + ROW_VERSION + " FROM " + getTableNameForId(tableId, TableType.STATUS);
	}

	/**
	 * Create SQL used to get the current schema hash of a table.
	 * @param tableId
	 * @return
	 */
	public static String getSchemaHashSQL(IdAndVersion tableId) {
		return "SELECT " + SCHEMA_HASH + " FROM " + getTableNameForId(tableId, TableType.STATUS);
	}
	
	/**
	 * Insert ignore file handle ids into a table's secondary file index.
	 * @param tableId
	 * @return
	 */
	public static String createSQLInsertIgnoreFileHandleId(IdAndVersion tableId){
		return "INSERT IGNORE INTO "+getTableNameForId(tableId, TableType.FILE_IDS)+" ("+FILE_ID+") VALUES(?)";
	}
	
	/**
	 * SQL for finding all file handle ids bound to a table that are included in the provided set.
	 * @param tableId
	 * @return
	 */
	public static String createSQLGetBoundFileHandleId(IdAndVersion tableId){
		return "SELECT "+FILE_ID+" FROM "+getTableNameForId(tableId, TableType.FILE_IDS)+" WHERE "+FILE_ID+" IN( :"+FILE_ID_BIND+")";
	}
	
	/**
	 * Select distinct values from the given column ID.
	 * 
	 * @param tableId
	 * @param columnName
	 * @return
	 */
	public static String createSQLGetDistinctValues(IdAndVersion tableId, String columnName){
		return "SELECT DISTINCT "+columnName+" FROM "+getTableNameForId(tableId, TableType.INDEX);
	}
	
	/**
	 * SQL to create a table if it does not exist.
	 * @param tableId
	 * @return
	 */
	public static String createTableIfDoesNotExistSQL(IdAndVersion tableId, boolean isView){
		StringBuilder builder = new StringBuilder();
		builder.append("CREATE TABLE IF NOT EXISTS ");
		builder.append(getTableNameForId(tableId, TableType.INDEX));
		builder.append("( ");
		builder.append(ROW_ID).append(" bigint(20) NOT NULL, ");
		builder.append(ROW_VERSION).append(" bigint(20) NOT NULL, ");
		if(isView){
			builder.append(ROW_ETAG).append(" varchar(36) NOT NULL, ");
			builder.append(ROW_BENEFACTOR).append(" bigint(20) NOT NULL, ");
		}
		builder.append("PRIMARY KEY (").append("ROW_ID").append(")");
		if(isView){
			builder.append(", KEY `IDX_ETAG` (").append(ROW_ETAG).append(")");
			builder.append(", KEY `IDX_BENEFACTOR` (").append(ROW_BENEFACTOR).append(")");
		}
		builder.append(")");
		return builder.toString();
	}

	/**
	 * Create an alter table SQL statement for the given set of column changes.
	 * 
	 * @param changes
	 * @return
	 */
	public static String createAlterTableSql(List<ColumnChangeDetails> changes, IdAndVersion tableId, boolean alterTemp){
		StringBuilder builder = new StringBuilder();
		builder.append("ALTER TABLE ");
		if(alterTemp){
			builder.append(getTemporaryTableName(tableId));
		}else{
			builder.append(getTableNameForId(tableId, TableType.INDEX));
		}
		builder.append(" ");
		boolean isFirst = true;
		boolean hasChanges = false;
		for(ColumnChangeDetails change: changes){
			boolean useDepricatedUtf8ThreeBytes = isTableTooLargeForFourByteUtf8(tableId.getId());
			boolean hasChange = appendAlterTableSql(builder, change, isFirst, useDepricatedUtf8ThreeBytes);
			if(hasChange){
				hasChanges = true;
				isFirst = false;
			}
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
	 * @param useDepricatedUtf8ThreeBytes Should only be set to true for the few old
	 * tables that are too large to build with the correct 4 byte UTF-8.
	 */
	public static boolean appendAlterTableSql(StringBuilder builder,
			ColumnChangeDetails change, boolean isFirst, boolean useDepricatedUtf8ThreeBytes) {
		if(change.getOldColumn() == null && change.getNewColumn() == null){
			// nothing to do
			return false;
		}
		if(change.getOldColumn() == null){
			// add
			appendAddColumn(builder, change.getNewColumn(), isFirst, useDepricatedUtf8ThreeBytes);
			// change was added.
			return true;
		}

		if(change.getNewColumn() == null){
			// delete
			appendDeleteColumn(builder, change.getOldColumn(), isFirst);
			// change was added.
			return true;
		}

		if (change.getNewColumn().equals(change.getOldColumn())) {
			// both columns are the same so do nothing.
			return false;
		}
		// update
		appendUpdateColumn(builder, change, isFirst, useDepricatedUtf8ThreeBytes);
		// change was added.
		return true;

	}
	
	/**
	 * Append an add column statement to the passed builder.
	 * @param builder
	 * @param newColumn
	 * @param useDepricatedUtf8ThreeBytes Should only be set to true for the few old
	 * tables that are too large to build with the correct 4 byte UTF-8.
	 */
	public static void appendAddColumn(StringBuilder builder,
			ColumnModel newColumn, boolean isFirst, boolean useDepricatedUtf8ThreeBytes) {
		ValidateArgument.required(newColumn, "newColumn");
		if(!isFirst){
			builder.append(", ");
		}
		builder.append("ADD COLUMN ");
		appendColumnDefinition(builder, newColumn, useDepricatedUtf8ThreeBytes);
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
			ColumnModel oldColumn, boolean isFirst) {
		if(!isFirst){
			builder.append(", ");
		}
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
	 * @param useDepricatedUtf8ThreeBytes Should only be set to true for the few old
	 * tables that are too large to build with the correct 4 byte UTF-8.
	 */
	public static void appendUpdateColumn(StringBuilder builder,
			ColumnChangeDetails change, boolean isFirst, boolean useDepricatedUtf8ThreeBytes) {
		ValidateArgument.required(change, "change");
		ValidateArgument.required(change.getOldColumn(), "change.getOldColumn()");
		ValidateArgument.required(change.getOldColumnInfo(), "change.getOldColumnInfo()");
		ValidateArgument.required(change.getNewColumn(), "change.getNewColumn()");
		if(!isFirst){
			builder.append(", ");
		}
		
		if(change.getOldColumnInfo().hasIndex()){
			// drop the index on the old column before changing the column.
			ValidateArgument.required(change.getOldColumnInfo().getIndexName(), "change.getOldColumnInfo().getIndexName");
			appendDropIndex(builder, change.getOldColumnInfo());	
			builder.append(", ");
		}
		builder.append("CHANGE COLUMN ");
		builder.append(getColumnNameForId(change.getOldColumn().getId()));
		builder.append(" ");
		appendColumnDefinition(builder, change.getNewColumn(), useDepricatedUtf8ThreeBytes);
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
	 * @param builder
	 * @param column
	 * @param useDepricatedUtf8ThreeBytes Should only be set to true for the few old
	 * tables that are too large to build with the correct 4 byte UTF-8.
	 */
	public static void appendColumnDefinition(StringBuilder builder, ColumnModel column , boolean useDepricatedUtf8ThreeBytes){
		builder.append(getColumnNameForId(column.getId()));
		builder.append(" ");
		ColumnTypeInfo info = ColumnTypeInfo.getInfoForType(column.getColumnType());
		builder.append(info.toSql(column.getMaximumSize(), column.getDefaultValue(), useDepricatedUtf8ThreeBytes));
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
	public static String createTruncateSql(IdAndVersion tableId) {
		return "TRUNCATE TABLE "+getTableNameForId(tableId, TableType.INDEX);
	}

	
	/**
	 * A single SQL statement to get the cardinality of each column as a single call.
	 * 
	 * @param list
	 * @param tableId
	 * @return
	 */
	public static String createCardinalitySql(List<DatabaseColumnInfo> list, IdAndVersion tableId){
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
	public static String createOptimizedAlterIndices(List<DatabaseColumnInfo> list, IdAndVersion tableId, int maxNumberOfIndex){
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
	public static IndexChange calculateIndexOptimization(List<DatabaseColumnInfo> list, IdAndVersion tableId, int maxNumberOfIndex){
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
			if(info.isMetadata()){
				continue;
			}
			// do not index blobs.
			if(MySqlColumnType.MEDIUMTEXT.equals(info.getType())) {
				// remove the index if it has one
				if(info.hasIndex()){
					toRemove.add(info);
				}
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
	public static String createAlterIndices(IndexChange change, IdAndVersion tableId){
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
	public static List<ColumnChangeDetails> createReplaceSchemaChange(List<DatabaseColumnInfo> infoList, List<ColumnModel> newSchema){
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
	public static List<ColumnChangeDetails> createReplaceSchemaChangeIds(List<ColumnModel> currentColunm, List<ColumnModel> newSchema){
		Set<String> oldSet = createColumnIdSet(currentColunm);
		Set<String> newSet = createColumnIdSet(newSchema);
		List<ColumnChangeDetails> changes = new LinkedList<ColumnChangeDetails>();
		// remove any column in the current that is not in the new.
		for(ColumnModel oldColumn: currentColunm){
			if(!newSet.contains(oldColumn.getId())){
				// Remove this column
				ColumnModel newColumn = null;
				changes.add(new ColumnChangeDetails(oldColumn, newColumn));
			}
		}
		// Add any column in the current that is not in the old.
		for(ColumnModel newColumn: newSchema){
			if(!oldSet.contains(newColumn.getId())){
				ColumnModel oldColumn = null;
				changes.add(new ColumnChangeDetails(oldColumn, newColumn));
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
				if(!info.isMetadata()){
					if(info.getColumnType() != null){
						long columnId = getColumnId(info);
						ColumnModel cm = new ColumnModel();
						cm.setId(EMPTY_STRING+columnId);
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
	public static String getTemporaryTableName(IdAndVersion tableId){
		return TEMP+getTableNameForId(tableId, TableType.INDEX);
	}

	/**
	 * Create the SQL used to create a temporary table 
	 * @param tableId
	 * @return
	 */
	public static String createTempTableSql(IdAndVersion tableId) {
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
	public static String copyTableToTempSql(IdAndVersion tableId){
		String tableName = getTableNameForId(tableId, TableType.INDEX);
		String tempName = getTemporaryTableName(tableId);
		return String.format(SQL_COPY_TABLE_TO_TEMP, tempName, tableName);
	}
	
	/**
	 * SQL to count the rows in temp table.
	 * @param tableId
	 * @return
	 */
	public static String countTempRowsSql(IdAndVersion tableId){
		String tempName = getTemporaryTableName(tableId);
		return SELECT_COUNT_FROM_TEMP+tempName;
	}
	
	/**
	 *SQL to delete a temp table.
	 * @param tableId
	 * @return
	 */
	public static String deleteTempTableSql(IdAndVersion tableId){
		String tempName = getTemporaryTableName(tableId);
		return String.format(DROP_TABLE_IF_EXISTS, tempName);
	}

	/**
	 * Translate a list of ColumnModels to a list of ColumnMetadata.
	 * @param currentSchema
	 * @return
	 */
	public static List<ColumnMetadata> translateColumns(List<ColumnModel> currentSchema){
		List<ColumnMetadata> list = new LinkedList<ColumnMetadata>();
		for(int i=0; i<currentSchema.size(); i++){
			ColumnModel cm = currentSchema.get(i);
			list.add(translateColumns(cm, i));
		}
		return list;
	}
	
	/**
	 * Translate a single ColumnModel into ColumnMetadata.
	 * @param model
	 * @param index
	 * @return
	 */
	public static ColumnMetadata translateColumns(ColumnModel model, int index){
		// First determine if this an entity column or an annotation
		EntityField entityField = EntityField.findMatch(model);
		String tableAlias;
		String selectColumnName;
		String columnNameForId = getColumnNameForId(model.getId());
		AnnotationType annotationType = null;
		if(entityField != null){
			tableAlias = TableConstants.ENTITY_REPLICATION_ALIAS;
			selectColumnName = entityField.getDatabaseColumnName();
		}else{
			tableAlias = TableConstants.ANNOTATION_REPLICATION_ALIAS+index;
			selectColumnName = translateColumnTypeToAnnotationValueName(model.getColumnType());
			annotationType = translateColumnTypeToAnnotationType(model.getColumnType());
		}
		return new ColumnMetadata(model, entityField, tableAlias, selectColumnName, columnNameForId, index, annotationType);
	}
	
	/**
	 * Translate form ColumnType to AnnotationType;
	 * @param type
	 * @return
	 */
	public static AnnotationType translateColumnTypeToAnnotationType(ColumnType type){
		switch(type){
		case STRING:
			return AnnotationType.STRING;
		case DATE:
			return AnnotationType.DATE;
		case DOUBLE:
			return AnnotationType.DOUBLE;
		case INTEGER:
			return AnnotationType.LONG;
		default:
			return AnnotationType.STRING;
		}
	}
	
	/**
	 * Translate from a ColumnType to name of annotation value column.
	 * @param type
	 * @return
	 */
	public static String translateColumnTypeToAnnotationValueName(ColumnType type){
		switch(type){
		case DATE:
		case INTEGER:
		case ENTITYID:
		case FILEHANDLEID:
		case USERID:
			return TableConstants.ANNOTATION_REPLICATION_COL_LONG_VALUE;
		case DOUBLE:
			return TableConstants.ANNOTATION_REPLICATION_COL_DOUBLE_VALUE;
		case BOOLEAN:
			return TableConstants.ANNOTATION_REPLICATION_COL_BOOLEAN_VALUE;
		default:
			// Everything else is a string
			return TableConstants.ANNOTATION_REPLICATION_COL_STRING_VALUE;
		}
	}

	
	/**
	 * Generate the SQL used to insert select data from the entity replication tables to a
	 * table's index.
	 * @param viewId
	 * @param currentSchema
	 * @return
	 */
	public static String createSelectInsertFromEntityReplication(Long viewId, Long viewTypeMask,
			List<ColumnModel> currentSchema) {
		List<ColumnMetadata> metadata = translateColumns(currentSchema);
		StringBuilder builder = new StringBuilder();
		builder.append("INSERT INTO ");
		builder.append(getTableNameForId(IdAndVersion.newBuilder().setId(viewId).build(), TableType.INDEX));
		builder.append("(");
		buildInsertValues(builder, metadata);
		builder.append(") SELECT ");
		buildSelect(builder, metadata);
		builder.append(" FROM ");
		builder.append(ENTITY_REPLICATION_TABLE);
		builder.append(" ");
		builder.append(ENTITY_REPLICATION_ALIAS);
		builder.append(" LEFT JOIN ");
		builder.append(ANNOTATION_REPLICATION_TABLE);
		builder.append(" ").append(ANNOTATION_REPLICATION_ALIAS);
		builder.append(" ON(");
		builder.append(ENTITY_REPLICATION_ALIAS).append(".").append(ENTITY_REPLICATION_COL_ID);
		builder.append(" = ");
		builder.append(ANNOTATION_REPLICATION_ALIAS).append(".").append(ANNOTATION_REPLICATION_COL_ENTITY_ID);
		builder.append(")");
		builder.append(" WHERE ");
		builder.append(TableConstants.ENTITY_REPLICATION_ALIAS);
		builder.append(".");
		builder.append(getViewScopeFilterColumnForType(viewTypeMask));
		builder.append(" IN (:");
		builder.append(TableConstants.PARENT_ID_PARAMETER_NAME);
		builder.append(") AND ");
		builder.append(createViewTypeFilter(viewTypeMask));
		builder.append(" GROUP BY ").append(ENTITY_REPLICATION_ALIAS).append(".").append(ENTITY_REPLICATION_COL_ID);
		return builder.toString();
	}
	
	/**
	 * Filter for each view type.
	 * @param type
	 * @return
	 */
	public static String createViewTypeFilter(Long viewTypeMask){
		ValidateArgument.required(viewTypeMask, "viewTypeMask");
		StringBuilder builder = new StringBuilder();
		builder.append(TableConstants.ENTITY_REPLICATION_COL_TYPE);
		builder.append(" IN (");
		// add all types that match the given mask
		int count = 0;
		for(ViewTypeMask type: ViewTypeMask.values()) {
			if((type.getMask() & viewTypeMask) > 0) {
				if(count > 0) {
					builder.append(", ");
				}
				builder.append("'");
				builder.append(type.getEntityType().name());
				builder.append("'");
				count++;
			}
		}
		builder.append(")");
		return builder.toString();
	}
	
	/**
	 * Build the select clause of the entity replication insert select.
	 * @param builder
	 * @param metadata
	 */
	public static void buildSelect(StringBuilder builder,
			List<ColumnMetadata> metadata) {
		// select the standard entity columns.
		buildEntityReplicationSelectStandardColumns(builder);
		for(ColumnMetadata meta: metadata){
			buildSelectMetadata(builder, meta);
		}
	}
	
	/**
	 * Build a entity replication select for the given ColumnMetadata.
	 * 
	 * @param builder
	 * @param meta
	 */
	public static void buildSelectMetadata(StringBuilder builder, ColumnMetadata meta) {
		if(meta.getEntityField() != null) {
			// entity field select
			buildEntityReplicationSelect(builder, meta.getEntityField().getDatabaseColumnName());
		}else {
			// annotation select
			if (AnnotationType.DOUBLE.equals(meta.getAnnotationType())) {
				// For doubles, the double-meta columns is also selected.
				boolean isDoubleAbstract = true;
				buildAnnotationSelect(builder, meta, isDoubleAbstract);
			}
			// select the annotation
			boolean isDoubleAbstract = false;
			buildAnnotationSelect(builder, meta, isDoubleAbstract);
		}
	}
	
	/**
	 * Build the select including the standard entity columns of, id, version, etag, and benefactor..
	 * @param builder
	 */
	public static void buildEntityReplicationSelectStandardColumns(StringBuilder builder) {
		builder.append(ENTITY_REPLICATION_ALIAS);
		builder.append(".");
		builder.append(ENTITY_REPLICATION_COL_ID);
		buildEntityReplicationSelect(builder,
				ENTITY_REPLICATION_COL_VERSION,
				ENTITY_REPLICATION_COL_ETAG,
				ENTITY_REPLICATION_COL_BENEFACTOR_ID);
	}
	/**
	 * For each provided name: ', MAX(R.name) AS name'
	 * @param builder
	 * @param names
	 */
	public static void buildEntityReplicationSelect(StringBuilder builder, String...names) {
		for(String name: names) {
			builder.append(String.format(TEMPLATE_MAX_ENTITY_SELECT, ENTITY_REPLICATION_ALIAS, name));
		}
	}
	/**
	 * If isDoubleAbstract = false then builds: ', MAX(IF(A.ANNO_KEY='keyValue', A.valueColumnName, NULL)) as _columnId_'
	 * If isDoubleAbstract = true then builds: ', MAX(IF(A.ANNO_KEY='keyValue', A.DOUBLE_ABSTRACT, NULL)) as _DBL_columnId_'
	 * @param builder
	 * @param keyName
	 * @param valueName
	 * @param alias
	 */
	public static void buildAnnotationSelect(StringBuilder builder, ColumnMetadata meta, boolean isDoubleAbstract) {
		String aliasPrefix =  isDoubleAbstract ? ABSTRACT_DOUBLE_ALIAS_PREFIX: EMPTY_STRING;
		String valueColumnName = isDoubleAbstract ? ANNOTATION_REPLICATION_COL_DOUBLE_ABSTRACT : meta.getSelectColumnName();
		builder.append(String.format(TEMPLATE_MAX_ANNOTATION_SELECT,
				ANNOTATION_REPLICATION_ALIAS,
				ANNOTATION_REPLICATION_COL_KEY,
				meta.getColumnModel().getName(),
				valueColumnName,
				aliasPrefix,
				meta.getColumnNameForId()
		));
	}

	/**
	 * Build the insert clause section of entity replication insert select.
	 * 
	 * @param builder
	 * @param metadata
	 */
	public static void buildInsertValues(StringBuilder builder,
			List<ColumnMetadata> metadata) {
		builder.append(TableConstants.ROW_ID);
		builder.append(", ");
		builder.append(TableConstants.ROW_VERSION);
		builder.append(", ");
		builder.append(TableConstants.ROW_ETAG);
		builder.append(", ");
		builder.append(TableConstants.ROW_BENEFACTOR);
		for(ColumnMetadata meta: metadata){
			if (AnnotationType.DOUBLE.equals(meta.getAnnotationType())) {
				builder.append(", _DBL");
				builder.append(meta.getColumnNameForId());
			}
			builder.append(", ");
			builder.append(meta.getColumnNameForId());
		}
	}
	
	/**
	 * Create the SQL used to calculate the CRC32 of a table view.
	 * 
	 * @param viewId
	 * @param etagColumnId
	 * @return
	 */
	public static String buildTableViewCRC32Sql(Long viewId){
		String tableName = getTableNameForId(IdAndVersion.newBuilder().setId(viewId).build(), TableType.INDEX);
		return String.format(TableConstants.SQL_TABLE_VIEW_CRC_32_TEMPLATE, tableName);
	}
	
	/**
	 * 
	 * @param refs
	 * @param selectColumns
	 * @return
	 */
	public static String buildSelectRowIds(String tableId, List<RowReference> refs, List<ColumnModel> selectColumns){
		ValidateArgument.required(tableId, "tableId");
		ValidateArgument.required(refs, "RowReferences");
		ValidateArgument.requirement(!refs.isEmpty(), "Must include at least one RowReference");
		ValidateArgument.required(selectColumns, "select columns");
		ValidateArgument.requirement(!selectColumns.isEmpty(), "Must include at least one select column");

		StringBuilder builder = new StringBuilder();
		builder.append("SELECT ");
		boolean first = true;
		for(ColumnModel cm: selectColumns){
			if(!first){
				builder.append(", ");
			}
			builder.append("`");
			builder.append(cm.getName());
			builder.append("`");
			first = false;
		}
		builder.append(" FROM ");
		builder.append(tableId);
		builder.append(" WHERE ");
		builder.append(ROW_ID);
		builder.append(" IN (");
		first = true;
		for(RowReference ref: refs){
			if(!first){
				builder.append(", ");
			}
			ValidateArgument.required(ref.getRowId(), "RowReference.rowId");
			builder.append(ref.getRowId());
			first = false;
		}
		builder.append(")");
		return builder.toString();
	}

	/**
	 * Match the given column changes to the current schema. If a column update
	 * is requested but the column does not exist in the current schema, then
	 * the updated will be changed to an add.
	 * 
	 * @param currentIndexSchema
	 * @param changes
	 * @return
	 */
	public static List<ColumnChangeDetails> matchChangesToCurrentInfo(
			List<DatabaseColumnInfo> currentIndexSchema,
			List<ColumnChangeDetails> changes) {
		// Map the ColumnIds of the current schema to the DatabaseColumnInfo for each column.
		Map<String, DatabaseColumnInfo> currentColumnIdToInfo = new HashMap<String, DatabaseColumnInfo>(currentIndexSchema.size());
		for(DatabaseColumnInfo info: currentIndexSchema){
			if(!info.isMetadata()){
				if(info.getColumnType() != null){
					String columnId = EMPTY_STRING+getColumnId(info);
					currentColumnIdToInfo.put(columnId, info);
				}
			}
		}
		List<ColumnChangeDetails> results = new LinkedList<ColumnChangeDetails>();
		for (ColumnChangeDetails change : changes) {
			DatabaseColumnInfo oldColumnInfo = null;
			ColumnModel oldColumn = change.getOldColumn();
			if (oldColumn != null) {
				oldColumnInfo = currentColumnIdToInfo.get(oldColumn.getId());
				if (oldColumnInfo == null) {
					/*
					 * The old column does not exist in the table. Setting the
					 * old column to null will treat this change as an add
					 * instead of an update.
					 */
					oldColumn = null;
				}
			}
			results.add(new ColumnChangeDetails(oldColumn, oldColumnInfo, change
					.getNewColumn()));
		}
		return results;
	}

	/**
	 * Determine if an incompatibility between view's schema and the possible
	 * annotations is the cause of the passed exception. If the cause is
	 * determined an appropriate IllegalArgumentException will be thrown.
	 * 
	 * @param viewSchema
	 *            The schema of the view.
	 * @param possibleAnnotations
	 *            The possible column models for the annotations within the
	 *            view's scope.
	 */
	public static void determineCauseOfException(Exception exception,
			List<ColumnModel> viewSchema, List<ColumnModel> possibleAnnotations) {
		// Find matches
		for (ColumnModel annotation : possibleAnnotations) {
			for (ColumnModel schemaModel : viewSchema) {
				determineCauseOfException(exception, schemaModel, annotation);
			}
		}
	}

	/**
	 * Determine if an incompatibility between the passed two columns is the
	 * cause of the passed exception.
	 * <p>
	 * The fix for PLFM-5348 was to change this method to only throw an exception
	 * for the case where an annotation string value is too large for a view
	 * string column.
	 * </p>
	 * @param exception
	 * @param annotationMetadata
	 * @param columnMetadata
	 * @throws IllegalArgumentException if both the view column type and annotation type
	 * are strings, and the annotation value size is larger than the view column size.
	 * No other case will throw an exception.
	 */
	public static void determineCauseOfException(Exception exception,
			ColumnModel columnModel, ColumnModel annotationModel) {
		EntityField entityField = EntityField.findMatch(columnModel);
		if(entityField != null){
			// entity field are not matched to annotations.
			return;
		}
		// lookup the annotation type that matches the column type.
		AnnotationType columnModelAnnotationType = translateColumnTypeToAnnotationType(columnModel.getColumnType());
		AnnotationType annotationType = translateColumnTypeToAnnotationType(annotationModel.getColumnType());
		// do the names match?
		if (columnModel.getName().equals(annotationModel.getName())) {
			// Do they map to the same annotation type?
			if (columnModelAnnotationType.equals(annotationType)) {
				// Have match.
				if (ColumnType.STRING.equals(columnModel.getColumnType())) {
					if (columnModel.getMaximumSize() < annotationModel
							.getMaximumSize()) {
						throw new IllegalArgumentException(
								"The size of the column '"
										+ columnModel.getName()
										+ "' is too small.  The column size needs to be at least "
										+ annotationModel.getMaximumSize()
										+ " characters.", exception);
					}
				}
			}
		}
	}
	
	/**
	 * Get the column used to filter the rows in a view. Project views scopes
	 * are filtered by entityIds, while all other view types scopes are filtered
	 * parentId.
	 * 
	 * @param type
	 * @return
	 */
	public static String getViewScopeFilterColumnForType(Long viewTypeMask) {
		if(ViewTypeMask.Project.getMask() == viewTypeMask) {
			return TableConstants.ENTITY_REPLICATION_COL_ID;
		}else {
			return TableConstants.ENTITY_REPLICATION_COL_PARENT_ID;
		}
	}
	
	/**
	 * Generate the SQL used to get the distinct annotations for a view
	 * of the given type.
	 * 
	 * @param type
	 * @return
	 */
	public static String getDistinctAnnotationColumnsSql(Long viewTypeMask){
		String filterColumln = getViewScopeFilterColumnForType(viewTypeMask);
		return String.format(TableConstants.SELECT_DISTINCT_ANNOTATION_COLUMNS_TEMPLATE, filterColumln);
	}
	
	/**
	 * Generate the SQL used to calculate CRC32 for views. 
	 * @param type
	 * @return
	 */
	public static String getCalculateCRC32Sql(Long viewTypeMask){
		String typeFilter = createViewTypeFilter(viewTypeMask);
		String filterColumln = getViewScopeFilterColumnForType(viewTypeMask);
		return String.format(TableConstants.SQL_ENTITY_REPLICATION_CRC_32_TEMPLATE, typeFilter, filterColumln);
	}
	
	/**
	 * Write the given annotations DTO to the given prepared statement for insert into the database.
	 * 
	 * @param ps
	 * @param dto
	 * @throws SQLException
	 */
	public static void writeAnnotationDtoToPreparedStatement(PreparedStatement ps, AnnotationDTO dto) throws SQLException{
		int parameterIndex = 1;
		ps.setLong(parameterIndex++, dto.getEntityId());
		ps.setString(parameterIndex++, dto.getKey());
		ps.setString(parameterIndex++, dto.getType().name());
		ps.setString(parameterIndex++, dto.getValue());
		String stringValue = dto.getValue();
		// Handle longs
		AllLongTypeParser longParser = new AllLongTypeParser();
		Long longValue = null;
		if(longParser.isOfType(stringValue)){
			longValue = (Long) longParser.parseValueForDatabaseWrite(stringValue);
		}
		if(longValue == null){
			ps.setNull(parameterIndex++, Types.BIGINT);
		}else{
			ps.setLong(parameterIndex++, longValue);
		}
		// Handle doubles
		Double doubleValue = null;
		AbstractDouble abstractDoubleType = null;
		DoubleParser doubleParser = new DoubleParser();
		if(doubleParser.isOfType(stringValue)){
			doubleValue = (Double) doubleParser.parseValueForDatabaseWrite(stringValue);
			// Is this an abstract double?
			if(AbstractDouble.isAbstractValue(doubleValue)){
				abstractDoubleType = AbstractDouble.lookupType(doubleValue);
				doubleValue = abstractDoubleType.getApproximateValue();
			}
		}
		if(doubleValue == null){
			ps.setNull(parameterIndex++, Types.DOUBLE);
		}else{
			ps.setDouble(parameterIndex++, doubleValue);
		}
		// Handle abstract doubles
		if(abstractDoubleType == null){
			ps.setNull(parameterIndex++, Types.VARCHAR);
		}else{
			ps.setString(parameterIndex++, abstractDoubleType.getEnumerationValue());
		}
		// Handle booleans
		Boolean booleanValue = null;
		BooleanParser booleanParser = new BooleanParser();
		if(booleanParser.isOfType(stringValue)){
			booleanValue = (Boolean) booleanParser.parseValueForDatabaseWrite(stringValue);
		}
		if(booleanValue == null){
			ps.setNull(parameterIndex, Types.BOOLEAN);
		}else{
			ps.setBoolean(parameterIndex, booleanValue);
		}
	}

}
