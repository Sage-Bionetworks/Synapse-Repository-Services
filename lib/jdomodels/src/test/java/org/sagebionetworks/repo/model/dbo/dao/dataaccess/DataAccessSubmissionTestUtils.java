package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.util.Arrays;
import java.util.Date;

import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;

public class DataAccessSubmissionTestUtils {

	public static DataAccessSubmission createSubmission() {
		DataAccessSubmission dto = new DataAccessSubmission();
		dto.setId("1");
		dto.setAccessRequirementId("2");
		dto.setDataAccessRequestId("3");
		ResearchProject researchProjectSnapshot = ResearchProjectTestUtils.createNewDto();
		dto.setResearchProjectSnapshot(researchProjectSnapshot);
		dto.setAccessors(Arrays.asList("6", "7", "8"));
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
		dto.setState(DataAccessSubmissionState.SUBMITTED);
		dto.setRejectedReason("no reasons");
		return dto;
	}

}
