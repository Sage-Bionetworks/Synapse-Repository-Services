package org.sagebionetworks.repo.manager.grid.internal.replica.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class SynapseRowTest {

	@Test
	public void testJSONRoundTrip() {
		SynapseRow sr = new SynapseRow().setRowId(1L).setVersionNumber(2L).setEtag("etag");
		String json = sr.toJSON();
		SynapseRow back = new SynapseRow().setFromJSON(json);
		assertEquals(sr, back);
	}

	@Test
	public void testJSONRoundTripWithNullId() {
		SynapseRow sr = new SynapseRow().setRowId(null).setVersionNumber(2L).setEtag("etag");
		String json = sr.toJSON();
		SynapseRow back = new SynapseRow().setFromJSON(json);
		assertEquals(sr, back);
	}

	@Test
	public void testJSONRoundTripWithNullVersion() {
		SynapseRow sr = new SynapseRow().setRowId(1L).setVersionNumber(null).setEtag("etag");
		String json = sr.toJSON();
		SynapseRow back = new SynapseRow().setFromJSON(json);
		assertEquals(sr, back);
	}

	@Test
	public void testJSONRoundTripWithNullEtag() {
		SynapseRow sr = new SynapseRow().setRowId(1L).setVersionNumber(2L).setEtag(null);
		String json = sr.toJSON();
		SynapseRow back = new SynapseRow().setFromJSON(json);
		assertEquals(sr, back);
	}
	
	@Test
	public void testJSONRoundTripWithAllNull() {
		SynapseRow sr = new SynapseRow();
		String json = sr.toJSON();
		SynapseRow back = new SynapseRow().setFromJSON(json);
		assertEquals(sr, back);
	}

}
