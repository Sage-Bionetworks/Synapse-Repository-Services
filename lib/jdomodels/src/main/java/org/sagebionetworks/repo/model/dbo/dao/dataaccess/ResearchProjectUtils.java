package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.nio.charset.Charset;

import org.sagebionetworks.repo.model.dataaccess.ResearchProject;

public class ResearchProjectUtils {
	public static final Charset UTF8 = Charset.forName("UTF-8");

	public static void copyDtoToDbo(ResearchProject dto, DBOResearchProject dbo) {
		dbo.setId(Long.parseLong(dto.getId()));
		dbo.setAccessRequirementId(Long.parseLong(dto.getAccessRequirementId()));
		dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		dbo.setCreatedOn(dto.getCreatedOn());
		dbo.setModifiedBy(Long.parseLong(dto.getModifiedBy()));
		dbo.setModifiedOn(dto.getModifiedOn());
		dbo.setOwnerId(Long.parseLong(dto.getOwnerId()));
		dbo.setEtag(dto.getEtag());
		dbo.setProjectLead(dto.getProjectLead());
		dbo.setInstitution(dto.getInstitution());
		dbo.setIdu(dto.getIntendedDataUseStatement().getBytes(UTF8));
	}

	public static void copyDboToDto(DBOResearchProject dbo, ResearchProject dto) {
		dto.setId(dbo.getId().toString());
		dto.setAccessRequirementId(dbo.getAccessRequirementId().toString());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(dbo.getCreatedOn());
		dto.setModifiedBy(dbo.getModifiedBy().toString());
		dto.setModifiedOn(dbo.getModifiedOn());
		dto.setOwnerId(dbo.getOwnerId().toString());
		dto.setEtag(dbo.getEtag());
		dto.setProjectLead(dbo.getProjectLead());
		dto.setInstitution(dbo.getInstitution());
		dto.setIntendedDataUseStatement(new String(dbo.getIdu(), UTF8));
	}
}
