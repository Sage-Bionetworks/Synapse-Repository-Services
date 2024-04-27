package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.upload.MultithreadMultipartUpload;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;
import org.sagebionetworks.util.RandomTempFileUtil;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

@ExtendWith(ITTestExtension.class)
public class ITMultipartUpload {

	private final int BYTES_PER_MB = (int) Math.pow(1024, 2);

	private final SynapseAdminClient adminSynapse;
	private final SynapseClient synapse;
	private final StackConfiguration stackConfig;
	private final AmazonS3 s3Client;
	private final ExecutorService threadPool;
	private final int numberOfThreads = 50;
	private final int fileSizeByptes = BYTES_PER_MB * 50 + 10;

	public ITMultipartUpload(SynapseAdminClient adminSynapse, SynapseClient synapse, StackConfiguration stackConfig,
			AmazonS3 s3Client) {
		super();
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
		this.stackConfig = stackConfig;
		this.s3Client = s3Client;
		this.threadPool = Executors.newFixedThreadPool(numberOfThreads);
	}

	/**
	 * This test use the Amazon TransferManager to do a multi-part upload directly
	 * to S3. The resulting upload speed serves as a baseline for the expected
	 * performance.
	 * 
	 * @throws IOException
	 */
	@Disabled
	@Test
	public void testS3ClientFileUpload() throws IOException {
		RandomTempFileUtil.consumeRandomTempFile(fileSizeByptes, "random", ".bin", (temp) -> {
			TransferManager transferManager = TransferManagerBuilder.standard().withS3Client(s3Client)
					.withExecutorFactory(() -> {
						return threadPool;
					}).build();

			String bucket = stackConfig.getS3Bucket();
			String key = "ITMultipartUpload/testS3ClientFileUpload/" + UUID.randomUUID().toString() + ".bin";
			long start = System.currentTimeMillis();
			Upload upload = transferManager.upload(new PutObjectRequest(bucket, key, temp));
			try {
				upload.waitForCompletion();
			} catch (AmazonClientException | InterruptedException e) {
				throw new RuntimeException(e);
			}
			long eplase = System.currentTimeMillis() - start;
			long bytesPerMS = fileSizeByptes / eplase;
			System.out.println(String.format(
					"File was uploaded to S3 using AWS TransferManager in %d ms with an average rate of %d bytes/ms", eplase,
					bytesPerMS));
			s3Client.deleteObject(bucket, key);
		});
	}

	@Test
	public void testLargeFileUpoadToS3() {
		RandomTempFileUtil.consumeRandomTempFile(fileSizeByptes, "random", ".bin", (temp) -> {
			boolean forceRestart = false;
			try {
				long start = System.currentTimeMillis();
				CloudProviderFileHandleInterface fileHandle = MultithreadMultipartUpload.doUpload(threadPool, synapse,
						temp, new MultipartUploadRequest().setGeneratePreview(false), forceRestart);
				long eplase = System.currentTimeMillis() - start;
				long bytesPerMS = fileSizeByptes / eplase;
				System.out.println(String.format(
						"File was uploaded to S3 in %d ms with an average rate of %d bytes/ms FileHandle: %s", eplase,
						bytesPerMS, fileHandle.toString()));
				assertTrue(s3Client.doesObjectExist(fileHandle.getBucketName(), fileHandle.getKey()));
				synapse.deleteFileHandle(fileHandle.getId());
				assertFalse(s3Client.doesObjectExist(fileHandle.getBucketName(), fileHandle.getKey()));
			} catch (SynapseException | IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Test
	public void testLargeGoogleFileUpload() throws SynapseException {

		// Only run this test if Google Cloud is enabled.
		Assumptions.assumeTrue(stackConfig.getGoogleCloudEnabled());

		ExternalGoogleCloudStorageLocationSetting storageLocationSetting = new ExternalGoogleCloudStorageLocationSetting();
		String baseKey = "integration-test/ITMultipartUpload/testLargeGoogleFileUpload/" + UUID.randomUUID().toString();
		String bucket = "dev.test.gcp-storage.sagebase.org";

		storageLocationSetting.setBucket(bucket);
		storageLocationSetting.setBaseKey(baseKey);
		storageLocationSetting.setUploadType(UploadType.GOOGLECLOUDSTORAGE);
		storageLocationSetting = adminSynapse.createStorageLocationSetting(storageLocationSetting);
		Long storageLocationId = storageLocationSetting.getStorageLocationId();

		RandomTempFileUtil.consumeRandomTempFile(fileSizeByptes, "random", ".bin", (temp) -> {
			boolean forceRestart = false;
			try {
				long start = System.currentTimeMillis();
				CloudProviderFileHandleInterface fileHandle = MultithreadMultipartUpload.doUpload(threadPool, synapse,
						temp,
						new MultipartUploadRequest().setGeneratePreview(false).setStorageLocationId(storageLocationId),
						forceRestart);
				long eplase = System.currentTimeMillis() - start;
				long bytesPerMS = fileSizeByptes / eplase;
				System.out.println(String.format(
						"File was uploaded to Google in %d ms with an average rate of %d bytes/ms FileHandle: %s",
						eplase, bytesPerMS, fileHandle.toString()));
				synapse.deleteFileHandle(fileHandle.getId());
			} catch (SynapseException | IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

}
