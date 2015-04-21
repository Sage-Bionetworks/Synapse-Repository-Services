package org.sagebionetworks.repo.manager.file;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.file.transfer.TransferUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ChunkRequest;
import org.sagebionetworks.repo.model.file.ChunkResult;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.file.CompleteChunkedFileRequest;
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.utils.MD5ChecksumHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

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
import com.amazonaws.event.ProgressListener;
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
	
	// [base/]userid/UUID/filename
	public static final String FILE_TOKEN_TEMPLATE_SEPARATOR = "/";
	private static final String FILE_TOKEN_TEMPLATE = "%1$s%2$s" + FILE_TOKEN_TEMPLATE_SEPARATOR + "%3$s" + FILE_TOKEN_TEMPLATE_SEPARATOR
			+ "%4$s";
	
	@Autowired
	AmazonS3Client s3Client;
	@Autowired
	FileHandleDao fileHandleDao;
	@Autowired
	TransferManager transferManager;
	@Autowired
	ProjectSettingsManager projectSettingsManager;

	@Override
	public ChunkResult copyPart(ChunkedFileToken token, int partNumber, Long storageLocationId) throws DatastoreException, NotFoundException {
		// The part number cannot be less than one
		if(partNumber < 1) throw new IllegalArgumentException("partNumber cannot be less than one");
		String partKey = getChunkPartKey(token, partNumber);
		StorageLocationSetting storageLocationSetting = getStorageLocationSetting(storageLocationId);
		// copy this part to the larger file.
		CopyPartRequest copyPartRequest = new CopyPartRequest();
		copyPartRequest.setDestinationBucketName(getBucket(storageLocationSetting));
		copyPartRequest.setDestinationKey(token.getKey());
		copyPartRequest.setPartNumber(partNumber);
		copyPartRequest.setSourceBucketName(getBucket(storageLocationSetting));
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
	public boolean doesPartExist(ChunkedFileToken token, int partNumber, Long storageLocationId) throws DatastoreException, NotFoundException {
		if(partNumber < 1) throw new IllegalArgumentException("partNumber cannot be less than one");
		StorageLocationSetting storageLocationSetting = getStorageLocationSetting(storageLocationId);
		String partKey = getChunkPartKey(token, partNumber);
		try{
			ObjectMetadata meta = s3Client.getObjectMetadata(getBucket(storageLocationSetting), partKey);
			return true;
		}catch (AmazonClientException e){
			return false;
		}
	}

	@Override
	public ChunkedFileToken createChunkedFileUploadToken(CreateChunkedFileTokenRequest ccftr, Long storageLocationId,
 String userId)
			throws DatastoreException, NotFoundException {
		if(ccftr == null) throw new IllegalArgumentException("CreateChunkedFileTokenRequest cannot be null");
		if(ccftr.getFileName() == null) throw new IllegalArgumentException("CreateChunkedFileTokenRequest.fileName cannot be null");
		String contentType = ccftr.getContentType();
		if(contentType == null){
			contentType = "application/octet-stream";
		}
		StorageLocationSetting storageLocationSetting = getStorageLocationSetting(storageLocationId);
		// Start a multi-file upload
		String key = createNewKey(userId, ccftr.getFileName(), storageLocationSetting);
		ObjectMetadata objMeta = new ObjectMetadata();
		objMeta.setContentType(contentType);
		objMeta.setContentDisposition(TransferUtils.getContentDispositionValue(ccftr.getFileName()));
		if(ccftr.getContentMD5() != null){
			// convert it from hex to base64.
			objMeta.setContentMD5(BinaryUtils.toBase64(BinaryUtils.fromHex(ccftr.getContentMD5())));
		}
		InitiateMultipartUploadResult imur = s3Client.initiateMultipartUpload(new InitiateMultipartUploadRequest(
				getBucket(storageLocationSetting), key).withObjectMetadata(objMeta));
		// the token will be the ke
		ChunkedFileToken cft = new ChunkedFileToken();
		cft.setKey(key);
		cft.setUploadId(imur.getUploadId());
		cft.setFileName(ccftr.getFileName());
		cft.setContentType(contentType);
		cft.setContentMD5(ccftr.getContentMD5());
		cft.setStorageLocationId(storageLocationId);
		return cft;
	}
	
	private StorageLocationSetting getStorageLocationSetting(Long storageLocationId) throws DatastoreException, NotFoundException {
		StorageLocationSetting storageLocationSetting = null;
		if (storageLocationId != null) {
			storageLocationSetting = projectSettingsManager.getStorageLocationSetting(storageLocationId);
		}
		return storageLocationSetting;
	}

	/**
	 * Create a new key
	 * @param userId
	 * @param fileName
	 * @return
	 */
	public static String createNewKey(String userId, String fileName, StorageLocationSetting storageLocationSetting) {
		String base = "";
		if (storageLocationSetting instanceof ExternalS3StorageLocationSetting) {
			ExternalS3StorageLocationSetting externalS3StorageLocationSetting = (ExternalS3StorageLocationSetting) storageLocationSetting;
			if (!StringUtils.isEmpty(externalS3StorageLocationSetting.getBaseKey())) {
				base = externalS3StorageLocationSetting.getBaseKey() + FILE_TOKEN_TEMPLATE_SEPARATOR;
			}
		}
		return String.format(FILE_TOKEN_TEMPLATE, base, userId, UUID.randomUUID().toString(), fileName);
	}
	
	private static String getBucket(StorageLocationSetting storageLocationSetting) {
		String bucket;
		if (storageLocationSetting == null || storageLocationSetting instanceof S3StorageLocationSetting) {
			bucket = StackConfiguration.getS3Bucket();
		} else if (storageLocationSetting instanceof ExternalS3StorageLocationSetting) {
			bucket = ((ExternalS3StorageLocationSetting) storageLocationSetting).getBucket();
		} else {
			throw new IllegalArgumentException("Cannot get bucket from storage location setting type " + storageLocationSetting.getClass());
		}
		return bucket;
	}

	@Override
	public String getBucket(Long storageLocationId) throws DatastoreException, NotFoundException {
		StorageLocationSetting storageLocationSetting = getStorageLocationSetting(storageLocationId);
		return getBucket(storageLocationSetting);
	}

	/**
	 * Get the key for a token and a part number
	 */
	@Override
	public String getChunkPartKey(ChunkedFileToken token, int partNumber) {
		return token.getKey()+"/"+partNumber;
	}

	@Override
	public URL createChunkedFileUploadPartURL(ChunkRequest cpr, Long storageLocationId) throws DatastoreException, NotFoundException {
		if(cpr == null) throw new IllegalArgumentException("ChunkedPartRequest cannot be null");
		if(cpr.getChunkedFileToken() == null) throw new IllegalArgumentException("ChunkedPartRequest.chunkedFileToken cannot be null");
		if(cpr.getChunkNumber() == null) throw new IllegalArgumentException("ChunkedPartRequest.chunkNumber cannot be null");
		ChunkedFileToken token = cpr.getChunkedFileToken();
		int partNumber = cpr.getChunkNumber().intValue();
		// The part number cannot be less than one
		if(partNumber < 1) throw new IllegalArgumentException("partNumber cannot be less than one");
		String partKey = getChunkPartKey(token, partNumber);
		// For each block we want to create a pre-signed URL file.
		StorageLocationSetting storageLocationSetting = getStorageLocationSetting(storageLocationId);
		GeneratePresignedUrlRequest gpur = new GeneratePresignedUrlRequest(getBucket(storageLocationSetting), partKey)
				.withMethod(HttpMethod.PUT);
		if(cpr.getChunkedFileToken().getContentType() != null){
			gpur.setContentType(cpr.getChunkedFileToken().getContentType());
		}
		return  s3Client.generatePresignedUrl(gpur);
	}

	@Override
	public S3FileHandle completeChunkFileUpload(CompleteChunkedFileRequest ccfr, Long storageLocationId, String userId)
			throws DatastoreException, NotFoundException {
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
		StorageLocationSetting storageLocationSetting = getStorageLocationSetting(storageLocationId);
		// We are now ready to complete the parts
		CompleteMultipartUploadResult cmp = s3Client.completeMultipartUpload(new CompleteMultipartUploadRequest(
				getBucket(storageLocationSetting), token.getKey(), token.getUploadId(), ptList));
		// Update the metadata
		// The file is now in S3.
		S3FileHandle fileHandle = new S3FileHandle();
		fileHandle.setFileName(token.getFileName());
		fileHandle.setContentType(token.getContentType());
		fileHandle.setBucketName(getBucket(storageLocationSetting));
		fileHandle.setKey(token.getKey());
		fileHandle.setCreatedBy(userId);
		fileHandle.setCreatedOn(new Date(System.currentTimeMillis()));
		fileHandle.setEtag(UUID.randomUUID().toString());
		fileHandle.setContentMd5(token.getContentMD5());
		fileHandle.setStorageLocationId(token.getStorageLocationId());
		// Lookup the final file size
		ObjectMetadata current = s3Client.getObjectMetadata(getBucket(storageLocationSetting), token.getKey());
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
			s3Client.deleteObject(getBucket(storageLocationSetting), partKey);
		}
		return result;
	}

	@Override
	public S3FileHandle multipartUploadLocalFile(Long storageLocationId, String userId, File fileToUpload,
			String contentType, ProgressListener listener) {
		try {
			StorageLocationSetting storageLocationSetting = getStorageLocationSetting(storageLocationId);
			// We let amazon's TransferManager do most of the heavy lifting
			String key = createNewKey(userId, fileToUpload.getName(), storageLocationSetting);
			String md5 = MD5ChecksumHelper.getMD5Checksum(fileToUpload);
			// Start the fileHandle
			// We can now create a FileHandle for this upload
			S3FileHandle handle = new S3FileHandle();
			handle.setBucketName(getBucket(storageLocationSetting));
			handle.setKey(key);
			handle.setContentMd5(md5);
			handle.setContentType(contentType);
			handle.setCreatedBy(userId);
			handle.setCreatedOn(new Date(System.currentTimeMillis()));
			handle.setEtag(UUID.randomUUID().toString());
			handle.setFileName(fileToUpload.getName());
			
			PutObjectRequest por = new PutObjectRequest(getBucket(storageLocationSetting), key, fileToUpload);
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
