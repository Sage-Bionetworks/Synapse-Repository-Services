package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.utils.ColumnConstants;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.TimeUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Utilities for generating Table SQL, DML, and DDL.
 * 
 * @author jmhill
 * 
 */
public class SQLUtils {


	public static final String ROW_ID_BIND = "bRI";
	public static final String ROW_VERSION_BIND = "bRV";
	public static final String DEFAULT = "DEFAULT";
	public static final String TABLE_PREFIX = "T";
	private static final String COLUMN_PREFIX = "_C";
	private static final String COLUMN_POSTFIX = "_";

	private static final Pattern COLUMN_NAME_PATTERN = Pattern.compile(COLUMN_PREFIX + "(\\d+)" + COLUMN_POSTFIX);

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
		 * the current row table that holds the current versions for each row in a table
		 */
		CURRENT_ROW("CR");

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
	 * Generate the SQL need to create or alter a table from one schema to
	 * another.
	 * 
	 * @param oldSchema
	 *            The original schema of the table. Should be null if the table
	 *            does not already exist.
	 * @param newSchema
	 *            The new schema that the table should have when the resulting
	 *            SQL is executed.
	 * @return
	 */
	public static String creatOrAlterTableSQL(List<String> oldColumns,
			List<ColumnModel> newSchema, String tableId) {
		if (oldColumns == null || oldColumns.isEmpty()) {
			// This is a create
			return createTableSQL(newSchema, tableId);
		} else {
			// This is an alter
			return alterTableSql(oldColumns, newSchema, tableId);
		}
	}

	/**
	 * Alter a table by adding all new columns and removing all columns no
	 * longer used.
	 * 
	 * @param oldSchema
	 * @param newSchema
	 * @param tableId
	 * @return
	 */
	public static String alterTableSql(List<String> oldColumns,
			List<ColumnModel> newSchema, String tableId) {
		// Calculate both the columns to add and remove.
		List<ColumnModel> toAdd = Lists.newArrayList();
		List<String> toDrop = Lists.newArrayList();
		calculateColumnsToAddOrDrop(oldColumns, newSchema, toAdd, toDrop);
		// There is nothing to do if both are empty
		if (toAdd.isEmpty() && toDrop.isEmpty()) {
			return null;
		}
		return alterTableSQLInner(toAdd, toDrop, tableId);
	}

	/**
	 * Given a new schema generate the create table DDL.
	 * 
	 * @param newSchema
	 * @return
	 */
	public static String createTableSQL(List<ColumnModel> newSchema,
			String tableId) {
		ValidateArgument.required(newSchema, "Table schema");
		ValidateArgument.requirement(newSchema.size() >= 1, "Table schema must include at least one column");
		ValidateArgument.required(tableId, "tableId");
		String columnDefinitions = getColumnDefinitionsToCreate(newSchema);
		return createTableSQL(tableId, TableType.INDEX, columnDefinitions);
	}

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
			columnDefinitions.append("single_key ENUM('') NOT NULL PRIMARY KEY, ");
			columnDefinitions.append(ROW_VERSION).append(" bigint(20) NOT NULL");
			break;
		case CURRENT_ROW:
			columnDefinitions.append(ROW_ID).append(" bigint(20) NOT NULL PRIMARY KEY, ");
			columnDefinitions.append(ROW_VERSION).append(" bigint(20) NOT NULL, ");
			columnDefinitions.append("INDEX `" + getTableNameForId(tableId, type) + "_VERSION_INDEX` (" + ROW_VERSION + ") ");
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

	private static String getColumnDefinitionsToCreate(List<ColumnModel> newSchema) {
		List<String> columns = Lists.newArrayListWithExpectedSize(newSchema.size() * 2);
		// Every table must have a ROW_ID and ROW_VERSION
		columns.add(ROW_ID + " bigint(20) NOT NULL");
		columns.add(ROW_VERSION + " bigint(20) NOT NULL");
		for (int i = 0; i < newSchema.size(); i++) {
			appendColumnDefinitionsToBuilder(columns, newSchema.get(i), false);
		}
		columns.add("PRIMARY KEY (" + ROW_ID + ")");
		return StringUtils.join(columns, ", ");
	}

	/**
	 * Append a column definition to the passed builder.
	 * 
	 * @param builder
	 * @param newSchema
	 */
	static void appendColumnDefinitionsToBuilder(List<String> columns,
			ColumnModel newSchema, boolean justNames) {
		switch (newSchema.getColumnType()) {
		case DOUBLE:
			if (justNames) {
				columns.add(getColumnNameForId(newSchema.getId()));
				columns.add(TableConstants.DOUBLE_PREFIX + getColumnNameForId(newSchema.getId()));
			} else {
				columns.add("`" + getColumnNameForId(newSchema.getId()) + "` "
						+ getSQLTypeForColumnType(newSchema.getColumnType(), newSchema.getMaximumSize()) + " "
						+ getSQLDefaultForColumnType(newSchema.getColumnType(), newSchema.getDefaultValue()));
				columns.add("`" + TableConstants.DOUBLE_PREFIX + getColumnNameForId(newSchema.getId()) + "` " + DOUBLE_ENUM_CLAUSE);
			}
			break;
		default:
			if (justNames) {
				columns.add(getColumnNameForId(newSchema.getId()));
			} else {
				columns.add("`" + getColumnNameForId(newSchema.getId()) + "` "
						+ getSQLTypeForColumnType(newSchema.getColumnType(), newSchema.getMaximumSize()) + " "
						+ getSQLDefaultForColumnType(newSchema.getColumnType(), newSchema.getDefaultValue()));
			}
			break;
		}
	}

	/**
	 * Get the DML for this column type.
	 * 
	 * @param type
	 * @return
	 */
	public static String getSQLTypeForColumnType(ColumnType type, Long maxSize) {
		if (type == null) {
			throw new IllegalArgumentException("ColumnType cannot be null");
		}
		switch (type) {
		case INTEGER:
		case FILEHANDLEID:
		case DATE:
			return "bigint(20)";
		case ENTITYID:
			return "varchar(" + ColumnConstants.MAX_ENTITY_ID_BYTES_AS_STRING + ") CHARACTER SET utf8 COLLATE utf8_general_ci";
		case LINK:
		case STRING:
			// Strings and links must have a size
			if (maxSize == null)
				throw new IllegalArgumentException("Cannot create a string column without a max size.");
			return "varchar(" + maxSize + ") CHARACTER SET utf8 COLLATE utf8_general_ci";
		case DOUBLE:
			return "double";
		case BOOLEAN:
			return "boolean";
		}
		throw new IllegalArgumentException("Unknown type: " + type.name());
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
		try {
			switch (type) {
			case STRING:
			case ENTITYID:
			case LINK:
				return value;
			case DOUBLE:
				return Double.parseDouble(value);
			case INTEGER:
			case FILEHANDLEID:
				return Long.parseLong(value);
			case DATE:
				// value can be either a number (in which case it is milliseconds since blah) or not a number (in which
				// case it is date string)
				try {
					return Long.parseLong(value);
				} catch (NumberFormatException e) {
					return TimeUtils.parseSqlDate(value);
				}
			case BOOLEAN:
				boolean booleanValue = Boolean.parseBoolean(value);
				return booleanValue;
			}
			throw new IllegalArgumentException("Unknown Type: " + type);
		} catch (NumberFormatException e) {
			// Convert all parsing errors to illegal args.
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Generate the Default part of a column definition.
	 * 
	 * @param type
	 * @param defaultString
	 * @return
	 */
	public static String getSQLDefaultForColumnType(ColumnType type,
			String defaultString) {
		if (defaultString == null)
			return DEFAULT + " NULL";
		// Prevent SQL injection attack
		defaultString = StringEscapeUtils.escapeSql(defaultString);
		Object objectValue = parseValueForDB(type, defaultString);
		StringBuilder builder = new StringBuilder();
		builder.append(DEFAULT).append(" ");
		boolean needsStringEscape = type == ColumnType.STRING || type == ColumnType.ENTITYID || type == ColumnType.LINK;
		if (needsStringEscape) {
			builder.append("'");
		}
		builder.append(objectValue.toString());
		if (needsStringEscape) {
			builder.append("'");
		}
		return builder.toString();
	}

	/**
	 * Generate the SQL needed to alter a table given the list of columns to add
	 * and drop.
	 * 
	 * @param toAdd
	 * @param toRemove
	 * @return
	 */
	public static String alterTableSQLInner(Iterable<ColumnModel> toAdd, Iterable<String> toDrop, String tableId) {
		StringBuilder builder = new StringBuilder();
		builder.append("ALTER TABLE ");
		builder.append("`").append(getTableNameForId(tableId, TableType.INDEX)).append("`");
		boolean first = true;
		// Drops first
		for (String drop : toDrop) {
			if (!first) {
				builder.append(",");
			}
			builder.append(" DROP COLUMN `").append(drop).append("`");
			first = false;
		}
		// Now the adds
		List<String> columnsToAdd = Lists.newArrayList();
		for (ColumnModel add : toAdd) {
			appendColumnDefinitionsToBuilder(columnsToAdd, add, false);
		}
		for (String add : columnsToAdd) {
			if (!first) {
				builder.append(",");
			}
			builder.append(" ADD COLUMN ").append(add);
			first = false;
		}
		return builder.toString();
	}

	/**
	 * Given both the old and new schema which columns need to be added.
	 * 
	 * @param oldSchema
	 * @param newSchema
	 * @return
	 */
	static void calculateColumnsToAddOrDrop(List<String> oldColumns, List<ColumnModel> newSchema, List<ColumnModel> toAdd,
			List<String> toDrop) {
		Set<String> oldColumnSet = Sets.newLinkedHashSet(oldColumns);
		oldColumnSet.remove(ROW_ID);
		oldColumnSet.remove(ROW_VERSION);
		for (ColumnModel cm : newSchema) {
			List<String> columnNames = Lists.newArrayList();
			appendColumnDefinitionsToBuilder(columnNames, cm, true);
			boolean added = false;
			for (String columnName : columnNames) {
				if (!oldColumnSet.remove(columnName)) {
					added = true;
				}
			}
			if (added) {
				toAdd.add(cm);
			}
		}
		toDrop.addAll(oldColumnSet);
	}

	/**
	 * Given both the old and new schema which columns need to be removed.
	 * 
	 * @param oldSchema
	 * @param newSchema
	 * @return
	 */
	public static List<String> calculateColumnsToDrop(
			List<String> oldSchema, List<ColumnModel> newSchema) {
		// Add any column in the old schema that is not in the new.
		Set<String> set = createColumnIdSet(newSchema);
		List<String> toDrop = new LinkedList<String>();
		for(String columnId: oldSchema){
			if(!set.contains(columnId)){
				toDrop.add(columnId);
			}
		}
		return toDrop;
	}

	/**
	 * Build up a set of column Ids from the passed schema.
	 * 
	 * @param schema
	 * @return
	 */
	static Set<String> createColumnIdSet(List<ColumnModel> schema) {
		HashSet<String> set = new HashSet<String>();
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
	 * is the a certain type of table name?
	 */
	public static boolean isTableName(String name, TableType type) {
		return type.getTableNamePattern().matcher(name).matches();
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

	static Iterable<String> getColumnNames(final Iterable<ColumnModel> columnModels) {
		return new Iterable<String>() {
			@Override
			public Iterator<String> iterator() {
				final Iterator<ColumnModel> cmIterator = columnModels.iterator();
				return new Iterator<String>() {
					Iterator<String> names = null;

					@Override
					public void remove() {
						cmIterator.remove();
					}

					@Override
					public String next() {
						if (names != null) {
							String name = names.next();
							if (!names.hasNext()) {
								names = null;
							}
							return name;
						} else {
							ColumnModel cm = cmIterator.next();
							List<String> columns = Lists.newArrayList();
							appendColumnDefinitionsToBuilder(columns, cm, true);
							if (columns.size() == 1) {
								return columns.get(0);
							} else {
								names = columns.iterator();
								return names.next();
							}
						}
					}

					@Override
					public boolean hasNext() {
						if (names == null) {
							return cmIterator.hasNext();
						} else {
							return names.hasNext();
						}
					}
				};
			}
		};
	}

	public static void appendColumnName(ColumnModel column, StringBuilder builder) {
		builder.append(COLUMN_PREFIX).append(column.getId()).append(COLUMN_POSTFIX);
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
	public static void appendDoubleCase(ColumnModel column, String subName, String tableName, boolean isSelectClause, StringBuilder builder) {
		if (isSelectClause) {
			builder.append("CASE WHEN ").append(TableConstants.DOUBLE_PREFIX).append(subName);
			appendColumnName(column, builder);
			builder.append(" IS NULL THEN ").append(subName);
			appendColumnName(column, builder);
			builder.append(" ELSE ").append(TableConstants.DOUBLE_PREFIX).append(subName);
			appendColumnName(column, builder);
			builder.append(" END AS ").append(subName);
			appendColumnName(column, builder);
		} else {
			if (tableName == null) {
				throw new IllegalStateException("Table name should be available at this point");
			}
			builder.append(tableName).append(".").append(subName);
			appendColumnName(column, builder);
		}
	}

	/**
	 * Get the
	 * 
	 * @param columnName
	 * @return
	 */
	public static String getColumnIdForColumnName(String columnName) {
		if (columnName == null)
			throw new IllegalArgumentException("Column name cannot be null");
		Matcher matcher = COLUMN_NAME_PATTERN.matcher(columnName);
		if(!matcher.matches()){
			throw new IllegalArgumentException("name '" + columnName + "' is not a column name");
		}
		return matcher.group(1);
	}

	public static String replaceColumnNames(String name, Map<Long, ColumnModel> columnIdToModelMap) {
		Matcher matcher = COLUMN_NAME_PATTERN.matcher(name);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			String columnId = matcher.group(1);
			ColumnModel columnModel = columnIdToModelMap.get(Long.parseLong(columnId));
			if (columnModel != null) {
				matcher.appendReplacement(sb, columnModel.getName());
			}
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	/**
	 * Does the column name start with the column prefix?
	 * @param columnName
	 * @return
	 */
	public static boolean isColumnName(String columnName){
		if (columnName == null)
			throw new IllegalArgumentException("Column name cannot be null");
		return COLUMN_NAME_PATTERN.matcher(columnName).matches();
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
		for (String columnName : getColumnNames(schema)) {
			builder.append(", ");
			builder.append(columnName);
		}
		builder.append(") VALUES ( :").append(ROW_ID_BIND).append(", :").append(ROW_VERSION_BIND);
		for (String columnName : getColumnNames(schema)) {
			builder.append(", :");
			builder.append(columnName);
		}
		builder.append(") ON DUPLICATE KEY UPDATE " + ROW_VERSION + " = VALUES(" + ROW_VERSION + ")");
		for (String columnName : getColumnNames(schema)) {
			builder.append(", ");
			builder.append(columnName);
			builder.append(" = VALUES(").append(columnName).append(")");
		}
		return builder.toString();
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
		builder.append("REPLACE INTO ");
		builder.append(getTableNameForId(tableId, TableType.STATUS));
		builder.append(" ( ");
		builder.append(ROW_VERSION);
		builder.append(" ) VALUES ( ? )");
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
		for (String header : toBind.getHeaders()) {
			columnIndexMap.put(header, index);
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
	 * Create the SQL used to get the max version number from a table.
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

	public static String selectCurrentRowVersionsForRowRange(Long tableId) {
		return "SELECT " + ROW_ID + "," + ROW_VERSION + " FROM " + getTableNameForId(tableId, TableType.CURRENT_ROW) + " WHERE " + ROW_ID
				+ " >= ? AND " + ROW_ID + " < ?";
	}

	public static String selectCurrentRowVersionsForInRows(Long tableId) {
		return "SELECT " + ROW_ID + "," + ROW_VERSION + " FROM " + getTableNameForId(tableId, TableType.CURRENT_ROW) + " WHERE " + ROW_ID
				+ " IN ( :" + ROW_ID_BIND + " )";
	}

	public static String selectCurrentRowVersionOfRow(Long tableId) {
		return "SELECT " + ROW_VERSION + " FROM " + getTableNameForId(tableId, TableType.CURRENT_ROW) + " WHERE " + ROW_ID + " = ?";
	}

	public static String updateCurrentRowSQL(Long tableId) {
		return "INSERT INTO " + getTableNameForId(tableId, TableType.CURRENT_ROW) + " (" + ROW_ID + ", " + ROW_VERSION
				+ ") VALUES (?, ?) ON DUPLICATE KEY UPDATE " + ROW_VERSION + " = VALUES(" + ROW_VERSION + ")";
	}

	public static String selectCurrentRowMaxVersion(Long tableId) {
		return "SELECT MAX(" + ROW_VERSION + ") FROM " + getTableNameForId(tableId, TableType.CURRENT_ROW);
	}
}
