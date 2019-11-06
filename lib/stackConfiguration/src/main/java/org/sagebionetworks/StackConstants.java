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

	/**
	 * Template used to generate the name of the AWS SNS topics and SQS Queues
	 */
	public static final String QUEUE_AND_TOPIC_NAME_TEMPLATE = "%1$s-%2$s-%3$s";



	/**
	 * The bucket containing all table row data.
	 */
	public static final String TABLE_ROW_CHANGE_BUCKET = "%1$s.table.row.changes";
	
	/**
	 * Bucket name for view snapshots.
	 */
	public static final String VIEW_SNAPSHOT_BUCKET = "%1$s.view.snapshots";
	
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
