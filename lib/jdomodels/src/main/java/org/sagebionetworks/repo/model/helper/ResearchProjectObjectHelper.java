package org.sagebionetworks.repo.model.helper;

import java.util.Date;
import java.util.function.Consumer;

import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResearchProjectObjectHelper implements DaoObjectHelper<ResearchProject>{
	
	@Autowired
	private ResearchProjectDAO researchProjectDao;

	@Override
	public ResearchProject create(Consumer<ResearchProject> consumer) {
		ResearchProject dto = new ResearchProject();
		dto.setAccessRequirementId("2");
		dto.setCreatedBy("1");
		dto.setCreatedOn(new Date());
		dto.setModifiedBy("4");
		dto.setModifiedOn(new Date());
		dto.setEtag("etag");
		dto.setInstitution("institution");
		dto.setProjectLead("Project Lead");

		consumer.accept(dto);
		
		return researchProjectDao.create(dto);
	}

}
