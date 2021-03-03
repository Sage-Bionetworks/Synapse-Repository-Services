package org.sagebionetworks.repo.model.helper;

import java.util.Date;
import java.util.function.Consumer;

import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataAccessSubmissionObjectHelper implements DaoObjectHelper<Submission> {
	
	@Autowired
	private SubmissionDAO submissionDao;

	@Override
	public Submission create(Consumer<Submission> consumer) {
		Submission dto = new Submission();
		dto.setId("1");
		dto.setAccessRequirementId("2");
		dto.setRequestId("3");
		dto.setIsRenewalSubmission(true);
		dto.setPublication("publication");
		dto.setSummaryOfUse("summaryOfUse");
		dto.setModifiedBy("5");
		dto.setModifiedOn(new Date());
		dto.setSubmittedBy("5");
		dto.setSubmittedOn(new Date());
		dto.setState(SubmissionState.SUBMITTED);
		dto.setRejectedReason("no reasons");
		
		consumer.accept(dto);
		
		return submissionDao.getSubmission(submissionDao.createSubmission(dto).getSubmissionId());
	}
}
