package org.sagebionetworks.repo.model.table;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;

public class TableConstants {

	/**
	 * The reserved column name for row id.
	 */
	public static final String ROW_ID = "ROW_ID";

	/**
	 * Column name only used in Index tables of multi-value list columns. Indicates the index number of a particular element in its list
	 */
	public static final String INDEX_NUM = "INDEX_NUM";

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
			Arrays.asList(ROW_ID, ROW_VERSION, ROW_ETAG, ROW_BENEFACTOR));

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

	// OBJECT_REPLICATION
	public static final String OBJECT_REPLICATION_TABLE 					= "OBJECT_REPLICATION";
	public static final String OBJECT_REPLICATION_COL_OBJECT_TYPE			= "OBJECT_TYPE";
	public static final String OBJECT_REPLICATION_COL_OBJECT_ID				= "OBJECT_ID";
	public static final String OBJECT_REPLICATION_COL_VERSION				= "CURRENT_VERSION";
	public static final String OBJECT_REPLICATION_COL_CREATED_BY			= "CREATED_BY";
	public static final String OBJECT_REPLICATION_COL_CREATED_ON			= "CREATED_ON";
	public static final String OBEJCT_REPLICATION_COL_ETAG					= "ETAG";
	public static final String OBJECT_REPLICATION_COL_NAME					= "NAME";
	public static final String OBJECT_REPLICATION_COL_SUBTYPE				= "SUBTYPE";
	public static final String OBJECT_REPLICATION_COL_PARENT_ID				= "PARENT_ID";
	public static final String OBJECT_REPLICATION_COL_BENEFACTOR_ID			= "BENEFACTOR_ID";
	public static final String OBJECT_REPLICATION_COL_PROJECT_ID			= "PROJECT_ID";
	public static final String OBJECT_REPLICATION_COL_MODIFIED_BY			= "MODIFIED_BY";
	public static final String OBJECT_REPLICATION_COL_MODIFIED_ON			= "MODIFIED_ON";
	public static final String OBJECT_REPLICATION_COL_FILE_ID				= "FILE_ID";
	public static final String OBJECT_REPLICATION_COL_FILE_SIZE_BYTES		= "FILE_SIZE_BYTES";
	public static final String OBJECT_REPLICATION_COL_IN_SYNAPSE_STORAGE	= "IN_SYNAPSE_STORAGE";
	public static final String OBJECT_REPLICATION_COL_FILE_MD5				= "FILE_MD5";

	// REPLICATION_SYNC_EXPIRATION
	public static final String REPLICATION_SYNC_EXPIRATION_TABLE			= "REPLICATION_SYNC_EXPIRATION";
	public static final String REPLICATION_SYNC_EXP_COL_OBJECT_TYPE			= "OBJECT_TYPE";
	public static final String REPLICATION_SYNC_EXP_COL_OBJECT_ID 			= "OBJECT_ID";
	public static final String REPLICATION_SYNC_EXP_COL_EXPIRES				= "EXPIRES_MS";
	
	public static final String ANNOTATION_KEYS_PARAM_NAME = "annotationKeys";

	public static final String SUBTYPE_PARAM_NAME = "subTypes";
	public static final String OBJECT_TYPE_PARAM_NAME = "objectType";
	public static final String PARENT_ID_PARAM_NAME = "parentIds";
	public static final String ID_PARAM_NAME = "ids";
	public static final String EXPIRES_PARAM_NAME = "bExpires";
	public static final String EXCLUSION_LIST_PARAM_NAME = "exclusionList";
	
	// Dynamic string of all the object types, used to build the enum type in the replication table
	private static final String OBJECT_TYPES_ENUM_STRING = joinEnumForSQL(ViewObjectType.values());
	
	public final static String REPLICATION_SYNCH_EXPIRATION_TABLE_CREATE = 
			"CREATE TABLE IF NOT EXISTS "+REPLICATION_SYNC_EXPIRATION_TABLE+ "("
			+ REPLICATION_SYNC_EXP_COL_OBJECT_TYPE + " ENUM(" + OBJECT_TYPES_ENUM_STRING + ") NOT NULL,"
			+ REPLICATION_SYNC_EXP_COL_OBJECT_ID +" BIGINT NOT NULL,"
			+ REPLICATION_SYNC_EXP_COL_EXPIRES +" BIGINT NOT NULL,"
			+ "PRIMARY KEY("+ REPLICATION_SYNC_EXP_COL_OBJECT_ID + "," + REPLICATION_SYNC_EXP_COL_OBJECT_TYPE + ")"
			+")";
	
	public static final String BATCH_INSERT_REPLICATION_SYNC_EXP =
			"INSERT INTO "+REPLICATION_SYNC_EXPIRATION_TABLE +" ("
			+ REPLICATION_SYNC_EXP_COL_OBJECT_TYPE + ", "
			+ REPLICATION_SYNC_EXP_COL_OBJECT_ID + ", "
			+ REPLICATION_SYNC_EXP_COL_EXPIRES
			+")"
			+ " VALUES (?,?,?) ON DUPLICATE KEY UPDATE"
			+ " "+REPLICATION_SYNC_EXP_COL_EXPIRES+" = ?";
	
	public static final String SELECT_NON_EXPIRED_IDS =
			"SELECT "
					+REPLICATION_SYNC_EXP_COL_OBJECT_ID
					+" FROM "+REPLICATION_SYNC_EXPIRATION_TABLE
					+" WHERE "
					+ REPLICATION_SYNC_EXP_COL_OBJECT_TYPE + " =:" + OBJECT_TYPE_PARAM_NAME
					+ " AND " + REPLICATION_SYNC_EXP_COL_EXPIRES + " > :"+EXPIRES_PARAM_NAME
					+ " AND " + REPLICATION_SYNC_EXP_COL_OBJECT_ID + " IN (:" + ID_PARAM_NAME+")";	
	
	public static final String TRUNCATE_REPLICATION_SYNC_EXPIRATION_TABLE = 
			"TRUNCATE TABLE "+REPLICATION_SYNC_EXPIRATION_TABLE;

	public final static String OBJECT_REPLICATION_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS "+OBJECT_REPLICATION_TABLE+"("
			+ OBJECT_REPLICATION_COL_OBJECT_TYPE + " ENUM(" + OBJECT_TYPES_ENUM_STRING+") NOT NULL,"
			+ OBJECT_REPLICATION_COL_OBJECT_ID +" BIGINT NOT NULL,"
			+ OBJECT_REPLICATION_COL_VERSION +" BIGINT NOT NULL,"
			+ OBJECT_REPLICATION_COL_CREATED_BY +" BIGINT NOT NULL,"
			+ OBJECT_REPLICATION_COL_CREATED_ON +" BIGINT NOT NULL,"
			+ OBEJCT_REPLICATION_COL_ETAG +" char(36) NOT NULL,"
			+ OBJECT_REPLICATION_COL_NAME +" varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,"
			+ OBJECT_REPLICATION_COL_SUBTYPE +" varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,"
			+ OBJECT_REPLICATION_COL_PARENT_ID +" BIGINT DEFAULT NULL,"
			+ OBJECT_REPLICATION_COL_BENEFACTOR_ID +" BIGINT NOT NULL,"
			+ OBJECT_REPLICATION_COL_PROJECT_ID +" BIGINT DEFAULT NULL,"
			+ OBJECT_REPLICATION_COL_MODIFIED_BY +" BIGINT NOT NULL,"
			+ OBJECT_REPLICATION_COL_MODIFIED_ON +" BIGINT NOT NULL,"
			+ OBJECT_REPLICATION_COL_FILE_ID +" BIGINT DEFAULT NULL,"
			+ OBJECT_REPLICATION_COL_FILE_SIZE_BYTES +" BIGINT DEFAULT NULL,"
			+ OBJECT_REPLICATION_COL_IN_SYNAPSE_STORAGE +" boolean DEFAULT NULL,"
			+ OBJECT_REPLICATION_COL_FILE_MD5 + " varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,"
			+ "PRIMARY KEY("+ OBJECT_REPLICATION_COL_OBJECT_ID + ", " + OBJECT_REPLICATION_COL_OBJECT_TYPE + ")"
			+ ", INDEX ("+OBJECT_REPLICATION_COL_VERSION+")"
			+ ", INDEX ("+OBJECT_REPLICATION_COL_CREATED_BY+")"
			+ ", INDEX ("+OBJECT_REPLICATION_COL_CREATED_ON+")"
			+ ", INDEX ("+OBEJCT_REPLICATION_COL_ETAG+")"
			+ ", INDEX ("+OBJECT_REPLICATION_COL_NAME+")"
			+ ", INDEX ("+OBJECT_REPLICATION_COL_SUBTYPE+")"
			+ ", INDEX ("+OBJECT_REPLICATION_COL_PARENT_ID+")"
			+ ", INDEX ("+OBJECT_REPLICATION_COL_BENEFACTOR_ID+")"
			+ ", INDEX ("+OBJECT_REPLICATION_COL_PROJECT_ID+")"
			+ ", INDEX ("+OBJECT_REPLICATION_COL_MODIFIED_BY+")"
			+ ", INDEX ("+OBJECT_REPLICATION_COL_MODIFIED_ON+")"
			+ ")";
	public final static String OBJECT_REPLICATION_DELETE_ALL = "DELETE FROM "+OBJECT_REPLICATION_TABLE+" WHERE " 
			+ OBJECT_REPLICATION_COL_OBJECT_TYPE + " = ? AND "
			+ OBJECT_REPLICATION_COL_OBJECT_ID+" = ?";
	
	// Note: 
	// After we upgrade to MySQL >= 8.0.19 we should switch to using the ON DUPLICATE KEY UPDATE form with alias reference
	// since it's a way simpler way to write SQL without having to repeat parameters:
	//
	// INSERT INTO TABLE (COLUMN) VALUES (?) AS data ON DUPLICATE KEY UPDATE COLUMN = data.COLUMN
	//
	// Unfortunately this form is supported only from version 8.0.19 and the previous way of referencing newly 
	// inserted values was to use the VALUES keyword:
	//
	// INSERT INTO TABLE (COLUMN) VALUES (?) AS data ON DUPLICATE KEY UPDATE VALUES(COLUMN)
	//
	// which will be deprecated in 8.0.20 therefore not worth using it now (at the time of writing we are on 8.0.16)
	//
	// See https://dev.mysql.com/doc/refman/8.0/en/insert-on-duplicate.html for reference
	public final static String OBJECT_REPLICATION_INSERT_OR_UPDATE = "INSERT INTO " + OBJECT_REPLICATION_TABLE + " ("
			+ OBJECT_REPLICATION_COL_OBJECT_TYPE 		+ ", "
			+ OBJECT_REPLICATION_COL_OBJECT_ID 			+ ","
			+ OBJECT_REPLICATION_COL_VERSION 			+ ","
			+ OBJECT_REPLICATION_COL_CREATED_BY 		+ ","
			+ OBJECT_REPLICATION_COL_CREATED_ON 		+ ","
			+ OBEJCT_REPLICATION_COL_ETAG 				+ ","
			+ OBJECT_REPLICATION_COL_NAME 				+ ","
			+ OBJECT_REPLICATION_COL_SUBTYPE 			+ ","
			+ OBJECT_REPLICATION_COL_PARENT_ID 			+ ","
			+ OBJECT_REPLICATION_COL_BENEFACTOR_ID 		+ ","
			+ OBJECT_REPLICATION_COL_PROJECT_ID 		+ ","
			+ OBJECT_REPLICATION_COL_MODIFIED_BY 		+ ","
			+ OBJECT_REPLICATION_COL_MODIFIED_ON 		+ ","
			+ OBJECT_REPLICATION_COL_FILE_ID 			+ ","
			+ OBJECT_REPLICATION_COL_FILE_SIZE_BYTES 	+ ","
			+ OBJECT_REPLICATION_COL_IN_SYNAPSE_STORAGE + ","
			+ OBJECT_REPLICATION_COL_FILE_MD5
			+ ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
			+ " ON DUPLICATE KEY UPDATE "
			+ OBJECT_REPLICATION_COL_VERSION 			+ "=?,"
			+ OBJECT_REPLICATION_COL_CREATED_BY 		+ "=?,"
			+ OBJECT_REPLICATION_COL_CREATED_ON 		+ "=?,"
			+ OBEJCT_REPLICATION_COL_ETAG 				+ "=?,"
			+ OBJECT_REPLICATION_COL_NAME 				+ "=?,"
			+ OBJECT_REPLICATION_COL_SUBTYPE 			+ "=?,"
			+ OBJECT_REPLICATION_COL_PARENT_ID 			+ "=?,"
			+ OBJECT_REPLICATION_COL_BENEFACTOR_ID 		+ "=?,"
			+ OBJECT_REPLICATION_COL_PROJECT_ID 		+ "=?,"
			+ OBJECT_REPLICATION_COL_MODIFIED_BY 		+ "=?,"
			+ OBJECT_REPLICATION_COL_MODIFIED_ON 		+ "=?,"
			+ OBJECT_REPLICATION_COL_FILE_ID 			+ "=?,"
			+ OBJECT_REPLICATION_COL_FILE_SIZE_BYTES 	+ "=?,"
			+ OBJECT_REPLICATION_COL_IN_SYNAPSE_STORAGE + "=?,"
			+ OBJECT_REPLICATION_COL_FILE_MD5 			+ "=?";
	
	public final static String OBJECT_REPLICATION_GET = "SELECT * FROM "+ OBJECT_REPLICATION_TABLE + " WHERE "
			+ OBJECT_REPLICATION_COL_OBJECT_TYPE + " = ? AND "
			+ OBJECT_REPLICATION_COL_OBJECT_ID+" = ?";
	
	public static final String TRUNCATE_OBJECT_REPLICATION_TABLE = 
			"DELETE FROM "+OBJECT_REPLICATION_TABLE;	
	
	public static final String OBJECT_REPLICATION_ALIAS = "R";
	public static final String ANNOTATION_REPLICATION_ALIAS = "A";
	
	// template to calculate CRC32 of a table view.
	public static final String SQL_TABLE_VIEW_CRC_32_TEMPLATE = 
			"SELECT"
			+ " SUM(CRC32(CONCAT("
					+ROW_ID+", '-', "+ROW_ETAG+", '-', "+ROW_BENEFACTOR+"))) FROM %1$s";
	
	// ANNOTATION_REPLICATION
	public static final String ANNOTATION_REPLICATION_TABLE 				="ANNOTATION_REPLICATION";
	public static final String ANNOTATION_REPLICATION_COL_OBJECT_TYPE		="OBJECT_TYPE";
	public static final String ANNOTATION_REPLICATION_COL_OBJECT_ID			="OBJECT_ID";
	public static final String ANNOTATION_REPLICATION_COL_KEY				="ANNO_KEY";
	public static final String ANNOTATION_REPLICATION_COL_TYPE				="ANNO_TYPE";
	public static final String ANNOTATION_REPLICATION_COL_MAX_STRING_LENGTH	="MAX_STRING_LENGTH";
	public static final String ANNOTATION_REPLICATION_COL_LIST_LENGTH       ="LIST_LENGTH";
	public static final String ANNOTATION_REPLICATION_COL_STRING_VALUE		="STRING_VALUE";
	public static final String ANNOTATION_REPLICATION_COL_LONG_VALUE		="LONG_VALUE";
	public static final String ANNOTATION_REPLICATION_COL_DOUBLE_VALUE		="DOUBLE_VALUE";
	public static final String ANNOTATION_REPLICATION_COL_DOUBLE_ABSTRACT	="DOUBLE_ABSTRACT";
	public static final String ANNOTATION_REPLICATION_COL_BOOLEAN_VALUE		="BOOLEAN_VALUE";
	public static final String ANNOTATION_REPLICATION_COL_STRING_LIST_VALUE ="STRING_LIST_VALUE";
	public static final String ANNOTATION_REPLICATION_COL_LONG_LIST_VALUE	="LONG_LIST_VALUE";
	public static final String ANNOTATION_REPLICATION_COL_BOOLEAN_LIST_VALUE="BOOLEAN_LIST_VALUE";

	public static final String ANNOTATION_TYPES = joinEnumForSQL(AnnotationType.values());
	
	public static final String ANNOTATION_REPLICATION_TABLE_CREATE = 
			"CREATE TABLE IF NOT EXISTS "+ANNOTATION_REPLICATION_TABLE+"("
			+ ANNOTATION_REPLICATION_COL_OBJECT_TYPE+" ENUM (" + OBJECT_TYPES_ENUM_STRING+ ") NOT NULL,"
			+ ANNOTATION_REPLICATION_COL_OBJECT_ID+" BIGINT NOT NULL,"
			+ ANNOTATION_REPLICATION_COL_KEY+" VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,"
			+ ANNOTATION_REPLICATION_COL_TYPE+" ENUM("+ANNOTATION_TYPES+") NOT NULL,"
			+ ANNOTATION_REPLICATION_COL_MAX_STRING_LENGTH+" BIGINT NOT NULL,"
			+ ANNOTATION_REPLICATION_COL_LIST_LENGTH+" BIGINT NOT NULL,"
			+ ANNOTATION_REPLICATION_COL_STRING_VALUE+" VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,"
			+ ANNOTATION_REPLICATION_COL_LONG_VALUE+" BIGINT DEFAULT NULL,"
			+ ANNOTATION_REPLICATION_COL_DOUBLE_VALUE+" DOUBLE DEFAULT NULL,"
			+ ANNOTATION_REPLICATION_COL_DOUBLE_ABSTRACT+" ENUM('NaN','Infinity','-Infinity') DEFAULT NULL,"
			+ ANNOTATION_REPLICATION_COL_BOOLEAN_VALUE+" BOOLEAN DEFAULT NULL,"
			+ ANNOTATION_REPLICATION_COL_STRING_LIST_VALUE + " JSON,"
			+ ANNOTATION_REPLICATION_COL_LONG_LIST_VALUE+" JSON,"
			+ ANNOTATION_REPLICATION_COL_BOOLEAN_LIST_VALUE+" JSON,"
			+ "PRIMARY KEY("+ANNOTATION_REPLICATION_COL_OBJECT_ID +","+ANNOTATION_REPLICATION_COL_OBJECT_TYPE+","+ANNOTATION_REPLICATION_COL_KEY+","+ANNOTATION_REPLICATION_COL_TYPE+"),"
			+ "INDEX `OBJECT_ID_OBJECT_TYPE_IDX` ("+ANNOTATION_REPLICATION_COL_OBJECT_ID+","+ANNOTATION_REPLICATION_COL_OBJECT_TYPE+"),"
			+ "INDEX `OBJECT_ID_OBJECT_TYPE_ANNO_KEY_IDX` ("+ ANNOTATION_REPLICATION_COL_OBJECT_ID+","+ANNOTATION_REPLICATION_COL_OBJECT_TYPE+","+ANNOTATION_REPLICATION_COL_KEY+"),"
			+ "INDEX `STRING_VALUE_IDX`("+ANNOTATION_REPLICATION_COL_STRING_VALUE+"),"
			+" CONSTRAINT `OBJECT_ID_OBJECT_TYPE_FK` FOREIGN KEY ("+ANNOTATION_REPLICATION_COL_OBJECT_ID+ "," +ANNOTATION_REPLICATION_COL_OBJECT_TYPE+") REFERENCES "
				+ OBJECT_REPLICATION_TABLE + " ("+OBJECT_REPLICATION_COL_OBJECT_ID+","+OBJECT_REPLICATION_COL_OBJECT_TYPE+") ON DELETE CASCADE "
			+ ")";
	
	public static final String ANNOTATION_REPLICATION_INSERT_OR_UPDATE ="INSERT INTO " + ANNOTATION_REPLICATION_TABLE + " ("
			+ ANNOTATION_REPLICATION_COL_OBJECT_TYPE 		+ ","
			+ ANNOTATION_REPLICATION_COL_OBJECT_ID 			+ ","
			+ ANNOTATION_REPLICATION_COL_KEY 				+ ","
			+ ANNOTATION_REPLICATION_COL_TYPE 				+ ","
			+ ANNOTATION_REPLICATION_COL_STRING_VALUE 		+ ","
			+ ANNOTATION_REPLICATION_COL_LONG_VALUE 		+ ","
			+ ANNOTATION_REPLICATION_COL_DOUBLE_VALUE 		+ ","
			+ ANNOTATION_REPLICATION_COL_DOUBLE_ABSTRACT 	+ ","
			+ ANNOTATION_REPLICATION_COL_BOOLEAN_VALUE 		+ ","
			+ ANNOTATION_REPLICATION_COL_STRING_LIST_VALUE 	+ ","
			+ ANNOTATION_REPLICATION_COL_LONG_LIST_VALUE 	+ ","
			+ ANNOTATION_REPLICATION_COL_BOOLEAN_LIST_VALUE + ","
			+ ANNOTATION_REPLICATION_COL_MAX_STRING_LENGTH 	+ ","
			+ ANNOTATION_REPLICATION_COL_LIST_LENGTH
			+ ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
			+ " ON DUPLICATE KEY UPDATE "
			+ ANNOTATION_REPLICATION_COL_STRING_VALUE 		+ "=?,"
			+ ANNOTATION_REPLICATION_COL_LONG_VALUE 		+ "=?,"
			+ ANNOTATION_REPLICATION_COL_DOUBLE_VALUE 		+ "=?,"
			+ ANNOTATION_REPLICATION_COL_DOUBLE_ABSTRACT 	+ "=?,"
			+ ANNOTATION_REPLICATION_COL_BOOLEAN_VALUE 		+ "=?,"
			+ ANNOTATION_REPLICATION_COL_STRING_LIST_VALUE 	+ "=?,"
			+ ANNOTATION_REPLICATION_COL_LONG_LIST_VALUE 	+ "=?,"
			+ ANNOTATION_REPLICATION_COL_BOOLEAN_LIST_VALUE + "=?,"
			+ ANNOTATION_REPLICATION_COL_MAX_STRING_LENGTH 	+ "=?,"
			+ ANNOTATION_REPLICATION_COL_LIST_LENGTH 		+ "=?";
	
	public final static String ANNOTATION_REPLICATION_GET = "SELECT * FROM "+ANNOTATION_REPLICATION_TABLE+" WHERE "
			+ANNOTATION_REPLICATION_COL_OBJECT_TYPE+" = ? AND "
			+ANNOTATION_REPLICATION_COL_OBJECT_ID+" = ?";
	
	public static final String TRUNCATE_ANNOTATION_REPLICATION_TABLE = 
			"TRUNCATE TABLE "+ANNOTATION_REPLICATION_TABLE;	

	public static final String NULL_VALUE_KEYWORD = "org.sagebionetworks.UNDEFINED_NULL_NOTSET";
	
	public static final String P_OFFSET = "pOffset";

	public static final String P_LIMIT = "pLimit";
	
	public static final String VIEW_ROWS_OUT_OF_DATE_TEMPLATE = 
			"WITH DELTAS (ID, MISSING) AS ( " 
			+ "SELECT R."+OBJECT_REPLICATION_COL_OBJECT_ID+", V."+ROW_ID+" FROM "+OBJECT_REPLICATION_TABLE+" R "
			+ "   LEFT JOIN %1$s V ON ("
			+ "		 R."+OBJECT_REPLICATION_COL_OBJECT_ID+" = V."+ROW_ID
			+ "      AND R."+OBEJCT_REPLICATION_COL_ETAG+" = V."+ROW_ETAG
			+ "      AND R."+OBJECT_REPLICATION_COL_BENEFACTOR_ID+" = V."+ROW_BENEFACTOR+")" 
			+ "   WHERE R."+OBJECT_REPLICATION_COL_OBJECT_TYPE+" = :"+OBJECT_TYPE_PARAM_NAME+""
			+ "      AND R.%2$s IN (:"+PARENT_ID_PARAM_NAME+")"
			+ "      AND R."+OBJECT_REPLICATION_COL_SUBTYPE+" IN (:"+SUBTYPE_PARAM_NAME+")" 
			+ " UNION ALL"
			+ " SELECT V."+ROW_ID+", R."+OBJECT_REPLICATION_COL_OBJECT_ID+" FROM "+OBJECT_REPLICATION_TABLE+" R "
			+ "   RIGHT JOIN %1$s V ON ("
			+ "      R."+OBJECT_REPLICATION_COL_OBJECT_TYPE+" = :"+OBJECT_TYPE_PARAM_NAME
			+ "      AND R."+OBJECT_REPLICATION_COL_OBJECT_ID+" = V."+ROW_ID
			+ "      AND R."+OBEJCT_REPLICATION_COL_ETAG+" = V."+ROW_ETAG
			+ "      AND R."+OBJECT_REPLICATION_COL_BENEFACTOR_ID+" = V."+ROW_BENEFACTOR
			+ "      AND R.%2$s IN (:" +PARENT_ID_PARAM_NAME+ ")"
			+ "      AND R."+OBJECT_REPLICATION_COL_SUBTYPE+" IN (:"+SUBTYPE_PARAM_NAME+"))"
			+ ")"
			+ "SELECT ID FROM DELTAS WHERE MISSING IS NULL ORDER BY ID DESC LIMIT :"+P_LIMIT;
	
	public static final String SELECT_DISTINCT_ANNOTATION_COLUMNS_TEMPLATE = "SELECT "
			+"A." + ANNOTATION_REPLICATION_COL_KEY
			+", GROUP_CONCAT(DISTINCT A." + ANNOTATION_REPLICATION_COL_TYPE + ")"
			+", MAX("+ANNOTATION_REPLICATION_COL_MAX_STRING_LENGTH+")"
			+", MAX("+ANNOTATION_REPLICATION_COL_LIST_LENGTH+")"
			+ " FROM "
			+ OBJECT_REPLICATION_TABLE + " AS E" 
			+ " INNER JOIN "
			+ ANNOTATION_REPLICATION_TABLE + " AS A" + " ON E." + OBJECT_REPLICATION_COL_OBJECT_TYPE + " = A." + ANNOTATION_REPLICATION_COL_OBJECT_TYPE
			+ " AND E." + OBJECT_REPLICATION_COL_OBJECT_ID + " = A." + ANNOTATION_REPLICATION_COL_OBJECT_ID 
			+ " WHERE E." + ANNOTATION_REPLICATION_COL_OBJECT_TYPE + "=:" + OBJECT_TYPE_PARAM_NAME
			// This can be the object id or the parent id according to the view type filter (e.g. project filters on the object id)
			+ " AND E.%1$s IN (:"+ PARENT_ID_PARAM_NAME + ")"
			+ " AND E."+OBJECT_REPLICATION_COL_SUBTYPE+" IN (:"+SUBTYPE_PARAM_NAME+")"
			// The following is replaced with a NOT IN of excluded keys if present
			// e.g. AND A.ANNOTATION_REPLICATION_COL_KEY NOT IN (:exclusionList)
			+ " %2$s"
			+ " GROUP BY A." + ANNOTATION_REPLICATION_COL_KEY 
			+ " LIMIT :" + P_LIMIT
			+ " OFFSET :" + P_OFFSET;
	
	public static final String ANNOTATION_KEY_EXCLUSION_LIST = "AND A." + ANNOTATION_REPLICATION_COL_KEY 
			+ " NOT IN (:" + EXCLUSION_LIST_PARAM_NAME + ")";
	
	public static final String CRC_ALIAS = "CRC";
	
	public static final String SELECT_OBJECT_CHILD_CRC =
			"SELECT "
					+OBJECT_REPLICATION_COL_PARENT_ID
					+", SUM(CRC32(CONCAT("
					+OBJECT_REPLICATION_COL_OBJECT_ID
					+",'-',"+OBEJCT_REPLICATION_COL_ETAG
					+",'-',"+OBJECT_REPLICATION_COL_BENEFACTOR_ID
					+ "))) AS "+CRC_ALIAS
					+" FROM "+OBJECT_REPLICATION_TABLE
					+" WHERE "+ OBJECT_REPLICATION_COL_OBJECT_TYPE + "=:" + OBJECT_TYPE_PARAM_NAME
					+" AND " + OBJECT_REPLICATION_COL_PARENT_ID+" IN (:"+PARENT_ID_PARAM_NAME+")"
					+" GROUP BY "+OBJECT_REPLICATION_COL_PARENT_ID;
	
	public static final String SELECT_OBJECT_CHILD_ID_ETAG = 
			"SELECT "
			+ OBJECT_REPLICATION_COL_OBJECT_ID
			+", " + OBEJCT_REPLICATION_COL_ETAG
			+", " + OBJECT_REPLICATION_COL_BENEFACTOR_ID
			+ " FROM " + OBJECT_REPLICATION_TABLE
			+ " WHERE " + OBJECT_REPLICATION_COL_OBJECT_TYPE + " = ?"
			+ " AND " + OBJECT_REPLICATION_COL_PARENT_ID + " = ?";
			
	/**
	 * Marker for a table's label indicating the version is 'in progress'.
	 */
	public static final String IN_PROGRESS = "in progress";
	
	/**
	 * Given the values of an enumeration produces a joined string of the {@link Enum#name()} of each item surrounded by single quote (') and joined by
	 * a comma so that it can be used to defined a MySQL enum
	 * 
	 * @param values
	 * @return
	 */
	public static String joinEnumForSQL(Enum<?>[] values) {
		return joinEnumForSQL(Stream.of(values));
	}
	
	public static String joinEnumForSQL(Stream<Enum<?>> valuesStream) {
		return joinValueForSQL(valuesStream, Enum::name);
	}
	
	public static <T> String joinValueForSQL(Stream<T> valuesStream, Function<T, String> valueMapper) {
		return valuesStream.map( e -> "'" + valueMapper.apply(e) + "'").collect(Collectors.joining(","));
	}

}
