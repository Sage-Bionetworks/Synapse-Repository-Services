package org.sagebionetworks.repo.manager.grid.synch.io;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

public class SynchRowTest {

	@Test
	public void testRoundTrip() {

		SynchRow sr = new SynchRow(new TreeMap<>(Map.of("a", new ConValue(ConType.LONG, 99L))), "syn123",
				new SynapseRow().setRowId(123L).setVersionNumber(1L).setEtag("etag1"));

		SynchRow back = new SynchRow(sr.getBytes(), sr.getKey());

		assertEquals(sr, back);

		assertTrue(Arrays.equals(sr.getHash(), back.getHash()));
		assertEquals(sr.getSynRow(), back.getSynRow());

	}

	@Test
	public void testRoundTripWithNullSynRow() {

		SynchRow sr = new SynchRow(new TreeMap<>(Map.of("a", new ConValue(ConType.LONG, 99L))), "syn123", null);
		assertEquals(Optional.empty(), sr.getSynRow());

		SynchRow back = new SynchRow(sr.getBytes(), sr.getKey());

		assertEquals(sr, back);

		assertTrue(Arrays.equals(sr.getHash(), back.getHash()));

	}

}
