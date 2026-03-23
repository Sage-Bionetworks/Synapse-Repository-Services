package org.sagebionetworks.repo.manager.grid.internal.replica.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

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

	@Test
	public void testToAndFromConValue() {
		SynapseRow sr = new SynapseRow().setRowId(1L).setVersionNumber(2L).setEtag("etag");
		ConValue cv = sr.toConValue();
		SynapseRow back = new SynapseRow().setFromConValue(cv);
		assertEquals(sr, back);
	}

	@Test
	public void testFromConValueWrongType() {
		ConValue cv = new ConValue(ConType.BOOLEAN, true);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			new SynapseRow().setFromConValue(cv);
		}).getMessage();
		assertEquals("Expected a ContType.JSON_ARRAY", message);
	}

}
