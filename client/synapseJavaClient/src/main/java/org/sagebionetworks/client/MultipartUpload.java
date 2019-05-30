package org.sagebionetworks.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.PartUtils;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Business logic for multi-part upload.
 * 
 * 
 */
public class MultipartUpload {

	// input parameters
	final SynapseClient client;
	final InputStream input;
	final Boolean forceRestart;
	final MultipartUploadRequest request;
	final FileProvider fileProvider;

	public MultipartUpload(SynapseClient client, InputStream input,
			long fileSizeBytes, String fileName, String contentType,
			Long storageLocationId, Boolean generatePreview, Boolean forceRestart, FileProvider fileProvider) {
		super();
		ValidateArgument.required(client, "SynapseClient");
		ValidateArgument.required(input, "InputStream");
		ValidateArgument.required(fileName, "fileName");
		ValidateArgument.required(contentType, "contentType");
		ValidateArgument.required(fileProvider, "fileProvider");
		this.request = new MultipartUploadRequest();
		this.request.setFileName(fileName);
		this.request.setContentType(contentType);
		this.request.setFileSizeBytes(fileSizeBytes);
		this.request.setGeneratePreview(generatePreview);
		this.request.setStorageLocationId(storageLocationId);
		this.client = client;
		this.input = input;
		this.forceRestart = forceRestart;
		this.fileProvider = fileProvider;
	}

	/**
	 * Upload the file.
	 * 
	 * @return
	 * @throws SynapseException 
	 * @throws DigestException
	 */
	public S3FileHandle uploadFile() throws SynapseException {
		// the number of bytes per part
		final long fileSizeBytes = request.getFileSizeBytes();
		long partSizeBytes = PartUtils.choosePartSize(fileSizeBytes);
		// the number of parts for this upload
		long numberOfParts = PartUtils.calculateNumberOfParts(
				fileSizeBytes, partSizeBytes);
		// metadata for each part.
		List<PartData> partDataList = new ArrayList<PartData>(
				(int) numberOfParts);
		try {
			// All of the part files are created.
			String fileMD5Hex = createParts(fileProvider, input, fileSizeBytes,
					partSizeBytes, numberOfParts, partDataList);
			
			this.request.setPartSizeBytes(partSizeBytes);
			this.request.setContentMD5Hex(fileMD5Hex);
			// Ready to start the upload
			MultipartUploadStatus status = client.startMultipartUpload(request, forceRestart);
			// If the file upload is done then just return the FileHandle
			if(status.getResultFileHandleId() != null){
				return (S3FileHandle) client.getRawFileHandle(status.getResultFileHandleId());
			}
			// Add only the parts that are needed
			uploadMissingParts(client, status, partDataList, request.getContentType());
			// Complete the file upload
			status = client.completeMultipartUpload(status.getUploadId());
			return (S3FileHandle) client.getRawFileHandle(status.getResultFileHandleId());
		} finally {
			deleteTempFiles(partDataList);
		}
	}
	
	/**
	 * 
	 * @param client
	 * @param partsState
	 * @param partDataList
	 * @throws SynapseException 
	 */
	public static void uploadMissingParts(final SynapseClient client,
			final MultipartUploadStatus status, final List<PartData> partDataList,
			final String contentType) throws SynapseException{
		char[] partStateArray = status.getPartsState().toCharArray();
		for(int i=0; i<partStateArray.length; i++){
			char state = partStateArray[i];
			if('0' == state){
				// this is a missing part
				PartData partData = partDataList.get(i);
				// Get a URL for this part.
				BatchPresignedUploadUrlRequest batchRequest = new BatchPresignedUploadUrlRequest();
				batchRequest.setUploadId(status.getUploadId());
				batchRequest.setPartNumbers(new LinkedList<Long>());
				batchRequest.getPartNumbers().add((long) partData.getPartNumber());
				BatchPresignedUploadUrlResponse batchResponse = client.getMultipartPresignedUrlBatch(batchRequest);
				URL url;
				try {
					url = new URL(batchResponse.getPartPresignedUrls().get(0).getUploadPresignedUrl());
					// upload the part to the url
					client.putFileToURL(url, partData.getPartFile(), contentType);
					// Add the part to the upload
					client.addPartToMultipartUpload(status.getUploadId(), partData.getPartNumber(), partData.getPartMD5Hex());
				} catch (MalformedURLException e) {
					new RuntimeException(e);
				}
			}
		}
	}

	/**
	 * Delete all created temp files.
	 * @param partDataList
	 */
	public static void deleteTempFiles(List<PartData> partDataList) {
		if(partDataList != null){
			// unconditionally delete temp files
			for (PartData partData : partDataList) {
				try {
					partData.getPartFile().delete();
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * Create all of the parts for a multi-part upload and calculate the MD5 of the file.
	 * @param input
	 * @param fileSizeBytes
	 * @param partSizeBytes
	 * @param numberOfParts
	 * @param partDataList
	 * @return
	 */
	public static String createParts(FileProvider fileProvider, InputStream input, long fileSizeBytes,
			long partSizeBytes, long numberOfParts, List<PartData> partDataList) {
		// digest for the entire file.
		MessageDigest fileMD5Digest = createMD5Digest();
		byte[] buffer = new byte[(int) partSizeBytes];
		int offset = 0;
		int length = (int) partSizeBytes;

		// Stream the parts to temp files and calculate all MD5s
		for (long i = 0; i < numberOfParts; i++) {
			try {
				if (i == numberOfParts - 1) {
					// this is the last part
					int remainder = (int) (fileSizeBytes % partSizeBytes);
					if(remainder > 0){
						length = remainder;
					}else{
						length = (int) partSizeBytes;
					}

				}
				// fill the buffer
				input.read(buffer, offset, length);
				// continue with the entire file
				fileMD5Digest.update(buffer, offset, length);
				// Calculate the MD5 of the part
				String partMD5Hex = calculateMD5Hex(buffer, offset, length);
				File partFile = fileProvider.createTempFile("multipart", ".tmp");
				// write the buffer to a file.
				writeByteArrayToFile(fileProvider, partFile, buffer, offset, length);
				PartData partData = new PartData();
				partData.setPartFile(partFile);
				partData.setPartMD5Hex(partMD5Hex);
				partData.setPartNumber((int)(i+1));
				partDataList.add(partData);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		// All of the part files are created.
		return new String(Hex.encodeHex(fileMD5Digest.digest()));
	}

	/**
	 * Write the given byte array to the passed file.
	 * 
	 * @param file
	 * @param data
	 * @param offset
	 * @param length
	 * @throws IOException
	 */
	public static void writeByteArrayToFile(FileProvider fileProvider, File file, byte[] data, int offset,
			int length) throws IOException {
		OutputStream out = null;
		try {
			out = fileProvider.createFileOutputStream(file);
			out.write(data, offset, length);
		} finally {
			IOUtils.closeQuietly(out);
		}
	}
	
	/**
	 * Calculate the MD5 as hex for the given byte array
	 * @param bytes
	 * @param offset
	 * @param length
	 * @return
	 */
	public static String calculateMD5Hex(byte[] bytes, int offset, int length) {
		MessageDigest digest = createMD5Digest();
		digest.update(bytes, offset, length);
		return new String(Hex.encodeHex(digest.digest()));
	}

	/**
	 * Simple warp to convert exceptions to runtime.
	 * 
	 * @return
	 */
	public static MessageDigest createMD5Digest() {
		try {
			return MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

}
