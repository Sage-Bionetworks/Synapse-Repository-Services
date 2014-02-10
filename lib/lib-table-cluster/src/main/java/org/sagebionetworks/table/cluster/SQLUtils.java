package org.sagebionetworks.table.cluster;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * Utilities for generating Table SQL, DML, and DDL.
 * 
 * @author jmhill
 * 
 */
public class SQLUtils {

	public static final String ROW_ID = "ROW_ID";
	public static final String ROW_ID_BIND = "bRI";
	public static final String ROW_VERSION = "ROW_VERSION";
	public static final String ROW_VERSION_BIND = "bRV";
	public static final String DEFAULT = "DEFAULT";
	public static final String TABLE_PREFIX = "T";
	public static final String COLUMN_PREFIX = "C";
	public static final Integer TRUE_INT = new Integer(1);
	public static final Integer FALSE_INT = new Integer(0);
	
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
	public static String creatOrAlterTableSQL(List<String> oldSchema,
			List<ColumnModel> newSchema, String tableId) {
		if (oldSchema == null || oldSchema.isEmpty()) {
			// This is a create
			return createTableSQL(newSchema, tableId);
		} else {
			// This is an alter
			return alterTableSql(oldSchema, newSchema, tableId);
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
	public static String alterTableSql(List<String> oldSchema,
			List<ColumnModel> newSchema, String tableId) {
		// Calculate both the columns to add and remove.
		List<ColumnModel> toAdd = calculateColumnsToAdd(oldSchema, newSchema);
		List<String> toDrop = calculateColumnsToDrop(oldSchema, newSchema);
		// There is nothing to do if both are empty
		if(toAdd.isEmpty() && toDrop.isEmpty()) return null;
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
		if (newSchema == null)
			throw new IllegalArgumentException("Table schema cannot be null");
		if (newSchema.size() < 1)
			throw new IllegalArgumentException(
					"Table schema must include at least one column");
		if (tableId == null)
			throw new IllegalArgumentException("TableId cannot be null");
		StringBuilder builder = new StringBuilder();
		builder.append("CREATE TABLE IF NOT EXISTS `").append(getTableNameForId(tableId)).append("` ");
		appendColumnDefinitionsToCreate(builder, newSchema);
		return builder.toString();
	}

	static void appendColumnDefinitionsToCreate(StringBuilder builder,
			List<ColumnModel> newSchema) {
		builder.append("( ");
		// Every table must have a ROW_ID and ROW_VERSION
		builder.append(ROW_ID).append(" bigint(20) NOT NULL");
		builder.append(", ");
		builder.append(ROW_VERSION).append(" bigint(20) NOT NULL");
		for (int i = 0; i < newSchema.size(); i++) {
			builder.append(", ");
			appendColumnDefinitionsToBuilder(builder, newSchema.get(i));
		}
		builder.append(", PRIMARY KEY (").append(ROW_ID).append(") ");
		builder.append(")");
	}

	/**
	 * Append a column definition to the passed builder.
	 * 
	 * @param builder
	 * @param newSchema
	 */
	static void appendColumnDefinitionsToBuilder(StringBuilder builder,
			ColumnModel newSchema) {
		// Column name
		builder.append("`").append(getColumnNameForId(newSchema.getId())).append("` ");
		// column data type
		builder.append(getSQLTypeForColumnType(newSchema.getColumnType()));
		builder.append(" ");
		// default value
		builder.append((getSQLDefaultForColumnType(newSchema.getColumnType(),
				newSchema.getDefaultValue())));
	}

	/**
	 * Get the DML for this column type.
	 * 
	 * @param type
	 * @return
	 */
	public static String getSQLTypeForColumnType(ColumnType type) {
		if (ColumnType.LONG.equals(type)
				|| ColumnType.FILEHANDLEID.equals(type)) {
			return "bigint(20)";
		} else if (ColumnType.STRING.equals(type)) {
			return "varchar(" + TableModelUtils.MAX_STRING_LENGTH
					+ ") CHARACTER SET utf8 COLLATE utf8_general_ci";
		} else if (ColumnType.DOUBLE.equals(type)) {
			return "double";
		} else if (ColumnType.BOOLEAN.equals(type)) {
			return "boolean";
		} else {
			throw new IllegalArgumentException("Unknown type: " + type.name());
		}
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
			if(ColumnType.STRING.equals(type)){
				return value;
			}else if(ColumnType.DOUBLE.equals(type)){
				return Double.parseDouble(value);
			}else if(ColumnType.LONG.equals(type) || ColumnType.FILEHANDLEID.equals(type)){
				return Long.parseLong(value);
			}else if (ColumnType.BOOLEAN.equals(type)) {
				boolean booleanValue = Boolean.parseBoolean(value);
				if (booleanValue) {
					return TRUE_INT;
				} else {
					return FALSE_INT;
				}
			}else{
				throw new IllegalArgumentException("Unknown Type: "+type);
			}
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
		if(ColumnType.STRING.equals(type)){
			builder.append("'");
		}
		builder.append(objectValue.toString());
		if(ColumnType.STRING.equals(type)){
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
	public static String alterTableSQLInner(List<ColumnModel> toAdd,
			List<String> toDrop, String tableId) {
		StringBuilder builder = new StringBuilder();
		builder.append("ALTER TABLE ");
		builder.append("`").append(getTableNameForId(tableId)).append("`");
		boolean first = true;
		// Drops first
		for (String drop : toDrop) {
			if (!first) {
				builder.append(",");
			}
			builder.append(" DROP COLUMN `").append(getColumnNameForId(drop)).append("`");
			first = false;
		}
		// Now the adds
		for (ColumnModel add : toAdd) {
			if (!first) {
				builder.append(",");
			}
			builder.append(" ADD COLUMN ");
			appendColumnDefinitionsToBuilder(builder, add);
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
	public static List<ColumnModel> calculateColumnsToAdd(
			List<String> oldSchema, List<ColumnModel> newSchema) {
		// Add any column that is in the new schema but not in the old.
		Set<String> set = new HashSet<String>(oldSchema);
		return listNotInSet(set, newSchema);
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
	public static String getTableNameForId(String tableId) {
		if (tableId == null)
			throw new IllegalArgumentException("Table ID cannot be null");
		return TABLE_PREFIX + KeyFactory.stringToKey(tableId);
	}

	/**
	 * Get a table ID from a TableName
	 * 
	 * @param tableName
	 * @return
	 */
	public static String getTableIdForTableName(String tableName) {
		if (tableName == null)
			throw new IllegalArgumentException("TableName cannot be null");
		return tableName.substring(TABLE_PREFIX.length());
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
		return COLUMN_PREFIX + columnId.toString();
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
		return columnName.substring(COLUMN_PREFIX.length());
	}
	
	/**
	 * Does the column name start with the column prefix?
	 * @param columnName
	 * @return
	 */
	public static boolean hasColumnPrefixe(String columnName){
		if (columnName == null)
			throw new IllegalArgumentException("Column name cannot be null");
		return columnName.startsWith(COLUMN_PREFIX);
	}
	
	/**
	 * Create the DROP table SQL.
	 * @param tableId
	 * @return
	 */
	public static String dropTableSQL(String tableId){
		String tableName = getTableNameForId(tableId);
		StringBuilder builder = new StringBuilder();
		builder.append("DROP TABLE ").append(tableName);
		return builder.toString();
	}
	
	/**
	 * Convert a list of column names to a list of column IDs.
	 * Note: Any column that is not derived from a ColumnModel will not be
	 * included in the results.
	 * 
	 * @param names
	 * @return
	 */
	public static List<String> convertColumnNamesToColumnId(List<String> names){
		List<String> results = new LinkedList<String>();
		if(names == null) return null;
		for(String name: names){
			if(hasColumnPrefixe(name)){
				String columId = getColumnIdForColumnName(name);
				results.add(columId);
			}
		}
		return results;
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
		builder.append(getTableNameForId(tableId));
		builder.append(" (");
		// Unconditionally set these two columns
		builder.append(ROW_ID);
		builder.append(", ").append(ROW_VERSION);
		for(ColumnModel cm: schema){
			builder.append(", ");
			builder.append(getColumnNameForId(cm.getId()));
		}
		builder.append(") VALUES ( :").append(ROW_ID_BIND).append(", :").append(ROW_VERSION_BIND);
		for(ColumnModel cm: schema){
			builder.append(", :");
			builder.append(getColumnNameForId(cm.getId()));
		}
		builder.append(") ON DUPLICATE KEY UPDATE ROW_VERSION = :").append(ROW_VERSION_BIND);
		for(ColumnModel cm: schema){
			builder.append(", ");
			String name = getColumnNameForId(cm.getId());
			builder.append(name);
			builder.append(" = :").append(name);
		}
		return builder.toString();
	}

	
	/**
	 * Build the parameters that will bind the passed RowSet to a SQL statement.
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
		MapSqlParameterSource[] results = new MapSqlParameterSource[toBind.getRows().size()];
		int rowIndex = 0;
		for(Row row: toBind.getRows()){
			Map<String, Object> rowMap = new HashMap<String, Object>(schema.size()+2);
			// Always bind the row ID and version
			if(row.getRowId() == null) throw new IllegalArgumentException("RowID cannot be null");
			if(row.getVersionNumber() == null) throw new IllegalArgumentException("RowVersionNumber cannot be null");
			rowMap.put(ROW_ID_BIND, row.getRowId());
			rowMap.put(ROW_VERSION_BIND, row.getVersionNumber());
			// Bind each column
			for(ColumnModel cm: schema){
				// Lookup the index of this column
				String columnName = getColumnNameForId(cm.getId());
				Integer columnIndex = columnIndexMap.get(cm.getId());
				Object value = null;
				if(columnIndex == null){
					// Use the default value for this column
					value = parseValueForDB(cm.getColumnType(), cm.getDefaultValue());
				}else{
					value = parseValueForDB(cm.getColumnType(), row.getValues().get(columnIndex));
				}
				rowMap.put(columnName, value);
			}
			results[rowIndex] = new MapSqlParameterSource(rowMap);
			rowIndex++;
		}
		return results;
	}
	
	/**
	 * Create the SQL used to get the max version number from a table.
	 * @return
	 */
	public static String getCountSQL(String tableId){
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT COUNT(").append(ROW_ID).append(") FROM ").append(getTableNameForId(tableId));
		return builder.toString();
	}
	
	/**
	 * Create the SQL used to get the max version number from a table.
	 * @return
	 */
	public static String getMaxVersionSQL(String tableId){
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT MAX(").append(ROW_VERSION).append(") FROM ").append(getTableNameForId(tableId));
		return builder.toString();
	}
}
