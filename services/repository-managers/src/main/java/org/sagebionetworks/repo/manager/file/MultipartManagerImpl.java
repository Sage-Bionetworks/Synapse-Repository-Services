package org.sagebionetworks.repo.manager.file;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.sagebionetworks.repo.manager.file.transfer.TransferUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ChunkRequest;
import org.sagebionetworks.repo.model.file.ChunkResult;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.file.CompleteChunkedFileRequest;
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.utils.MD5ChecksumHelper;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.AmazonClientException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.CopyPartResult;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.ProgressListener;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import com.amazonaws.util.BinaryUtils;

/**
 * Multi-part implementation.
 * 
 * @author jmhill
 *
 */
public class MultipartManagerImpl implements MultipartManager {
	
	private static String FILE_TOKEN_TEMPLATE = "%1$s/%2$s/%3$s"; // userid/UUID/filename
	
	@Autowired
	AmazonS3Client s3Client;
	@Autowired
	FileHandleDao fileHandleDao;
	@Autowired
	TransferManager transferManager;

	@Override
	public ChunkResult copyPart(ChunkedFileToken token, int partNumber,	String bucket) {
		// The part number cannot be less than one
		if(partNumber < 1) throw new IllegalArgumentException("partNumber cannot be less than one");
		String partKey = getChunkPartKey(token, partNumber);
		// copy this part to the larger file.
		CopyPartRequest copyPartRequest = new CopyPartRequest();
		copyPartRequest.setDestinationBucketName(bucket);
		copyPartRequest.setDestinationKey(token.getKey());
		copyPartRequest.setPartNumber(partNumber);
		copyPartRequest.setSourceBucketName(bucket);
		copyPartRequest.setSourceKey(partKey);
		copyPartRequest.setUploadId(token.getUploadId());
		// copy the part
		CopyPartResult result = s3Client.copyPart(copyPartRequest);
		ChunkResult cp = new ChunkResult();
		cp.setEtag(result.getETag());
		cp.setChunkNumber((long) result.getPartNumber());
		return cp;
	}

	@Override
	public boolean doesPartExist(ChunkedFileToken token, int partNumber, String bucket) {
		if(partNumber < 1) throw new IllegalArgumentException("partNumber cannot be less than one");
		String partKey = getChunkPartKey(token, partNumber);
		try{
			ObjectMetadata meta = s3Client.getObjectMetadata(bucket, partKey);
			return true;
		}catch (AmazonClientException e){
			return false;
		}
	}

	@Override
	public ChunkedFileToken createChunkedFileUploadToken(CreateChunkedFileTokenRequest ccftr, String bucket, String userId) {
		if(ccftr == null) throw new IllegalArgumentException("CreateChunkedFileTokenRequest cannot be null");
		if(ccftr.getFileName() == null) throw new IllegalArgumentException("CreateChunkedFileTokenRequest.fileName cannot be null");
		String contentType = ccftr.getContentType();
		if(contentType == null){
			contentType = "application/octet-stream";
		}
		// Start a multi-file upload
		String key = createNewKey(userId, ccftr.getFileName());
		ObjectMetadata objMeta = new ObjectMetadata();
		objMeta.setContentType(contentType);
		objMeta.setContentDisposition(TransferUtils.getContentDispositionValue(ccftr.getFileName()));
		if(ccftr.getContentMD5() != null){
			// convert it from hex to base64.
			objMeta.setContentMD5(BinaryUtils.toBase64(BinaryUtils.fromHex(ccftr.getContentMD5())));
		}
		InitiateMultipartUploadResult imur = s3Client.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucket, key).withObjectMetadata(objMeta));
		// the token will be the ke
		ChunkedFileToken cft = new ChunkedFileToken();
		cft.setKey(key);
		cft.setUploadId(imur.getUploadId());
		cft.setFileName(ccftr.getFileName());
		cft.setContentType(contentType);
		cft.setContentMD5(ccftr.getContentMD5());
		return cft;
	}
	
	/**
	 * Create a new key
	 * @param userId
	 * @param fileName
	 * @return
	 */
	public static String createNewKey(String userId, String fileName) {
		return String.format(FILE_TOKEN_TEMPLATE, userId, UUID.randomUUID().toString(), fileName);
	}
	
	/**
	 * Get the key for a token and a part number
	 */
	@Override
	public String getChunkPartKey(ChunkedFileToken token, int partNumber) {
		return token.getKey()+"/"+partNumber;
	}

	@Override
	public URL createChunkedFileUploadPartURL(ChunkRequest cpr, String bucket) {
		if(cpr == null) throw new IllegalArgumentException("ChunkedPartRequest cannot be null");
		if(cpr.getChunkedFileToken() == null) throw new IllegalArgumentException("ChunkedPartRequest.chunkedFileToken cannot be null");
		if(cpr.getChunkNumber() == null) throw new IllegalArgumentException("ChunkedPartRequest.chunkNumber cannot be null");
		ChunkedFileToken token = cpr.getChunkedFileToken();
		int partNumber = cpr.getChunkNumber().intValue();
		// The part number cannot be less than one
		if(partNumber < 1) throw new IllegalArgumentException("partNumber cannot be less than one");
		String partKey = getChunkPartKey(token, partNumber);
		// For each block we want to create a pre-signed URL file.
		GeneratePresignedUrlRequest gpur = new GeneratePresignedUrlRequest(bucket, partKey).withMethod(HttpMethod.PUT);
		if(cpr.getChunkedFileToken().getContentType() != null){
			gpur.setContentType(cpr.getChunkedFileToken().getContentType());
		}
		return  s3Client.generatePresignedUrl(gpur);
	}

	@Override
	public S3FileHandle completeChunkFileUpload(CompleteChunkedFileRequest ccfr, String bucket, String userId) {
		if(ccfr == null) throw new IllegalArgumentException("CompleteChunkedFileRequest cannot be null");
		ChunkedFileToken token = ccfr.getChunkedFileToken();
		List<ChunkResult> chunkParts = ccfr.getChunkResults();
		if(chunkParts == null) throw new IllegalArgumentException("ChunkParts cannot be null");
		if(chunkParts.size() < 1) throw new IllegalArgumentException("ChunkParts.getList() must contain at least one ChunkPart");
		// Create the list of PartEtags
		List<PartETag> ptList = new LinkedList<PartETag>();
		for(ChunkResult cp: chunkParts){
			if(cp == null) 	throw new IllegalArgumentException("ChunkPart cannot be null");
			if(cp.getEtag() == null) throw new IllegalArgumentException("ChunkPart.getEtag() cannot be null");
			if(cp.getChunkNumber() == null) throw new IllegalArgumentException("ChunkPart.chunkNumber() cannot be null");
			PartETag pe = new PartETag(cp.getChunkNumber().intValue(), cp.getEtag());
			ptList.add(pe);
		}
		// We are now ready to complete the parts
		CompleteMultipartUploadResult cmp = s3Client.completeMultipartUpload(new CompleteMultipartUploadRequest(bucket, token.getKey(), token.getUploadId(), ptList));
		// Update the metadata
		// The file is now in S3.
		S3FileHandle fileHandle = new S3FileHandle();
		fileHandle.setFileName(token.getFileName());
		fileHandle.setContentType(token.getContentType());
		fileHandle.setBucketName(bucket);
		fileHandle.setKey(token.getKey());
		fileHandle.setCreatedBy(userId);
		fileHandle.setCreatedOn(new Date(System.currentTimeMillis()));
		fileHandle.setEtag(UUID.randomUUID().toString());
		fileHandle.setContentMd5(token.getContentMD5());
		// Lookup the final file size
		ObjectMetadata current = s3Client.getObjectMetadata(bucket, token.getKey());
		// Capture the content length
		fileHandle.setContentSize(current.getContentLength());
		
		// By default, previews are generated
		if (ccfr.getShouldPreviewBeGenerated() == null) {
			ccfr.setShouldPreviewBeGenerated(true);
		}
		
		S3FileHandle result = fileHandleDao.createFile(fileHandle, ccfr.getShouldPreviewBeGenerated());
		// Upon success we need to delete all of the parts.
		for(ChunkResult cp: chunkParts){
			String partKey = getChunkPartKey(token, cp.getChunkNumber().intValue());
			s3Client.deleteObject(bucket, partKey);
		}
		return result;
	}

	@Override
	public S3FileHandle multipartUploadLocalFile(String bucket, String userId, File fileToUpload, String contentType, ProgressListener listener) {
		try {
			// We let amazon's TransferManager do most of the heavy lifting
			String key = createNewKey(userId, fileToUpload.getName());
			String md5 = MD5ChecksumHelper.getMD5Checksum(fileToUpload);
			// Start the fileHandle
			// We can now create a FileHandle for this upload
			S3FileHandle handle = new S3FileHandle();
			handle.setBucketName(bucket);
			handle.setKey(key);
			handle.setContentMd5(md5);
			handle.setContentType(contentType);
			handle.setCreatedBy(userId);
			handle.setCreatedOn(new Date(System.currentTimeMillis()));
			handle.setEtag(UUID.randomUUID().toString());
			handle.setFileName(fileToUpload.getName());
			
			PutObjectRequest por = new PutObjectRequest(bucket, key, fileToUpload);
			ObjectMetadata meta = TransferUtils.prepareObjectMetadata(handle);
			por.setMetadata(meta);
			Upload upload = transferManager.upload(por);
			// Make sure the caller can watch the progress.
			upload.addProgressListener(listener);
			// This will throw an exception if the upload fails for any reason.
			UploadResult results = upload.waitForUploadResult();
			// get the metadata for this file.
			meta = this.s3Client.getObjectMetadata(results.getBucketName(), results.getKey());
			handle.setContentSize(meta.getContentLength());

			// Save the file handle
			handle = fileHandleDao.createFile(handle);
			// done
			return handle;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} 
	}

}
