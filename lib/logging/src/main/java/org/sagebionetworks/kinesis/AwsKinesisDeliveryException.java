package org.sagebionetworks.kinesis;

/**
 * Exception thrown when we could not deliver a batch of records to kinesis even after several attempts with exponential backoff
 */
public class AwsKinesisDeliveryException extends RuntimeException {
	
	public AwsKinesisDeliveryException(String message, Throwable cause) {
		super(message, cause);
	}

	public AwsKinesisDeliveryException(String message) {
		super(message);
	}

	public AwsKinesisDeliveryException(Throwable cause) {
		super(cause);
	}

	

}
