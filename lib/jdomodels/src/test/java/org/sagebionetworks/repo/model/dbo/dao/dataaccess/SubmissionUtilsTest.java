package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

import com.google.common.collect.ImmutableSet;

public class SubmissionUtilsTest {

	@Test
	public void testRoundTrip() {
		Submission dto = SubmissionTestUtils.createSubmission();
		DBOSubmission dbo = new DBOSubmission();
		SubmissionUtils.copyDtoToDbo(dto, dbo);
		DBOSubmissionStatus status = SubmissionUtils.getDBOStatus(dto);
		// the accessor changes are stored in their own table
		dto.setAccessorChanges(null);

		Submission newDto = SubmissionUtils.copyDboToDto(dbo, status);
		assertEquals(dto, newDto);

		status.setState("APPROVED");
		newDto = SubmissionUtils.copyDboToDto(dbo, status);
		dto.setState(SubmissionState.APPROVED);
		assertEquals(dto, newDto);
	}

	@Test
	public void testCopyDboToDtoWithSchemaData() {
		Submission dto = SubmissionTestUtils.createSubmission();
		JSONObject schemaData = new JSONObject("{\"projectLead\":\"lead\",\"institution\":{\"name\":\"inst\"},\"n\":[1,2.5,true]}");
		dto.setSchemaData(schemaData);
		DBOSubmission dbo = new DBOSubmission();
		SubmissionUtils.copyDtoToDbo(dto, dbo);
		DBOSubmissionStatus status = SubmissionUtils.getDBOStatus(dto);

		// call under test
		Submission newDto = SubmissionUtils.copyDboToDto(dbo, status);

		// The free-form schemaData comes back as a JSONObjectAdapter, which is never equal to the caller's JSONObject
		assertTrue(schemaData.similar(new JSONObject(newDto.getSchemaData().toString())));
		newDto.setSchemaData(schemaData);
		dto.setAccessorChanges(null);
		assertEquals(dto, newDto);
	}

	@Test
	public void testCreateSubmissionJsonWithColumnFields() {
		Submission dto = SubmissionTestUtils.createSubmission();
		Submission original = JDOSecondaryPropertyUtils.createObjectFromJSON(Submission.class, JDOSecondaryPropertyUtils.createJSONFromObject(dto));

		// call under test
		String json = SubmissionUtils.createSubmissionJson(dto);

		// None of the fields with their own column, or stored in the status and accessor changes tables
		assertEquals(Set.of("researchProjectSnapshot", "isRenewalSubmission", "publication", "summaryOfUse", "subjectId", "subjectType"),
				new JSONObject(json).keySet());
		// The fields are cleared on a copy, the caller's submission is unchanged
		assertEquals(original, dto);
	}

	@Test
	public void testCopyDboToDtoWithNullOptionalFields() {
		Submission dto = SubmissionTestUtils.createSubmission();
		dto.setAttachments(null);
		dto.setDucFileHandleId(null);
		dto.setIrbFileHandleId(null);
		dto.setPublication(null);
		dto.setSummaryOfUse(null);
		dto.setSubjectId(null);
		dto.setSubjectType(null);
		dto.setIsRenewalSubmission(false);
		DBOSubmission dbo = new DBOSubmission();
		SubmissionUtils.copyDtoToDbo(dto, dbo);
		DBOSubmissionStatus status = SubmissionUtils.getDBOStatus(dto);
		dto.setAccessorChanges(null);

		// call under test
		Submission newDto = SubmissionUtils.copyDboToDto(dbo, status);

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
	public void testCreateDBOSubmissionAccessorChanges() {
		Submission dto = SubmissionTestUtils.createSubmission();

		List<DBOSubmissionAccessorChange> expected = dto.getAccessorChanges().stream().map(c -> {
			DBOSubmissionAccessorChange add = new DBOSubmissionAccessorChange();
			add.setAccessorId(Long.valueOf(c.getUserId()));
			add.setAccessType(c.getType().name());
			add.setSubmissionId(Long.valueOf(dto.getId()));
			return add;
		}).collect(Collectors.toList());
		
		List<DBOSubmissionAccessorChange> result = SubmissionUtils.createDBOSubmissionAccessorChanges(dto);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testCreateDBOSubmissionAccessorChangesWithNull() {
		Submission dto = SubmissionTestUtils.createSubmission();
		
		dto.setAccessorChanges(null);

		List<DBOSubmissionAccessorChange> expected = Collections.emptyList();
		
		List<DBOSubmissionAccessorChange> result = SubmissionUtils.createDBOSubmissionAccessorChanges(dto);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testCreateDBOSubmissionAccessorChangesWithEmpty() {
		Submission dto = SubmissionTestUtils.createSubmission();
		
		dto.setAccessorChanges(Collections.emptyList());

		List<DBOSubmissionAccessorChange> expected = Collections.emptyList();
		
		List<DBOSubmissionAccessorChange> result = SubmissionUtils.createDBOSubmissionAccessorChanges(dto);
		
		assertEquals(expected, result);
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
