package org.sagebionetworks.repo.model.jdo;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.DaemonType;
import org.sagebionetworks.repo.model.jdo.persistence.JDODaemonStatus;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * Translates to and from the JDO and DTO objects.
 * 
 * @author John
 *
 */
public class BackupRestoreStatusUtil {
	
	/**
	 * Create a new DTO from the given JDO.
	 * @param jdo
	 * @return
	 * @throws DatastoreException
	 */
	public static BackupRestoreStatus createDtoFromJdo(JDODaemonStatus jdo) throws DatastoreException{
		BackupRestoreStatus dto = new BackupRestoreStatus();
		if(jdo.getId() != null){
			dto.setId(jdo.getId().toString());
		}
		dto.setStatus(DaemonStatus.valueOf(jdo.getStatus()));
		dto.setType(DaemonType.valueOf(jdo.getType()));
		dto.setStartedBy(jdo.getStartedBy());
		if(jdo.getStartedOn() != null){
			dto.setStartedOn(new Date(jdo.getStartedOn()));
		}
		dto.setProgresssCurrent(jdo.getProgresssCurrent());
		dto.setProgresssTotal(jdo.getProgresssTotal());
		dto.setProgresssMessage(jdo.getProgresssMessage());
		dto.setErrorMessage(jdo.getErrorMessage());
		if(jdo.getErrorDetails() != null){
			try {
				dto.setErrorDetails(new String(jdo.getErrorDetails(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new DatastoreException(e);
			}
		}
		dto.setTotalTimeMS(jdo.getTotalRunTimeMS());
		dto.setBackupUrl(jdo.getBackupUrl());
		return dto;
	}
	
	/**
	 * Update the passed JDO object using the passed DTO.
	 * @param dto
	 * @param jdo
	 * @throws DatastoreException
	 */
	public static void updateJdoFromDto(BackupRestoreStatus dto, JDODaemonStatus jdo) throws DatastoreException{
		if(jdo.getId() != null){
			jdo.setId(Long.parseLong(dto.getId()));
		}
		jdo.setStatus(dto.getStatus().name());
		jdo.setType(dto.getType().name());
		jdo.setStartedBy(dto.getStartedBy());
		if(dto.getStartedOn() != null){
			jdo.setStartedOn(dto.getStartedOn().getTime());
		}
		jdo.setProgresssCurrent(dto.getProgresssCurrent());
		jdo.setProgresssTotal(dto.getProgresssTotal());
		jdo.setProgresssMessage(dto.getProgresssMessage());
		if(dto.getErrorMessage() != null){
			// Make sure the error message will fit
			String errorMessage = dto.getErrorMessage();
			if(errorMessage.length() > SqlConstants.ERROR_MESSAGE_MAX_LENGTH-1){
				errorMessage = errorMessage.substring(0, SqlConstants.ERROR_MESSAGE_MAX_LENGTH-1);
			}
			jdo.setErrorMessage(errorMessage);
		}

		if(dto.getErrorDetails() != null){
			try {
				jdo.setErrorDetails(dto.getErrorDetails().getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new DatastoreException(e);
			}
		}
		jdo.setTotalRunTimeMS(dto.getTotalTimeMS());
		jdo.setBackupUrl(dto.getBackupUrl());
	}
	
	/**
	 * Create a clone by first making a JDO object, then back to the DTO.
	 * @param dto
	 * @return
	 * @throws DatastoreException 
	 */
	public static BackupRestoreStatus cloneStatus(BackupRestoreStatus dto) throws DatastoreException{
		JDODaemonStatus jdo = new JDODaemonStatus();
		updateJdoFromDto(dto, jdo);
		if(dto.getId() != null){
			jdo.setId(Long.parseLong(dto.getId()));
		}
		return createDtoFromJdo(jdo);
	}

}
