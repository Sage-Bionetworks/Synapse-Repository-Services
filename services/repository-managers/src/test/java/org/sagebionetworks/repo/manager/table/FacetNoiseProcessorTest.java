package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.FacetNoiseParameters;
import org.sagebionetworks.repo.model.FacetPostProcessingAlgorithm;
import org.sagebionetworks.repo.model.FacetRoundingParameters;
import org.sagebionetworks.repo.model.table.FacetColumnResult;
import org.sagebionetworks.repo.model.table.FacetColumnResultRange;
import org.sagebionetworks.repo.model.table.FacetColumnResultValueCount;
import org.sagebionetworks.repo.model.table.FacetColumnResultValues;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.table.cluster.CountPostProcessing;

@ExtendWith(MockitoExtension.class)
public class FacetNoiseProcessorTest {

	@Mock
	private RandomGenerator mockRandom;

	private FacetNoiseParameters parameters;
	private FacetPostProcessingContext context;

	@BeforeEach
	public void before() {
		// A bin of 10 with scale 1/epsilon = 4 and a threshold of 5.
		parameters = new FacetNoiseParameters().setBinSize(10L).setEpsilon(0.25);
		context = new FacetPostProcessingContext(parameters, 5L);
	}

	@Test
	public void testGetAlgorithm() {
		// call under test
		assertEquals(FacetPostProcessingAlgorithm.NOISE, new FacetNoiseProcessor().getAlgorithm());
	}

	@Test
	public void testReleaseWithNoNoiseIsTheMidpoint() {
		// A draw of 0.5 is zero noise: 210 + 4.5 rounds to 215.
		// call under test
		assertEquals(215L, FacetNoiseProcessor.release(212L, 10L, 4.0, 5L, 0.5));
	}

	@Test
	public void testReleaseWithPositiveNoise() {
		// A draw of 0.75 is -4 * ln(0.5) = +2.77: 214.5 + 2.77 rounds to 217.
		// call under test
		assertEquals(217L, FacetNoiseProcessor.release(212L, 10L, 4.0, 5L, 0.75));
	}

	@Test
	public void testReleaseWithNegativeNoise() {
		// A draw of 0.25 is -2.77: 214.5 - 2.77 rounds to 212.
		// call under test
		assertEquals(212L, FacetNoiseProcessor.release(212L, 10L, 4.0, 5L, 0.25));
	}

	@Test
	public void testReleaseIsClampedToTheTopOfTheBin() {
		// A draw of 0.999 is about +25, far beyond the bin [210, 219].
		// call under test
		assertEquals(219L, FacetNoiseProcessor.release(212L, 10L, 4.0, 5L, 0.999));
	}

	@Test
	public void testReleaseIsClampedToTheThreshold() {
		// A draw of 0.001 is about -25. The bin of 7 is [0, 9], but a released non-zero count is never below k.
		// call under test
		assertEquals(5L, FacetNoiseProcessor.release(7L, 10L, 4.0, 5L, 0.001));
	}

	@Test
	public void testReleaseWithDrawOfZero() {
		// A draw of exactly zero would be an infinite sample, so it releases the midpoint.
		// call under test
		assertEquals(215L, FacetNoiseProcessor.release(212L, 10L, 4.0, 5L, 0.0));
	}

	@Test
	public void testReleaseWithZero() {
		// call under test
		assertEquals(0L, FacetNoiseProcessor.release(0L, 10L, 4.0, 5L, 0.999));
	}

	@Test
	public void testReleaseRevealsOnlyTheBin() {
		// Every released value lies in its count's own bin, at or above the threshold, so the bin can be read
		// from the value but nothing finer. A wide scale pushes many values to the edges to exercise the clamps.
		Random random = new Random(42);
		for (long count = 5; count < 300; count++) {
			long lowerBound = (count / 10) * 10;
			for (int i = 0; i < 20; i++) {
				// call under test
				long released = FacetNoiseProcessor.release(count, 10L, 100.0, 5L, random.nextDouble());
				assertTrue(released >= Math.max(lowerBound, 5L) && released <= lowerBound + 9,
						count + " released as " + released);
			}
		}
	}

	@Test
	public void testReleaseAveragesToTheBinMidpoint() {
		// Averaging many releases of two different counts from the same bin converges on the bin's midpoint, so
		// averaging recovers the bin and nothing finer.
		Random random = new Random(42);
		double low = 0;
		double high = 0;
		int draws = 5000;
		for (int i = 0; i < draws; i++) {
			// call under test
			low += FacetNoiseProcessor.release(1201L, 100L, 20.0, 5L, random.nextDouble());
			high += FacetNoiseProcessor.release(1298L, 100L, 20.0, 5L, random.nextDouble());
		}
		assertEquals(1249.5, low / draws, 2.0);
		assertEquals(1249.5, high / draws, 2.0);
	}

	@Test
	public void testProcessCount() {
		when(mockRandom.nextDouble()).thenReturn(0.75);
		FacetNoiseProcessor processor = new FacetNoiseProcessor(mockRandom);

		// call under test
		assertEquals(217L, processor.processCount(212L, context));
	}

	@Test
	public void testProcess() {
		when(mockRandom.nextDouble()).thenReturn(0.75, 0.25);
		FacetNoiseProcessor processor = new FacetNoiseProcessor(mockRandom);
		FacetColumnResultValues raw = new FacetColumnResultValues().setColumnName("state").setJsonPath("$.state")
				.setFacetType(FacetType.enumeration).setFacetValues(List.of(
						new FacetColumnResultValueCount().setValue("a").setCount(212L).setIsSelected(true),
						new FacetColumnResultValueCount().setValue("b").setCount(0L).setIsSelected(false),
						new FacetColumnResultValueCount().setValue("c").setCount(-1L).setIsSelected(false),
						new FacetColumnResultValueCount().setValue("d").setCount(55L).setIsSelected(false)));
		FacetColumnResultRange range = new FacetColumnResultRange().setColumnName("age").setFacetType(FacetType.range)
				.setColumnMin("1").setColumnMax("9");

		// call under test
		List<FacetColumnResult> results = processor.process(List.of(raw, range), context);

		FacetColumnResultValues expected = new FacetColumnResultValues().setColumnName("state").setJsonPath("$.state")
				.setFacetType(FacetType.enumeration).setFacetValues(List.of(
						new FacetColumnResultValueCount().setValue("a").setCount(217L).setIsSelected(true),
						// zero stays zero and a masked count stays masked, drawing no noise
						new FacetColumnResultValueCount().setValue("b").setCount(0L).setIsSelected(false),
						new FacetColumnResultValueCount().setValue("c").setCount(-1L).setIsSelected(false),
						// 54.5 - 2.77 rounds to 52
						new FacetColumnResultValueCount().setValue("d").setCount(52L).setIsSelected(false)));
		assertEquals(List.of(expected, range), results);
	}

	@Test
	public void testGetCountPostProcessing() {
		// call under test
		CountPostProcessing result = new FacetNoiseProcessor(mockRandom).getCountPostProcessing(context);

		assertEquals(new CountPostProcessing.BinnedNoise(10L, 4.0, 5L), result);
		verifyNoInteractions(mockRandom);
	}

	@Test
	public void testValidateParameters() {
		// call under test
		new FacetNoiseProcessor().validateParameters(parameters);
	}

	@Test
	public void testValidateParametersWithNull() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new FacetNoiseProcessor().validateParameters(null);
		}).getMessage();
		assertEquals("parameters is required.", message);
	}

	@Test
	public void testValidateParametersWithWrongType() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new FacetNoiseProcessor().validateParameters(new FacetRoundingParameters().setRoundTo(5L));
		}).getMessage();
		assertEquals("The NOISE algorithm requires FacetNoiseParameters", message);
	}

	@Test
	public void testValidateParametersWithNullBinSize() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new FacetNoiseProcessor().validateParameters(new FacetNoiseParameters().setEpsilon(0.5));
		}).getMessage();
		assertEquals("binSize is required.", message);
	}

	@Test
	public void testValidateParametersWithZeroBinSize() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new FacetNoiseProcessor().validateParameters(parameters.setBinSize(0L));
		}).getMessage();
		assertEquals("binSize must be a positive value", message);
	}

	@Test
	public void testValidateParametersWithNullEpsilon() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new FacetNoiseProcessor().validateParameters(new FacetNoiseParameters().setBinSize(10L));
		}).getMessage();
		assertEquals("epsilon is required.", message);
	}

	@Test
	public void testValidateParametersWithZeroEpsilon() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new FacetNoiseProcessor().validateParameters(parameters.setEpsilon(0.0));
		}).getMessage();
		assertEquals("epsilon must be a positive value", message);
	}

	@Test
	public void testProcessCountWithInvalidParameters() {
		FacetPostProcessingContext invalid = new FacetPostProcessingContext(parameters.setBinSize(-1L), 5L);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new FacetNoiseProcessor(mockRandom).processCount(12L, invalid);
		}).getMessage();
		assertEquals("binSize must be a positive value", message);
		verifyNoInteractions(mockRandom);
	}
}
