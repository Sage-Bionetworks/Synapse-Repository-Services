package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirement;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

public class AccessRequirementUtils {

	// the convention is that the individual fields take precedence
	// over the serialized objects.  When restoring the dto we first deserialize
	// the 'blob' and then populate the individual fields

	public static void copyDtoToDbo(AccessRequirement dto, DBOAccessRequirement dbo) throws DatastoreException {
		dbo.setId(dto.getId());
		dbo.seteTag(dto.getEtag());
		if (dto.getCreatedBy()!=null) dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		if (dto.getCreatedOn()!=null) dbo.setCreatedOn(dto.getCreatedOn().getTime());
		dbo.setModifiedBy(Long.parseLong(dto.getModifiedBy()));
		dbo.setModifiedOn(dto.getModifiedOn().getTime());
		dbo.setAccessType(dto.getAccessType().name());
		dbo.setEntityType(dto.getEntityType());
		copyToSerializedField(dto, dbo);
	}

	public static AccessRequirement copyDboToDto(DBOAccessRequirement dbo, List<Long> entities) throws DatastoreException {
		AccessRequirement dto = copyFromSerializedField(dbo);
		dto.setId(dbo.getId());
		dto.setEtag(dbo.geteTag());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setModifiedBy(dbo.getModifiedBy().toString());
		dto.setModifiedOn(new Date(dbo.getModifiedOn()));
		List<String> entityIds = new ArrayList<String>();
		for (Long id : entities) entityIds.add(KeyFactory.keyToString(id));
		dto.setEntityIds(entityIds);
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
