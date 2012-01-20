package org.sagebionetworks.tool.migration.job;

import com.amazonaws.auth.AWSCredentials;

/**
 * Information need to work with S3 Objects.
 * @author John
 *
 */
public class AWSInfo {
	
	private AWSCredentials credentials = null;
	private String bucket = null;
	public AWSCredentials getCredentials() {
		return credentials;
	}
	public void setCredentials(AWSCredentials credentials) {
		this.credentials = credentials;
	}
	public String getBucket() {
		return bucket;
	}
	public void setBucket(String bucket) {
		this.bucket = bucket;
	}
	public AWSInfo(AWSCredentials credentials, String bucket) {
		super();
		this.credentials = credentials;
		this.bucket = bucket;
	}

}
