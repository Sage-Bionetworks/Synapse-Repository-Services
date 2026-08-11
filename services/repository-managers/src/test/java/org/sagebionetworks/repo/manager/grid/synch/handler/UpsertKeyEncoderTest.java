package org.sagebionetworks.repo.manager.grid.synch.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

public class UpsertKeyEncoderTest {

	@Test
	public void testEncodeWithEqualValuesProducesEqualKeys() {
		// call under test
		String key1 = UpsertKeyEncoder.encode(List.of(new ConValue(ConType.LONG, 1L)));
		String key2 = UpsertKeyEncoder.encode(List.of(new ConValue(ConType.LONG, 1L)));
		assertEquals(key1, key2);
	}

	@Test
	public void testEncodeWithDifferentValuesProducesDifferentKeys() {
		// call under test
		assertNotEquals(UpsertKeyEncoder.encode(List.of(new ConValue(ConType.LONG, 1L))),
				UpsertKeyEncoder.encode(List.of(new ConValue(ConType.LONG, 2L))));
	}

	@Test
	public void testEncodeDistinguishesTypes() {
		// A CSV "1" parsed as a STRING must not collide with the grid's numeric 1.
		// call under test
		assertNotEquals(UpsertKeyEncoder.encode(List.of(new ConValue(ConType.STRING, "1"))),
				UpsertKeyEncoder.encode(List.of(new ConValue(ConType.LONG, 1L))));
	}

	@Test
	public void testEncodeIsOrderSensitive() {
		List<ConValue> forward = List.of(new ConValue(ConType.STRING, "a"), new ConValue(ConType.STRING, "b"));
		List<ConValue> reversed = List.of(new ConValue(ConType.STRING, "b"), new ConValue(ConType.STRING, "a"));
		// call under test
		assertNotEquals(UpsertKeyEncoder.encode(forward), UpsertKeyEncoder.encode(reversed));
	}

	@Test
	public void testEncodeIsCollisionSafeAcrossColumnBoundaries() {
		// Two columns ("a","b") must not collide with a single column whose value
		// happens to contain a delimiter-like character ("a-b" or "ab").
		String twoColumns = UpsertKeyEncoder
				.encode(List.of(new ConValue(ConType.STRING, "a"), new ConValue(ConType.STRING, "b")));
		// call under test
		assertNotEquals(twoColumns, UpsertKeyEncoder.encode(List.of(new ConValue(ConType.STRING, "a-b"))));
		assertNotEquals(twoColumns, UpsertKeyEncoder.encode(List.of(new ConValue(ConType.STRING, "ab"))));
	}

	@Test
	public void testEncodeParityBetweenCsvAndGridValues() {
		// The CSV source side and the grid copy side both funnel values through
		// ConValue; equal ConValues must produce identical keys regardless of origin.
		ConValue fromGrid = new ConValue(ConType.LONG, 1L);
		// A CSV integer "1" parsed by the LONG translator yields ConValue(LONG, 1L)
		ConValue fromCsv = ConValue.fromString("1");
		// call under test
		assertEquals(UpsertKeyEncoder.encode(List.of(fromGrid)), UpsertKeyEncoder.encode(List.of(fromCsv)));
	}

	@Test
	public void testEncodeFromDataSelectsKeyColumnsInOrder() {
		Map<String, ConValue> rowData = Map.of(
				"a", new ConValue(ConType.LONG, 1L),
				"b", new ConValue(ConType.STRING, "x"),
				"c", new ConValue(ConType.BOOLEAN, true));
		// call under test — key is [b, a], so the encoding must reflect that order/subset
		String key = UpsertKeyEncoder.encodeFromData(rowData, List.of("b", "a"));
		assertEquals(
				UpsertKeyEncoder.encode(List.of(new ConValue(ConType.STRING, "x"), new ConValue(ConType.LONG, 1L))),
				key);
	}

	@Test
	public void testEncodeFromDataWithMissingKeyColumnUsesUndefined() {
		Map<String, ConValue> rowData = Map.of("a", new ConValue(ConType.LONG, 1L));
		// call under test — a missing key column encodes as UNDEFINED so both sides agree
		String key = UpsertKeyEncoder.encodeFromData(rowData, List.of("a", "missing"));
		assertEquals(UpsertKeyEncoder.encode(
				List.of(new ConValue(ConType.LONG, 1L), new ConValue(ConType.UNDEFINED, null))), key);
	}
}

