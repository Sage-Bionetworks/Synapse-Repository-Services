package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.PrincipalInvestigator;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.SigningOfficial;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

import com.google.common.collect.ImmutableSet;

public class RequestUtilsTest {

	private static final String SCHEMA_DATA = "{\"institution\":{\"name\":\"Sage\",\"tags\":[\"a\",2]},\"count\":42}";

	@Test
	public void testCopyDtoToDboRoundTrip() {
		Renewal dto = RequestTestUtils.createNewRenewal();

		DBORequest dbo = new DBORequest();
		RequestUtils.copyDtoToDbo(dto, dbo);
		Renewal newDto = (Renewal) RequestUtils.copyDboToDto(dbo);
		assertEquals(dto, newDto);
	}

	@Test
	public void testCopyDtoToDboRoundTripWithScalarSchemaData() {
		Request dto = RequestTestUtils.createNewRequest();
		dto.setSchemaData(Boolean.TRUE);

		DBORequest dbo = new DBORequest();
		RequestUtils.copyDtoToDbo(dto, dbo);
		RequestInterface newDto = RequestUtils.copyDboToDto(dbo);

		assertEquals(dto, newDto);
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
		dto.setSchemaData(new JSONObject(SCHEMA_DATA));
	}

	/**
	 * @return The columns of the request created by {@link #populateAllFields(RequestInterface)}, the document is not set
	 */
	private static DBORequest createExpectedColumns() {
		DBORequest expected = new DBORequest();
		expected.setId(101L);
		expected.setAccessRequirementId(202L);
		expected.setResearchProjectId(303L);
		expected.setCreatedBy(404L);
		expected.setCreatedOn(1700000000000L);
		expected.setModifiedBy(505L);
		expected.setModifiedOn(1700000100000L);
		expected.setEtag("etag-1234");
		expected.setEDucEnvelopeId("envelope-abc");
		expected.setDucFileHandleId(9001L);
		expected.setIrbFileHandleId(9002L);
		expected.setAttachmentsJson("[\"9003\",\"9004\"]");
		return expected;
	}

	/**
	 * @return The document of the request created by {@link #populateAllFields(RequestInterface)}: none of the fields stored in
	 *         their own column
	 */
	private static JSONObject createExpectedDocument(Class<? extends RequestInterface> concreteType) {
		return new JSONObject()
				.put("concreteType", concreteType.getName())
				.put("accessRequirementVersionNumber", 3)
				.put("accessorChanges", new JSONArray("[{\"userId\":\"6\",\"type\":\"GAIN_ACCESS\"},{\"userId\":\"7\",\"type\":\"REVOKE_ACCESS\"}]"))
				.put("institution", "Sage Bionetworks")
				.put("principalInvestigator", new JSONObject("{\"userId\":\"606\",\"name\":\"Ada PI\",\"institutionalEmail\":\"ada@sage.org\"}"))
				.put("signingOfficial", new JSONObject("{\"name\":\"Sam Official\",\"institutionalEmail\":\"sam@sage.org\"}"))
				.put("schemaData", new JSONObject(SCHEMA_DATA));
	}

	private static void assertDocument(JSONObject expected, DBORequest dbo) {
		assertTrue(expected.similar(new JSONObject(dbo.getRequestJson())), () -> "Unexpected document: " + dbo.getRequestJson());
	}

	/**
	 * The free-form schemaData comes back as a JSONObjectAdapter, which is never equal to the caller's JSONObject
	 */
	private static void assertSameSchemaData(RequestInterface expected, RequestInterface actual) {
		assertTrue(new JSONObject(expected.getSchemaData().toString()).similar(new JSONObject(actual.getSchemaData().toString())),
				() -> "Unexpected schemaData: " + actual.getSchemaData());
		actual.setSchemaData(expected.getSchemaData());
	}

	@Test
	public void testCopyDtoToDboWithAllRequestFields() {
		Request dto = new Request();
		populateAllFields(dto);
		DBORequest expected = createExpectedColumns();

		DBORequest dbo = new DBORequest();
		// call under test
		RequestUtils.copyDtoToDbo(dto, dbo);

		assertDocument(createExpectedDocument(Request.class), dbo);
		// The order of the keys in the document is not relevant, it was verified above
		expected.setRequestJson(dbo.getRequestJson());
		assertEquals(expected, dbo);
	}

	@Test
	public void testCopyDtoToDboWithAllRenewalFields() {
		Renewal dto = new Renewal();
		populateAllFields(dto);
		dto.setPublication("publication-link");
		dto.setSummaryOfUse("summary of use");
		DBORequest expected = createExpectedColumns();

		DBORequest dbo = new DBORequest();
		// call under test
		RequestUtils.copyDtoToDbo(dto, dbo);

		assertDocument(createExpectedDocument(Renewal.class).put("publication", "publication-link").put("summaryOfUse", "summary of use"), dbo);
		expected.setRequestJson(dbo.getRequestJson());
		assertEquals(expected, dbo);
	}

	@Test
	public void testCreateRequestJsonWithColumnFields() {
		Request dto = new Request();
		populateAllFields(dto);
		RequestInterface original = JDOSecondaryPropertyUtils.createObjectFromJSON(Request.class, JDOSecondaryPropertyUtils.createJSONFromObject(dto));

		// call under test
		String json = RequestUtils.createRequestJson(dto);

		Set<String> keys = new JSONObject(json).keySet();
		for (String columnField : Arrays.asList("id", "accessRequirementId", "researchProjectId", "createdBy", "createdOn", "modifiedBy",
				"modifiedOn", "etag", "eDucSignatureEnvelopeId", "ducFileHandleId", "irbFileHandleId", "attachments")) {
			assertTrue(!keys.contains(columnField), () -> "The document contains the column field " + columnField + ": " + json);
		}
		// The fields are cleared on a copy, the caller's request is unchanged
		assertSameSchemaData(dto, original);
		assertEquals(original, dto);
	}

	@Test
	public void testCopyDboToDtoWithAllRequestFields() {
		Request dto = new Request();
		populateAllFields(dto);
		DBORequest dbo = new DBORequest();
		RequestUtils.copyDtoToDbo(dto, dbo);

		// call under test
		RequestInterface result = RequestUtils.copyDboToDto(dbo);

		assertSameSchemaData(dto, result);
		assertEquals(dto, result);
	}

	@Test
	public void testCopyDboToDtoWithAllRenewalFields() {
		Renewal dto = new Renewal();
		populateAllFields(dto);
		dto.setPublication("publication-link");
		dto.setSummaryOfUse("summary of use");
		DBORequest dbo = new DBORequest();
		RequestUtils.copyDtoToDbo(dto, dbo);

		// call under test
		RequestInterface result = RequestUtils.copyDboToDto(dbo);

		assertSameSchemaData(dto, result);
		assertEquals(dto, result);
	}

	@Test
	public void testCopyDtoToDboRoundTripWithOptionalFieldsNull() {
		Request dto = RequestTestUtils.createNewRequest();
		dto.setAccessorChanges(null);
		dto.setAttachments(null);
		dto.setDucFileHandleId(null);
		dto.setIrbFileHandleId(null);
		DBORequest expected = new DBORequest();
		expected.setId(1L);
		expected.setAccessRequirementId(2L);
		expected.setResearchProjectId(3L);
		expected.setCreatedBy(4L);
		expected.setCreatedOn(dto.getCreatedOn().getTime());
		expected.setModifiedBy(5L);
		expected.setModifiedOn(dto.getModifiedOn().getTime());
		expected.setEtag("etag");
		expected.setRequestJson("{\"concreteType\":\"" + Request.class.getName() + "\"}");

		DBORequest dbo = new DBORequest();
		// call under test
		RequestUtils.copyDtoToDbo(dto, dbo);

		assertEquals(expected, dbo);

		// call under test
		RequestInterface result = RequestUtils.copyDboToDto(dbo);

		assertEquals(dto, result);
	}

	@Test
	public void testCopyDboToDtoWithUnsupportedConcreteType() {
		DBORequest dbo = new DBORequest();
		dbo.setRequestJson("{\"concreteType\":\"org.sagebionetworks.Unknown\"}");

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			RequestUtils.copyDboToDto(dbo);
		}).getMessage();

		assertEquals("Unknown concreteType : 'org.sagebionetworks.Unknown'", message);
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
}
