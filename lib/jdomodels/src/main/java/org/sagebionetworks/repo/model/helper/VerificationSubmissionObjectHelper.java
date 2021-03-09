package org.sagebionetworks.repo.model.helper;

import java.util.Collections;
import java.util.Date;
import java.util.function.Consumer;

import org.sagebionetworks.repo.model.dbo.verification.VerificationDAO;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VerificationSubmissionObjectHelper implements DaoObjectHelper<VerificationSubmission> {

	private VerificationDAO verificationDao;
	
	@Autowired
	public VerificationSubmissionObjectHelper(VerificationDAO verificationDao) {
		this.verificationDao = verificationDao;
	}
	
	@Override
	public VerificationSubmission create(Consumer<VerificationSubmission> consumer) {
		
		VerificationSubmission submission = new VerificationSubmission();
		
		submission.setCreatedBy("123");
		submission.setCreatedOn(new Date());
		submission.setAttachments(Collections.emptyList());
		
		consumer.accept(submission);
		
		return verificationDao.createVerificationSubmission(submission);
	}

}
