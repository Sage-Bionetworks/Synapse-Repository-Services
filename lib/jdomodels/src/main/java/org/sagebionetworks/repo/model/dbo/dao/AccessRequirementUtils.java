package org.sagebionetworks.repo.model.dbo.dao;

import java.util.Date;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirement;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.schema.ObjectSchema;

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
		dbo.setRequirementType(dto.getAccessRequirementType().name());
	}
	
	public static void copyAccessRequirementParamsDtoToDbo(Object paramsDto, DBOAccessRequirement dbo, ObjectSchema schema) throws DatastoreException {
		dbo.setRequirementParameters(SchemaSerializationUtils.mapDtoFieldsToAnnotations(paramsDto, schema));
	}
	
	public static void copyDboToDto(DBOAccessRequirement dbo, AccessRequirement dto) {
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
		dto.setAccessRequirementType(AccessRequirementType.valueOf(dbo.getRequirementType()));
	}
	
	public static void copyRequirementParamsDboToDto(DBOAccessRequirement dbo, Object paramsDto, ObjectSchema schema) throws DatastoreException {
		SchemaSerializationUtils.mapAnnotationsToDtoFields(dbo.getRequirementParameters(), paramsDto, schema);
	}
	
}
