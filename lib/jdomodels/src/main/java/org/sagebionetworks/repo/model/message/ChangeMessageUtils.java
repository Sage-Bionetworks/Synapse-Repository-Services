package org.sagebionetworks.repo.model.message;

import java.util.ArrayList;
import java.util.List;

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
			DBOChange dbo = new DBOChange();

			dbo.setObjectType(dto.getObjectType());
			dbo.setChangeType(dto.getChangeType());
			dbo.setObjectEtag(dto.getObjectEtag());
			if(ObjectType.ENTITY == dto.getObjectType()){
				// Entities get an 'syn' prefix
				dbo.setObjectId(KeyFactory.stringToKey(dto.getObjectId()));
			}else{
				// All other types are longs.
				dbo.setObjectId(Long.parseLong(dto.getObjectId()));
			}
			dboList.add(dbo);
		}
		return dboList;
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
			ChangeMessage dto = new ChangeMessage();
			dto.setObjectEtag(dbo.getObjectEtag());
			dto.setObjectType(dbo.getObjectTypeEnum());
			if(ObjectType.ENTITY == dbo.getObjectTypeEnum()){
				// Entities get an 'syn' prefix
				dto.setObjectId(KeyFactory.keyToString(dbo.getObjectId()));
			}else{
				// All other types are longs.
				dto.setObjectId(dbo.getObjectId().toString());
			}
			dto.setChangeType(dbo.getChangeTypeEnum());
			dtoList.add(dto);
		}
		return dtoList;
	}

}
