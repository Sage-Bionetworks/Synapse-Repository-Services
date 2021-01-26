package org.sagebionetworks.repo.manager.file;

import java.util.Date;
import java.util.UUID;

import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.file.transfer.TransferUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.utils.MD5ChecksumHelper;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;

/**
 * Multi-part implementation.
 * 
 * @author jmhill
 *
 */
public class MultipartManagerImpl implements MultipartManager {
	
	@Autowired
	SynapseS3Client s3Client;
	@Autowired
	FileHandleDao fileHandleDao;
	@Autowired
	TransferManager transferManager;
	@Autowired
	ProjectSettingsManager projectSettingsManager;
	@Autowired
	IdGenerator idGenerator;
	
	private StorageLocationSetting getStorageLocationSetting(Long storageLocationId) throws DatastoreException, NotFoundException {
		StorageLocationSetting storageLocationSetting = null;
		if (storageLocationId != null) {
			storageLocationSetting = projectSettingsManager.getStorageLocationSetting(storageLocationId);
		}
		return storageLocationSetting;
	}

	@Override
	public S3FileHandle multipartUploadLocalFile(LocalFileUploadRequest request) {
		try {
			StorageLocationSetting storageLocationSetting = getStorageLocationSetting(request.getStorageLocationId());
			// If the file name is provide then use it.
			String fileName = request.getFileName();
			if(fileName == null) {
				// use the name of th passed file when the name is null.
				fileName = request.getFileToUpload().getName();
			}
			// We let amazon's TransferManager do most of the heavy lifting
			String key = MultipartUtils.createNewKey(request.getUserId(), fileName, storageLocationSetting);
			String md5 = MD5ChecksumHelper.getMD5Checksum(request.getFileToUpload());
			// Start the fileHandle
			// We can now create a FileHandle for this upload
			S3FileHandle handle = new S3FileHandle();
			handle.setBucketName(MultipartUtils.getBucket(storageLocationSetting));
			handle.setKey(key);
			handle.setContentMd5(md5);
			handle.setContentType(request.getContentType());
			handle.setCreatedBy(request.getUserId());
			handle.setCreatedOn(new Date(System.currentTimeMillis()));
			handle.setEtag(UUID.randomUUID().toString());
			handle.setFileName(fileName);
			handle.setStorageLocationId(request.getStorageLocationId());

			PutObjectRequest por = new PutObjectRequest(MultipartUtils.getBucket(storageLocationSetting), key, request.getFileToUpload());
			ObjectMetadata meta = TransferUtils.prepareObjectMetadata(handle);
			por.setMetadata(meta);
			Upload upload = transferManager.upload(por);
			// Make sure the caller can watch the progress.
			upload.addProgressListener(request.getListener());
			// This will throw an exception if the upload fails for any reason.
			UploadResult results = upload.waitForUploadResult();
			// get the metadata for this file.
			meta = this.s3Client.getObjectMetadata(results.getBucketName(), results.getKey());
			handle.setContentSize(meta.getContentLength());

			handle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
			// Save the file handle
			handle = (S3FileHandle) fileHandleDao.createFile(handle);
			// done
			return handle;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} 
	}

}
