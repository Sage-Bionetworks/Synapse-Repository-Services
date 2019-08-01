package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURRENT_REV;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_BUCKET_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CONTENT_MD5;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CONTENT_SIZE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_LAST_ACCESSED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_ACTIVITY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_ANNOS_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_COLUMN_MODEL_IDS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_COMMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_ENTITY_PROPERTY_ANNOTATIONS_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_LABEL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_OWNER_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_REF_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_SCOPE_IDS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_USER_ANNOTATIONS_V1_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.CONSTRAINT_UNIQUE_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.CONSTRAINT_UNIQUE_CHILD_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.FUNCTION_GET_ENTITY_BENEFACTOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.FUNCTION_GET_ENTITY_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PROJECT_STAT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REVISION;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.NotImplementedException;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeIdAndType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.dbo.persistence.NodeMapper;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.file.ChildStatsRequest;
import org.sagebionetworks.repo.model.file.ChildStatsResponse;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.jdo.AnnotationUtils;
import org.sagebionetworks.repo.model.jdo.JDORevisionUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageToSend;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.query.jdo.QueryUtils;
import org.sagebionetworks.repo.model.table.EntityDTO;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.SerializationUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * This is a basic implementation of the NodeDAO.
 * 
 * @author jmhill
 *
 */
public class NodeDAOImpl implements NodeDAO, InitializingBean {

	private static final String SQL_CREATE_SNAPSHOT_VERSION = "UPDATE " + TABLE_REVISION + " SET "
			+ COL_REVISION_COMMENT + " = ?, " + COL_REVISION_LABEL + " = ?, " + COL_REVISION_ACTIVITY_ID + " = ?, "
			+ COL_REVISION_MODIFIED_BY + " = ?, " + COL_REVISION_MODIFIED_ON + " = ? WHERE " + COL_REVISION_OWNER_NODE
			+ " = ? AND " + COL_REVISION_NUMBER + " = ?";

	private static final String UPDATE_REVISION = "UPDATE " + TABLE_REVISION + " SET " + COL_REVISION_ACTIVITY_ID
			+ " = ?, " + COL_REVISION_COMMENT + " = ?, " + COL_REVISION_LABEL + " = ?, " + COL_REVISION_FILE_HANDLE_ID
			+ " = ?, " + COL_REVISION_COLUMN_MODEL_IDS + " = ?, " + COL_REVISION_SCOPE_IDS + " = ?, "
			+ COL_REVISION_REF_BLOB + " = ? WHERE " + COL_REVISION_OWNER_NODE + " = ? AND " + COL_REVISION_NUMBER + " = ?";
	
	private static final String UPDATE_NODE = "UPDATE " + TABLE_NODE + " SET " + COL_NODE_NAME + " = ?, "
			+ COL_NODE_PARENT_ID + " = ?, " + COL_NODE_ALIAS + " = ? WHERE " + COL_NODE_ID + " = ?";
	
	private static final String SQL_UPDATE_ANNOTATIONS_FORMAT = "UPDATE " + TABLE_REVISION + " SET %s"
			+ " = ? WHERE " + COL_REVISION_OWNER_NODE + " = ? AND " + COL_REVISION_NUMBER + " = ?";
	private static final String SQL_TOUCH_REVISION = "UPDATE " + TABLE_REVISION + " SET " + COL_REVISION_MODIFIED_BY
			+ " = ?, " + COL_REVISION_MODIFIED_ON + " = ? WHERE " + COL_REVISION_OWNER_NODE + " = ? AND "
			+ COL_REVISION_NUMBER + " = ?";
	private static final String SQL_TOUCH_ETAG = "UPDATE "+TABLE_NODE+" SET "+COL_NODE_ETAG+" = ? WHERE "+COL_NODE_ID+" = ?";
	private static final String MAXIMUM_NUMBER_OF_IDS_EXCEEDED = "Maximum number of IDs exceeded";
	private static final String SQL_SELECT_GET_ENTITY_BENEFACTOR_ID = "SELECT "+FUNCTION_GET_ENTITY_BENEFACTOR_ID+"(?)";
	private static final String BIND_NODE_IDS =  "bNodeIds";
	private static final String BIND_PROJECT_STAT_USER_ID = "bUserIds";
	private static final String BIND_PARENT_ID = "bParentId";
	private static final String BIND_NODE_TYPES = "bNodeTypes";
	private static final String BIND_LIMIT = "bLimit";
	private static final String BIND_OFFSET = "bOffset";
	
	private static final String SQL_SELECT_CHILD_CRC32 = 
			"SELECT "+COL_NODE_PARENT_ID+","
					+ " SUM(CRC32(CONCAT("+COL_NODE_ID
					+",'-',"+COL_NODE_ETAG
					+",'-',"+FUNCTION_GET_ENTITY_BENEFACTOR_ID+"("+COL_NODE_ID+")"
							+ "))) AS 'CRC'"
							+ " FROM "+TABLE_NODE+" WHERE "+COL_NODE_PARENT_ID+" IN(:"+BIND_PARENT_ID+")"
									+ " GROUP BY "+COL_NODE_PARENT_ID;
	
	private static final String SQL_SELECT_CHILDREN_ID_AND_ETAG = 
			"SELECT "+COL_NODE_ID
			+", "+COL_NODE_ETAG
			+", "+FUNCTION_GET_ENTITY_BENEFACTOR_ID+"("+COL_NODE_ID+")"
			+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_PARENT_ID+" = ?";

	private static final String SQL_SELECT_CHILD = "SELECT "+COL_NODE_ID
			+ " FROM "+TABLE_NODE
			+ " WHERE "+COL_NODE_PARENT_ID+" = :"+COL_NODE_PARENT_ID
			+ " AND "+COL_NODE_NAME+" = :"+COL_NODE_NAME;

	private static final String SQL_SELECT_CHILDREN = 
			"SELECT"
			+ " "+COL_NODE_ID
			+", "+COL_NODE_TYPE
			+" FROM "+TABLE_NODE
			+" WHERE "+COL_NODE_PARENT_ID+" = ?"
					+ " LIMIT ? OFFSET ?";
	
	private static final String SQL_COUNT_CHILDREN = 
			"SELECT COUNT("+COL_NODE_ID+")"
			+ " FROM "+TABLE_NODE+""
					+ " WHERE "+COL_NODE_PARENT_ID+" = ?";
	
	private static final String SELECT_PROJECTS_STATS = "SELECT n."
			+ COL_NODE_ID
			+ ", n."
			+ COL_NODE_NAME
			+ ", n."
			+ COL_NODE_TYPE
			+ ", COALESCE(ps."
			+ COL_PROJECT_STAT_LAST_ACCESSED
			+ ", n."
			+ COL_NODE_CREATED_ON
			+ ") AS "
			+ COL_PROJECT_STAT_LAST_ACCESSED
			+ ", r."
			+ COL_REVISION_MODIFIED_BY
			+ ", r."
			+ COL_REVISION_MODIFIED_ON
			+ " FROM "
			+ TABLE_NODE
			+ " n JOIN "
			+ TABLE_REVISION
			+ " r ON n."
			+ COL_NODE_ID
			+ " = r."
			+ COL_REVISION_OWNER_NODE
			+ " AND n."
			+ COL_CURRENT_REV
			+ " = r."
			+ COL_REVISION_NUMBER
			+ " LEFT JOIN "
			+ TABLE_PROJECT_STAT
			+ " ps ON n."
			+ COL_NODE_ID
			+ " = ps."
			+ COL_PROJECT_STAT_PROJECT_ID
			+ " AND ps."
			+ COL_PROJECT_STAT_USER_ID
			+ " = :"
			+ BIND_PROJECT_STAT_USER_ID
			+ " WHERE n."
			+ COL_NODE_TYPE
			+ " = '"
			+ EntityType.project.name()
			+ "' AND n."
			+ COL_NODE_ID
			+ " IN (:"
			+ BIND_NODE_IDS
			+ ")";

	private static final String BENEFACTOR_ALIAS = "BENEFACTOR";
	private static final String SQL_SELECT_BENEFACTOR_N = FUNCTION_GET_ENTITY_BENEFACTOR_ID+"(N."+COL_NODE_ID+") AS "+BENEFACTOR_ALIAS;
	
	private static final String SQL_SELECT_BENEFACTORS =
			"SELECT N."+COL_NODE_ID+", "+SQL_SELECT_BENEFACTOR_N
			+ " FROM "+TABLE_NODE+" N"
			+ " WHERE N."+COL_NODE_ID+" IN (:"+BIND_NODE_IDS+")";
	
	private static final String ENTITY_HEADER_SELECT = "SELECT N." + COL_NODE_ID + ", R." + COL_REVISION_LABEL + ", N."
			+ COL_NODE_NAME + ", N." + COL_NODE_TYPE + ", " + SQL_SELECT_BENEFACTOR_N + ", R." + COL_REVISION_NUMBER
			+ ", N." + COL_NODE_CREATED_BY + ", N." + COL_NODE_CREATED_ON + ", R." + COL_REVISION_MODIFIED_BY + ", R."
			+ COL_REVISION_MODIFIED_ON;
	
	private static final String JOIN_NODE_REVISION = TABLE_NODE+" N"+
			" JOIN "+TABLE_REVISION+" R"+
			" ON (N."+COL_NODE_ID+" = R."+COL_REVISION_OWNER_NODE+" AND N."+COL_CURRENT_REV+" = R."+COL_REVISION_NUMBER+")";
	private static final String JOIN_NODE_REVISION_FILES = JOIN_NODE_REVISION+ " LEFT JOIN "
			+ TABLE_FILES + " F ON (R." + COL_REVISION_FILE_HANDLE_ID + " = F." + COL_FILES_ID + " )";
	
	private static final String SQL_SELECT_CHIDREN_TEMPLATE =
			ENTITY_HEADER_SELECT+
				" FROM "+JOIN_NODE_REVISION+
				" WHERE N."+COL_NODE_PARENT_ID+" = :"+BIND_PARENT_ID+
						" %1$s"+
						" AND N."+COL_NODE_TYPE+" IN (:"+BIND_NODE_TYPES+")"+
						" ORDER BY %2$s %3$s"+
						" LIMIT :"+BIND_LIMIT+" OFFSET :"+BIND_OFFSET;
	
	private static final String SQL_SELECT_CHIDREN_STATS =
			"SELECT COUNT(*), SUM(F."+COL_FILES_CONTENT_SIZE+")"+
				" FROM "+TABLE_NODE+" N"+
				" JOIN "+TABLE_REVISION+" R"+
					" ON (N."+COL_NODE_ID+" = R."+COL_REVISION_OWNER_NODE+" AND N."+COL_CURRENT_REV+" = R."+COL_REVISION_NUMBER+")"+
				" LEFT JOIN "+TABLE_FILES+" F"+
					" ON (R."+COL_REVISION_FILE_HANDLE_ID+" = F."+COL_FILES_ID+")"+
				" WHERE N."+COL_NODE_PARENT_ID+" = :"+BIND_PARENT_ID+
						" %1$s"+
						" AND N."+COL_NODE_TYPE+" IN (:"+BIND_NODE_TYPES+")";
	
	public static final String N_NAME = "N."+COL_NODE_NAME;
	public static final String N_CREATED_ON = "N."+COL_NODE_CREATED_ON;
	
	public static final String SQL_ID_NOT_IN_SET = " AND N."+COL_NODE_ID+" NOT IN (:"+BIND_NODE_IDS+")";
	
	private static final String SQL_SELECT_WITHOUT_ANNOTATIONS = "SELECT N.*, R."+COL_REVISION_OWNER_NODE+", R."+COL_REVISION_NUMBER+", R."+COL_REVISION_ACTIVITY_ID+", R."+COL_REVISION_LABEL+", R."+COL_REVISION_COMMENT+", R."+COL_REVISION_MODIFIED_BY+", R."+COL_REVISION_MODIFIED_ON+", R."+COL_REVISION_FILE_HANDLE_ID+", R."+COL_REVISION_COLUMN_MODEL_IDS+", R."+COL_REVISION_SCOPE_IDS+", R."+COL_REVISION_REF_BLOB;
	private static final String SQL_SELECT_CURRENT_NODE = SQL_SELECT_WITHOUT_ANNOTATIONS+" FROM "+TABLE_NODE+" N, "+TABLE_REVISION+" R WHERE N."+COL_NODE_ID+"= R."+COL_REVISION_OWNER_NODE+" AND N."+COL_CURRENT_REV+" = R."+COL_REVISION_NUMBER+" AND N."+COL_NODE_ID+"= ?";
	private static final String SQL_SELECT_NODE_VERSION = SQL_SELECT_WITHOUT_ANNOTATIONS+" FROM "+TABLE_NODE+" N, "+TABLE_REVISION+" R WHERE N."+COL_NODE_ID+"= R."+COL_REVISION_OWNER_NODE+" AND R."+COL_REVISION_NUMBER+" = ? AND N."+COL_NODE_ID+"= ?";

	private static final String SELECT_FUNCTION_PROJECT_ID = "SELECT "+FUNCTION_GET_ENTITY_PROJECT_ID+"(?)";
	private static final String SQL_SELECT_NODE_ID_BY_ALIAS = "SELECT "+COL_NODE_ID+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_ALIAS+" = ?";
	
	private static final String SELECT_ENTITY_HEADERS_FOR_ENTITY_IDS =
			ENTITY_HEADER_SELECT +
			" FROM "+TABLE_NODE +" N" +
			" JOIN "+TABLE_REVISION+" R"+
			" ON (N."+COL_NODE_ID+" = R."+COL_REVISION_OWNER_NODE+" AND N."+COL_CURRENT_REV+" = R."+COL_REVISION_NUMBER+")"+
			" WHERE "+COL_NODE_ID+" IN (:nodeIds)";
	
	private static final String IDS_PARAM_NAME = "ids_param";

	private static final String SQL_SELECT_CONTAINERS_WITH_PARENT_IDS_IN_CLAUSE = 
			"SELECT "+COL_NODE_ID
			+" FROM "+TABLE_NODE
			+" WHERE "
				+ COL_NODE_PARENT_ID+" IN (:"+IDS_PARAM_NAME+")"
				+ " AND "+COL_NODE_TYPE+" IN ('"+EntityType.folder.name()+"', '"+EntityType.project.name()+"')"
						+ " ORDER BY "+COL_NODE_ID+" ASC LIMIT :"+BIND_LIMIT;

	private static final String SQL_SELECT_REV_FILE_HANDLE_ID = "SELECT "+COL_REVISION_FILE_HANDLE_ID+" FROM "+TABLE_REVISION+" WHERE "+COL_REVISION_OWNER_NODE+" = ? AND "+COL_REVISION_NUMBER+" = ?";
	private static final String SELECT_ANNOTATIONS_ONLY_SELECT_CLAUSE_PREFIX = "SELECT N."+COL_NODE_ID+", N."+COL_NODE_ETAG+", N."+COL_NODE_CREATED_ON+", N."+COL_NODE_CREATED_BY+", R.";

	private static final String SELECT_ANNOTATIONS_ONLY_FROM_AND_WHERE_CLAUSE_PREFIX = " FROM  "+TABLE_NODE+" N, "+TABLE_REVISION+" R WHERE N."+COL_NODE_ID+" = ? AND R."+COL_REVISION_OWNER_NODE+" = N."+COL_NODE_ID+" AND R."+COL_REVISION_NUMBER + "=";
	private static final String CANNOT_FIND_A_NODE_WITH_ID = "Cannot find a node with id: ";
	private static final String ERROR_RESOURCE_NOT_FOUND = "The resource you are attempting to access cannot be found";
	private static final String GET_CURRENT_REV_NUMBER_SQL = "SELECT "+COL_CURRENT_REV+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_ID+" = ?";
	private static final String GET_NODE_TYPE_SQL = "SELECT "+COL_NODE_TYPE+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_ID+" = ?";
	private static final String GET_REV_ACTIVITY_ID_SQL = "SELECT "+COL_REVISION_ACTIVITY_ID+" FROM "+TABLE_REVISION+" WHERE "+COL_REVISION_OWNER_NODE+" = ? AND "+ COL_REVISION_NUMBER +" = ?";
	private static final String GET_NODE_CREATED_BY_SQL = "SELECT "+COL_NODE_CREATED_BY+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_ID+" = ?";
	private static final String SQL_SELECT_PARENT_TYPE_NAME = "SELECT "+COL_NODE_ID+", "+COL_NODE_PARENT_ID+", "+COL_NODE_TYPE+", "+COL_NODE_NAME+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_ID+" = ?";
	private static final String SQL_GET_ALL_CHILDREN_IDS = "SELECT "+COL_NODE_ID+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_PARENT_ID+" = ? ORDER BY "+COL_NODE_ID;
	private static final String NODE_IDS_LIST_PARAM_NAME = "NODE_IDS";
	
	private static final String SELECT_CURRENT_VERSION_FILE_HANDLES = "SELECT N." + COL_NODE_ID + ", R."
			+ COL_REVISION_FILE_HANDLE_ID + " FROM " + TABLE_NODE + " N JOIN " + TABLE_REVISION + " R ON (N."
			+ COL_NODE_ID + " = R." + COL_REVISION_OWNER_NODE + " AND N." + COL_CURRENT_REV + " = R."
			+ COL_REVISION_NUMBER + ") WHERE N." + COL_NODE_TYPE + " = '" + EntityType.file + "' AND N." + COL_NODE_ID
			+ " IN (:" + NODE_IDS_LIST_PARAM_NAME + ")";
	
	public static final String BENEFACTOR_FUNCTION_ALIAS = FUNCTION_GET_ENTITY_BENEFACTOR_ID+"(N."+COL_NODE_ID+")";
	public static final String PROJECT_FUNCTION_ALIAS = FUNCTION_GET_ENTITY_PROJECT_ID+"(N."+COL_NODE_ID+")";
	
	private static final String SQL_SELECT_ENTITY_DTO = "SELECT N." + COL_NODE_ID + ", N." + COL_CURRENT_REV + ", N."
			+ COL_NODE_CREATED_BY + ", N." + COL_NODE_CREATED_ON + ", N." + COL_NODE_ETAG + ", N." + COL_NODE_NAME
			+ ", N." + COL_NODE_TYPE + ", N." + COL_NODE_PARENT_ID + ", " + BENEFACTOR_FUNCTION_ALIAS + ", "
			+ PROJECT_FUNCTION_ALIAS + ", R." + COL_REVISION_MODIFIED_BY + ", R." + COL_REVISION_MODIFIED_ON + ", R."
			+ COL_REVISION_FILE_HANDLE_ID + ", R." + COL_REVISION_USER_ANNOTATIONS_V1_BLOB
			+ ", F." + COL_FILES_CONTENT_SIZE +
			", F." + COL_FILES_BUCKET_NAME
			+ " FROM " + JOIN_NODE_REVISION_FILES+" WHERE N."
			+ COL_NODE_ID + " IN(:" + NODE_IDS_LIST_PARAM_NAME + ") ORDER BY N."+COL_NODE_ID+" ASC";
	
	private static final String SQL_GET_CURRENT_VERSIONS = "SELECT "+COL_NODE_ID+","+COL_CURRENT_REV+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_ID+" IN ( :"+NODE_IDS_LIST_PARAM_NAME + " )";
	private static final String OWNER_ID_PARAM_NAME = "OWNER_ID";

	private static final String LAST_ACCESSED_OR_CREATED =
		"coalesce(ps." + COL_PROJECT_STAT_LAST_ACCESSED + ", n." + COL_NODE_CREATED_ON + ")";

	private static final String BIND_CREATED_BY = "bCreatedBy";
	private static final String SELECT_CREATED =
		" AND n." + COL_NODE_CREATED_BY + " = :"+BIND_CREATED_BY;
	private static final String SELECT_NOT_CREATED =
		" AND n." + COL_NODE_CREATED_BY + " <> :"+BIND_CREATED_BY;
	
	private static final String SELECT_PROJECTS_ORDER =
		" ORDER BY " + LAST_ACCESSED_OR_CREATED;

	private static final String SELECT_NAME_ORDER =
		" ORDER BY n." + COL_NODE_NAME ;

	/**
	 * To determine if a node has children we fetch the first child ID.
	 */
	private static final String SQL_GET_FIRST_CHILD = "SELECT "+COL_NODE_ID+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_PARENT_ID+" = ? LIMIT 1 OFFSET 0";

	private static final String SQL_GET_ALL_VERSION_INFO_PAGINATED = "SELECT rr."
			+ COL_REVISION_NUMBER + ", rr." + COL_REVISION_LABEL + ", rr."
			+ COL_REVISION_COMMENT + ", rr." + COL_REVISION_MODIFIED_BY + ", rr."
			+ COL_REVISION_MODIFIED_ON 
			+ ", ff." + COL_FILES_CONTENT_MD5 + ", ff." + COL_FILES_CONTENT_SIZE + " FROM " + TABLE_REVISION + " rr left outer join "
			+ TABLE_FILES+" ff on (rr."+COL_REVISION_FILE_HANDLE_ID+" = ff."+COL_FILES_ID+") WHERE rr."
			+ COL_REVISION_OWNER_NODE + " = :"+OWNER_ID_PARAM_NAME+" ORDER BY rr." + COL_REVISION_NUMBER
			+ " DESC LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;

	/**
	 * The max number of entity versions a MD5 string can map to. This puts a check
	 * to potential DDOS attacks via MD5. We retrieve at most MD5_LIMIT + 1 rows.
	 * If the number of rows retrieved is > MD5_LIMIT, an exception is thrown.
	 */
	private static final int NODE_VERSION_LIMIT_BY_FILE_MD5 = 200;
	private static final String SELECT_NODE_VERSION_BY_FILE_MD5 =
			ENTITY_HEADER_SELECT
			+ " FROM " + TABLE_REVISION + " R, " + TABLE_FILES + " F, "+TABLE_NODE+" N"
			+ " WHERE R."+COL_REVISION_OWNER_NODE+" = N."+COL_NODE_ID+" AND  R." + COL_REVISION_FILE_HANDLE_ID + " = F." + COL_FILES_ID
			+ " AND F." + COL_FILES_CONTENT_MD5 + " = :" + COL_FILES_CONTENT_MD5
			+ " LIMIT " + (NODE_VERSION_LIMIT_BY_FILE_MD5 + 1);

	// Track the trash folder.
	public static final Long TRASH_FOLDER_ID = Long.parseLong(StackConfigurationSingleton.singleton().getTrashFolderEntityId());

	private static final RowMapper<EntityHeader> ENTITY_HEADER_ROWMAPPER = new RowMapper<EntityHeader>() {
		@Override
		public EntityHeader mapRow(ResultSet rs, int rowNum) throws SQLException {
			EntityHeader header = new EntityHeader();
			Long entityId = rs.getLong(COL_NODE_ID);
			header.setId(KeyFactory.keyToString(entityId));
			EntityType type = EntityType.valueOf(rs.getString(COL_NODE_TYPE));
			header.setType(EntityTypeUtils.getEntityTypeClassName(type));
			header.setName(rs.getString(COL_NODE_NAME));
			header.setVersionNumber(rs.getLong(COL_REVISION_NUMBER));
			header.setVersionLabel(rs.getString(COL_REVISION_LABEL));
			header.setBenefactorId(rs.getLong(BENEFACTOR_ALIAS));
			header.setCreatedBy(rs.getString(COL_NODE_CREATED_BY));
			header.setCreatedOn(new Date(rs.getLong(COL_NODE_CREATED_ON)));
			header.setModifiedBy(rs.getString(COL_REVISION_MODIFIED_BY));
			header.setModifiedOn(new Date(rs.getLong(COL_REVISION_MODIFIED_ON)));
			return header;
		}
	};

	// This is better suited for JDBC query.
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private TransactionalMessenger transactionalMessenger;

	@Autowired
	private DBOBasicDao dboBasicDao;

	private final Long ROOT_NODE_ID = Long.parseLong(StackConfigurationSingleton.singleton().getRootFolderEntityId());
	
	private static final String BIND_ID_KEY = "bindId";
	private static final String SQL_ETAG_WITHOUT_LOCK = "SELECT "+COL_NODE_ETAG+" FROM "+TABLE_NODE+" WHERE ID = ?";
	private static final String SQL_ETAG_FOR_UPDATE = SQL_ETAG_WITHOUT_LOCK+" FOR UPDATE";
	
	private static final String SQL_GET_ALL_VERSION_NUMBERS = "SELECT "+COL_REVISION_NUMBER+" FROM "+TABLE_REVISION+" WHERE "+COL_REVISION_OWNER_NODE +" = ? ORDER BY "+COL_REVISION_NUMBER+" DESC";

	
	private static final String SQL_COUNT_ALL = "SELECT COUNT("+COL_NODE_ID+") FROM "+TABLE_NODE;
	// Used to determine if a node id already exists
	private static final String SQL_COUNT_NODE_ID = "SELECT COUNT("+COL_NODE_ID+") FROM "+TABLE_NODE+" WHERE "+COL_NODE_ID +" = :"+BIND_ID_KEY;
	private static final String SQL_COUNT_REVISON_ID = "SELECT COUNT("+COL_REVISION_OWNER_NODE+") FROM "+TABLE_REVISION+" WHERE "+COL_REVISION_OWNER_NODE +" = ? AND "+COL_REVISION_NUMBER+" = ?";

	private static final String SQL_GET_FILE_HANDLE_IDS =
			"SELECT DISTINCT "+COL_REVISION_FILE_HANDLE_ID
			+" FROM "+TABLE_REVISION
			+" WHERE "+COL_REVISION_OWNER_NODE+" = ?";
	
	private static final String SQL_DELETE_BY_IDS = "DELETE FROM " + TABLE_NODE + " WHERE ID IN (:"+ IDS_PARAM_NAME+")";
	
	@WriteTransaction
	@Override
	public String createNew(Node dto) throws NotFoundException, DatastoreException, InvalidModelException {
		Node node = createNewNode(dto);
		return node.getId();
	}
	
	@WriteTransaction
	@Override
	public Node bootstrapNode(Node node, long id) {
		ValidateArgument.required(node, "Entity");
		// ensure the ID is reserved.
		idGenerator.reserveId(id, IdType.ENTITY_ID);
		node.setId(KeyFactory.keyToString(id));
		return create(node);
	}
	
	@WriteTransaction
	@Override
	public Node createNewNode(Node node) throws NotFoundException, DatastoreException, InvalidModelException {
		ValidateArgument.required(node, "Entity");
		// issue a new ID for this node.
		long newId = idGenerator.generateNewId(IdType.ENTITY_ID);
		node.setId(KeyFactory.keyToString(newId));
		return create(node);
	}
	
	/**
	 * Create a new node with an ID.
	 * s
	 * @param dto
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	private Node create(Node dto) throws NotFoundException, DatastoreException, InvalidModelException {
		ValidateArgument.required(dto, "Entity");
		ValidateArgument.required(dto.getCreatedByPrincipalId(), "Entity.createdBy");
		ValidateArgument.required(dto.getCreatedOn(), "Entity.createdOn");
		ValidateArgument.required(dto.getNodeType(), "Entity.type");
		ValidateArgument.required(dto.getModifiedByPrincipalId(), "Entity.modifiedBy");
		ValidateArgument.required(dto.getModifiedOn(), "Entity.modifiedOn");
		ValidateArgument.required(dto.getId(), "Entity.id");
		if(dto.getName() == null) {
			dto.setName(dto.getId());
		}

		DBORevision dboRevision = NodeUtils.transalteNodeToDBORevision(dto);
		dboRevision.setRevisionNumber(NodeConstants.DEFAULT_VERSION_NUMBER);
		DBONode dboNode = NodeUtils.translateNodeToDBONode(dto);
		dboNode.setCurrentRevNumber(dboRevision.getRevisionNumber());

		// Start it with a new e-tag
		dboNode.seteTag(UUID.randomUUID().toString());
		transactionalMessenger.sendMessageAfterCommit(new MessageToSend().withObservableEntity(dboNode).withChangeType(ChangeType.CREATE).withUserId(dboNode.getCreatedBy()));

		// Now create the revision
		dboRevision.setOwner(dboNode.getId());
		// Now save the node and revision
		try{
			dboBasicDao.createNew(dboNode);
		}catch(IllegalArgumentException e){
			checkExceptionDetails(dboNode.getName(), dboNode.getAlias(), KeyFactory.keyToString(dboNode.getParentId()), e);
		}
		dboBasicDao.createNew(dboRevision);		
		return getNode(""+dboNode.getId());
	}

	/**
	 * Determine which constraint was violated and throw a more meaningful exception.
	 * @param dto
	 * @param node
	 * @param e
	 */
	private void checkExceptionDetails(String name, String alias, String parentId, IllegalArgumentException e) {
		if(e.getMessage().indexOf(CONSTRAINT_UNIQUE_CHILD_NAME) > 0) throw new NameConflictException("An entity with the name: "+name+" already exists with a parentId: "+parentId);
		if(e.getMessage().indexOf(CONSTRAINT_UNIQUE_ALIAS) > 0) throw new NameConflictException("The friendly url name (alias): "+alias+" is already taken.  Please select another.");
		throw e;
	}
	
	@WriteTransaction
	@Override
	public Long createNewVersion(Node newVersion) throws NotFoundException, DatastoreException, InvalidModelException {
		if(newVersion == null) throw new IllegalArgumentException("New version node cannot be null");
		if(newVersion.getId() == null) throw new IllegalArgumentException("New version node ID cannot be null");
		// Get the Node
		Long nodeId = KeyFactory.stringToKey(newVersion.getId());
		DBONode jdo = getNodeById(nodeId);
		// Look up the current version
		DBORevision rev  = getNodeRevisionById(jdo.getId(), jdo.getCurrentRevNumber());
		// Make a copy of the current revision with an incremented the version number
		DBORevision newRev = JDORevisionUtils.makeCopyForNewVersion(rev);
		if(newVersion.getVersionLabel() == null) {
			// This is a fix for PLFM-995.  This was modified not to use the KeyFactory because
			// version labels should NOT be prefixed with syn (per PLFM-1408).
			newVersion.setVersionLabel(newRev.getRevisionNumber().toString());
		}
		
		// Now update the new revision and node
		NodeUtils.updateFromDto(newVersion, jdo, newRev, shouldDeleteActivityId(newVersion));
		// The new revision becomes the current version
		jdo.setCurrentRevNumber(newRev.getRevisionNumber());

		// Save the change to the node
		dboBasicDao.update(jdo);
		dboBasicDao.createNew(newRev);
		return newRev.getRevisionNumber();
	}

	@Override
	public Node getNode(String id) throws NotFoundException, DatastoreException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		try {
			return this.jdbcTemplate.queryForObject(SQL_SELECT_CURRENT_NODE, new NodeMapper(), KeyFactory.stringToKey(id));
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(ERROR_RESOURCE_NOT_FOUND);
		}
	}
	
	@Override
	public Node getNodeForVersion(String id, Long versionNumber) throws NotFoundException, DatastoreException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		if(versionNumber == null) throw new IllegalArgumentException("Version number cannot be null");
		try {
			return this.jdbcTemplate.queryForObject(SQL_SELECT_NODE_VERSION, new NodeMapper(),versionNumber, KeyFactory.stringToKey(id));
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(ERROR_RESOURCE_NOT_FOUND);
		}
	}

	@WriteTransaction
	@Override
	public boolean delete(String id) throws DatastoreException {
		if(id == null) throw new IllegalArgumentException("NodeId cannot be null");
		Long longId = KeyFactory.stringToKey(id);
		MapSqlParameterSource prams = getNodeParameters(longId);
		// Send a delete message
		transactionalMessenger.sendDeleteMessageAfterCommit(id, ObjectType.ENTITY);
		return dboBasicDao.deleteObjectByPrimaryKey(DBONode.class, prams);
	}
	
	@WriteTransaction
	@Override
	public int delete(List<Long> ids) throws DatastoreException{
		ValidateArgument.required(ids, "ids");
		if(ids.isEmpty()){
			//no need to update database if not deleting anything
			return 0;
		}
		
		for(long id : ids){
			String stringID = KeyFactory.keyToString(id);
			transactionalMessenger.sendDeleteMessageAfterCommit(stringID, ObjectType.ENTITY);
		}
		MapSqlParameterSource parameters = new MapSqlParameterSource(IDS_PARAM_NAME, ids);
		return namedParameterJdbcTemplate.update(SQL_DELETE_BY_IDS, parameters);
	}
	
	@WriteTransaction
	@Override
	public void deleteVersion(String nodeId, Long versionNumber) throws NotFoundException, DatastoreException {
		// Get the version in question
		Long id = KeyFactory.stringToKey(nodeId);
		// Delete the revision.
		boolean wasDeleted = dboBasicDao.deleteObjectByPrimaryKey(DBORevision.class, getRevisionParameters(id, versionNumber));
		if(wasDeleted){
			// Make sure the node is still pointing the the current version
			List<Long> versions = getVersionNumbers(nodeId);
			if(versions == null || versions.size() < 1){
				throw new IllegalArgumentException("Cannot delete the last version of a node");
			}
			DBONode node = getNodeById(id);
			// Make sure the node is still pointing the the current version
			node.setCurrentRevNumber(versions.get(0));
			dboBasicDao.update(node);
		}
	}
	
	
	@Override
	public EntityType getNodeTypeById(String nodeId) throws NotFoundException, DatastoreException {
		if(nodeId == null) throw new IllegalArgumentException("Node Id cannot be null");
		try{
			String typeString = this.jdbcTemplate.queryForObject(GET_NODE_TYPE_SQL, String.class, KeyFactory.stringToKey(nodeId));
			return EntityType.valueOf(typeString);
		} catch(EmptyResultDataAccessException e){
			throw new NotFoundException(ERROR_RESOURCE_NOT_FOUND);
		}
	}
	

	
	/**
	 * Try to get a node, and throw a NotFoundException if it fails.
	 * @param id
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
	private DBONode getNodeById(Long id) throws NotFoundException, DatastoreException{
		if(id == null) throw new IllegalArgumentException("Node ID cannot be null");
		MapSqlParameterSource params = getNodeParameters(id);
		return dboBasicDao.getObjectByPrimaryKey(DBONode.class, params);
	}

	/**
	 * @param id
	 * @return params
	 */
	public MapSqlParameterSource getNodeParameters(Long id) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", id);
		return params;
	}
	

	private DBORevision getCurrentRevision(DBONode node) throws NotFoundException, DatastoreException{
		if(node == null) throw new IllegalArgumentException("Node cannot be null");
		return getNodeRevisionById(node.getId(),  node.getCurrentRevNumber());
	}
	
	private DBORevision getNodeRevisionById(Long id, Long revNumber) throws NotFoundException, DatastoreException{
		MapSqlParameterSource params = getRevisionParameters(id, revNumber);
		return dboBasicDao.getObjectByPrimaryKey(DBORevision.class, params);
	}

	/**
	 * Get the parameters used to get revision.
	 * @param id
	 * @param revNumber
	 * @return
	 */
	private MapSqlParameterSource getRevisionParameters(Long id, Long revNumber) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("owner", id);
		params.addValue("revisionNumber", revNumber);
		return params;
	}

	@Override
	public Annotations getUserAnnotationsV1(final String id) throws NotFoundException, DatastoreException {
		ValidateArgument.requiredNotEmpty(id, "id");
		return getAnnotations(id, null, COL_REVISION_USER_ANNOTATIONS_V1_BLOB);
	}

	@Override
	public Annotations getUserAnnotationsV1ForVersion(final String id, Long versionNumber) throws NotFoundException, DatastoreException {
		ValidateArgument.requiredNotEmpty(id, "id");
		ValidateArgument.required(versionNumber, "versionNumber");
		return getAnnotations(id, versionNumber, COL_REVISION_USER_ANNOTATIONS_V1_BLOB);
	}

	@Override
	public Annotations getEntityPropertyAnnotations(final String id) throws NotFoundException, DatastoreException {
		ValidateArgument.requiredNotEmpty(id, "id");
		return getAnnotations(id, null, COL_REVISION_ENTITY_PROPERTY_ANNOTATIONS_BLOB);
	}

	@Override
	public Annotations getEntityPropertyAnnotationsForVersion(final String id, Long versionNumber) throws NotFoundException, DatastoreException {
		ValidateArgument.requiredNotEmpty(id, "id");
		ValidateArgument.required(versionNumber, "versionNumber");
		return getAnnotations(id, versionNumber, COL_REVISION_ENTITY_PROPERTY_ANNOTATIONS_BLOB);
	}
	private Annotations getAnnotations(final String id, Long version, final String annotationsColumnName){
		final Long idLong = KeyFactory.stringToKey(id);

		StringBuilder sql = new StringBuilder(SELECT_ANNOTATIONS_ONLY_SELECT_CLAUSE_PREFIX);
		sql.append(annotationsColumnName);
		sql.append(SELECT_ANNOTATIONS_ONLY_FROM_AND_WHERE_CLAUSE_PREFIX);
		Object[] args;
		if(version == null){
			sql.append("N." + COL_CURRENT_REV);
			args = new Object[]{idLong};
		}else {
			sql.append("?");
			args = new Object[]{idLong, version};
		}
		// Select just the references, not the entire node.
		try{
			Annotations annos =  jdbcTemplate.queryForObject(sql.toString(), new AnnotationsRowMapper(annotationsColumnName), args);
			// Remove the eTags when version is specified (See PLFM-1420)
			if(annos != null && version != null){
					annos.setEtag(NodeConstants.ZERO_E_TAG);
			}
			return annos;
		}catch (EmptyResultDataAccessException e){
			// Occurs if there are no results
			throw new NotFoundException(CANNOT_FIND_A_NODE_WITH_ID+id);
		}
	}

	/**
	 * A RowMapper that extracts Annotation from a result set.
	 * The result set must COL_REVISION_ANNOS_BLOB, COL_NODE_ETAG, COL_NODE_CREATED_ON, COL_NODE_ID, COL_NODE_CREATED_BY
	 *
	 */

	private static class AnnotationsRowMapper implements RowMapper<Annotations> {

		private String annotationColumnName;

		private AnnotationsRowMapper(String annotationColumnName){
			this.annotationColumnName = annotationColumnName;
		}

		@Override
		public Annotations mapRow(ResultSet rs, int rowNum) throws SQLException {
			Annotations annos = null;
			byte[] bytes = rs.getBytes(annotationColumnName);
			if(bytes != null){
				try {
					annos = AnnotationUtils.decompressedAnnotationsV1(bytes);
				} catch (IOException e) {
					throw new DatastoreException(e);
				}
			}else{
				// If there is no annotations blob then create a new one.
				annos = new Annotations();
			}
			// Pull out the rest of the data.
			annos.setEtag(rs.getString(COL_NODE_ETAG));
			annos.setId(KeyFactory.keyToString(rs.getLong(COL_NODE_ID)));
			return annos;
		}
	}
	
	@Override
	public String peekCurrentEtag(String id) throws NotFoundException, DatastoreException {
		try{
			return jdbcTemplate.queryForObject(SQL_ETAG_WITHOUT_LOCK, String.class, KeyFactory.stringToKey(id));
		}catch(EmptyResultDataAccessException e){
			// Occurs if there are no results
			throw new NotFoundException(CANNOT_FIND_A_NODE_WITH_ID+id);
		}
	}

	@MandatoryWriteTransaction
	@Override
	public String lockNode(final String nodeIdString) {
		Long longId = KeyFactory.stringToKey(nodeIdString);
		String currentTag = jdbcTemplate.queryForObject(SQL_ETAG_FOR_UPDATE, String.class, longId);
		return currentTag;
	}

	@WriteTransaction
	@Override
	public void updateNode(Node updatedNode) throws NotFoundException, DatastoreException, InvalidModelException {
		if (updatedNode == null) {
			throw new IllegalArgumentException("Node to update cannot be null");
		}
		if (updatedNode.getId() == null) {
			throw new IllegalArgumentException("Node to update cannot have a null ID");
		}
		Long nodeId = KeyFactory.stringToKey(updatedNode.getId());

		String newName = updatedNode.getName();
		Long newParentId = NodeUtils.translateNodeId(updatedNode.getParentId());
		String newAlias = NodeUtils.translateAlias(updatedNode.getAlias());

		// Update the node.
		try {
			this.jdbcTemplate.update(UPDATE_NODE, newName, newParentId, newAlias, nodeId);
		} catch (DataIntegrityViolationException e) {
			// Check to see if this is a duplicate name exception.
			checkExceptionDetails(updatedNode.getName(), updatedNode.getAlias(), updatedNode.getParentId(),
					new IllegalArgumentException(e));
		}
		// update the revision
		long currentRevision = getCurrentRevisionNumber(updatedNode.getId());
		Long newActivity = NodeUtils.translateActivityId(updatedNode.getActivityId());
		String newComment = NodeUtils.translateVersionComment(updatedNode.getVersionComment());
		String newLabel = NodeUtils.translateVersionLabel(updatedNode.getVersionLabel());
		Long newFileHandleId = NodeUtils.translateFileHandleId(updatedNode.getFileHandleId());
		byte[] newColumns = NodeUtils.createByteForIdList(updatedNode.getColumnModelIds());
		byte[] newScope = NodeUtils.createByteForIdList(updatedNode.getScopeIds());
		byte[] newReferences = NodeUtils.compressReference(updatedNode.getReference());
		// Update the revision
		this.jdbcTemplate.update(UPDATE_REVISION, newActivity, newComment, newLabel, newFileHandleId, newColumns,
				newScope, newReferences, nodeId, currentRevision);
	}
	

	@WriteTransaction
	@Override
	public void updateUserAnnotationsV1(String nodeId, Annotations updatedAnnos) throws NotFoundException, DatastoreException {
		updateAnnotations(nodeId, updatedAnnos, COL_REVISION_USER_ANNOTATIONS_V1_BLOB);
	}

	@WriteTransaction
	@Override
	public void updateEntityPropertyAnnotations(String nodeId, Annotations updatedAnnos) throws NotFoundException, DatastoreException {
		updateAnnotations(nodeId, updatedAnnos, COL_REVISION_ENTITY_PROPERTY_ANNOTATIONS_BLOB);
	}

	@WriteTransaction
	void updateAnnotations(String nodeId, Annotations updatedAnnos, String annotationColumnName) throws NotFoundException, DatastoreException {

		if(updatedAnnos == null) throw new IllegalArgumentException("Updateded Annotations cannot be null");
		if(updatedAnnos.getId() == null) throw new IllegalArgumentException("Node ID cannot be null");
		if(updatedAnnos.getEtag() == null) throw new IllegalArgumentException("Annotations must have a valid eTag");

		final Long nodeIdLong = KeyFactory.stringToKey(nodeId);
		final Long currentRevision = getCurrentRevisionNumber(nodeId);

		// now update the annotations from the passed values.
		try {
			// Compress the annotations.
			byte[] newAnnos = AnnotationUtils.compressAnnotationsV1(updatedAnnos);
			this.jdbcTemplate.update(String.format(SQL_UPDATE_ANNOTATIONS_FORMAT, annotationColumnName), newAnnos, nodeIdLong, currentRevision);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

	@Override
	public List<Long> getVersionNumbers(String id) throws NotFoundException, DatastoreException {
		List<Long> list = new ArrayList<Long>();
		List<Map<String, Object>> restuls = jdbcTemplate.queryForList(SQL_GET_ALL_VERSION_NUMBERS, KeyFactory.stringToKey(id));
		for(Map<String, Object> row: restuls){
			Long revId = (Long) row.get(COL_REVISION_NUMBER);
			list.add(revId);
		}
		return list;
	}

	@Override
	public List<VersionInfo> getVersionsOfEntity(final String entityId, long offset,
			long limit) throws NotFoundException, DatastoreException {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(OWNER_ID_PARAM_NAME, KeyFactory.stringToKey(entityId));
		params.addValue(OFFSET_PARAM_NAME, offset);
		params.addValue(LIMIT_PARAM_NAME, limit);
		return namedParameterJdbcTemplate.query(SQL_GET_ALL_VERSION_INFO_PAGINATED, params, new RowMapper<VersionInfo>() {

			@Override
			public VersionInfo mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				VersionInfo info = new VersionInfo();
				info.setId(entityId);
				info.setModifiedByPrincipalId(rs.getString(COL_REVISION_MODIFIED_BY));
				info.setModifiedOn(new Date(rs.getLong(COL_REVISION_MODIFIED_ON)));
				info.setVersionNumber(rs.getLong(COL_REVISION_NUMBER));
				info.setVersionLabel(rs.getString(COL_REVISION_LABEL));
				info.setVersionComment(rs.getString(COL_REVISION_COMMENT));
				info.setContentMd5(rs.getString(COL_FILES_CONTENT_MD5));
				info.setContentSize(rs.getString(COL_FILES_CONTENT_SIZE));
				return info;
			}

		});
	}

	@WriteTransaction
	@Override
	public void afterPropertiesSet() throws Exception {

	}

	@Override
	public boolean doesNodeExist(Long nodeId) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(BIND_ID_KEY, nodeId);
		try{
			long count = namedParameterJdbcTemplate.queryForObject(SQL_COUNT_NODE_ID, parameters, Long.class);
			return count > 0;
		}catch(Exception e){
			// Can occur when the schema does not exist.
			return false;
		}
	}
	
	@Override
	public boolean isNodeAvailable(Long nodeId) {
		try{
			Long benefactorId = getBenefactorId(nodeId);
			if(benefactorId == null){
				return false;
			}
			// node is available if it is not in the trash.
			return !TRASH_FOLDER_ID.equals(benefactorId);
		}catch(Exception e){
			// Can occur when the schema does not exist.
			return false;
		}
	}

	/**
	 * Call the getEntityBenefactorId() function to get the node's benefactor.
	 * 
	 * @param nodeId
	 * @return
	 */
	private Long getBenefactorId(Long nodeId) {
		ValidateArgument.required(nodeId, "nodeId");
		Long benefactorId = this.jdbcTemplate.queryForObject(SQL_SELECT_GET_ENTITY_BENEFACTOR_ID, Long.class, nodeId);
		return benefactorId;
	}
	
	@Override 
	public boolean isNodeAvailable(String nodeId){
		ValidateArgument.required("EntityId", nodeId);
		Long longId = KeyFactory.stringToKey(nodeId);
		return isNodeAvailable(longId);
	}
	
	@Override
	public long getCount() {
		return jdbcTemplate.queryForObject(SQL_COUNT_ALL, Long.class);
	}
	
	@Override
	public boolean doesNodeRevisionExist(String nodeId, Long revNumber) {
		try{
			long count = jdbcTemplate.queryForObject(SQL_COUNT_REVISON_ID, Long.class, KeyFactory.stringToKey(nodeId), revNumber);
			return count > 0;
		}catch(Exception e){
			// Can occur when the schema does not exist.
			return false;
		}
	}

	@Override
	public EntityHeader getEntityHeader(String nodeId, Long versionNumber) throws DatastoreException, NotFoundException {
		Reference ref = new Reference();
		ref.setTargetId(nodeId);
		ref.setTargetVersionNumber(versionNumber);
		LinkedList<Reference> list = new LinkedList<Reference>();
		list.add(ref);
		List<EntityHeader> header = getEntityHeader(list);
		if(header.size() != 1){
			throw new NotFoundException(ERROR_RESOURCE_NOT_FOUND);
		}
		return header.get(0);
	}
	
	@Override
	public List<EntityHeader> getEntityHeader(List<Reference> references) {
		ValidateArgument.required(references, "references");
		if(references.isEmpty()){
			return new LinkedList<EntityHeader>();
		}
		Set<Long> entityIdSet = Sets.newHashSetWithExpectedSize(references.size());
		for(Reference ref:references){
			Long id = KeyFactory.stringToKey(ref.getTargetId());
			entityIdSet.add(id);
		}
		List<EntityHeader> unorderedResults = getEntityHeader(entityIdSet);
		Map<Long, EntityHeader> idToHeader = Maps.newHashMapWithExpectedSize(unorderedResults.size());
		for(EntityHeader header: unorderedResults){
			Long id = KeyFactory.stringToKey(header.getId());
			idToHeader.put(id, header);
		}
		// Create the results driven by the input
		List<EntityHeader> finalResults = new ArrayList<EntityHeader>(references.size());
		for(Reference ref: references){
			Long id = KeyFactory.stringToKey(ref.getTargetId());
			EntityHeader original = idToHeader.get(id);
			if(original != null){
				EntityHeader clone = SerializationUtils.cloneJSONEntity(original);
				if(ref.getTargetVersionNumber() != null){
					clone.setVersionLabel(ref.getTargetVersionNumber().toString());
					clone.setVersionNumber(ref.getTargetVersionNumber());
				}
				finalResults.add(clone);
			}
		}
		return finalResults;
	}
	
	@Override
	public List<EntityHeader> getEntityHeader(Set<Long> entityIds) {
		Map<String, Set<Long>> namedParameters = Collections.singletonMap("nodeIds", entityIds);
		return namedParameterJdbcTemplate.query(SELECT_ENTITY_HEADERS_FOR_ENTITY_IDS, namedParameters,ENTITY_HEADER_ROWMAPPER);
	}


	@Override
	public List<EntityHeader> getEntityHeaderByMd5(String md5) throws DatastoreException, NotFoundException {

		if (md5 == null) {
			throw new IllegalArgumentException("md5 cannot be null.");
		}
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_FILES_CONTENT_MD5, md5);
		List<EntityHeader> rowList = namedParameterJdbcTemplate.query(SELECT_NODE_VERSION_BY_FILE_MD5, paramMap, ENTITY_HEADER_ROWMAPPER);

		if (rowList.size() > NODE_VERSION_LIMIT_BY_FILE_MD5) {
			throw new DatastoreException("MD5 " + md5 + " maps to more than "
					+ NODE_VERSION_LIMIT_BY_FILE_MD5 + " entity versions.");
		}
		return rowList;
	}

	/**
	 * Create a header for 
	 * @param nodeId
	 * @param ptn
	 * @return the entity header
	 */
	public static EntityHeader createHeaderFromParentTypeName(ParentTypeName ptn, Long versionNumber, String versionLabel) {
		EntityHeader header = new EntityHeader();
		header.setId(KeyFactory.keyToString(ptn.getId()));
		header.setName(ptn.getName());
		header.setVersionNumber(versionNumber);
		header.setVersionLabel(versionLabel);
		EntityType type = ptn.getType();
		header.setType(EntityTypeUtils.getEntityTypeClassName(type));
		return header;
	}
	/**
	 * Fetch the Parent, Type, Name for a Node.
	 * @param nodeId
	 * @return
	 * @throws NotFoundException 
	 */
	private ParentTypeName getParentTypeName(Long nodeId) throws NotFoundException{
		if(nodeId == null) throw new IllegalArgumentException("NodeId cannot be null");
		try{
			Map<String, Object> row = jdbcTemplate.queryForMap(SQL_SELECT_PARENT_TYPE_NAME, nodeId);
			ParentTypeName results = new ParentTypeName();
			results.setId((Long) row.get(COL_NODE_ID));
			results.setName((String) row.get(COL_NODE_NAME));
			results.setParentId((Long) row.get(COL_NODE_PARENT_ID));
			results.setType(EntityType.valueOf((String) row.get(COL_NODE_TYPE)));
			return results;
		}catch(EmptyResultDataAccessException e){
			// Occurs if there are no results
			throw new NotFoundException(CANNOT_FIND_A_NODE_WITH_ID+nodeId);
		}
	}
	
	/**
	 * returns up to n ancestors of nodeId, ordered from leaf to root and including the given node Id
	 * 
	 * @param nodeId
	 * @param n
	 * @return
	 * @throws NotFoundException
	 */
	private List<ParentTypeName> getAncestorsPTN(Long nodeId, int depth) throws NotFoundException {
		if(nodeId == null) throw new IllegalArgumentException("NodeId cannot be null");
		Map<String, Object> row = null;
		try {
			row = jdbcTemplate.queryForMap(nodeAncestorSQL(depth), nodeId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Entity " + nodeId + " is not found.");
		}
		List<ParentTypeName> result = new ArrayList<ParentTypeName>();
		for (int i=0; i<depth; i++) {
			Long id = (Long)row.get(COL_NODE_ID+"_"+i);
			if (id==null) break;
			ParentTypeName ptn = new ParentTypeName();
			ptn.setId(id);
			ptn.setName((String)row.get(COL_NODE_NAME+"_"+i));
			ptn.setParentId((Long)row.get(COL_NODE_PARENT_ID+"_"+i));
			ptn.setType(EntityType.valueOf((String)row.get(COL_NODE_TYPE+"_"+i)));
			result.add(ptn);
		}
		return result;
	}
	
	public static String nodeAncestorSQL(int depth) {
		if (depth<2) throw new IllegalArgumentException("Depth must be at least 1");
		StringBuilder sb = new StringBuilder("SELECT ");
		for (int i=0; i<depth; i++) {
			if (i>0) sb.append(", ");
			sb.append("n"+i+"."+COL_NODE_ID+" as "+COL_NODE_ID+"_"+i+", ");
			sb.append("n"+i+"."+COL_NODE_NAME+" as "+COL_NODE_NAME+"_"+i+", ");
			sb.append("n"+i+"."+COL_NODE_PARENT_ID+" as "+COL_NODE_PARENT_ID+"_"+i+", ");
			sb.append("n"+i+"."+COL_NODE_TYPE+" as "+COL_NODE_TYPE+"_"+i);
		}
		sb.append(" \nFROM ");
		sb.append(outerJoinElement(depth-1));
		sb.append(" \nWHERE \nn0."+COL_NODE_ID+"=?");
		return sb.toString();
	}
	
	private static String outerJoinElement(int i) {
		if (i<0) throw new IllegalArgumentException(""+i);
		if (i==0) return "JDONODE n0";
		return "("+outerJoinElement(i-1)+") \nLEFT OUTER JOIN JDONODE n"+(i)+" ON n"+(i-1)+".parent_id=n"+(i)+".id";
	}
	
	@Override
	public List<EntityHeader> getEntityPath(String nodeId) throws DatastoreException, NotFoundException {
		// Call the recursive method
		LinkedList<EntityHeader> results = new LinkedList<EntityHeader>();
		appendPathBatch(results, KeyFactory.stringToKey(nodeId));
		return results;
	}
	
	public static final int BATCH_PATH_DEPTH = 5;

	/**
	 * A recursive method to build up the full path of of an entity, recursing in batch rather than one 
	 * node at a time.
	 * The first EntityHeader in the results will be the Root Node, and the last EntityHeader will be the requested Node.
	 * @param results
	 * @param nodeId
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	private void appendPathBatch(LinkedList<EntityHeader> results, Long nodeId) throws NotFoundException, DatastoreException{
		List<ParentTypeName> ptns = getAncestorsPTN(nodeId, BATCH_PATH_DEPTH); // ordered from leaf to root, length always >=1
		for (ParentTypeName ptn : ptns) {
			EntityHeader header = createHeaderFromParentTypeName(ptn, null, null);
			// Add at the front
			results.add(0, header);
		}
		Long lastParentId = ptns.get(ptns.size()-1).getParentId();
		if(lastParentId!= null){
			// Recurse
			appendPathBatch(results, lastParentId);
		}
	}
	
	@Override
	public String getNodeIdForPath(String path) throws DatastoreException {
		// Get the names
		Map<String, Object> params = new HashMap<String, Object>();
		String sql = createPathQuery(path, params);
		// Since this query is used to boostrap the system we want to return null if the
		// the schema has not been created yet.
		try{
			List<Map<String, Object>> list = namedParameterJdbcTemplate.queryForList(sql, params);
			if(list == null || list.size() < 1) return null;
			if(list.size() > 1) throw new IllegalStateException("Found more than one node with a path: "+path);
			Map<String, Object> row = list.get(0);
			Long id = (Long) row.get(COL_NODE_ID);
			return KeyFactory.keyToString(id);
		}catch(BadSqlGrammarException e){
			// Was this simply called before the schema was setup?
			if(e.getMessage().indexOf("doesn't exist")> 0) return null;
			throw e;
		}
	}

	/**
	 * Builds up a path query.
	 * @param names
	 * @param params
	 * @return
	 */
	static String createPathQuery(String path, Map<String, Object> params) {
		List<String> names = getNamesFromPath(path);
		// Build up the SQL from the Names
		StringBuilder sql = new StringBuilder();
		String lastAlias = "n"+(names.size()-1);
		sql.append("SELECT ");
		sql.append(lastAlias);
		sql.append(".");
		sql.append(COL_NODE_ID);
		sql.append(" FROM ");
		StringBuilder where = new StringBuilder();
		where.append(" WHERE ");
		String previousAlias = null;
		for(int i=1; i<names.size(); i++){
			// We need an alias for each name
			if(i != 1){
				sql.append(", ");
				where.append(" AND ");
			}
			String alias = "n"+i;
			sql.append(TABLE_NODE+" n"+i);
			// Now add the where
			where.append(alias);
			where.append(".");
			where.append(COL_NODE_PARENT_ID);
			if(previousAlias == null){
				where.append(" IS NULL");
			}else{
				where.append(" = ");
				where.append(previousAlias);
				where.append(".");
				where.append(COL_NODE_ID);
			}
			where.append(" AND ");
			where.append(alias);
			where.append(".");
			where.append(COL_NODE_NAME);
			where.append(" = :");
			String key2 = "nam"+i;
			where.append(key2);
			params.put(key2, names.get(i));
			// This alias becomes the previous
			previousAlias = alias;
		}
		sql.append(where);
		return sql.toString();
	}
	/**
	 * Get all of the names from a path.
	 * @param path
	 * @return
	 */
	protected static List<String> getNamesFromPath(String path){
		if(path == null) throw new IllegalArgumentException("Path cannot be null");
		if(!path.startsWith(NodeConstants.PATH_PREFIX)){
			path = NodeConstants.PATH_PREFIX+path;
		}
		String[] split = path.split(NodeConstants.PATH_PREFIX);
		List<String> resutls = new ArrayList<String>();
		for(int i=0; i<split.length; i++){
			if(i==0){
				resutls.add(NodeConstants.PATH_PREFIX);
			}else{
				resutls.add(split[i].trim());
			}
		}
		return resutls;
	}

	@Override
	public List<String> getChildrenIdsAsList(String id) throws DatastoreException {
		List<String> list = new ArrayList<String>();
		List<Map<String, Object>> restuls = jdbcTemplate.queryForList(SQL_GET_ALL_CHILDREN_IDS, KeyFactory.stringToKey(id));
		for(Map<String, Object> row: restuls){
			Long childId = (Long) row.get(COL_NODE_ID);
			list.add(KeyFactory.keyToString(childId));
		}
		return list;
	}

	@Override
	public String getParentId(String nodeId) throws NumberFormatException, NotFoundException, DatastoreException{
		ParentTypeName nodeParent = getParentTypeName(KeyFactory.stringToKey(nodeId));
		Long pId = nodeParent.getParentId();
		String toReturn = KeyFactory.keyToString(pId);
		return toReturn;
	}

	@Override
	public Long getCurrentRevisionNumber(String nodeId) throws NotFoundException, DatastoreException{
		if(nodeId == null) throw new IllegalArgumentException("Node Id cannot be null");
		try{
			return this.jdbcTemplate.queryForObject(GET_CURRENT_REV_NUMBER_SQL, Long.class, KeyFactory.stringToKey(nodeId));
		}catch(EmptyResultDataAccessException e){
			throw new NotFoundException(ERROR_RESOURCE_NOT_FOUND);
		}
	}
	
	@Override
	public String getActivityId(String nodeId) throws NotFoundException, DatastoreException {
		return getActivityId(nodeId, getCurrentRevisionNumber(nodeId));	
	}

	@Override
	public String getActivityId(String nodeId, Long revNumber) throws NotFoundException, DatastoreException {
		if(nodeId == null) throw new IllegalArgumentException("Node Id cannot be null");
		try{
			Long activityId = this.jdbcTemplate
					.queryForObject(GET_REV_ACTIVITY_ID_SQL, Long.class, KeyFactory.stringToKey(nodeId), revNumber);
			return activityId == null ? null : activityId.toString();
		}catch(EmptyResultDataAccessException e){
			throw new NotFoundException(ERROR_RESOURCE_NOT_FOUND);
		}		
	}
	
	@Override
	public Long getCreatedBy(String nodeId) throws NotFoundException, DatastoreException{
		if(nodeId == null) throw new IllegalArgumentException("Node Id cannot be null");
		try{
			return this.jdbcTemplate.queryForObject(GET_NODE_CREATED_BY_SQL, Long.class, KeyFactory.stringToKey(nodeId));
		}catch(EmptyResultDataAccessException e){
			throw new NotFoundException(ERROR_RESOURCE_NOT_FOUND);
		}
	}

	@Override
	public boolean isNodeRoot(String nodeId) throws NotFoundException, DatastoreException {
		return ROOT_NODE_ID.equals(KeyFactory.stringToKey(nodeId));
	}

	@Override
    public boolean isNodesParentRoot(String nodeId) throws NotFoundException, DatastoreException {
        ParentTypeName ptn = getParentTypeName(KeyFactory.stringToKey(nodeId));
		return ROOT_NODE_ID.equals(ptn.parentId);
	}

	@Override
	public boolean doesNodeHaveChildren(String nodeId) {
		if(nodeId == null) throw new IllegalArgumentException("Node Id cannot be null");
		try{
			this.jdbcTemplate.queryForObject(SQL_GET_FIRST_CHILD, Long.class, KeyFactory.stringToKey(nodeId));
			// At least one node has this parent id.
			return true;
		}catch(EmptyResultDataAccessException e){
			// Nothing has that parent id.
			return false;
		}
	}
	/*
	 * Private Methods
	 */
	private boolean shouldDeleteActivityId(Node dto) {
		return DELETE_ACTIVITY_VALUE.equals(dto.getActivityId()) ? true : false;
	}

	@Override
	public String getFileHandleIdForVersion(String id, Long versionNumber) {
		if (versionNumber == null) {
			versionNumber = getCurrentRevisionNumber(id);
		}
		Long nodeId = KeyFactory.stringToKey(id);
		try{
			Long handleId = jdbcTemplate.queryForObject(SQL_SELECT_REV_FILE_HANDLE_ID, Long.class, nodeId, versionNumber);
			if (handleId != null) {
				return handleId.toString();
			}else{
				return null;
			}
		}catch (EmptyResultDataAccessException e){
			return null;
		}
	}
	
	public List<FileHandleAssociation> getFileHandleAssociationsForCurrentVersion(List<String> entityIds){
		if(entityIds.isEmpty()) {
			return new LinkedList<>();
		}
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue(NODE_IDS_LIST_PARAM_NAME, KeyFactory.stringToKey(entityIds));
		return namedParameterJdbcTemplate.query(
				SELECT_CURRENT_VERSION_FILE_HANDLES,
				parameters, new RowMapper<FileHandleAssociation>() {

			@Override
			public FileHandleAssociation mapRow(ResultSet rs, int rowNum) throws SQLException {
				FileHandleAssociation fha = new FileHandleAssociation();
				fha.setAssociateObjectId(rs.getString(1));
				fha.setAssociateObjectType(FileHandleAssociateType.FileEntity);
				fha.setFileHandleId(rs.getString(2));
				return fha;
			}
		});
	}
	
	

	@Override
	public List<Reference> getCurrentRevisionNumbers(List<String> nodeIds) {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue(NODE_IDS_LIST_PARAM_NAME, KeyFactory.stringToKey(nodeIds));
		List<Reference> refs = this.namedParameterJdbcTemplate.query(SQL_GET_CURRENT_VERSIONS, parameters, new RowMapper<Reference>() {
			@Override
			public Reference mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				Reference ref = new Reference(); 
				ref.setTargetId(KeyFactory.keyToString(rs.getLong(COL_NODE_ID)));
				ref.setTargetVersionNumber(rs.getLong(COL_CURRENT_REV));
				if(rs.wasNull()) ref.setTargetVersionNumber(null);
				return ref;
			}		
		});
		return refs;
	}

	@Override
	public List<ProjectHeader> getProjectHeaders(Long userId, Set<Long> projectIds,
				ProjectListType type, ProjectListSortColumn sortColumn, SortDirection sortDirection, Long limit, Long offset) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(projectIds, "projectIds");
		ValidateArgument.requirement(limit >= 0 && offset >= 0, "limit and offset must be greater than 0");
		if(projectIds.isEmpty()){
			return new LinkedList<>();
		}
		// get one page of projects
		Map<String, Object> parameters = Maps.newHashMap();
		parameters.put(BIND_NODE_IDS, projectIds);
		parameters.put(BIND_PROJECT_STAT_USER_ID, userId);
		StringBuilder sqlBuilder = new StringBuilder(SELECT_PROJECTS_STATS);
		// some types add an additional condition.
		String additionalCondition = getProjectStatAdditionalCondition(parameters, userId, type);
		sqlBuilder.append(additionalCondition);
		// order and paging
		String orgerAndPaging = getProjectStatsOderByAndPaging(parameters, sortColumn, sortDirection, limit, offset);
		sqlBuilder.append(orgerAndPaging);
		return getProjectHeaders(parameters, sqlBuilder.toString());
	}
	
	/**
	 * Get the the additional condition for a project stats query based on type.
	 * @param type
	 * @return
	 */
	public static String getProjectStatAdditionalCondition(Map<String, Object> parameters, Long userId, ProjectListType type){
		switch (type) {
		case MY_PROJECTS:
		case OTHER_USER_PROJECTS:
		case MY_TEAM_PROJECTS:
		case TEAM_PROJECTS:
			return "";
		case MY_CREATED_PROJECTS:
			parameters.put(BIND_CREATED_BY, userId);
			return SELECT_CREATED;
		case MY_PARTICIPATED_PROJECTS:
			parameters.put(BIND_CREATED_BY, userId);
			return SELECT_NOT_CREATED;
		default:
			throw new NotImplementedException("project list type " + type + " not yet implemented");
		}
	}
	
	/**
	 * Build the ORDER BY and paging for a project stats query.
	 * @param parameters
	 * @param sortColumn
	 * @param sortDirection
	 * @param limit
	 * @param offset
	 * @return
	 */
	public static String getProjectStatsOderByAndPaging(Map<String, Object> parameters, ProjectListSortColumn sortColumn, SortDirection sortDirection, Long limit, Long offset){
		StringBuilder builder = new StringBuilder();
		switch (sortColumn) {
		case LAST_ACTIVITY:
			builder.append(SELECT_PROJECTS_ORDER);
			break;
		case PROJECT_NAME:
			builder.append(SELECT_NAME_ORDER);
			break;
		default:
			throw new NotImplementedException("project list sort column " + sortColumn + " not yet implemented");
		}
		builder.append(" ").append(sortDirection.name());
		builder.append(" ");
		String pagingSql = QueryUtils.buildPaging(offset, limit, parameters);
		builder.append(pagingSql);
		return builder.toString();
	}

	private List<ProjectHeader> getProjectHeaders(Map<String, Object> parameters, String selectSql) {
		MapSqlParameterSource params = new MapSqlParameterSource(parameters);
		return namedParameterJdbcTemplate.query(selectSql, params, new RowMapper<ProjectHeader>() {
			@Override
			public ProjectHeader mapRow(ResultSet rs, int rowNum) throws SQLException {
				ProjectHeader header = new ProjectHeader();
				header.setId(KeyFactory.keyToString(rs.getLong(COL_NODE_ID)));
				header.setName(rs.getString(COL_NODE_NAME));
				long lastActivity = rs.getLong(COL_PROJECT_STAT_LAST_ACCESSED);
				if (!rs.wasNull()) {
					header.setLastActivity(new Date(lastActivity));
				}
				header.setModifiedBy(rs.getLong(COL_REVISION_MODIFIED_BY));
				header.setModifiedOn(new Date(rs.getLong(COL_REVISION_MODIFIED_ON)));
				return header;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.NodeDAO#lockAllContainers(java.lang.Long)
	 */
	@Override
	public Set<Long> getAllContainerIds(Collection<Long> parentIds, int maxNumberIds) throws LimitExceededException {
		ValidateArgument.required(parentIds, "parentIds");
		// the parentIds are always included.
		Set<Long> results = new LinkedHashSet<Long>(parentIds);
		if(parentIds.isEmpty()){
			return results;
		}
		Map<String, Object> parameters = new HashMap<String, Object>(2);
		parameters.put(IDS_PARAM_NAME, parentIds);
		parameters.put(BIND_LIMIT, maxNumberIds+1);
		while(true){
			// Get all children at this level.
			List<Long> children = namedParameterJdbcTemplate.queryForList(SQL_SELECT_CONTAINERS_WITH_PARENT_IDS_IN_CLAUSE, parameters, Long.class);
			if(children.isEmpty()){
				// done
				return results;
			}
			/*
			 * If the children size is over the max then a single page exceeded
			 * the max. If the children size + result size is over the max then
			 * the total number exceed the max.
			 */
			if(children.size() > maxNumberIds
					|| children.size()+results.size() > maxNumberIds){
				throw new LimitExceededException(MAXIMUM_NUMBER_OF_IDS_EXCEEDED);
			}
			results.addAll(children);
			// Children become the parents
			parameters.put(IDS_PARAM_NAME, children);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.NodeDAO#getAllContainerIds(java.lang.String)
	 */
	@Override
	public Set<Long> getAllContainerIds(String parentId, int maxNumberIds) throws LimitExceededException{
		ValidateArgument.required(parentId, "parentId");
		Long id = KeyFactory.stringToKey(parentId);
		List<Long> ids = new LinkedList<>();
		ids.add(id);
		return getAllContainerIds(ids, maxNumberIds);
	}


	@Override
	public String getNodeIdByAlias(String alias) {
		ValidateArgument.required(alias, "alias");
		try {
			long id = this.jdbcTemplate.queryForObject(SQL_SELECT_NODE_ID_BY_ALIAS, Long.class, alias);
			return KeyFactory.keyToString(id);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Did not find a match for alias: "+alias);
		}
	}

	@Override
	public String getProjectId(String nodeId) {
		ValidateArgument.required(nodeId, "nodeId");
		long nodeIdLong = KeyFactory.stringToKey(nodeId);
		Long projectId = this.jdbcTemplate.queryForObject(SELECT_FUNCTION_PROJECT_ID, Long.class, nodeIdLong);
		if(projectId == null){
			/*
			 * ProjectId will be null if the node does not exist or if the node
			 * is in the trash. In either case a NotFoundException should be
			 * thrown.
			 */
			throw new NotFoundException(ERROR_RESOURCE_NOT_FOUND);
		}else if(projectId < 0){
			throw new IllegalStateException("Infinite loop detected for: "+nodeId);
		}
		return KeyFactory.keyToString(projectId);
	}
	
	@Override
	public String getBenefactor(String nodeId) {
		ValidateArgument.required(nodeId, "nodeId");
		Long id = KeyFactory.stringToKey(nodeId);
		Long benefactorId = getBenefactorId(id);
		if(benefactorId == null){
			/*
			 * Benefactor will be null if the node does not exist.
			 */
			throw new NotFoundException("Benefactor not found for: "+nodeId);
		}else if (benefactorId < 0){
			throw new IllegalStateException("Infinite loop detected for: "+nodeId);
		}
		return KeyFactory.keyToString(benefactorId);
	}

	@Override
	public Set<Long> getFileHandleIdsAssociatedWithFileEntity(List<Long> fileHandleIds, long entityId) {
		ValidateArgument.required(fileHandleIds, "fileHandleIds");
		Set<Long> results = new HashSet<Long>();
		if (fileHandleIds.isEmpty()) {
			return results;
		}
		results.addAll(fileHandleIds);
		List<Long> foundFileHandleIds = jdbcTemplate.queryForList(SQL_GET_FILE_HANDLE_IDS, Long.class, entityId);
		results.retainAll(foundFileHandleIds);
		return results;
	}

	@Override
	public List<EntityDTO> getEntityDTOs(List<String> ids,final int maxAnnotationSize) {
		ValidateArgument.required(ids, "ids");
		if(ids.isEmpty()){
			return new LinkedList<EntityDTO>();
		}
		List<Long> longIds = KeyFactory.stringToKey(ids);
		Map<String, List<Long>> parameters = new HashMap<String, List<Long>>(1);
		parameters.put(NODE_IDS_LIST_PARAM_NAME, longIds);
		return namedParameterJdbcTemplate.query(SQL_SELECT_ENTITY_DTO , parameters, new RowMapper<EntityDTO>() {

			@Override
			public EntityDTO mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				EntityDTO dto = new EntityDTO();
				long entityId = rs.getLong(COL_NODE_ID);
				dto.setId(entityId);
				dto.setCurrentVersion(rs.getLong(COL_CURRENT_REV));
				dto.setCreatedBy(rs.getLong(COL_NODE_CREATED_BY));
				dto.setCreatedOn(new Date(rs.getLong(COL_NODE_CREATED_ON)));
				dto.setEtag(rs.getString(COL_NODE_ETAG));
				dto.setName(rs.getString(COL_NODE_NAME));
				dto.setType(EntityType.valueOf(rs.getString(COL_NODE_TYPE)));
				dto.setParentId(rs.getLong(COL_NODE_PARENT_ID));
				if(rs.wasNull()){
					dto.setParentId(null);
				}
				dto.setBenefactorId(rs.getLong(BENEFACTOR_FUNCTION_ALIAS));
				if(rs.wasNull()){
					dto.setBenefactorId(null);
				}
				dto.setProjectId(rs.getLong(PROJECT_FUNCTION_ALIAS));
				if(rs.wasNull()){
					dto.setProjectId(null);
				}
				dto.setModifiedBy(rs.getLong(COL_REVISION_MODIFIED_BY));
				dto.setModifiedOn(new Date(rs.getLong(COL_REVISION_MODIFIED_ON)));
				dto.setFileHandleId(rs.getLong(COL_REVISION_FILE_HANDLE_ID));
				if(rs.wasNull()){
					dto.setFileHandleId(null);
				}
				dto.setFileSizeBytes(rs.getLong(COL_FILES_CONTENT_SIZE));
				if(rs.wasNull()) {
					dto.setFileSizeBytes(null);
				}
				dto.setIsInSynapseStorage(NodeUtils.isBucketSynapseStorage(rs.getString(COL_FILES_BUCKET_NAME)));
				if (rs.wasNull()) {
					dto.setIsInSynapseStorage(null);
				}
				byte[] bytes = rs.getBytes(COL_REVISION_USER_ANNOTATIONS_V1_BLOB);
				if(bytes != null){
					try {
						Annotations annos = AnnotationUtils.decompressedAnnotationsV1(bytes);
						dto.setAnnotations(AnnotationUtils.translate(entityId, annos, maxAnnotationSize));
					} catch (IOException e) {
						throw new DatastoreException(e);
					}
				}
				return dto;
			}
		});
	}

	@Override
	public List<EntityHeader> getChildren(String parentId,
			List<EntityType> includeTypes, Set<Long> childIdsToExclude,
			SortBy sortBy, Direction sortDirection, long limit, long offset) {
		ValidateArgument.required(parentId, "parentId");
		ValidateArgument.required(includeTypes, "includeTypes");
		ValidateArgument.requirement(!includeTypes.isEmpty(), "Must have at least one type for includeTypes");
		ValidateArgument.required(sortDirection, "sortDirection");
		Map<String, Object> parameters = new HashMap<String, Object>(1);
		parameters.put(BIND_PARENT_ID , KeyFactory.stringToKey(parentId));
		parameters.put(BIND_NODE_TYPES , getTypeNames(includeTypes));
		parameters.put(BIND_NODE_IDS , childIdsToExclude);
		parameters.put(BIND_LIMIT , limit);
		parameters.put(BIND_OFFSET , offset);
		// build the SQL from the template
		String sql = String.format(SQL_SELECT_CHIDREN_TEMPLATE,
				getFragmentExcludeNodeIds(childIdsToExclude),
				getFragmentSortColumn(sortBy),
				sortDirection.name());
		return namedParameterJdbcTemplate.query(sql,parameters,ENTITY_HEADER_ROWMAPPER);
	}
	
	@Override
	public ChildStatsResponse getChildernStats(ChildStatsRequest request) {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getParentId(), "parentId");
		ValidateArgument.required(request.getIncludeTypes(), "includeTypes");
		ValidateArgument.requirement(!request.getIncludeTypes().isEmpty(),
				"Must have at least one type for includeTypes");
		// null defaults to false
		final boolean includeCount = request.getIncludeTotalChildCount() != null ? request.getIncludeTotalChildCount()
				: false;
		// null defaults to false
		final boolean includeSumSizes = request.getIncludeSumFileSizes() != null ? request.getIncludeSumFileSizes()
				: false;
		// nothing to do if both are false
		if (!includeCount && !includeSumSizes) {
			return new ChildStatsResponse();
		}
		Map<String, Object> parameters = new HashMap<String, Object>(1);
		parameters.put(BIND_PARENT_ID, KeyFactory.stringToKey(request.getParentId()));
		parameters.put(BIND_NODE_TYPES, getTypeNames(request.getIncludeTypes()));
		parameters.put(BIND_NODE_IDS, request.getChildIdsToExclude());
		// build the SQL from the template
		String sql = String.format(SQL_SELECT_CHIDREN_STATS, getFragmentExcludeNodeIds(request.getChildIdsToExclude()));
		return namedParameterJdbcTemplate.queryForObject(sql, parameters, new RowMapper<ChildStatsResponse>() {

			@Override
			public ChildStatsResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
				ChildStatsResponse response = new ChildStatsResponse();
				if (includeCount) {
					response.withTotalChildCount(rs.getLong(1));
				}
				if (includeSumSizes) {
					response.withSumFileSizesBytes(rs.getLong(2));
				}
				return response;
			}
		});
	}
	
	/**
	 * Convert from enums to string names.
	 * @param includeTypes
	 * @return
	 */
	public static List<String> getTypeNames(List<EntityType> includeTypes){
		List<String> results = new LinkedList<String>();
		for(EntityType type: includeTypes){
			results.add(type.name());
		}
		return results;
	}
	
	/**
	 * When childIdsToExclude is not empty then a NOT IN () fragment is used.
	 * 
	 * @param childIdsToExclude
	 * @return
	 */
	public static String getFragmentExcludeNodeIds(Set<Long> childIdsToExclude){
		if(childIdsToExclude == null || childIdsToExclude.isEmpty()){
			return "";
		}else{
			return SQL_ID_NOT_IN_SET;
		}
	}
	
	/**
	 * Get the fragment of column name for a given sortBy.
	 * @param sortBy
	 * @return
	 */
	public static String getFragmentSortColumn(SortBy sortBy) {
		ValidateArgument.required(sortBy, "sortBy");
		switch (sortBy) {
		case NAME:
			return N_NAME;
		case CREATED_ON:
			return N_CREATED_ON;
		default:
			throw new IllegalArgumentException("Unknown SortBy: "+sortBy);
		}
	}

	@Override
	public long getChildCount(String parentId) {
		ValidateArgument.required(parentId, "parentId");
		return jdbcTemplate.queryForObject(SQL_COUNT_CHILDREN, Long.class, KeyFactory.stringToKey(parentId));
	}

	@Override
	public List<NodeIdAndType> getChildren(String parentId, long limit,
			long offset) {
		ValidateArgument.required(parentId, "parentId");
		Long parentIdLong = KeyFactory.stringToKey(parentId);
		return jdbcTemplate.query(SQL_SELECT_CHILDREN, new RowMapper<NodeIdAndType>(){

			@Override
			public NodeIdAndType mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				String nodeId = KeyFactory.keyToString(rs.getLong(COL_NODE_ID));
				EntityType type = EntityType.valueOf(rs.getString(COL_NODE_TYPE));
				return new NodeIdAndType(nodeId, type);
			}}, parentIdLong, limit, offset);
	}

	@Override
	public String lookupChild(String parentId, String entityName) {
		ValidateArgument.required(parentId, "parentId");
		ValidateArgument.required(entityName, "entityName");
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(COL_NODE_PARENT_ID , KeyFactory.stringToKey(parentId));
		parameters.put(COL_NODE_NAME , entityName);
		try {
			Long entityId = namedParameterJdbcTemplate.queryForObject(SQL_SELECT_CHILD, parameters, Long.class);
			return KeyFactory.keyToString(entityId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}

	@Override
	public Map<Long, Long> getSumOfChildCRCsForEachParent(List<Long> parentIds) {
		ValidateArgument.required(parentIds, "parentIdS");
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(BIND_PARENT_ID , parentIds);
		final Map<Long, Long> results = new HashMap<Long, Long>();
		if(parentIds.isEmpty()){
			return results;
		}
		namedParameterJdbcTemplate.query(SQL_SELECT_CHILD_CRC32, parameters, new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				Long id = rs.getLong(COL_NODE_PARENT_ID);
				if(id != null){
					Long crc = rs.getLong("CRC");
					results.put(id, crc);
				}
			}
		});
		return results;
	}

	@Override
	public List<IdAndEtag> getChildren(long parentId) {
		ValidateArgument.required(parentId, "parentId");
		return jdbcTemplate.query(SQL_SELECT_CHILDREN_ID_AND_ETAG, new RowMapper<IdAndEtag>(){
			@Override
			public IdAndEtag mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				Long id = rs.getLong(COL_NODE_ID);
				String etag = rs.getString(COL_NODE_ETAG);
				Long benefactorId = rs.getLong(3);
				if(rs.wasNull()) {
					benefactorId = null;
				}
				return new IdAndEtag(id, etag, benefactorId);
			}}, parentId);
	}

	@Override
	public Set<Long> getAvailableNodes(List<Long> nodeIds) {
		ValidateArgument.required(nodeIds, "nodeIds");
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(BIND_NODE_IDS , nodeIds);
		final HashSet<Long> results = new HashSet<Long>();
		if(nodeIds.isEmpty()){
			return results;
		}
		namedParameterJdbcTemplate.query(SQL_SELECT_BENEFACTORS, parameters, new RowCallbackHandler(){
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				Long id = rs.getLong(COL_NODE_ID);
				Long benefactorId = rs.getLong(BENEFACTOR_ALIAS);
				if(!TRASH_FOLDER_ID.equals(benefactorId)){
					results.add(id);
				}
			}});
		return results;
	}

	@WriteTransaction
	@Override
	public String touch(Long userId, String nodeIdString) {
		ChangeType changeType = ChangeType.UPDATE;
		return touch(userId, nodeIdString, changeType);
	}

	@WriteTransaction
	@Override
	public String touch(Long userId, String nodeIdString, ChangeType changeType) {
		ValidateArgument.required(userId, "UserId");
		ValidateArgument.required(nodeIdString, "nodeId");
		ValidateArgument.required(changeType, "ChangeType");
		String newEtag = UUID.randomUUID().toString();
		long nodeId = KeyFactory.stringToKey(nodeIdString);
		// change the etag first to lock the node row.
		this.jdbcTemplate.update(SQL_TOUCH_ETAG, newEtag, nodeId);
		// update the latest revision.
		Long revisionNumber = this.getCurrentRevisionNumber(nodeIdString);
		long currentTime = System.currentTimeMillis();
		this.jdbcTemplate.update(SQL_TOUCH_REVISION, userId, currentTime, nodeId, revisionNumber);
		transactionalMessenger.sendMessageAfterCommit(new MessageToSend().withObjectId(nodeIdString)
				.withObjectType(ObjectType.ENTITY).withChangeType(changeType).withUserId(userId));
		return newEtag;
	}
	
	@WriteTransaction
	@Override
	public long snapshotVersion(Long userId, String nodeIdString, SnapshotRequest request) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(nodeIdString, "nodeId");
		if(request == null) {
			request = new SnapshotRequest();
		}
		ValidateArgument.required(request, "SnapshotRequest");
		long nodeId = KeyFactory.stringToKey(nodeIdString);
		long modifiedOn = System.currentTimeMillis();
		Long revisionNumber = this.getCurrentRevisionNumber(nodeIdString);
		String label = request.getSnapshotLabel() != null ? request.getSnapshotLabel() : revisionNumber.toString();
		this.jdbcTemplate.update(SQL_CREATE_SNAPSHOT_VERSION, request.getSnapshotComment(), label,
				request.getSnapshotActivityId(), userId, modifiedOn, nodeId, revisionNumber);
		return revisionNumber;
	}

}
