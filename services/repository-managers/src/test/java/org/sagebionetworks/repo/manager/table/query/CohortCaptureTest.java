package org.sagebionetworks.repo.manager.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.web.BelowThresholdException;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.ResolvedCohort;

@ExtendWith(MockitoExtension.class)
public class CohortCaptureTest {

	@Mock
	private QueryTranslations mockTranslations;
	@Mock
	private MainQuery mockMainQuery;
	@Mock
	private QueryTranslator mockTranslator;

	private void setupTranslations(ColumnType type, boolean aggregateOnly, Long threshold) {
		when(mockTranslations.getMainQuery()).thenReturn(mockMainQuery);
		when(mockMainQuery.getTranslator()).thenReturn(mockTranslator);
		when(mockTranslator.getSelectColumns()).thenReturn(List.of(new SelectColumn().setName("pid").setColumnType(type)));
		when(mockTranslations.isAggregateOnly()).thenReturn(aggregateOnly);
		when(mockTranslations.getSuppressionThreshold()).thenReturn(threshold);
	}

	private static void stream(CohortCapture capture, String... values) {
		for (String value : values) {
			capture.nextRow(new Row().setValues(Arrays.asList(value)));
		}
	}

	@Test
	public void testToResolvedCohortWithFullAccess() {
		setupTranslations(ColumnType.INTEGER, false, null);
		CohortCapture capture = new CohortCapture("c1");
		assertSame(capture, capture.getHandler(mockTranslations));
		stream(capture, "3", "1", null, "3", "2");
		// call under test
		ResolvedCohort cohort = capture.toResolvedCohort(10);
		// Distinct and non-null, in first-seen order.
		assertEquals(new ResolvedCohort("c1", ColumnType.INTEGER, List.of("3", "1", "2"), false), cohort);
	}

	@Test
	public void testToResolvedCohortWithFullAccessBelowThreshold() {
		setupTranslations(ColumnType.STRING, false, null);
		CohortCapture capture = new CohortCapture("c1");
		capture.getHandler(mockTranslations);
		stream(capture, "a");
		// call under test
		ResolvedCohort cohort = capture.toResolvedCohort(10);
		assertEquals(new ResolvedCohort("c1", ColumnType.STRING, List.of("a"), false), cohort);
	}

	@Test
	public void testToResolvedCohortWithAggregateOnlyAtThreshold() {
		setupTranslations(ColumnType.INTEGER, true, 3L);
		CohortCapture capture = new CohortCapture("c1");
		capture.getHandler(mockTranslations);
		stream(capture, "1", "2", "3");
		// call under test
		ResolvedCohort cohort = capture.toResolvedCohort(10);
		assertEquals(new ResolvedCohort("c1", ColumnType.INTEGER, List.of("1", "2", "3"), true), cohort);
	}

	@Test
	public void testToResolvedCohortWithAggregateOnlyBelowThreshold() {
		setupTranslations(ColumnType.INTEGER, true, 3L);
		CohortCapture capture = new CohortCapture("c1");
		capture.getHandler(mockTranslations);
		// Repeated values count once, so many rows cannot inflate a small cohort past the threshold.
		stream(capture, "1", "2", "1", "2", "1", "2");
		BelowThresholdException exception = assertThrows(BelowThresholdException.class, () -> {
			// call under test
			capture.toResolvedCohort(10);
		});
		assertEquals(3L, exception.getSuppressionThreshold());
	}

	@Test
	public void testToResolvedCohortWithAggregateOnlyEmpty() {
		setupTranslations(ColumnType.INTEGER, true, 3L);
		CohortCapture capture = new CohortCapture("c1");
		capture.getHandler(mockTranslations);
		// call under test
		ResolvedCohort cohort = capture.toResolvedCohort(10);
		assertEquals(new ResolvedCohort("c1", ColumnType.INTEGER, List.of(), true), cohort);
	}

	@Test
	public void testToResolvedCohortWithTooManyValues() {
		setupTranslations(ColumnType.INTEGER, false, null);
		CohortCapture capture = new CohortCapture("c1");
		capture.getHandler(mockTranslations);
		stream(capture, "1", "2", "3");
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			capture.toResolvedCohort(2);
		}).getMessage();
		assertEquals("Cohort 'c1' exceeds the maximum of 2 values", message);
	}

	@Test
	public void testGetHandlerWithListColumn() {
		when(mockTranslations.getMainQuery()).thenReturn(mockMainQuery);
		when(mockMainQuery.getTranslator()).thenReturn(mockTranslator);
		when(mockTranslator.getSelectColumns())
				.thenReturn(List.of(new SelectColumn().setName("tags").setColumnType(ColumnType.STRING_LIST)));
		CohortCapture capture = new CohortCapture("c1");
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			capture.getHandler(mockTranslations);
		}).getMessage();
		assertEquals("Cohort 'c1' may not select a list column", message);
	}

	@Test
	public void testToResolvedCohortWithoutCapture() {
		CohortCapture capture = new CohortCapture("c1");
		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			capture.toResolvedCohort(10);
		}).getMessage();
		assertEquals("Cohort 'c1' was not captured", message);
	}

}
