package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;

import org.junit.Test;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRenewal;

public class DataAccessRequestUtilsTest {

	@Test
	public void testConvertListToStringNullList() {
		assertEquals("", DataAccessRequestUtils.convertListToString(null));
	}

	@Test
	public void testConvertListToStringEmptyList() {
		assertEquals("", DataAccessRequestUtils.convertListToString(new LinkedList<String>()));
	}

	@Test
	public void testConvertListToStringOneElement() {
		assertEquals("1", DataAccessRequestUtils.convertListToString(Arrays.asList("1")));
	}

	@Test
	public void testConvertListToStringMultipleElements() {
		assertEquals("1,2,3", DataAccessRequestUtils.convertListToString(Arrays.asList("1", "2", "3")));
	}

	@Test
	public void testConvertStringToListNullString() {
		assertTrue(DataAccessRequestUtils.convertStringToList(null).isEmpty());
	}

	@Test
	public void testConvertStringToListEmptyString() {
		assertTrue(DataAccessRequestUtils.convertStringToList("").isEmpty());
	}

	@Test
	public void testConvertStringToListOneElement() {
		assertEquals(Arrays.asList("1"), DataAccessRequestUtils.convertStringToList("1"));
	}

	@Test
	public void testConvertStringToListMultipleElements() {
		assertEquals(Arrays.asList("1", "2", "3"), DataAccessRequestUtils.convertStringToList("1,2,3"));
	}

	@Test
	public void testCopyDtoToDboRoundTrip() {
		DataAccessRenewal dto = new DataAccessRenewal();
		dto.setId("1");
		dto.setAccessRequirementId("2");
		dto.setResearchProjectId("3");
		dto.setCreatedBy("4");
		dto.setCreatedOn(new Date());
		dto.setModifiedBy("5");
		dto.setModifiedOn(new Date());
		dto.setEtag("etag");
		dto.setAccessors(Arrays.asList("6", "7", "8"));
		dto.setDucFileHandleId("9");
		dto.setIrbFileHandleId("10");
		dto.setAttachments(Arrays.asList("11", "12"));
		dto.setPublication("publication");
		dto.setSummaryOfUse("summaryOfUse");

		DBODataAccessRequest dbo = new DBODataAccessRequest();
		DataAccessRequestUtils.copyDtoToDbo(dto, dbo);
		DataAccessRenewal newDto = (DataAccessRenewal) DataAccessRequestUtils.copyDboToDto(dbo);
		assertEquals(dto, newDto);
	}
}
