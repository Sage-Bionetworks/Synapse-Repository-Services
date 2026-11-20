package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.FacetNoiseParameters;
import org.sagebionetworks.repo.model.FacetPostProcessingAlgorithm;
import org.sagebionetworks.repo.model.FacetRoundingParameters;
import org.sagebionetworks.repo.model.table.FacetColumnResult;
import org.sagebionetworks.repo.model.table.FacetColumnResultRange;
import org.sagebionetworks.repo.model.table.FacetColumnResultValueCount;
import org.sagebionetworks.repo.model.table.FacetColumnResultValues;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.table.cluster.CountPostProcessing;

public class FacetNoiseProcessorTest {

	private FacetNoiseProcessor processor;
	private FacetNoiseParameters parameters;
	private FacetPostProcessingContext context;

	@BeforeEach
	public void before() {
		processor = new FacetNoiseProcessor();
		parameters = new FacetNoiseParameters().setBinSize(10L).setEpsilon(0.5);
		context = new FacetPostProcessingContext(parameters, 5L);
	}

	@Test
	public void testGetAlgorithm() {
		assertEquals(FacetPostProcessingAlgorithm.NOISE, processor.getAlgorithm());
	}

	@Test
	public void testReleaseWithZero() {
		// call under test
		assertEquals(0L, FacetNoiseProcessor.release(0L, 10L, 2.0, 5L, "count"));
	}

	@Test
	public void testReleaseRevealsOnlyTheBin() {
		// Every released value lies in its count's own bin, at or above the threshold, so the bin can be read
		// from the value but nothing finer. A wide scale pushes many values to the edges to exercise the clamps.
		for (long count = 5; count < 300; count++) {
			long lowerBound = (count / 10) * 10;
			for (String label : List.of("count", "a", "b", "c")) {
				// call under test
				long released = FacetNoiseProcessor.release(count, 10L, 100.0, 5L, label);
				assertTrue(released >= Math.max(lowerBound, 5L) && released <= lowerBound + 9,
						count + " released as " + released);
			}
		}
	}

	@Test
	public void testReleaseDependsOnlyOnTheBin() {
		long expected = FacetNoiseProcessor.release(210L, 10L, 2.0, 5L, "count");
		for (long count = 210; count < 220; count++) {
			// call under test
			assertEquals(expected, FacetNoiseProcessor.release(count, 10L, 2.0, 5L, "count"), "count " + count);
		}
	}

	@Test
	public void testReleaseIsDeterministic() {
		// call under test
		long first = FacetNoiseProcessor.release(1234L, 100L, 20.0, 5L, "count");
		long second = FacetNoiseProcessor.release(1234L, 100L, 20.0, 5L, "count");
		assertEquals(first, second);
	}

	@Test
	public void testReleaseVariesWithLabel() {
		Set<Long> byLabel = new HashSet<>();
		for (int i = 0; i < 50; i++) {
			// call under test
			byLabel.add(FacetNoiseProcessor.release(1234L, 100L, 20.0, 5L, "label" + i));
		}
		assertTrue(byLabel.size() > 10, byLabel.toString());
	}

	@Test
	public void testReleaseIsCenteredOnTheBin() {
		// Over many cells, the released values average to the bin's midpoint rather than its lower bound.
		double sum = 0;
		int cells = 2000;
		for (int i = 0; i < cells; i++) {
			// call under test
			sum += FacetNoiseProcessor.release(1234L, 100L, 20.0, 5L, "cell" + i);
		}
		assertEquals(1249.5, sum / cells, 2.0);
	}

	@Test
	public void testReleaseMatchesTheIndexDatabaseFunction() {
		// The same golden values are asserted against aggregateCountRoundedNoise in TableIndexDAOImplTest, so the
		// Java and SQL releases stay identical.
		assertEquals(GOLDEN_12, FacetNoiseProcessor.release(12L, 10L, 0.5, 5L, "count"));
		assertEquals(GOLDEN_17, FacetNoiseProcessor.release(17L, 10L, 5.0, 5L, "facet\u001Fstage\u001F\u001FII"));
		assertEquals(GOLDEN_7, FacetNoiseProcessor.release(7L, 10L, 2.0, 5L, "count"));
		assertEquals(GOLDEN_214, FacetNoiseProcessor.release(214L, 10L, 1.0, 5L, "0[\"a\"]"));
		assertEquals(GOLDEN_1234, FacetNoiseProcessor.release(1234L, 100L, 50.0, 20L, "count"));
		assertEquals(GOLDEN_55, FacetNoiseProcessor.release(55L, 20L, 10.0, 5L, "1[]"));
	}

	private static final long GOLDEN_12 = 14L;
	private static final long GOLDEN_17 = 15L;
	private static final long GOLDEN_7 = 5L;
	private static final long GOLDEN_214 = 215L;
	private static final long GOLDEN_1234 = 1200L;
	private static final long GOLDEN_55 = 59L;

	@Test
	public void testProcessCount() {
		// call under test
		long result = processor.processCount(1234L, context);

		assertEquals(FacetNoiseProcessor.release(1234L, 10L, 2.0, 5L, FacetNoiseProcessor.TOTAL_COUNT_LABEL), result);
	}

	@Test
	public void testProcess() {
		FacetColumnResultValues stage = new FacetColumnResultValues().setColumnName("stage")
				.setFacetType(FacetType.enumeration).setFacetValues(List.of(
						new FacetColumnResultValueCount().setValue("II").setCount(57L).setIsSelected(true),
						new FacetColumnResultValueCount().setValue("III").setCount(0L).setIsSelected(false),
						new FacetColumnResultValueCount().setValue("IV").setCount(-1L).setIsSelected(false)));
		FacetColumnResultValues site = new FacetColumnResultValues().setColumnName("site").setJsonPath("$.a")
				.setFacetType(FacetType.enumeration).setFacetValues(List.of(
						new FacetColumnResultValueCount().setValue("II").setCount(57L).setIsSelected(false)));
		FacetColumnResultRange range = new FacetColumnResultRange().setColumnName("age").setFacetType(FacetType.range)
				.setColumnMin("1").setColumnMax("9");

		// call under test
		List<FacetColumnResult> result = processor.process(List.of(stage, site, range), context);

		List<FacetColumnResult> expected = List.of(
				new FacetColumnResultValues().setColumnName("stage").setFacetType(FacetType.enumeration).setFacetValues(List.of(
						new FacetColumnResultValueCount().setValue("II")
								.setCount(FacetNoiseProcessor.release(57L, 10L, 2.0, 5L, "facet\u001Fstage\u001F\u001FII"))
								.setIsSelected(true),
						// zero stays zero and a masked count stays masked
						new FacetColumnResultValueCount().setValue("III").setCount(0L).setIsSelected(false),
						new FacetColumnResultValueCount().setValue("IV").setCount(-1L).setIsSelected(false))),
				new FacetColumnResultValues().setColumnName("site").setJsonPath("$.a").setFacetType(FacetType.enumeration)
						.setFacetValues(List.of(new FacetColumnResultValueCount().setValue("II")
								.setCount(FacetNoiseProcessor.release(57L, 10L, 2.0, 5L, "facet\u001Fsite\u001F$.a\u001FII"))
								.setIsSelected(false))),
				range);
		assertEquals(expected, result);
	}

	@Test
	public void testProcessIsDeterministic() {
		List<FacetColumnResult> facets = List.of(new FacetColumnResultValues().setColumnName("stage")
				.setFacetType(FacetType.enumeration).setFacetValues(List.of(
						new FacetColumnResultValueCount().setValue("II").setCount(57L).setIsSelected(false))));

		// call under test
		List<FacetColumnResult> first = processor.process(facets, context);
		List<FacetColumnResult> second = processor.process(facets, context);

		assertEquals(first, second);
	}

	@Test
	public void testGetCountPostProcessing() {
		// call under test
		CountPostProcessing result = processor.getCountPostProcessing(context);

		assertEquals(new CountPostProcessing.RoundedNoise(10L, 2.0), result);
	}

	@Test
	public void testValidateParameters() {
		// call under test
		processor.validateParameters(parameters);
	}

	@Test
	public void testValidateParametersWithNull() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.validateParameters(null);
		}).getMessage();
		assertEquals("parameters is required.", message);
	}

	@Test
	public void testValidateParametersWithWrongType() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.validateParameters(new FacetRoundingParameters().setRoundTo(5L));
		}).getMessage();
		assertEquals("The NOISE algorithm requires FacetNoiseParameters", message);
	}

	@Test
	public void testValidateParametersWithNullBinSize() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.validateParameters(new FacetNoiseParameters().setEpsilon(0.5));
		}).getMessage();
		assertEquals("binSize is required.", message);
	}

	@Test
	public void testValidateParametersWithZeroBinSize() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.validateParameters(parameters.setBinSize(0L));
		}).getMessage();
		assertEquals("binSize must be a positive value", message);
	}

	@Test
	public void testValidateParametersWithNullEpsilon() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.validateParameters(new FacetNoiseParameters().setBinSize(10L));
		}).getMessage();
		assertEquals("epsilon is required.", message);
	}

	@Test
	public void testValidateParametersWithZeroEpsilon() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.validateParameters(parameters.setEpsilon(0.0));
		}).getMessage();
		assertEquals("epsilon must be a positive value", message);
	}

	@Test
	public void testProcessCountWithInvalidParameters() {
		FacetPostProcessingContext invalid = new FacetPostProcessingContext(parameters.setBinSize(-1L), 5L);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.processCount(12L, invalid);
		}).getMessage();
		assertEquals("binSize must be a positive value", message);
	}
}
