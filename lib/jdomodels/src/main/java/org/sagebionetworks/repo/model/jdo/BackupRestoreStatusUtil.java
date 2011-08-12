package org.sagebionetworks.repo.model.jdo;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.sagebionetworks.repo.model.BackupRestoreStatus;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.jdo.persistence.JDOBackupRestoreStatus;

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
	public static BackupRestoreStatus createDtoFromJdo(JDOBackupRestoreStatus jdo) throws DatastoreException{
		BackupRestoreStatus dto = new BackupRestoreStatus();
		if(jdo.getId() != null){
			dto.setId(KeyFactory.keyToString(jdo.getId()));
		}
		dto.setStatus(jdo.getStatus());
		dto.setType(jdo.getType());
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
	public static void updateJdoFromDto(BackupRestoreStatus dto, JDOBackupRestoreStatus jdo) throws DatastoreException{
		if(jdo.getId() != null){
			jdo.setId(KeyFactory.stringToKey(dto.getId()));
		}
		jdo.setStatus(dto.getStatus());
		jdo.setType(dto.getType());
		jdo.setStartedBy(dto.getStartedBy());
		if(dto.getStartedOn() != null){
			jdo.setStartedOn(dto.getStartedOn().getTime());
		}
		jdo.setProgresssCurrent(dto.getProgresssCurrent());
		jdo.setProgresssTotal(dto.getProgresssTotal());
		jdo.setProgresssMessage(dto.getProgresssMessage());
		jdo.setErrorMessage(dto.getErrorMessage());
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
		JDOBackupRestoreStatus jdo = new JDOBackupRestoreStatus();
		updateJdoFromDto(dto, jdo);
		if(dto.getId() != null){
			jdo.setId(KeyFactory.stringToKey(dto.getId()));
		}
		return createDtoFromJdo(jdo);
	}

}
