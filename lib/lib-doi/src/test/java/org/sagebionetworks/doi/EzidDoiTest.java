package org.sagebionetworks.doi;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.doi.Doi;

public class EzidDoiTest {

	private EzidDoi ezidDoi;
	private final Doi doiDto = new Doi();
	private final String doi = "doi:10.9999/test.1234567";
	private final EzidMetadata metadata = new EzidMetadata();

	@Before
	public void before() {
		ezidDoi = new EzidDoi();
		ezidDoi.setDto(doiDto);
		ezidDoi.setDoi(doi);
		ezidDoi.setMetadata(metadata);
	}

	@Test
	public void testGetSet() {
		assertEquals(doiDto, ezidDoi.getDto());
		Doi doiDto = new Doi();
		ezidDoi.setDto(doiDto);
		assertEquals(doiDto, ezidDoi.getDto());
		assertEquals(doi, ezidDoi.getDoi());
		assertEquals(metadata, ezidDoi.getMetadata());
		ezidDoi.setDoi("doi");
		assertEquals("doi", ezidDoi.getDoi());
		EzidMetadata metadata = new EzidMetadata();
		ezidDoi.setMetadata(metadata);
		assertEquals(metadata, ezidDoi.getMetadata());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testRequriedSetDto() {
		EzidDoi doi = new EzidDoi();
		doi.setDto(null);
	}

	@Test(expected=NullPointerException.class)
	public void testRequriedGetDto() {
		EzidDoi doi = new EzidDoi();
		doi.getDto();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testRequriedSetDoi() {
		EzidDoi doi = new EzidDoi();
		doi.setDoi(null);
	}

	@Test(expected=NullPointerException.class)
	public void testRequriedGetDoi() {
		EzidDoi doi = new EzidDoi();
		doi.getDoi();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testRequriedSetMetadata() {
		EzidDoi doi = new EzidDoi();
		doi.setMetadata(null);
	}

	@Test(expected=NullPointerException.class)
	public void testRequriedGetMetadata() {
		EzidDoi doi = new EzidDoi();
		doi.getMetadata();
	}
}
