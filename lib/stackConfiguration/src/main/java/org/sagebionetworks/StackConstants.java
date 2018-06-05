package org.sagebionetworks;

/**
 * All of these constants should default scope.
 * @author jmhill
 *
 */
public class StackConstants {
	
	static final String S3_PROPERTY_FILENAME_PREFIX = "https://s3.amazonaws.com";
	
	static final String STACK_PROPERTY_NAME 			= "org.sagebionetworks.stack";
	static final String STACK_INSTANCE_PROPERTY_NAME 	= "org.sagebionetworks.stack.instance";
	
	static final String PARAM4 = "PARAM4";
	static final String PARAM3 = "PARAM3";
	
	/**
	 * Any template property marked with this value must have the stack prefix applied.
	 */
	static final String REQUIRES_STACK_PREFIX = "<REQUIRES_STACK_PREFIX>";
	
	static final String REQUIRES_STACK_INTANCE_PREFIX = "<REQUIRES_INSTANCE_PREFIX>";
	
	static final String DATABASE_URL_PROPERTY = "connection.url";
	
	public static final String STACK_AND_INSTANCE = "%1$s-%2$s";
		
	public static final String ASYNC_QUEUE_TEMPLATE = "%1$s-%2$s-%3$s-async-queue";

	public static final String WORKER_QUEUE_TEMPLATE = "%1$s-%2$s-%3$s-worker-queue";

	/**
	 * Template used the name of the AWS topic where repository changes messages are published.
	 */
	public static final String TOPIC_NAME_TEMPLATE_PREFIX = "%1$s-%2$s-repo-";
	
	/**
	 * Template used for the name of the AWS SQS where search updates are pushed.
	 */
	public static final String SEARCH_QUEUE_NAME_TEMPLATE = "%1$s-%2$s-search-update-queue";

	/**
	 * Template used for the name of the AWS SQS dead letter where search updates are pushed.
	 */
	public static final String SEARCH_DEAD_LETTER_QUEUE_NAME_TEMPLATE = "%1$s-%2$s-search-update-queue-dl";

	/**
	 * Template used for the name of the AWS SQS where dynamo updates are pushed.
	 */
	public static final String DYNAMO_QUEUE_NAME_TEMPLATE = "%1$s-%2$s-dynamo-update-queue";
	
	/**
	 * Template used for the name of the AWS SQS where search updates are pushed.
	 */
	public static final String ENTITY_ANNOTATIONS_QUEUE_NAME_TEMPLATE = "%1$s-%2$s-entity-annotations-update-queue";
	
	/**
	 * Template used for the name of the AWS SQS where message (to user) updates are pushed.
	 */
	public static final String MESSAGE_QUEUE_NAME_TEMPLATE = "%1$s-%2$s-message-send-queue";
	
	/**
	 * Template used for the name of the AWS SQS where file updates are pushed.
	 */
	public static final String FILE_QUEUE_NAME_TEMPLATE = "%1$s-%2$s-file-update-queue";

	/**
	 * Template used for the name of the AWS SQS where file updates are pushed.
	 */
	public static final String FILE_DEAD_LETTER_QUEUE_NAME_TEMPLATE = "%1$s-%2$s-file-update-queue-dl";


	/**
	 * Template used for the name of the AWS SQS where annotations updates are pushed.
	 */
	public static final String SUBMISSION_ANNOTATIONS_QUEUE_NAME_TEMPLATE = "%1$s-%2$s-submission-annotations-update-queue";
	
	/**
	 * Template used for the name of the AWS SQS where ranges of change messages are pushed.
	 */
	public static final String UNSENT_MESSAGES_QUEUE_NAME_TEMPLATE = "%1$s-%2$s-unsent-messages-update-queue";
	
	/**
	 * Template used for the name of the AWS SQS where ranges of change messages are pushed.
	 */
	public static final String PRINCIPAL_HEADER_QUEUE_NAME_TEMPLATE = "%1$s-%2$s-principal-header-update-queue";
	
	/**
	 * Template used for the name of the AWS SQS where ranges of change messages are pushed.
	 */
	public static final String TABLE_CLUSTER_QUEUE_NAME_TEMPLATE = "%1$s-%2$s-table-cluster-queue";
	
	/**
	 * Template used for the name of the AWS SQS where ranges of change messages are pushed.
	 */
	public static final String TABLE_CLUSTER_DEAD_LETTER_QUEUE_NAME_TEMPLATE = "%1$s-%2$s-table-cluster-queue-dl";
	
	/**
	 * Template used for the name of the AWS SQS where ranges of change messages are pushed.
	 */
	public static final String TABLE_CURRENT_CACHE_QUEUE_NAME_TEMPLATE = "%1$s-%2$s-table-current-cache-queue";

	/**
	 * The bucket containing all table row data.
	 */
	public static final String TABLE_ROW_CHANGE_BUCKET = "%1$s.table.row.changes";
	
	/**
	 * The bucket containing all access record data.
	 */
	public static final String ACCESS_RECORD_BUCKET = "%1$s.access.record.sagebase.org";
	
	/**
	 * The bucket containing all object snapshot record data.
	 */
	public static final String SNAPSHOT_RECORD_BUCKET = "%1$s.snapshot.record.sagebase.org";

	/**
	 * The bucket containing all discussion data.
	 */
	public static final String DISCUSSION_BUCKET = "%1$s.discussion.sagebase.org";

	/**
	 * The bucket containing all access record data.
	 */
	public static final String STACK_LOG_BUCKET = "%1$s.log.sagebase.org";

	/**
	 * External S3 location test bucket.
	 */
	public static final String EXTERNAL_S3_TEST_BUCKET = "%1$s.external.s3.test.sagebase.org";

}
