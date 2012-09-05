package org.sagebionetworks.sweeper.log4j;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.rolling.helper.ActionBase;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;

public class SweepAction extends ActionBase {

	private AmazonS3 s3Client;

	private File file;

	private String instanceId;

	private String s3BucketName;

	private boolean deleteSource;

	public AmazonS3 getS3Client() {
		return s3Client;
	}

	public File getFile() {
		return file;
	}

	public String getS3BucketName() {
		return s3BucketName;
	}

	public boolean isDeleteSource() {
		return deleteSource;
	}

	public SweepAction(File file, String instanceId, String s3BucketName, AmazonS3 s3Client, boolean deleteSource) {
		if (file == null)
			throw new NullPointerException("fileName");
		if (instanceId == null)
			throw new NullPointerException("instanceId");
		if (s3BucketName == null)
			throw new NullPointerException("s3BucketName");
		if (s3Client == null)
			throw new NullPointerException("s3Client");

		this.file = file;
		this.instanceId = instanceId;
		this.s3BucketName = s3BucketName;
		this.s3Client = s3Client;
		this.deleteSource = deleteSource;
	}

	@Override
	public boolean execute() throws IOException {
		if (!file.exists())
			return false;

		StringBuilder key = new StringBuilder(instanceId);
		key.append("/");
		key.append(file.getName());

		try {
			s3Client.putObject(s3BucketName, key.toString(), file);
			if (deleteSource) {
				file.delete();
			}
		} catch (AmazonClientException e) {
			return false;
		}

		return true;
	}
}
