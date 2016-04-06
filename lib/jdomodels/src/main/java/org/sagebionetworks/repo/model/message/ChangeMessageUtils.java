package org.sagebionetworks.repo.model.message;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.persistence.DBOChange;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

/**
 * Utilities for translating between DTOs and DBOs for  ChangeMessage.
 * 
 * @author John
 *
 */
public class ChangeMessageUtils {
	
	/**
	 * Converts a list of data transfer objects (DTOs) to a list of database objects (DBOs).
	 * 
	 * @param dto
	 * @return
	 */
	public static List<DBOChange> createDBOList(List<ChangeMessage> dtos){
		if(dtos == null) throw new IllegalArgumentException("DTO list cannot be null");
		List<DBOChange> dboList = new ArrayList<DBOChange>();
		for(ChangeMessage dto: dtos){
			DBOChange dbo = createDBO(dto);
			dboList.add(dbo);
		}
		return dboList;
	}

	/**
	 * Converts a data transfer objects (DTOs) to a database objects (DBOs).
	 * @param dto
	 * @return
	 */
	public static DBOChange createDBO(ChangeMessage dto) {
		DBOChange dbo = new DBOChange();
		dbo.setChangeNumber(dto.getChangeNumber());
		if(dto.getTimestamp() != null){
			dbo.setTimeStamp(new Timestamp(dto.getTimestamp().getTime()));
		}
		dbo.setObjectType(dto.getObjectType().name());
		dbo.setChangeType(dto.getChangeType().name());
		dbo.setObjectEtag(dto.getObjectEtag());
		String dtoParentId = dto.getParentId();
		if(ObjectType.ENTITY == dto.getObjectType()){
			// Entities get an 'syn' prefix
			dbo.setObjectId(KeyFactory.stringToKey(dto.getObjectId()));
			if (dtoParentId != null) {
				dbo.setParentId(KeyFactory.stringToKey(dtoParentId));
			}
		}else{
			// All other types are longs.
			dbo.setObjectId(Long.parseLong(dto.getObjectId()));
			if (dtoParentId != null) {
				dbo.setParentId(Long.parseLong(dtoParentId));
			}
		}
		if (dto.getUserId() != null) {
			dbo.setUserId(dto.getUserId());
		}
		return dbo;
	}
	
	/**
	 * Convert a list of database objects (DBOs) to a list of data transfer objects (DTOs).
	 * @param dbos
	 * @return
	 */
	public static List<ChangeMessage> createDTOList(List<DBOChange>  dbos){
		if(dbos == null) throw new IllegalArgumentException("DBOs cannot be null");
		List<ChangeMessage> dtoList = new ArrayList<ChangeMessage>();
		for(DBOChange dbo: dbos){
			ChangeMessage dto = createDTO(dbo);
			dtoList.add(dto);
		}
		return dtoList;
	}

	/**
	 * Convert a database objects (DBOs) to a data transfer objects (DTOs).
	 * @param dbo
	 * @return
	 */
	public static ChangeMessage createDTO(DBOChange dbo) {
		ChangeMessage dto = new ChangeMessage();
		dto.setChangeNumber(dbo.getChangeNumber());
		dto.setTimestamp(dbo.getTimeStamp());
		dto.setObjectEtag(dbo.getObjectEtag());
		dto.setObjectType(ObjectType.valueOf(dbo.getObjectType()));
		if(ObjectType.ENTITY == dto.getObjectType()){
			// Entities get an 'syn' prefix
			dto.setObjectId(KeyFactory.keyToString(dbo.getObjectId()));
			dto.setParentId(KeyFactory.keyToString(dbo.getParentId()));
		}else{
			// All other types are longs.
			dto.setObjectId(dbo.getObjectId().toString());
			Long parentId = dbo.getParentId();
			if (parentId != null) {
				dto.setParentId(parentId.toString());
			}
		}
		dto.setChangeType(ChangeType.valueOf(dbo.getChangeType()));
		if (dbo.getUserId() != null) {
			dto.setUserId(dbo.getUserId());
		}
		return dto;
	}
	
	/**
	 * Simple helper to sort by ObjectID
	 * 
	 * @param list
	 * @return
	 */
	public static List<ChangeMessage> sortByObjectId(List<ChangeMessage> list){
		Collections.sort(list, new Comparator<ChangeMessage>() {
			@Override
			public int compare(ChangeMessage one, ChangeMessage two) {
				if(one == null) throw new IllegalArgumentException("DBOChange cannot be null");
				if(one.getObjectId() == null) throw new IllegalArgumentException("one.getObjectId() cannot be null");
				if(two == null) throw new IllegalArgumentException("DBOChange cannot be null");
				if(two.getObjectId() == null) throw new IllegalArgumentException("two.getObjectId() cannot be null");
				// First sort by ID
				int idOrder = one.getObjectId().compareTo(two.getObjectId());
				if(idOrder == 0){
					// When equal sort by type.
					return one.getObjectType().name().compareTo(two.getObjectType().name());
				}else{
					return idOrder;
				}
			}
		});
		return list;
	}

	
	/**
	 * Simple helper to sort by ChangeNumber
	 * 
	 * @param list
	 * @return
	 */
	public static List<ChangeMessage> sortByChangeNumber(List<ChangeMessage> list){
		Collections.sort(list, new Comparator<ChangeMessage>() {
			@Override
			public int compare(ChangeMessage one, ChangeMessage two) {
				if(one == null) throw new IllegalArgumentException("DBOChange cannot be null");
				if(one.getChangeNumber() == null) throw new IllegalArgumentException("one.getChangeNumber() cannot be null");
				if(two == null) throw new IllegalArgumentException("DBOChange cannot be null");
				if(two.getChangeNumber() == null) throw new IllegalArgumentException("two.getChangeNumber() cannot be null");
				return one.getChangeNumber().compareTo(two.getChangeNumber());
			}
		});
		return list;
	}

}
