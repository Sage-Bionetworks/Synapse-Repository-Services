package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.PrincipalInvestigator;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.SigningOfficial;

import com.google.common.collect.ImmutableSet;

public class RequestUtilsTest {

	@Test
	public void testCopyDtoToDboRoundTrip() {
		Renewal dto = RequestTestUtils.createNewRenewal();

		DBORequest dbo = new DBORequest();
		RequestUtils.copyDtoToDbo(dto, dbo);
		Renewal newDto = (Renewal) RequestUtils.copyDboToDto(dbo);
		assertEquals(dto, newDto);
	}

	@Test
	public void testCopyDtoToDboRoundTripWithSchemaData() {
		Request dto = RequestTestUtils.createNewRequest();
		// Saved progress is partial and unvalidated: a nested object, a property the bound schema
		// does not declare, and no answer at all for the remaining ones.
		JSONObject schemaData = new JSONObject(Map.of("institution", Map.of("name", "Sage"), "notASchemaProperty", 42));
		dto.setSchemaData(schemaData);

		DBORequest dbo = new DBORequest();
		RequestUtils.copyDtoToDbo(dto, dbo);
		RequestInterface newDto = RequestUtils.copyDboToDto(dbo);

		assertTrue(schemaData.similar(newDto.getSchemaData()), () -> "Unexpected schemaData: " + newDto.getSchemaData());
		assertEquals(schemaData, dto.getSchemaData());
	}

	@Test
	public void testCopyDtoToDboRoundTripWithSchemaDataFromTheWire() throws JSONObjectAdapterException {
		Request dto = RequestTestUtils.createNewRequest();
		// An object taken off the wire is an adapter rather than a JSONObject
		dto.setSchemaData(new JSONObjectAdapterImpl("{\"institution\":{\"name\":\"Sage\"},\"count\":42}"));

		DBORequest dbo = new DBORequest();
		RequestUtils.copyDtoToDbo(dto, dbo);
		RequestInterface newDto = RequestUtils.copyDboToDto(dbo);

		assertTrue(new JSONObject("{\"institution\":{\"name\":\"Sage\"},\"count\":42}").similar(newDto.getSchemaData()),
				() -> "Unexpected schemaData: " + newDto.getSchemaData());
	}

	@Test
	public void testCopyDtoToDboRoundTripWithNullResearchProjectId() {
		Request dto = RequestTestUtils.createNewRequest();
		// A request answering a JsonSchemaAccessRequirement has no research project.
		dto.setResearchProjectId(null);

		DBORequest dbo = new DBORequest();
		RequestUtils.copyDtoToDbo(dto, dbo);
		RequestInterface newDto = RequestUtils.copyDboToDto(dbo);

		assertEquals(dto, newDto);
	}

	private static void populateAllFields(RequestInterface dto) {
		dto.setId("101");
		dto.setAccessRequirementId("202");
		dto.setAccessRequirementVersionNumber(3L);
		dto.setResearchProjectId("303");
		dto.setCreatedOn(new Date(1700000000000L));
		dto.setModifiedOn(new Date(1700000100000L));
		dto.setCreatedBy("404");
		dto.setModifiedBy("505");
		dto.setDucFileHandleId("9001");
		dto.setIrbFileHandleId("9002");
		dto.setAttachments(Arrays.asList("9003", "9004"));
		AccessorChange gain = new AccessorChange().setUserId("6").setType(AccessType.GAIN_ACCESS);
		AccessorChange revoke = new AccessorChange().setUserId("7").setType(AccessType.REVOKE_ACCESS);
		dto.setAccessorChanges(Arrays.asList(gain, revoke));
		dto.setEtag("etag-1234");
		dto.setInstitution("Sage Bionetworks");
		dto.setPrincipalInvestigator(
				new PrincipalInvestigator().setUserId("606").setName("Ada PI").setInstitutionalEmail("ada@sage.org"));
		dto.setSigningOfficial(new SigningOfficial().setName("Sam Official").setInstitutionalEmail("sam@sage.org"));
		dto.setEDucSignatureEnvelopeId("envelope-abc");
		dto.setSchemaData(new JSONObject(Map.of("institution", Map.of("name", "Sage", "tags", Arrays.asList("a", 2)), "count", 42)));
		dto.setSchemaDataAttachments(Arrays.asList("9005", "9006"));
	}

	private static void assertSameSchemaData(RequestInterface expected, RequestInterface actual) {
		assertTrue(new JSONObject(expected.getSchemaData().toString()).similar(actual.getSchemaData()),
				() -> "Unexpected schemaData: " + actual.getSchemaData());
	}

	@Test
	public void testCopyDtoToDboRoundTripWithAllRequestFields() {
		Request dto = new Request();
		populateAllFields(dto);

		DBORequest dbo = new DBORequest();
		// call under test
		RequestUtils.copyDtoToDbo(dto, dbo);

		assertNull(dbo.getRequestSerialized());
		assertEquals(Request.class.getName(), dbo.getConcreteType());
		assertEquals(9001L, dbo.getDucFileHandleId());
		assertEquals(9002L, dbo.getIrbFileHandleId());
		assertEquals("[\"9003\",\"9004\"]", dbo.getAttachmentsJson());
		assertEquals("[\"9005\",\"9006\"]", dbo.getSchemaDataAttachmentsJson());
		assertNull(dbo.getPublication());
		assertNull(dbo.getSummaryOfUse());

		// call under test
		RequestInterface result = RequestUtils.copyDboToDto(dbo);

		assertSameSchemaData(dto, result);
		result.setSchemaData(dto.getSchemaData());
		assertEquals(dto, result);
	}

	@Test
	public void testCopyDtoToDboRoundTripWithAllRenewalFields() {
		Renewal dto = new Renewal();
		populateAllFields(dto);
		dto.setPublication("publication-link");
		dto.setSummaryOfUse("summary of use");

		DBORequest dbo = new DBORequest();
		// call under test
		RequestUtils.copyDtoToDbo(dto, dbo);

		assertEquals(Renewal.class.getName(), dbo.getConcreteType());
		assertEquals("publication-link", dbo.getPublication());
		assertEquals("summary of use", dbo.getSummaryOfUse());

		// call under test
		RequestInterface result = RequestUtils.copyDboToDto(dbo);

		assertSameSchemaData(dto, result);
		result.setSchemaData(dto.getSchemaData());
		assertEquals(dto, result);
	}

	@Test
	public void testCopyDtoToDboRoundTripWithOptionalFieldsNull() {
		Request dto = RequestTestUtils.createNewRequest();
		dto.setAccessorChanges(null);
		dto.setAttachments(null);
		dto.setDucFileHandleId(null);
		dto.setIrbFileHandleId(null);

		DBORequest dbo = new DBORequest();
		// call under test
		RequestUtils.copyDtoToDbo(dto, dbo);

		assertNull(dbo.getAttachmentsJson());
		assertNull(dbo.getAccessorChangesJson());
		assertNull(dbo.getPrincipalInvestigatorJson());
		assertNull(dbo.getSigningOfficialJson());
		assertNull(dbo.getSchemaDataJson());
		assertNull(dbo.getDucFileHandleId());
		assertNull(dbo.getIrbFileHandleId());

		// call under test
		RequestInterface result = RequestUtils.copyDboToDto(dbo);

		assertEquals(dto, result);
	}

	@Test
	public void testCopyDboToDtoWithUnsupportedConcreteType() {
		DBORequest dbo = new DBORequest();
		dbo.setConcreteType("org.sagebionetworks.Unknown");

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			RequestUtils.copyDboToDto(dbo);
		}).getMessage();

		assertEquals("Unsupported request concrete type: org.sagebionetworks.Unknown", message);
	}

	@Test
	public void testExtractAllFileHandleIdsWithNewRequest() {
		RequestInterface request = RequestTestUtils.createNewRequest();
		
		Set<String> expected = ImmutableSet.of("9", "10", "11", "12");
		
		Set<String> result = RequestUtils.extractAllFileHandleIds(request);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testExtractAllFileHandleIdsWithRenewal() {
		RequestInterface request = RequestTestUtils.createNewRenewal();
		
		Set<String> expected = ImmutableSet.of("9", "10", "11", "12");
		
		Set<String> result = RequestUtils.extractAllFileHandleIds(request);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testExtractAllFileHandleIdsWithNoAttachements() {
		RequestInterface request = RequestTestUtils.createNewRequest();
		
		request.setAttachments(null);
		
		Set<String> expected = ImmutableSet.of("9", "10");
		
		Set<String> result = RequestUtils.extractAllFileHandleIds(request);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testExtractAllFileHandleIdsWithIrb() {
		RequestInterface request = RequestTestUtils.createNewRequest();
		
		request.setIrbFileHandleId(null);
		
		Set<String> expected = ImmutableSet.of("9", "11", "12");
		
		Set<String> result = RequestUtils.extractAllFileHandleIds(request);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testExtractAllFileHandleIdsWithDuc() {
		RequestInterface request = RequestTestUtils.createNewRequest();
		
		request.setDucFileHandleId(null);
		
		Set<String> expected = ImmutableSet.of("10", "11", "12");
		
		Set<String> result = RequestUtils.extractAllFileHandleIds(request);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testExtractAllFileHandleIdsWithNoFileHandles() {
		RequestInterface request = RequestTestUtils.createNewRequest();
		
		request.setIrbFileHandleId(null);
		request.setDucFileHandleId(null);
		request.setAttachments(null);
		
		Set<String> expected = Collections.emptySet();
		
		Set<String> result = RequestUtils.extractAllFileHandleIds(request);
		
		assertEquals(expected, result);
	}

	@Test
	public void testExtractAllFileHandleIdsWithSchemaDataAttachments() {
		Request request = new Request();
		request.setAttachments(Arrays.asList("1"));
		request.setSchemaDataAttachments(Arrays.asList("2", "3"));
		request.setDucFileHandleId("4");

		// call under test
		assertEquals(new java.util.HashSet<>(Arrays.asList("1", "2", "3", "4")), RequestUtils.extractAllFileHandleIds(request));
	}
}
