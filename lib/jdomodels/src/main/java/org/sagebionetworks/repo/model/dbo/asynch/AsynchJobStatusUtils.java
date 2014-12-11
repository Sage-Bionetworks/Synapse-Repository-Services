package org.sagebionetworks.repo.model.dbo.asynch;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.dbo.asynch.DBOAsynchJobStatus.JobState;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

/**
 * Utils for translating DBOAsynchJobStatus to/from AsynchronousJobStatus
 * @author John
 *
 */
public class AsynchJobStatusUtils {
	
	/**
	 * Create a DTO from a DBO
	 * @param dbo
	 * @return
	 */
	public static AsynchronousJobStatus createDTOFromDBO(DBOAsynchJobStatus dbo){
		// Read in the compressed data
		AsynchronousJobStatus dto = new AsynchronousJobStatus();
		try {
			// The compressed body contains the truth data for all type specific data.
			AsynchronousRequestBody asynchronousRequestBody = (AsynchronousRequestBody) JDOSecondaryPropertyUtils.decompressedObject(dbo.getRequestBody(), dbo.getJobType().name(), dbo.getJobType().getRequestClass());
			asynchronousRequestBody.setConcreteType(asynchronousRequestBody.getClass().getName());
			dto.setRequestBody(asynchronousRequestBody);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if(dbo.getResponseBody() != null){
			try {
				dto.setResponseBody((AsynchronousResponseBody) JDOSecondaryPropertyUtils.decompressedObject(dbo.getResponseBody(), dbo.getJobType().name(), dbo.getJobType().getResponseClass()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		// The database contains the truth data for all generic data.
		dto.setChangedOn(dbo.getChangedOn());
		dto.setErrorDetails(dbo.getErrorDetails());
		dto.setErrorMessage(dbo.getErrorMessage());
		dto.setEtag(dbo.getEtag());
		dto.setJobId(dbo.getJobId().toString());
		dto.setJobState(AsynchJobState.valueOf(dbo.getJobState().name()));
		dto.setProgressCurrent(dbo.getProgressCurrent());
		dto.setProgressTotal(dbo.getProgressTotal());
		dto.setProgressMessage(dbo.getProgressMessage());
		dto.setStartedByUserId(dbo.getStartedByUserId());
		dto.setStartedOn(dbo.getStartedOn());
		dto.setRuntimeMS(dbo.getRuntimeMS());
		return dto;
	}
	
	/**
	 * Create DBO from a DTO
	 * @param dto
	 * @return
	 */
	public static DBOAsynchJobStatus createDBOFromDTO(AsynchronousJobStatus dto){
		if(dto == null) throw new IllegalArgumentException("AsynchronousJobStatus cannot be null");
		// Lookup the type
		AsynchJobType type = AsynchJobType.findTypeFromRequestClass(dto.getRequestBody().getClass());
		DBOAsynchJobStatus dbo = new DBOAsynchJobStatus();
		dbo.setChangedOn(dto.getChangedOn()); 
		dbo.setErrorDetails(dto.getErrorDetails());
		dbo.setErrorMessage(truncateMessageStringIfNeeded(dto.getErrorMessage()));
		dbo.setEtag(dto.getEtag());
		dbo.setJobId(Long.parseLong(dto.getJobId()));
		dbo.setJobState(JobState.valueOf(dto.getJobState().name()));
		dbo.setJobType(type);
		dbo.setProgressCurrent(dto.getProgressCurrent());
		dbo.setProgressTotal(dto.getProgressTotal());
		dbo.setProgressMessage(truncateMessageStringIfNeeded(dto.getProgressMessage()));
		dbo.setStartedByUserId(dto.getStartedByUserId());
		dbo.setStartedOn(dto.getStartedOn());
		dbo.setRuntimeMS(dto.getRuntimeMS());
		// Compress the body
		try {
			dbo.setRequestBody(JDOSecondaryPropertyUtils.compressObject(dto.getRequestBody(), type.name()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if(dto.getResponseBody() != null){
			try {
				dbo.setResponseBody(JDOSecondaryPropertyUtils.compressObject(dto.getResponseBody(), type.name()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
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
		if(message.length() > DBOAsynchJobStatus.MAX_MESSAGE_CHARS){
			return message.substring(0, DBOAsynchJobStatus.MAX_MESSAGE_CHARS-1);
		}else{
			return message;
		}
	}
	
	/**
	 * UTF-8
	 * @param bytes
	 * @return
	 */
	public static String bytesToString(byte[] bytes){
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
	public static byte[] stringToBytes(String string){
		if(string == null) return null;
		try {
			return string.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
