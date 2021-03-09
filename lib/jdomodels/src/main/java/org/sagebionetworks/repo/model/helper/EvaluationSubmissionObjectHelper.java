package org.sagebionetworks.repo.model.helper;

import java.util.Date;
import java.util.function.Consumer;

import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EvaluationSubmissionObjectHelper implements DaoObjectHelper<Submission> {

	private IdGenerator idGenerator;
	private SubmissionDAO submissionDao;
	
	@Autowired
	public EvaluationSubmissionObjectHelper(IdGenerator idGenerator, SubmissionDAO submissionDao) {
		this.idGenerator = idGenerator;
		this.submissionDao = submissionDao;
	}

	@Override
	public Submission create(Consumer<Submission> consumer) {
		Submission submission = new Submission();
		
		submission.setId(idGenerator.generateNewId(IdType.EVALUATION_SUBMISSION_ID).toString());
		submission.setEvaluationId("123");
		submission.setUserId("123");
		submission.setEntityId("123");
		submission.setVersionNumber(1L);
		submission.setCreatedOn(new Date());
		
		consumer.accept(submission);
		
		String submissionId = submissionDao.create(submission);
		
		return submissionDao.get(submissionId);
	}

}
