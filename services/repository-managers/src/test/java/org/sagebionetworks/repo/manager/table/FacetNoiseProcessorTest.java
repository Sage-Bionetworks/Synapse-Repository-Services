package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

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

@ExtendWith(MockitoExtension.class)
public class FacetNoiseProcessorTest {

	@Mock
	private RandomGenerator mockRandom;

	@Test
	public void testGetAlgorithm() {
		// call under test
		assertEquals(FacetPostProcessingAlgorithm.NOISE, new FacetNoiseProcessor().getAlgorithm());
	}

	@Test
	public void testProcessWithZeroNoise() {
		// nextDouble()==0.5 -> u==0 -> sample==0 -> the count is unchanged.
		when(mockRandom.nextDouble()).thenReturn(0.5);
		FacetNoiseProcessor processor = new FacetNoiseProcessor(mockRandom);
		FacetColumnResultValues raw = new FacetColumnResultValues()
				.setColumnName("state")
				.setJsonPath("$.state")
				.setFacetType(FacetType.enumeration)
				.setFacetValues(List.of(new FacetColumnResultValueCount().setValue("a").setCount(212L).setIsSelected(true)));

		FacetColumnResultValues expected = new FacetColumnResultValues()
				.setColumnName("state")
				.setJsonPath("$.state")
				.setFacetType(FacetType.enumeration)
				.setFacetValues(List.of(new FacetColumnResultValueCount().setValue("a").setCount(212L).setIsSelected(true)));

		// call under test
		List<FacetColumnResult> results = processor.process(List.of(raw), new FacetNoiseParameters().setEpsilon(1.0));

		assertEquals(List.of(expected), results);
	}

	@Test
	public void testProcessWithPositiveNoise() {
		// nextDouble()==0.75 -> u==0.25 -> sample = -1*sign(+)*ln(0.5) = 0.693 -> round==1.
		when(mockRandom.nextDouble()).thenReturn(0.75);
		FacetNoiseProcessor processor = new FacetNoiseProcessor(mockRandom);
		FacetColumnResultValues raw = new FacetColumnResultValues().setColumnName("state").setFacetType(FacetType.enumeration)
				.setFacetValues(List.of(new FacetColumnResultValueCount().setValue("a").setCount(212L).setIsSelected(false)));

		// call under test
		List<FacetColumnResult> results = processor.process(List.of(raw), new FacetNoiseParameters().setEpsilon(1.0));

		FacetColumnResultValues noised = (FacetColumnResultValues) results.get(0);
		assertEquals(213L, noised.getFacetValues().get(0).getCount());
	}

	@Test
	public void testProcessClampsNegativeToZero() {
		// nextDouble()==0.25 -> u==-0.25 -> sample = -1 -> count 0 would go to -1 and must clamp to 0.
		when(mockRandom.nextDouble()).thenReturn(0.25);
		FacetNoiseProcessor processor = new FacetNoiseProcessor(mockRandom);
		FacetColumnResultValues raw = new FacetColumnResultValues().setColumnName("state").setFacetType(FacetType.enumeration)
				.setFacetValues(List.of(new FacetColumnResultValueCount().setValue("a").setCount(0L).setIsSelected(false)));

		// call under test
		List<FacetColumnResult> results = processor.process(List.of(raw), new FacetNoiseParameters().setEpsilon(1.0));

		FacetColumnResultValues noised = (FacetColumnResultValues) results.get(0);
		assertEquals(0L, noised.getFacetValues().get(0).getCount());
	}

	@Test
	public void testProcessPassesRangeFacetThrough() {
		FacetNoiseProcessor processor = new FacetNoiseProcessor(mockRandom);
		FacetColumnResultRange range = new FacetColumnResultRange().setColumnName("age").setFacetType(FacetType.range)
				.setColumnMin("1").setColumnMax("99");

		// call under test
		List<FacetColumnResult> results = processor.process(List.of(range), new FacetNoiseParameters().setEpsilon(1.0));

		assertEquals(List.of(range), results);
	}

	@Test
	public void testProcessStatisticalProperties() {
		// Over a large sample the mean of the noised counts is close to the raw count and no count is
		// ever negative. A real seeded generator drives the draws here.
		long rawCount = 1000L;
		int sampleSize = 5000;
		FacetColumnResultValueCount[] values = new FacetColumnResultValueCount[sampleSize];
		for (int i = 0; i < sampleSize; i++) {
			values[i] = new FacetColumnResultValueCount().setValue("v" + i).setCount(rawCount).setIsSelected(false);
		}
		FacetColumnResultValues raw = new FacetColumnResultValues().setColumnName("state").setFacetType(FacetType.enumeration)
				.setFacetValues(List.of(values));

		FacetNoiseProcessor processor = new FacetNoiseProcessor(new Random(42));

		// call under test
		List<FacetColumnResult> results = processor.process(List.of(raw), new FacetNoiseParameters().setEpsilon(1.0));

		FacetColumnResultValues noised = (FacetColumnResultValues) results.get(0);
		long sum = 0;
		for (FacetColumnResultValueCount count : noised.getFacetValues()) {
			assertTrue(count.getCount() >= 0, "noised count must never be negative");
			sum += count.getCount();
		}
		double mean = (double) sum / sampleSize;
		assertTrue(Math.abs(mean - rawCount) < 5.0, "expected mean near " + rawCount + " but was " + mean);
	}

	@Test
	public void testProcessWithNullEpsilon() {
		FacetNoiseProcessor processor = new FacetNoiseProcessor(mockRandom);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.process(List.of(), new FacetNoiseParameters().setEpsilon(null));
		}).getMessage();
		assertEquals("epsilon is required.", message);
	}

	@Test
	public void testProcessWithZeroEpsilon() {
		FacetNoiseProcessor processor = new FacetNoiseProcessor(mockRandom);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.process(List.of(), new FacetNoiseParameters().setEpsilon(0.0));
		});
	}

	@Test
	public void testProcessWithNegativeEpsilon() {
		FacetNoiseProcessor processor = new FacetNoiseProcessor(mockRandom);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.process(List.of(), new FacetNoiseParameters().setEpsilon(-1.0));
		});
	}

	@Test
	public void testProcessWithWrongParametersType() {
		FacetNoiseProcessor processor = new FacetNoiseProcessor(mockRandom);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.process(List.of(), new FacetRoundingParameters().setRoundTo(5L));
		});
	}

}
