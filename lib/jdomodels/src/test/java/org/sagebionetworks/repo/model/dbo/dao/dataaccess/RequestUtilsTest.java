package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
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
