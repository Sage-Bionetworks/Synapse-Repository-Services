package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;

import com.google.common.collect.ImmutableSet;

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
	
	@Test
	public void testExtractAllFileHandleIds() {
		Submission submission = SubmissionTestUtils.createSubmission();
		
		Set<String> expected = ImmutableSet.of("0", "9", "10");
		
		Set<String> result = SubmissionUtils.extractAllFileHandleIds(submission);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testExtractAllFileHandleIdsWithNoAttachments() {
		Submission submission = SubmissionTestUtils.createSubmission();
		
		submission.setAttachments(null);
		
		Set<String> expected = ImmutableSet.of("0", "10");
		
		Set<String> result = SubmissionUtils.extractAllFileHandleIds(submission);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testExtractAllFileHandleIdsWithNoIrb() {
		Submission submission = SubmissionTestUtils.createSubmission();

		submission.setIrbFileHandleId(null);
		
		Set<String> expected = ImmutableSet.of("0", "9");
		
		Set<String> result = SubmissionUtils.extractAllFileHandleIds(submission);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testExtractAllFileHandleIdsWithNoDuc() {
		Submission submission = SubmissionTestUtils.createSubmission();

		submission.setDucFileHandleId(null);
		
		Set<String> expected = ImmutableSet.of("9", "10");
		
		Set<String> result = SubmissionUtils.extractAllFileHandleIds(submission);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testExtractAllFileHandleIdsWithNoFileHandles() {
		Submission submission = SubmissionTestUtils.createSubmission();

		submission.setDucFileHandleId(null);
		submission.setIrbFileHandleId(null);
		submission.setAttachments(null);
		
		Set<String> expected = Collections.emptySet();
		
		Set<String> result = SubmissionUtils.extractAllFileHandleIds(submission);
		
		assertEquals(expected, result);
	}
	

}
