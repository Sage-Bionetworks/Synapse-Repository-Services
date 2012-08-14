package org.sagebionetworks.sweeper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;

/**
 * An application for collecting log files from an ec2 instance and collecting
 * them into s3 buckets. Configurable on what log-file patterns to match, and
 * which s3 bucket they should be swept to.
 *
 */
public class Sweeper {
	private final Set<SweepConfiguration> configList;

	private final String ec2InstanceId;
	private AmazonS3 client;

	private boolean shouldDeleteAfterSweeping;

	public Sweeper(AmazonS3 client, EC2IdProvider idProvider,
			Set<SweepConfiguration> configList, boolean shouldDeleteAfterSweeping) {
		this.client = client;
		String ec2InstanceId;
		try {
			ec2InstanceId = idProvider.getEC2InstanceId();
		} catch (IOException e) {
			ec2InstanceId = UUID.randomUUID().toString();
		}
		this.ec2InstanceId = ec2InstanceId;
		this.configList = configList;
		this.shouldDeleteAfterSweeping = shouldDeleteAfterSweeping;
	}

	public AmazonS3 getClient() {
		return client;
	}

	public void setClient(AmazonS3 client) {
		this.client = client;
	}

	public boolean getShouldDeleteAfterSweeping() {
		return shouldDeleteAfterSweeping;
	}

	public void setShouldDeleteAfterSweeping(boolean shouldDeleteAfterSweeping) {
		this.shouldDeleteAfterSweeping = shouldDeleteAfterSweeping;
	}

	public void validateConfigurations() {
		List<String> myBucketNames = new ArrayList<String>();

		for (Bucket bucket : client.listBuckets()) {
			myBucketNames.add(bucket.getName());
		}

		for (SweepConfiguration config : configList) {
			boolean bucketExists = client.doesBucketExist(config
					.getS3BucketName());
			boolean isInMyBuckets = myBucketNames.contains(config
					.getS3BucketName());
			if (bucketExists && isInMyBuckets) {
				continue;
			} else if (bucketExists) {
				// Could also try to add a unique identifier to the front/back
				// of bucketname
				// but this requires a lot more work ;)
				throw new RuntimeException("Bucketname: \""
						+ config.getS3BucketName()
						+ "\" already exists and is not owned by us!");
			} else {
				// Create a new bucket.  There needs to be some configuration about
				// how buckets are setup by default...
				throw new RuntimeException("Bucketname: \""
						+ config.getS3BucketName()
						+ "\" does not exist.");
			}
		}
	}

	public void sweepAllLogFiles() {
		sweepRolledLogFiles();
		sweepActiveLogFiles();
	}

	public void sweepActiveLogFiles() {
		for (SweepConfiguration config : configList) {
			File activeFile = new File(config.getLogBaseDir()+"/"+config.getLogBaseFile());
			if (activeFile.exists()) {
				List<File> arrList = new ArrayList<File>();
				arrList.add(activeFile);
				sweep(config, arrList);
			}
		}
	}

	public void sweepRolledLogFiles() {
		for (SweepConfiguration config : configList) {
			List<File> files = findFiles(config);
			sweep(config, files);
		}
	}

	public void sweep(SweepConfiguration config, List<File> filesToSweep) {
		for (File file : filesToSweep) {
			String key = String.format("%s-%s", ec2InstanceId, file.getName());
			try {
				client.putObject(config.getS3BucketName(), key, file);
				if (shouldDeleteAfterSweeping) {
					file.delete();
				}
			} catch (AmazonClientException e) {
				// Do something?
			}
		}
	}

	public List<File> findFiles(SweepConfiguration config) {
		File dir = new File(config.getLogBaseDir());
		File[] files = dir.listFiles(config.getFilter());

		if (files != null) {
			return Arrays.asList(files);
		} else {
			return new ArrayList<File>();
		}
	}

}
