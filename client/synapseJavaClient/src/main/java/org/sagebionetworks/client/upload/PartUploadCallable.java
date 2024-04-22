package org.sagebionetworks.client.upload;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.concurrent.Callable;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.PartPresignedUrl;
import org.sagebionetworks.util.ValidateArgument;

/**
 * A Callable used to upload a single part of a multi-part upload.
 *
 */
public class PartUploadCallable implements Callable<AddPartResponse> {

	private static final int MAX_SKIPS = 100;

	private final FilePartRequest request;

	public PartUploadCallable(FilePartRequest request) {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getSynapseClient(), "request.synapseclient");
		ValidateArgument.required(request.getUploadId(), "request.uploadId");
		ValidateArgument.required(request.getPartNumber(), "request.partNumber");
		ValidateArgument.required(request.getFile(), "request.file");
		ValidateArgument.required(request.getPartLength(), "request.partLength");
		ValidateArgument.required(request.getPartOffset(), "request.partOffset");
		ValidateArgument.required(request.getHttpClient(), "request.httpClient");
		this.request = request;
	}

	String putToUrl(PartPresignedUrl url) throws URISyntaxException, IOException, ClientProtocolException, SynapseClientException {
		ValidateArgument.required(url, "url");
		ValidateArgument.required(url.getUploadPresignedUrl(), "url.uloadPresignedUrl");
		try (FileInputStream fis = new FileInputStream(request.getFile())) {
			skipBytes(fis, request.getPartOffset());
			HttpPut httpPut = new HttpPut(new URL(url.getUploadPresignedUrl()).toURI());
			if(url.getSignedHeaders() != null) {
				url.getSignedHeaders().forEach((k,v)->{
					httpPut.setHeader(k, v);
				});
			}
			MessageDigest digest = ThreadLocalMD5Digest.getThreadDigest();
			// the MD5 will be calculated as the data is PUT to the URL.
			InputStreamEntity entity = new InputStreamEntity(new DigestInputStream(fis, digest),
					request.getPartLength());
			entity.setChunked(false);
			httpPut.setEntity(entity);
			try (CloseableHttpResponse response = request.getHttpClient().execute(httpPut)) {
				if (response.getStatusLine().getStatusCode() != 200) {
					throw new SynapseClientException(String.format("PUT failed code: %d reason: '%s'",
							response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase()));
				}
			}
			return new String(Hex.encodeHex(digest.digest()));
		}
	}

	@Override
	public AddPartResponse call() throws Exception {
		// Get the PUT URL
		BatchPresignedUploadUrlResponse batchResponse = request.getSynapseClient()
				.getMultipartPresignedUrlBatch(new BatchPresignedUploadUrlRequest().setUploadId(request.getUploadId())
						.setPartNumbers(Collections.singletonList(request.getPartNumber())));
		// PUT to the URL
		String partMD5Hex = putToUrl(batchResponse.getPartPresignedUrls().get(0));
		// Add the part to the Upload
		return request.getSynapseClient().addPartToMultipartUpload(request.getUploadId(),
				request.getPartNumber().intValue(), partMD5Hex);
	}

	/**
	 * Set the starting offset of the provided InputStream by skipping to the offset
	 * position.
	 * 
	 * @param fs
	 * @param offset
	 * @throws IOException If unable to skip
	 */
	public static void skipBytes(FileInputStream fs, long offset) throws IOException {
		long skippedSoFar = 0;
		for (int skips = 0; skips < MAX_SKIPS && skippedSoFar < offset; ++skips) {
			skippedSoFar += fs.skip(offset - skippedSoFar);
		}
		if (skippedSoFar != offset) {
			throw new IOException(String.format("Failed to skip to file offset %d after %d tries", offset, MAX_SKIPS));
		}
	}

}
