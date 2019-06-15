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
			AsynchronousRequestBody asynchronousRequestBody = (AsynchronousRequestBody) JDOSecondaryPropertyUtils.decompressObject(AsynchJobType.getRequestXStream(), dbo.getRequestBody());
			asynchronousRequestBody.setConcreteType(asynchronousRequestBody.getClass().getName());
			dto.setRequestBody(asynchronousRequestBody);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if(dbo.getResponseBody() != null){
			try {
				dto.setResponseBody((AsynchronousResponseBody) JDOSecondaryPropertyUtils.decompressObject(AsynchJobType.getResponseXStream(), dbo.getResponseBody()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		// The database contains the truth data for all generic data.
		dto.setChangedOn(dbo.getChangedOn());
		dto.setException(dbo.getException());
		dto.setErrorDetails(dbo.getErrorDetails());
		dto.setErrorMessage(dbo.getErrorMessage());
		dto.setEtag(dbo.getEtag());
		dto.setJobId(dbo.getJobId().toString());
		dto.setJobState(AsynchJobState.valueOf(dbo.getJobState().name()));
		dto.setJobCanceling(dbo.getCanceling());
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
		AsynchronousRequestBody requestBody = dto.getRequestBody();
		AsynchJobType type = AsynchJobType.findTypeFromRequestClass(requestBody.getClass());
		DBOAsynchJobStatus dbo = new DBOAsynchJobStatus();
		dbo.setChangedOn(dto.getChangedOn()); 
		dbo.setException(dto.getException());
		dbo.setErrorDetails(dto.getErrorDetails());
		dbo.setErrorMessage(truncateMessageStringIfNeeded(dto.getErrorMessage()));
		dbo.setEtag(dto.getEtag());
		dbo.setJobId(Long.parseLong(dto.getJobId()));
		dbo.setJobState(JobState.valueOf(dto.getJobState().name()));
		dbo.setJobType(type);
		dbo.setCanceling(dto.getJobCanceling());
		dbo.setProgressCurrent(dto.getProgressCurrent());
		dbo.setProgressTotal(dto.getProgressTotal());
		dbo.setProgressMessage(truncateMessageStringIfNeeded(dto.getProgressMessage()));
		dbo.setStartedByUserId(dto.getStartedByUserId());
		dbo.setStartedOn(dto.getStartedOn());
		dbo.setRuntimeMS(dto.getRuntimeMS());
		// Compress the body
		try {
			dbo.setRequestBody(JDOSecondaryPropertyUtils.compressObject(AsynchJobType.getRequestXStream(), requestBody));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if(dto.getResponseBody() != null){
			dbo.setResponseBody(getBytesForResponseBody(type, dto.getResponseBody()));
		}
		return dbo;
	}
	
	/**
	 * Get the bytes for a compressed response body.
	 * 
	 * @param type
	 * @param body
	 * @return
	 */
	public static byte[] getBytesForResponseBody(AsynchJobType type, AsynchronousResponseBody body){
		try {
			return JDOSecondaryPropertyUtils.compressObject(AsynchJobType.getResponseXStream(), body);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
