package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTeam;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class TeamUtils {

	// the convention is that the individual fields take precedence
	// over the serialized objects.  When restoring the dto we first deserialize
	// the 'blob' and then populate the individual fields

	public static void copyDtoToDbo(Team dto, DBOTeam dbo) throws DatastoreException {
		dbo.setId(Long.parseLong(dto.getId()));
		dbo.setEtag(dto.getEtag());
		copyToSerializedField(dto, dbo);
	}

	public static Team copyDboToDto(DBOTeam dbo) throws DatastoreException {
		Team dto = copyFromSerializedField(dbo);
		dto.setId(dbo.getId().toString());
		dto.setEtag(dbo.getEtag());
		return dto;
	}

	public static void copyToSerializedField(Team dto, DBOTeam dbo) throws DatastoreException {
		try {
			dbo.setProperties(JDOSecondaryPropertyUtils.compressObject(dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	public static Team deserialize(byte[] b) {		
		try {
			return (Team)JDOSecondaryPropertyUtils.decompressedObject(b);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	public static Team copyFromSerializedField(DBOTeam dbo) throws DatastoreException {
		return deserialize(dbo.getProperties());
	}
}
