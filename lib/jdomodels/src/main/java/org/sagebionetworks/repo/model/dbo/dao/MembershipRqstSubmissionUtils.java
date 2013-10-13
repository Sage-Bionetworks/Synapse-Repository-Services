package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.util.Date;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMembershipRqstSubmission;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class MembershipRqstSubmissionUtils {

	// the convention is that the individual fields take precedence
	// over the serialized objects.  When restoring the dto we first deserialize
	// the 'blob' and then populate the individual fields

	private static final String CLASS_ALIAS = "MembershipRqstSubmission";

	public static void copyDtoToDbo(MembershipRqstSubmission dto, DBOMembershipRqstSubmission dbo) throws DatastoreException {
		if (dto.getId()!=null) dbo.setId(Long.parseLong(dto.getId()));
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		if(dto.getExpiresOn()==null) dbo.setExpiresOn(null); else dbo.setExpiresOn(dto.getExpiresOn().getTime());
		dbo.setTeamId(Long.parseLong(dto.getTeamId()));
		dbo.setUserId(Long.parseLong(dto.getUserId()));
		copyToSerializedField(dto, dbo);
	}

	public static MembershipRqstSubmission copyDboToDto(DBOMembershipRqstSubmission dbo) throws DatastoreException {
		MembershipRqstSubmission dto = copyFromSerializedField(dbo);
		dto.setId(dbo.getId().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		if (dbo.getExpiresOn()==null) dto.setExpiresOn(null);else dto.setExpiresOn(new Date(dbo.getExpiresOn()));
		dto.setTeamId(dbo.getTeamId().toString());
		dto.setUserId(dbo.getUserId().toString());
		return dto;
	}
	
	public static void copyToSerializedField(MembershipRqstSubmission dto, DBOMembershipRqstSubmission dbo) throws DatastoreException {
		try {
			dbo.setProperties(JDOSecondaryPropertyUtils.compressObject(dto, CLASS_ALIAS));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	public static MembershipRqstSubmission deserialize(byte[] b) {
		try {
			return (MembershipRqstSubmission)JDOSecondaryPropertyUtils.decompressedObject(b, CLASS_ALIAS, MembershipRqstSubmission.class);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
		
	}
	
	public static MembershipRqstSubmission copyFromSerializedField(DBOMembershipRqstSubmission dbo) throws DatastoreException {
		return deserialize(dbo.getProperties());
	}
}
