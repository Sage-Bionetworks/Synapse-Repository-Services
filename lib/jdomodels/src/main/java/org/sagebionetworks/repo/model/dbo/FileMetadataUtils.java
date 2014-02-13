package org.sagebionetworks.repo.model.dbo;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;

import org.sagebionetworks.repo.model.backup.FileHandleBackup;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle.MetadataType;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandleInterface;

/**
 * Translates between DBOs and DTOs.
 * @author John
 *
 */
public class FileMetadataUtils {
	
	/**
	 * Convert abstract DTO to the DBO.
	 * @param dto
	 * @return
	 * @throws MalformedURLException 
	 */
	public static DBOFileHandle createDBOFromDTO(FileHandle dto) {
		if(dto == null) throw new IllegalArgumentException("DTO cannot be null");
		if(dto instanceof ExternalFileHandle){
			return createDBOFromDTO((ExternalFileHandle)dto);
		}else if(dto instanceof S3FileHandle){
			return createDBOFromDTO((S3FileHandle)dto);
		}else if(dto instanceof PreviewFileHandle){
			return createDBOFromDTO((PreviewFileHandle)dto);
		}else{
			throw new IllegalArgumentException("Unknown FileMetadata implementaion: "+dto.getClass().getName());
		}
	}
	
	/**
	 * Convert from the DTO to the DBO.
	 * @param dto
	 * @return
	 * @throws MalformedURLException 
	 */
	private static DBOFileHandle createDBOFromDTO(ExternalFileHandle dto) {
		DBOFileHandle dbo = new DBOFileHandle();
		dbo.setMetadataType(MetadataType.EXTERNAL);
		// Validate the URL
		try {
			// Make sure it is really a URL.
			new URL(dto.getExternalURL());
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
		dbo.setKey(dto.getExternalURL());
		dbo.setEtag(dto.getEtag());
		if(dto.getCreatedBy() != null){
			dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		}
		if(dto.getPreviewId() != null){
			dbo.setPreviewId(Long.parseLong(dto.getPreviewId()));
		}
		if(dto.getId() != null){
			dbo.setId(Long.parseLong(dto.getId()));
		}
		if(dto.getCreatedOn() != null){
			dbo.setCreatedOn(new Timestamp(dto.getCreatedOn().getTime()));
		}
		dbo.setName(dto.getFileName());
		dbo.setContentType(dto.getContentType());
		return dbo;
	}
	
	/**
	 * Convert from the DTO to the DBO.
	 * @param dto
	 * @return
	 */
	private static DBOFileHandle createDBOFromDTO(S3FileHandle dto){
		DBOFileHandle dbo = new DBOFileHandle();
		dbo.setMetadataType(MetadataType.S3);
		if(dto.getPreviewId() != null){
			dbo.setPreviewId(new Long(dto.getPreviewId()));
		}
		// Fill in the common data.
		setDBOFromDTO(dbo, dto);
		return dbo;
	}
	
	private static DBOFileHandle createDBOFromDTO(PreviewFileHandle dto){
		DBOFileHandle dbo = new DBOFileHandle();
		dbo.setMetadataType(MetadataType.PREVIEW);
		// Fill in the common data.
		setDBOFromDTO(dbo, dto);
		return dbo;
	}
	/**
	 * Fill in the data common to all S3FileInterface implementations.
	 * @param dbo
	 * @param dto
	 */
	private static void setDBOFromDTO(DBOFileHandle dbo, S3FileHandleInterface dto){
		if(dto.getId() != null){
			dbo.setId(new Long(dto.getId()));
		}
		dbo.setEtag(dto.getEtag());
		dbo.setBucketName(dto.getBucketName());
		dbo.setKey(dto.getKey());
		dbo.setContentMD5(dto.getContentMd5());
		dbo.setContentSize(dto.getContentSize());
		dbo.setContentType(dto.getContentType());
		if(dto.getCreatedBy() != null){
			dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		}
		if(dto.getCreatedOn() != null){
			dbo.setCreatedOn(new Timestamp(dto.getCreatedOn().getTime()));
		}
		dbo.setName(dto.getFileName());
	}
	
	/**
	 * Create a DTO from the DBO.
	 * @param dbo
	 * @return
	 */
	public static FileHandle createDTOFromDBO(DBOFileHandle dbo){
		// First determine the type
		if(MetadataType.EXTERNAL == dbo.getMetadataTypeEnum()){
			// External
			ExternalFileHandle external = new ExternalFileHandle();
			if(dbo.getCreatedBy() != null){
				external.setCreatedBy(dbo.getCreatedBy().toString());
			}
			external.setCreatedOn(dbo.getCreatedOn());
			if(dbo.getPreviewId() != null){
				external.setPreviewId(dbo.getPreviewId().toString());
			}
			if(dbo.getId() != null){
				external.setId(dbo.getId().toString());
			}
			external.setEtag(dbo.getEtag());
			external.setExternalURL(dbo.getKey());
			external.setFileName(dbo.getName());
			external.setContentType(dbo.getContentType());
			return external;
		}else if(MetadataType.S3 == dbo.getMetadataTypeEnum() || MetadataType.PREVIEW == dbo.getMetadataTypeEnum()){
			S3FileHandleInterface metaInterface = null;
			// Is this a S3 file or a preview.
			if(MetadataType.S3 == dbo.getMetadataTypeEnum()){
				S3FileHandle meta = new S3FileHandle();
				metaInterface = meta;
				if(dbo.getPreviewId() != null){
					meta.setPreviewId(dbo.getPreviewId().toString());
				}
			}else if(MetadataType.PREVIEW == dbo.getMetadataTypeEnum()){
				PreviewFileHandle meta = new PreviewFileHandle();
				metaInterface = meta;
			}else{
				throw new IllegalArgumentException("Must be S3 or Preview but was: "+dbo.getMetadataTypeEnum());
			}
			// Set the common data.
			if(dbo.getId() != null){
				metaInterface.setId(dbo.getId().toString());
			}
			metaInterface.setBucketName(dbo.getBucketName());
			metaInterface.setKey(dbo.getKey());
			metaInterface.setContentMd5(dbo.getContentMD5());
			metaInterface.setContentType(dbo.getContentType());
			metaInterface.setContentSize(dbo.getContentSize());
			metaInterface.setFileName(dbo.getName());
			metaInterface.setEtag(dbo.getEtag());
			if(dbo.getCreatedBy() != null){
				metaInterface.setCreatedBy(dbo.getCreatedBy().toString());
			}
			metaInterface.setCreatedOn(dbo.getCreatedOn());
			return metaInterface;
		}else{
			throw new IllegalArgumentException("Unknown metadata type: "+dbo.getMetadataTypeEnum());
		}
	}
	
	/**
	 * Create a backup copy of a DBO object
	 * @param dbo
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
		if(in.getMetadataTypeEnum() != null){
			out.setMetadataType(in.getMetadataType());
		}
		if(in.getName() != null){
			out.setName(in.getName());
		}
		if(in.getPreviewId() != null){
			out.setPreviewId(in.getPreviewId());
		}
		return out;
	}
	
	/**
	 * Create a DTO from a backup object.
	 * @param backup
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
		if(in.getMetadataType() != null){
			out.setMetadataType(MetadataType.valueOf(in.getMetadataType()));
		}
		if(in.getName() != null){
			out.setName(in.getName());
		}
		if(in.getPreviewId() != null){
			out.setPreviewId(in.getPreviewId());
		}
		return out;
	}

}
