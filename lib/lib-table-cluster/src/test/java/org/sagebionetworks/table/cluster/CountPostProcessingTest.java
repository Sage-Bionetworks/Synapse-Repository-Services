package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class CountPostProcessingTest {

	@Test
	public void testToSqlWithNone() {
		// call under test
		assertEquals("COUNT(_C1_)", new CountPostProcessing.None().toSql("COUNT(_C1_)"));
	}

	@Test
	public void testToSqlWithRounding() {
		// call under test
		assertEquals("aggregateCountRound(COUNT(_C1_), 5)", new CountPostProcessing.Rounding(5L).toSql("COUNT(_C1_)"));
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
	public void testToSqlWithBinnedNoise() {
		// call under test
		assertEquals("aggregateCountBinnedNoise(COUNT(DISTINCT _C1_), 10, 0.5, 5)",
				new CountPostProcessing.BinnedNoise(10L, 0.5, 5L).toSql("COUNT(DISTINCT _C1_)"));
	}

	@Test
	public void testToSqlWithBinnedNoiseAndSmallScale() {
		// Double.toString would write 1.0E-4, which is not the plain decimal the SQL is written with.
		// call under test
		assertEquals("aggregateCountBinnedNoise(COUNT(_C1_), 10, 0.0001, 5)",
				new CountPostProcessing.BinnedNoise(10L, 1.0 / 10_000, 5L).toSql("COUNT(_C1_)"));
	}

	@Test
	public void testToSqlWithBinnedNoiseAndLargeScale() {
		// call under test
		assertEquals("aggregateCountBinnedNoise(COUNT(_C1_), 10, 100000000, 5)",
				new CountPostProcessing.BinnedNoise(10L, 1.0E8, 5L).toSql("COUNT(_C1_)"));
	}

	@Test
	public void testBinnedNoiseWithZeroBinSize() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CountPostProcessing.BinnedNoise(0L, 0.5, 5L);
		}).getMessage();
		assertEquals("binSize must be a positive value", message);
	}

	@Test
	public void testBinnedNoiseWithZeroScale() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CountPostProcessing.BinnedNoise(10L, 0.0, 5L);
		}).getMessage();
		assertEquals("scale must be a positive finite value", message);
	}

	@Test
	public void testBinnedNoiseWithInfiniteScale() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CountPostProcessing.BinnedNoise(10L, Double.POSITIVE_INFINITY, 5L);
		}).getMessage();
		assertEquals("scale must be a positive finite value", message);
	}

	@Test
	public void testBinnedNoiseWithNaNScale() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CountPostProcessing.BinnedNoise(10L, Double.NaN, 5L);
		}).getMessage();
		assertEquals("scale must be a positive finite value", message);
	}
}
