package org.sagebionetworks.repo.model.dbo;

import java.sql.Timestamp;

import org.sagebionetworks.repo.model.dbo.persistence.DBOFileMetadata;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileMetadata.MetadataType;
import org.sagebionetworks.repo.model.file.ExternalFileMetadata;
import org.sagebionetworks.repo.model.file.FileMetadata;
import org.sagebionetworks.repo.model.file.PreviewFileMetadata;
import org.sagebionetworks.repo.model.file.S3FileInterface;
import org.sagebionetworks.repo.model.file.S3FileMetadata;

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
	 */
	public static DBOFileMetadata createDBOFromDTO(FileMetadata dto){
		if(dto == null) throw new IllegalArgumentException("DTO cannot be null");
		if(dto instanceof ExternalFileMetadata){
			return createDBOFromDTO((ExternalFileMetadata)dto);
		}else if(dto instanceof S3FileMetadata){
			return createDBOFromDTO((S3FileMetadata)dto);
		}else if(dto instanceof PreviewFileMetadata){
			return createDBOFromDTO((PreviewFileMetadata)dto);
		}else{
			throw new IllegalArgumentException("Unknown FileMetadata implementaion: "+dto.getClass().getName());
		}
	}
	
	/**
	 * Convert from the DTO to the DBO.
	 * @param dto
	 * @return
	 */
	private static DBOFileMetadata createDBOFromDTO(ExternalFileMetadata dto){
		DBOFileMetadata dbo = new DBOFileMetadata();
		dbo.setMetadataType(MetadataType.EXTERNAL);
		dbo.setKey(dto.getExternalURL());
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
		return dbo;
	}
	
	/**
	 * Convert from the DTO to the DBO.
	 * @param dto
	 * @return
	 */
	private static DBOFileMetadata createDBOFromDTO(S3FileMetadata dto){
		DBOFileMetadata dbo = new DBOFileMetadata();
		dbo.setMetadataType(MetadataType.S3);
		if(dto.getPreviewId() != null){
			dbo.setPreviewId(new Long(dto.getPreviewId()));
		}
		// Fill in the common data.
		setDBOFromDTO(dbo, dto);
		return dbo;
	}
	
	private static DBOFileMetadata createDBOFromDTO(PreviewFileMetadata dto){
		DBOFileMetadata dbo = new DBOFileMetadata();
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
	private static void setDBOFromDTO(DBOFileMetadata dbo, S3FileInterface dto){
		if(dto.getId() != null){
			dbo.setId(new Long(dto.getId()));
		}
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
	public static FileMetadata createDTOFromDBO(DBOFileMetadata dbo){
		// First determine the type
		if(MetadataType.EXTERNAL == dbo.getMetadataTypeEnum()){
			// External
			ExternalFileMetadata external = new ExternalFileMetadata();
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
			external.setExternalURL(dbo.getKey());
			return external;
		}else if(MetadataType.S3 == dbo.getMetadataTypeEnum() || MetadataType.PREVIEW == dbo.getMetadataTypeEnum()){
			S3FileInterface metaInterface = null;
			// Is this a S3 file or a preview.
			if(MetadataType.S3 == dbo.getMetadataTypeEnum()){
				S3FileMetadata meta = new S3FileMetadata();
				metaInterface = meta;
				if(dbo.getPreviewId() != null){
					meta.setPreviewId(dbo.getPreviewId().toString());
				}
			}else if(MetadataType.PREVIEW == dbo.getMetadataTypeEnum()){
				PreviewFileMetadata meta = new PreviewFileMetadata();
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
			if(dbo.getCreatedBy() != null){
				metaInterface.setCreatedBy(dbo.getCreatedBy().toString());
			}
			metaInterface.setCreatedOn(dbo.getCreatedOn());
			return metaInterface;
		}else{
			throw new IllegalArgumentException("Unknown metadata type: "+dbo.getMetadataTypeEnum());
		}
	}

}
