package org.sagebionetworks.repo.model.dbo.dao;

import java.sql.Timestamp;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.persistence.DBODoi;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

class DoiUtils {

	static Doi convertToDto(DBODoi dbo) {
		if (dbo == null) {
			throw new IllegalArgumentException("DBO cannot be null.");
		}
		Doi dto = new Doi();
		dto.setId(dbo.getId().toString());
		dto.setEtag(dbo.getETag());
		dto.setDoiStatus(DoiStatus.valueOf(dbo.getDoiStatus()));
		final ObjectType objectType = ObjectType.valueOf(dbo.getObjectType());
		if (ObjectType.ENTITY.equals(objectType)) {
			dto.setObjectId(KeyFactory.keyToString(dbo.getObjectId()));
		} else {
			dto.setObjectId(dbo.getObjectId().toString());
		}
		dto.setObjectType(objectType);
		dto.setObjectVersion(dbo.getObjectVersion());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(dbo.getCreatedOn());
		dto.setUpdatedOn(dbo.getUpdatedOn());
		return dto;
	}

	static DBODoi convertToDbo(Doi dto) {
		if (dto == null) {
			throw new IllegalArgumentException("DTO cannot be null.");
		}
		DBODoi dbo = new DBODoi();
		dbo.setId(Long.valueOf(dto.getId()));
		dbo.setETag(dto.getEtag());
		dbo.setDoiStatus(dto.getDoiStatus());
		dbo.setObjectId(KeyFactory.stringToKey(dto.getObjectId()));
		dbo.setObjectType(dto.getObjectType());
		dbo.setObjectVersion(dto.getObjectVersion());
		dbo.setCreatedBy(Long.valueOf(dto.getCreatedBy()));
		dbo.setCreatedOn(new Timestamp(dto.getCreatedOn().getTime()));
		dbo.setUpdatedOn(new Timestamp(dto.getUpdatedOn().getTime()));
		return dbo;
	}
}
