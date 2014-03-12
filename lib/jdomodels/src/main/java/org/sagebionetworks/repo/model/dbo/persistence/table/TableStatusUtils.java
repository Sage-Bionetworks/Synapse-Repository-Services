package org.sagebionetworks.repo.model.dbo.persistence.table;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;

/**
 * DBO to DTO round tripper.
 * 
 * @author John
 *
 */
public class TableStatusUtils {

	/**
	 * Create the DBO from the DTO
	 * @param dto
	 * @return
	 */
	public static DBOTableStatus createDBOFromDTO(TableStatus dto){
		DBOTableStatus dbo = new DBOTableStatus();
		if(dto.getTableId() != null){
			dbo.setTableId(Long.parseLong(dto.getTableId()));
		}
		if(dto.getStartedOn() != null){
			dbo.setStartedOn(dto.getStartedOn().getTime());
		}
		if(dto.getChangedOn() != null){
			dbo.setChangedOn(dto.getChangedOn().getTime());
		}
		dbo.setErrorDetails(createErrorDetails(dto.getErrorDetails()));
		dbo.setErrorMessage(dto.getErrorMessage());
		if(dto.getState() != null){
			dbo.setState(TableStateEnum.valueOf(dto.getState().name()));
		}
		dbo.setProgresssCurrent(dto.getProgresssCurrent());
		dbo.setProgresssTotal(dto.getProgresssTotal());
		dbo.setProgresssMessage(dto.getProgresssMessage());
		dbo.setResetToken(dto.getResetToken());
		dbo.setTotalRunTimeMS(dto.getTotalTimeMS());
		dbo.setLastTableChangeEtag(dto.getLastTableChangeEtag());
		return dbo;
	}
	
	/**
	 * Create the DTO from the DBO.
	 * @param dbo
	 * @return
	 */
	public static TableStatus createDTOFromDBO(DBOTableStatus dbo){
		TableStatus dto = new TableStatus();
		if(dbo.getTableId() != null){
			dto.setTableId(dbo.getTableId().toString());
		}
		if(dbo.getStartedOn() != null){
			dto.setStartedOn(new Date(dbo.getStartedOn()));
		}
		if(dbo.getChangedOn() != null){
			dto.setChangedOn(new Date(dbo.getChangedOn()));
		}
		dto.setErrorDetails(createErrorDetails(dbo.getErrorDetails()));
		dto.setErrorMessage(dbo.getErrorMessage());
		if(dbo.getState() != null){
			dto.setState(TableState.valueOf(dbo.getState().name()));
		}
		dto.setProgresssCurrent(dbo.getProgresssCurrent());
		dto.setProgresssMessage(dbo.getProgresssMessage());
		dto.setResetToken(dbo.getResetToken());
		dto.setProgresssTotal(dbo.getProgresssTotal());
		dto.setTotalTimeMS(dbo.getTotalRunTimeMS());
		dto.setLastTableChangeEtag(dbo.getLastTableChangeEtag());
		return dto;
	}
	
	/**
	 * Helper to convert from UTF-8 bytes to String
	 * @param bytes
	 * @return
	 */
	public static String createErrorDetails(byte[] bytes){
		if(bytes == null) return null;
		try {
			return new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Helper to convert from String to UTF-8 bytes.
	 * @param error
	 * @return
	 */
	public static byte[] createErrorDetails(String error){
		if(error == null) return null;
		try {
			return error.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
