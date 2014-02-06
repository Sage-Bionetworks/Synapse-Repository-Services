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
		if(dto.getChangedOn() != null){
			dbo.setChangedOn(dto.getChangedOn().getTime());
		}
		if(dto.getErrorDetails() != null){
			try {
				dbo.setErrorDetails(dto.getErrorDetails().getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		dbo.setErrorMessage(dto.getErrorMessage());
		if(dto.getState() != null){
			dbo.setState(TableStateEnum.valueOf(dto.getState().name()));
		}
		dbo.setProgresssCurrent(dto.getProgresssCurrent());
		dbo.setProgresssTotal(dto.getProgresssTotal());
		dbo.setProgresssMessage(dto.getProgresssMessage());
		dbo.setTotalRunTimeMS(dto.getTotalTimeMS());
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
		if(dbo.getChangedOn() != null){
			dto.setChangedOn(new Date(dbo.getChangedOn()));
		}
		if(dbo.getErrorDetails() != null){
			try {
				dto.setErrorDetails(new String(dbo.getErrorDetails(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		dto.setErrorMessage(dbo.getErrorMessage());
		if(dbo.getState() != null){
			dto.setState(TableState.valueOf(dbo.getState().name()));
		}
		dto.setProgresssCurrent(dbo.getProgresssCurrent());
		dto.setProgresssMessage(dbo.getProgresssMessage());
		dto.setProgresssTotal(dbo.getProgresssTotal());
		dto.setTotalTimeMS(dbo.getTotalRunTimeMS());
		return dto;
	}
}
