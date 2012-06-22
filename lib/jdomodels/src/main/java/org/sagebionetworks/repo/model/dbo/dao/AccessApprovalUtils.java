package org.sagebionetworks.repo.model.dbo.dao;

import java.util.Date;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApprovalType;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessApproval;
import org.sagebionetworks.schema.ObjectSchema;


/*
	private Long id;
	private Long eTag = 0L;
	private Long createdBy;
	private Date createdOn;
	private Long modifiedBy;
	private Date modifiedOn;
	private Long requirementId;
	private Long accessorId;
	private AccessApprovalType approvalType;
	private byte[] approvalParameters;
 */
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
	}
	
	public static void copyAccessApprovalParamsDtoToDbo(Object paramsDto, DBOAccessApproval dbo, ObjectSchema schema) throws DatastoreException {
		dbo.setApprovalParameters(SchemaSerializationUtils.mapDtoFieldsToAnnotations(paramsDto, schema));
	}
	
	public static void copyDboToDto(DBOAccessApproval dbo, AccessApproval dto) {
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
	}
	
	public static void copyApprovalParamsDboToDto(DBOAccessApproval dbo, Object paramsDto, ObjectSchema schema) throws DatastoreException {
		SchemaSerializationUtils.mapAnnotationsToDtoFields(dbo.getApprovalParameters(), paramsDto, schema);
	}
	
}
