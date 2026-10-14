package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.schema.JsonSchemaConstants;

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
	public void testRoundTripWithSchemaData() {
		Submission dto = SubmissionTestUtils.createSubmission();
		// The answers snapshotted onto a schema based submission, with the submission context the
		// server validated them under.
		Map<String, Object> schemaData = Map.of("institution", Map.of("name", "Sage"), "yearsOfAccess", 2,
				JsonSchemaConstants.SUBMISSION_CONTEXT_PROPERTY, JsonSchemaConstants.SUBMISSION_CONTEXT_REQUEST);
		dto.setSchemaData(schemaData);

		DBOSubmission dbo = new DBOSubmission();
		SubmissionUtils.copyDtoToDbo(dto, dbo);
		Submission newDto = SubmissionUtils.copyDboToDto(dbo, SubmissionUtils.getDBOStatus(dto));

		// The blob stores canonical JSON text, so it comes back in the wire friendly shape an
		// opaque-Object field expects rather than as the caller's Map.
		assertTrue(new JSONObject(schemaData).similar(newDto.getSchemaData()),
				() -> "Unexpected schemaData: " + newDto.getSchemaData());
		// Serializing must not replace the caller's own value with the JSON text.
		assertEquals(schemaData, dto.getSchemaData());
	}

	@Test
	public void testRoundTripWithScalarSchemaData() {
		Submission dto = SubmissionTestUtils.createSubmission();
		dto.setSchemaData(Boolean.TRUE);

		DBOSubmission dbo = new DBOSubmission();
		SubmissionUtils.copyDtoToDbo(dto, dbo);
		Submission newDto = SubmissionUtils.copyDboToDto(dbo, SubmissionUtils.getDBOStatus(dto));

		assertEquals(Boolean.TRUE, newDto.getSchemaData());
	}

	@Test
	public void testRoundTripWithNullSchemaData() {
		Submission dto = SubmissionTestUtils.createSubmission();

		DBOSubmission dbo = new DBOSubmission();
		SubmissionUtils.copyDtoToDbo(dto, dbo);
		Submission newDto = SubmissionUtils.copyDboToDto(dbo, SubmissionUtils.getDBOStatus(dto));

		assertNull(newDto.getSchemaData());
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
