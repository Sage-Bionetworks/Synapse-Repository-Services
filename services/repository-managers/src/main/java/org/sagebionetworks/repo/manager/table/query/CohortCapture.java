package org.sagebionetworks.repo.manager.table.query;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sagebionetworks.repo.manager.table.RowHandlerProvider;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.web.BelowThresholdException;
import org.sagebionetworks.table.cluster.ResolvedCohort;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Captures the distinct values of a request-scoped cohort query as its rows stream, without ever
 * returning them. Acts as the provider of its own handler so the translated cohort query, which carries
 * the caller's access level to the cohort source, is known before the first row arrives.
 * <p>
 * Error messages never echo the captured values or their count.
 */
public class CohortCapture implements RowHandlerProvider, RowHandler {

	private final String name;
	private final Set<String> values;
	private ColumnType columnType;
	private boolean aggregateOnly;
	private Long suppressionThreshold;

	/**
	 * @param name the cohort's name.
	 */
	public CohortCapture(String name) {
		ValidateArgument.required(name, "name");
		this.name = name;
		this.values = new LinkedHashSet<>();
	}

	@Override
	public RowHandler getHandler(QueryTranslations translations) {
		columnType = translations.getMainQuery().getTranslator().getSelectColumns().get(0).getColumnType();
		if (ColumnTypeListMappings.isList(columnType)) {
			throw new IllegalArgumentException("Cohort '" + name + "' may not select a list column");
		}
		aggregateOnly = translations.isAggregateOnly();
		suppressionThreshold = translations.getSuppressionThreshold();
		return this;
	}

	@Override
	public void nextRow(Row row) {
		// A null can never match an IN predicate, so it is neither a member nor counted toward the gate.
		String value = row.getValues().get(0);
		if (value != null) {
			values.add(value);
		}
	}

	/**
	 * Build the resolved cohort from the captured values.
	 *
	 * @param maxValues the maximum number of distinct values a cohort may have.
	 * @throws IllegalArgumentException if the cohort has more than the maximum values.
	 * @throws BelowThresholdException  if the caller has aggregate-only access to the cohort source and the
	 *                                  cohort is non-empty but smaller than the source's threshold.
	 */
	public ResolvedCohort toResolvedCohort(int maxValues) {
		if (columnType == null) {
			throw new IllegalStateException("Cohort '" + name + "' was not captured");
		}
		if (values.size() > maxValues) {
			throw new IllegalArgumentException("Cohort '" + name + "' exceeds the maximum of " + maxValues + " values");
		}
		// The gate counts distinct values rather than rows, so a many-to-many source cannot inflate a
		// small cohort past the threshold.
		if (aggregateOnly && !values.isEmpty() && values.size() < suppressionThreshold) {
			throw new BelowThresholdException(suppressionThreshold);
		}
		return new ResolvedCohort(name, columnType, new ArrayList<>(values), aggregateOnly);
	}

}
