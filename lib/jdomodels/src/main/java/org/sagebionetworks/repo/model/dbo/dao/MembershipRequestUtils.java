package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.util.Date;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMembershipRequest;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class MembershipRequestUtils {

	// the convention is that the individual fields take precedence
	// over the serialized objects.  When restoring the dto we first deserialize
	// the 'blob' and then populate the individual fields

	public static final String CLASS_ALIAS = "MembershipRequest";
	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder()
			.allowTypes(MembershipRequest.class)
			.alias(CLASS_ALIAS, MembershipRequest.class)
			.build();

	public static void copyDtoToDbo(MembershipRequest dto, DBOMembershipRequest dbo) throws DatastoreException {
		if (dto.getId()!=null) dbo.setId(Long.parseLong(dto.getId()));
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		if(dto.getExpiresOn()==null) dbo.setExpiresOn(null); else dbo.setExpiresOn(dto.getExpiresOn().getTime());
		dbo.setTeamId(Long.parseLong(dto.getTeamId()));
		dbo.setUserId(Long.parseLong(dto.getUserId()));
		copyToSerializedField(dto, dbo);
	}

	public static MembershipRequest copyDboToDto(DBOMembershipRequest dbo) throws DatastoreException {
		MembershipRequest dto = copyFromSerializedField(dbo);
		dto.setId(dbo.getId().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		if (dbo.getExpiresOn()==null) dto.setExpiresOn(null);else dto.setExpiresOn(new Date(dbo.getExpiresOn()));
		dto.setTeamId(dbo.getTeamId().toString());
		dto.setUserId(dbo.getUserId().toString());
		return dto;
	}
	
	public static void copyToSerializedField(MembershipRequest dto, DBOMembershipRequest dbo) throws DatastoreException {
		try {
			dbo.setProperties(JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	public static MembershipRequest deserialize(byte[] b) {
		try {
			return (MembershipRequest)JDOSecondaryPropertyUtils.decompressObject(X_STREAM, b);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
		
	}
	
	public static MembershipRequest copyFromSerializedField(DBOMembershipRequest dbo) throws DatastoreException {
		return deserialize(dbo.getProperties());
	}
}
