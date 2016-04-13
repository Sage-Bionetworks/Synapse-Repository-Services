package org.sagebionetworks.repo.model.query.jdo;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeConstants;
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
	public static final String COL_NODE_PROJECT_ID		= "PROJECT_ID";
	public static final String COL_NODE_NAME			= "NAME";
	public static final String COL_NODE_ANNOTATIONS		= "ANNOTATIONS_ID_OID";
	public static final String COL_NODE_DESCRIPTION 	= "DESCRIPTION";
	public static final String COL_NODE_ETAG 			= "ETAG";
	public static final String COL_NODE_CREATED_BY 		= "CREATED_BY";
	public static final String COL_NODE_CREATED_ON 		= "CREATED_ON";
	public static final String COL_NODE_TYPE			= "NODE_TYPE";
	public static final String COL_NODE_ACL				= "NODE_ACL";
	public static final String COL_CURRENT_REV			= "CURRENT_REV_NUM";
	public static final String COL_NODE_ALIAS 			= "ALIAS";
	public static final String DDL_FILE_NODE			="schema/Node-ddl.sql";
	
	// The Revision table
	public static final String TABLE_REVISION 				= "JDOREVISION";
	public static final String COL_REVISION_OWNER_NODE		= "OWNER_NODE_ID";
	public static final String COL_REVISION_NUMBER			= "NUMBER";
	public static final String COL_REVISION_ACTIVITY_ID		= "ACTIVITY_ID";
	public static final String COL_REVISION_LABEL			= "LABEL";
	public static final String COL_REVISION_COMMENT			= "COMMENT";
	public static final String COL_REVISION_ANNOS_BLOB		= "ANNOTATIONS";
	public static final String COL_REVISION_REF_BLOB		= "REFERENCE";
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
	public static final String COL_USER_PROFILE_PICTURE_ID		= "PICTURE_ID";
	public static final String COL_USER_PROFILE_EMAIL_NOTIFICATION	= "SEND_EMAIL_NOTIFICATION";
	public static final String COL_USER_PROFILE_FIRST_NAME		= "FIRST_NAME";
	public static final String COL_USER_PROFILE_LAST_NAME		= "LAST_NAME";
	public static final String DDL_FILE_USER_PROFILE			= "schema/UserProfile-ddl.sql";

	// The Project Settings table
	public static final String TABLE_PROJECT_SETTING			= "PROJECT_SETTING";
	public static final String COL_PROJECT_SETTING_ID			= "ID";
	public static final String COL_PROJECT_SETTING_PROJECT_ID	= "PROJECT_ID";
	public static final String COL_PROJECT_SETTING_ETAG			= "ETAG";
	public static final String COL_PROJECT_SETTING_TYPE			= "TYPE";
	public static final String COL_PROJECT_SETTING_DATA			= "DATA";

	// The Upload Destination Location table
	public static final String TABLE_STORAGE_LOCATION				= "STORAGE_LOCATION";
	public static final String COL_STORAGE_LOCATION_ID				= "ID";
	public static final String COL_STORAGE_LOCATION_DESCRIPTION		= "DESCRIPTION";
	public static final String COL_STORAGE_LOCATION_UPLOAD_TYPE		= "UPLOAD_TYPE";
	public static final String COL_STORAGE_LOCATION_ETAG			= "ETAG";
	public static final String COL_STORAGE_LOCATION_DATA			= "DATA";
	public static final String COL_STORAGE_LOCATION_CREATED_ON		= "CREATED_ON";
	public static final String COL_STORAGE_LOCATION_CREATED_BY		= "CREATED_BY";

	// The Project Stats table
	public static final String TABLE_PROJECT_STAT				= "PROJECT_STAT";
	public static final String COL_PROJECT_STAT_ID				= "ID";
	public static final String COL_PROJECT_STAT_PROJECT_ID		= "PROJECT_ID";
	public static final String COL_PROJECT_STAT_USER_ID			= "USER_ID";
	public static final String COL_PROJECT_STAT_LAST_ACCESSED	= "LAST_ACCESSED";
	public static final String COL_PROJECT_STAT_ETAG			= "ETAG";
	
	// Principal Prefix table
	public static final String TABLE_PRINCIPAL_PREFIX 				= "PRINCIPAL_PREFIX";
	public static final String COL_PRINCIPAL_PREFIX_TOKEN			= "TOKEN";
	public static final String COL_PRINCIPAL_PREFIX_PRINCIPAL_ID 	= "PRINCIPAL_ID";

	// The ACCESS_REQUIREMENT table
	public static final String TABLE_ACCESS_REQUIREMENT				= "ACCESS_REQUIREMENT";
	public static final String COL_ACCESS_REQUIREMENT_ID			= "ID";
	public static final String COL_ACCESS_REQUIREMENT_ETAG			= "ETAG";
	public static final String COL_ACCESS_REQUIREMENT_CREATED_BY	= "CREATED_BY";
	public static final String COL_ACCESS_REQUIREMENT_CREATED_ON	= "CREATED_ON";
	public static final String COL_ACCESS_REQUIREMENT_MODIFIED_BY	= "MODIFIED_BY";
	public static final String COL_ACCESS_REQUIREMENT_MODIFIED_ON	= "MODIFIED_ON";
	public static final String COL_ACCESS_REQUIREMENT_ACCESS_TYPE	= "ACCESS_TYPE";
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
	public static final String COL_CHANGES_USER_ID					= "USER_ID";
	public static final String DDL_CHANGES							= "schema/Changes-ddl.sql";
	
	// Sent messages
	public static final String TABLE_SENT_MESSAGES					= "SENT_MESSAGES";
	public static final String COL_SENT_MESSAGES_CHANGE_NUM			= "CHANGE_NUM";
	public static final String COL_SENT_MESSAGES_TIME_STAMP			= "TIME_STAMP";
	public static final String COL_SENT_MESSAGES_OBJECT_ID			= "OBJECT_ID";
	public static final String COL_SENT_MESSAGES_OBJECT_TYPE		= "OBJECT_TYPE";
	
 	// Processed messages
 	public static final String TABLE_PROCESSED_MESSAGES				= "PROCESSED_MESSAGES";
 	public static final String COL_PROCESSED_MESSAGES_CHANGE_NUM	= "CHANGE_NUM";
 	public static final String COL_PROCESSED_MESSAGES_TIME_STAMP	= "TIME_STAMP";
	public static final String COL_PROCESSED_MESSAGES_QUEUE_NAME	= "QUEUE_NAME";
 	public static final String DDL_PROCESSED_MESSAGES				= "schema/ProcessedMessages-ddl.sql";
 	
 	// Message content
 	public static final String TABLE_MESSAGE_CONTENT                = "MESSAGE_CONTENT";
 	public static final String COL_MESSAGE_CONTENT_ID               = "ID";
 	public static final String COL_MESSAGE_CONTENT_CREATED_BY       = "CREATED_BY";
 	public static final String COL_MESSAGE_CONTENT_FILE_HANDLE_ID   = "FILE_HANDLE_ID";
 	public static final String COL_MESSAGE_CONTENT_CREATED_ON       = "CREATED_ON";
 	public static final String COL_MESSAGE_CONTENT_ETAG             = "ETAG";
 	public static final String DDL_MESSAGE_CONTENT                  = "schema/MessageContent-ddl.sql";
 	
 	// Message to user
 	public static final String TABLE_MESSAGE_TO_USER                = "MESSAGE_TO_USER";
 	public static final String COL_MESSAGE_TO_USER_MESSAGE_ID       = "MESSAGE_ID";
 	public static final String COL_MESSAGE_TO_USER_ROOT_ID          = "ROOT_MESSAGE_ID";
 	public static final String COL_MESSAGE_TO_USER_REPLY_TO_ID      = "IN_REPLY_TO";
 	public static final String COL_MESSAGE_TO_USER_SUBJECT          = "SUBJECT";
 	public static final String COL_MESSAGE_TO_USER_SENT       		= "SENT";
 	public static final String COL_MESSAGE_TO_USER_TO	     		= "TO";
 	public static final String COL_MESSAGE_TO_USER_CC       		= "CC";
 	public static final String COL_MESSAGE_TO_USER_BCC       		= "BCC";
 	public static final String COL_MESSAGE_NOTIFICATIONS_ENDPOINT   = "NOTIFICATIONS_ENDPOINT";
 	public static final String DDL_MESSAGE_TO_USER                  = "schema/MessageToUser-ddl.sql";
 	
 	// Message recipient
 	public static final String TABLE_MESSAGE_RECIPIENT              = "MESSAGE_RECIPIENT";
 	public static final String COL_MESSAGE_RECIPIENT_MESSAGE_ID     = "MESSAGE_ID";
 	public static final String COL_MESSAGE_RECIPIENT_ID             = "RECIPIENT_ID";
 	public static final String DDL_MESSAGE_RECIPIENT                = "schema/MessageRecipient-ddl.sql";
 	
 	// Message status
 	public static final String TABLE_MESSAGE_STATUS                 = "MESSAGE_STATUS";
 	public static final String COL_MESSAGE_STATUS_MESSAGE_ID        = "MESSAGE_ID";
 	public static final String COL_MESSAGE_STATUS_RECIPIENT_ID      = "RECIPIENT_ID";
 	public static final String COL_MESSAGE_STATUS                   = "STATUS";
 	public static final String DDL_MESSAGE_STATUS                   = "schema/MessageStatus-ddl.sql";
 	
 	// Comment
 	public static final String TABLE_COMMENT                        = "COMMENT";
 	public static final String COL_COMMENT_MESSAGE_ID               = "MESSAGE_ID";
 	public static final String COL_COMMENT_OBJECT_TYPE              = "OBJECT_TYPE";
 	public static final String COL_COMMENT_OBJECT_ID                = "OBJECT_ID";
 	public static final String DDL_COMMENT                          = "schema/Comment-ddl.sql";

	// The file metadata table
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
	public static final String COL_FILES_STORAGE_LOCATION_ID		= "STORAGE_LOCATION_ID";
	public static final String DDL_FILES							= "schema/Files-ddl.sql";
	
	// multipart upload state
	public static final String TABLE_MULTIPART_UPLOAD				= "MULTIPART_UPLOAD";
	public static final String COL_MULTIPART_UPLOAD_ID				= "ID";
	public static final String COL_MULTIPART_REQUEST_HASH			= "REQUEST_HASH";
	public static final String COL_MULTIPART_UPLOAD_ETAG			= "ETAG";
	public static final String COL_MULTIPART_UPLOAD_REQUEST			= "REQUEST_BLOB";
	public static final String COL_MULTIPART_STARTED_BY				= "STARTED_BY";
	public static final String COL_MULTIPART_STARTED_ON				= "STARTED_ON";
	public static final String COL_MULTIPART_UPDATED_ON				= "UPDATED_ON";
	public static final String COL_MULTIPART_FILE_HANDLE_ID			= "FILE_HANDLE_ID";
	public static final String COL_MULTIPART_STATE					= "STATE";
	public static final String COL_MULTIPART_UPLOAD_TOKEN			= "UPLOAD_TOKEN";
	public static final String COL_MULTIPART_BUCKET					= "S3_BUCKET";
	public static final String COL_MULTIPART_KEY					= "S3_KEY";
	public static final String COL_MULTIPART_NUMBER_OF_PARTS		= "NUMBER_OF_PARTS";
	public static final String COL_MULTIPART_DDL					= "schema/MutipartUpload-ddl.sql";
	
	// multipar upload part state
	public static final String TABLE_MULTIPART_UPLOAD_PART_STATE	= "MULTIPART_UPLOAD_PART_STATE";
	public static final String COL_MULTIPART_PART_UPLOAD_ID			= "UPLOAD_ID";
	public static final String COL_MULTIPART_PART_NUMBER			= "PART_NUMBER";
	public static final String COL_MULTIPART_PART_MD5_HEX			= "PART_MD5_HEX";
	public static final String COL_MULTIPART_PART_ERROR_DETAILS		= "ERROR_DETAILS";
	public static final String COL_MULTIPART_UPLOAD_PART_STATE_DDL	= "schema/MultipartUploadPartState-ddl.sql";
	
	// 
	public static final String COL_STACK_STATUS_ID					= "ID";
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
	
	public static final String COL_OWNER_TYPE	 			= "OWNER_TYPE";	
	
	public static final String TABLE_ANNOTATION_TYPE		= "ANNOTATION_TYPE";

	public static final String TABLE_USER					= "JDOUSER";
	public static final String TABLE_USER_GROUP				= "JDOUSERGROUP";
	public static final String TABLE_USER_GROUP_USERS		= "JDOUSERGROUPUSERS";
	public static final String COL_USER_GROUP_ID			= "ID";
	public static final String COL_USER_GROUP_IS_INDIVIDUAL = "ISINDIVIDUAL";
	public static final String COL_USER_GROUP_E_TAG         = "ETAG";
	public static final String COL_USER_GROUP_CREATION_DATE = "CREATION_DATE";
	public static final String DDL_FILE_USER_GROUP			="schema/UserGroup-ddl.sql";
	
	// Principal headers table
	public static final String TABLE_PRINCIPAL_HEADER              = "PRINCIPAL_HEADER";
	public static final String COL_PRINCIPAL_HEADER_ID             = "PRINCIPAL_ID";
	public static final String COL_PRINCIPAL_HEADER_FRAGMENT       = "FRAGMENT";
	public static final String COL_PRINCIPAL_HEADER_SOUNDEX        = "SOUNDEX";
	public static final String COL_PRINCIPAL_HEADER_PRINCIPAL_TYPE = "PRINCIPAL_TYPE";
	public static final String COL_PRINCIPAL_HEADER_DOMAIN_TYPE    = "DOMAIN_TYPE";
    
    // The group members table
    public static final String TABLE_GROUP_MEMBERS         = "GROUP_MEMBERS";
    public static final String COL_GROUP_MEMBERS_GROUP_ID  = "GROUP_ID";
    public static final String COL_GROUP_MEMBERS_MEMBER_ID = "MEMBER_ID";
    public static final String DDL_FILE_GROUP_MEMBERS      = "schema/GroupMembers-ddl.sql";
    
    // The CHALLENGE table
    public static final String TABLE_CHALLENGE         				= "CHALLENGE";
    public static final String COL_CHALLENGE_ID 					= "ID";
    public static final String COL_CHALLENGE_ETAG					= "ETAG";
    public static final String COL_CHALLENGE_PROJECT_ID 			= "PROJECT_ID";
    public static final String COL_CHALLENGE_PARTICIPANT_TEAM_ID  	= "TEAM_ID";
	public static final String COL_CHALLENGE_SERIALIZED_ENTITY		= "SERIALIZED_ENTITY";

    // The CHALLENGE-TEAM table
    public static final String TABLE_CHALLENGE_TEAM        				= "CHALLENGE_TEAM";
    public static final String COL_CHALLENGE_TEAM_ID 					= "ID";
    public static final String COL_CHALLENGE_TEAM_ETAG					= "ETAG";
    public static final String COL_CHALLENGE_TEAM_TEAM_ID  				= "TEAM_ID";
    public static final String COL_CHALLENGE_TEAM_CHALLENGE_ID 			= "CHALLENGE_ID";
	public static final String COL_CHALLENGE_TEAM_SERIALIZED_ENTITY		= "SERIALIZED_ENTITY";

	public static final String TABLE_ACCESS_CONTROL_LIST  = "ACL";
	public static final String COL_ACL_ID          = "ID";
	public static final String COL_ACL_OWNER_ID           = "OWNER_ID";
	public static final String COL_ACL_OWNER_TYPE               = "OWNER_TYPE";
	public static final String COL_ACL_ETAG               = "ETAG";
	public static final String COL_ACL_CREATED_ON         = "CREATED_ON";
	public static final String DDL_FILE_ACL               = "schema/ACL-ddl.sql";
	
	// The resource access table
	public static final String TABLE_RESOURCE_ACCESS			= "JDORESOURCEACCESS";
	public static final String COL_RESOURCE_ACCESS_OWNER		= "OWNER_ID";
	public static final String COL_RESOURCE_ACCESS_GROUP_ID		= "GROUP_ID";
	public static final String COL_RESOURCE_ACCESS_ID			= "ID";
	public static final String DDL_FILE_RES_ACCESS				= "schema/ResourceAccess-ddl.sql";
	
	// The resource access join table
	// datanucleus doesn't seem to be respecting the join table name when creating the schema
	// so I've modified the string to match the generated name
	public static final String TABLE_RESOURCE_ACCESS_TYPE			= "JDORESOURCEACCESS_ACCESSTYPE"; 
	public static final String COL_RESOURCE_ACCESS_TYPE_OWNER		= "OWNER_ID";
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
	public static final String CONSTRAINT_UNIQUE_ALIAS = "NODE_UNIQUE_ALIAS";
	
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
	
	// Tracks changes messages that were broadcast by email.
	public static final String TABLE_BROADCAST_MESSAGE				= "MESSAGE_BROADCAST";
	public static final String COL_BROADCAST_MESSAGE_CHANGE_NUMBER	= "CHANGE_NUMBER";
	public static final String COL_BROADCAST_MESSAGE_SENT_ON		= "SENT_ON";
	public static final String DDL_BROADCAST_MESSAGE = "schema/BroadcastMessage-ddl.sql";
	
	// The column model table
	public static final String TABLE_COLUMN_MODEL			= "COLUMN_MODEL";
	public static final String COL_CM_ID					= "ID";
	public static final String COL_CM_NAME					= "NAME";
	public static final String COL_CM_HASH					= "HASH";
	public static final String COL_CM_BYTES					= "BYTES";
	public static final String DDL_COLUMN_MODEL = "schema/ColumnModel-ddl.sql";
	
	// This table controls IDs issued to TableEntities.
	public static final String TABLE_TABLE_ID_SEQUENCE		= "TABLE_ID_SEQUENCE";
	public static final String COL_ID_SEQUENCE_TABLE_ID		= "TABLE_ID";
	public static final String COL_ID_SEQUENCE_TABLE_ETAG	= "ETAG";
	public static final String COL_ID_SEQUENCE_VERSION		= "ROW_VERSION";
	public static final String COL_ID_SEQUENCE				= "SEQUENCE";
	public static final String DDL_TABLE_ID_SEQUENCE = "schema/TableIdSequence-ddl.sql";
	
	// The table row changes
	public static final String TABLE_ROW_CHANGE				= "TABLE_ROW_CHANGE";
	public static final String COL_TABLE_ROW_TABLE_ID		= "TABLE_ID";
	public static final String COL_TABLE_ROW_TABLE_ETAG		= "ETAG";
	public static final String COL_TABLE_ROW_VERSION		= "ROW_VERSION";
	public static final String COL_TABLE_ROW_COL_IDS		= "COLUMN_IDS";
	public static final String COL_TABLE_ROW_CREATED_BY		= "CREATED_BY";
	public static final String COL_TABLE_ROW_CREATED_ON		= "CREATED_ON";
	public static final String COL_TABLE_ROW_BUCKET			= "S3_BUCKET";
	public static final String COL_TABLE_ROW_KEY			= "S3_KEY";
	public static final String COL_TABLE_ROW_COUNT			= "ROW_COUNT";
	public static final String DDL_TABLE_ROW_CHANGE = "schema/TableRowChange-ddl.sql";
	
	// Tracks view scope.
	public static final String TABLE_VIEW_SCOPE				= "VIEW_SCOPE";
	public static final String COL_VIEW_SCOPE_ID			= "ID";
	public static final String COL_VIEW_SCOPE_VIEW_ID		= "VIEW_ID";
	public static final String COL_VIEW_SCOPE_CONTAINER_ID	= "CONTAINER_ID";
	public static final String DDL_VIEW_SCOPE = "schema/ViewScope-ddl.sql";
	
	
	public static final String TABLE_BOUND_COLUMN_OWNER		= "BOUND_COLUMN_OWNER";
	public static final String COL_BOUND_OWNER_OBJECT_ID	= "OBJECT_ID";
	public static final String COL_BOUND_OWNER_ETAG			= "ETAG";
	
	// Tracks the file handles associated with each table.
	public static final String TABLE_TABLE_FILE_ASSOCIATION 	= "TABLE_FILE_ASSOCIATION";
	public static final String COL_TABLE_FILE_ASSOC_TABLE_ID	= "TABLE_ID";
	public static final String COL_TABLE_FILE_ASSOC_FILE_ID		= "FILE_ID";
	
	// The bound column model table
	public static final String TABLE_BOUND_COLUMN			= "BOUND_COLUMN";
	public static final String COL_BOUND_CM_COLUMN_ID		= "COLUMN_ID";
	public static final String COL_BOUND_CM_OBJECT_ID		= "OBJECT_ID";
	public static final String COL_BOUND_CM_UPDATED_ON		= "UPDATED_ON";
	public static final String DDL_BOUND_COLUMN = "schema/BoundColumn-ddl.sql";
	
	// The bound column ordinal model table
	public static final String TABLE_BOUND_COLUMN_ORDINAL		= "BOUND_COLUMN_ORDINAL";
	public static final String COL_BOUND_CM_ORD_COLUMN_ID		= "COLUMN_ID";
	public static final String COL_BOUND_CM_ORD_OBJECT_ID		= "OBJECT_ID";
	public static final String COL_BOUND_CM_ORD_ORDINAL			= "ORDINAL";
	public static final String DDL_BOUND_COLUMN_ORDINAL = "schema/BoundColumnOrdinal-ddl.sql";
	
	// The bound column ordinal model table
	public static final String TABLE_STATUS							= "TABLE_STATUS";
	public static final String COL_TABLE_STATUS_ID					= "TABLE_ID";
	public static final String COL_TABLE_STATUS_STATE				= "STATE";
	public static final String COL_TABLE_STATUS_RESET_TOKEN			= "RESET_TOKEN";
	public static final String COL_TABLE_STATUS_STARTED_ON			= "STARTED_ON";
	public static final String COL_TABLE_STATUS_CHANGE_ON			= "CHANGED_ON";
	public static final String COL_TABLE_STATUS_PROGRESS_MESSAGE	= "PROGRESS_MESSAGE";
	public static final String COL_TABLE_STATUS_PROGRESS_CURRENT	= "PROGRESS_CURRENT";
	public static final String COL_TABLE_STATUS_PROGRESS_TOTAL		= "PROGRESS_TOTAL";
	public static final String COL_TABLE_STATUS_ERROR_MESSAGE		= "ERROR_MESSAGE";
	public static final String COL_TABLE_STATUS_ERROR_DETAILS		= "ERROR_DETAILS";
	public static final String COL_TABLE_STATUS_RUNTIME_MS			= "RUNTIME_MS";
	public static final String COL_TABLE_LAST_TABLE_CHANGE_ETAG		= "LAST_TABLE_CHANGE_ETAG";
	
	// Status table for Asynchronous jobs
	public static final String ASYNCH_JOB_STATUS					= "ASYNCH_JOB_STATUS";
	public static final String COL_ASYNCH_JOB_ID					= "JOB_ID";
	public static final String COL_ASYNCH_JOB_ETAG					= "ETAG";
	public static final String COL_ASYNCH_JOB_STATE					= "JOB_STATE";
	public static final String COL_ASYNCH_JOB_TYPE					= "JOB_TYPE";
	public static final String COL_ASYNCH_JOB_CANCELING				= "CANCELING";
	public static final String COL_ASYNCH_JOB_PROGRESS_CURRENT		= "PROGRESS_CURRENT";
	public static final String COL_ASYNCH_JOB_PROGRESS_TOTAL		= "PROGRESS_TOTAL";
	public static final String COL_ASYNCH_JOB_PROGRESS_MESSAGE		= "PROGRESS_MESSAGE";
	public static final String COL_ASYNCH_JOB_EXCEPTION				= "EXCEPTION";
	public static final String COL_ASYNCH_JOB_ERROR_MESSAGE			= "ERROR_MESSAGE";
	public static final String COL_ASYNCH_JOB_ERROR_DETAILS			= "ERROR_DETAILS";
	public static final String COL_ASYNCH_JOB_STARTED_ON			= "STARTED_ON";
	public static final String COL_ASYNCH_JOB_STARTED_BY			= "STARTED_BY";
	public static final String COL_ASYNCH_JOB_CHANGED_ON			= "CHANGED_ON";
	public static final String COL_ASYNCH_JOB_REQUEST_BODY			= "COMPRESSED_REQUEST_BODY";
	public static final String COL_ASYNCH_JOB_RESPONSE_BODY			= "COMPRESSED_RESPONSE_BODY";
	public static final String COL_ASYNCH_JOB_REQUEST_HASH			= "REQUEST_HASH";
	public static final String COL_ASYNCH_JOB_RUNTIME_MS			= "RUNTIME_MS";

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
	public static final String V2_COL_WIKI_ORDER_HINT			= "ORDER_HINT";
	public static final String V2_DDL_FILE_WIKI_PAGE 			= "schema/v2-WikiPage-ddl.sql";
	
	// The wiki markdown table
	public static final String V2_TABLE_WIKI_MARKDOWN				= "V2_WIKI_MARKDOWN";
	public static final String V2_COL_WIKI_MARKDOWN_ID				= "WIKI_ID";
	public static final String V2_COL_WIKI_MARKDOWN_FILE_HANDLE_ID 	= "FILE_HANDLE_ID";
	public static final String V2_COL_WIKI_MARKDOWN_VERSION_NUM		= "MARKDOWN_VERSION";
	public static final String V2_COL_WIKI_MARKDOWN_MODIFIED_ON		= "MODIFIED_ON";
	public static final String V2_COL_WIKI_MARKDOWN_MODIFIED_BY		= "MODIFIED_BY";
	public static final String V2_COL_WIKI_MARKDOWN_TITLE			= "TITLE";
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
	public static final String V2_COL_WIKI_OWNERS_ORDER_HINT			= "ORDER_HINT";
	public static final String V2_COL_WIKI_OWNERS_ETAG					= "ETAG";
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

	
	// Exclusive semaphore
	public static final String TABLE_EXCLUSIVE_SEMAPHORE 				= "EXCLUSIVE_SEMAPHORE";
	public static final String COL_EXCLUSIVE_SEMAPHORE_KEY				= "SEMAPHORE_KEY";
	public static final String COL_EXCLUSIVE_SEMAPHORE_PRECURSOR_TOKEN	= "PRECURSOR_TOKEN";
	public static final String COL_EXCLUSIVE_SEMAPHORE_LOCK_TOKEN		= "LOCK_TOKEN";
	public static final String COL_EXCLUSIVE_SEMAPHORE_EXPIRES			= "EXPIRATION";
	
	// Shared semaphore
	public static final String TABLE_SHARED_SEMAPHORE 				= "SHARED_SEMAPHORE";
	public static final String COL_SHARED_SEMAPHORE_KEY				= "SEMAPHORE_KEY";
	public static final String COL_SHARED_SEMAPHORE_LOCK_TOKEN		= "LOCK_TOKEN";
	public static final String COL_SHARED_SEMAPHORE_EXPIRES			= "EXPIRATION";

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
	public static final String COL_CREDENTIAL_PASS_HASH     = "PASS_HASH";
	public static final String COL_CREDENTIAL_SECRET_KEY    = "SECRET_KEY";
	public static final String DDL_CREDENTIAL               = "schema/Credential-ddl.sql";
	
	// Session token
	public static final String TABLE_SESSION_TOKEN             = "SESSION_TOKEN";
	public static final String COL_SESSION_TOKEN_PRINCIPAL_ID  = "PRINCIPAL_ID";
	public static final String COL_SESSION_TOKEN_VALIDATED_ON  = "VALIDATED_ON";
	public static final String COL_SESSION_TOKEN_DOMAIN  	   = "DOMAIN";
	public static final String COL_SESSION_TOKEN_SESSION_TOKEN = "SESSION_TOKEN";
	
	// Terms of use agreement
	public static final String TABLE_TERMS_OF_USE_AGREEMENT             = "TERMS_OF_USE_AGREEMENT";
	public static final String COL_TERMS_OF_USE_AGREEMENT_PRINCIPAL_ID  = "PRINCIPAL_ID";
	public static final String COL_TERMS_OF_USE_AGREEMENT_DOMAIN  		= "DOMAIN";
	public static final String COL_TERMS_OF_USE_AGREEMENT_AGREEMENT     = "AGREES_TO_TERMS_OF_USE";
	
	// The Team table
	public static final String TABLE_TEAM				= "TEAM";
	public static final String COL_TEAM_ID				= "ID";
	public static final String COL_TEAM_ETAG			= "ETAG";
	public static final String COL_TEAM_PROPERTIES		= "PROPERTIES";
	public static final String DDL_FILE_TEAM = "schema/Team-ddl.sql";
	
	// This table holds the binding of principal IDs to alias.
	// These alias can be used to lookup a principal.
	public static final String TABLE_PRINCIPAL_ALIAS 				= "PRINCIPAL_ALIAS";
	public static final String COL_PRINCIPAL_ALIAS_ID 				= "ID";
	public static final String COL_PRINCIPAL_ALIAS_ETAG				= "ETAG";
	public static final String COL_PRINCIPAL_ALIAS_PRINCIPAL_ID		= "PRINCIPAL_ID";
	public static final String COL_PRINCIPAL_ALIAS_UNIQUE			= "ALIAS_UNIQUE";
	public static final String COL_BOUND_ALIAS_DISPLAY				= "ALIAS_DISPLAY";
	public static final String COL_PRINCIPAL_ALIAS_TYPE				= "TYPE";
	public static final String COL_PRINCIPAL_ALIAS_IS_VALIDATED		= "IS_VALIDATED";
	public static final String CONSTRAINT_PRINCIPAL_ALIAS_UNIQUE 	= "UNIQUE KEY `PRINCIPAL_ALIAS_UNIQUE` (`"+COL_PRINCIPAL_ALIAS_UNIQUE+"`)";

	// this table tells which of a principal's aliases is their notification email
	public static final String TABLE_NOTIFICATION_EMAIL				= "NOTIFICATION_EMAIL";
	public static final String COL_NOTIFICATION_EMAIL_ID			= "ID";
	public static final String COL_NOTIFICATION_EMAIL_ETAG			= "ETAG";
	public static final String COL_NOTIFICATION_EMAIL_PRINCIPAL_ID	= "PRINCIPAL_ID";
	public static final String COL_NOTIFICATION_EMAIL_ALIAS_ID		= "ALIAS_ID";
	
	// MembershipInvitation Table
	public static final String TABLE_MEMBERSHIP_INVITATION_SUBMISSION	= "MEMBERSHIP_INVITATION_SUBMISSION";
	public static final String COL_MEMBERSHIP_INVITATION_SUBMISSION_ID				= "ID";
	public static final String COL_MEMBERSHIP_INVITATION_SUBMISSION_CREATED_ON		= "CREATED_ON";
	public static final String COL_MEMBERSHIP_INVITATION_SUBMISSION_TEAM_ID			= "TEAM_ID";
	public static final String COL_MEMBERSHIP_INVITATION_SUBMISSION_EXPIRES_ON		= "EXPIRES_ON";
	public static final String COL_MEMBERSHIP_INVITATION_SUBMISSION_INVITEE_ID		= "INVITEE_ID";
	public static final String COL_MEMBERSHIP_INVITATION_SUBMISSION_PROPERTIES		= "PROPERTIES";
	public static final String DDL_FILE_MEMBERSHIP_INVITATION_SUBMISSION	= "schema/MembershipInvitationSubmission-ddl.sql";
	
	// MembershipRequest Table
	public static final String TABLE_MEMBERSHIP_REQUEST_SUBMISSION	= "MEMBERSHIP_REQUEST_SUBMISSION";
	public static final String COL_MEMBERSHIP_REQUEST_SUBMISSION_ID				= "ID";
	public static final String COL_MEMBERSHIP_REQUEST_SUBMISSION_CREATED_ON		= "CREATED_ON";
	public static final String COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID		= "TEAM_ID";
	public static final String COL_MEMBERSHIP_REQUEST_SUBMISSION_USER_ID		= "USER_ID";
	public static final String COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON		= "EXPIRES_ON";
	public static final String COL_MEMBERSHIP_REQUEST_SUBMISSION_PROPERTIES		= "PROPERTIES";
	public static final String DDL_FILE_MEMBERSHIP_REQUEST_SUBMISSION	= "schema/MembershipRequestSubmission-ddl.sql";

	// QuizResponse data set
	public static final String TABLE_QUIZ_RESPONSE = "QUIZ_RESPONSE";
	public static final String COL_QUIZ_RESPONSE_ID = "ID";
	public static final String COL_QUIZ_RESPONSE_CREATED_BY = "CREATED_BY";
	public static final String COL_QUIZ_RESPONSE_CREATED_ON = "CREATED_ON";
	public static final String COL_QUIZ_RESPONSE_QUIZ_ID = "QUIZ_ID";
	public static final String COL_QUIZ_RESPONSE_SCORE = "SCORE";
	public static final String COL_QUIZ_RESPONSE_PASSED = "PASSED";
	public static final String COL_QUIZ_RESPONSE_SERIALIZED = "SERIALIZED";
	public static final String COL_QUIZ_RESPONSE_PASSING_RECORD = "PASSING_RECORD";
	
	public static final String TABLE_VERIFICATION_SUBMISSION = "VERIFICATION_SUBMISSION";
	public static final String COL_VERIFICATION_SUBMISSION_ID = "ID";
	public static final String COL_VERIFICATION_SUBMISSION_CREATED_BY = "CREATED_BY";
	public static final String COL_VERIFICATION_SUBMISSION_CREATED_ON = "CREATED_ON";
	public static final String COL_VERIFICATION_SUBMISSION_SERIALIZED = "SERIALIZED";
	public static final String FK_VERIFICATION_USER_GROUP_ID = "VERI_USER_GROUP_ID";

	
	public static final String TABLE_VERIFICATION_STATE = "VERIFICATION_STATE";
	public static final String COL_VERIFICATION_STATE_ID = "ID";
	public static final String COL_VERIFICATION_STATE_VERIFICATION_ID = "VERIFICATION_ID";
	public static final String COL_VERIFICATION_STATE_CREATED_BY = "CREATED_BY";
	public static final String COL_VERIFICATION_STATE_CREATED_ON = "CREATED_ON";
	public static final String COL_VERIFICATION_STATE_REASON = "REASON";
	public static final String COL_VERIFICATION_STATE_STATE = "STATE";
	public static final String FK_VERIFICATION_STATE_VERIFICATION_ID = "VERI_STATE_VERI_ID";
	public static final String FK_VERIFICATION_STATE_USER_ID = "VERI_STATE_USER_ID";
	
	public static final String TABLE_VERIFICATION_FILE = "VERIFICATION_FILE";
	public static final String COL_VERIFICATION_FILE_VERIFICATION_ID = "VERIFICATION_ID";
	public static final String COL_VERIFICATION_FILE_FILEHANDLEID = "FILE_HANDLE_ID";
	public static final String FK_VERIFICATION_FILE_FILE_ID = "VERI_FILE_FILE_ID";
	public static final String FK_VERIFICATION_FILE_VERIFICATION_ID = "VERI_FILE_VERI_ID";

	// Forum table
	public static final String TABLE_FORUM = "FORUM";
	public static final String COL_FORUM_ID = "ID";
	public static final String COL_FORUM_PROJECT_ID = "PROJECT_ID";
	public static final String COL_FORUM_ETAG = "ETAG";
	public static final String DDL_FORUM = "schema/Forum-ddl.sql";

	// Discussion Thread table
	public static final String TABLE_DISCUSSION_THREAD = "DISCUSSION_THREAD";
	public static final String COL_DISCUSSION_THREAD_ID = "ID";
	public static final String COL_DISCUSSION_THREAD_FORUM_ID = "FORUM_ID";
	public static final String COL_DISCUSSION_THREAD_TITLE = "TITLE";
	public static final String COL_DISCUSSION_THREAD_ETAG = "ETAG";
	public static final String COL_DISCUSSION_THREAD_CREATED_ON = "CREATED_ON";
	public static final String COL_DISCUSSION_THREAD_CREATED_BY = "CREATED_BY";
	public static final String COL_DISCUSSION_THREAD_MODIFIED_ON = "MODIFIED_ON";
	public static final String COL_DISCUSSION_THREAD_MESSAGE_KEY = "MESSAGE_KEY";
	public static final String COL_DISCUSSION_THREAD_IS_EDITED = "IS_EDITED";
	public static final String COL_DISCUSSION_THREAD_IS_DELETED = "IS_DELETED";
	public static final String DDL_DISCUSSION_THREAD = "schema/DiscussionThread-ddl.sql";

	// Discussion Thread Stats table
	public static final String TABLE_DISCUSSION_THREAD_STATS = "DISCUSSION_THREAD_STATS";
	public static final String COL_DISCUSSION_THREAD_STATS_THREAD_ID = "THREAD_ID";
	public static final String COL_DISCUSSION_THREAD_STATS_NUMBER_OF_VIEWS = "NUMBER_OF_VIEWS";
	public static final String COL_DISCUSSION_THREAD_STATS_NUMBER_OF_REPLIES = "NUMBER_OF_REPLIES";
	public static final String COL_DISCUSSION_THREAD_STATS_LAST_ACTIVITY = "LAST_ACTIVITY";
	public static final String COL_DISCUSSION_THREAD_STATS_ACTIVE_AUTHORS = "ACTIVE_AUTHORS";
	public static final String DDL_DISCUSSION_THREAD_STATS = "schema/DiscussionThreadStats-ddl.sql";

	// Discussion Thread View table
	public static final String TABLE_DISCUSSION_THREAD_VIEW = "DISCUSSION_THREAD_VIEW";
	public static final String COL_DISCUSSION_THREAD_VIEW_ID = "ID";
	public static final String COL_DISCUSSION_THREAD_VIEW_THREAD_ID = "THREAD_ID";
	public static final String COL_DISCUSSION_THREAD_VIEW_USER_ID = "USER_ID";
	public static final String DDL_DISCUSSION_THREAD_VIEW = "schema/DiscussionThreadView-ddl.sql";

	// Discussion Reply table
	public static final String TABLE_DISCUSSION_REPLY = "DISCUSSION_REPLY";
	public static final String COL_DISCUSSION_REPLY_ID = "ID";
	public static final String COL_DISCUSSION_REPLY_THREAD_ID = "THREAD_ID";
	public static final String COL_DISCUSSION_REPLY_ETAG = "ETAG";
	public static final String COL_DISCUSSION_REPLY_CREATED_ON = "CREATED_ON";
	public static final String COL_DISCUSSION_REPLY_CREATED_BY = "CREATED_BY";
	public static final String COL_DISCUSSION_REPLY_MODIFIED_ON = "MODIFIED_ON";
	public static final String COL_DISCUSSION_REPLY_MESSAGE_KEY = "MESSAGE_KEY";
	public static final String COL_DISCUSSION_REPLY_IS_EDITED = "IS_EDITED";
	public static final String COL_DISCUSSION_REPLY_IS_DELETED = "IS_DELETED";
	public static final String DDL_DISCUSSION_REPLY = "schema/DiscussionReply-ddl.sql";

	// Subscription table
	public static final String TABLE_SUBSCRIPTION = "SUBSCRIPTION";
	public static final String COL_SUBSCRIPTION_ID = "ID";
	public static final String COL_SUBSCRIPTION_SUBSCRIBER_ID = "SUBSCRIBER_ID";
	public static final String COL_SUBSCRIPTION_OBJECT_ID = "OBJECT_ID";
	public static final String COL_SUBSCRIPTION_OBJECT_TYPE = "OBJECT_TYPE";
	public static final String COL_SUBSCRIPTION_CREATED_ON = "CREATED_ON";
	public static final String DDL_SUBSCRIPTION = "schema/Subscription-ddl.sql";

	// This seems to be the name of the id column for all tables.
	public static final String COLUMN_ID		= "id";
	
	public static final String TYPE_COLUMN_NAME = "nodeType";
	
	public static final String AUTH_FILTER_ALIAS = "auth";
	
	// standard range parameters
	public static final String OFFSET_PARAM_NAME = "OFFSET";
	public static final String LIMIT_PARAM_NAME = "LIMIT";
	
	
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
	
	/**
	 * This is from the DB: SHOW VARIABLES LIKE 'max_allowed_packet';
	 */
	public static final int MAX_ALLOWED_PACKET_BYTES = 16777216;
	public static final int MAX_BYTES_PER_LONG_AS_STRING = 20*2; // 20 chars at 2 bytes per char.;
	public static final int MAX_LONGS_PER_IN_CLAUSE = MAX_ALLOWED_PACKET_BYTES/MAX_BYTES_PER_LONG_AS_STRING;

	static{
		// Map column names to the field names
		// RELEASE_DATE,STATUS,PLATFORM,PROCESSING_FACILITY,QC_BY,QC_DATE,TISSUE_TYPE,TYPE,CREATION_DATE,DESCRIPTION,PREVIEW,PUBLICATION_DATE,RELEASE_NOTES
		primaryFieldColumns = new HashMap<String, String>();
		SqlConstants.addAllFields(Node.class, primaryFieldColumns);
		// This is a special case for nodes.
		primaryFieldColumns.put(NodeConstants.COL_PARENT_ID, "PARENT_ID_OID");
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
