package org.sagebionetworks.repo.model.dbo.persistence.table;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.sagebionetworks.repo.model.dbo.persistence.table.DBOAsynchTableJobStatus.JobState;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOAsynchTableJobStatus.JobType;
import org.sagebionetworks.repo.model.table.AsynchJobState;
import org.sagebionetworks.repo.model.table.AsynchJobType;
import org.sagebionetworks.repo.model.table.AsynchTableJobStatus;

/**
 * Utils for DBOAsynchTableJobStatus and AsynchTableJobStatus;
 * @author John
 *
 */
public class AsynchTableJobStatusUtils {
	
	/**
	 * Create a DTO from a DBO
	 * @param dbo
	 * @return
	 */
	public static AsynchTableJobStatus createDTOFromDBO(DBOAsynchTableJobStatus dbo){
		AsynchTableJobStatus dto = new AsynchTableJobStatus();
		dto.setChangedOn(new Date(dbo.getChangedOn()));
		dto.setDownloadURL(dbo.getDownloadUrl());
		dto.setErrorDetails(bytesToString(dbo.getErrorDetails()));
		dto.setErrorMessage(dbo.getErrorMessage());
		dto.setEtag(dbo.getEtag());
		dto.setJobId(dbo.getJobId().toString());
		dto.setJobState(AsynchJobState.valueOf(dbo.getJobState().name()));
		dto.setJobType(AsynchJobType.valueOf(dbo.getJobType().name()));
		dto.setProgressCurrent(dbo.getProgressCurrent());
		dto.setProgressTotal(dbo.getProgressTotal());
		dto.setProgressMessage(dbo.getProgressMessage());
		dto.setStartedByUserId(dbo.getStartedByUserId());
		dto.setStartedOn(new Date(dbo.getStartedOn()));
		dto.setTableId(dbo.getTableId());
		if(dbo.getUploadFileHandleId() != null){
			dto.setUploadFileHandleId(dbo.getUploadFileHandleId().toString());
		}

		return dto;
	}
	
	/**
	 * Create DBO from a DTO
	 * @param dto
	 * @return
	 */
	public static DBOAsynchTableJobStatus createDBOFromDTO(AsynchTableJobStatus dto){
		DBOAsynchTableJobStatus dbo = new DBOAsynchTableJobStatus();
		dbo.setChangedOn(dto.getChangedOn().getTime()); 
		dbo.setDownloadUrl(truncateMessageStringIfNeeded(dto.getDownloadURL()));
		dbo.setErrorDetails(stringToBytes(dto.getErrorDetails()));
		dbo.setErrorMessage(truncateMessageStringIfNeeded(dto.getErrorMessage()));
		dbo.setEtag(dto.getEtag());
		dbo.setJobId(Long.parseLong(dto.getJobId()));
		dbo.setJobState(JobState.valueOf(dto.getJobState().name()));
		dbo.setJobType(JobType.valueOf(dto.getJobType().name()));
		dbo.setProgressCurrent(dto.getProgressCurrent());
		dbo.setProgressTotal(dto.getProgressTotal());
		dbo.setProgressMessage(truncateMessageStringIfNeeded(dto.getProgressMessage()));
		dbo.setStartedByUserId(dto.getStartedByUserId());
		dbo.setStartedOn(dto.getStartedOn().getTime());
		dbo.setTableId(dto.getTableId());
		if(dto.getUploadFileHandleId() != null){
			dbo.setUploadFileHandleId(Long.parseLong(dto.getUploadFileHandleId()));
		}
		return dbo;
	}
	
	/**
	 * Ensures messages fit in the table.
	 * 
	 * @param message
	 * @return
	 */
	public static String truncateMessageStringIfNeeded(String message){
		if(message == null) return null;
		if(message.length() > DBOAsynchTableJobStatus.MAX_MESSAGE_CHARS){
			return message.substring(0, DBOAsynchTableJobStatus.MAX_MESSAGE_CHARS-1);
		}else{
			return message;
		}
	}
	
	/**
	 * UTF-8
	 * @param bytes
	 * @return
	 */
	private static String bytesToString(byte[] bytes){
		if(bytes == null) return null;
		try {
			return new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * UTF-8
	 * @param string
	 * @return
	 */
	private static byte[] stringToBytes(String string){
		if(string == null) return null;
		try {
			return string.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
