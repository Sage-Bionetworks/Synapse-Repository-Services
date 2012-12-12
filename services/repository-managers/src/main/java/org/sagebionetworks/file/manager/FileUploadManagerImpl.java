package org.sagebionetworks.file.manager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileMetadataDao;
import org.sagebionetworks.repo.model.file.S3FileMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.amazonaws.util.BinaryUtils;

/**
 * Basic implementation of the file upload manager.
 * 
 * @author John
 *
 */
public class FileUploadManagerImpl implements FileUploadManager {
	
	@Autowired
	AmazonS3Client s3Client;
	
	@Autowired
	FileMetadataDao fileMetadataDao;
	
	/**
	 * Used by spring
	 */
	public FileUploadManagerImpl(){
		super();
	}
	/**
	 * Used for unit tests.
	 * @param s3Client
	 * @param fileMetadataDao
	 */
	public FileUploadManagerImpl(AmazonS3Client s3Client, FileMetadataDao fileMetadataDao) {
		super();
		this.s3Client = s3Client;
		this.fileMetadataDao = fileMetadataDao;
	}

	static private Log log = LogFactory.getLog(FileUploadManagerImpl.class);
	
	private static String FILE_TOKEN_TEMPLATE = "%1$s/%2$s/%3$s"; // userid/UUID/filename

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public FileUploadResults uploadfiles(UserInfo userInfo,	Set<String> expectedParams, FileItemIterator itemIterator, long contentLength) throws FileUploadException, IOException {
		if(userInfo == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(expectedParams == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(itemIterator == null) throw new IllegalArgumentException("FileItemIterator cannot be null");
		FileUploadResults results = new FileUploadResults();
		// Upload all of the files
		// Before we try to read any files make sure we have all of the expected parameters.
		Set<String> expectedCopy = new HashSet<String>(expectedParams);
		while(itemIterator.hasNext()){
			FileItemStream fis = itemIterator.next();
			if(fis.isFormField()){
				// This is a parameter
				// By removing it from the set we indicate that it was found.
				expectedCopy.remove(fis.getFieldName());
				// Map parameter in the results
				results.getParameters().put(fis.getFieldName(), Streams.asString(fis.openStream()));
			}else{
				// This is a file
				if(!expectedCopy.isEmpty()){
					// We are missing some required parameters
					throw new IllegalArgumentException("Missing one or more of the expected form fields: "+expectedCopy);
				}
				// Create a token for this file
				S3FileMetadata fileData = createMetadata(fis.getContentType(), userInfo.getIndividualGroup().getId(), fis.getName());

				// Upload the file to S3
				ObjectMetadata objectMeta = new ObjectMetadata();
				objectMeta.setContentLength(contentLength);
				PutObjectRequest por = new PutObjectRequest(fileData.getBucketName(), fileData.getKey(), fis.openStream(), objectMeta);
				PutObjectResult response = s3Client.putObject(por);
				
				// Fill in the rest of the data
				
				results.getFiles().add(fileData);
			}
		}
		if(log.isDebugEnabled()){
			log.debug(results);
		}
		return results;
	}
	
	/**
	 * Build up the S3FileMetadata.
	 * @param contentType
	 * @param userId
	 * @param fileName
	 * @return
	 */
	public static S3FileMetadata createMetadata(String contentType, String userId, String fileName){
		// Create a token for this file
		S3FileMetadata fileData = new S3FileMetadata();
		fileData.setContentType(contentType);
		fileData.setBucketName(StackConfiguration.getS3Bucket());
		fileData.setCreatedBy(userId);
		fileData.setKey(String.format(FILE_TOKEN_TEMPLATE, userId, UUID.randomUUID().toString(), fileName));
		return fileData;
	}
	
	/**
	 * Upload a file to S3 as a multi-part upload.
	 * Note: We cannot simply pass the InputStream to the AmazonS3Client.putObject() because it will close the stream.  
	 * Since the stream is actually part of the HTTP request, closing it puts the entire r
	 * @param metadata
	 * @param in
	 * @throws IOException 
	 */
	public void uploadFileAsMultipart(S3FileMetadata metadata, InputStream in, int bufferSize) throws IOException{
		// Start a multipart upload
		InitiateMultipartUploadResult initiate = s3Client.initiateMultipartUpload(new InitiateMultipartUploadRequest(metadata.getBucketName(), metadata.getKey()));
		// Now read the stream
		// each part upload.
		// Use the digest to calcuate the MD5
		MessageDigest fullDigest = null;
		MessageDigest partDigest = null;
		try {
			fullDigest =  MessageDigest.getInstance("MD5");
			partDigest =  MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
		List<PartETag> partETags = new ArrayList<PartETag>();
		byte[] buffer = new byte[bufferSize];
		int read = -1;
		int partNumber = 1;
		long contentSize = 0;
		while ((read = in.read(buffer)) > 0) {
			// Update the MD5 calculation of the entire file
			fullDigest.update(buffer, 0, read);
			// Calculate the MD5 of the part
			partDigest.reset();
			partDigest.update(buffer, 0, read);
			String partMD5 = BinaryUtils.toBase64(partDigest.digest());
			// Upload this part
			ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
			UploadPartResult partResult = s3Client
					.uploadPart(new UploadPartRequest()
							.withUploadId(initiate.getUploadId())
							.withBucketName(initiate.getBucketName())
							.withKey(initiate.getKey())
							.withPartSize(read)
							.withMD5Digest(partMD5)
							.withInputStream(bais).withPartNumber(partNumber));
			// Add to the list of etags
			partETags.add(partResult.getPartETag());
			// Increment the part number
			partNumber++;
			contentSize += read;
		}
		// Get the MD5
		String contentMd5 = BinaryUtils.toHex(fullDigest.digest());
		// Complete the Upload
		CompleteMultipartUploadResult result = s3Client.completeMultipartUpload(new CompleteMultipartUploadRequest(
						initiate.getBucketName(), initiate.getKey(), initiate
								.getUploadId(), partETags));
		// Make sure the MD5 and ETag match
//		if(!contentMd5.equals(result.getETag())){
//			throw new IOException("Failed to upload file.  The calculated MD5: "+contentMd5+" did not match the S3 etag: "+result.getETag());
//		}
		// Is the file ready?
//		ObjectMetadata meta = s3Client.getObjectMetadata(initiate.getBucketName(), initiate.getKey());
		// Set the MD5 Of the file.
		metadata.setContentMd5(contentMd5);
		// Set the file size
		metadata.setContentSize(contentSize);
	}
	


}
