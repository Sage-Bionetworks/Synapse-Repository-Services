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
	 * The reserved column name for row version.
	 */
	public static final String ROW_VERSION = "ROW_VERSION";
	public static final String SINGLE_KEY = "SINGLE_KEY";
	public static final String SCHEMA_HASH = "SCHEMA_HASH";
	
	public static final String ROW_ETAG = "ROW_ETAG";
	public static final String ROW_BENEFACTOR = "ROW_BENEFACTOR";
	
	/**
	 * FileHandle IDs 
	 */
	public static final String FILE_ID = "FILE_ID";
	
	/**
	 * The reserved column id for row id.
	 */
	public static final Long ROW_ID_ID = -1L;
	/**
	 * The reserved column name for row version.
	 */
	public static final Long ROW_VERSION_ID = -2L;
	public static final Long ROW_ETAG_ID = -3L;
	
	public static final int MAX_COLUMN_NAME_SIZE_CHARS = 256;

	/**
	 * The set of reserved column names includes things like ROW_ID and
	 * ROW_VERSION
	 */
	private static final Set<String> RESERVED_COLUMNS_NAMES = new HashSet<String>(
			Arrays.asList(ROW_ID, ROW_VERSION, ROW_ETAG));

	/**
	 * The Map of reserved column names like ROW_ID and
	 * ROW_VERSION to pseudo ids
	 */
	private static final Map<String, Long> RESERVED_COLUMNS_IDS = ImmutableMap.<String, Long> builder()
			.put(ROW_ID, ROW_ID_ID)
			.put(ROW_VERSION, ROW_VERSION_ID)
			.put(ROW_ETAG, ROW_ETAG_ID)
			.build();
	
	/**
	 * The column name prefix for extra doubles column.
	 */
	public static final String DOUBLE_PREFIX = "_DBL";

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
	
	// REPLICATION_SYNC_EXPIRATION
	public static final String REPLICATION_SYNC_EXPIRATION_TABLE	= "REPLICATION_SYNC_EXPIRATION";
	public static final String REPLICATION_SYNC_EXP_COL_ID 			= "ID";
	public static final String REPLICATION_SYNC_EXP_COL_EXPIRES		= "EXPIRES_MS";
	
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
	
	public final static String REPLICATION_SYNCH_EXPIRATION_TABLE_CREATE = 
			"CREATE TABLE IF NOT EXISTS "+REPLICATION_SYNC_EXPIRATION_TABLE+ "("
			+ REPLICATION_SYNC_EXP_COL_ID +" bigint(20) NOT NULL,"
			+ REPLICATION_SYNC_EXP_COL_EXPIRES +" bigint(20) NOT NULL,"
			+ "PRIMARY KEY("+REPLICATION_SYNC_EXP_COL_ID+")"
			+")";

	public final static String ENTITY_REPLICATION_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS "+ENTITY_REPLICATION_TABLE+"("
			+ ENTITY_REPLICATION_COL_ID +" bigint(20) NOT NULL,"
			+ ENTITY_REPLICATION_COL_VERSION +" bigint(20) NOT NULL,"
			+ ENTITY_REPLICATION_COL_CRATED_BY +" bigint(20) NOT NULL,"
			+ ENTITY_REPLICATION_COL_CRATED_ON +" bigint(20) NOT NULL,"
			+ ENTITY_REPLICATION_COL_ETAG +" char(36) NOT NULL,"
			+ ENTITY_REPLICATION_COL_NAME +" varchar(256) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,"
			+ ENTITY_REPLICATION_COL_TYPE +" ENUM("+ENTITY_TYPES+") NOT NULL,"
			+ ENTITY_REPLICATION_COL_PARENT_ID +" bigint(20) DEFAULT NULL,"
			+ ENTITY_REPLICATION_COL_BENEFACTOR_ID +" bigint(20) NOT NULL,"
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
	public static final String ID_PARAMETER_NAME = "ids";
	public static final String EXPIRES_PARAM = "bExpires";
	public static final String ENTITY_REPLICATION_ALIAS = "R";
	public static final String ANNOTATION_REPLICATION_ALIAS = "A";
	
	//  Select the CRC32 from the entity replication for a given type and scope
	public static final String SQL_ENTITY_REPLICATION_CRC_32_TEMPLATE = 
			"SELECT"
			+ " SUM(CRC32(CONCAT("
					+ ENTITY_REPLICATION_COL_ID+ ", '-',"+ ENTITY_REPLICATION_COL_ETAG+", '-', "+ENTITY_REPLICATION_COL_BENEFACTOR_ID
			+ ")))"
			+ " FROM "+ ENTITY_REPLICATION_TABLE
			+ " WHERE %1$s AND %2$s IN (:"+PARENT_ID_PARAMETER_NAME+")";
	
	// template to calculate CRC32 of a table view.
	public static final String SQL_TABLE_VIEW_CRC_32_TEMPLATE = 
			"SELECT"
			+ " SUM(CRC32(CONCAT("
					+ROW_ID+", '-', "+ROW_ETAG+", '-', "+ROW_BENEFACTOR+"))) FROM %1$s";
	
	// ANNOTATION_REPLICATION
	public static final String ANNOTATION_REPLICATION_TABLE 				="ANNOTATION_REPLICATION";
	public static final String ANNOTATION_REPLICATION_COL_ENTITY_ID			="ENTITY_ID";
	public static final String ANNOTATION_REPLICATION_COL_KEY				="ANNO_KEY";
	public static final String ANNOTATION_REPLICATION_COL_TYPE				="ANNO_TYPE";
	public static final String ANNOTATION_REPLICATION_COL_STRING_VALUE		="STRING_VALUE";
	public static final String ANNOTATION_REPLICATION_COL_LONG_VALUE		="LONG_VALUE";
	public static final String ANNOTATION_REPLICATION_COL_DOUBLE_VALUE		="DOUBLE_VALUE";
	public static final String ANNOTATION_REPLICATION_COL_DOUBLE_ABSTRACT	="DOUBLE_ABSTRACT";
	public static final String ANNOTATION_REPLICATION_COL_BOOLEAN_VALUE		="BOOLEAN_VALUE";
	
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
			+ ANNOTATION_REPLICATION_COL_KEY+" VARCHAR(256) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,"
			+ ANNOTATION_REPLICATION_COL_TYPE+" ENUM("+ANNOTATION_TYPES+") NOT NULL,"
			+ ANNOTATION_REPLICATION_COL_STRING_VALUE+" VARCHAR(500) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,"
			+ ANNOTATION_REPLICATION_COL_LONG_VALUE+" BIGINT DEFAULT NULL,"
			+ ANNOTATION_REPLICATION_COL_DOUBLE_VALUE+" DOUBLE DEFAULT NULL,"
			+ ANNOTATION_REPLICATION_COL_DOUBLE_ABSTRACT+" ENUM('NaN','Infinity','-Infinity') DEFAULT NULL,"
			+ ANNOTATION_REPLICATION_COL_BOOLEAN_VALUE+" BOOLEAN DEFAULT NULL,"
			+ "PRIMARY KEY("+ANNOTATION_REPLICATION_COL_ENTITY_ID+","+ANNOTATION_REPLICATION_COL_KEY+","+ANNOTATION_REPLICATION_COL_TYPE+"),"
			+ "INDEX `ENTITY_ID_IDX` ("+ANNOTATION_REPLICATION_COL_ENTITY_ID+"),"
			+ "INDEX `ENTITY_ID_ANNO_KEY_IDX` ("+ANNOTATION_REPLICATION_COL_ENTITY_ID+","+ANNOTATION_REPLICATION_COL_KEY+"),"
			+ "INDEX `STRING_VALUE_IDX`("+ANNOTATION_REPLICATION_COL_STRING_VALUE+"),"
			+" CONSTRAINT `ENTITY_ID_FK` FOREIGN KEY ("+ANNOTATION_REPLICATION_COL_ENTITY_ID+") REFERENCES "+ENTITY_REPLICATION_TABLE+" ("+ENTITY_REPLICATION_COL_ID+") ON DELETE CASCADE "
			+ ")";
	
	public static final String ANNOTATION_REPLICATION_INSERT ="INSERT INTO "+ANNOTATION_REPLICATION_TABLE+"("
			+ANNOTATION_REPLICATION_COL_ENTITY_ID+","
			+ANNOTATION_REPLICATION_COL_KEY+","
			+ANNOTATION_REPLICATION_COL_TYPE+","
			+ANNOTATION_REPLICATION_COL_STRING_VALUE+","
			+ANNOTATION_REPLICATION_COL_LONG_VALUE+","
			+ANNOTATION_REPLICATION_COL_DOUBLE_VALUE+","
			+ANNOTATION_REPLICATION_COL_DOUBLE_ABSTRACT+","
			+ANNOTATION_REPLICATION_COL_BOOLEAN_VALUE
			+ ") VALUES (?,?,?,?,?,?,?,?)";
	
	public final static String ANNOTATION_REPLICATION_GET = "SELECT * FROM "+TableConstants.ANNOTATION_REPLICATION_TABLE+" WHERE "+TableConstants.ANNOTATION_REPLICATION_COL_ENTITY_ID+" = ?";

	public static final String NULL_VALUE_KEYWORD = "org.sagebionetworks.UNDEFINED_NULL_NOTSET";
	
	public static final String P_OFFSET = "pOffset";

	public static final String P_LIMIT = "pLimit";
	
	public static final String SELECT_DISTINCT_ANNOTATION_COLUMNS_TEMPLATE = "SELECT A."
			+ ANNOTATION_REPLICATION_COL_KEY + ", GROUP_CONCAT(DISTINCT A."
			+ ANNOTATION_REPLICATION_COL_TYPE + "), MAX(LENGTH(A."
			+ ANNOTATION_REPLICATION_COL_STRING_VALUE + "))" + " FROM "
			+ ENTITY_REPLICATION_TABLE + " AS E" + " INNER JOIN "
			+ ANNOTATION_REPLICATION_TABLE + " AS A" + " ON E."
			+ ENTITY_REPLICATION_COL_ID + " = A."
			+ ANNOTATION_REPLICATION_COL_ENTITY_ID + " WHERE E."
			+ "%1$s IN (:"
			+ PARENT_ID_PARAMETER_NAME + ") GROUP BY A."
			+ ANNOTATION_REPLICATION_COL_KEY + " LIMIT :" + P_LIMIT
			+ " OFFSET :" + P_OFFSET;
	
	public static final String CRC_ALIAS = "CRC";
	
	public static final String SELECT_ENTITY_CHILD_CRC =
			"SELECT "
					+ENTITY_REPLICATION_COL_PARENT_ID
					+", SUM(CRC32(CONCAT("+ENTITY_REPLICATION_COL_ID+",'-',"+ENTITY_REPLICATION_COL_ETAG+"))) AS "+CRC_ALIAS
					+" FROM "+ENTITY_REPLICATION_TABLE
					+" WHERE "+ENTITY_REPLICATION_COL_PARENT_ID+" IN (:"+PARENT_ID_PARAMETER_NAME+")"
					+" GROUP BY "+ENTITY_REPLICATION_COL_PARENT_ID;
	
	public static final String SELECT_ENTITY_CHILD_ID_ETAG = 
			"SELECT "
			+ ENTITY_REPLICATION_COL_ID+", "+ENTITY_REPLICATION_COL_ETAG
			+ " FROM "+ENTITY_REPLICATION_TABLE
			+ " WHERE "+ENTITY_REPLICATION_COL_PARENT_ID+" = ?";
	
	public static final String BATCH_INSERT_REPLICATION_SYNC_EXP =
			"INSERT INTO "+REPLICATION_SYNC_EXPIRATION_TABLE
			+" ("+REPLICATION_SYNC_EXP_COL_ID+", "+REPLICATION_SYNC_EXP_COL_EXPIRES+")"
			+" VALUES (?,?) ON DUPLICATE KEY UPDATE"
			+" "+REPLICATION_SYNC_EXP_COL_EXPIRES+" = ?";
	
	public static final String TRUNCATE_REPLICATION_SYNC_EXPIRATION_TABLE = 
			"TRUNCATE TABLE "+REPLICATION_SYNC_EXPIRATION_TABLE;
			
	
	public static final String SELECT_NON_EXPIRED_IDS =
			"SELECT "
					+REPLICATION_SYNC_EXP_COL_ID
					+" FROM "+REPLICATION_SYNC_EXPIRATION_TABLE
					+" WHERE "
					+REPLICATION_SYNC_EXP_COL_EXPIRES+" > :"+EXPIRES_PARAM
					+" AND "+REPLICATION_SYNC_EXP_COL_ID+" IN (:"+ID_PARAMETER_NAME+")";
	
}
