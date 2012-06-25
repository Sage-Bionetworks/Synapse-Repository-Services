package org.sagebionetworks.repo.model.dbo.dao;

import java.util.Date;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessApproval;
import org.sagebionetworks.schema.ObjectSchema;

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
		dbo.setApprovalType(dto.getApprovalType().name());
		copyAccessApprovalParamsDtoToDbo(dto, dbo);
	}
	
	public static void copyAccessApprovalParamsDtoToDbo(AccessApproval dto, DBOAccessApproval dbo) throws DatastoreException {
		Object params = SchemaSerializationUtils.getParamsField(dto);
		ObjectSchema schema = SchemaSerializationUtils.getParamsSchema(dto);
		dbo.setApprovalParameters(SchemaSerializationUtils.mapDtoFieldsToAnnotations(params, schema));
	}
	
	public static void copyDboToDto(DBOAccessApproval dbo, AccessApproval dto) throws DatastoreException {
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
		dto.setApprovalType(AccessApprovalType.valueOf(dbo.getApprovalType()));
		copyApprovalParamsDboToDto(dbo, dto);
	}
	
	public static void copyApprovalParamsDboToDto(DBOAccessApproval dbo, AccessApproval dto) throws DatastoreException {
		Object params = SchemaSerializationUtils.setParamsField(dto);
		ObjectSchema schema = SchemaSerializationUtils.getParamsSchema(dto);
		SchemaSerializationUtils.mapAnnotationsToDtoFields(dbo.getApprovalParameters(), params, schema);
	}
	
}
