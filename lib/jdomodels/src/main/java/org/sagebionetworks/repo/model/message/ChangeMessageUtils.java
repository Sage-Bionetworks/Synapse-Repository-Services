package org.sagebionetworks.repo.model.message;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.persistence.DBOChange;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSentMessage;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Utilities for translating between DTOs and DBOs for  ChangeMessage.
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
		dbo.setObjectId(KeyFactory.stringToKey(dto.getObjectId()));
		dbo.setObjectVersion(dto.getObjectVersion());
		if(dto.getObjectVersion() == null) {
			dbo.setObjectVersion(DBOChange.DEFAULT_NULL_VERSION);
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
		dto.setObjectType(ObjectType.valueOf(dbo.getObjectType()));
		if(ObjectType.ENTITY == dto.getObjectType()){
			// Entities get an 'syn' prefix
			dto.setObjectId(KeyFactory.keyToString(dbo.getObjectId()));
		}else{
			// All other types are longs.
			dto.setObjectId(dbo.getObjectId().toString());
		}
		if(DBOChange.DEFAULT_NULL_VERSION == dbo.getObjectVersion()) {
			dto.setObjectVersion(null);
		}else {
			dto.setObjectVersion(dbo.getObjectVersion());
		}

		dto.setChangeType(ChangeType.valueOf(dbo.getChangeType()));
		if (dbo.getUserId() != null) {
			dto.setUserId(dbo.getUserId());
		}
		return dto;
	}
	
	/**
	 * Create a DBOSentMessage for the given change message.
	 * @param message
	 * @param now
	 * @return
	 */
	public static DBOSentMessage createSentDBO(ChangeMessage message, Timestamp now) {
		DBOSentMessage sent = new DBOSentMessage();
		sent.setChangeNumber(message.getChangeNumber());
		sent.setObjectId(KeyFactory.stringToKey(message.getObjectId()));
		if(message.getObjectVersion() == null) {
			sent.setObjectVersion(DBOChange.DEFAULT_NULL_VERSION);
		}else {
			sent.setObjectVersion(message.getObjectVersion());
		}
		sent.setObjectType(message.getObjectType().name());
		sent.setTimeStamp(now);
		return sent;
	}
	
	/**
	 * Simple helper to sort by ObjectID
	 * 
	 * @param list
	 * @return
	 */
	public static List<ChangeMessage> sortByObjectId(List<ChangeMessage> list){
		Collections.sort(list, (ChangeMessage one, ChangeMessage two) -> {
			return compareIdVersionType(one, two);
		});
		return list;
	}
	
	/**
	 * Compare two changes first by objectId, then objectVersion, then objectType.
	 * @param one
	 * @param two
	 * @return
	 */
	public static int compareIdVersionType(ChangeMessage one, ChangeMessage two) {
		ChangeMessageUtils.validateChangeMessage(one);
		ChangeMessageUtils.validateChangeMessage(two);
		// First 
		int order = KeyFactory.compare(one.getObjectId(), two.getObjectId());
		if(order != 0) {
			return order;
		}
		// Ids match so compare versions
		order = compareWithNull(one.getObjectVersion(), two.getObjectVersion());
		if(order != 0) {
			return order;
		}
		// Ids and version match so compare types.
		return one.getObjectType().name().compareTo(two.getObjectType().name());
	}
	
	/**
	 * Compare two Longs.  Handles the case where either is null.
	 * 
	 * @param one
	 * @param two
	 * @return
	 */
	public static int compareWithNull(Long one, Long two) {
		if(one == null) {
			return two == null ? 0 : -1;
		}
		return two == null ? 1 : one.compareTo(two);
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

	
	/**
	 * Null checks for all required fields of a change message.
	 * 
	 * @param toValiate
	 */
	public static void validateChangeMessage(ChangeMessage toValiate) {
		ValidateArgument.required(toValiate, "ChangeMessage");
		ValidateArgument.required(toValiate.getObjectId(), "ChangeMessage.objectId");
		ValidateArgument.required(toValiate.getObjectType(), "ChangeMessage.objectType");
	}
}
