package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.util.Date;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMembershipInvitation;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class MembershipInvitationUtils {

	// the convention is that the individual fields take precedence
	// over the serialized objects.  When restoring the dto we first deserialize
	// the 'blob' and then populate the individual fields

	public static final String CLASS_ALIAS = "MembershipInvitation";
	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder()
			.allowTypes(MembershipInvitation.class)
			.alias(CLASS_ALIAS, MembershipInvitation.class)
			.build();

	public static void copyDtoToDbo(MembershipInvitation dto, DBOMembershipInvitation dbo) throws DatastoreException {
		if (dto.getId()!=null) dbo.setId(Long.parseLong(dto.getId()));
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		if(dto.getExpiresOn()==null) dbo.setExpiresOn(null); else dbo.setExpiresOn(dto.getExpiresOn().getTime());
		dbo.setTeamId(Long.parseLong(dto.getTeamId()));
		if (dto.getInviteeId()==null) dbo.setInviteeId(null); else dbo.setInviteeId(Long.parseLong(dto.getInviteeId()));
		if (dto.getInviteeEmail()==null) dbo.setInviteeEmail(null); else dbo.setInviteeEmail(dto.getInviteeEmail());
		copyToSerializedField(dto, dbo);
	}

	public static MembershipInvitation copyDboToDto(DBOMembershipInvitation dbo) throws DatastoreException {
		MembershipInvitation dto = copyFromSerializedField(dbo);
		dto.setId(dbo.getId().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		if (dbo.getExpiresOn()==null) dto.setExpiresOn(null); else dto.setExpiresOn(new Date(dbo.getExpiresOn()));
		dto.setTeamId(dbo.getTeamId().toString());
		if (dbo.getInviteeId()==null) dto.setInviteeId(null); else dto.setInviteeId(dbo.getInviteeId().toString());
		if (dbo.getInviteeEmail()==null) dto.setInviteeEmail(null); else dto.setInviteeEmail(dbo.getInviteeEmail());
		return dto;
	}

	public static void copyToSerializedField(MembershipInvitation dto, DBOMembershipInvitation dbo) throws DatastoreException {
		try {
			dbo.setProperties(JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	public static MembershipInvitation deserialize(byte[] b) {
		try {
			return (MembershipInvitation)JDOSecondaryPropertyUtils.decompressObject(X_STREAM, b);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
		
	}
	
	public static MembershipInvitation copyFromSerializedField(DBOMembershipInvitation dbo) throws DatastoreException {
		return deserialize(dbo.getProperties());
	}
}
