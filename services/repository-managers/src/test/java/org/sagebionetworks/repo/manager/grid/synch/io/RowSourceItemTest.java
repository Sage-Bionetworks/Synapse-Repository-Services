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

public class RowSourceItemTest {

	@Test
	public void testRoundTrip() {

		RowSourceItem sr = new RowSourceItem(new TreeMap<>(Map.of("a", new ConValue(ConType.LONG, 99L))), "syn123",
				new SynapseRow().setRowId(123L).setVersionNumber(1L).setEtag("etag1"));

		RowSourceItem back = new RowSourceItem(sr.getBytes(), sr.getKey());

		assertEquals(sr, back);

		assertTrue(Arrays.equals(sr.getHash(), back.getHash()));
		assertEquals(sr.getSynapseRow(), back.getSynapseRow());

	}

	@Test
	public void testRoundTripWithNullSynRow() {

		RowSourceItem sr = new RowSourceItem(new TreeMap<>(Map.of("a", new ConValue(ConType.LONG, 99L))), "syn123", null);
		assertEquals(Optional.empty(), sr.getSynapseRow());

		RowSourceItem back = new RowSourceItem(sr.getBytes(), sr.getKey());

		assertEquals(sr, back);

		assertTrue(Arrays.equals(sr.getHash(), back.getHash()));

	}

}
