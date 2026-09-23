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

}
