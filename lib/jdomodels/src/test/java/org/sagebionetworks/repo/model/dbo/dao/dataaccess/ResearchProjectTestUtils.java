package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.util.Date;

import org.sagebionetworks.repo.model.dataaccess.ResearchProject;

public class ResearchProjectTestUtils {

	public static ResearchProject createNewDto() {
		return createNewDto("3", "projectLead", "institution", "intendedDataUseStatement");
	}
		
	public static ResearchProject createNewDto(String createdBy, String projectLead, String institution, String idu) {
		ResearchProject dto = new ResearchProject();
		dto.setAccessRequirementId("2");
		dto.setCreatedBy(createdBy);
		dto.setCreatedOn(new Date());
		dto.setModifiedBy("4");
		dto.setModifiedOn(new Date());
		dto.setEtag("etag");
		dto.setProjectLead(projectLead);
		dto.setInstitution("institution");
		dto.setIntendedDataUseStatement(idu);
		return dto;
	}
}
