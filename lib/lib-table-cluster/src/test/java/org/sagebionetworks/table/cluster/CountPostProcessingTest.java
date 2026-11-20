package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.cluster.CountPostProcessing.CountCell;

public class CountPostProcessingTest {

	private static final CountCell CELL = new CountCell("COUNT(_C1_)", "CONCAT('1', JSON_ARRAY(_C0_))", 5L);

	@Test
	public void testToSqlWithNone() {
		// call under test
		assertEquals("COUNT(_C1_)", new CountPostProcessing.None().toSql(CELL));
	}

	@Test
	public void testToSqlWithRounding() {
		// call under test
		assertEquals("aggregateCountRound(COUNT(_C1_), 5)", new CountPostProcessing.Rounding(5L).toSql(CELL));
	}

	@Test
	public void testRoundingWithZeroRoundTo() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CountPostProcessing.Rounding(0L);
		}).getMessage();
		assertEquals("roundTo must be a positive value", message);
	}

	@Test
	public void testRoundingWithNegativeRoundTo() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CountPostProcessing.Rounding(-5L);
		}).getMessage();
		assertEquals("roundTo must be a positive value", message);
	}

	@Test
	public void testToSqlWithRoundedNoise() {
		// call under test
		assertEquals("aggregateCountRoundedNoise(COUNT(_C1_), 10, 0.5, 5, CONCAT('1', JSON_ARRAY(_C0_)))",
				new CountPostProcessing.RoundedNoise(10L, 0.5).toSql(CELL));
	}

	@Test
	public void testToSqlWithRoundedNoiseAndSmallScale() {
		// Double.toString would write 1.0E-4, which is not the plain decimal the SQL is written with.
		// call under test
		assertEquals("aggregateCountRoundedNoise(COUNT(_C1_), 10, 0.0001, 5, CONCAT('1', JSON_ARRAY(_C0_)))",
				new CountPostProcessing.RoundedNoise(10L, 1.0 / 10_000).toSql(CELL));
	}

	@Test
	public void testToSqlWithRoundedNoiseAndLargeScale() {
		// call under test
		assertEquals("aggregateCountRoundedNoise(COUNT(_C1_), 10, 100000000, 5, CONCAT('1', JSON_ARRAY(_C0_)))",
				new CountPostProcessing.RoundedNoise(10L, 1.0E8).toSql(CELL));
	}

	@Test
	public void testRoundedNoiseWithZeroBinSize() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CountPostProcessing.RoundedNoise(0L, 0.5);
		}).getMessage();
		assertEquals("binSize must be a positive value", message);
	}

	@Test
	public void testRoundedNoiseWithZeroScale() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CountPostProcessing.RoundedNoise(10L, 0.0);
		}).getMessage();
		assertEquals("scale must be a positive finite value", message);
	}

	@Test
	public void testRoundedNoiseWithInfiniteScale() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CountPostProcessing.RoundedNoise(10L, Double.POSITIVE_INFINITY);
		}).getMessage();
		assertEquals("scale must be a positive finite value", message);
	}

	@Test
	public void testRoundedNoiseWithNaNScale() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CountPostProcessing.RoundedNoise(10L, Double.NaN);
		}).getMessage();
		assertEquals("scale must be a positive finite value", message);
	}
}
