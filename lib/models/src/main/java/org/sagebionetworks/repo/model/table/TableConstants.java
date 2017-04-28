package org.sagebionetworks.repo.model.table;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.EntityType;

import com.google.common.collect.ImmutableMap;

public class TableConstants {

	/**
	 * The reserved column name for row id.
	 */
	public static final String ROW_ID = "ROW_ID";
	/**
	 * The reserved column id for row id.
	 */
	public static final Long ROW_ID_ID = -1L;
	/**
	 * The reserved column name for row version.
	 */
	public static final String ROW_VERSION = "ROW_VERSION";
	public static final String SINGLE_KEY = "SINGLE_KEY";
	public static final String SCHEMA_HASH = "SCHEMA_HASH";
	/**
	 * FileHandle IDs 
	 */
	public static final String FILE_ID = "FILE_ID";
	/**
	 * The reserved column name for row version.
	 */
	public static final Long ROW_VERSION_ID = -2L;

	/**
	 * The set of reserved column names includes things like ROW_ID and
	 * ROW_VERSION
	 */
	private static final Set<String> RESERVED_COLUMNS_NAMES = new HashSet<String>(
			Arrays.asList(ROW_ID, ROW_VERSION));

	/**
	 * The Map of reserved column names like ROW_ID and
	 * ROW_VERSION to pseudo ids
	 */
	private static final Map<String, Long> RESERVED_COLUMNS_IDS = ImmutableMap.<String, Long> builder().put(ROW_ID, ROW_ID_ID)
			.put(ROW_VERSION, ROW_VERSION_ID).build();
	
	/**
	 * The column name prefix for extra doubles column.
	 */
	public static final String DOUBLE_PREFIX = "_DBL";

	/**
	 * The set of key words that are defined in SQL.
	 */
	private static final Set<String> KEY_WORDS = new HashSet<String>(
			Arrays.asList("ALL", "AND", "ASC", "AS", "AVG", "BETWEEN", "DATE",
					"COUNT", "DESC", "CURRENT_DATE", "CURRENT_TIME", "DEFAULT",
					"DISTINCT", "ESCAPE", "FALSE", "FROM", "GROUP BY",
					"INTERVAL", "IN", "IS", "LIKE", "LIMIT", "MAX", "MIN",
					"NOT", "NULL", "OFFSET", "OR", "ORDER BY", "SELECT", "SUM",
					"TIME", "TIMESTAMP", "TRUE", "WHERE", "UNKNOWN"));

	/**
	 * Is the passed column name a reserved column name like ROW_ID or
	 * ROW_VERSION?
	 * 
	 * @return
	 */
	public static boolean isReservedColumnName(String columnName) {
		if (columnName == null)
			return false;
		// Make it case insensitive.
		return RESERVED_COLUMNS_NAMES.contains(columnName.toUpperCase().trim());
	}

	/**
	 * Return the pseudo id if the passed column name is a reserved column name like ROW_ID or
	 * ROW_VERSION?
	 * 
	 * @return
	 */
	public static Long getReservedColumnId(String columnName) {
		if (columnName == null)
			return null;
		// Make it case insensitive.s
		return RESERVED_COLUMNS_IDS.get(columnName.toUpperCase().trim());
	}

	/**
	 * Is the passed word a SQL key word?
	 * 
	 * @param word
	 * @return
	 */
	public static boolean isKeyWord(String word) {
		if (word == null)
			return false;
		// Make it case insensitive.
		return KEY_WORDS.contains(word.toUpperCase().trim());
	}
	// ENTITY_REPLICATION
	public static final String ENTITY_REPLICATION_TABLE 			= "ENTITY_REPLICATION";
	public static final String ENTITY_REPLICATION_COL_ID			= "ID";
	public static final String ENTITY_REPLICATION_COL_VERSION		= "CURRENT_VERSION";
	public static final String ENTITY_REPLICATION_COL_CRATED_BY		= "CREATED_BY";
	public static final String ENTITY_REPLICATION_COL_CRATED_ON		= "CREATED_ON";
	public static final String ENTITY_REPLICATION_COL_ETAG			= "ETAG";
	public static final String ENTITY_REPLICATION_COL_NAME			= "NAME";
	public static final String ENTITY_REPLICATION_COL_TYPE			= "TYPE";
	public static final String ENTITY_REPLICATION_COL_PARENT_ID		= "PARENT_ID";
	public static final String ENTITY_REPLICATION_COL_BENEFACTOR_ID	= "BENEFACTOR_ID";
	public static final String ENTITY_REPLICATION_COL_PROJECT_ID	= "PROJECT_ID";
	public static final String ENTITY_REPLICATION_COL_MODIFIED_BY	= "MODIFIED_BY";
	public static final String ENTITY_REPLICATION_COL_MODIFIED_ON	= "MODIFIED_ON";
	public static final String ENTITY_REPLICATION_COL_FILE_ID		= "FILE_ID";
	
	// Dynamic string of all of the entity types.
	public static final String ENTITY_TYPES;
	static{
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for(EntityType type: EntityType.values()){
			if(!first){
				builder.append(", ");
			}
			builder.append("'");
			builder.append(type.name());
			builder.append("'");
			first = false;
		}
		ENTITY_TYPES = builder.toString();
	}

	public final static String ENTITY_REPLICATION_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS "+ENTITY_REPLICATION_TABLE+"("
			+ ENTITY_REPLICATION_COL_ID +" bigint(20) NOT NULL,"
			+ ENTITY_REPLICATION_COL_VERSION +" bigint(20) NOT NULL,"
			+ ENTITY_REPLICATION_COL_CRATED_BY +" bigint(20) NOT NULL,"
			+ ENTITY_REPLICATION_COL_CRATED_ON +" bigint(20) NOT NULL,"
			+ ENTITY_REPLICATION_COL_ETAG +" char(36) NOT NULL,"
			+ ENTITY_REPLICATION_COL_NAME +" varchar(256) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,"
			+ ENTITY_REPLICATION_COL_TYPE +" ENUM("+ENTITY_TYPES+") NOT NULL,"
			+ ENTITY_REPLICATION_COL_PARENT_ID +" bigint(20) DEFAULT NULL,"
			+ ENTITY_REPLICATION_COL_BENEFACTOR_ID +" bigint(20) DEFAULT NULL,"
			+ ENTITY_REPLICATION_COL_PROJECT_ID +" bigint(20) DEFAULT NULL,"
			+ ENTITY_REPLICATION_COL_MODIFIED_BY +" bigint(20) NOT NULL,"
			+ ENTITY_REPLICATION_COL_MODIFIED_ON +" bigint(20) NOT NULL,"
			+ ENTITY_REPLICATION_COL_FILE_ID +" bigint(20) DEFAULT NULL,"
			+ "PRIMARY KEY("+ENTITY_REPLICATION_COL_ID+")"
			+ ", INDEX ("+ENTITY_REPLICATION_COL_VERSION+")"
			+ ", INDEX ("+ENTITY_REPLICATION_COL_CRATED_BY+")"
			+ ", INDEX ("+ENTITY_REPLICATION_COL_CRATED_ON+")"
			+ ", INDEX ("+ENTITY_REPLICATION_COL_ETAG+")"
			+ ", INDEX ("+ENTITY_REPLICATION_COL_NAME+")"
			+ ", INDEX ("+ENTITY_REPLICATION_COL_TYPE+")"
			+ ", INDEX ("+ENTITY_REPLICATION_COL_PARENT_ID+")"
			+ ", INDEX ("+ENTITY_REPLICATION_COL_BENEFACTOR_ID+")"
			+ ", INDEX ("+ENTITY_REPLICATION_COL_PROJECT_ID+")"
			+ ", INDEX ("+ENTITY_REPLICATION_COL_MODIFIED_BY+")"
			+ ", INDEX ("+ENTITY_REPLICATION_COL_MODIFIED_ON+")"
			+ ")";
	public final static String ENTITY_REPLICATION_DELETE_ALL = "DELETE FROM "+ENTITY_REPLICATION_TABLE+" WHERE "+ENTITY_REPLICATION_COL_ID+" = ?";
	
	public final static String ENTITY_REPLICATION_INSERT = "INSERT INTO "+ENTITY_REPLICATION_TABLE+" ("
			+ ENTITY_REPLICATION_COL_ID+","
			+ ENTITY_REPLICATION_COL_VERSION+","
			+ ENTITY_REPLICATION_COL_CRATED_BY+","
			+ ENTITY_REPLICATION_COL_CRATED_ON+","
			+ ENTITY_REPLICATION_COL_ETAG+","
			+ ENTITY_REPLICATION_COL_NAME+","
			+ ENTITY_REPLICATION_COL_TYPE+","
			+ ENTITY_REPLICATION_COL_PARENT_ID+","
			+ ENTITY_REPLICATION_COL_BENEFACTOR_ID+","
			+ ENTITY_REPLICATION_COL_PROJECT_ID+","
			+ ENTITY_REPLICATION_COL_MODIFIED_BY+","
			+ ENTITY_REPLICATION_COL_MODIFIED_ON+","
			+ ENTITY_REPLICATION_COL_FILE_ID
			+ ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
	
	public final static String ENTITY_REPLICATION_GET = "SELECT * FROM "+TableConstants.ENTITY_REPLICATION_TABLE+" WHERE "+TableConstants.ENTITY_REPLICATION_COL_ID+" = ?";
	
	public static final String TYPE_PARAMETER_NAME = "typeParam";
	public static final String PARENT_ID_PARAMETER_NAME = "parentIds";
	public static final String ENTITY_REPLICATION_ALIAS = "R";
	public static final String ANNOTATION_REPLICATION_ALIAS = "A";
	
	//  Select the CRC32 from the entity replication for a given type and scope
	public static final String SQL_ENTITY_REPLICATION_CRC_32 = "SELECT SUM(CRC32(CONCAT("
			+ ENTITY_REPLICATION_COL_ID
			+ ", '-',"
			+ ENTITY_REPLICATION_COL_ETAG
			+ "))) FROM "
			+ ENTITY_REPLICATION_TABLE
			+ " WHERE "
			+ ENTITY_REPLICATION_COL_TYPE
			+ " = :"+TYPE_PARAMETER_NAME+" AND "
			+ ENTITY_REPLICATION_COL_PARENT_ID + " IN (:"+PARENT_ID_PARAMETER_NAME+")";
	
	// template to calculate CRC32 of a table view.
	public static final String SQL_TABLE_VIEW_CRC_32_TEMPLATE = "SELECT SUM(CRC32(CONCAT("+ROW_ID+", '-', %1$s))) FROM %2$s";
	
	// ANNOTATION_REPLICATION
	public static final String ANNOTATION_REPLICATION_TABLE 		="ANNOTATION_REPLICATION";
	public static final String ANNOTATION_REPLICATION_COL_ENTITY_ID	="ENTITY_ID";
	public static final String ANNOTATION_REPLICATION_COL_KEY		="ANNO_KEY";
	public static final String ANNOTATION_REPLICATION_COL_TYPE		="ANNO_TYPE";
	public static final String ANNOTATION_REPLICATION_COL_VALUE		="ANNO_VALUE";
	
	public static final String ANNOTATION_TYPES;
	static {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for(AnnotationType type: AnnotationType.values()){
			if(!first){
				builder.append(", ");
			}
			builder.append("'");
			builder.append(type.name());
			builder.append("'");
			first = false;
		}
		ANNOTATION_TYPES = builder.toString();
	}
	
	public static final String ANNOTATION_REPLICATION_TABLE_CREATE = 
			"CREATE TABLE IF NOT EXISTS "+ANNOTATION_REPLICATION_TABLE+"("
			+ ANNOTATION_REPLICATION_COL_ENTITY_ID+" bigint(20) NOT NULL,"
			+ ANNOTATION_REPLICATION_COL_KEY+" varchar(256) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,"
			+ ANNOTATION_REPLICATION_COL_TYPE+" ENUM("+ANNOTATION_TYPES+") NOT NULL,"
			+ ANNOTATION_REPLICATION_COL_VALUE+" varchar(500) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,"
			+ "PRIMARY KEY("+ANNOTATION_REPLICATION_COL_ENTITY_ID+","+ANNOTATION_REPLICATION_COL_KEY+","+ANNOTATION_REPLICATION_COL_TYPE+"),"
			+ "INDEX ("+ANNOTATION_REPLICATION_COL_ENTITY_ID+","+ANNOTATION_REPLICATION_COL_KEY+"),"
			+ "INDEX ("+ANNOTATION_REPLICATION_COL_VALUE+"),"
			+" CONSTRAINT `ENTITY_ID_FK` FOREIGN KEY ("+ANNOTATION_REPLICATION_COL_ENTITY_ID+") REFERENCES "+ENTITY_REPLICATION_TABLE+" ("+ENTITY_REPLICATION_COL_ID+") ON DELETE CASCADE "
			+ ")";
	
	public static final String ANNOTATION_REPLICATION_INSERT ="INSERT INTO "+ANNOTATION_REPLICATION_TABLE+"("
			+ANNOTATION_REPLICATION_COL_ENTITY_ID+","
			+ANNOTATION_REPLICATION_COL_KEY+","
			+ANNOTATION_REPLICATION_COL_TYPE+","
			+ANNOTATION_REPLICATION_COL_VALUE
			+ ") VALUES (?,?,?,?)";
	
	public final static String ANNOTATION_REPLICATION_GET = "SELECT * FROM "+TableConstants.ANNOTATION_REPLICATION_TABLE+" WHERE "+TableConstants.ANNOTATION_REPLICATION_COL_ENTITY_ID+" = ?";

	public static final String NULL_VALUE_KEYWORD = "org.sagebionetworks.UNDEFINED_NULL_NOTSET";
	
	public static final String P_OFFSET = "pOffset";

	public static final String P_LIMIT = "pLimit";
	
	public static final String SELECT_DISTINCT_ANNOTATION_COLUMNS = "SELECT A."
			+ ANNOTATION_REPLICATION_COL_KEY + ", A."
			+ ANNOTATION_REPLICATION_COL_TYPE + ", MAX(LENGTH(A."
			+ ANNOTATION_REPLICATION_COL_VALUE + "))" + " FROM "
			+ ENTITY_REPLICATION_TABLE + " AS E" + " INNER JOIN "
			+ ANNOTATION_REPLICATION_TABLE + " AS A" + " ON E."
			+ ENTITY_REPLICATION_COL_ID + " = A."
			+ ANNOTATION_REPLICATION_COL_ENTITY_ID + " WHERE E."
			+ ENTITY_REPLICATION_COL_PARENT_ID + " IN (:"
			+ PARENT_ID_PARAMETER_NAME + ") GROUP BY A."
			+ ANNOTATION_REPLICATION_COL_KEY + ", A."
			+ ANNOTATION_REPLICATION_COL_TYPE + " LIMIT :" + P_LIMIT
			+ " OFFSET :" + P_OFFSET;
}
