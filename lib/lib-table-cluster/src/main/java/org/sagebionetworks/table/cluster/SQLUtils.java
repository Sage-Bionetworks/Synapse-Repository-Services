package org.sagebionetworks.table.cluster;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;

/**
 * Utilities for generating Table SQL, DML, and DDL.
 * 
 * @author jmhill
 *
 */
public class SQLUtils {

	public static final String ROW_ID = "ROW_ID";
	public static final String ROW_VERSION = "ROW_VERSION";
	public static final String DEFAULT = "DEFAULT";
	
	/**
	 * Generate the SQL need to create or alter a table from one schema to another.
	 * @param oldSchema The original schema of the table.  Should be null if the table does not already exist.
	 * @param newSchema The new schema that the table should have when the resulting SQL is executed.
	 * @return
	 */
	public static String creatOrAlterTableSQL(List<ColumnModel> oldSchema, List<ColumnModel> newSchema, String tableId){
		if(oldSchema == null || oldSchema.isEmpty()){
			// This is a create
			return createTableSQL(newSchema, tableId);
		}else{
			//This is an alter
			return alterTableSql(oldSchema, newSchema, tableId);
		}
	}
	
	/**
	 * Alter a table by adding all new columns and removing all columns no longer used.
	 * @param oldSchema
	 * @param newSchema
	 * @param tableId
	 * @return
	 */
	public static String alterTableSql(List<ColumnModel> oldSchema, List<ColumnModel> newSchema, String tableId){
		// Calculate both the columns to add and remove.
		List<ColumnModel> toAdd = calculateColumnsToAdd(oldSchema, newSchema);
		List<ColumnModel> toDrop = calculateColumnsToDrop(oldSchema, newSchema);
		return alterTableSQLInner(toAdd, toDrop, tableId);
	}
	
	/**
	 * Given a new schema generate the create table DDL. 
	 * @param newSchema
	 * @return
	 */
	public static String createTableSQL(List<ColumnModel> newSchema, String tableId){
		if(newSchema == null) throw new IllegalArgumentException("Table schema cannot be null");
		if(newSchema.size() < 1) throw new IllegalArgumentException("Table schema must include at least one column");
		if(tableId == null) throw new IllegalArgumentException("TableId cannot be null");
		StringBuilder builder = new StringBuilder();
		builder.append("CREATE TABLE IF NOT EXISTS `T");
		builder.append(tableId);
		builder.append("` ");
		appendColumnDefinitionsToCreate(builder, newSchema);
		return builder.toString();
	}
	
	static void appendColumnDefinitionsToCreate(StringBuilder builder, List<ColumnModel> newSchema){
		builder.append("( ");
		// Every table must have a ROW_ID and ROW_VERSION
		builder.append(ROW_ID).append(" bigint(20) NOT NULL");
		builder.append(", ");
		builder.append(ROW_VERSION).append(" bigint(20) NOT NULL");
		for(int i=0; i<newSchema.size(); i++){
			builder.append(", ");
			appendColumnDefinitionsToBuilder(builder, newSchema.get(i));
		}
		builder.append(", PRIMARY KEY (").append(ROW_ID).append(") ");
		builder.append(")"); 
	}
	
	/**
	 * Append a column definition to the passed builder.
	 * @param builder
	 * @param newSchema
	 */
	static void appendColumnDefinitionsToBuilder(StringBuilder builder, ColumnModel newSchema){
		// Column name
		builder.append("`C");
		builder.append(newSchema.getId());
		builder.append("` ");
		// column data type
		builder.append(getSQLTypeForColumnType(newSchema.getColumnType()));
		builder.append(" ");
		// default value
		builder.append((getSQLDefaultForColumnType(newSchema.getColumnType(), newSchema.getDefaultValue())));
	}
	
	/**
	 * Get the DML for this column type.
	 * 
	 * @param type
	 * @return
	 */
	public static String getSQLTypeForColumnType(ColumnType type){
		if(ColumnType.LONG.equals(type) || ColumnType.FILEHANDLEID.equals(type)){
			return "bigint(20)";
		}else if(ColumnType.STRING.equals(type)){
			return "varchar("+TableModelUtils.MAX_STRING_LENGTH+") CHARACTER SET utf8 COLLATE utf8_general_ci";
		}else if(ColumnType.DOUBLE.equals(type)){
			return "double";
		}else if(ColumnType.BOOLEAN.equals(type)){
			return "boolean";
		} else{
			throw new IllegalArgumentException("Unknown type: "+type.name());
		}
	}
	
	/**
	 * Generate the Default part of a column definition.
	 * @param type
	 * @param defaultString
	 * @return
	 */
	public static String getSQLDefaultForColumnType(ColumnType type, String defaultString){
		if(defaultString == null) return DEFAULT+" NULL";
		try {
			if(ColumnType.LONG.equals(type) || ColumnType.FILEHANDLEID.equals(type)){
				// Convert the default to a long
				Long defaultValue = Long.parseLong(defaultString);
				return DEFAULT+" "+defaultValue.toString();
			}else if(ColumnType.STRING.equals(type)){
				return  DEFAULT+" '"+defaultString+"'";
			}else if(ColumnType.DOUBLE.equals(type)){
				Double doubleValue = Double.parseDouble(defaultString);
				return DEFAULT+" "+doubleValue.toString();
			}else if(ColumnType.BOOLEAN.equals(type)){
				boolean booleanValue = Boolean.parseBoolean(defaultString);
				if(booleanValue){
					return DEFAULT+" 1";
				}else{
					return DEFAULT+" 0";
				}
			}else{
				throw new IllegalArgumentException("Unknown type"+type.name());
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Generate the SQL needed to alter a table given the list of columns to add and drop.
	 * 
	 * @param toAdd
	 * @param toRemove
	 * @return
	 */
	public static String alterTableSQLInner(List<ColumnModel> toAdd, List<ColumnModel> toDrop, String tableId){
		StringBuilder builder = new StringBuilder();
		builder.append("ALTER TABLE ");
		builder.append("`T").append(tableId).append("`");
		boolean first = true;
		// Drops first
		for(ColumnModel drop: toDrop){
			if(!first){
				builder.append(",");
			}
			builder.append(" DROP COLUMN `C").append(drop.getId()).append("`");
			first = false;
		}
		// Now the adds
		for(ColumnModel add: toAdd){
			if(!first){
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
	public static List<ColumnModel> calculateColumnsToAdd(List<ColumnModel> oldSchema, List<ColumnModel> newSchema){
		// Add any column that is in the new schema but not in the old.
		Set<String> set = createColumnIdSet(oldSchema);
		return listNotInSet(set, newSchema);
	}
	
	/**
	 * Given both the old and new schema which columns need to be removed.
	 * @param oldSchema
	 * @param newSchema
	 * @return
	 */
	public static List<ColumnModel> calculateColumnsToDrop(List<ColumnModel> oldSchema, List<ColumnModel> newSchema){
		// Add any column in the old schema that is not in the new.
		Set<String> set = createColumnIdSet(newSchema);
		return listNotInSet(set, oldSchema);
	}
	
	/**
	 * Build up a set of column Ids from the passed schema.
	 * @param schema
	 * @return
	 */
	static Set<String> createColumnIdSet(List<ColumnModel> schema){
		HashSet<String> set = new HashSet<String>();
		for(ColumnModel cm: schema){
			if(cm.getId() == null) throw new IllegalArgumentException("ColumnId cannot be null");
			set.add(cm.getId());
		}
		return set;
	}
	
	/**
	 * Get the list of ColumnModels that do not have their IDs in the passed set.
	 * @param set
	 * @param schema
	 * @return
	 */
	static List<ColumnModel> listNotInSet(Set<String> set, List<ColumnModel> schema){
		List<ColumnModel> list = new LinkedList<ColumnModel>();
		for(ColumnModel cm: schema){
			if(!set.contains(cm.getId())){
				list.add(cm);
			}
		}
		return list;
	}
}

