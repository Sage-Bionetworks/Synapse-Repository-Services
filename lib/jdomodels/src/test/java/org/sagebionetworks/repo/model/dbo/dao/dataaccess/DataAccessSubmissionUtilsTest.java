package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;

public class DataAccessSubmissionUtilsTest {

	@Test
	public void testRoundTrip() {
		DataAccessSubmission dto = createSubmission();
		DBODataAccessSubmission dbo = new DBODataAccessSubmission();
		DataAccessSubmissionUtils.copyDtoToDbo(dto, dbo);
		DBODataAccessSubmissionStatus status = DataAccessSubmissionUtils.getDBOStatus(dto);

		List<DBODataAccessSubmissionAccessor> accessors = DataAccessSubmissionUtils.getDBOAccessors(dto);
		Set<String> accessorsSet = new HashSet<String>();
		for (DBODataAccessSubmissionAccessor accessor : accessors) {
			assertEquals(dto.getId(), accessor.getSubmissionId().toString());
			accessorsSet.add(accessor.getAccessorId().toString());
		}
		assertEquals(accessorsSet, new HashSet<String>(dto.getAccessors()));

		DataAccessSubmission newDto = DataAccessSubmissionUtils.copyDboToDto(dbo, status);
		assertEquals(dto, newDto);

		status.setState(DataAccessSubmissionState.APPROVED);
		newDto = DataAccessSubmissionUtils.copyDboToDto(dbo, status);
		dto.setState(DataAccessSubmissionState.APPROVED);
		assertEquals(dto, newDto);
	}

	public DataAccessSubmission createSubmission() {
		DataAccessSubmission dto = new DataAccessSubmission();
		dto.setId("1");
		dto.setAccessRequirementId("2");
		dto.setDataAccessRequestId("3");
		ResearchProject researchProjectSnapshot = new ResearchProject();
		researchProjectSnapshot.setId("4");
		researchProjectSnapshot.setAccessRequirementId("2");
		researchProjectSnapshot.setInstitution("Sage");
		researchProjectSnapshot.setProjectLead("projectLead");
		researchProjectSnapshot.setIntendedDataUseStatement("intendedDataUseStatement");
		researchProjectSnapshot.setCreatedBy("5");
		researchProjectSnapshot.setCreatedOn(new Date());
		researchProjectSnapshot.setModifiedBy("5");
		researchProjectSnapshot.setModifiedOn(new Date());
		researchProjectSnapshot.setEtag("etag1");
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
