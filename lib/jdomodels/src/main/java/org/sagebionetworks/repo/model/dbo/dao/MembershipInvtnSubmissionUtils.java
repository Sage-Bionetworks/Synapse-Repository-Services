package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.util.Date;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMembershipInvtnSubmission;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class MembershipInvtnSubmissionUtils {

	// the convention is that the individual fields take precedence
	// over the serialized objects.  When restoring the dto we first deserialize
	// the 'blob' and then populate the individual fields

	public static void copyDtoToDbo(MembershipInvtnSubmission dto, DBOMembershipInvtnSubmission dbo) throws DatastoreException {
		if (dto.getId()!=null) dbo.setId(Long.parseLong(dto.getId()));
		dbo.setEtag(dto.getEtag());
		if(dto.getExpiresOn()==null) dbo.setExpiresOn(0L); else dbo.setExpiresOn(dto.getExpiresOn().getTime());
		dbo.setTeamId(Long.parseLong(dto.getTeamId()));
		copyToSerializedField(dto, dbo);
	}

	public static MembershipInvtnSubmission copyDboToDto(DBOMembershipInvtnSubmission dbo) throws DatastoreException {
		MembershipInvtnSubmission dto = copyFromSerializedField(dbo);
		dto.setId(dbo.getId().toString());
		dto.setEtag(dbo.getEtag());
		if (dbo.getExpiresOn()==0L) dto.setExpiresOn(null);else dto.setExpiresOn(new Date(dbo.getExpiresOn()));
		dto.setTeamId(dbo.getTeamId().toString());
		return dto;
	}

	public static void copyToSerializedField(MembershipInvtnSubmission dto, DBOMembershipInvtnSubmission dbo) throws DatastoreException {
		try {
			dbo.setProperties(JDOSecondaryPropertyUtils.compressObject(dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	public static MembershipInvtnSubmission deserialize(byte[] b) {
		try {
			return (MembershipInvtnSubmission)JDOSecondaryPropertyUtils.decompressedObject(b);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
		
	}
	
	public static MembershipInvtnSubmission copyFromSerializedField(DBOMembershipInvtnSubmission dbo) throws DatastoreException {
		return deserialize(dbo.getProperties());
	}
}
