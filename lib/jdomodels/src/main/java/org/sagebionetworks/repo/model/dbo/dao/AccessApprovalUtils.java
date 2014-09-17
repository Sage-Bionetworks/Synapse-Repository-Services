package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.util.Date;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessApproval;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class AccessApprovalUtils {

	// the convention is that the individual fields take precedence
	// over the serialized objects.  When restoring the dto we first deserialize
	// the 'blob' and then populate the individual fields

	public static void copyDtoToDbo(AccessApproval dto, DBOAccessApproval dbo) throws DatastoreException {
		dbo.setId(dto.getId());
		dbo.seteTag(dto.getEtag());
		if (dto.getCreatedBy()!=null) dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		if (dto.getCreatedOn()!=null) dbo.setCreatedOn(dto.getCreatedOn().getTime());
		dbo.setModifiedBy(Long.parseLong(dto.getModifiedBy()));
		dbo.setModifiedOn(dto.getModifiedOn().getTime());
		dbo.setRequirementId(dto.getRequirementId());
		dbo.setAccessorId(Long.parseLong(dto.getAccessorId()));
		copyToSerializedField(dto, dbo);
	}

	public static AccessApproval copyDboToDto(DBOAccessApproval dbo) throws DatastoreException {
		AccessApproval dto = copyFromSerializedField(dbo);
		dto.setId(dbo.getId());
		dto.setEtag(dbo.geteTag());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setModifiedBy(dbo.getModifiedBy().toString());
		dto.setModifiedOn(new Date(dbo.getModifiedOn()));
		dto.setRequirementId(dbo.getRequirementId());
		dto.setAccessorId(dbo.getAccessorId().toString());
		return dto;
	}

	public static void copyToSerializedField(AccessApproval dto, DBOAccessApproval dbo) throws DatastoreException {
		try {
			dbo.setSerializedEntity(JDOSecondaryPropertyUtils.compressObject(dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

	public static AccessApproval copyFromSerializedField(DBOAccessApproval dbo) throws DatastoreException {
		try {
			return (AccessApproval)JDOSecondaryPropertyUtils.decompressedObject(dbo.getSerializedEntity());
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
}
