package org.sagebionetworks.repo.model.helper;

import java.util.Date;
import java.util.function.Consumer;

import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataAccessRequestObjectHelper implements DaoObjectHelper<Request> {

	@Autowired
	private RequestDAO requestDao;
	
	@Override
	public Request create(Consumer<Request> consumer) {
		Request dto = new Request();

		dto.setId("1");
		dto.setAccessRequirementId("2");
		dto.setResearchProjectId("3");
		dto.setCreatedBy("4");
		dto.setCreatedOn(new Date());
		dto.setModifiedBy("5");
		dto.setModifiedOn(new Date());
		
		consumer.accept(dto);

		
		return requestDao.create(dto);
	}

}
