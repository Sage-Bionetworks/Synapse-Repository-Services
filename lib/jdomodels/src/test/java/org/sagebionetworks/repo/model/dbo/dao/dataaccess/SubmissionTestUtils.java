package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.util.Arrays;
import java.util.Date;

import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;

public class SubmissionTestUtils {

	public static Submission createSubmission() {
		Submission dto = new Submission();
		dto.setId("1");
		dto.setAccessRequirementId("2");
		dto.setRequestId("3");
		ResearchProject researchProjectSnapshot = ResearchProjectTestUtils.createNewDto();
		dto.setResearchProjectSnapshot(researchProjectSnapshot);
		AccessorChange add = new AccessorChange();
		add.setUserId("6");
		add.setType(AccessType.GAIN_ACCESS);
		AccessorChange renew = new AccessorChange();
		renew.setType(AccessType.RENEW_ACCESS);
		renew.setUserId("7");
		AccessorChange revoke = new AccessorChange();
		revoke.setType(AccessType.REVOKE_ACCESS);
		revoke.setUserId("8");
		dto.setAccessorChanges(Arrays.asList(add, renew, revoke));
		dto.setAttachments(Arrays.asList("9"));
		dto.setDucFileHandleId("0");
		dto.setIrbFileHandleId("10");
		dto.setEtag("etag2");
		dto.setIsRenewalSubmission(true);
		dto.setPublication("publication");
		dto.setSummaryOfUse("summaryOfUse");
		dto.setModifiedBy("5");
		dto.setModifiedOn(new Date());
		dto.setSubmittedBy("5");
		dto.setSubmittedOn(new Date());
		dto.setState(SubmissionState.SUBMITTED);
		dto.setRejectedReason("no reasons");
		return dto;
	}

}
