package org.sagebionetworks.repo.model.dbo.asynch;

import java.io.UnsupportedEncodingException;
import java.sql.Date;
import java.sql.Timestamp;

import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.dbo.asynch.DBOAsynchJobStatus.JobState;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

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
	public static AsynchronousJobStatus createDTOFromDBO(DBOAsynchJobStatus dbo) {
		AsynchronousJobStatus dto = new AsynchronousJobStatus();
		// set the asynch request body
		try {
			AsynchronousRequestBody asynchronousRequestBody = 
					EntityFactory.createEntityFromJSONString(dbo.getRequestBody(), AsynchronousRequestBody.class);
			dto.setRequestBody(asynchronousRequestBody);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		// set the asynch response body
		try {
			if (dbo.getResponseBody() != null) {
				AsynchronousResponseBody asynchronousResponseBody = 
						EntityFactory.createEntityFromJSONString(dbo.getResponseBody(), AsynchronousResponseBody.class);
				dto.setResponseBody(asynchronousResponseBody);
			}
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		// The database contains the truth data for all generic data.
		dto.setChangedOn(new Date(dbo.getChangedOn().getTime()));
		dto.setException(dbo.getException());
		dto.setErrorDetails(dbo.getErrorDetails());
		dto.setErrorMessage(dbo.getErrorMessage());
		dto.setEtag(dbo.getEtag());
		dto.setJobId(dbo.getJobId().toString());
		dto.setJobState(AsynchJobState.valueOf(dbo.getJobState()));
		dto.setJobCanceling(dbo.getCanceling());
		dto.setProgressCurrent(dbo.getProgressCurrent());
		dto.setProgressTotal(dbo.getProgressTotal());
		dto.setProgressMessage(dbo.getProgressMessage());
		dto.setStartedByUserId(dbo.getStartedByUserId());
		dto.setStartedOn(new Date(dbo.getStartedOn().getTime()));
		dto.setRuntimeMS(dbo.getRuntimeMS());
		return dto;
	}
	
	/**
	 * Create DBO from a DTO
	 * @param dto
	 * @return
	 */
	public static DBOAsynchJobStatus createDBOFromDTO(AsynchronousJobStatus dto) {
		if(dto == null) throw new IllegalArgumentException("AsynchronousJobStatus cannot be null");
		// Lookup the type
		AsynchronousRequestBody requestBody = dto.getRequestBody();
		AsynchJobType type = AsynchJobType.findTypeFromRequestClass(requestBody.getClass());
		DBOAsynchJobStatus dbo = new DBOAsynchJobStatus();
		dbo.setChangedOn(new Timestamp(dto.getChangedOn().getTime())); 
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
		dbo.setStartedOn(new Timestamp(dto.getStartedOn().getTime()));
		dbo.setRuntimeMS(dto.getRuntimeMS());
		// set the request body
		try {
			if (requestBody.getConcreteType() == null) {
				requestBody.setConcreteType(requestBody.getClass().getName());
			}
			dbo.setRequestBody(EntityFactory.createJSONStringForEntity(requestBody));
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		// set the response body
		try {
			AsynchronousResponseBody responseBody = dto.getResponseBody();
			if (responseBody != null) {
				if (responseBody.getConcreteType() == null) {
					responseBody.setConcreteType(responseBody.getClass().getName());
				}
				dbo.setResponseBody(EntityFactory.createJSONStringForEntity(responseBody));
			}
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
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
