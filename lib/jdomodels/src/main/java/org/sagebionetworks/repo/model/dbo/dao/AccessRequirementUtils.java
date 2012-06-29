package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.util.Date;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirement;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

public class AccessRequirementUtils {
	
	public static void copyDtoToDbo(AccessRequirement dto, DBOAccessRequirement dbo) throws DatastoreException{
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
		dbo.setNodeId(KeyFactory.stringToKey(dto.getEntityId()));
		dbo.setAccessType(dto.getAccessType().name());
		dbo.setEntityType(dto.getEntityType());
		copyToSerializedField(dto, dbo);
	}
	
	public static AccessRequirement copyDboToDto(DBOAccessRequirement dbo) throws DatastoreException {
		AccessRequirement dto = copyFromSerializedField(dbo);
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
		dto.setEntityId(KeyFactory.keyToString(dbo.getNodeId()));
		dto.setAccessType(ACCESS_TYPE.valueOf(dbo.getAccessType()));
		dto.setEntityType(dbo.getEntityType());
		return dto;
	}
	
	
	public static void copyToSerializedField(AccessRequirement dto, DBOAccessRequirement dbo) throws DatastoreException {
		try {
			dbo.setSerializedEntity(JDOSecondaryPropertyUtils.compressObject(dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	public static AccessRequirement copyFromSerializedField(DBOAccessRequirement dbo) throws DatastoreException {
		try {
			return (AccessRequirement)JDOSecondaryPropertyUtils.decompressedObject(dbo.getSerializedEntity());
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	

}
