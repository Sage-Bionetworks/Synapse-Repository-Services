package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.util.Date;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessApproval;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class AccessApprovalUtils {
	
	public static void copyDtoToDbo(AccessApproval dto, DBOAccessApproval dbo) throws DatastoreException{
		if (dto.getId()==null) {
			dbo.setId(null);
		} else {
			dbo.setId(dto.getId());
		}
		if (dto.getEtag()==null) {
			dbo.seteTag(null);
		} else {
			dbo.seteTag(Long.parseLong(dto.getEtag()));
		}
		dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		dbo.setModifiedBy(Long.parseLong(dto.getModifiedBy()));
		dbo.setModifiedOn(dto.getModifiedOn().getTime());
		dbo.setRequirementId(dto.getRequirementId());
		dbo.setAccessorId(Long.parseLong(dto.getAccessorId()));
		dbo.setEntityType(dto.getEntityType());
		copyToSerializedField(dto, dbo);
	}
	
	public static AccessApproval copyDboToDto(DBOAccessApproval dbo) throws DatastoreException {
		AccessApproval dto = copyFromSerializedField(dbo);
		// TODO the rest may not be necessary ...
		if (dbo.getId()==null) {
			dto.setId(null);
		} else {
			dto.setId(dbo.getId());
		}
		if (dbo.geteTag()==null) {
			dto.setEtag(null);
		} else {
			dto.setEtag(""+dbo.geteTag());
		}
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setModifiedBy(dbo.getModifiedBy().toString());
		dto.setModifiedOn(new Date(dbo.getModifiedOn()));
		dto.setRequirementId(dbo.getRequirementId());
		dto.setAccessorId(dbo.getAccessorId().toString());
		dto.setEntityType(dbo.getEntityType());
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
