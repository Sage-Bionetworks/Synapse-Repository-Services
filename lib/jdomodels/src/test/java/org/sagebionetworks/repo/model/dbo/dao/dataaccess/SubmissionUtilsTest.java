package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.internal.SubmissionContent;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

import com.google.common.collect.ImmutableSet;

public class SubmissionUtilsTest {

	/**
	 * @return The given submission without the fields stored in the status and accessor changes tables, which
	 *         {@link SubmissionUtils#toDto(DBOSubmission)} does not set
	 */
	private static Submission withoutStatusAndAccessorChanges(Submission dto) {
		return dto.setState(null).setModifiedBy(null).setModifiedOn(null).setRejectedReason(null).setAccessorChanges(null);
	}

	@Test
	public void testToDboWithAllFields() {
		Submission dto = SubmissionTestUtils.createSubmission();
		dto.setAccessRequirementVersion(4L);
		DBOSubmission expected = new DBOSubmission();
		expected.setId(1L);
		expected.setAccessRequirementId(2L);
		expected.setAccessRequirementVersion(4L);
		expected.setDataAccessRequestId(3L);
		expected.setCreatedBy(5L);
		expected.setCreatedOn(dto.getSubmittedOn().getTime());
		expected.setEtag("etag2");
		expected.setResearchProjectId(101L);
		expected.setDucFileHandleId(0L);
		expected.setIrbFileHandleId(10L);
		expected.setAttachmentsJson("[\"9\"]");

		// call under test
		DBOSubmission dbo = SubmissionUtils.toDbo(dto);

		// None of the fields with their own column, or stored in the status and accessor changes tables
		assertEquals(Set.of("researchProjectSnapshot", "isRenewalSubmission", "publication", "summaryOfUse", "subjectId", "subjectType"),
				new JSONObject(dbo.getSubmissionJson()).keySet());
		// The order of the keys in the document is not relevant, it was verified above
		expected.setSubmissionJson(dbo.getSubmissionJson());
		assertEquals(expected, dbo);
	}

	@Test
	public void testToContentWithAllFields() {
		Submission dto = SubmissionTestUtils.createSubmission();
		JSONObject schemaData = new JSONObject("{\"n\":1}");
		dto.setSchemaData(schemaData);
		SubmissionContent expected = new SubmissionContent()
				.setResearchProjectSnapshot(dto.getResearchProjectSnapshot())
				.setIsRenewalSubmission(true)
				.setPublication("publication")
				.setSummaryOfUse("summaryOfUse")
				.setSubjectId("syn123")
				.setSubjectType(dto.getSubjectType())
				.setSchemaData(schemaData);

		// call under test
		SubmissionContent content = SubmissionUtils.toContent(dto);

		assertEquals(expected, content);
	}

	@Test
	public void testToDtoWithAllFields() {
		Submission dto = SubmissionTestUtils.createSubmission();
		DBOSubmission dbo = SubmissionUtils.toDbo(dto);

		// call under test
		Submission result = SubmissionUtils.toDto(dbo);

		assertEquals(withoutStatusAndAccessorChanges(dto), result);
	}

	@Test
	public void testToDtoWithSchemaData() {
		Submission dto = SubmissionTestUtils.createSubmission();
		JSONObject schemaData = new JSONObject("{\"projectLead\":\"lead\",\"institution\":{\"name\":\"inst\"},\"n\":[1,2.5,true]}");
		dto.setSchemaData(schemaData);
		DBOSubmission dbo = SubmissionUtils.toDbo(dto);

		// call under test
		Submission result = SubmissionUtils.toDto(dbo);

		// The free-form schemaData comes back as a JSONObjectAdapter, which is never equal to the caller's JSONObject
		assertTrue(schemaData.similar(new JSONObject(result.getSchemaData().toString())));
		result.setSchemaData(schemaData);
		assertEquals(withoutStatusAndAccessorChanges(dto), result);
	}

	@Test
	public void testToDtoWithNullOptionalFields() {
		Submission dto = SubmissionTestUtils.createSubmission();
		dto.setAttachments(null);
		dto.setDucFileHandleId(null);
		dto.setIrbFileHandleId(null);
		dto.setPublication(null);
		dto.setSummaryOfUse(null);
		dto.setSubjectId(null);
		dto.setSubjectType(null);
		dto.setIsRenewalSubmission(false);
		DBOSubmission dbo = SubmissionUtils.toDbo(dto);

		// call under test
		Submission result = SubmissionUtils.toDto(dbo);

		assertEquals(withoutStatusAndAccessorChanges(dto), result);
	}

	@Test
	public void testToDtoWithStoredDocument() {
		Submission dto = SubmissionTestUtils.createSubmission();
		DBOSubmission dbo = SubmissionUtils.toDbo(dto);
		// A document written as the submission with its column fields cleared
		dbo.setSubmissionJson(new JSONObject()
				.put("researchProjectSnapshot", new JSONObject(JDOSecondaryPropertyUtils.createJSONFromObject(dto.getResearchProjectSnapshot())))
				.put("isRenewalSubmission", true).put("publication", "publication").put("summaryOfUse", "summaryOfUse")
				.put("subjectId", "syn123").put("subjectType", "ENTITY").toString());

		// call under test
		Submission result = SubmissionUtils.toDto(dbo);

		assertEquals(withoutStatusAndAccessorChanges(dto), result);
	}

	@Test
	public void testReadAccessorChangesWithUnorderedJson() {
		String json = "[{\"type\": \"REVOKE_ACCESS\", \"userId\": \"10\"}, {\"type\": \"GAIN_ACCESS\", \"userId\": \"9\"}]";
		List<AccessorChange> expected = Arrays.asList(new AccessorChange().setUserId("9").setType(AccessType.GAIN_ACCESS),
				new AccessorChange().setUserId("10").setType(AccessType.REVOKE_ACCESS));

		// call under test
		List<AccessorChange> result = SubmissionUtils.readAccessorChanges(json);

		// ordered by numeric user id, not by text
		assertEquals(expected, result);
	}

	@Test
	public void testReadAccessorChangesWithNull() {
		// call under test
		List<AccessorChange> result = SubmissionUtils.readAccessorChanges(null);

		assertEquals(Collections.emptyList(), result);
	}

	@Test
	public void testGetDBOStatus() {
		Submission dto = SubmissionTestUtils.createSubmission();
		dto.setSubmittedOn(new Date(1700000000000L));
		dto.setModifiedOn(new Date(1700000001000L));
		DBOSubmissionStatus expected = new DBOSubmissionStatus();
		expected.setSubmisionId(1L);
		expected.setCreatedBy(5L);
		expected.setCreatedOn(1700000000000L);
		expected.setModifiedBy(5L);
		expected.setModifiedOn(1700000001000L);
		expected.setState("SUBMITTED");
		expected.setReason("no reasons".getBytes(StandardCharsets.UTF_8));

		// call under test
		DBOSubmissionStatus status = SubmissionUtils.getDBOStatus(dto);

		assertEquals(expected, status);
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
