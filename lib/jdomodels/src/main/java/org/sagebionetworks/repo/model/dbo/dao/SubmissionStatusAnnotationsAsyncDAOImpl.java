package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.evaluation.dao.AnnotationsDAO;
import org.sagebionetworks.evaluation.dao.SubmissionStatusDAO;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.SubmissionStatusAnnotationsAsyncDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class SubmissionStatusAnnotationsAsyncDAOImpl implements SubmissionStatusAnnotationsAsyncDAO {
	

	@Autowired
	private SubmissionStatusDAO submissionStatusDAO;
	@Autowired
	private AnnotationsDAO subStatusAnnoDAO;	
	
	public SubmissionStatusAnnotationsAsyncDAOImpl() {};
	
	public SubmissionStatusAnnotationsAsyncDAOImpl(SubmissionStatusDAO submissionStatusDAO, 
			AnnotationsDAO subStatusAnnoDAO) {
		this.submissionStatusDAO = submissionStatusDAO;
		this.subStatusAnnoDAO = subStatusAnnoDAO;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean updateSubmissionStatus(String subId) throws NotFoundException, DatastoreException, JSONObjectAdapterException {
		if (subId == null) throw new IllegalArgumentException("Id cannot be null");
		replaceSubmissionStatusAnnotations(subId);
		return true;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean deleteSubmission(String id) {
		if (id == null) throw new IllegalArgumentException("Id cannot be null");
		Long subId = KeyFactory.stringToKey(id);
		subStatusAnnoDAO.deleteAnnotationsByOwnerId(subId);
		return true;
	}

	private void replaceSubmissionStatusAnnotations(String subId) throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		SubmissionStatus subStatus = submissionStatusDAO.get(subId.toString());		
		subStatusAnnoDAO.replaceAnnotations(subStatus.getAnnotations());
	}
}
