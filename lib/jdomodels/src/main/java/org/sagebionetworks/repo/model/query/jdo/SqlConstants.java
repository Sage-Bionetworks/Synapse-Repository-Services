package org.sagebionetworks.repo.model.query.jdo;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.jdo.BasicIdentifierFactory;
import org.sagebionetworks.repo.model.query.Comparator;
 
@SuppressWarnings("rawtypes")
public class SqlConstants {
	
	public static final String COL_ID					= "ID";
	// Node table constants
	public static final String TABLE_NODE 				= "JDONODE";
	public static final String COL_NODE_ID				= "ID";
	public static final String COL_NODE_PARENT_ID		= "PARENT_ID";
	public static final String COL_NODE_BENEFACTOR_ID	= "BENEFACTOR_ID";
	public static final String COL_NODE_NAME			= "NAME";
	public static final String COL_NODE_ANNOATIONS		= "ANNOTATIONS_ID_OID";
	public static final String COL_NODE_DESCRIPTION 	= "DESCRIPTION";
	public static final String COL_NODE_ETAG 			= "ETAG";
	public static final String COL_NODE_CREATED_BY 		= "CREATED_BY";
	public static final String COL_NODE_CREATED_ON 		= "CREATED_ON";
	public static final String COL_NODE_TYPE			= "NODE_TYPE";
	public static final String COL_NODE_ACL				= "NODE_ACL";
	public static final String COL_CURRENT_REV			= "CURRENT_REV_NUM";
	public static final String DDL_FILE_NODE			="schema/Node-ddl.sql";
	
	// The Revision table
	public static final String TABLE_REVISION 				= "JDOREVISION";
	public static final String COL_REVISION_OWNER_NODE		= "OWNER_NODE_ID";
	public static final String COL_REVISION_NUMBER			= "NUMBER";
	public static final String COL_REVISION_ACTIVITY_ID		= "ACTIVITY_ID";
	public static final String COL_REVISION_LABEL			= "LABEL";
	public static final String COL_REVISION_COMMENT			= "COMMENT";
	public static final String COL_REVISION_ANNOS_BLOB		= "ANNOTATIONS";
	public static final String COL_REVISION_REFS_BLOB		= "REFERENCES";
	public static final String COL_REVISION_MODIFIED_BY		= "MODIFIED_BY";
	public static final String COL_REVISION_MODIFIED_ON		= "MODIFIED_ON";
	public static final String COL_REVISION_FILE_HANDLE_ID	= "FILE_HANDLE_ID";
	public static final String COL_REVISION_COLUMN_MODEL_IDS= "COLUMN_MODEL_IDS";
	public static final String DDL_FILE_REVISION			="schema/Revision-ddl.sql";

	// The Reference table
	public static final String TABLE_REFERENCE						= "JDOREFERENCE";
	public static final String COL_REFERENCE_OWNER_NODE				= "REF_OWNER_NODE_ID";
	public static final String COL_REFERENCE_TARGET_NODE			= "REF_TARGET_NODE_ID";
	public static final String COL_REFERENCE_TARGET_REVISION_NUMBER	= "REF_TARGET_REV_NUM";
	public static final String COL_REFERENCE_GROUP_NAME				= "REF_GROUP_NAME";
	public static final String DDL_FILE_REFERENCE					= "schema/Reference-ddl.sql";
	
	// Annotations tables
	public static final String TABLE_ANNOTATIONS_OWNER	= "ANNOTATIONS_OWNER";
	public static final String TABLE_STRING_ANNOTATIONS	= "JDOSTRINGANNOTATION";
	public static final String TABLE_DOUBLE_ANNOTATIONS	= "JDODOUBLEANNOTATION";
	public static final String TABLE_LONG_ANNOTATIONS	= "JDOLONGANNOTATION";
	public static final String TABLE_DATE_ANNOTATIONS	= "JDODATEANNOTATION";
	public static final String TABLE_STACK_STATUS		= "JDOSTACKSTATUS";
	
	// The one column of the annotations owner table
	public static final String COL_ANNOTATION_OWNER				= "OWNER_ID";
	public static final String DDL_ANNOTATIONS_OWNER			= "schema/AnnotationsOwner-ddl.sql";
	
	// The User Profile table
	public static final String TABLE_USER_PROFILE				= "JDOUSERPROFILE";
	public static final String COL_USER_PROFILE_ID				= "OWNER_ID";
	public static final String COL_USER_PROFILE_ETAG			= "ETAG";
	public static final String COL_USER_PROFILE_PROPS_BLOB		= "PROPERTIES";
	public static final String DDL_FILE_USER_PROFILE			= "schema/UserProfile-ddl.sql";

	// The ACCESS_REQUIREMENT table
	public static final String TABLE_ACCESS_REQUIREMENT				= "ACCESS_REQUIREMENT";
	public static final String COL_ACCESS_REQUIREMENT_ID			= "ID";
	public static final String COL_ACCESS_REQUIREMENT_ETAG			= "ETAG";
	public static final String COL_ACCESS_REQUIREMENT_CREATED_BY	= "CREATED_BY";
	public static final String COL_ACCESS_REQUIREMENT_CREATED_ON	= "CREATED_ON";
	public static final String COL_ACCESS_REQUIREMENT_MODIFIED_BY	= "MODIFIED_BY";
	public static final String COL_ACCESS_REQUIREMENT_MODIFIED_ON	= "MODIFIED_ON";
	public static final String COL_ACCESS_REQUIREMENT_ACCESS_TYPE	= "ACCESS_TYPE";
	public static final String COL_ACCESS_REQUIREMENT_ENTITY_TYPE	= "ENTITY_TYPE";
	public static final String COL_ACCESS_REQUIREMENT_SERIALIZED_ENTITY	= "SERIALIZED_ENTITY";
	public static final String DDL_FILE_ACCESS_REQUIREMENT			= "schema/AccessRequirement-ddl.sql";

	// The SUBJECT_ACCESS_REQUIREMENT table (a join table linking the ENTITY or EVALUTION and ACCESS_REQUIREMENT tables
	// !!! Note: The table name should be SUBJECT_ACCESS_REQUIREMENT, but migration issues prevent
	// !!!       us from doing that as this time.
	public static final String TABLE_SUBJECT_ACCESS_REQUIREMENT		= "NODE_ACCESS_REQUIREMENT";
	public static final String COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID			= "SUBJECT_ID";
	public static final String COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE			= "SUBJECT_TYPE";
	public static final String COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID	= "REQUIREMENT_ID";
	public static final String DDL_FILE_SUBJECT_ACCESS_REQUIREMENT			= "schema/SubjectAccessRequirement-ddl.sql";

	
	// the following are defined temporarily, for a 'bridge' migration table
	public static final String TABLE_NODE_ACCESS_REQUIREMENT		= "NODE_ACCESS_REQUIREMENT";
	public static final String COL_NODE_ACCESS_REQUIREMENT_NODE_ID			= "NODE_ID";
	public static final String COL_NODE_ACCESS_REQUIREMENT_NODE_TYPE			= "NODE_TYPE";
	public static final String COL_NODE_ACCESS_REQUIREMENT_REQUIREMENT_ID	= "REQUIREMENT_ID";
	public static final String DDL_FILE_NODE_ACCESS_REQUIREMENT			= "schema/NodeAccessRequirement-ddl.sql";
	
	// The ACCESS_APPROVAL table
	public static final String TABLE_ACCESS_APPROVAL				= "ACCESS_APPROVAL";
	public static final String COL_ACCESS_APPROVAL_ID				= "ID";
	public static final String COL_ACCESS_APPROVAL_ETAG				= "ETAG";
	public static final String COL_ACCESS_APPROVAL_CREATED_BY		= "CREATED_BY";
	public static final String COL_ACCESS_APPROVAL_CREATED_ON		= "CREATED_ON";
	public static final String COL_ACCESS_APPROVAL_MODIFIED_BY		= "MODIFIED_BY";
	public static final String COL_ACCESS_APPROVAL_MODIFIED_ON		= "MODIFIED_ON";
	public static final String COL_ACCESS_APPROVAL_REQUIREMENT_ID	= "REQUIREMENT_ID";
	public static final String COL_ACCESS_APPROVAL_ACCESSOR_ID		= "ACCESSOR_ID";
	public static final String COL_ACCESS_APPROVAL_ENTITY_TYPE		= "ENTITY_TYPE";
	public static final String COL_ACCESS_APPROVAL_SERIALIZED_ENTITY= "SERIALIZED_ENTITY";
	public static final String DDL_FILE_ACCESS_APPROVAL				= "schema/AccessApproval-ddl.sql";
	
	// The CHANGES table
	public static final String TABLE_CHANGES						= "CHANGES";
	public static final String COL_CHANGES_CHANGE_NUM				= "CHANGE_NUM";
	public static final String COL_CHANGES_TIME_STAMP				= "TIME_STAMP";
	public static final String COL_CHANGES_OBJECT_ID				= "OBJECT_ID";
	public static final String COL_CHANGES_PARENT_ID                = "PARENT_ID";
	public static final String COL_CHANGES_OBJECT_TYPE				= "OBJECT_TYPE";
	public static final String COL_CHANGES_OBJECT_ETAG				= "OBJECT_ETAG";
	public static final String COL_CHANGES_CHANGE_TYPE				= "CHANGE_TYPE";
	public static final String DDL_CHANGES							= "schema/Changes-ddl.sql";
	
	// Sent messages
	public static final String TABLE_SENT_MESSAGES					= "SENT_MESSAGES";
	public static final String COL_SENT_MESSAGES_CHANGE_NUM			= "CHANGE_NUM";
	public static final String COL_SENT_MESSAGES_TIME_STAMP			= "TIME_STAMP";
	public static final String DDL_SENT_MESSAGES					= "schema/SentMessages-ddl.sql";
	
	// The file metada table
	public static final String TABLE_FILES							= "FILES";
	public static final String COL_FILES_ID							= "ID";
	public static final String COL_FILES_ETAG						= "ETAG";
	public static final String COL_FILES_PREVIEW_ID					= "PREVIEW_ID";
	public static final String COL_FILES_CREATED_BY					= "CREATED_BY";
	public static final String COL_FILES_CREATED_ON					= "CREATED_ON";
	public static final String COL_FILES_METADATA_TYPE				= "METADATA_TYPE";
	public static final String COL_FILES_CONTENT_TYPE				= "CONTENT_TYPE";
	public static final String COL_FILES_CONTENT_SIZE				= "CONTENT_SIZE";
	public static final String COL_FILES_CONTENT_MD5				= "CONTENT_MD5";
	public static final String COL_FILES_BUCKET_NAME				= "BUCKET_NAME";
	public static final String COL_FILES_KEY						= "KEY";
	public static final String COL_FILES_NAME						= "NAME";
	public static final String DDL_FILES							= "schema/Files-ddl.sql";

	// 
	public static final String COL_STACK_STATUS_STATUS				= "STATUS";
	public static final String COL_STACK_STATUS_CURRENT_MESSAGE		= "CURRENT_MESSAGE";
	public static final String COL_STACK_STATUS_PENDING_MESSAGE		= "PENDING_MESSAGE";
	public static final String DDL_FILE_STACK_STATUS				= "schema/StackStatus-ddl.sql";
	
	// The width of the string annotations value column
	public static final int STRING_ANNOTATIONS_VALUE_LENGTH = 500;
	
	// There are the column names that all annotation tables have.
	public static final String ANNOTATION_ATTRIBUTE_COLUMN 		= "ATTRIBUTE";
	public static final String ANNOTATION_VALUE_COLUMN			= "VALUE";
	public static final String ANNOTATION_OWNER_ID_COLUMN		= "OWNER_ID";
	public static final String DDL_FILE_STRING_ANNOTATION		= "schema/StringAnnotation-ddl.sql";
	public static final String DDL_FILE_LONG_ANNOTATION			= "schema/LongAnnotation-ddl.sql";
	public static final String DDL_FILE_DATE_ANNOTATION			= "schema/DateAnnotation-ddl.sql";
	public static final String DDL_FILE_DOUBLE_ANNOTATION		= "schema/DoubleAnnotation-ddl.sql";

	// The name of the node type table.
	public static final String TABLE_NODE_TYPE				= "NODE_TYPE";
	public static final String COL_NODE_TYPE_NAME 			= "NAME";
	public static final String COL_NODE_TYPE_ID 			= "ID";
	public static final String DDL_FILE_NODE_TYPE			= "schema/NodeType-ddl.sql";
	
	// The name of the node type table.
	public static final String TABLE_NODE_TYPE_ALIAS		= "NODE_TYPE_ALIAS";
	public static final String COL_OWNER_TYPE	 			= "OWNER_TYPE";
	public static final String COL_NODE_TYPE_ALIAS 			= "ALIAS";
	public static final String DDL_FILE_NODE_TYPE_ALIAS		= "schema/NodeTypeAlias-ddl.sql";
	
	
	public static final String TABLE_ANNOTATION_TYPE		= "ANNOTATION_TYPE";

	public static final String TABLE_USER					= "JDOUSER";
	public static final String TABLE_USER_GROUP				= "JDOUSERGROUP";
	public static final String TABLE_USER_GROUP_USERS		= "JDOUSERGROUPUSERS";
	public static final String COL_USER_GROUP_ID			= "ID";
	public static final String COL_USER_GROUP_NAME 			= "NAME";
	public static final String COL_USER_GROUP_IS_INDIVIDUAL = "ISINDIVIDUAL";
	public static final String COL_USER_GROUP_E_TAG         = "ETAG";
	public static final String COL_USER_GROUP_CREATION_DATE = "CREATION_DATE";
	public static final String DDL_FILE_USER_GROUP			="schema/UserGroup-ddl.sql";
    
    // The group members table
    public static final String TABLE_GROUP_MEMBERS         = "GROUP_MEMBERS";
    public static final String COL_GROUP_MEMBERS_GROUP_ID  = "GROUP_ID";
    public static final String COL_GROUP_MEMBERS_MEMBER_ID = "MEMBER_ID";
    public static final String DDL_FILE_GROUP_MEMBERS      = "schema/GroupMembers-ddl.sql";
    
    // The group parents cache table
    public static final String TABLE_GROUP_PARENTS_CACHE        = "GROUP_PARENTS_CACHE";
    public static final String COL_GROUP_PARENTS_CACHE_GROUP_ID = "GROUP_ID";
    public static final String COL_GROUP_PARENTS_CACHE_PARENTS  = "PARENTS";
    public static final String DDL_FILE_GROUP_PARENTS_CACHE     = "schema/GroupParentsCache-ddl.sql";

	public static final String TABLE_ACCESS_CONTROL_LIST  = "ACL";
	public static final String COL_ACL_ID                 = "ID";
	public static final String COL_ACL_ETAG               = "ETAG";
	public static final String COL_ACL_CREATED_ON         = "CREATED_ON";
	public static final String DDL_FILE_ACL               = "schema/ACL-ddl.sql";
	
	// The resource access table
	public static final String TABLE_RESOURCE_ACCESS			= "JDORESOURCEACCESS";
	public static final String COL_RESOURCE_ACCESS_OWNER		= "OWNER_ID";
	public static final String COL_RESOURCE_ACCESS_GROUP_ID		= "GROUP_ID";
	public static final String COL_RESOURCE_ACCESS_TYPE			= "RESOURCE_TYPE";
	public static final String COL_RESOURCE_ACCESS_RESOURCE_ID	= "RESOURCE_ID";
	public static final String COL_RESOURCE_ACCESS_ID			= "ID";
	public static final String DDL_FILE_RES_ACCESS				= "schema/ResourceAccess-ddl.sql";
	
	// The resource access join table
	// datanucleus doesn't seem to be respecting the join table name when creating the schema
	// so I've modified the string to match the generated name
	public static final String TABLE_RESOURCE_ACCESS_TYPE			= "JDORESOURCEACCESS_ACCESSTYPE"; 
	public static final String COL_RESOURCE_ACCESS__TYPE_OWNER		= "OWNER_ID";
	public static final String COL_RESOURCE_ACCESS_TYPE_ID			= "ID_OID";
	public static final String COL_RESOURCE_ACCESS_TYPE_ELEMENT		= "STRING_ELE";
	public static final String DDL_FILE_RES_ACCESS_TYPE				= "schema/ResourceAccessType-ddl.sql";
	
	// The backup/restore status table
	public static final String TABLE_BACKUP_STATUS 				= "DAEMON_STATUS";
	public static final String COL_BACKUP_ID					= "ID";
	public static final String COL_BACKUP_STATUS				= "STATUS";
	public static final String COL_BACKUP_TYPE					= "TYPE";
	public static final String COL_BACKUP_STARTED_BY 			= "STARTED_BY";
	public static final String COL_BACKUP_STARTED_ON 			= "STARTED_ON";
	public static final String COL_BACKUP_PROGRESS_MESSAGE		= "PROGRESS_MESSAGE";
	public static final String COL_BAKUP_PROGRESS_CURRENT		= "PROGRESS_CURRENT";
	public static final String COL_BACKUP_PROGRESS_TOTAL		= "PROGRESS_TOTAL";
	public static final String COL_BACKUP_ERORR_MESSAGE			= "ERROR_MESSAGE";
	public static final String COL_BACKUP_ERROR_DETAILS			= "ERROR_DETAILS";
	public static final String COL_BACKUP_LOG					= "LOG";
	public static final String COL_BACKUP_URL					= "BACKUP_URL";
	public static final String COL_BACKUP_RUNTIME				= "RUN_TIME_MS";
	public static final String DDL_DAEMON_STATUS				= "schema/DaemonStatus-ddl.sql";
	// the max size of the error message.
	public static final int ERROR_MESSAGE_MAX_LENGTH			= 3000;
	
	public static final String TABLE_BACKUP_TERMINATE 			= "DAEMON_TERMINATE";
	public static final String COL_BACKUP_TERM_OWNER			= "BACKUP_OWNER";
	public static final String COL_BACKUP_FORCE_TERMINATION		= "FORCE_TERMINATION";
	public static final String DDL_DAEMON_TERMINATE				= "schema/DaemonTerminate-ddl.sql";
	
	
	// Preview blobs.
	public static final String TABLE_PREVIEW_BLOB				= "PREVIEW_BLOB";
	public static final String COL_PREVIEW_OWNER_ID				= "OWNER_NODE_ID";
	public static final String COL_PREVIEW_TOKEN_ID				= "TOKEN_ID";
	public static final String COL_PREVIEW_BLOB					= "PREVIEW_BLOB";
	public static final String DDL_FILE_PREVIEW_BLOB			= "schema/PreviewBlob-ddl.sql";
	
	// This constraint ensure that children names are unique within their parent.
	public static final String CONSTRAINT_UNIQUE_CHILD_NAME = "NODE_UNIQUE_CHILD_NAME";
	
	// The ACTIVITY table
	public static final String TABLE_ACTIVITY 					= "ACTIVITY";
	public static final String COL_ACTIVITY_ID 					= "ID";
	public static final String COL_ACTIVITY_ETAG 				= "ETAG";
	public static final String COL_ACTIVITY_CREATED_BY			= "CREATED_BY";
	public static final String COL_ACTIVITY_CREATED_ON			= "CREATED_ON";
	public static final String COL_ACTIVITY_MODIFIED_BY			= "MODIFIED_BY";
	public static final String COL_ACTIVITY_MODIFIED_ON			= "MODIFIED_ON";
	public static final String COL_ACTIVITY_SERIALIZED_OBJECT 	= "SERIALIZED_OBJECT";
	public static final String DDL_FILE_ACTIVITY = "schema/Activity-ddl.sql";
	
	// The trash can table
	public static final String TABLE_TRASH_CAN                  = "TRASH_CAN";
	public static final String COL_TRASH_CAN_NODE_ID            = "NODE_ID";
	public static final String COL_TRASH_CAN_NODE_NAME          = "NODE_NAME";
	public static final String COL_TRASH_CAN_DELETED_BY         = "DELETED_BY";
	public static final String COL_TRASH_CAN_DELETED_ON         = "DELETED_ON";
	public static final String COL_TRASH_CAN_PARENT_ID          = "PARENT_ID";
	public static final String DDL_FILE_TRASH_CAN               = "schema/TrashCan-ddl.sql";
	
	// The wiki page table
	public static final String TABLE_WIKI_PAGE				= "WIKI_PAGE";
	public static final String COL_WIKI_ID					= "ID";
	public static final String COL_WIKI_ETAG				= "ETAG";
	public static final String COL_WIKI_TITLE				= "TITLE";
	public static final String COL_WIKI_CREATED_ON			= "CREATED_ON";
	public static final String COL_WIKI_CREATED_BY			= "CREATED_BY";
	public static final String COL_WIKI_MODIFIED_ON			= "MODIFIED_ON";
	public static final String COL_WIKI_MODIFIED_BY			= "MODIFIED_BY";
	public static final String COL_WIKI_PARENT_ID			= "PARENT_ID";
	public static final String COL_WIKI_ROOT_ID				= "ROOT_ID";
	public static final String COL_WIKI_MARKDOWN			= "MARKDOWN";
	public static final String DDL_FILE_WIKI_PAGE = "schema/WikiPage-ddl.sql";
	
	// The column model table
	public static final String TABLE_COLUMN_MODEL			= "COLUMN_MODEL";
	public static final String COL_CM_ID					= "ID";
	public static final String COL_CM_NAME					= "NAME";
	public static final String COL_CM_HASH					= "HASH";
	public static final String COL_CM_BYTES					= "BYTES";
	public static final String DDL_COLUMN_MODEL = "schema/ColumnModel-ddl.sql";
	
	// The bound column model table
	public static final String TABLE_BOUND_COLUMN			= "BOUND_COLUMN";
	public static final String COL_BOUND_CM_COLUMN_ID		= "COLUMN_ID";
	public static final String COL_BOUND_CM_OBJECT_ID		= "OBJECT_ID";
	public static final String COL_BOUND_CM_IS_CURRENT		= "IS_CURRENT";
	public static final String DDL_BOUND_COLUMN = "schema/BoundColumn-ddl.sql";
	
	// The wiki attachment table
	public static final String TABLE_WIKI_ATTACHMENT				= "WIKI_ATTACHMENTS";
	public static final String COL_WIKI_ATTACHMENT_ID				= "WIKI_ID";
	public static final String COL_WIKI_ATTACHMENT_FILE_HANDLE_ID	= "FILE_HANDLE_ID";
	public static final String COL_WIKI_ATTACHMENT_FILE_NAME		= "FILE_NAME";
	public static final String DDL_FILE_WIKI_ATTATCHMENT = "schema/WikiAttachments-ddl.sql";

	// The wiki owners table
	public static final String TABLE_WIKI_OWNERS					= "WIKI_OWNERS";
	public static final String COL_WIKI_ONWERS_OWNER_ID				= "OWNER_ID";
	public static final String COL_WIKI_ONWERS_OBJECT_TYPE			= "OWNER_OBJECT_TYPE";
	public static final String COL_WIKI_ONWERS_ROOT_WIKI_ID			= "ROOT_WIKI_ID";
	public static final String DDL_FILE_WIKI_ONWERS = "schema/WikiOwners-ddl.sql";
	
	/** V2 constants for wiki-related tables **/
	// The wiki page table
	public static final String V2_TABLE_WIKI_PAGE				= "V2_WIKI_PAGE";
	public static final String V2_COL_WIKI_ID					= "ID";
	public static final String V2_COL_WIKI_ETAG					= "ETAG";
	public static final String V2_COL_WIKI_TITLE				= "TITLE";
	public static final String V2_COL_WIKI_CREATED_ON			= "CREATED_ON";
	public static final String V2_COL_WIKI_CREATED_BY			= "CREATED_BY";
	public static final String V2_COL_WIKI_MODIFIED_ON			= "MODIFIED_ON";
	public static final String V2_COL_WIKI_MODIFIED_BY			= "MODIFIED_BY";
	public static final String V2_COL_WIKI_PARENT_ID			= "PARENT_ID";
	public static final String V2_COL_WIKI_ROOT_ID				= "ROOT_ID";
	public static final String V2_COL_WIKI_MARKDOWN_VERSION		= "MARKDOWN_VERSION";
	public static final String V2_DDL_FILE_WIKI_PAGE 			= "schema/v2-WikiPage-ddl.sql";
	
	// The wiki markdown table
	public static final String V2_TABLE_WIKI_MARKDOWN				= "V2_WIKI_MARKDOWN";
	public static final String V2_COL_WIKI_MARKDOWN_ID				= "WIKI_ID";
	public static final String V2_COL_WIKI_MARKDOWN_FILE_HANDLE_ID 	= "FILE_HANDLE_ID";
	public static final String V2_COL_WIKI_MARKDOWN_VERSION_NUM		= "MARKDOWN_VERSION";
	public static final String V2_COL_WIKI_MARKDOWN_MODIFIED_ON		= "MODIFIED_ON";
	public static final String V2_COL_WIKI_MARKDOWN_MODIFIED_BY		= "MODIFIED_BY";
	public static final String V2_COL_WIKI_MARKDOWN_ATTACHMENT_ID_LIST	= "ATTACHMENT_ID_LIST";
	public static final String V2_DDL_FILE_WIKI_MARKDOWN 			= "schema/v2-WikiMarkdown-ddl.sql";
	
	// The wiki attachments reservation table
	public static final String V2_TABLE_WIKI_ATTACHMENT_RESERVATION					= "V2_WIKI_ATTACHMENT_RESERVATION";
	public static final String V2_COL_WIKI_ATTACHMENT_RESERVATION_ID				= "WIKI_ID";
	public static final String V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID	= "FILE_HANDLE_ID";
	public static final String V2_COL_WIKI_ATTACHMENT_RESERVATION_TIMESTAMP 		= "TIME_STAMP";
	public static final String V2_DDL_FILE_WIKI_ATTATCHMENT_RESERVATION				= "schema/v2-WikiAttachmentReservation-ddl.sql";

	// The wiki owners table
	public static final String V2_TABLE_WIKI_OWNERS						= "V2_WIKI_OWNERS";
	public static final String V2_COL_WIKI_ONWERS_OWNER_ID				= "OWNER_ID";
	public static final String V2_COL_WIKI_ONWERS_OBJECT_TYPE			= "OWNER_OBJECT_TYPE";
	public static final String V2_COL_WIKI_ONWERS_ROOT_WIKI_ID			= "ROOT_WIKI_ID";
	public static final String V2_DDL_FILE_WIKI_ONWERS					= "schema/v2-WikiOwners-ddl.sql";
	
	// The alias used for the dataset table.
	public static final String NODE_ALIAS					= "nod";
	public static final String REVISION_ALIAS				= "rev";
	public static final String SORT_ALIAS					= "srt";
	public static final String EXPRESSION_ALIAS_PREFIX		= "exp";
	
	// The FAVORITE table
	public static final String TABLE_FAVORITE 					= "FAVORITE";
	public static final String COL_FAVORITE_PRINCIPAL_ID 		= "PRINCIPAL_ID";
	public static final String COL_FAVORITE_NODE_ID 			= "NODE_ID";
	public static final String COL_FAVORITE_CREATED_ON			= "CREATED_ON";
	public static final String COL_FAVORITE_ID					= "FAVORITE_ID";
	public static final String DDL_FILE_FAVORITE = "schema/Favorite-ddl.sql";

	// The DOI table
	public static final String TABLE_DOI                = "DOI";
	public static final String COL_DOI_ID               = "ID";
	public static final String COL_DOI_ETAG             = "ETAG";
	public static final String COL_DOI_DOI_STATUS       = "DOI_STATUS";
	public static final String COL_DOI_OBJECT_ID        = "OBJECT_ID";
	public static final String COL_DOI_OBJECT_TYPE      = "OBJECT_TYPE";
	public static final String COL_DOI_OBJECT_VERSION   = "OBJECT_VERSION";
	public static final String COL_DOI_CREATED_BY       = "CREATED_BY";
	public static final String COL_DOI_CREATED_ON       = "CREATED_ON";
	public static final String COL_DOI_UPDATED_ON       = "UPDATED_ON";
	public static final String DDL_FILE_DOI = "schema/Doi-ddl.sql";

	
	// SEMAPHORE
	public static final String TABLE_SEMAPHORE 			= "SEMAPHORE";
	public static final String COL_SEMAPHORE_KEY		= "SEM_KEY";
	public static final String COL_SEMAPHORE_TOKEN		= "TOKEN";
	public static final String COL_SEMAPHORE_EXPIRES	= "EXPIRATION";
	public static final String DDL_FILE_SEMAPHORE		= "schema/Semaphore-ddl.sql";
	
	// Upload status
	public static final String TABLE_UPLOAD_STATUS					= "UPLOAD_STATUS";
	public static final String COL_UPLOAD_STATUS_ID					= "ID";
	public static final String COL_UPLOAD_STATUS_STATE				= "STATE";
	public static final String COL_UPLOAD_STATUS_STARTED_BY			= "STARTED_BY";
	public static final String COL_UPLOAD_STATUS_STARTED_ON			= "STARTED_ON";
	public static final String COL_UPLOAD_STATUS_PERCENT_COMPLETE	= "PERCENT_COMPLETE";
	public static final String COL_UPLOAD_STATUS_ERROR_MESSAGE		= "ERROR_MESSAGE";
	public static final String COL_UPLOAD_STATUS_FILE_HANDLE_IDS	= "FILE_HANDLE_IDS";
	public static final String COL_UPLOAD_STATUS_RUNTIME_MS			= "RUNTIME_MS";
	public static final String DDL_UPLOAD_STATUS					= "schema/UploadDaemonStatus-ddl.sql";
	
	// Storage Quota
	public static final String TABLE_STORAGE_QUOTA            = "STORAGE_QUOTA";
	public static final String COL_STORAGE_QUOTA_OWNER_ID     = "OWNER_ID";
	public static final String COL_STORAGE_QUOTA_ETAG         = "ETAG";
	public static final String COL_STORAGE_QUOTA_QUOTA_IN_MB  = "QUOTA_IN_MB";
	public static final String DDL_FILE_STORAGE_QUOTA         ="schema/StorageQuota-ddl.sql";
	
	// Credential
	public static final String TABLE_CREDENTIAL             = "CREDENTIAL";
	public static final String COL_CREDENTIAL_PRINCIPAL_ID  = "PRINCIPAL_ID";
	public static final String COL_CREDENTIAL_VALIDATED_ON  = "VALIDATED_ON";
	public static final String COL_CREDENTIAL_SESSION_TOKEN = "SESSION_TOKEN";
	public static final String COL_CREDENTIAL_PASS_HASH     = "PASS_HASH";
	public static final String COL_CREDENTIAL_SECRET_KEY    = "SECRET_KEY";
	public static final String COL_CREDENTIAL_TOU           = "AGREES_TO_TERMS_OF_USE";
	public static final String DDL_CREDENTIAL               = "schema/Credential-ddl.sql";

	// The Team table
	public static final String TABLE_TEAM				= "TEAM";
	public static final String COL_TEAM_ID				= "ID";
	public static final String COL_TEAM_ETAG			= "ETAG";
	public static final String COL_TEAM_PROPERTIES		= "PROPERTIES";
	public static final String DDL_FILE_TEAM = "schema/Team-ddl.sql";

	
	// MembershipInvitation Table
	public static final String TABLE_MEMBERSHIP_INVITATION_SUBMISSION	= "MEMBERSHIP_INVITATION_SUBMISSION";
	public static final String COL_MEMBERSHIP_INVITATION_SUBMISSION_ID				= "ID";
	public static final String COL_MEMBERSHIP_INVITATION_SUBMISSION_CREATED_ON		= "CREATED_ON";
	public static final String COL_MEMBERSHIP_INVITATION_SUBMISSION_TEAM_ID			= "TEAM_ID";
	public static final String COL_MEMBERSHIP_INVITATION_SUBMISSION_EXPIRES_ON		= "EXPIRES_ON";
	public static final String COL_MEMBERSHIP_INVITATION_SUBMISSION_PROPERTIES		= "PROPERTIES";
	public static final String DDL_FILE_MEMBERSHIP_INVITATION_SUBMISSION	= "schema/MembershipInvitationSubmission-ddl.sql";
	
	// Membership-Invitee Table
	public static final String TABLE_MEMBERSHIP_INVITEE = "MEMBERSHIP_INVITEE";
	public static final String COL_MEMBERSHIP_INVITEE_INVITATION_ID		= "MEMBERSHIP_INVITATION_ID";
	public static final String COL_MEMBERSHIP_INVITEE_INVITEE_ID		= "INVITEE_ID";
	public static final String DDL_FILE_MEMBERSHIP_INVITEE_SUBMISSION	= "schema/Membership-Invitee-ddl.sql";
	
	// MembershipRequest Table
	public static final String TABLE_MEMBERSHIP_REQUEST_SUBMISSION	= "MEMBERSHIP_REQUEST_SUBMISSION";
	public static final String COL_MEMBERSHIP_REQUEST_SUBMISSION_ID				= "ID";
	public static final String COL_MEMBERSHIP_REQUEST_SUBMISSION_CREATED_ON		= "CREATED_ON";
	public static final String COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID		= "TEAM_ID";
	public static final String COL_MEMBERSHIP_REQUEST_SUBMISSION_USER_ID		= "USER_ID";
	public static final String COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON		= "EXPIRES_ON";
	public static final String COL_MEMBERSHIP_REQUEST_SUBMISSION_PROPERTIES		= "PROPERTIES";
	public static final String DDL_FILE_MEMBERSHIP_REQUEST_SUBMISSION	= "schema/MembershipRequestSubmission-ddl.sql";

	// This seems to be the name of the id column for all tables.
	public static final String COLUMN_ID		= "id";
	
	public static final String TYPE_COLUMN_NAME = "nodeType";
	
	public static final String AUTH_FILTER_ALIAS = "auth";
	
	// standard range parameters
	public static final String OFFSET_PARAM_NAME = "OFFSET";
	public static final String LIMIT_PARAM_NAME = "LIMIT";
	
	public static final String[] PRIMARY_FIELDS;
	
	static {
		Field[] fields = Node.class.getDeclaredFields();
		PRIMARY_FIELDS = new String[fields.length+1];
		for(int i=0; i<fields.length; i++){
			PRIMARY_FIELDS[i] = fields[i].getName();
		}
	}
	

	
	// This is the alias of the sub-query used for sorting on annotations.
	public static final String ANNOTATION_SORT_SUB_ALIAS 	= "assa";
	
	public static final String OPERATOR_SQL_EQUALS					= "=";
	public static final String OPERATOR_SQL_DOES_NOT_EQUAL			= "!=";
	public static final String OPERATOR_SQL_GREATER_THAN			= ">";
	public static final String OPERATOR_SQL_LESS_THAN				= "<";
	public static final String OPERATOR_SQL_GREATER_THAN_OR_EQUALS	= ">=";
	public static final String OPERATOR_SQL_LESS_THAN_OR_EQUALS		= "<=";
	public static final String OPERATOR_SQL_IN						= "in";
		
	public static final String INPUT_DATA_LAYER_DATASET_ID = "INPUT_LAYERS_ID_OWN";
	
	private static final Map<String, String> primaryFieldColumns;

	static{
		// Map column names to the field names
		// RELEASE_DATE,STATUS,PLATFORM,PROCESSING_FACILITY,QC_BY,QC_DATE,TISSUE_TYPE,TYPE,CREATION_DATE,DESCRIPTION,PREVIEW,PUBLICATION_DATE,RELEASE_NOTES
		primaryFieldColumns = new HashMap<String, String>();
		SqlConstants.addAllFields(Node.class, primaryFieldColumns);
		// This is a special case for nodes.
		primaryFieldColumns.put(NodeConstants.COL_PARENT_ID, "PARENT_ID_OID");
		
		// These will be deleted once we move to NodeDao
		SqlConstants.addAllFields(Study.class, primaryFieldColumns);
		SqlConstants.addAllFields(Data.class, primaryFieldColumns);
		primaryFieldColumns.put(NodeConstants.COL_PARENT_ID, "PARENT_ID");
		primaryFieldColumns.put("INPUT_LAYERS_ID_OWN", "INPUT_LAYERS_ID_OWN");
				
		
	}

	
	/**
	 * Add all of the fields for a given object.
	 * @param clazz
	 * @param map
	 */
	private static void addAllFields(Class clazz, Map<String, String> map){
		// This class generates the names the same way as datanucleus.
		BasicIdentifierFactory factory = new BasicIdentifierFactory();
		Field[] fields = clazz.getDeclaredFields();
		for(int i=0; i<fields.length; i++){
			if(!fields[i].isAccessible()){
				fields[i].setAccessible(true);
			}
			String fieldName = fields[i].getName();
			map.put(fieldName, factory.generateIdentifierNameForJavaName(fieldName));
		}
	}
	
	/**
	 * Get the database column name for a given primary field name.
	 * @param field
	 * @return
	 */
	public static String getColumnNameForPrimaryField(String field){
		if(field == null) return null;
		String column = primaryFieldColumns.get(field);
		if(column == null) throw new IllegalArgumentException("Unknown field: "+field);
		return column;
	}
		
	/**
	 * Translate an Comparator to SQL
	 * @param comp
	 * @return
	 */
	public static String getSqlForComparator(Comparator comp){
		if(Comparator.EQUALS == comp){
			return OPERATOR_SQL_EQUALS;
		}else if(Comparator.NOT_EQUALS == comp){
			return OPERATOR_SQL_DOES_NOT_EQUAL;
		}else if(Comparator.GREATER_THAN == comp){
			return OPERATOR_SQL_GREATER_THAN;
		}else if(Comparator.LESS_THAN == comp){
			return OPERATOR_SQL_LESS_THAN;
		}else if(Comparator.GREATER_THAN_OR_EQUALS == comp){
			return OPERATOR_SQL_GREATER_THAN_OR_EQUALS;
		}else if(Comparator.LESS_THAN_OR_EQUALS == comp){
			return OPERATOR_SQL_LESS_THAN_OR_EQUALS;
		}else if(Comparator.IN == comp){
			return OPERATOR_SQL_IN;
		}else{
			throw new IllegalArgumentException("Unsupported Comparator: "+comp);
		}
	}
	

}
