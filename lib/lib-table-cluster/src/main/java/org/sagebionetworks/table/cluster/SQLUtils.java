package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.repo.model.table.ColumnConstants.isTableTooLargeForFourByteUtf8;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_KEYS_PARAM_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_ALIAS;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_DOUBLE_ABSTRACT;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_KEY;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_LIST_LENGTH;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_OBJECT_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_OBJECT_VERSION;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_TABLE;
import static org.sagebionetworks.repo.model.table.TableConstants.FILE_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ID_PARAM_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.INDEX_NUM;
import static org.sagebionetworks.repo.model.table.TableConstants.OBEJCT_REPLICATION_COL_ETAG;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_ALIAS;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_BENEFACTOR_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_OBJECT_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_OBJECT_VERSION;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_TABLE;
import static org.sagebionetworks.repo.model.table.TableConstants.P_LIMIT;
import static org.sagebionetworks.repo.model.table.TableConstants.P_OFFSET;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_BENEFACTOR;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ETAG;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_SEARCH_CONTENT;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;
import static org.sagebionetworks.repo.model.table.TableConstants.STATUS_COL_SCHEMA_HASH;
import static org.sagebionetworks.repo.model.table.TableConstants.STATUS_COL_SEARCH_ENABLED;
import static org.sagebionetworks.repo.model.table.TableConstants.STATUS_COL_SINGLE_KEY;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectAnnotationDTO;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.parser.AllLongTypeParser;
import org.sagebionetworks.repo.model.table.parser.BooleanParser;
import org.sagebionetworks.repo.model.table.parser.DoubleParser;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.Grouping;
import org.sagebionetworks.table.model.SparseRow;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;
import org.sagebionetworks.table.query.util.SqlElementUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.doubles.AbstractDouble;
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

	private static final String VIEW_ROWS_OUT_OF_DATE_TEMPLATE = SQLUtils.loadSQLFromClasspath("sql/ViewOutOfDate.sql");
	private static final String SELECT_DISTINCT_ANNOTATION_COLUMNS_TEMPLATE = SQLUtils.loadSQLFromClasspath("sql/ViewDistinctAnnotations.sql");
	
	private static final String EMPTY_STRING = "";
	private static final String ABSTRACT_DOUBLE_ALIAS_PREFIX = "_DBL";
	private static final String TEMPLATE_MAX_ANNOTATION_SELECT = ", MAX(IF(%1$s.%2$s ='%3$s', %1$s.%4$s, NULL)) AS %5$s%6$s";
	private static final String TEMPLATE_MAX_OBJECT_SELECT = ", MAX(%1$s.%2$s) AS %2$s";
	private static final String DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS %1$S";
	private static final String SELECT_COUNT_FROM_TEMP = "SELECT COUNT(*) FROM ";
	private static final String SQL_COPY_TABLE_TO_TEMP = "INSERT INTO %1$S SELECT * FROM %2$S";
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
	public static final String UNNEST_SUFFIX = "_UNNEST";
	private static final String DOUBLE_NAN = Double.toString(Double.NaN);
	private static final String DOUBLE_POSITIVE_INFINITY = Double.toString(Double.POSITIVE_INFINITY);
	private static final String DOUBLE_NEGATIVE_INFINITY = Double.toString(Double.NEGATIVE_INFINITY);
	private static final String DOUBLE_ENUM_CLAUSE = " ENUM ('" + DOUBLE_NAN + "', '" + DOUBLE_POSITIVE_INFINITY + "', '"
			+ DOUBLE_NEGATIVE_INFINITY + "') DEFAULT null";

	public enum TableIndexType {
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

		private TableIndexType(String tablePostFix) {
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
	public static final List<TableIndexType> SECONDARY_TYPES = ImmutableList.of(TableIndexType.STATUS, TableIndexType.FILE_IDS);
	
	/**
	 * Given a new schema generate the create table DDL.
	 * 
	 * @param newSchema
	 * @return
	 */
	public static String createTableSQL(IdAndVersion tableId, TableIndexType type) {
		ValidateArgument.required(tableId, "tableId");
		StringBuilder columnDefinitions = new StringBuilder();
		switch (type) {
		case STATUS:
			columnDefinitions.append(STATUS_COL_SINGLE_KEY).append(" ENUM('1') NOT NULL PRIMARY KEY, ");
			columnDefinitions.append(ROW_VERSION).append(" BIGINT NOT NULL,");
			columnDefinitions.append(STATUS_COL_SCHEMA_HASH).append(" CHAR(35) NOT NULL,");
			columnDefinitions.append(STATUS_COL_SEARCH_ENABLED).append(" BOOLEAN NOT NULL");
			break;
		case FILE_IDS:
			columnDefinitions.append(FILE_ID).append(" BIGINT NOT NULL PRIMARY KEY");
			break;
		default:
			throw new IllegalArgumentException("Cannot handle type " + type);
		}
		return createTableSQL(tableId, type, columnDefinitions.toString());
	}

	private static String createTableSQL(IdAndVersion tableId, TableIndexType type, String columnDefinitions) {
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
	public static String getTableNameForId(IdAndVersion id, TableIndexType type) {
		if (id == null) {
			throw new IllegalArgumentException("Table ID cannot be null");			
		}
		StringBuilder builder = new StringBuilder();
		appendTableNameForId(id, type, builder);
		return builder.toString();
	}
	
	/**
	 * Get the table alias for the given index.
	 * @param tableIndex
	 * @return
	 */
	public static String getTableAliasForIndex(int tableIndex) {
		return "_A"+tableIndex;
	}

	private static void appendTableNameForId(IdAndVersion id, TableIndexType type, StringBuilder builder) {
		builder.append(TABLE_PREFIX);
		if (id.getId() < 0) {
			// When the id is negative the "-" sign can break some queries since we do not enquote the table name
			builder.append("__");
			builder.append(-id.getId());
		} else {
			builder.append(id.getId());
		}
		if (id.getVersion().isPresent()) {
			builder.append("_").append(id.getVersion().get());
		}
		builder.append(type.getTablePostFix());
	}
	
	/**
	 * Get the prefix shared by all multi-value tables associated with the given table.
	 * @param idAndVersion
	 * @return
	 */
	public static String getTableNamePrefixForMultiValueColumns(IdAndVersion idAndVersion, boolean alterTemp) {
		StringBuilder builder = new StringBuilder();
		if(alterTemp){
			builder.append(TEMP);
		}
		//currently only TableType.INDEX (i.e. the original user table) have multi-value columns
		appendTableNameForId(idAndVersion, TableIndexType.INDEX, builder);
		builder.append("_INDEX");
		return builder.toString();
	}

	/**
	 * Get the full table name for a multi-value table associated with the given table and column.
	 * @param idAndVersion
	 * @param columnId
	 * @return
	 */
	public static String getTableNameForMultiValueColumnIndex(IdAndVersion idAndVersion, String columnId){
		boolean alterTemp = false;
		return getTableNameForMultiValueColumnIndex(idAndVersion, columnId, alterTemp);
	}

	public static String getTableNameForMultiValueColumnIndex(IdAndVersion idAndVersion, String columnId, boolean alterTemp){
		ValidateArgument.required(idAndVersion, "idAndVersion");
		ValidateArgument.required(columnId, "columnId");
		StringBuilder builder = new StringBuilder(getTableNamePrefixForMultiValueColumns(idAndVersion, alterTemp));
		appendColumnNameForId(columnId, builder);
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
		StringBuilder builder = new StringBuilder();
		appendColumnNameForId(columnId, builder);
		return builder.toString();
	}

	/**
	 * Get the Column name for a Unnested column given a column ID.
	 * Unnested columns are present in the tables with names generated by 
	 * {@link #getTableNameForMultiValueColumnIndex(IdAndVersion, String)}
	 * and contain a single value per row for each
	 *
	 * @param columnId
	 * @return
	 */
	public static String getUnnestedColumnNameForId(String columnId) {
		ValidateArgument.required(columnId, "columnId");

		StringBuilder builder = new StringBuilder();
		appendColumnNameForId(columnId, builder);
		builder.append(UNNEST_SUFFIX);
		return builder.toString();
	}

	/**
	 * Column name used for secondary tables that make a foreign key reference to an original table that has the ROW_ID column
	 *
	 * Row ID ref columns are present in the tables with names generated by
	 * {@link #getTableNameForMultiValueColumnIndex(IdAndVersion, String)}
	 *
	 * The unique name derived from columnId allows us to avoid having to
	 * qualify the table name from which ROW_ID comes from
	 * when a JOIN is used to join the main table to the table containg unnested values of a list column
	 * @param columnId
	 * @return
	 */
	public static String getRowIdRefColumnNameForId(String columnId) {
		ValidateArgument.required(columnId, "columnId");

		StringBuilder builder = new StringBuilder();
		builder.append(ROW_ID);
		builder.append("_REF");
		appendColumnNameForId(columnId, builder);
		return builder.toString();
	}

	public static void appendColumnNameForId(String columnId, StringBuilder builder){
		appendColumnName(null, columnId, builder);
	}

	private static void appendColumnName(String prefix, String columnId, StringBuilder builder) {
		if (prefix != null) {
			builder.append(prefix);
		}
		builder.append(COLUMN_PREFIX).append(columnId).append(COLUMN_POSTFIX);
	}

	/**
	 * Append the column name that stores abstract values for doubles (Infinity, NaN).
	 * Example : _DBL_C1_
	 * @param reference
	 * @param builder
	 */
	static void appendDoubleAbstractColumnName(String columnId, StringBuilder builder){
		appendColumnName(TableConstants.DOUBLE_PREFIX, columnId, builder);
	}

	static String getDoubleAbstractColumnName(String columnId){
		StringBuilder builder = new StringBuilder();
		appendDoubleAbstractColumnName(columnId, builder);
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
	 * @param columnId
	 * @param builder
	 */
	public static void appendIsNan(String columnId, StringBuilder builder) {
		builder.append("(");
		appendDoubleAbstractColumnName(columnId, builder);
		builder.append(" IS NOT NULL AND ");
		appendDoubleAbstractColumnName(columnId, builder);
		builder.append(" = '").append(DOUBLE_NAN).append("')");
	}

	/**
	 * Compare doubles to infinity
	 * 
	 * <pre>
	 * _DBL_C1_ IN ('Infinity', '-Infinity')
	 * </pre>
	 * 
	 * @param columnId
	 * @param builder
	 */
	public static void appendIsInfinity(String columnId,  StringBuilder builder) {
		builder.append("(");
		appendDoubleAbstractColumnName(columnId, builder);
		builder.append(" IS NOT NULL AND ");
		appendDoubleAbstractColumnName(columnId, builder);
		builder.append(" IN ('").append(DOUBLE_NEGATIVE_INFINITY).append("', '").append(DOUBLE_POSITIVE_INFINITY).append("'))");
	}

	/**
	 * Create the DROP table SQL.
	 * @param tableId
	 * @return
	 */
	public static String dropTableSQL(IdAndVersion tableId, TableIndexType type) {
		String tableName = getTableNameForId(tableId, type);
		return "DROP TABLE IF EXISTS " + tableName;
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
		builder.append(getTableNameForId(tableId, TableIndexType.INDEX));
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
			if(cm.getColumnType() == ColumnType.DOUBLE){
				names.add(getDoubleAbstractColumnName(cm.getId()));
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
		builder.append(getTableNameForId(tableId, TableIndexType.STATUS));
		builder.append(" ( ");
		builder.append(STATUS_COL_SINGLE_KEY);
		builder.append(",");
		builder.append(ROW_VERSION);
		builder.append(",");
		builder.append(STATUS_COL_SCHEMA_HASH);
		builder.append(",");
		builder.append(STATUS_COL_SEARCH_ENABLED);
		builder.append(" ) VALUES ('1', ?, '" + TableModelUtils.EMPTY_SCHEMA_MD5 + "', FALSE) ON DUPLICATE KEY UPDATE "+ROW_VERSION+" = ?");
		return builder.toString();
	}
	
	public static String buildCreateOrUpdateSearchStatusSQL(IdAndVersion tableId) {
		if (tableId == null)
			throw new IllegalArgumentException("TableID cannot be null");
		StringBuilder builder = new StringBuilder();
		builder.append("INSERT INTO ");
		builder.append(getTableNameForId(tableId, TableIndexType.STATUS));
		builder.append(" ( ");
		builder.append(STATUS_COL_SINGLE_KEY);
		builder.append(",");
		builder.append(ROW_VERSION);
		builder.append(",");
		builder.append(STATUS_COL_SCHEMA_HASH);
		builder.append(",");
		builder.append(STATUS_COL_SEARCH_ENABLED);
		builder.append(" ) VALUES ('1', -1, '" + TableModelUtils.EMPTY_SCHEMA_MD5 + "', ?) ON DUPLICATE KEY UPDATE " + STATUS_COL_SEARCH_ENABLED + " = ?");
		return builder.toString();
	}
	
	public static String buildCreateOrUpdateStatusHashSQL(IdAndVersion tableId) {
		if (tableId == null)
			throw new IllegalArgumentException("TableID cannot be null");
		StringBuilder builder = new StringBuilder();
		builder.append("INSERT INTO ");
		builder.append(getTableNameForId(tableId, TableIndexType.STATUS));
		builder.append(" ( ");
		builder.append(STATUS_COL_SINGLE_KEY);
		builder.append(",");
		builder.append(ROW_VERSION);
		builder.append(",");
		builder.append(STATUS_COL_SCHEMA_HASH);
		builder.append(",");
		builder.append(STATUS_COL_SEARCH_ENABLED);
		builder.append(" ) VALUES ('1', -1, ?, FALSE) ON DUPLICATE KEY UPDATE "+STATUS_COL_SCHEMA_HASH+" = ?");
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
		builder.append(getTableNameForId(tableId, TableIndexType.INDEX));
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
						String doubleEnumerationName = getDoubleAbstractColumnName(cm.getId());
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
		builder.append("SELECT COUNT(").append(ROW_ID).append(") FROM ").append(getTableNameForId(tableId, TableIndexType.INDEX));
		return builder.toString();
	}
	
	/**
	 * Create the SQL used to get the max version number from a table.
	 * @return
	 */
	public static String getStatusMaxVersionSQL(IdAndVersion tableId) {
		return "SELECT " + ROW_VERSION + " FROM " + getTableNameForId(tableId, TableIndexType.STATUS);
	}

	/**
	 * Create SQL used to get the current schema hash of a table.
	 * @param tableId
	 * @return
	 */
	public static String getSchemaHashSQL(IdAndVersion tableId) {
		return "SELECT " + STATUS_COL_SCHEMA_HASH + " FROM " + getTableNameForId(tableId, TableIndexType.STATUS);
	}
	
	public static String getSearchStatusSQL(IdAndVersion tableId) {
		return "SELECT COUNT(" + STATUS_COL_SEARCH_ENABLED + ") FROM " + getTableNameForId(tableId, TableIndexType.STATUS) + " WHERE " + STATUS_COL_SEARCH_ENABLED + " = TRUE";
	}
	
	/**
	 * Insert ignore file handle ids into a table's secondary file index.
	 * @param tableId
	 * @return
	 */
	public static String createSQLInsertIgnoreFileHandleId(IdAndVersion tableId){
		return "INSERT IGNORE INTO "+getTableNameForId(tableId, TableIndexType.FILE_IDS)+" ("+FILE_ID+") VALUES(?)";
	}
	
	/**
	 * SQL for finding all file handle ids bound to a table that are included in the provided set.
	 * @param tableId
	 * @return
	 */
	public static String createSQLGetBoundFileHandleId(IdAndVersion tableId){
		return "SELECT "+FILE_ID+" FROM "+getTableNameForId(tableId, TableIndexType.FILE_IDS)+" WHERE "+FILE_ID+" IN( :"+FILE_ID_BIND+")";
	}
	
	/**
	 * Select distinct values from the given column ID.
	 * 
	 * @param tableId
	 * @param columnName
	 * @return
	 */
	public static String createSQLGetDistinctValues(IdAndVersion tableId, String columnName){
		return "SELECT DISTINCT "+columnName+" FROM "+getTableNameForId(tableId, TableIndexType.INDEX);
	}

	/**
	 * Create alter table SQL statements for the given set of column changes.
	 * 
	 * @param changes
	 * @return
	 */
	public static String[] createAlterTableSql(List<ColumnChangeDetails> changes, IdAndVersion tableId, boolean alterTemp){
		List<String> result = new LinkedList<>();
		boolean useDepricatedUtf8ThreeBytes = isTableTooLargeForFourByteUtf8(tableId.getId());
		String tableName = null;
		if (alterTemp) {
			tableName = getTemporaryTableName(tableId);
		} else {
			tableName = getTableNameForId(tableId, TableIndexType.INDEX);
		}
		for (ColumnChangeDetails change : changes) {
			result.addAll(createAlterTableSqlColumnChangeDetailHandler(change, tableName, useDepricatedUtf8ThreeBytes));
		}
		return result.toArray(new String[result.size()]);
	}

	/**
	 * Helper for createAlterTableSql to handle a single column change detail
	 * 
	 * @param change
	 * @param tableName
	 * @param useDepricatedUtf8ThreeBytes
	 * @return
	 */
	private static List<String> createAlterTableSqlColumnChangeDetailHandler(ColumnChangeDetails change, 
			String tableName, boolean useDepricatedUtf8ThreeBytes) {
		// if changing to a _LIST type from a non-_LIST type
		if (change.getOldColumn() != null && change.getNewColumn() != null
				&& !ColumnTypeListMappings.isList(change.getOldColumn().getColumnType())
				&& ColumnTypeListMappings.isList(change.getNewColumn().getColumnType())) {
			return createAlterToListColumnTypeSqlBatch(change, tableName, useDepricatedUtf8ThreeBytes);
		} else { // handles all other alter table cases
			String sql = appendAlterTableSql(change, useDepricatedUtf8ThreeBytes, tableName);
			if (sql.length() == 0) { // only non-empty SQL, batchUpdate does not want empty SQL
				return new ArrayList<>();
			}
			return Arrays.asList(sql);
		}
	}
	
	public static String createAlterListColumnIndexTable(IdAndVersion tableId, Long oldColumnId, ColumnModel newColumn, boolean alterTemp){
		String tableName = getTableNameForMultiValueColumnIndex(tableId, oldColumnId.toString(), alterTemp);
		String oldColumnName = getUnnestedColumnNameForId(oldColumnId.toString());

		String newColumnName = getUnnestedColumnNameForId(newColumn.getId());
		String newColumnTypeSql = ColumnTypeInfo.getInfoForType(ColumnTypeListMappings.nonListType(newColumn.getColumnType()))
				.toSql(newColumn.getMaximumSize(), null, false);

		String newTableName = getTableNameForMultiValueColumnIndex(tableId, newColumn.getId(), alterTemp);

		String oldRowRefName = getRowIdRefColumnNameForId(oldColumnId.toString());
		String newRowRefName = getRowIdRefColumnNameForId(newColumn.getId());
		String parentTableName = alterTemp ? getTemporaryTableName(tableId) : getTableNameForId(tableId, TableIndexType.INDEX);

		return  "ALTER TABLE " + tableName +
				" DROP INDEX " + oldColumnName + "_IDX," +

				//modify the row_id column which references the main table's row_ids
				" DROP FOREIGN KEY " + getMultiValueIndexTableForeignKeyConstraintName(tableName) + "," +
				" RENAME COLUMN " + oldRowRefName + " TO " + newRowRefName + "," +
				" ADD " + getMultiValueIndexTableForeignKeyConstraint(parentTableName,newTableName,newRowRefName)+

				", CHANGE COLUMN " + oldColumnName +  " " + newColumnName + " "+ newColumnTypeSql + "," +
				" ADD INDEX " + newColumnName + "_IDX ("+newColumnName+" ASC)," +

				" RENAME " + newTableName;
	}

	/**
		 * Alter a single column for a given column change.
		 * @param builder
		 * @param change
		 * @param useDepricatedUtf8ThreeBytes Should only be set to true for the few old
		 * tables that are too large to build with the correct 4 byte UTF-8.
		 */
	public static String appendAlterTableSql(ColumnChangeDetails change, boolean useDepricatedUtf8ThreeBytes, String tableName) {
		StringBuilder builder = new StringBuilder();
		builder.append("ALTER TABLE ");
		builder.append(tableName);
		builder.append(" ");
		if(change.getOldColumn() == null && change.getNewColumn() == null){
			// nothing to do
			return "";
		}
		if(change.getOldColumn() == null){
			// add
			appendAddColumn(builder, change.getNewColumn(), useDepricatedUtf8ThreeBytes);
			return builder.toString();
		}

		if(change.getNewColumn() == null){
			// delete
			appendDeleteColumn(builder, change.getOldColumn());
			return builder.toString();
		}
		if (change.getNewColumn().equals(change.getOldColumn())) {
			// both columns are the same so do nothing.
			return "";
		}
		// update
		appendUpdateColumn(builder, change, useDepricatedUtf8ThreeBytes);
		// change was added.
		return builder.toString();
	}
	
	/**
	 * Append an add column statement to the passed builder.
	 * @param builder
	 * @param newColumn
	 * @param useDepricatedUtf8ThreeBytes Should only be set to true for the few old
	 * tables that are too large to build with the correct 4 byte UTF-8.
	 */
	public static void appendAddColumn(StringBuilder builder,
			ColumnModel newColumn, boolean useDepricatedUtf8ThreeBytes) {
		ValidateArgument.required(newColumn, "newColumn");
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
			ColumnModel oldColumn) {
		ValidateArgument.required(oldColumn, "oldColumn");
		builder.append("DROP COLUMN ");
		appendColumnNameForId(oldColumn.getId(), builder);
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
			ColumnChangeDetails change, boolean useDepricatedUtf8ThreeBytes) {
		ValidateArgument.required(change, "change");
		ValidateArgument.required(change.getOldColumn(), "change.getOldColumn()");
		ValidateArgument.required(change.getOldColumnInfo(), "change.getOldColumnInfo()");
		ValidateArgument.required(change.getNewColumn(), "change.getNewColumn()");
		
		if(change.getOldColumnInfo().hasIndex()){
			// drop the index on the old column before changing the column.
			ValidateArgument.required(change.getOldColumnInfo().getIndexName(), "change.getOldColumnInfo().getIndexName");
			appendDropIndex(builder, change.getOldColumnInfo());	
			builder.append(", ");
		}
		builder.append("CHANGE COLUMN ");
		appendColumnNameForId(change.getOldColumn().getId(), builder);
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
	 * Creates alter table and update statements for altering to a JSON column for _LIST types.
	 * @param change
	 * @param tableId
	 * @param alterTemp
	 * @return
	 */
	public static List<String> createAlterToListColumnTypeSqlBatch(ColumnChangeDetails change, 
			String tableName, boolean useDepricatedUtf8ThreeBytes) {
		
		return Arrays.asList(createAppendListColumnSql(change, tableName, useDepricatedUtf8ThreeBytes),
				createSetListColumnFromNonListColumnSql(change, tableName),
				createDeleteColumnThatWasReplacedWithAListColumnSql(change, tableName));
	}
	
	private static String createAppendListColumnSql(ColumnChangeDetails change, String tableName, boolean useDepricatedUtf8ThreeBytes) {
		StringBuilder builder = new StringBuilder();
		builder.append("ALTER TABLE ");
		builder.append(tableName);
		builder.append(" ");
		appendAddColumn(builder, change.getNewColumn(), useDepricatedUtf8ThreeBytes);
		return builder.toString();
	}
	
	private static String createSetListColumnFromNonListColumnSql(ColumnChangeDetails change, String tableName) {
		StringBuilder builder = new StringBuilder();
		builder.append("UPDATE ");
		builder.append(tableName);
		builder.append(" SET ");
		appendColumnNameForId(change.getNewColumn().getId(), builder);
		builder.append(" = JSON_ARRAY(");
		appendColumnNameForId(change.getOldColumn().getId(), builder);
		builder.append(")");
		return builder.toString();
	}
	
	private static String createDeleteColumnThatWasReplacedWithAListColumnSql(ColumnChangeDetails change, String tableName ) {
		StringBuilder builder = new StringBuilder();
		builder.append("ALTER TABLE ");
		builder.append(tableName);
		builder.append(" ");
		appendDeleteColumn(builder, change.getOldColumn());
		return builder.toString();
	}
	
	/**
	 * Append a column type definition to the passed builder.
	 * @param builder
	 * @param column
	 * @param useDepricatedUtf8ThreeBytes Should only be set to true for the few old
	 * tables that are too large to build with the correct 4 byte UTF-8.
	 */
	public static void appendColumnDefinition(StringBuilder builder, ColumnModel column , boolean useDepricatedUtf8ThreeBytes){
		appendColumnNameForId(column.getId(), builder);
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
		appendDoubleAbstractColumnName(columnId, builder);
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
		appendDoubleAbstractColumnName(columnId, builder);
	}
	
	/**
	 * Append a the rename of a double enumeration column.
	 * @param builder
	 * @param oldId
	 * @param newId
	 */
	public static void appendRenameDoubleEnum(StringBuilder builder, String oldId, String newId){
		builder.append(", CHANGE COLUMN ");
		appendDoubleAbstractColumnName(oldId, builder);
		builder.append(" ");
		appendDoubleAbstractColumnName(newId, builder);
		builder.append(DOUBLE_ENUM_CLAUSE);
	}

	/**
	 * Create the SQL to truncate the given table.
	 * @param tableId
	 * @return
	 */
	public static String createTruncateSql(IdAndVersion tableId) {
		return "DELETE FROM "+getTableNameForId(tableId, TableIndexType.INDEX);
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
			// There is no need to run a distinct count for columns for which an index is not created 
			// or for metadata columns (such as row id) that manage their own indices
			// Using MAX with a constant is relatively cheap, note that when there are no rows in the table MAX will return NULL
			if (info.isMetadata() || !info.getType().isCreateIndex()) {
				builder.append("MAX(");
				builder.append(TableConstants.COLUMN_NO_CARDINALITY);
				builder.append(")");
			} else {
				builder.append("COUNT(DISTINCT ");
				builder.append(info.getColumnName());
				builder.append(")");
			}
			builder.append(" AS ");
			builder.append(info.getColumnName());
			isFirst = false;
		}
		builder.append(" FROM ");
		builder.append(getTableNameForId(tableId, TableIndexType.INDEX));
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
			// ignore metadata columns such as row id and version
			if (info.isMetadata()) {
				continue;
			}
			// If the index is skipped for the type, make sure to remove existing ones (e.g. if the type was updated)
			if (!info.getType().isCreateIndex()) {
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
		builder.append(getTableNameForId(tableId, TableIndexType.INDEX));
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
		return getColumnId(info.getColumnName());
	}

	public static long getColumnId(String columnName) {
		ValidateArgument.requiredNotEmpty(columnName, "columnName");
		try {
			return Long.parseLong(columnName.substring(COLUMN_PREFIX.length(), columnName.length()-COLUMN_POSTFIX.length()));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Unexpected columnName: "+columnName);
		}
	}

	static long getColumnIdFromMultivalueColumnIndexTableName(IdAndVersion tableId, String indexTableName){
		ValidateArgument.required(tableId, "tableId");
		ValidateArgument.requiredNotEmpty(indexTableName, "columnName");
		boolean alterTemp = false;
		return getColumnId(indexTableName.substring(getTableNamePrefixForMultiValueColumns(tableId, alterTemp).length()));
	}

	/**
	 * The name of the temporary table for the given table Id.
	 * 
	 * @param tableId
	 * @return
	 */
	public static String getTemporaryTableName(IdAndVersion tableId){
		return TEMP+getTableNameForId(tableId, TableIndexType.INDEX);
	}

	/**
	 * Create the SQL used to create a temporary table 
	 * @param tableId
	 * @return
	 */
	public static String createTempTableSql(IdAndVersion tableId) {
		String tableName = getTableNameForId(tableId, TableIndexType.INDEX);
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
		String tableName = getTableNameForId(tableId, TableIndexType.INDEX);
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
	 * Create the SQL used to create a temporary multivalue column index table
	 * @param tableId
	 * @return
	 */
	public static String[] createTempMultiValueColumnIndexTableSql(IdAndVersion tableId, String columnId) {
		String tableName = getTableNameForMultiValueColumnIndex(tableId, columnId, false);
		String tempName = getTableNameForMultiValueColumnIndex(tableId, columnId, true);
		String tempParentTable = getTemporaryTableName(tableId);
		String columnIndexTableName = getTableNameForMultiValueColumnIndex(tableId, columnId, true);
		String rowIdRefColumnName = getRowIdRefColumnNameForId(columnId);

		return new String[]{String.format(CREATE_TABLE_LIKE, tempName, tableName),
				// foreign keys are not copied over so we manually add it
				"ALTER TABLE " + tempName +
				" ADD " + getMultiValueIndexTableForeignKeyConstraint(tempParentTable, columnIndexTableName, rowIdRefColumnName)};
	}

	/**
	 * Create the SQL used to copy all of the data from a table to the temp table.
	 *
	 * @param tableId
	 * @return
	 */
	public static String copyMultiValueColumnIndexTableToTempSql(IdAndVersion tableId, String columnId){
		String tableName = getTableNameForMultiValueColumnIndex(tableId, columnId, false);
		String tempName = getTableNameForMultiValueColumnIndex(tableId, columnId, true);
		return String.format(SQL_COPY_TABLE_TO_TEMP, tempName, tableName);
	}
	
	/**
	 * Translate form ColumnType to AnnotationType;
	 * @param type
	 * @return
	 */
	public static AnnotationType translateColumnTypeToAnnotationType(ColumnType type){
		switch(type){
		case STRING:
		case STRING_LIST:
			return AnnotationType.STRING;
		case DATE:
		case DATE_LIST:
			return AnnotationType.DATE;
		case DOUBLE:
				return AnnotationType.DOUBLE;
		case INTEGER:
		case INTEGER_LIST:
				return AnnotationType.LONG;
		case BOOLEAN:
			return AnnotationType.BOOLEAN;
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
		case SUBMISSIONID:
		case EVALUATIONID:
		case FILEHANDLEID:
		case USERID:
			return TableConstants.ANNOTATION_REPLICATION_COL_LONG_VALUE;
		case DOUBLE:
			return TableConstants.ANNOTATION_REPLICATION_COL_DOUBLE_VALUE;
		case BOOLEAN:
			return TableConstants.ANNOTATION_REPLICATION_COL_BOOLEAN_VALUE;
		case STRING_LIST:
			return TableConstants.ANNOTATION_REPLICATION_COL_STRING_LIST_VALUE;
		case INTEGER_LIST:
		case DATE_LIST:
		case ENTITYID_LIST:
		case USERID_LIST:
			return TableConstants.ANNOTATION_REPLICATION_COL_LONG_LIST_VALUE;
		case BOOLEAN_LIST:
			return TableConstants.ANNOTATION_REPLICATION_COL_BOOLEAN_LIST_VALUE;
		default:
			// Everything else is a string
			return TableConstants.ANNOTATION_REPLICATION_COL_STRING_VALUE;
		}
	}
	
	/**
	 * Generate the SQL used to insert select data from the object replication tables to a
	 * table's index.
	 * @param viewId
	 * @param currentSchema
	 * @return
	 */
	public static String createSelectInsertFromObjectReplication(Long viewId, List<ColumnMetadata> metadata, String filterSql) {
		StringBuilder builder = new StringBuilder();
		builder.append("INSERT INTO ");
		builder.append(getTableNameForId(IdAndVersion.newBuilder().setId(viewId).build(), TableIndexType.INDEX));
		builder.append("(");
		buildInsertValues(builder, metadata);
		builder.append(") ");
		createSelectFromObjectReplication(builder, metadata, filterSql);
		return builder.toString();
	}
	
	/**
	 * Generate the SQL to get all of the data for a view table from the object replication tables.
	 * @param viewId
	 * @param viewTypeMask
	 * @param currentSchema
	 * @return
	 */
	public static void createSelectFromObjectReplication(StringBuilder builder, List<ColumnMetadata> metadata, String filterSql) {
		builder.append("SELECT ");
		buildObjectReplicationSelect(builder, metadata);
		objectReplicationJoinAnnotationReplicationFilter(builder, filterSql);
		builder.append(" GROUP BY ").append(OBJECT_REPLICATION_ALIAS).append(".").append(OBJECT_REPLICATION_COL_OBJECT_ID);
		builder.append(", ").append(OBJECT_REPLICATION_ALIAS).append(".").append(OBJECT_REPLICATION_COL_OBJECT_VERSION);
		builder.append(" ORDER BY ").append(OBJECT_REPLICATION_ALIAS).append(".").append(OBJECT_REPLICATION_COL_OBJECT_ID);
		builder.append(", ").append(OBJECT_REPLICATION_ALIAS).append(".").append(OBJECT_REPLICATION_COL_OBJECT_VERSION);
	}

	private static void objectReplicationJoinAnnotationReplicationFilter(StringBuilder builder, String filterSql) {
		builder.append(" FROM ");
		builder.append(OBJECT_REPLICATION_TABLE);
		builder.append(" ");
		builder.append(OBJECT_REPLICATION_ALIAS);
		builder.append(" LEFT JOIN ");
		builder.append(ANNOTATION_REPLICATION_TABLE);
		builder.append(" ").append(ANNOTATION_REPLICATION_ALIAS);
		builder.append(" ON(");
		builder.append(OBJECT_REPLICATION_ALIAS).append(".").append(OBJECT_REPLICATION_COL_OBJECT_TYPE);
		builder.append(" = ");
		builder.append(ANNOTATION_REPLICATION_ALIAS).append(".").append(ANNOTATION_REPLICATION_COL_OBJECT_TYPE);
		builder.append(" AND ");
		builder.append(OBJECT_REPLICATION_ALIAS).append(".").append(OBJECT_REPLICATION_COL_OBJECT_ID);
		builder.append(" = ");
		builder.append(ANNOTATION_REPLICATION_ALIAS).append(".").append(ANNOTATION_REPLICATION_COL_OBJECT_ID);
		builder.append(" AND ");
		builder.append(OBJECT_REPLICATION_ALIAS).append(".").append(OBJECT_REPLICATION_COL_OBJECT_VERSION);
		builder.append(" = ");
		builder.append(ANNOTATION_REPLICATION_ALIAS).append(".").append(ANNOTATION_REPLICATION_COL_OBJECT_VERSION);
		builder.append(")");
		builder.append(" WHERE");
		builder.append(filterSql);
	}

	/**
	 * Generate the SQL to validate that all of the list columns for a view table from the object replication tables.
	 * @param viewId
	 * @param viewTypeMask
	 * @param annotationNames
	 * @return
	 */
	public static String createAnnotationMaxListLengthSQL(Set<String> annotationNames, String filterSql) {
		ValidateArgument.requiredNotEmpty(annotationNames,"annotationNames");

		StringBuilder builder = new StringBuilder();
		
		builder.append("SELECT ")
				.append(ANNOTATION_REPLICATION_ALIAS).append(".").append(ANNOTATION_REPLICATION_COL_KEY)
				.append(", MAX(").append(ANNOTATION_REPLICATION_ALIAS).append(".").append(ANNOTATION_REPLICATION_COL_LIST_LENGTH).append(")");
		
		objectReplicationJoinAnnotationReplicationFilter(builder, filterSql);
		
		builder.append(" AND ").append(ANNOTATION_REPLICATION_ALIAS).append(".").append(ANNOTATION_REPLICATION_COL_KEY)
				.append(" IN (:").append(ANNOTATION_KEYS_PARAM_NAME).append(")");
		builder.append(" GROUP BY ").append(ANNOTATION_REPLICATION_ALIAS).append(".").append(ANNOTATION_REPLICATION_COL_KEY);
		
		return builder.toString();
	}
	
	/**
	 * Build the select clause of the object replication insert select.
	 * @param builder
	 * @param metadata
	 */
	public static void buildObjectReplicationSelect(StringBuilder builder, List<ColumnMetadata> metadata) {
		// select the standard object replication columns.
		buildObjectReplicationSelectStandardColumns(builder);
		for(ColumnMetadata meta: metadata) {
			buildObjectReplicationSelectMetadata(builder, meta);
		}
	}
	
	/**
	 * Build a object replication select for the given ColumnMetadata.
	 * 
	 * @param builder
	 * @param meta
	 */
	public static void buildObjectReplicationSelectMetadata(StringBuilder builder, ColumnMetadata meta) {
		if (meta.isObjectReplicationField()) {
			// object field select
			buildObjectReplicationSelect(builder, meta.getSelectColumnName());
		} else {
			// annotation select
			if (ColumnType.DOUBLE.equals(meta.getColumnModel().getColumnType())) {
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
	 * Build the select including the standard object columns of, id, version, etag, and benefactor..
	 * @param builder
	 */
	public static void buildObjectReplicationSelectStandardColumns(StringBuilder builder) {

		builder.append(OBJECT_REPLICATION_ALIAS);
		builder.append(".");
		builder.append(OBJECT_REPLICATION_COL_OBJECT_ID);
		builder.append(", ");
		builder.append(OBJECT_REPLICATION_ALIAS);
		builder.append(".");
		builder.append(OBJECT_REPLICATION_COL_OBJECT_VERSION);
		buildObjectReplicationSelect(builder,
				OBEJCT_REPLICATION_COL_ETAG,
				OBJECT_REPLICATION_COL_BENEFACTOR_ID);
	}
	/**
	 * For each provided name: ', MAX(R.name) AS name'
	 * @param builder
	 * @param names
	 */
	public static void buildObjectReplicationSelect(StringBuilder builder, String...names) {
		for(String name: names) {
			builder.append(String.format(TEMPLATE_MAX_OBJECT_SELECT, OBJECT_REPLICATION_ALIAS, name));
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
	 * Build the insert clause section of object replication insert select.
	 * 
	 * @param builder
	 * @param metadata
	 */
	public static void buildInsertValues(StringBuilder builder, List<ColumnMetadata> metadata) {
		builder.append(ROW_ID);
		builder.append(", ");
		builder.append(ROW_VERSION);
		builder.append(", ");
		builder.append(ROW_ETAG);
		builder.append(", ");
		builder.append(ROW_BENEFACTOR);
		for(ColumnMetadata meta: metadata){
			if (ColumnType.DOUBLE.equals(meta.getColumnModel().getColumnType())) {
				builder.append(", _DBL");
				builder.append(meta.getColumnNameForId());
			}
			builder.append(", ");
			builder.append(meta.getColumnNameForId());
		}
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
			builder.append(SqlElementUtils.wrapInDoubleQuotes(cm.getName()));
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
			ColumnModel oldColumn = change.getOldColumn();
			ColumnModel newColumn = change.getNewColumn();


			String newColumnId = newColumn == null ? null : newColumn.getId();
			String oldColumnId = oldColumn == null ? null : oldColumn.getId();
			DatabaseColumnInfo oldColumnInfo = currentColumnIdToInfo.get(oldColumnId);

			boolean newColumnExistsInDatabase = currentColumnIdToInfo.get(newColumnId) != null;
			boolean oldColumnExistsInDatabase = oldColumnInfo != null;
			boolean isColumnUpdate = !Objects.equals(newColumnId, oldColumnId);

			if (!oldColumnExistsInDatabase) {
					/*
					 * The old column does not exist in the table. Setting the
					 * old column to null will treat this change as an add
					 * instead of an update.
					 */
					oldColumn = null;
			}

			if (newColumnExistsInDatabase && isColumnUpdate) {
				/*
				 * The new column already exists in the table and this is a real change so we do no need to re-add it
				 */
				newColumn = null;
			}

			results.add(new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn));
		}
		return results;
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
	public static void determineCauseOfException(Exception exception, ColumnModel columnModel,
			ColumnModel annotationModel) {
		// lookup the annotation type that matches the column type.
		AnnotationType columnModelAnnotationType = translateColumnTypeToAnnotationType(columnModel.getColumnType());
		AnnotationType annotationType = translateColumnTypeToAnnotationType(annotationModel.getColumnType());
		// do the names match?
		if (columnModel.getName().equals(annotationModel.getName())) {
			// Do they map to the same annotation type?
			if (columnModelAnnotationType.equals(annotationType)) {
				// Have match.
				if (ColumnType.STRING.equals(columnModel.getColumnType())) {
					if (columnModel.getMaximumSize() != null && annotationModel.getMaximumSize() != null) {
						if (columnModel.getMaximumSize() < annotationModel.getMaximumSize()) {
							throw new IllegalArgumentException("The size of the column '" + columnModel.getName()
									+ "' is too small.  The column size needs to be at least "
									+ annotationModel.getMaximumSize() + " characters.", exception);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Generate the SQL used to get the distinct annotations for a view
	 * of the given type.
	 * 
	 * @param type
	 * @param withExclusionList If true the SQL will include a NOT IN clause on the annotation key with the :exclusionList parameter
	 * @return
	 */
	public static String getDistinctAnnotationColumnsSql(String filterSql){
		return String.format(SELECT_DISTINCT_ANNOTATION_COLUMNS_TEMPLATE, filterSql);
	}
	
	/**
	 * Write the given annotations DTO to the given prepared statement for insert into the database.
	 * 
	 * @param ps
	 * @param dto
	 * @throws SQLException
	 */
	public static void writeAnnotationDtoToPreparedStatement(ReplicationType mainType, PreparedStatement ps, ObjectAnnotationDTO dto) throws SQLException{
		int parameterIndex = 1;
		int updateOffset = 10;
		
		ps.setString(parameterIndex++, mainType.name());
		ps.setLong(parameterIndex++, dto.getObjectId());
		ps.setLong(parameterIndex++, dto.getObjectVersion());
		ps.setString(parameterIndex++, dto.getKey());
		ps.setString(parameterIndex++, dto.getType().name());
		
		List<String> stringList = dto.getValue();

		String stringValue = stringList.isEmpty() ? null : stringList.get(0);

		ps.setString(parameterIndex++, stringValue);
		ps.setString(parameterIndex + updateOffset, stringValue);
		
		// Handle longs
		AllLongTypeParser longParser = new AllLongTypeParser();
		List<Long> longList = new ArrayList<>(stringList.size());
		for(String value :stringList){
			//if any values fail to parse, then the entire list of longs is invalid
			if(!longParser.isOfType(value)){
				longList = null;
				break;
			}
			longList.add((Long) longParser.parseValueForDatabaseWrite(value));
		}

		Long longValue = longList == null || longList.isEmpty() ? null : longList.get(0);
		if(longValue == null){
			ps.setNull(parameterIndex++, Types.BIGINT);
			ps.setNull(parameterIndex + updateOffset, Types.BIGINT);
		}else{
			ps.setLong(parameterIndex++, longValue);
			ps.setLong(parameterIndex + updateOffset, longValue);
		}


		// Handle doubles
		DoubleParser doubleParser = new DoubleParser();

		List<Double> doubleList = new ArrayList<>(stringList.size());
		for(String value : stringList) {
			//if any values fail to parse, then the entire list of doubles is invalid
			if (!doubleParser.isOfType(value)) {
				doubleList = null;
				break;
			}

			doubleList.add((Double) doubleParser.parseValueForDatabaseWrite(value));
		}

		Double doubleValue = doubleList == null || doubleList.isEmpty() ? null : doubleList.get(0);
		AbstractDouble abstractDoubleType = null;
		if(AbstractDouble.isAbstractValue(doubleValue)){
			abstractDoubleType = AbstractDouble.lookupType(doubleValue);
			doubleValue = abstractDoubleType.getApproximateValue();
		}
		if(doubleValue == null){
			ps.setNull(parameterIndex++, Types.DOUBLE);
			ps.setNull(parameterIndex + updateOffset, Types.DOUBLE);
		}else{
			ps.setDouble(parameterIndex++, doubleValue);
			ps.setDouble(parameterIndex + updateOffset, doubleValue);
		}
		// Handle abstract doubles
		if(abstractDoubleType == null){
			ps.setNull(parameterIndex++, Types.VARCHAR);
			ps.setNull(parameterIndex + updateOffset, Types.VARCHAR);
		}else{
			ps.setString(parameterIndex++, abstractDoubleType.getEnumerationValue());
			ps.setString(parameterIndex + updateOffset, abstractDoubleType.getEnumerationValue());
		}
		// Handle booleans
		List<Boolean> booleanList = new ArrayList<>(stringList.size());
		BooleanParser booleanParser = new BooleanParser();
		for(String value : stringList){
			//if any values fail to parse, then the entire list of boolean is invalid
			if (!booleanParser.isOfType(value)){
				booleanList = null;
				break;
			}

			booleanList.add((Boolean) booleanParser.parseValueForDatabaseWrite(value));
		}
		Boolean booleanValue = booleanList == null || booleanList.isEmpty() ? null : booleanList.get(0);
		if(booleanValue == null){
			ps.setNull(parameterIndex++, Types.BOOLEAN);
			ps.setNull(parameterIndex + updateOffset, Types.BOOLEAN);
		}else{
			ps.setBoolean(parameterIndex++, booleanValue);
			ps.setBoolean(parameterIndex + updateOffset, booleanValue);
		}

		String stringListValue = stringList == null ? null : new JSONArray(stringList).toString();
		ps.setString(parameterIndex++, stringListValue);
		ps.setString(parameterIndex + updateOffset, stringListValue);
		
		String longListValue = longList == null ? null : new JSONArray(longList).toString();
		ps.setString(parameterIndex++, longListValue);
		ps.setString(parameterIndex + updateOffset, longListValue);
		
		String booleanListValue = booleanList == null ? null : new JSONArray(booleanList).toString();
		ps.setString(parameterIndex++, booleanListValue);
		ps.setString(parameterIndex + updateOffset, booleanListValue);

		Integer maxElementStringSize = stringList.stream()
				.map(String::length)
				.max(Integer::compareTo)
				.orElse(0);
		
		ps.setLong(parameterIndex++, maxElementStringSize);
		ps.setLong(parameterIndex + updateOffset, maxElementStringSize);
		
		ps.setLong(parameterIndex++, stringList.size());
		ps.setLong(parameterIndex + updateOffset, stringList.size());
		ps.setBoolean(parameterIndex++, dto.isDerived());
		ps.setBoolean(parameterIndex + updateOffset, dto.isDerived());
	}

	/**
	 * Create SQL to insert into a table for the IdAndVersion with the given headers.
	 * @param idAndVersion
	 * @param headers
	 * @return
	 */
	public static String createInsertIntoTableIndex(IdAndVersion idAndVersion, String[] headers) {
		String tableName = getTableNameForId(idAndVersion, TableIndexType.INDEX);
		StringBuilder builder = new StringBuilder();
		builder.append("INSERT INTO ");
		builder.append(tableName);
		boolean useBindVariables = false;
		buildHeaders(builder, headers, useBindVariables);
		builder.append(" VALUES ");
		useBindVariables = true;
		buildHeaders(builder, headers, useBindVariables);
		return builder.toString();
	}
	
	static void buildHeaders(StringBuilder builder, String[] headers, boolean useBindVariables) {
		builder.append(" (");
		boolean isFirst = true;
		for(String header: headers) {
			if(!isFirst) {
				builder.append(",");
			}
			if(useBindVariables) {
				builder.append("?");
			}else {
				builder.append(header);
			}
			isFirst = false;
		}
		builder.append(")");
	}
	
	/**
	 * Calculate the bytes of the given string array assuming 4 bytes per character.
	 * 
	 * @param row
	 * @return
	 */
	public static long calculateBytes(String[] row) {
		long rowSize = 0;
		for(String cell: row) {
			if(cell != null) {
				rowSize += cell.length()*4L;
			}
		}
		return rowSize;
	}


	static String createListColumnIndexTable(IdAndVersion tableIdAndVersion, ColumnModel columnModel, boolean alterTemp){
		ValidateArgument.required(tableIdAndVersion, "tableIdAndVersion");
		ValidateArgument.required(columnModel, "columnModel");
		ValidateArgument.requirement(ColumnTypeListMappings.isList(columnModel.getColumnType()), "columnModel's type must be a LIST type");

		String parentTable = alterTemp ? getTemporaryTableName(tableIdAndVersion) : getTableNameForId(tableIdAndVersion, TableIndexType.INDEX);
		String columnIndexTableName = getTableNameForMultiValueColumnIndex(tableIdAndVersion, columnModel.getId(), alterTemp);
		String columnName = getUnnestedColumnNameForId(columnModel.getId());
		String rowIdRefColumnName = getRowIdRefColumnNameForId(columnModel.getId());
		String columnTypeSql = ColumnTypeInfo.getInfoForType(ColumnTypeListMappings.nonListType(columnModel.getColumnType())).toSql(columnModel.getMaximumSize(), null, false);
		return "CREATE TABLE IF NOT EXISTS " + columnIndexTableName + " (" +
				rowIdRefColumnName + " BIGINT NOT NULL, " +
				INDEX_NUM + " BIGINT NOT NULL, " + //index of value in its list
				columnName + " " + columnTypeSql + ", " +
				"PRIMARY KEY (" + rowIdRefColumnName + ", " + INDEX_NUM + "), " +
				"INDEX " + columnName + "_IDX (" + columnName + " ASC), " +
				getMultiValueIndexTableForeignKeyConstraint(parentTable, columnIndexTableName, rowIdRefColumnName) +
				");";
	}

	private static String getMultiValueIndexTableForeignKeyConstraint(String parentTable, String columnIndexTableName, String rowIdRefColumnName) {
		return "CONSTRAINT " + getMultiValueIndexTableForeignKeyConstraintName(columnIndexTableName) + " FOREIGN KEY (" + rowIdRefColumnName + ") REFERENCES " + parentTable + "(" + ROW_ID + ") ON DELETE CASCADE";
	}
	
	static String getMultiValueIndexTableForeignKeyConstraintName(String columnIndexTableName) {
		// Note: the pattern tableName + _ibfk_ is important so that when renaming the table the FK is renamed automatically (See https://dev.mysql.com/doc/refman/8.0/en/rename-table.html)
		return columnIndexTableName + "_ibfk_FK";
	}

	/**
	 * 
	 * @param tableIdAndVersion
	 * @param columnInfo
	 * @param filterRows When true a where clause to filter by ROW_ID will be included.
	 * @param alterTemp
	 * @return
	 */
	public static String insertIntoListColumnIndexTable(IdAndVersion tableIdAndVersion, ColumnModel columnInfo, boolean filterRows, boolean alterTemp){
		String columnName = getColumnNameForId(columnInfo.getId());
		String unnestedColumnName = getUnnestedColumnNameForId(columnInfo.getId());

		String rowIdRefColumnName = getRowIdRefColumnNameForId(columnInfo.getId());
		String columnIndexTableName = getTableNameForMultiValueColumnIndex(tableIdAndVersion, columnInfo.getId(), alterTemp);
		String tableName = alterTemp ? getTemporaryTableName(tableIdAndVersion) : getTableNameForId(tableIdAndVersion, TableIndexType.INDEX);
		MySqlColumnType mySqlColumnType = ColumnTypeInfo.getInfoForType(ColumnTypeListMappings.nonListType(columnInfo.getColumnType())).getMySqlType();

		String columnExpandTypeSQl =  mySqlColumnType.name() + (mySqlColumnType.hasSize() && columnInfo.getMaximumSize() != null ? "("  + columnInfo.getMaximumSize() + ")" : "");
		String rowFilter = "";
		if(filterRows) {
			rowFilter = " WHERE "+tableName+"."+ROW_ID+" IN (:"+ID_PARAM_NAME+")";
		}

		
		return "INSERT INTO " + columnIndexTableName + " (" + rowIdRefColumnName + "," + INDEX_NUM + ","+ unnestedColumnName +") " +
				"SELECT " + ROW_ID + " ,  TEMP_JSON_TABLE.ORDINAL - 1 , TEMP_JSON_TABLE.COLUMN_EXPAND" +
				" FROM "+ tableName + ", JSON_TABLE(" +
				columnName +
				", '$[*]'" +
				" COLUMNS (" +
				" ORDINAL FOR ORDINALITY, " +
				// "error on error" ensures that data will not be replicated if varchar() size is too small to fit the values
				// see PLFM-6690
				" COLUMN_EXPAND " + columnExpandTypeSQl + " PATH '$' ERROR ON ERROR" +
				" )" +
				") TEMP_JSON_TABLE"+rowFilter;
	}
	
	/**
	 * Create SQL to find out-of-date rows for a view.
	 * @param viewId
	 * @param viewTypeMask
	 * @return
	 */
	public static String getOutOfDateRowsForViewSql(IdAndVersion viewId, String filterSql) {
		String viewName = SQLUtils.getTableNameForId(viewId, TableIndexType.INDEX);
		return String.format(VIEW_ROWS_OUT_OF_DATE_TEMPLATE, viewName, filterSql);
	}
	
	public static final String DELETE_ROWS_FROM_VIEW_TEMPLATE = "DELETE FROM %1$s WHERE "+ROW_ID+" = ?";

	/**
	 * Create SQL to delete the given rows from a view.
	 * @param viewId
	 * @return
	 */
	public static String getDeleteRowsFromViewSql(IdAndVersion viewId) {
		String viewName = SQLUtils.getTableNameForId(viewId, TableIndexType.INDEX);
		return String.format(DELETE_ROWS_FROM_VIEW_TEMPLATE, viewName);
	}

	public static String generateSqlToRefreshViewBenefactors(IdAndVersion viewId) {
		ValidateArgument.required(viewId, "viewId");
		String viewName = SQLUtils.getTableNameForId(viewId, TableIndexType.INDEX);
		return String.format("UPDATE %1$s T JOIN " + OBJECT_REPLICATION_TABLE + " O ON (T." + ROW_ID + " = O."
				+ OBJECT_REPLICATION_COL_OBJECT_ID + " AND O." + OBJECT_REPLICATION_COL_OBJECT_TYPE + " = ?) SET T."
				+ ROW_BENEFACTOR + " = O." + OBJECT_REPLICATION_COL_BENEFACTOR_ID + " WHERE T." + ROW_BENEFACTOR
				+ " <> O." + OBJECT_REPLICATION_COL_BENEFACTOR_ID, viewName);
	}
	
	/**
	 * Load a SQL string from the classpath.
	 * @param fileName
	 * @return
	 */
	public static String loadSQLFromClasspath(String fileName) {
		try(InputStream in = SQLUtils.class.getClassLoader().getResourceAsStream(fileName)){
			if(in == null){
				throw new RuntimeException("Failed to load the schema file from the classpath: "+fileName);
			}
			return IOUtils.toString(in, StandardCharsets.UTF_8.name());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
		
	public static String buildSelectTableDataByRowIdSQL(IdAndVersion id, List<ColumnModel> columns) {
		
		StringBuilder sql = buildSelectTableData(id, columns, ROW_ID)
				.append(" WHERE ").append(ROW_ID).append(" IN(:").append(ROW_ID).append(")");
		
		return sql.toString();
	}
	
	public static String buildSelectTableDataPage(IdAndVersion id, List<ColumnModel> columns) {
		
		StringBuilder sql = buildSelectTableData(id, columns, ROW_ID)
				.append(" ORDER BY ").append(ROW_ID).append(" LIMIT :").append(P_LIMIT).append(" OFFSET :").append(P_OFFSET);
		
		return sql.toString();
	}
	
	public static StringBuilder buildSelectTableData(IdAndVersion id, List<ColumnModel> columns, String ...metadataColumns) {
		ValidateArgument.required(id, "The id");
		ValidateArgument.requiredNotEmpty(columns, "The columns");
		
		StringBuilder builder = new StringBuilder("SELECT ");
		
		for (String metadataColumn : metadataColumns) {
			builder.append(metadataColumn).append(",");
		}
		
		return builder.append(String.join(",", getColumnNames(columns)))
				.append(" FROM ")
				.append(getTableNameForId(id, TableIndexType.INDEX));
	}
	
	public static List<String> getSelectTableDataHeaders(List<ColumnModel> columns, String ...metadataColumns) {
		ValidateArgument.required(columns, "The columns");
		List<String> headers = new ArrayList<>();
		if (metadataColumns != null) {
			headers.addAll(Arrays.asList(metadataColumns));
		}
		headers.addAll(getColumnNames(columns));
		return headers;
	}
	 
	public static String buildBatchUpdateSearchContentSql(IdAndVersion id) {
		ValidateArgument.required(id, "The id");
		
		return "UPDATE " + getTableNameForId(id, TableIndexType.INDEX) + " SET `" + ROW_SEARCH_CONTENT + "` = ? WHERE " + ROW_ID + " = ?";
	}
	
	public static String buildClearSearchContentSql(IdAndVersion id) {
		ValidateArgument.required(id, "The id");
		
		return "UPDATE " + getTableNameForId(id, TableIndexType.INDEX) + " SET `" + ROW_SEARCH_CONTENT + "` = NULL";
	}
	
}
