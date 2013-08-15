package org.sagebionetworks;

/**
 * All of these constants should default scope.
 * @author jmhill
 *
 */
public class StackConstants {
	
	static final String S3_PROPERTY_FILENAME_PREFIX = "https://s3.amazonaws.com";

	static final String STACK_PROPERTY_FILE_URL 	= "org.sagebionetworks.stack.configuration.url";
	static final String STACK_IAM_ID 				= "org.sagebionetworks.stack.iam.id";
	static final String STACK_IAM_KEY 				= "org.sagebionetworks.stack.iam.key";
	static final String STACK_ENCRYPTION_KEY		 = "org.sagebionetworks.stackEncryptionKey";
	
	static final String STACK_PROPERTY_NAME 			= "org.sagebionetworks.stack";
	static final String STACK_INSTANCE_PROPERTY_NAME 	= "org.sagebionetworks.stack.instance";
	
	static final String PARAM1 = "PARAM1";
	static final String PARAM2 = "PARAM2";
	static final String PARAM4 = "PARAM4";
	static final String PARAM3 = "PARAM3";
	
	/**
	 * Any template property marked with this value must have the stack prefix applied.
	 */
	static final String REQUIRES_STACK_PREFIX = "<REQUIRES_STACK_PREFIX>";
	
	static final String REQUIRES_STACK_INTANCE_PREFIX = "<REQUIRES_INSTANCE_PREFIX>";
	
	static final String DATABASE_URL_PROPERTY = "connection.url";
	
	/**
	 * Template used the name of the AWS topic where repository changes messages are published.
	 */
	public static final String TOPIC_NAME_TEMPLATE = "%1$s-%2$s-repo-changes";
	
	/**
	 * Template used for the name of the AWS SQS where search updates are pushed.
	 */
	public static final String SEARCH_QUEUE_NAME_TEMPLATE = "%1$s-%2$s-search-update-queue";

	/**
	 * Template used for the name of the AWS SQS where dynamo updates are pushed.
	 */
	public static final String DYNAMO_QUEUE_NAME_TEMPLATE = "%1$s-%2$s-dynamo-update-queue";
	
	/**
	 * Template used for the name of the AWS SQS where search updates are pushed.
	 */
	public static final String RDS_QUEUE_NAME_TEMPLATE = "%1$s-%2$s-rds-update-queue";
	
	/**
	 * Template used for the name of the AWS SQS where file updates are pushed.
	 */
	public static final String FILE_QUEUE_NAME_TEMPLATE = "%1$s-%2$s-file-update-queue";


	/**
	 * Template used for the name of the AWS SQS where annotations updates are pushed.
	 */
	public static final String ANNOTATIONS_QUEUE_NAME_TEMPLATE = "%1$s-%2$s-annotations-update-queue";
	
	/**
	 * The bucket containing all access record data.
	 */
	public static final String ACCESS_RECORD_BUCKET = "%1$s.access.record.sagebase.org";

}
