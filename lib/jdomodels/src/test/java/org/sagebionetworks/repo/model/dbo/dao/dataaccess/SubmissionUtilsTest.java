package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;

public class SubmissionUtilsTest {

	@Test
	public void testRoundTrip() {
		Submission dto = SubmissionTestUtils.createSubmission();
		DBOSubmission dbo = new DBOSubmission();
		SubmissionUtils.copyDtoToDbo(dto, dbo);
		DBOSubmissionStatus status = SubmissionUtils.getDBOStatus(dto);

		Submission newDto = SubmissionUtils.copyDboToDto(dbo, status);
		assertEquals(dto, newDto);

		status.setState("APPROVED");
		newDto = SubmissionUtils.copyDboToDto(dbo, status);
		dto.setState(SubmissionState.APPROVED);
		assertEquals(dto, newDto);
	}

	@Test
	public void testCreateDBOSubmissionSubmitter() {
		IdGenerator mockIdGenerator = Mockito.mock(IdGenerator.class);
		when(mockIdGenerator.generateNewId(IdType.DATA_ACCESS_SUBMISSION_SUBMITTER_ID)).thenReturn(1L, 2L, 3L, 4L, 5L);
		Submission dto = SubmissionTestUtils.createSubmission();

		DBOSubmissionSubmitter submitter = SubmissionUtils.createDBOSubmissionSubmitter(dto, mockIdGenerator);
		assertNotNull(submitter.getId());
		assertNotNull(submitter.getEtag());
		assertEquals(dto.getId(), submitter.getCurrentSubmissionId().toString());
		assertEquals(dto.getAccessRequirementId(), submitter.getAccessRequirementId().toString());
		assertEquals(submitter.getSubmitterId().toString(), dto.getSubmittedBy());
	}

}
