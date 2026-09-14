package org.sagebionetworks.aws.v2;

/**
 * A pre-defined S3 access control list to assign to a new object.
 * <p>
 * Only the canned ACLs Synapse actually writes are represented. Extend this enum when a new one is
 * needed rather than exposing the SDK's own enum through {@link S3ObjectStore}.
 */
public enum S3CannedAcl {

	/**
	 * Grants the bucket owner full control, which is required when writing to a bucket owned by
	 * another account.
	 */
	BUCKET_OWNER_FULL_CONTROL,

	PUBLIC_READ

}
