package org.sagebionetworks.repo.model.query.jdo;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.jdo.BasicIdentifierFactory;
 
@SuppressWarnings("rawtypes")
public class SqlConstants {
	
	public static final String COL_ID					= "ID";
	// Node table constants
	public static final String TABLE_NODE 				= "JDONODE";
	public static final String COL_NODE_ID				= "ID";
	public static final String COL_NODE_PARENT_ID		= "PARENT_ID";
	public static final String COL_NODE_NAME			= "NAME";
	public static final String COL_NODE_ANNOTATIONS		= "ANNOTATIONS_ID_OID";
	public static final String COL_NODE_ETAG 			= "ETAG";
	public static final String COL_NODE_CREATED_BY 		= "CREATED_BY";
	public static final String COL_NODE_CREATED_ON 		= "CREATED_ON";
	public static final String COL_NODE_MODIFIED_ON 	= "MODIFIED_ON";
	public static final String COL_NODE_TYPE			= "NODE_TYPE";
	public static final String COL_NODE_ACL				= "NODE_ACL";
	public static final String COL_NODE_CURRENT_REV			= "CURRENT_REV_NUM";
	public static final String COL_NODE_MAX_REV			= "MAX_REV_NUM";
	public static final String COL_NODE_ALIAS 			= "ALIAS";
	public static final String DDL_FILE_NODE			="schema/Node-ddl.sql";
	
	// The Revision table
	public static final String TABLE_REVISION 				= "JDOREVISION";
	public static final String COL_REVISION_OWNER_NODE		= "OWNER_NODE_ID";
	public static final String COL_REVISION_NUMBER			= "NUMBER";
	public static final String COL_REVISION_ACTIVITY_ID		= "ACTIVITY_ID";
	public static final String COL_REVISION_LABEL			= "LABEL";
	public static final String COL_REVISION_COMMENT			= "COMMENT";
	public static final String COL_REVISION_ENTITY_PROPERTY_ANNOTATIONS_BLOB = "ENTITY_PROPERTY_ANNOTATIONS";
	public static final String COL_REVISION_USER_ANNOS_JSON	= "USER_ANNOTATIONS";
	public static final String COL_REVISION_REF_BLOB		= "REFERENCE";
	public static final String COL_REVISION_MODIFIED_BY		= "MODIFIED_BY";
	public static final String COL_REVISION_MODIFIED_ON		= "MODIFIED_ON";
	public static final String COL_REVISION_FILE_HANDLE_ID	= "FILE_HANDLE_ID";
	public static final String COL_REVISION_COLUMN_MODEL_IDS= "COLUMN_MODEL_IDS";
	public static final String COL_REVISION_SCOPE_IDS		= "SCOPE_IDS";
	public static final String DDL_FILE_REVISION			="schema/Revision-ddl.sql";
	
	public static final String TABLE_STACK_STATUS		= "JDOSTACKSTATUS";
	
	
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
	public static final String COL_STORAGE_LOCATION_DATA_HASH		= "DATA_HASH";
	
	// form processing
	// FormGroup
	public static final String TABLE_FORM_GROUP 			= "FORM_GROUP";
	public static final String COL_FORM_GROUP_ID 			= "GROUP_ID";
	public static final String COL_FORM_GROUP_NAME 			= "NAME";
	public static final String COL_FORM_GROUP_CREATED_ON 	= "CREATED_ON";
	public static final String COL_FORM_GROUP_CREATED_BY 	= "CREATED_BY";
	public static final String DDL_FILE_FORM_GROUP 			= "schema/FormGroup-ddl.sql";
	
	// Form data
	public static final String TABLE_FORM_DATA 					= "FORM_DATA";
	public static final String COL_FORM_DATA_ID 				= "ID";
	public static final String COL_FORM_DATA_ETAG				= "ETAG";
	public static final String COL_FORM_DATA_NAME 				= "NAME";
	public static final String COL_FORM_DATA_CREATED_ON 		= "CREATED_ON";
	public static final String COL_FORM_DATA_CREATED_BY 		= "CREATED_BY";
	public static final String COL_FORM_DATA_MODIFIED_ON 		= "MODIFIED_ON";
	public static final String COL_FORM_DATA_GROUP_ID 			= "GROUP_ID";
	public static final String COL_FORM_DATA_FILE_ID 			= "FILE_HANDLE_ID";
	public static final String COL_FORM_DATA_SUBMITTED_ON  		= "SUBMITTED_ON";
	public static final String COL_FORM_DATA_REVIEWED_ON 		= "REVIEWED_ON";
	public static final String COL_FORM_DATA_REVIEWED_BY		= "REVIEWED_BY";
	public static final String COL_FORM_DATA_STATE	 			= "STATE";
	public static final String COL_FORM_DATA_REJECTION_MESSAGE 	= "REJECTION_MESSAGE";
	public static final String DDL_FILE_FORM_DATA 				= "schema/FormData-ddl.sql";
	
	// object-schema related tables
	public static final String TABLE_ORGANIZATION				= "ORGANIZATION";
	public static final String COL_ORGANIZATION_ID 				= "ID";
	public static final String COL_ORGANIZATION_NAME			= "NAME";
	public static final String COL_ORGANIZATION_CREATED_ON 		= "CREATED_ON";
	public static final String COL_ORGANIZATION_CREATED_BY 		= "CREATED_BY";
	public static final String DDL_FILE_ORGANIZATION 			= "schema/Organization-ddl.sql";
	
	// Json-Schema
	public static final String TABLE_JSON_SCHEMA			= "JSON_SCHEMA";
	public static final String COL_JSON_SCHEMA_ID			= "SCHEMA_ID";
	public static final String COL_JSON_SCHEMA_ORG_ID		= "ORGANIZATION_ID";
	public static final String COL_JSON_SCHEMA_NAME			= "SCHEMA_NAME";
	public static final String COL_JSON_SCHEMA_CREATED_BY	= "CREATED_BY";
	public static final String COL_JSON_SCHEMA_CREATED_ON	= "CREATED_ON";
	public static final String DDL_FILE_JSON_SCHEMA			= "schema/JsonSchema-ddl.sql";
	
	// Json-Schema blobs
	public static final String TABLE_JSON_SCHEMA_BLOB		= "JSON_SCHEMA_BLOB";
	public static final String COL_JSON_SCHEMA_BLOB_ID		= "BLOB_ID";
	public static final String COL_JSON_SCHEMA_BLOB_BLOB	= "JSON_BLOB";
	public static final String COL_JSON_SCHEMA_BLOB_SHA256	= "SHA_256_HEX";
	public static final String DDL_FILE_JSON_SCHEMA_BLOB	= "schema/JsonSchemaBlob-ddl.sql";
	
	// Json-Schema-Version
	public static final String TABLE_JSON_SCHEMA_VERSION 		= "JSON_SCHEMA_VERSION";
	public static final String COL_JSON_SCHEMA_VER_ID			= "VERSION_ID";
	public static final String COL_JSON_SCHEMA_VER_SCHEMA_ID 	= "SCHEMA_ID";
	public static final String COL_JSON_SCHEMA_VER_SEMANTIC		= "SEMANTIC_VERSION";
	public static final String COL_JSON_SCHEMA_VER_CREATED_BY	= "CREATED_BY";
	public static final String COL_JSON_SCHEMA_VER_CREATED_ON	= "CREATED_ON";
	public static final String COL_JSON_SCHEMA_VER_BLOB_ID		= "BLOB_ID";
	public static final String DDL_FILE_JSON_SCHEMA_VERSION		= "schema/JsonSchemaVersion-ddl.sql";
	
	// JSON schema latest version.
	public static final String TABLE_JSON_SCHEMA_LATEST_VERSION		= "JSON_SCHEMA_LATEST_VERSION";
	public static final String COL_JSON_SCHEMA_LATEST_VER_SCHEMA_ID	= "SCHEMA_ID";
	public static final String COL_JSON_SCHEMA_LATEST_VER_ETAG		= "ETAG";
	public static final String COL_JSON_SCHEMA_LATEST_VER_VER_ID	= "VERSION_ID";
	public static final String DDL_FILE_JSON_SCHEMA_LATEST_VERSION	="schema/JsonSchemaLatestVersion-ddl.sql";
	
	// Json-Schema-Dependency
	public static final String TABLE_JSON_SCHEMA_DEPENDENCY						= "JSON_SCHEMA_DEPENDENCY";
	public static final String COL_JSON_SCHEMA_DEPENDENCY_VERSION_ID			= "VERSION_ID";
	public static final String COL_JSON_SCHEMA_DEPEPNDENCY_DEPENDS_ON_SCHEMA_ID	= "DEPENDS_ON_SCHEMA_ID";
	public static final String COL_JSON_SCHEMA_DEPENDENCY_DEPENDS_ON_VERSION_ID	= "DEPENDS_ON_VERSION_ID";
	public static final String DDL_FILE_JSON_SCHEMA_DEPENDS						= "schema/JsonSchemaDependency-ddl.sql";
	
	// Json-Schema Object binding
	public static final String TABLE_JSON_SCHEMA_OBJECT_BINDING			= "JSON_SCHEMA_OBJECT_BINDING";
	public static final String COL_JSON_SCHEMA_BINDING_BIND_ID			= "BIND_ID";
	public static final String COL_JSON_SCHEMA_BINDING_SCHEMA_ID		= "SCHEMA_ID";
	public static final String COL_JSON_SCHEMA_BINDING_VERSION_ID		= "VERSION_ID";
	public static final String COL_JONS_SCHEMA_BINDING_OBJECT_ID		= "OBJECT_ID";
	public static final String COL_JSON_SCHEMA_BINDING_OBJECT_TYPE		= "OBJECT_TYPE";
	public static final String COL_JSON_SCHEMA_BINDING_CREATED_BY		= "CREATED_BY";
	public static final String COL_JSON_SCHEMA_BINDING_CREATED_ON		= "CREATED_ON";
	public static final String DDL_FILE_JSON_SCHEMA_BINDING				= "schema/JsonSchemaBindObject-ddl.sql";
	
	// Table to track JSON schema validation results.
	public static final String TABLE_SCHEMA_VALIDATION_RESULTS			= "JSON_SCHEMA_VALIDATION_RESULTS";
	public static final String COL_JSON_SCHEMA_VALIDATION_OBJECT_ID		= "OBJECT_ID";
	public static final String COL_JSON_SCHEMA_VALIDATION_OBJECT_TYPE	= "OBJECT_TYPE";
	public static final String COL_JSON_SCHEMA_VALIDATION_OBJECT_ETAG	= "OBJECT_ETAG";
	public static final String COL_JSON_SCHEMA_VALIDATION_SCHEMA_ID		= "SCHEMA_ID";
	public static final String COL_JSON_SCHEMA_VALIDATION_IS_VALID		= "IS_VALID";
	public static final String COL_JSON_SCHEMA_VALIDATION_VALIDATED_ON	= "VALIDATED_ON";
	public static final String COL_JSON_SCHEMA_VALIDATION_ERROR_MESSAGE = "ERROR_MESSAGE";
	public static final String COL_JSON_SCHEMA_VALIDATION_ALL_ERRORS	= "ALL_ERROR_MESSAGES";
	public static final String COL_JSON_SCHEMA_VALIDATION_EXCEPTION		= "VALIDATION_EXCEPTION";
	public static final String DDL_FILE_JSON_SCHEMA_VALIDATION_RESULTS	= "schema/JsonSchemaValidationResults-ddl.sql";
	
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
	public static final String TABLE_ACCESS_REQUIREMENT							= "ACCESS_REQUIREMENT";
	public static final String COL_ACCESS_REQUIREMENT_ID						= "ID";
	public static final String COL_ACCESS_REQUIREMENT_ETAG						= "ETAG";
	public static final String COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER	= "CURRENT_REV_NUM";
	public static final String COL_ACCESS_REQUIREMENT_CREATED_BY				= "CREATED_BY";
	public static final String COL_ACCESS_REQUIREMENT_CREATED_ON				= "CREATED_ON";
	public static final String COL_ACCESS_REQUIREMENT_ACCESS_TYPE				= "ACCESS_TYPE";
	public static final String COL_ACCESS_REQUIREMENT_CONCRETE_TYPE				= "CONCRETE_TYPE";
	public static final String DDL_FILE_ACCESS_REQUIREMENT						= "schema/AccessRequirement-ddl.sql";
	
	// The ACCESS_REQUIREMENT_REVISION table
	public static final String TABLE_ACCESS_REQUIREMENT_REVISION				= "ACCESS_REQUIREMENT_REVISION";
	public static final String COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID			= "OWNER_ID";
	public static final String COL_ACCESS_REQUIREMENT_REVISION_NUMBER			= "NUMBER";
	public static final String COL_ACCESS_REQUIREMENT_REVISION_MODIFIED_BY		= "MODIFIED_BY";
	public static final String COL_ACCESS_REQUIREMENT_REVISION_MODIFIED_ON		= "MODIFIED_ON";
	public static final String COL_ACCESS_REQUIREMENT_REVISION_SERIALIZED_ENTITY= "SERIALIZED_ENTITY";
	public static final String DDL_FILE_ACCESS_REQUIREMENT_REVISION				= "schema/AccessRequirementRevision-ddl.sql";	

	// The SUBJECT_ACCESS_REQUIREMENT table (a join table linking the ENTITY or EVALUTION and ACCESS_REQUIREMENT tables
	// !!! Note: The table name should be SUBJECT_ACCESS_REQUIREMENT, but migration issues prevent
	// !!!       us from doing that as this time.
	public static final String TABLE_SUBJECT_ACCESS_REQUIREMENT					= "NODE_ACCESS_REQUIREMENT";
	public static final String COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_ID		= "SUBJECT_ID";
	public static final String COL_SUBJECT_ACCESS_REQUIREMENT_SUBJECT_TYPE		= "SUBJECT_TYPE";
	public static final String COL_SUBJECT_ACCESS_REQUIREMENT_REQUIREMENT_ID	= "REQUIREMENT_ID";
	public static final String DDL_FILE_SUBJECT_ACCESS_REQUIREMENT			= "schema/SubjectAccessRequirement-ddl.sql";

	
	// the following are defined temporarily, for a 'bridge' migration table
	public static final String TABLE_NODE_ACCESS_REQUIREMENT		= "NODE_ACCESS_REQUIREMENT";
	public static final String COL_NODE_ACCESS_REQUIREMENT_NODE_ID			= "NODE_ID";
	public static final String COL_NODE_ACCESS_REQUIREMENT_NODE_TYPE			= "NODE_TYPE";
	public static final String COL_NODE_ACCESS_REQUIREMENT_REQUIREMENT_ID	= "REQUIREMENT_ID";
	public static final String DDL_FILE_NODE_ACCESS_REQUIREMENT			= "schema/NodeAccessRequirement-ddl.sql";
	
	// The ACCESS_APPROVAL table
	public static final String TABLE_ACCESS_APPROVAL						= "ACCESS_APPROVAL";
	public static final String COL_ACCESS_APPROVAL_ID						= "ID";
	public static final String COL_ACCESS_APPROVAL_ETAG						= "ETAG";
	public static final String COL_ACCESS_APPROVAL_CREATED_BY				= "CREATED_BY";
	public static final String COL_ACCESS_APPROVAL_CREATED_ON				= "CREATED_ON";
	public static final String COL_ACCESS_APPROVAL_MODIFIED_BY				= "MODIFIED_BY";
	public static final String COL_ACCESS_APPROVAL_MODIFIED_ON				= "MODIFIED_ON";
	public static final String COL_ACCESS_APPROVAL_REQUIREMENT_ID			= "REQUIREMENT_ID";
	public static final String COL_ACCESS_APPROVAL_REQUIREMENT_VERSION		= "REQUIREMENT_VERSION";
	public static final String COL_ACCESS_APPROVAL_SUBMITTER_ID				= "SUBMITTER_ID";
	public static final String COL_ACCESS_APPROVAL_ACCESSOR_ID				= "ACCESSOR_ID";
	public static final String COL_ACCESS_APPROVAL_EXPIRED_ON				= "EXPIRED_ON";
	public static final String COL_ACCESS_APPROVAL_STATE					= "STATE";
	public static final String DDL_FILE_ACCESS_APPROVAL						= "schema/AccessApproval-ddl.sql";
	
	// The CHANGES table
	public static final String TABLE_CHANGES						= "CHANGES";
	public static final String COL_CHANGES_CHANGE_NUM				= "CHANGE_NUM";
	public static final String COL_CHANGES_TIME_STAMP				= "TIME_STAMP";
	public static final String COL_CHANGES_OBJECT_ID				= "OBJECT_ID";
	public static final String COL_CHANGES_OBJECT_VERSION			= "OBJECT_VERSION";
	public static final String COL_CHANGES_OBJECT_TYPE				= "OBJECT_TYPE";
	public static final String COL_CHANGES_CHANGE_TYPE				= "CHANGE_TYPE";
	public static final String COL_CHANGES_USER_ID					= "USER_ID";
	public static final String DDL_CHANGES							= "schema/Changes-ddl.sql";
	
	// Sent messages
	public static final String TABLE_SENT_MESSAGES					= "SENT_MESSAGES";
	public static final String COL_SENT_MESSAGES_CHANGE_NUM			= "CHANGE_NUM";
	public static final String COL_SENT_MESSAGES_TIME_STAMP			= "TIME_STAMP";
	public static final String COL_SENT_MESSAGES_OBJECT_ID			= "OBJECT_ID";
	public static final String COL_SENT_MESSAGES_OBJECT_VERSION		= "OBJECT_VERSION";
	public static final String COL_SENT_MESSAGES_OBJECT_TYPE		= "OBJECT_TYPE";
	public static final String DDL_SENT_MESSAGE						= "schema/SentMessage-ddl.sql";
	
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
 	public static final String TABLE_MESSAGE_TO_USER                		= "MESSAGE_TO_USER";
 	public static final String COL_MESSAGE_TO_USER_MESSAGE_ID       		= "MESSAGE_ID";
 	public static final String COL_MESSAGE_TO_USER_ROOT_ID          		= "ROOT_MESSAGE_ID";
 	public static final String COL_MESSAGE_TO_USER_REPLY_TO_ID      		= "IN_REPLY_TO";
 	public static final String COL_MESSAGE_TO_USER_SUBJECT          		= "SUBJECT";
 	public static final String COL_MESSAGE_TO_USER_SENT       				= "SENT";
 	public static final String COL_MESSAGE_TO_USER_TO	     				= "TO";
 	public static final String COL_MESSAGE_TO_USER_CC       				= "CC";
 	public static final String COL_MESSAGE_TO_USER_BCC       				= "BCC";
 	public static final String COL_MESSAGE_NOTIFICATIONS_ENDPOINT   		= "NOTIFICATIONS_ENDPOINT";
 	public static final String COL_MESSAGE_PROFILE_SETTING_ENDPOINT 		= "PROFILE_SETTING_ENDPOINT";
 	public static final String COL_MESSAGE_WITH_UNSUBSCRIBE_LINK    		= "WITH_UNSUBSCRIBE_LINK";
 	public static final String COL_MESSAGE_WITH_PROFILE_SETTING_LINK		= "WITH_PROFILE_SETTING_LINK";
 	public static final String COL_MESSAGE_IS_NOTIFICATION_MESSAGE 			= "IS_NOTIFICATION_MESSAGE";
 	public static final String COL_MESSAGE_OVERRIDE_NOTIFICATION_SETTINGS 	= "OVERRIDE_NOTIFICATION_SETTINGS";
 	public static final String DDL_MESSAGE_TO_USER                  		= "schema/MessageToUser-ddl.sql";
 	
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
	public static final String COL_FILES_ENDPOINT					= "ENDPOINT";
	public static final String COL_FILES_IS_PREVIEW					= "IS_PREVIEW";
	public static final String DDL_FILES							= "schema/files/Files-ddl.sql";
	
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
	public static final String COL_MULTIPART_UPLOAD_TYPE			= "UPLOAD_TYPE";
	public static final String COL_MULTIPART_BUCKET					= "BUCKET";
	public static final String COL_MULTIPART_KEY					= "FILE_KEY";
	public static final String COL_MULTIPART_NUMBER_OF_PARTS		= "NUMBER_OF_PARTS";
	public static final String COL_MULTIPART_REQUEST_TYPE   		= "REQUEST_TYPE";
	public static final String COL_MULTIPART_PART_SIZE				= "PART_SIZE";
	public static final String COL_MULTIPART_SOURCE_FILE_HANDLE_ID	= "SOURCE_FILE_HANDLE_ID";
	public static final String COL_MULTIPART_SOURCE_FILE_ETAG		= "SOURCE_FILE_ETAG";
	public static final String COL_MULTIPART_DDL					= "schema/MultipartUpload-ddl.sql";
	
	// multipart upload part state
	public static final String TABLE_MULTIPART_UPLOAD_PART_STATE	= "MULTIPART_UPLOAD_PART_STATE";
	public static final String COL_MULTIPART_PART_UPLOAD_ID			= "UPLOAD_ID";
	public static final String COL_MULTIPART_PART_NUMBER			= "PART_NUMBER";
	public static final String COL_MULTIPART_PART_MD5_HEX			= "PART_MD5_HEX";
	public static final String COL_MULTIPART_PART_ERROR_DETAILS		= "ERROR_DETAILS";
	public static final String COL_MULTIPART_UPLOAD_PART_STATE_DDL	= "schema/MultipartUploadPartState-ddl.sql";

	// multipart upload composer part state
	public static final String TABLE_MULTIPART_UPLOAD_COMPOSER_PART_STATE = "MULTIPART_UPLOAD_COMPOSER_PART_STATE";
	public static final String COL_MULTIPART_COMPOSER_PART_UPLOAD_ID = "UPLOAD_ID";
	public static final String COL_MULTIPART_COMPOSER_PART_RANGE_LOWER_BOUND = "PART_RANGE_LOWER_BOUND";
	public static final String COL_MULTIPART_COMPOSER_PART_RANGE_UPPER_BOUND = "PART_RANGE_UPPER_BOUND";
	public static final String DDL_MULTIPART_COMPOSER_UPLOAD_PART_STATE = "schema/MultipartUploadComposerPartState-ddl.sql";

	// 
	public static final String COL_STACK_STATUS_ID					= "ID";
	public static final String COL_STACK_STATUS_STATUS				= "STATUS";
	public static final String COL_STACK_STATUS_CURRENT_MESSAGE		= "CURRENT_MESSAGE";
	public static final String COL_STACK_STATUS_PENDING_MESSAGE		= "PENDING_MESSAGE";
	public static final String DDL_FILE_STACK_STATUS				= "schema/StackStatus-ddl.sql";
	
	// The width of the string annotations value column
	public static final int STRING_ANNOTATIONS_VALUE_LENGTH = 500;
	
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
	public static final String COL_TRASH_CAN_PRIORITY_PURGE     = "PRIORITY_PURGE";
	public static final String COL_TRASH_CAN_ETAG               = "ETAG";
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
	
	// Table transaction tracking.
	public static final String TABLE_TABLE_TRANSACTION 	= "TABLE_TRANSACTION";
	public static final String COL_TABLE_TRX_ID		 	= "TRX_ID";
	public static final String COL_TABLE_TRX_TABLE_ID 	= "TABLE_ID";
	public static final String COL_TABLE_TRX_STARTED_BY = "STARTED_BY";
	public static final String COL_TABLE_TRX_STARTED_ON = "STARTED_ON";
	public static final String COL_TABLE_TRX_ETAG		= "ETAG";
	public static final String DDL_TABLE_TRANSACTION	= "schema/TableTransaction-ddl.sql";
	
	// Links table transactions to table version.
	public static final String TABLE_TABLE_TRX_TO_VERSION 	= "TABLE_TRX_TO_VERSION";
	public static final String COL_TABLE_TRX_TO_VER_TRX_ID	= "TRX_ID";
	public static final String COL_TABLE_TRX_TO_VER_VER_NUM	= "VERSION";
	public static final String DDL_TABLE_TRX_TO_VERSION	= "schema/TableTrxToVesion-ddl.sql";
	
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
	public static final String COL_TABLE_ROW_CREATED_BY		= "CREATED_BY";
	public static final String COL_TABLE_ROW_CREATED_ON		= "CREATED_ON";
	public static final String COL_TABLE_ROW_BUCKET			= "S3_BUCKET";
	public static final String COL_TABLE_ROW_KEY_NEW		= "S3_KEY";
	public static final String COL_TABLE_ROW_COUNT			= "ROW_COUNT";
	public static final String COL_TABLE_ROW_TYPE			= "CHANGE_TYPE";
	public static final String COL_TABLE_ROW_TRX_ID			= "TRX_ID";
	public static final String DDL_TABLE_ROW_CHANGE = "schema/TableRowChange-ddl.sql";
	
	// Tracks view scope.
	public static final String TABLE_VIEW_TYPE					= "VIEW_TYPE";
	public static final String COL_VIEW_TYPE_VIEW_ID			= "VIEW_ID";
	public static final String COL_VIEW_TYPE_VIEW_OBJECT_TYPE	= "VIEW_OBJECT_TYPE";
	public static final String COL_VIEW_TYPE_VIEW_TYPE_MASK		= "VIEW_TYPE_MASK";
	public static final String COL_VIEW_TYPE_ETAG				= "ETAG";
	public static final String DDL_VIEW_TYPE = "schema/ViewType-ddl.sql";
	
	// Tracks view scope.
	public static final String TABLE_VIEW_SCOPE				= "VIEW_SCOPE";
	public static final String COL_VIEW_SCOPE_VIEW_ID		= "VIEW_ID";
	public static final String COL_VIEW_SCOPE_CONTAINER_ID	= "CONTAINER_ID";
	public static final String DDL_VIEW_SCOPE = "schema/ViewScope-ddl.sql";
	
	// Metadata about view snapshots.
	public static final String TABLE_VIEW_SNAPSHOT 			= "VIEW_SNAPSHOT";
	public static final String COL_VIEW_SNAPSHOT_ID			= "SNAPSHOT_ID";
	public static final String COL_VIEW_SNAPSHOT_VIEW_ID	= "VIEW_ID";
	public static final String COL_VIEW_SNAPSHOT_VERSION	= "VERSION";
	public static final String COL_VIEW_SNAPSHOT_CREATED_BY	= "CREATED_BY";
	public static final String COL_VIEW_SNAPSHOT_CREATED_ON = "CREATED_ON";
	public static final String COL_VIEW_SNAPSHOT_BUCKET		= "BUCKET_NAME";
	public static final String COL_VIEW_SNAPSHOT_KEY		= "KEY";
	public static final String DDL_VIEW_SNAPSHOT = "schema/ViewSnapshot-ddl.sql";
		
	public static final String TABLE_BOUND_COLUMN_OWNER		= "BOUND_COLUMN_OWNER";
	public static final String COL_BOUND_OWNER_OBJECT_ID	= "OBJECT_ID";
	public static final String COL_BOUND_OWNER_ETAG			= "ETAG";
	
	// Tracks the file handles associated with each table.
	public static final String TABLE_TABLE_FILE_ASSOCIATION 	= "TABLE_FILE_ASSOCIATION";
	public static final String COL_TABLE_FILE_ASSOC_TABLE_ID	= "TABLE_ID";
	public static final String COL_TABLE_FILE_ASSOC_FILE_ID		= "FILE_ID";
		
	// The bound column ordinal model table
	public static final String TABLE_BOUND_COLUMN_ORDINAL		= "BOUND_COLUMN_ORDINAL";
	public static final String COL_BOUND_CM_ORD_COLUMN_ID		= "COLUMN_ID";
	public static final String COL_BOUND_CM_ORD_OBJECT_ID		= "OBJECT_ID";
	public static final String COL_BOUND_CM_ORD_OBJECT_VERSION	= "OBJECT_VERSION";
	public static final String COL_BOUND_CM_ORD_ORDINAL			= "ORDINAL";
	public static final String DDL_BOUND_COLUMN_ORDINAL = "schema/BoundColumnOrdinal-ddl.sql";
	
	// The bound column ordinal model table
	public static final String TABLE_STATUS							= "TABLE_STATUS";
	public static final String COL_TABLE_STATUS_ID					= "TABLE_ID";
	public static final String COL_TABLE_STATUS_VERSION				= "VERSION";
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
	public static final String DDL_TABLE_STATUE = "schema/TableStatus-ddl.sql";
	
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
	
	// Download list  - deprecated
	public static final String TABLE_DOWNLOAD_LIST 				= "DOWNLOAD_LIST";
	public static final String COL_DOWNLOAD_LIST_PRINCIPAL_ID 	= "PRINCIPAL_ID";
	public static final String COL_DOWNLOAD_LIST_UPDATED_ON		= "UPDATED_ON";
	public static final String COL_DOWNLOAD_LIST_ETAG			= "ETAG";
	public static final String DDL_DOWNLOAD_LIST				= "schema/DownloadList-ddl.sql";
	
	// Download list item. - deprecated
	public static final String TABLE_DOWNLOAD_LIST_ITEM							= "DOWNLOAD_LIST_ITEM";
	public static final String COL_DOWNLOAD_LIST_ITEM_PRINCIPAL_ID				= "PRINCIPAL_ID";
	public static final String COL_DOWNLOAD_LIST_ITEM_ASSOCIATED_OBJECT_ID 		= "ASSOCIATED_OBJECT_ID";
	public static final String COL_DOWNLOAD_LIST_ITEM_ASSOCIATED_OBJECT_TYPE 	= "ASSOCIATED_OBJECT_TYPE";
	public static final String COL_DOWNLOAD_LIST_ITEM_FILE_HANDLE_ID 			= "FILE_HANDLE_ID";
	public static final String DDL_DOWNLOAD_LIST_ITEM							= "schema/DownloadListItem-ddl.sql";
	
	// Download order - deprecated
	public static final String TABLE_DOWNLOAD_ORDER					= "DOWNLOAD_ORDER";
	public static final String COL_DOWNLOAD_ORDER_ID				= "ORDER_ID";
	public static final String COL_DOWNLOAD_ORDER_CREATED_BY		= "CREATED_BY";
	public static final String COL_DOWNLOAD_ORDER_CREATED_ON 		= "CREATED_ON";
	public static final String COL_DOWNLOAD_ORDER_FILE_NAME			= "FILE_NAME";
	public static final String COL_DOWNLOAD_ORDER_TOTAL_SIZE_BYTES 	= "TOTAL_SIZE_BYTES";
	public static final String COL_DOWNLOAD_ORDER_TOTAL_NUM_FILES	= "TOTAL_NUM_FILES";
	public static final String COL_DOWNLOAD_ORDER_FILES_BLOB		= "FILES_BLOB";
	public static final String DDL_DOWNLOAD_ORDER					= "schema/DownloadOrder-ddl.sql";
	
	// Download list
	public static final String TABLE_DOWNLOAD_LIST_V2 				= "DOWNLOAD_LIST_V2";
	public static final String COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID 	= "PRINCIPAL_ID";
	public static final String COL_DOWNLOAD_LIST_V2_UPDATED_ON		= "UPDATED_ON";
	public static final String COL_DOWNLOAD_LIST_V2_ETAG			= "ETAG";
	public static final String DDL_DOWNLOAD_V2_LIST					= "schema/DownloadList-V2-ddl.sql";
	
	// Download list item.
	public static final String TABLE_DOWNLOAD_LIST_ITEM_V2				= "DOWNLOAD_LIST_ITEM_V2";
	public static final String COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID	= "PRINCIPAL_ID";
	public static final String COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID		= "ENTITY_ID";
	public static final String COL_DOWNLOAD_LIST_ITEM_V2_VERION_NUMBER	= "VERSION_NUMBER";
	public static final String COL_DOWNLOAD_LIST_ITEM_V2_ADDED_ON		= "ADDED_ON";
	public static final String DDL_DOWNLOAD_LIST_ITEM_V2				= "schema/DownloadListItem-V2-ddl.sql";
	
	// Data type
	public static final String TABLE_DATA_TYPE				= "DATA_TYPE";
	public static final String COL_DATA_TYPE_ID 			= "ID";
	public static final String COL_DATA_TYPE_OBJECT_ID 		= "OBJECT_ID";
	public static final String COL_DATA_TYPE_OBJECT_TYPE 	= "OBJECT_TYPE";
	public static final String COL_DATA_TYPE_TYPE			= "DATA_TYPE";
	public static final String COL_DATA_TYPE_UPDATED_ON 	= "UPDATED_ON";
	public static final String COL_DATA_TYPE_UPDATED_BY 	= "UPDATED_BY";
	public static final String DDL_DATA_TYPE				= "schema/DataType-ddl.sql";
			
	
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
	public static final String COL_DOI_UPDATED_BY       = "UPDATED_BY";
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

	// Credential
	public static final String TABLE_CREDENTIAL             = "CREDENTIAL";
	public static final String COL_CREDENTIAL_PRINCIPAL_ID  = "PRINCIPAL_ID";
	public static final String COL_CREDENTIAL_PASS_HASH     = "PASS_HASH";
	public static final String COL_CREDENTIAL_SECRET_KEY    = "SECRET_KEY";
	public static final String DDL_CREDENTIAL               = "schema/Credential-ddl.sql";
	
	// Session token
	public static final String TABLE_SESSION_TOKEN             = "SESSION_TOKEN";
	public static final String COL_SESSION_TOKEN_PRINCIPAL_ID  = "PRINCIPAL_ID";
	public static final String COL_SESSION_TOKEN_SESSION_TOKEN = "SESSION_TOKEN";

	// AuthenticatedOn
	public static final String TABLE_AUTHENTICATED_ON					= "AUTHENTICATED_ON";
	public static final String COL_AUTHENTICATED_ON_PRINCIPAL_ID		= "PRINCIPAL_ID";
	public static final String COL_AUTHENTICATED_ON_ETAG				= "ETAG";
	public static final String COL_AUTHENTICATED_ON_AUTHENTICATED_ON	= "AUTHENTICATED_ON";
	public static final String DDL_AUTHENTICATED_ON = "schema/AuthenticatedOn-ddl.sql";
	
	// Terms of use agreement
	public static final String TABLE_TERMS_OF_USE_AGREEMENT             = "TERMS_OF_USE_AGREEMENT";
	public static final String COL_TERMS_OF_USE_AGREEMENT_PRINCIPAL_ID  = "PRINCIPAL_ID";
	public static final String COL_TERMS_OF_USE_AGREEMENT_AGREEMENT     = "AGREES_TO_TERMS_OF_USE";
	
	// The Team table
	public static final String TABLE_TEAM				= "TEAM";
	public static final String COL_TEAM_ID				= "ID";
	public static final String COL_TEAM_ETAG			= "ETAG";
	public static final String COL_TEAM_ICON			= "ICON";
	public static final String COL_TEAM_PROPERTIES		= "PROPERTIES";
	public static final String DDL_FILE_TEAM = "schema/Team-ddl.sql";
	
	// This table holds the binding of principal IDs to alias.
	// These alias can be used to lookup a principal.
	public static final String TABLE_PRINCIPAL_ALIAS 				= "PRINCIPAL_ALIAS";
	public static final String COL_PRINCIPAL_ALIAS_ID 				= "ID";
	public static final String COL_PRINCIPAL_ALIAS_ETAG				= "ETAG";
	public static final String COL_PRINCIPAL_ALIAS_PRINCIPAL_ID		= "PRINCIPAL_ID";
	public static final String COL_PRINCIPAL_ALIAS_UNIQUE			= "ALIAS_UNIQUE";
	public static final String COL_PRINCIPAL_ALIAS_DISPLAY          = "ALIAS_DISPLAY";
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
	public static final String TABLE_MEMBERSHIP_INVITATION = "MEMBERSHIP_INVITATION_SUBMISSION";
	public static final String COL_MEMBERSHIP_INVITATION_ID = "ID";
	public static final String COL_MEMBERSHIP_INVITATION_ETAG = "ETAG";
	public static final String COL_MEMBERSHIP_INVITATION_CREATED_ON = "CREATED_ON";
	public static final String COL_MEMBERSHIP_INVITATION_TEAM_ID = "TEAM_ID";
	public static final String COL_MEMBERSHIP_INVITATION_EXPIRES_ON = "EXPIRES_ON";
	public static final String COL_MEMBERSHIP_INVITATION_INVITEE_ID = "INVITEE_ID";
	public static final String COL_MEMBERSHIP_INVITATION_INVITEE_EMAIL = "INVITEE_EMAIL";
	public static final String COL_MEMBERSHIP_INVITATION_PROPERTIES = "PROPERTIES";
	public static final String DDL_FILE_MEMBERSHIP_INVITATION = "schema/MembershipInvitation-ddl.sql";
	
	// MembershipRequest Table
	public static final String TABLE_MEMBERSHIP_REQUEST = "MEMBERSHIP_REQUEST_SUBMISSION";
	public static final String COL_MEMBERSHIP_REQUEST_ID = "ID";
	public static final String COL_MEMBERSHIP_REQUEST_CREATED_ON = "CREATED_ON";
	public static final String COL_MEMBERSHIP_REQUEST_TEAM_ID = "TEAM_ID";
	public static final String COL_MEMBERSHIP_REQUEST_USER_ID = "USER_ID";
	public static final String COL_MEMBERSHIP_REQUEST_EXPIRES_ON = "EXPIRES_ON";
	public static final String COL_MEMBERSHIP_REQUEST_PROPERTIES = "PROPERTIES";
	public static final String DDL_FILE_MEMBERSHIP_REQUEST = "schema/MembershipRequest-ddl.sql";

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
	public static final String COL_VERIFICATION_SUBMISSION_ETAG = "ETAG";
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
	public static final String COL_VERIFICATION_STATE_NOTES = "NOTES";
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
	public static final String COL_DISCUSSION_THREAD_IS_PINNED = "IS_PINNED";
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

	// Discussion Thread Entity Reference table
	public static final String TABLE_DISCUSSION_THREAD_ENTITY_REFERENCE = "DISCUSSION_THREAD_ENTITY_REFERENCE";
	public static final String COL_DISCUSSION_THREAD_ENTITY_REFERENCE_THREAD_ID = "THREAD_ID";
	public static final String COL_DISCUSSION_THREAD_ENTITY_REFERENCE_ENTITY_ID = "ENTITY_ID";
	public static final String DDL_DISCUSSION_THREAD_ENTITY_REFERENCE = "schema/DiscussionThreadEntityReference-ddl.sql";

	// Subscription table
	public static final String TABLE_SUBSCRIPTION = "SUBSCRIPTION";
	public static final String COL_SUBSCRIPTION_ID = "ID";
	public static final String COL_SUBSCRIPTION_SUBSCRIBER_ID = "SUBSCRIBER_ID";
	public static final String COL_SUBSCRIPTION_OBJECT_ID = "OBJECT_ID";
	public static final String COL_SUBSCRIPTION_OBJECT_TYPE = "OBJECT_TYPE";
	public static final String COL_SUBSCRIPTION_CREATED_ON = "CREATED_ON";
	public static final String DDL_SUBSCRIPTION = "schema/Subscription-ddl.sql";

	// AuthenticationReceipt table
	public static final String TABLE_AUTHENTICATION_RECEIPT = "AUTHENTICATION_RECEIPT";
	public static final String COL_AUTHENTICATION_RECEIPT_ID = "ID";
	public static final String COL_AUTHENTICATION_RECEIPT_USER_ID = "USER_ID";
	public static final String COL_AUTHENTICATION_RECEIPT_RECEIPT = "RECEIPT";
	public static final String COL_AUTHENTICATION_RECEIPT_EXPIRATION = "EXPIRATION";
	public static final String DDL_AUTHENTICATION_RECEIPT = "schema/AuthenticationReceipt-ddl.sql";
	
	// DockerRepsitoryName table
	public static final String TABLE_DOCKER_REPOSITORY_NAME = "DOCKER_REPOSITORY_NAME";
	public static final String COL_DOCKER_REPOSITORY_OWNER_ID = "OWNER_ID";
	public static final String COL_DOCKER_REPOSITORY_NAME = "REPOSITORY_NAME";
	
	// DockerCommit table
	public static final String TABLE_DOCKER_COMMIT = "DOCKER_COMMIT";
	public static final String COL_DOCKER_COMMIT_ID = "ID";
	public static final String COL_DOCKER_COMMIT_OWNER_ID = "OWNER_ID";
	public static final String COL_DOCKER_COMMIT_TAG = "TAG";
	public static final String COL_DOCKER_COMMIT_DIGEST = "DIGEST";
	public static final String COL_DOCKER_COMMIT_CREATED_ON = "CREATED_ON";
	
	//ThrottleRules table
	public static final String TABLE_THROTTLE_RULES = "THROTTLE_RULES";
	public static final String COL_THROTTLE_RULES_ID = "THROTTLE_ID";
	public static final String COL_THROTTLE_RULES_NORMALIZED_URI = "NORMALIZED_PATH";
	public static final String COL_THROTTLE_RULES_MAX_CALLS = "MAX_CALLS_PER_USER_PER_PERIOD";
	public static final String COL_THROTTLE_RULES_CALL_PERIOD = "PERIOD_IN_SECONDS";
	public static final String COL_THROTTLE_RULES_MODIFIED_ON = "MODIFIED_ON";
	public static final String DDL_THROTTLE_RULES = "schema/ThrottleRules-ddl.sql";

	//Unsucessful login lockout table
	public static final String DDL_UNSUCCESSFUL_LOGIN_LOCKOUT = "schema/UnsuccessfulLoginLockout-ddl.sql";
	public static final String TABLE_UNSUCCESSFUL_LOGIN_LOCKOUT = "UNSUCCESSFUL_LOGIN_LOCKOUT";
	public static final String COL_UNSUCCESSFUL_LOGIN_KEY = "USER_ID";
	public static final String COL_UNSUCCESSFUL_LOGIN_COUNT = "UNSUCCESSFUL_LOGIN_COUNT";
	public static final String COL_UNSUCCESSFUL_LOGIN_LOCKOUT_EXPIRATION_TIMESTAMP_MILLIS = "LOCKOUT_EXPIRATION";

	// ResearchProject
	public static final String DDL_RESEARCH_PROJECT = 							"schema/ResearchProject-ddl.sql";
	public static final String TABLE_RESEARCH_PROJECT = 						"RESEARCH_PROJECT";
	public static final String COL_RESEARCH_PROJECT_ID = 						"ID";
	public static final String COL_RESEARCH_PROJECT_ACCESS_REQUIREMENT_ID = 	"ACCESS_REQUIREMENT_ID";
	public static final String COL_RESEARCH_PROJECT_CREATED_BY = 				"CREATED_BY";
	public static final String COL_RESEARCH_PROJECT_CREATED_ON = 				"CREATED_ON";
	public static final String COL_RESEARCH_PROJECT_MODIFIED_BY = 				"MODIFIED_BY";
	public static final String COL_RESEARCH_PROJECT_MODIFIED_ON = 				"MODIFIED_ON";
	public static final String COL_RESEARCH_PROJECT_ETAG = 						"ETAG";
	public static final String COL_RESEARCH_PROJECT_PROJECT_LEAD = 				"PROJECT_LEAD";
	public static final String COL_RESEARCH_PROJECT_INSTITUTION = 				"INSTITUTION";
	public static final String COL_RESEARCH_PROJECT_IDU = 						"IDU";

	// DataAccessRequest
	public static final String DDL_DATA_ACCESS_REQUEST = 							"schema/DataAccessRequest-ddl.sql";
	public static final String TABLE_DATA_ACCESS_REQUEST = 							"DATA_ACCESS_REQUEST";
	public static final String COL_DATA_ACCESS_REQUEST_ID = 						"ID";
	public static final String COL_DATA_ACCESS_REQUEST_ACCESS_REQUIREMENT_ID = 		"ACCESS_REQUIREMENT_ID";
	public static final String COL_DATA_ACCESS_REQUEST_RESEARCH_PROJECT_ID = 		"RESEARCH_PROJECT_ID";
	public static final String COL_DATA_ACCESS_REQUEST_CREATED_BY = 				"CREATED_BY";
	public static final String COL_DATA_ACCESS_REQUEST_CREATED_ON = 				"CREATED_ON";
	public static final String COL_DATA_ACCESS_REQUEST_MODIFIED_BY = 				"MODIFIED_BY";
	public static final String COL_DATA_ACCESS_REQUEST_MODIFIED_ON = 				"MODIFIED_ON";
	public static final String COL_DATA_ACCESS_REQUEST_ETAG = 						"ETAG";
	public static final String COL_DATA_ACCESS_REQUEST_REQUEST_SERIALIZED = 		"REQUEST_SERIALIZED";

	// DataAccessSubmission
	public static final String DDL_DATA_ACCESS_SUBMISSION = 						"schema/DataAccessSubmission-ddl.sql";
	public static final String TABLE_DATA_ACCESS_SUBMISSION = 						"DATA_ACCESS_SUBMISSION";
	public static final String COL_DATA_ACCESS_SUBMISSION_ID = 						"ID";
	public static final String COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID = 	"ACCESS_REQUIREMENT_ID";
	public static final String COL_DATA_ACCESS_SUBMISSION_DATA_ACCESS_REQUEST_ID = 	"DATA_ACCESS_REQUEST_ID";
	public static final String COL_DATA_ACCESS_SUBMISSION_CREATED_BY = 				"CREATED_BY";
	public static final String COL_DATA_ACCESS_SUBMISSION_CREATED_ON = 				"CREATED_ON";
	public static final String COL_DATA_ACCESS_SUBMISSION_ETAG = 					"ETAG";
	public static final String COL_DATA_ACCESS_SUBMISSION_SUBMISSION_SERIALIZED = 	"SUBMISSION_SERIALIZED";
	public static final String COL_DATA_ACCESS_SUBMISSION_RESEARCH_PROJECT_ID = 	"RESEARCH_PROJECT_ID";

	// DataAccessSubmissionStatus
	public static final String DDL_DATA_ACCESS_SUBMISSION_STATUS = 					"schema/DataAccessSubmissionStatus-ddl.sql";
	public static final String TABLE_DATA_ACCESS_SUBMISSION_STATUS = 				"DATA_ACCESS_SUBMISSION_STATUS";
	public static final String COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID = 	"SUBMISSION_ID";
	public static final String COL_DATA_ACCESS_SUBMISSION_STATUS_CREATED_BY = 		"CREATED_BY";
	public static final String COL_DATA_ACCESS_SUBMISSION_STATUS_CREATED_ON = 		"CREATED_ON";
	public static final String COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_BY = 		"MODIFIED_BY";
	public static final String COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON = 		"MODIFIED_ON";
	public static final String COL_DATA_ACCESS_SUBMISSION_STATUS_STATE = 			"STATE";
	public static final String COL_DATA_ACCESS_SUBMISSION_STATUS_REASON = 			"REASON";

	// DataAccessSubmissionSubmitter
	public static final String DDL_DATA_ACCESS_SUBMISSION_SUBMITTER = 							"schema/DataAccessSubmissionSubmitter-ddl.sql";
	public static final String TABLE_DATA_ACCESS_SUBMISSION_SUBMITTER = 						"DATA_ACCESS_SUBMISSION_SUBMITTER";
	public static final String COL_DATA_ACCESS_SUBMISSION_SUBMITTER_ID = 						"ID";
	public static final String COL_DATA_ACCESS_SUBMISSION_SUBMITTER_SUBMITTER_ID = 				"SUBMITTER_ID";
	public static final String COL_DATA_ACCESS_SUBMISSION_SUBMITTER_CURRENT_SUBMISSION_ID = 	"CURRENT_SUBMISSION_ID";
	public static final String COL_DATA_ACCESS_SUBMISSION_SUBMITTER_ACCESS_REQUIREMENT_ID = 	"ACCESS_REQUIREMENT_ID";
	public static final String COL_DATA_ACCESS_SUBMISSION_SUBMITTER_ETAG = 						"ETAG";
	
	// OAuth related tables, columns etc,
	public static final String DDL_OAUTH_SECTOR_IDENTIFIER = 							"schema/OAuthSectorIdentifier-ddl.sql";
	public static final String TABLE_OAUTH_SECTOR_IDENTIFIER = 							"OAUTH_SECTOR_IDENTIFIER";
	public static final String COL_OAUTH_SECTOR_IDENTIFIER_ID =							"ID";
	public static final String COL_OAUTH_SECTOR_IDENTIFIER_URI = 						"URI";
	public static final String COL_OAUTH_SECTOR_IDENTIFIER_SECRET = 					"SECRET";
	public static final String COL_OAUTH_SECTOR_IDENTIFIER_CREATED_BY = 				"CREATED_BY";
	public static final String COL_OAUTH_SECTOR_IDENTIFIER_CREATED_ON = 				"CREATED_ON";
	
	public static final String DDL_OAUTH_CLIENT = 										"schema/OAuthClient-ddl.sql";
	public static final String TABLE_OAUTH_CLIENT = 									"OAUTH_CLIENT";
	public static final String COL_OAUTH_CLIENT_ID = 									"ID";
	public static final String COL_OAUTH_CLIENT_NAME = 									"NAME";
	public static final String COL_OAUTH_CLIENT_SECRET_HASH = 							"SECRET_HASH";
	public static final String COL_OAUTH_CLIENT_PROPERTIES = 							"PROPERTIES";
	public static final String COL_OAUTH_CLIENT_ETAG = 									"ETAG";
	public static final String COL_OAUTH_CLIENT_CREATED_BY = 							"CREATED_BY";
	public static final String COL_OAUTH_CLIENT_CREATED_ON = 							"CREATED_ON";
	public static final String COL_OAUTH_CLIENT_MODIFIED_ON = 							"MODIFIED_ON";
	public static final String COL_OAUTH_CLIENT_SECTOR_IDENTIFIER_URI = 				"OAUTH_SECTOR_IDENTIFIER_URI";
	public static final String COL_OAUTH_CLIENT_IS_VERIFIED = 							"IS_VERIFIED";

	public static final String DDL_AUTHORIZATION_CONSENT = 								"schema/AuthorizationConsent-ddl.sql";
	public static final String TABLE_AUTHORIZATION_CONSENT = 							"AUTHORIZATION_CONSENT";
	public static final String COL_AUTHORIZATION_CONSENT_ID = 							"ID";
	public static final String COL_AUTHORIZATION_CONSENT_ETAG = 						"ETAG";
	public static final String COL_AUTHORIZATION_CONSENT_USER_ID = 						"USER_ID";
	public static final String COL_AUTHORIZATION_CONSENT_CLIENT_ID = 					"CLIENT_ID";
	public static final String COL_AUTHORIZATION_CONSENT_SCOPE_HASH = 					"SCOPE_HASH";
	public static final String COL_AUTHORIZATION_CONSENT_GRANTED_ON = 					"GRANTED_ON";

	public static final String DDL_OAUTH_REFRESH_TOKEN = 								"schema/OAuthRefreshToken-ddl.sql";
	public static final String TABLE_OAUTH_REFRESH_TOKEN = 								"OAUTH_REFRESH_TOKEN";
	public static final String COL_OAUTH_REFRESH_TOKEN_ID =								"ID";
	public static final String COL_OAUTH_REFRESH_TOKEN_HASH =							"TOKEN_HASH";
	public static final String COL_OAUTH_REFRESH_TOKEN_NAME =							"NAME";
	public static final String COL_OAUTH_REFRESH_TOKEN_PRINCIPAL_ID =					"PRINCIPAL_ID";
	public static final String COL_OAUTH_REFRESH_TOKEN_CLIENT_ID =						"CLIENT_ID";
	public static final String COL_OAUTH_REFRESH_TOKEN_SCOPES =							"SCOPES";
	public static final String COL_OAUTH_REFRESH_TOKEN_CLAIMS =							"CLAIMS";
	public static final String COL_OAUTH_REFRESH_TOKEN_LAST_USED =						"LAST_USED";
	public static final String COL_OAUTH_REFRESH_TOKEN_CREATED_ON =						"CREATED_ON";
	public static final String COL_OAUTH_REFRESH_TOKEN_MODIFIED_ON =					"MODIFIED_ON";
	public static final String COL_OAUTH_REFRESH_TOKEN_ETAG =							"ETAG";

	public static final String DDL_PERSONAL_ACCESS_TOKEN = 								"schema/PersonalAccessToken-ddl.sql";
	public static final String TABLE_PERSONAL_ACCESS_TOKEN = 							"PERSONAL_ACCESS_TOKEN";
	public static final String COL_PERSONAL_ACCESS_TOKEN_ID =							"ID";
	public static final String COL_PERSONAL_ACCESS_TOKEN_NAME =							"NAME";
	public static final String COL_PERSONAL_ACCESS_TOKEN_PRINCIPAL_ID =					"PRINCIPAL_ID";
	public static final String COL_PERSONAL_ACCESS_TOKEN_SCOPES =						"SCOPES";
	public static final String COL_PERSONAL_ACCESS_TOKEN_CLAIMS =						"CLAIMS";
	public static final String COL_PERSONAL_ACCESS_TOKEN_CREATED_ON =					"CREATED_ON";
	public static final String COL_PERSONAL_ACCESS_TOKEN_LAST_USED =					"LAST_USED";

	// Status table for monthly statistics
	public static final String TABLE_STATISTICS_MONTHLY_STATUS =						"STATISTICS_MONTHLY_STATUS";
	public static final String DDL_STATISTICS_MONTHLY_STATUS =							"schema/statistics/StatisticsMonthlyStatus-ddl.sql";
	public static final String COL_STATISTICS_MONTHLY_STATUS_OBJECT_TYPE =				"OBJECT_TYPE";
	public static final String COL_STATISTICS_MONTHLY_STATUS_MONTH =					"MONTH";
	public static final String COL_STATISTICS_MONTHLY_STATUS_STATUS =					"STATUS";
	public static final String COL_STATISTICS_MONTHLY_STATUS_LAST_STARTED_ON =			"LAST_STARTED_ON";
	public static final String COL_STATISTICS_MONTHLY_STATUS_LAST_UPDATED_ON =			"LAST_UPDATED_ON";
	public static final String COL_STATISTICS_MONTHLY_STATUS_ERROR_MESSAGE	=			"ERROR_MESSAGE";
	public static final String COL_STATISTICS_MONTHLY_STATUS_ERROR_DETAILS = 			"ERROR_DETAILS";
	
	// Status table for monthly statistics
	public static final String TABLE_STATISTICS_MONTHLY_PROJECT_FILES =					"STATISTICS_MONTHLY_PROJECT_FILES";
	public static final String DDL_STATISTICS_MONTHLY_PROJECT_FILES =					"schema/statistics/StatisticsMonthlyProjectFiles-ddl.sql";
	public static final String COL_STATISTICS_MONTHLY_PROJECT_FILES_PROJECT_ID =		"PROJECT_ID";
	public static final String COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH =				"MONTH";
	public static final String COL_STATISTICS_MONTHLY_PROJECT_FILES_EVENT_TYPE =		"EVENT_TYPE";
	public static final String COL_STATISTICS_MONTHLY_PROJECT_FILES_FILES_COUNT =		"FILES_COUNT";
	public static final String COL_STATISTICS_MONTHLY_PROJECT_FILES_USERS_COUNT =		"USERS_COUNT";
	public static final String COL_STATISTICS_MONTHLY_PROJECT_FILES_LAST_UPDATED_ON =	"LAST_UPDATED_ON";
	
	// Table constants related to SES Notifications
	public static final String TABLE_SES_NOTIFICATIONS = 								"SES_NOTIFICATIONS";
	public static final String DDL_SES_NOTIFICATIONS = 									"schema/ses/SESNotifications-ddl.sql";
	public static final String COL_SES_NOTIFICATIONS_ID = 								"ID";
	public static final String COL_SES_NOTIFICATIONS_INSTANCE_NUMBER = 					"INSTANCE_NUMBER";
	public static final String COL_SES_NOTIFICATIONS_CREATED_ON = 						"CREATED_ON";
	public static final String COL_SES_NOTIFICATIONS_SES_MESSAGE_ID = 					"SES_MESSAGE_ID";
	public static final String COL_SES_NOTIFICATIONS_SES_FEEDBACK_ID = 					"SES_FEEDBACK_ID";
	public static final String COL_SES_NOTIFICATIONS_TYPE = 							"NOTIFICATION_TYPE";
	public static final String COL_SES_NOTIFICATIONS_SUBTYPE = 							"NOTIFICATION_SUBTYPE";
	public static final String COL_SES_NOTIFICATIONS_REASON = 							"NOTIFICATION_REASON";
	public static final String COL_SES_NOTIFICATIONS_BODY = 							"NOTIFICATION_BODY";
	
	// Table constants related to emails that are quarantined
	public static final String TABLE_QUARANTINED_EMAILS = 								"QUARANTINED_EMAILS";
	public static final String DDL_QUARANTINED_EMAILS = 								"schema/ses/QuarantinedEmails-ddl.sql";
	public static final String COL_QUARANTINED_EMAILS_ID = 								"ID";
	public static final String COL_QUARANTINED_EMAILS_EMAIL = 							"EMAIL";
	public static final String COL_QUARANTINED_EMAILS_ETAG = 							"ETAG";
	public static final String COL_QUARANTINED_EMAILS_CREATED_ON = 						"CREATED_ON";
	public static final String COL_QUARANTINED_EMAILS_UPDATED_ON = 						"UPDATED_ON";
	public static final String COL_QUARANTINED_EMAILS_EXPIRES_ON = 						"EXPIRES_ON";
	public static final String COL_QUARANTINED_EMAILS_REASON = 							"REASON";
	public static final String COL_QUARANTINED_EMAILS_REASON_DETAILS =					"REASON_DETAILS";
	public static final String COL_QUARANTINED_EMAILS_SES_MESSAGE_ID = 					"SES_MESSAGE_ID";
	
	// DataAccessNotification
	public static final String DDL_DATA_ACCESS_NOTIFICATION = 							"schema/DataAccessNotification-ddl.sql";
	public static final String TABLE_DATA_ACCESS_NOTIFICATION = 						"DATA_ACCESS_NOTIFICATION";
	public static final String COL_DATA_ACCESS_NOTIFICATION_ID = 						"ID";
	public static final String COL_DATA_ACCESS_NOTIFICATION_ETAG = 						"ETAG";
	public static final String COL_DATA_ACCESS_NOTIFICATION_TYPE = 						"NOTIFICATION_TYPE";
	public static final String COL_DATA_ACCESS_NOTIFICATION_REQUIREMENT_ID = 			"REQUIREMENT_ID";
	public static final String COL_DATA_ACCESS_NOTIFICATION_RECIPIENT_ID = 				"RECIPIENT_ID";
	public static final String COL_DATA_ACCESS_NOTIFICATION_APPROVAL_ID = 				"ACCESS_APPROVAL_ID";
	public static final String COL_DATA_ACCESS_NOTIFICATION_SENT_ON = 					"SENT_ON";
	public static final String COL_DATA_ACCESS_NOTIFICATION_MESSAGE_ID = 				"MESSAGE_ID";	
	
	// Feature testing
	public static final String DDL_FEATURE_STATUS =										"schema/testing/FeatureStatus-ddl.sql";
	public static final String TABLE_FEATURE_STATUS = 									"FEATURE_STATUS";
	public static final String COL_FEATURE_STATUS_ID = 									"ID";
	public static final String COL_FEATURE_STATUS_ETAG = 								"ETAG";
	public static final String COL_FEATURE_STATUS_TYPE = 								"FEATURE_TYPE";
	public static final String COL_FEATURE_STATUS_ENABLED =								"ENABLED";
	
	// The file associations scanner status
	public static final String DDL_FILES_SCANNER_STATUS = 								"schema/files/FilesScannerStatus-ddl.sql";
	public static final String TABLE_FILES_SCANNER_STATUS = 							"FILES_SCANNER_STATUS";
	public static final String COL_FILES_SCANNER_STATUS_ID = 							"ID";
	public static final String COL_FILES_SCANNER_STATUS_STARTED_ON = 					"STARTED_ON";
	public static final String COL_FILES_SCANNER_STATUS_UPDATED_ON = 					"UPDATED_ON";
	public static final String COL_FILES_SCANNER_STATUS_JOBS_STARTED_COUNT = 			"JOBS_STARTED_COUNT";
	public static final String COL_FILES_SCANNER_STATUS_JOBS_COMPLETED_COUNT = 			"JOBS_COMPLETED_COUNT";
	public static final String COL_FILES_SCANNER_STATUS_SCANNED_ASSOCIATIONS_COUNT = 	"SCANNED_ASSOCIATIONS_COUNT";

	// This seems to be the name of the id column for all tables.
	public static final String COLUMN_ID		= "id";
	
	public static final String TYPE_COLUMN_NAME = "nodeType";
	
	public static final String AUTH_FILTER_ALIAS = "auth";

	// standard range parameters
	public static final String OFFSET_PARAM_NAME = "OFFSET";
	public static final String LIMIT_PARAM_NAME = "LIMIT";
	
	
	// This is the alias of the sub-query used for sorting on annotations.
	public static final String ANNOTATION_SORT_SUB_ALIAS 	= "assa";
		
	public static final String INPUT_DATA_LAYER_DATASET_ID = "INPUT_LAYERS_ID_OWN";
	
	private static final Map<String, String> primaryFieldColumns;
	
	/**
	 * This is from the DB: SHOW VARIABLES LIKE 'max_allowed_packet';
	 */
	public static final int MAX_ALLOWED_PACKET_BYTES = 16777216;
	public static final int MAX_BYTES_PER_LONG_AS_STRING = 20*2; // 20 chars at 2 bytes per char.;
	public static final int MAX_LONGS_PER_IN_CLAUSE = MAX_ALLOWED_PACKET_BYTES/MAX_BYTES_PER_LONG_AS_STRING;
	
	/**
	 * Function names:
	 */
	public static final String FUNCTION_GET_ENTITY_BENEFACTOR_ID = "getEntityBenefactorId";
	public static final String FUNCTION_GET_ENTITY_PROJECT_ID = "getEntityProjectId";

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

}
