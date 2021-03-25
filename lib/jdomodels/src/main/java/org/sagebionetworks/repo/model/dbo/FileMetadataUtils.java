package org.sagebionetworks.repo.model.dbo;

import java.net.MalformedURLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationImpl;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.backup.FileHandleBackup;
import org.sagebionetworks.repo.model.dao.FileHandleMetadataType;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.ExternalObjectStoreFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Translates between DBOs and DTOs.
 * @author John
 *
 */
public class FileMetadataUtils {
	
	static final String DEFAULT_S3_BUCKET = StackConfigurationSingleton.singleton().getS3Bucket();

	/**
	 * Convert abstract DTO to the DBO.
	 * @return
	 * @throws MalformedURLException
	 */
	public static DBOFileHandle createDBOFromDTO(FileHandle fileHandle) {
		ValidateArgument.required(fileHandle, "The fileHandle");
		
		DBOFileHandle dbo = new DBOFileHandle();

		/** Previews can only be set by calling {@link org.sagebionetworks.repo.model.dbo.dao.DBOFileHandleDaoImpl#setPreviewId} */
		dbo.setIsPreview(false);

		if (fileHandle instanceof ExternalFileHandle) {
			dbo.setMetadataType(FileHandleMetadataType.EXTERNAL);
			updateDBOFromDTO(dbo, (ExternalFileHandle) fileHandle);
		} else if (fileHandle instanceof S3FileHandle) {
			dbo.setMetadataType(FileHandleMetadataType.S3);
		} else if (fileHandle instanceof GoogleCloudFileHandle) {
			dbo.setMetadataType(FileHandleMetadataType.GOOGLE_CLOUD);
		} else if (fileHandle instanceof ProxyFileHandle) {
			dbo.setMetadataType(FileHandleMetadataType.PROXY);
		}else if (fileHandle instanceof ExternalObjectStoreFileHandle){
			dbo.setMetadataType(FileHandleMetadataType.EXTERNAL_OBJ_STORE);
		}else {
			throw new IllegalArgumentException("Unhandled file handle type: " + fileHandle.getClass().getName());
		}

		updateDBOFromDTO(dbo, fileHandle);
		if (fileHandle instanceof CloudProviderFileHandleInterface) {
			updateDBOFromDTO(dbo, (CloudProviderFileHandleInterface) fileHandle);
		}
		if(fileHandle instanceof ProxyFileHandle){
			updateDBOFromDTO(dbo, (ProxyFileHandle) fileHandle);
		}
		if(fileHandle instanceof ExternalObjectStoreFileHandle){
			updateDBOFromDTO(dbo, (ExternalObjectStoreFileHandle) fileHandle);
		}

		return dbo;
	}

	private static void updateDBOFromDTO(DBOFileHandle dbo, FileHandle fileHandle) {
		dbo.setEtag(fileHandle.getEtag());
		if (fileHandle.getCreatedBy() != null) {
			dbo.setCreatedBy(Long.parseLong(fileHandle.getCreatedBy()));
		}
		if (fileHandle.getId() != null) {
			dbo.setId(Long.parseLong(fileHandle.getId()));
		}
		if (fileHandle.getCreatedOn() != null) {
			dbo.setCreatedOn(new Timestamp(fileHandle.getCreatedOn().getTime()));
		} else {
			dbo.setCreatedOn(new Timestamp(System.currentTimeMillis()));
		}
		dbo.setName(fileHandle.getFileName());
		dbo.setStorageLocationId(fileHandle.getStorageLocationId());
		dbo.setContentType(fileHandle.getContentType());
		dbo.setContentSize(fileHandle.getContentSize());
		dbo.setContentMD5(fileHandle.getContentMd5());
	}

	private static void updateDBOFromDTO(DBOFileHandle dbo, ExternalFileHandle fileHandle) {
		// Validate the URL
		ValidateArgument.validExternalUrl(fileHandle.getExternalURL());
		dbo.setKey(fileHandle.getExternalURL());
	}

	private static void updateDBOFromDTO(DBOFileHandle dbo, CloudProviderFileHandleInterface fileHandle) {
		if (fileHandle.getPreviewId() != null) {
			dbo.setPreviewId(Long.parseLong(fileHandle.getPreviewId()));
		}
		dbo.setBucketName(fileHandle.getBucketName());
		dbo.setKey(fileHandle.getKey());
		dbo.setContentSize(fileHandle.getContentSize());
	}

	private static void updateDBOFromDTO(DBOFileHandle dbo, ProxyFileHandle fileHandle) {
		dbo.setKey(fileHandle.getFilePath());
		dbo.setContentSize(fileHandle.getContentSize());
	}

	private static void updateDBOFromDTO(DBOFileHandle dbo, ExternalObjectStoreFileHandle fileHandle){
		dbo.setKey(fileHandle.getFileKey());
		dbo.setContentSize(fileHandle.getContentSize());
		dbo.setEndpoint(fileHandle.getEndpointUrl());
		dbo.setBucketName(fileHandle.getBucket());
	}

	/**
	 * Create a DTO from the DBO.
	 *
	 * @param dbo
	 * @return
	 */
	public static FileHandle createDTOFromDBO(DBOFileHandle dbo) {
		FileHandle fileHandle;
		// First determine the type and create the correct type
		switch (dbo.getMetadataTypeEnum()) {
		case EXTERNAL:
			// External
			fileHandle = new ExternalFileHandle();
			break;
		case S3:
			fileHandle = new S3FileHandle();
			break;
		case GOOGLE_CLOUD:
			fileHandle = new GoogleCloudFileHandle();
			break;
		case PROXY:
			// proxy
			fileHandle = new ProxyFileHandle();
			break;
		case EXTERNAL_OBJ_STORE:
			fileHandle = new ExternalObjectStoreFileHandle();
			break;
		default:
			throw new IllegalArgumentException("Must be EXTERNAL, S3, GOOGLE_CLOUD, PROXY, EXTERNAL_OBJ_STORE but was: " + dbo.getMetadataTypeEnum());
		}

		// now fill in the information
		updateDTOFromDBO(fileHandle, dbo);
		if (fileHandle instanceof CloudProviderFileHandleInterface) {
			updateDTOFromDBO((CloudProviderFileHandleInterface) fileHandle, dbo);
		}
		if (fileHandle instanceof ExternalFileHandle) {
			updateDTOFromDBO((ExternalFileHandle) fileHandle, dbo);
		}
		if (fileHandle instanceof ProxyFileHandle) {
			updateDTOFromDBO((ProxyFileHandle) fileHandle, dbo);
		}
		if (fileHandle instanceof ExternalObjectStoreFileHandle) {
			updateDTOFromDBO((ExternalObjectStoreFileHandle) fileHandle, dbo);
		}
		return fileHandle;
	}

	private static void updateDTOFromDBO(FileHandle fileHandle, DBOFileHandle dbo) {
		if (dbo.getCreatedBy() != null) {
			fileHandle.setCreatedBy(dbo.getCreatedBy().toString());
		}
		fileHandle.setCreatedOn(dbo.getCreatedOn());
		if (dbo.getId() != null) {
			fileHandle.setId(dbo.getId().toString());
		}
		fileHandle.setEtag(dbo.getEtag());
		fileHandle.setStorageLocationId(dbo.getStorageLocationId());
		fileHandle.setContentType(dbo.getContentType());
		fileHandle.setContentMd5(dbo.getContentMD5());
		fileHandle.setContentSize(dbo.getContentSize());
		fileHandle.setFileName(dbo.getName());
	}

	private static void updateDTOFromDBO(ExternalFileHandle fileHandle, DBOFileHandle dbo) {
		fileHandle.setExternalURL(dbo.getKey());
	}

	private static void updateDTOFromDBO(CloudProviderFileHandleInterface fileHandle, DBOFileHandle dbo) {
		if (dbo.getPreviewId() != null) {
			fileHandle.setPreviewId(dbo.getPreviewId().toString());
		}
		fileHandle.setIsPreview(dbo.getIsPreview());
		fileHandle.setBucketName(dbo.getBucketName());
		fileHandle.setKey(dbo.getKey());
		fileHandle.setContentSize(dbo.getContentSize());
	}

	private static void updateDTOFromDBO(ProxyFileHandle fileHandle, DBOFileHandle dbo) {
		fileHandle.setFilePath(dbo.getKey());
	}

	private static void updateDTOFromDBO(ExternalObjectStoreFileHandle fileHandle, DBOFileHandle dbo) {
		fileHandle.setFileKey(dbo.getKey());
		fileHandle.setContentSize(dbo.getContentSize());
		fileHandle.setEndpointUrl(dbo.getEndpoint());
		fileHandle.setBucket(dbo.getBucketName());
	}

	/**
	 * Create a backup copy of a DBO object
	 * @return
	 */
	public static FileHandleBackup createBackupFromDBO(DBOFileHandle in){
		FileHandleBackup out = new FileHandleBackup();
		if(in.getBucketName() != null){
			out.setBucketName(in.getBucketName());
		}
		if(in.getContentMD5() != null){
			out.setContentMD5(in.getContentMD5());
		}
		if(in.getContentSize() != null){
			out.setContentSize(in.getContentSize());
		}
		if(in.getContentType() != null){
			out.setContentType(in.getContentType());
		}
		if(in.getCreatedBy() != null){
			out.setCreatedBy(in.getCreatedBy());
		}
		if(in.getCreatedOn() != null){
			out.setCreatedOn(in.getCreatedOn().getTime());
		}
		if(in.getEtag() != null){
			out.setEtag(in.getEtag());
		}
		if(in.getId() != null){
			out.setId(in.getId());
		}
		if(in.getKey() != null){
			out.setKey(in.getKey());
		}
		if (in.getMetadataTypeEnum() != null) {
			out.setMetadataType(in.getMetadataTypeEnum().name());
		}
		if(in.getName() != null){
			out.setName(in.getName());
		}
		if(in.getPreviewId() != null){
			out.setPreviewId(in.getPreviewId());
		}
		if (in.getStorageLocationId() != null) {
			out.setStorageLocationId(in.getStorageLocationId());
		}
		if (in.getEndpoint() != null){
			out.setEndpoint(in.getEndpoint());
		}
		if (in.getIsPreview() != null){
			out.setIsPreview(in.getIsPreview());
		}

		return out;
	}

	/**
	 * Create a DTO from a backup object.
	 * @return
	 */
	public static DBOFileHandle createDBOFromBackup(FileHandleBackup in){
		DBOFileHandle out = new DBOFileHandle();
		if(in.getBucketName() != null){
			out.setBucketName(in.getBucketName());
		}
		if(in.getContentMD5() != null){
			out.setContentMD5(in.getContentMD5());
		}
		if(in.getContentSize() != null){
			out.setContentSize(in.getContentSize());
		}
		if(in.getContentType() != null){
			out.setContentType(in.getContentType());
		}
		if(in.getCreatedBy() != null){
			out.setCreatedBy(in.getCreatedBy());
		}
		if(in.getCreatedOn() != null){
			out.setCreatedOn(new Timestamp(in.getCreatedOn()));
		}
		if(in.getEtag() != null){
			out.setEtag(in.getEtag());
		}
		if(in.getId() != null){
			out.setId(in.getId());
		}
		if(in.getKey() != null){
			out.setKey(in.getKey());
		}
		if(in.getMetadataType() != null) {
			out.setMetadataType(FileHandleMetadataType.valueOf(in.getMetadataType()));
		}
		if(in.getName() != null){
			out.setName(in.getName());
		}
		if(in.getPreviewId() != null){
			out.setPreviewId(in.getPreviewId());
		}
		if (in.getStorageLocationId() != null) {
			out.setStorageLocationId(in.getStorageLocationId());
		} 
		// Backfill the default storage location, see PLFM-6637
		else if (FileHandleMetadataType.S3.equals(out.getMetadataTypeEnum()) && DEFAULT_S3_BUCKET.equals(out.getBucketName())) {
			out.setStorageLocationId(StorageLocationDAO.DEFAULT_STORAGE_LOCATION_ID);
		}
		if (in.getEndpoint() != null) {
			out.setEndpoint(in.getEndpoint());
		}
		if (in.getIsPreview() != null) {
			out.setIsPreview(in.getIsPreview());
		} else {
			out.setIsPreview(false);
		}
		return out;
	}

	public static List<DBOFileHandle> createDBOsFromDTOs(List<FileHandle> dtos) {
		ValidateArgument.required(dtos, "dtos");
		List<DBOFileHandle> dbos = new ArrayList<>(dtos.size());
		for (FileHandle dto : dtos){
			dbos.add(createDBOFromDTO(dto));
		}
		return dbos;
	}

}
