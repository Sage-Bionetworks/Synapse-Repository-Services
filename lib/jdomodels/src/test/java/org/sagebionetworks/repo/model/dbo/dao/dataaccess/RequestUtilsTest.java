package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;

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
		Map<String, Object> schemaData = Map.of("institution", Map.of("name", "Sage"), "notASchemaProperty", 42);
		dto.setSchemaData(schemaData);

		DBORequest dbo = new DBORequest();
		RequestUtils.copyDtoToDbo(dto, dbo);
		RequestInterface newDto = RequestUtils.copyDboToDto(dbo);

		// The blob stores canonical JSON text, so it comes back in the wire friendly shape an
		// opaque-Object field expects rather than as the caller's Map.
		assertTrue(new JSONObject(schemaData).similar(newDto.getSchemaData()),
				() -> "Unexpected schemaData: " + newDto.getSchemaData());
		// Serializing must not replace the caller's own value with the JSON text.
		assertEquals(schemaData, dto.getSchemaData());
	}

	@Test
	public void testCopyDtoToDboRoundTripWithScalarSchemaData() {
		Request dto = RequestTestUtils.createNewRequest();
		dto.setSchemaData(Boolean.TRUE);

		DBORequest dbo = new DBORequest();
		RequestUtils.copyDtoToDbo(dto, dbo);
		RequestInterface newDto = RequestUtils.copyDboToDto(dbo);

		assertEquals(Boolean.TRUE, newDto.getSchemaData());
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
