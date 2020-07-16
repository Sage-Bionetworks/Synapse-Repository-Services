package org.sagebionetworks.repo.model.dbo.dao;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessApproval;

public class AccessApprovalUtils {

	public static void copyDtoToDbo(AccessApproval dto, DBOAccessApproval dbo) throws DatastoreException {
		dbo.setId(dto.getId());
		dbo.seteTag(dto.getEtag());
		if (dto.getCreatedBy()!=null) {
			dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		}
		if (dto.getCreatedOn()!=null) {
			dbo.setCreatedOn(dto.getCreatedOn().getTime());
		}
		dbo.setModifiedBy(Long.parseLong(dto.getModifiedBy()));
		dbo.setModifiedOn(dto.getModifiedOn().getTime());
		if (dto.getExpiredOn() != null) {
			dbo.setExpiredOn(dto.getExpiredOn().getTime());
		}
		dbo.setRequirementId(dto.getRequirementId());
		dbo.setAccessorId(Long.parseLong(dto.getAccessorId()));
		dbo.setRequirementVersion(dto.getRequirementVersion());
		dbo.setSubmitterId(Long.parseLong(dto.getSubmitterId()));
		dbo.setState(dto.getState().name());
	}

	public static AccessApproval copyDboToDto(DBOAccessApproval dbo) throws DatastoreException {
		AccessApproval dto = new AccessApproval();
		dto.setId(dbo.getId());
		dto.setEtag(dbo.geteTag());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setModifiedBy(dbo.getModifiedBy().toString());
		dto.setModifiedOn(new Date(dbo.getModifiedOn()));
		dto.setRequirementId(dbo.getRequirementId());
		dto.setAccessorId(dbo.getAccessorId().toString());
		dto.setRequirementVersion(dbo.getRequirementVersion());
		dto.setSubmitterId(dbo.getSubmitterId().toString());
		if (dbo.getExpiredOn() != 0L) {
			dto.setExpiredOn(new Date(dbo.getExpiredOn()));
		}
		dto.setState(ApprovalState.valueOf(dbo.getState()));
		return dto;
	}

	public static List<DBOAccessApproval> copyDtosToDbos(List<AccessApproval> dtos, boolean forCreation, IdGenerator idGenerator) {
		List<DBOAccessApproval> dbos = new LinkedList<DBOAccessApproval>();
		for (AccessApproval dto : dtos) {
			DBOAccessApproval dbo = new DBOAccessApproval();
			copyDtoToDbo(dto, dbo);
			if (forCreation) {
				dbo.setId(idGenerator.generateNewId(IdType.ACCESS_APPROVAL_ID));
				dbo.seteTag(UUID.randomUUID().toString());
			}
			dbos.add(dbo);
		}
		return dbos;
	}

	public static List<AccessApproval> copyDbosToDtos(List<DBOAccessApproval> dbos) {
		List<AccessApproval> dtos = new LinkedList<AccessApproval>();
		for (DBOAccessApproval dbo : dbos) {
			dtos.add(copyDboToDto(dbo));
		}
		return dtos;
	}
}
