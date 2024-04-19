package org.sagebionetworks.client.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.util.ValidateArgument;

public class MultithreadMultipartUpload {

	public static final long MIN_PART_SIZE = 1024 * 1024 * 5; // 5 MB
	public static final long MAX_PARTS_PER_FILE = 10_000;

	private static final CloseableHttpClient httpClient;
	static {
		httpClient = HttpClients.custom().setDefaultCookieStore(new BasicCookieStore()).build();
	}

	/**
	 * Attempt to upload the provided file using multiple threads. A file upload can
	 * fail if one or more parts fails to upload. If this method is called again
	 * after a failure, it will attempt to resume uploading all failed or missing
	 * parts. Any parts that have already been uploaded will not need to be
	 * re-uploaded. Set forceRestart=true to abandon all successfully uploaded parts
	 * to restart the file upload from the beginning.
	 * 
	 * @param threadPool   The thread pool to be used for uploads (required).
	 * @param client       The Synapse Client to be used (required).
	 * @param toUpload     The local file to upload (required).
	 * @param request      Optional. Provide to override any default value.
	 * @param forceRestart Force this file to be upload from the beginning
	 * @return
	 * @throws SynapseException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static CloudProviderFileHandleInterface doUpload(ExecutorService threadPool, SynapseClient client,
			File toUpload, MultipartUploadRequest request, boolean forceRestart)
			throws SynapseException, FileNotFoundException, IOException {
		return doUpload((r) -> {
			return new PartUploadCallable(r);
		}, threadPool, client, toUpload, request, forceRestart);
	}

	public static CloudProviderFileHandleInterface doUpload(PartCallableFactory callableFactory,
			ExecutorService threadPool, SynapseClient client, File toUpload, MultipartUploadRequest request,
			boolean forceRestart) throws SynapseException, FileNotFoundException, IOException {
		ValidateArgument.required(threadPool, "threadPool");
		ValidateArgument.required(client, "SynapseClient");
		if (request == null) {
			request = new MultipartUploadRequest();
		}
		if (!toUpload.exists()) {
			throw new IllegalArgumentException("The provided file does not exist: " + toUpload.getAbsolutePath());
		}
		ValidateArgument.required(request, "request");
		if (request.getFileName() == null) {
			request.setFileName(toUpload.getName());
		}
		if (request.getContentType() == null) {
			request.setContentType(Files.probeContentType(toUpload.toPath()));
		}
		request.setFileSizeBytes(toUpload.length());
		try (FileInputStream fis = new FileInputStream(toUpload)) {
			request.setContentMD5Hex(
					new String(Hex.encodeHex(DigestUtils.digest(ThreadLocalMD5Digest.getThreadDigest(), fis))));
		}

		request.setPartSizeBytes(Math.max(MIN_PART_SIZE, (toUpload.length() / MAX_PARTS_PER_FILE)));
		MultipartUploadStatus status = client.startMultipartUpload(request, forceRestart);

		char[] parts = status.getPartsState().toCharArray();
		int numberOfParts = parts.length;
		List<Future<AddPartResponse>> addPartFutures = new ArrayList<>(numberOfParts);
		for (int partNumber = 1; partNumber < parts.length + 1; partNumber++) {
			if ('0' == parts[partNumber - 1]) {
				long partOffset = request.getPartSizeBytes() * (partNumber - 1);
				long partLength = partNumber < numberOfParts ? request.getPartSizeBytes()
						: toUpload.length() - partOffset;
				addPartFutures.add(threadPool.submit(
						callableFactory.createCallable(new FilePartRequest().setSynapseClient(client).setFile(toUpload)
								.setPartLength(partLength).setPartNumber((long) partNumber).setPartOffset(partOffset)
								.setUploadId(status.getUploadId()).setFile(toUpload).setHttpClient(httpClient))));
			}
		}

		addPartFutures.forEach(f -> {
			try {
				AddPartResponse response = f.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		});
		status = client.completeMultipartUpload(status.getUploadId());

		return (CloudProviderFileHandleInterface) client.getRawFileHandle(status.getResultFileHandleId());
	}

}
