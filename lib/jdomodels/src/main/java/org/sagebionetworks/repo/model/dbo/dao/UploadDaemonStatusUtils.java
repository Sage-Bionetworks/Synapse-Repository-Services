package org.sagebionetworks.repo.model.dbo.dao;

import java.util.Date;

import org.sagebionetworks.repo.model.dbo.persistence.DBOUploadDaemonStatus;
import org.sagebionetworks.repo.model.file.State;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;

/**
 * Translation utils.
 * @author John
 *
 */
public class UploadDaemonStatusUtils {
	
	public static final int MAX_ERROR_SIZE = 3000;

	/**
	 * Create the DBO from the DTO
	 * @param dto
	 * @return
	 */
	public static DBOUploadDaemonStatus createDBOFromDTO(UploadDaemonStatus dto){
		if(dto == null) throw new IllegalArgumentException("UploadDaemonStatus cannot be null");
		DBOUploadDaemonStatus dbo = new DBOUploadDaemonStatus();
		dbo.setRunTimeMS(dto.getRunTimeMS());
		if(dto.getErrorMessage() != null){
			// Truncate the message if needed
			String message = dto.getErrorMessage();
			if(message.length() > MAX_ERROR_SIZE){
				message = message.substring(0, MAX_ERROR_SIZE-1);
			}
			dbo.setErrorMessage(message);
		}
		if(dto.getFileHandleId() != null){
			dbo.setFileHandleId(Long.parseLong(dto.getFileHandleId()));
		}
		if(dto.getDaemonId() != null){
			dbo.setId(Long.parseLong(dto.getDaemonId()));
		}
		dbo.setPercentComplete(dto.getPercentComplete());
		if(dto.getStartedBy() != null){
			dbo.setStartedBy(Long.parseLong(dto.getStartedBy()));
		}
		if(dto.getStartedOn() != null){
			dbo.setStartedOn(dto.getStartedOn().getTime());
		}
		if(dto.getState() != null){
			dbo.setState(dto.getState().name());
		}
		return dbo;
	}
	
	/**
	 * Create the DTO from the DBO.
	 * @param dbo
	 * @return
	 */
	public static UploadDaemonStatus createDTOFromDBO(DBOUploadDaemonStatus dbo){
		if(dbo == null) throw new IllegalArgumentException("DBOUploadDaemonStatus cannot be null");
		UploadDaemonStatus dto = new UploadDaemonStatus();
		dto.setRunTimeMS(dbo.getRunTimeMS());
		dto.setErrorMessage(dbo.getErrorMessage());
		if(dbo.getFileHandleId() != null){
			dto.setFileHandleId(dbo.getFileHandleId().toString());
		}
		if(dbo.getId() != null){
			dto.setDaemonId(dbo.getId().toString());
		}
		dto.setPercentComplete(dbo.getPercentComplete());
		if(dbo.getStartedBy() != null){
			dto.setStartedBy(dbo.getStartedBy().toString());
		}
		if(dbo.getStartedOn() != null){
			dto.setStartedOn(new Date(dbo.getStartedOn()));
		}
		if(dbo.getState() != null){
			dto.setState(State.valueOf(dbo.getState()));
		}
		return dto;
	}
}
