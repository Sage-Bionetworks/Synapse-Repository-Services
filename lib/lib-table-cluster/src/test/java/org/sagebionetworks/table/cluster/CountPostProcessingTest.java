package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.cluster.CountPostProcessing.CountCell;

public class CountPostProcessingTest {

	private static final String SALT = "6f1c4a9e-2b7d-4e3a-9c51-0d8e7f6a5b41";
	private static final CountCell CELL = new CountCell("COUNT(_C1_)", "CONCAT('1', JSON_ARRAY(_C0_))", 5L);

	@Test
	public void testToSqlWithNone() {
		// call under test
		assertEquals("COUNT(_C1_)", new CountPostProcessing.None().toSql(CELL));
		assertEquals(Map.of(), new CountPostProcessing.None().getBindParameters());
	}

	@Test
	public void testToSqlWithRounding() {
		// call under test
		assertEquals("aggregateCountRound(COUNT(_C1_), 5)", new CountPostProcessing.Rounding(5L).toSql(CELL));
		assertEquals(Map.of(), new CountPostProcessing.Rounding(5L).getBindParameters());
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
		assertEquals("aggregateCountRoundNoise(COUNT(_C1_), 10, 0.5, 5, :aggregateDataSalt, CONCAT('1', JSON_ARRAY(_C0_)))",
				new CountPostProcessing.RoundedNoise(10L, 0.5, SALT).toSql(CELL));
	}

	@Test
	public void testToSqlWithRoundedNoiseAndSmallScale() {
		// Double.toString would write 1.0E-4, which is not the plain decimal the SQL is written with.
		// call under test
		assertEquals("aggregateCountRoundNoise(COUNT(_C1_), 10, 0.0001, 5, :aggregateDataSalt, CONCAT('1', JSON_ARRAY(_C0_)))",
				new CountPostProcessing.RoundedNoise(10L, 1.0 / 10_000, SALT).toSql(CELL));
	}

	@Test
	public void testToSqlWithRoundedNoiseAndLargeScale() {
		// call under test
		assertEquals("aggregateCountRoundNoise(COUNT(_C1_), 10, 100000000, 5, :aggregateDataSalt, CONCAT('1', JSON_ARRAY(_C0_)))",
				new CountPostProcessing.RoundedNoise(10L, 1.0E8, SALT).toSql(CELL));
	}

	@Test
	public void testRoundedNoiseBindParameters() {
		// call under test
		assertEquals(Map.of("aggregateDataSalt", SALT), new CountPostProcessing.RoundedNoise(10L, 0.5, SALT).getBindParameters());
	}

	@Test
	public void testRoundedNoiseToStringOmitsSalt() {
		// call under test
		String result = new CountPostProcessing.RoundedNoise(10L, 0.5, SALT).toString();

		assertEquals("RoundedNoise[binSize=10, scale=0.5]", result);
		assertFalse(result.contains(SALT));
	}

	@Test
	public void testRoundedNoiseWithZeroBinSize() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CountPostProcessing.RoundedNoise(0L, 0.5, SALT);
		}).getMessage();
		assertEquals("binSize must be a positive value", message);
	}

	@Test
	public void testRoundedNoiseWithZeroScale() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CountPostProcessing.RoundedNoise(10L, 0.0, SALT);
		}).getMessage();
		assertEquals("scale must be a positive finite value", message);
	}

	@Test
	public void testRoundedNoiseWithInfiniteScale() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CountPostProcessing.RoundedNoise(10L, Double.POSITIVE_INFINITY, SALT);
		}).getMessage();
		assertEquals("scale must be a positive finite value", message);
	}

	@Test
	public void testRoundedNoiseWithNaNScale() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CountPostProcessing.RoundedNoise(10L, Double.NaN, SALT);
		}).getMessage();
		assertEquals("scale must be a positive finite value", message);
	}

	@Test
	public void testRoundedNoiseWithNullSalt() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CountPostProcessing.RoundedNoise(10L, 0.5, null);
		}).getMessage();
		assertEquals("salt is required.", message);
	}
}
