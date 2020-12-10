package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.nio.charset.Charset;
import java.util.Date;

import org.sagebionetworks.repo.model.dataaccess.ResearchProject;

public class ResearchProjectUtils {
	public static final Charset UTF8 = Charset.forName("UTF-8");

	public static void copyDtoToDbo(ResearchProject dto, DBOResearchProject dbo) {
		dbo.setId(Long.parseLong(dto.getId()));
		dbo.setAccessRequirementId(Long.parseLong(dto.getAccessRequirementId()));
		dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		dbo.setModifiedBy(Long.parseLong(dto.getModifiedBy()));
		dbo.setModifiedOn(dto.getModifiedOn().getTime());
		dbo.setEtag(dto.getEtag());
		dbo.setProjectLead(dto.getProjectLead());
		dbo.setInstitution(dto.getInstitution());
		
		if (dto.getIntendedDataUseStatement() != null) {
			dbo.setIdu(dto.getIntendedDataUseStatement().getBytes(UTF8));
		}
	}

	public static void copyDboToDto(DBOResearchProject dbo, ResearchProject dto) {
		dto.setId(dbo.getId().toString());
		dto.setAccessRequirementId(dbo.getAccessRequirementId().toString());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setModifiedBy(dbo.getModifiedBy().toString());
		dto.setModifiedOn(new Date(dbo.getModifiedOn()));
		dto.setEtag(dbo.getEtag());
		dto.setProjectLead(dbo.getProjectLead());
		dto.setInstitution(dbo.getInstitution());
		
		if (dbo.getIdu() != null) {
			dto.setIntendedDataUseStatement(new String(dbo.getIdu(), UTF8));
		}
	}
}
