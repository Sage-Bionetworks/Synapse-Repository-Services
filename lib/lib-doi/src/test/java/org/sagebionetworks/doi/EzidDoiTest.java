package org.sagebionetworks.doi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.doi.DoiObjectType;

public class EzidDoiTest {

	private EzidDoi ezidDoi;
	private final String doi = "doi:10.9999/test.1234567";
	private final String objectId = "syn123";
	private final DoiObjectType objectType = DoiObjectType.ENTITY;
	private final Long objectVersion = 3L;
	private final EzidMetadata metadata = new EzidMetadata();

	@Before
	public void before() {
		ezidDoi = new EzidDoi();
		ezidDoi.setDoi(doi);
		ezidDoi.setObjectId(objectId);
		ezidDoi.setDoiObjectType(objectType);
		ezidDoi.setObjectVersion(objectVersion);
		ezidDoi.setMetadata(metadata);
	}

	@Test
	public void testGetSet() {
		assertEquals(doi, ezidDoi.getDoi());
		assertEquals(objectId, ezidDoi.getObjectId());
		assertEquals(objectType, ezidDoi.getDoiObjectType());
		assertEquals(objectVersion, ezidDoi.getObjectVersion());
		assertEquals(metadata, ezidDoi.getMetadata());
		ezidDoi.setDoi("doi");
		assertEquals("doi", ezidDoi.getDoi());
		ezidDoi.setObjectId("objectId");
		assertEquals("objectId", ezidDoi.getObjectId());
		ezidDoi.setDoiObjectType(DoiObjectType.EVALUATION);
		assertEquals(DoiObjectType.EVALUATION, ezidDoi.getDoiObjectType());
		ezidDoi.setObjectVersion(null);
		assertNull(ezidDoi.getObjectVersion());
		EzidMetadata metadata = new EzidMetadata();
		ezidDoi.setMetadata(metadata);
		assertEquals(metadata, ezidDoi.getMetadata());
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
	public void testRequriedSetDoiObjectType() {
		EzidDoi doi = new EzidDoi();
		doi.setDoiObjectType(null);
	}

	@Test(expected=NullPointerException.class)
	public void testRequriedGetDoiObjectType() {
		EzidDoi doi = new EzidDoi();
		doi.getDoiObjectType();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testRequriedSetObjectId() {
		EzidDoi doi = new EzidDoi();
		doi.setObjectId(null);
	}

	@Test(expected=NullPointerException.class)
	public void testRequriedGetObjectId() {
		EzidDoi doi = new EzidDoi();
		doi.getObjectId();
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
