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
	public void testToSqlWithLaplaceNoise() {
		// call under test
		assertEquals("aggregateCountNoise(COUNT(DISTINCT _C1_), 0.5)",
				new CountPostProcessing.LaplaceNoise(0.5).toSql("COUNT(DISTINCT _C1_)"));
	}

	@Test
	public void testToSqlWithLaplaceNoiseAndSmallScale() {
		// Double.toString would write 1.0E-4, which is not the plain decimal the SQL is written with.
		// call under test
		assertEquals("aggregateCountNoise(COUNT(_C1_), 0.0001)",
				new CountPostProcessing.LaplaceNoise(1.0 / 10_000).toSql("COUNT(_C1_)"));
	}

	@Test
	public void testToSqlWithLaplaceNoiseAndLargeScale() {
		// call under test
		assertEquals("aggregateCountNoise(COUNT(_C1_), 100000000)",
				new CountPostProcessing.LaplaceNoise(1.0E8).toSql("COUNT(_C1_)"));
	}

	@Test
	public void testLaplaceNoiseWithZeroScale() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CountPostProcessing.LaplaceNoise(0.0);
		}).getMessage();
		assertEquals("scale must be a positive finite value", message);
	}

	@Test
	public void testLaplaceNoiseWithInfiniteScale() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CountPostProcessing.LaplaceNoise(Double.POSITIVE_INFINITY);
		}).getMessage();
		assertEquals("scale must be a positive finite value", message);
	}

	@Test
	public void testLaplaceNoiseWithNaNScale() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CountPostProcessing.LaplaceNoise(Double.NaN);
		}).getMessage();
		assertEquals("scale must be a positive finite value", message);
	}
}
