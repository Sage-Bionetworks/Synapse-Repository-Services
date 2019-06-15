package org.sagebionetworks.aws;

/*
 * This is a special exception type to capture the state in which the AWS
 * client cannot determine a bucket's location.  It intentionally extends
 * IllegalArgumentException so that Spring will map it to a 400 level exception
 * (if it reaches the controller tier).
 */
public class CannotDetermineBucketLocationException extends IllegalArgumentException {

	public CannotDetermineBucketLocationException() {
	}

	public CannotDetermineBucketLocationException(String s) {
		super(s);
	}

	public CannotDetermineBucketLocationException(Throwable cause) {
		super(cause);
	}

	public CannotDetermineBucketLocationException(String message, Throwable cause) {
		super(message, cause);
	}

}
