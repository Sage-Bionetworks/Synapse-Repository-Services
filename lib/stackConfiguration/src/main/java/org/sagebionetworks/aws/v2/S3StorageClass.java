package org.sagebionetworks.aws.v2;

/**
 * The S3 storage class to assign to a new object.
 * <p>
 * Only the storage classes Synapse actually writes are represented. Extend this enum when a new
 * one is needed rather than exposing the SDK's own enum through {@link S3ObjectStore}.
 */
public enum S3StorageClass {

	STANDARD,

	INTELLIGENT_TIERING

}
