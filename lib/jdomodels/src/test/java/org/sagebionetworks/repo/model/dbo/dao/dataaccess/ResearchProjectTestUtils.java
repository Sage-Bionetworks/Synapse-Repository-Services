package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.util.Date;

import org.sagebionetworks.repo.model.dataaccess.ResearchProject;

public class ResearchProjectTestUtils {

	public static ResearchProject createNewDto() {
		ResearchProject dto = new ResearchProject();
		dto.setId("1");
		dto.setAccessRequirementId("2");
		dto.setCreatedBy("3");
		dto.setCreatedOn(new Date());
		dto.setModifiedBy("4");
		dto.setModifiedOn(new Date());
		dto.setOwnerId("5");
		dto.setEtag("etag");
		dto.setProjectLead("projectLead");
		dto.setInstitution("institution");
		dto.setIntendedDataUseStatement("intendedDataUseStatement");
		return dto;
	}
}
