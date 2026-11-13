package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.FacetNoiseParameters;
import org.sagebionetworks.repo.model.FacetPostProcessingAlgorithm;
import org.sagebionetworks.repo.model.FacetRoundingParameters;
import org.sagebionetworks.repo.model.table.FacetColumnResult;
import org.sagebionetworks.repo.model.table.FacetColumnResultBinnedValueCount;
import org.sagebionetworks.repo.model.table.FacetColumnResultBinnedValues;
import org.sagebionetworks.repo.model.table.FacetColumnResultRange;
import org.sagebionetworks.repo.model.table.FacetColumnResultValueCount;
import org.sagebionetworks.repo.model.table.FacetColumnResultValues;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.TableConstants;

public class FacetRoundingProcessorTest {

	private final FacetRoundingProcessor processor = new FacetRoundingProcessor();

	@Test
	public void testGetAlgorithm() {
		// call under test
		assertEquals(FacetPostProcessingAlgorithm.ROUNDING, processor.getAlgorithm());
	}

	@Test
	public void testProcessWithEnumerationFacet() {
		FacetColumnResultValues raw = new FacetColumnResultValues()
				.setColumnName("state")
				.setJsonPath("$.state")
				.setFacetType(FacetType.enumeration)
				.setFacetValues(List.of(
						new FacetColumnResultValueCount().setValue("a").setCount(212L).setIsSelected(true),
						new FacetColumnResultValueCount().setValue("b").setCount(214L).setIsSelected(false),
						new FacetColumnResultValueCount().setValue("c").setCount(215L).setIsSelected(false),
						new FacetColumnResultValueCount().setValue("d").setCount(0L).setIsSelected(false)));

		FacetColumnResultBinnedValues expected = new FacetColumnResultBinnedValues()
				.setColumnName("state")
				.setJsonPath("$.state")
				.setFacetType(FacetType.enumeration)
				.setBinSize(5L)
				.setBinnedValues(List.of(
						new FacetColumnResultBinnedValueCount().setValue("a").setBinMin(210L).setIsSelected(true),
						new FacetColumnResultBinnedValueCount().setValue("b").setBinMin(210L).setIsSelected(false),
						new FacetColumnResultBinnedValueCount().setValue("c").setBinMin(215L).setIsSelected(false),
						new FacetColumnResultBinnedValueCount().setValue("d").setBinMin(0L).setIsSelected(false)));

		// call under test
		List<FacetColumnResult> results = processor.process(List.of(raw), new FacetRoundingParameters().setRoundTo(5L));

		assertEquals(List.of(expected), results);
	}

	@Test
	public void testProcessWithMaskedCounts() {
		FacetColumnResultValues state = new FacetColumnResultValues().setColumnName("state").setFacetType(FacetType.enumeration)
				.setFacetValues(List.of(new FacetColumnResultValueCount().setValue("a").setCount(212L).setIsSelected(true),
						new FacetColumnResultValueCount().setValue("b").setCount(-1L).setIsSelected(false)));
		FacetColumnResultValues site = new FacetColumnResultValues().setColumnName("site").setFacetType(FacetType.enumeration)
				.setFacetValues(List.of(new FacetColumnResultValueCount().setValue("x").setCount(-1L).setIsSelected(false),
						new FacetColumnResultValueCount().setValue("y").setCount(44L).setIsSelected(false)));

		// call under test
		List<FacetColumnResult> results = processor.process(List.of(state, site), new FacetRoundingParameters().setRoundTo(5L));

		// A masked count stays masked; flooring it would report the lowest bin, which starts at zero.
		assertEquals(List.of(
				new FacetColumnResultBinnedValues().setColumnName("state").setFacetType(FacetType.enumeration).setBinSize(5L)
						.setBinnedValues(List.of(
								new FacetColumnResultBinnedValueCount().setValue("a").setBinMin(210L).setIsSelected(true),
								new FacetColumnResultBinnedValueCount().setValue("b").setBinMin(-1L).setIsSelected(false))),
				new FacetColumnResultBinnedValues().setColumnName("site").setFacetType(FacetType.enumeration).setBinSize(5L)
						.setBinnedValues(List.of(
								new FacetColumnResultBinnedValueCount().setValue("x").setBinMin(-1L).setIsSelected(false),
								new FacetColumnResultBinnedValueCount().setValue("y").setBinMin(40L).setIsSelected(false)))),
				results);
	}

	@Test
	public void testProcessPreservesNullValueKeyword() {
		FacetColumnResultValues raw = new FacetColumnResultValues()
				.setColumnName("state")
				.setFacetType(FacetType.enumeration)
				.setFacetValues(List.of(
						new FacetColumnResultValueCount().setValue(TableConstants.NULL_VALUE_KEYWORD).setCount(7L).setIsSelected(false)));

		// call under test
		List<FacetColumnResult> results = processor.process(List.of(raw), new FacetRoundingParameters().setRoundTo(5L));

		FacetColumnResultBinnedValues binned = (FacetColumnResultBinnedValues) results.get(0);
		assertEquals(TableConstants.NULL_VALUE_KEYWORD, binned.getBinnedValues().get(0).getValue());
		assertEquals(5L, binned.getBinnedValues().get(0).getBinMin());
	}

	@Test
	public void testProcessPassesRangeFacetThrough() {
		// Range facets carry no counts, so they must be passed through unchanged.
		FacetColumnResultRange range = new FacetColumnResultRange()
				.setColumnName("age")
				.setFacetType(FacetType.range)
				.setColumnMin("1")
				.setColumnMax("99");

		// call under test
		List<FacetColumnResult> results = processor.process(List.of(range), new FacetRoundingParameters().setRoundTo(5L));

		assertEquals(List.of(range), results);
	}

	@Test
	public void testProcessWithNullRoundTo() {
		FacetColumnResultValues raw = new FacetColumnResultValues().setColumnName("state").setFacetType(FacetType.enumeration)
				.setFacetValues(List.of(new FacetColumnResultValueCount().setValue("a").setCount(212L).setIsSelected(false)));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.process(List.of(raw), new FacetRoundingParameters().setRoundTo(null));
		}).getMessage();
		assertEquals("roundTo is required.", message);
	}

	@Test
	public void testProcessWithZeroRoundTo() {
		FacetColumnResultValues raw = new FacetColumnResultValues().setColumnName("state").setFacetType(FacetType.enumeration)
				.setFacetValues(List.of(new FacetColumnResultValueCount().setValue("a").setCount(212L).setIsSelected(false)));

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.process(List.of(raw), new FacetRoundingParameters().setRoundTo(0L));
		});
	}

	@Test
	public void testProcessWithNegativeRoundTo() {
		FacetColumnResultValues raw = new FacetColumnResultValues().setColumnName("state").setFacetType(FacetType.enumeration)
				.setFacetValues(List.of(new FacetColumnResultValueCount().setValue("a").setCount(212L).setIsSelected(false)));

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.process(List.of(raw), new FacetRoundingParameters().setRoundTo(-5L));
		});
	}

	@Test
	public void testProcessWithWrongParametersType() {
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.process(List.of(), new FacetNoiseParameters().setEpsilon(1.0));
		});
	}

	@Test
	public void testProcessWithNullRawFacets() {
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.process(null, new FacetRoundingParameters().setRoundTo(5L));
		});
	}

	@Test
	public void testValidateParametersWithValidParameters() {
		// call under test
		processor.validateParameters(new FacetRoundingParameters().setRoundTo(5L));
	}

	@Test
	public void testValidateParametersWithNullParameters() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.validateParameters(null);
		}).getMessage();
		assertEquals("parameters is required.", message);
	}

	@Test
	public void testValidateParametersWithNullRoundTo() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.validateParameters(new FacetRoundingParameters());
		}).getMessage();
		assertEquals("roundTo is required.", message);
	}

	@Test
	public void testValidateParametersWithNegativeRoundTo() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.validateParameters(new FacetRoundingParameters().setRoundTo(-5L));
		}).getMessage();
		assertEquals("roundTo must be a positive value", message);
	}

	@Test
	public void testValidateParametersWithWrongParametersType() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.validateParameters(new FacetNoiseParameters().setEpsilon(1.0));
		}).getMessage();
		assertEquals("The ROUNDING algorithm requires FacetRoundingParameters", message);
	}

	@Test
	public void testProcessCountWithNonMultiple() {
		// call under test
		assertEquals(210L, processor.processCount(212L, new FacetRoundingParameters().setRoundTo(5L)));
	}

	@Test
	public void testProcessCountWithUpperEdgeOfBin() {
		// call under test
		assertEquals(210L, processor.processCount(214L, new FacetRoundingParameters().setRoundTo(5L)));
	}

	@Test
	public void testProcessCountWithExactMultiple() {
		// call under test
		assertEquals(215L, processor.processCount(215L, new FacetRoundingParameters().setRoundTo(5L)));
	}

	@Test
	public void testProcessCountWithCountBelowRoundTo() {
		// call under test
		assertEquals(0L, processor.processCount(4L, new FacetRoundingParameters().setRoundTo(5L)));
	}

	@Test
	public void testProcessCountWithZero() {
		// call under test
		assertEquals(0L, processor.processCount(0L, new FacetRoundingParameters().setRoundTo(5L)));
	}

	@Test
	public void testProcessCountWithNullParameters() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.processCount(212L, null);
		}).getMessage();
		assertEquals("parameters is required.", message);
	}

	@Test
	public void testProcessCountWithNullRoundTo() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.processCount(212L, new FacetRoundingParameters().setRoundTo(null));
		}).getMessage();
		assertEquals("roundTo is required.", message);
	}

	@Test
	public void testProcessCountWithZeroRoundTo() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.processCount(212L, new FacetRoundingParameters().setRoundTo(0L));
		}).getMessage();
		assertEquals("roundTo must be a positive value", message);
	}

	@Test
	public void testProcessCountWithWrongParametersType() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			processor.processCount(212L, new FacetNoiseParameters().setEpsilon(1.0));
		}).getMessage();
		assertEquals("The ROUNDING algorithm requires FacetRoundingParameters", message);
	}

}
