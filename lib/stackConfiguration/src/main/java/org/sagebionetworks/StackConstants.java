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
	
	

}
