package org.sagebionetworks.sweeper.log4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

import org.apache.log4j.rolling.helper.ActionBase;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;

public class SweepAction extends ActionBase {

	private static final String EC2_METADATA_ENDPOINT = "http://169.254.169.254/latest/meta-data";

	private static boolean onEC2 = true;

	private AmazonS3 s3Client;

	private File file;

	private String s3BucketName;

	private boolean deleteSource;

	public static boolean isOnEC2() {
		return onEC2;
	}

	public static void setOnEC2(boolean onEC2) {
		SweepAction.onEC2 = onEC2;
	}

	public AmazonS3 getS3Client() {
		return s3Client;
	}

	public void setS3Client(AmazonS3 s3Client) {
		this.s3Client = s3Client;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getS3BucketName() {
		return s3BucketName;
	}

	public void setS3BucketName(String s3BucketName) {
		this.s3BucketName = s3BucketName;
	}

	public boolean isDeleteSource() {
		return deleteSource;
	}

	public void setDeleteSource(boolean deleteSource) {
		this.deleteSource = deleteSource;
	}

	public SweepAction(File file, String s3BucketName, AmazonS3 s3Client, boolean deleteSource) {
		if (file == null)
			throw new NullPointerException("fileName");
		if (s3BucketName == null)
			throw new NullPointerException("s3BucketName");
		if (s3Client == null)
			throw new NullPointerException("s3Client");

		this.file = file;
		this.s3BucketName = s3BucketName;
		this.s3Client = s3Client;
		this.deleteSource = deleteSource;
	}

	@Override
	public boolean execute() throws IOException {
		if (!file.exists())
			return false;

		StringBuilder key = new StringBuilder(getUniqueId());
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

	private static String getUniqueId() {
		String uid = UUID.randomUUID().toString();

		if (isOnEC2()) {
			try {
				uid = getEC2InstanceId();
			} catch (Exception e) {
			}
		}
		return uid;
	}

	private static String getEC2InstanceId() throws IOException {
		String ec2Id = "";
		String inputLine;

		URL ec2MetaData = new URL(EC2_METADATA_ENDPOINT + "/instance-id");
		URLConnection EC2MD = ec2MetaData.openConnection();

		BufferedReader in = new BufferedReader(new InputStreamReader(
				EC2MD.getInputStream()));
		while ((inputLine = in.readLine()) != null) {
			ec2Id = inputLine;
		}
		in.close();

		return ec2Id;
	}
}
