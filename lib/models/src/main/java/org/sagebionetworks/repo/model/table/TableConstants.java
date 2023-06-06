package org.sagebionetworks.repo.model.table;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

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
	
	/**
	 * Status table columns
	 */
	public static final String STATUS_COL_SINGLE_KEY = "SINGLE_KEY";
	public static final String STATUS_COL_SCHEMA_HASH = "SCHEMA_HASH";
	public static final String STATUS_COL_SEARCH_ENABLED = "SEARCH_ENABLED";
	
	public static final String ROW_ETAG = "ROW_ETAG";
	public static final String ROW_BENEFACTOR = "ROW_BENEFACTOR";
	public static final String ROW_SEARCH_CONTENT = "ROW_SEARCH_CONTENT";
	
	/**
	 * For a given view alias, get the benefactor column name.
	 * @param viewAlias The short alias for a view.
	 * @return
	 */
	public static String getBenefactorColumnNameForViewAlias(String viewAlias) {
		return ROW_BENEFACTOR+"_"+viewAlias;
	}
	
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
	 * Note: We raised this limit from 10K to 20K for PLFM-6287.  We raised it again for PLFM-7019 from 20 K to 30K.
	 * We raised it again for PLFM-7186 from 30K to 35K.
	 */
	public static final int MAX_CONTAINERS_PER_VIEW = 1000 * 35; // 30K;

	/**
	 * The set of reserved column names includes things like ROW_ID and
	 * ROW_VERSION
	 */
	public static final Set<String> RESERVED_COLUMNS_NAMES = new HashSet<String>(
			Arrays.asList(ROW_ID, ROW_VERSION, ROW_ETAG, ROW_BENEFACTOR, ROW_SEARCH_CONTENT));

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
	 * The set of column types eligible to be added to the search index
	 */
	public static final EnumSet<ColumnType> SEARCH_TYPES = EnumSet.of(
		ColumnType.STRING,
		ColumnType.STRING_LIST, 
		ColumnType.LARGETEXT,
		ColumnType.MEDIUMTEXT,
		ColumnType.LINK,
		ColumnType.ENTITYID,
		ColumnType.ENTITYID_LIST,
		ColumnType.EVALUATIONID,
		ColumnType.SUBMISSIONID
	);
		
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
	public static final String OBJECT_REPLICATION_COL_OBJECT_VERSION		= "OBJECT_VERSION";
	public static final String OBJECT_REPLICATION_COL_CUR_VERSION			= "CURRENT_VERSION";
	public static final String OBJECT_REPLICATION_COL_CREATED_BY			= "CREATED_BY";
	public static final String OBJECT_REPLICATION_COL_CREATED_ON			= "CREATED_ON";
	public static final String OBEJCT_REPLICATION_COL_ETAG					= "ETAG";
	public static final String OBJECT_REPLICATION_COL_NAME					= "NAME";
	public static final String OBJECT_REPLICATION_COL_DESCRIPTION			= "DESCRIPTION";
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

	public static final String OBJECT_REPLICATION_COL_ITEM_COUNT			= "ITEM_COUNT";
	public static final String OBJECT_REPLICATION_COL_FILE_CONCRETE_TYPE    = "FILE_CONCRETE_TYPE";
	public static final String OBJECT_REPLICATION_COL_FILE_BUCKET			= "FILE_BUCKET";
	public static final String OBJECT_REPLICATION_COL_FILE_KEY			    = "FILE_KEY";

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
			+ OBJECT_REPLICATION_COL_OBJECT_VERSION		+ ","
			+ OBJECT_REPLICATION_COL_CUR_VERSION 		+ ","
			+ OBJECT_REPLICATION_COL_CREATED_BY 		+ ","
			+ OBJECT_REPLICATION_COL_CREATED_ON 		+ ","
			+ OBEJCT_REPLICATION_COL_ETAG 				+ ","
			+ OBJECT_REPLICATION_COL_NAME 				+ ","
			+ OBJECT_REPLICATION_COL_DESCRIPTION		+ ","
			+ OBJECT_REPLICATION_COL_SUBTYPE 			+ ","
			+ OBJECT_REPLICATION_COL_PARENT_ID 			+ ","
			+ OBJECT_REPLICATION_COL_BENEFACTOR_ID 		+ ","
			+ OBJECT_REPLICATION_COL_PROJECT_ID 		+ ","
			+ OBJECT_REPLICATION_COL_MODIFIED_BY 		+ ","
			+ OBJECT_REPLICATION_COL_MODIFIED_ON 		+ ","
			+ OBJECT_REPLICATION_COL_FILE_ID 			+ ","
			+ OBJECT_REPLICATION_COL_FILE_SIZE_BYTES 	+ ","
			+ OBJECT_REPLICATION_COL_IN_SYNAPSE_STORAGE + ","
			+ OBJECT_REPLICATION_COL_FILE_MD5			+ ","
			+ OBJECT_REPLICATION_COL_ITEM_COUNT			+ ","
			+ OBJECT_REPLICATION_COL_FILE_CONCRETE_TYPE + ","
			+ OBJECT_REPLICATION_COL_FILE_BUCKET		+ ","
			+ OBJECT_REPLICATION_COL_FILE_KEY
			+ ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
			+ " ON DUPLICATE KEY UPDATE "
			+ OBJECT_REPLICATION_COL_CUR_VERSION 		+ "=?,"
			+ OBJECT_REPLICATION_COL_CREATED_BY 		+ "=?,"
			+ OBJECT_REPLICATION_COL_CREATED_ON 		+ "=?,"
			+ OBEJCT_REPLICATION_COL_ETAG 				+ "=?,"
			+ OBJECT_REPLICATION_COL_NAME 				+ "=?,"
			+ OBJECT_REPLICATION_COL_DESCRIPTION		+ "=?,"
			+ OBJECT_REPLICATION_COL_SUBTYPE 			+ "=?,"
			+ OBJECT_REPLICATION_COL_PARENT_ID 			+ "=?,"
			+ OBJECT_REPLICATION_COL_BENEFACTOR_ID 		+ "=?,"
			+ OBJECT_REPLICATION_COL_PROJECT_ID 		+ "=?,"
			+ OBJECT_REPLICATION_COL_MODIFIED_BY 		+ "=?,"
			+ OBJECT_REPLICATION_COL_MODIFIED_ON 		+ "=?,"
			+ OBJECT_REPLICATION_COL_FILE_ID 			+ "=?,"
			+ OBJECT_REPLICATION_COL_FILE_SIZE_BYTES 	+ "=?,"
			+ OBJECT_REPLICATION_COL_IN_SYNAPSE_STORAGE + "=?,"
			+ OBJECT_REPLICATION_COL_FILE_MD5 			+ "=?,"
			+ OBJECT_REPLICATION_COL_ITEM_COUNT			+ "=?,"
			+ OBJECT_REPLICATION_COL_FILE_CONCRETE_TYPE + "=?,"
			+ OBJECT_REPLICATION_COL_FILE_BUCKET		+ "=?,"
			+ OBJECT_REPLICATION_COL_FILE_KEY			+ "=?";
	
	public static final String TRUNCATE_OBJECT_REPLICATION_TABLE = 
			"DELETE FROM "+OBJECT_REPLICATION_TABLE;	
	
	public static final String OBJECT_REPLICATION_ALIAS = "R";
	public static final String ANNOTATION_REPLICATION_ALIAS = "A";
		
	// ANNOTATION_REPLICATION
	public static final String ANNOTATION_REPLICATION_TABLE 				="ANNOTATION_REPLICATION";
	public static final String ANNOTATION_REPLICATION_COL_OBJECT_TYPE		="OBJECT_TYPE";
	public static final String ANNOTATION_REPLICATION_COL_OBJECT_ID			="OBJECT_ID";
	public static final String ANNOTATION_REPLICATION_COL_OBJECT_VERSION	="OBJECT_VERSION";
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
	public static final String ANNOTATION_REPLICATION_COL_IS_DERIVED		="IS_DERIVED";
	
	public static final String ANNOTATION_REPLICATION_INSERT_OR_UPDATE ="INSERT INTO " + ANNOTATION_REPLICATION_TABLE + " ("
			+ ANNOTATION_REPLICATION_COL_OBJECT_TYPE 		+ ","
			+ ANNOTATION_REPLICATION_COL_OBJECT_ID 			+ ","
			+ ANNOTATION_REPLICATION_COL_OBJECT_VERSION		+ ","
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
			+ ANNOTATION_REPLICATION_COL_LIST_LENGTH		+ ","
			+ ANNOTATION_REPLICATION_COL_IS_DERIVED
			+ ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
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
			+ ANNOTATION_REPLICATION_COL_LIST_LENGTH 		+ "=?,"
			+ ANNOTATION_REPLICATION_COL_IS_DERIVED 		+ "=?";
	
	public static final String TRUNCATE_ANNOTATION_REPLICATION_TABLE = 
			"TRUNCATE TABLE "+ANNOTATION_REPLICATION_TABLE;	

	public static final String NULL_VALUE_KEYWORD = "org.sagebionetworks.UNDEFINED_NULL_NOTSET";
	
	public static final String P_OFFSET = "pOffset";

	public static final String P_LIMIT = "pLimit";
	
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
	
	public static final String JOIN_NOT_SUPPORTED_IN_THIS_CONTEX_MESSAGE = "The JOIN keyword is not supported in this context";
	
	public static final String UNION_NOT_SUPPORTED_IN_THIS_CONTEX_MESSAGE = "The UNION keyword is not supported in this context";

	public static final Supplier<IllegalArgumentException> JOIN_NOT_SUPPORTED_IN_THIS_CONTEXT = () -> new IllegalArgumentException(
			JOIN_NOT_SUPPORTED_IN_THIS_CONTEX_MESSAGE);
	
	public static final String DEFINING_SQL_WITH_GROUP_BY_ERROR = "The defining SQL of a materialized view with a view dependency cannot include a group by clause";
	
	public static final String MAXIMUM_OF_ITEMS_IN_A_DATASET_EXCEEDED = "Maximum of %,d items in a dataset exceeded.";
	
	public static final String MAXIMUM_OF_ITEMS_IN_A_DATASET_COLLECTION_EXCEEDED = "Maximum of %,d items in a dataset collection exceeded.";

	public static final Long COLUMN_NO_CARDINALITY = 0L;
	
}
