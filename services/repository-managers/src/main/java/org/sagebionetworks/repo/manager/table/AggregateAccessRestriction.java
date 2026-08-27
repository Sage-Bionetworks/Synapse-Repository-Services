package org.sagebionetworks.repo.manager.table;

import java.util.Objects;
import java.util.Optional;

import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Internal (non-API) result of a table read-access check. Describes whether the
 * user is limited to aggregate-only reads of a table and, when they are, the
 * single {@link AggregateDataConfiguration} bound to the source that forced that
 * mode. The whole-tree read decision (including rejecting a query that spans more
 * than one aggregate data source) is made by
 * {@code EntityAuthorizationManager.canQueryTableOrView}; this type only carries
 * the resolved configuration into the query pipeline.
 */
public class AggregateAccessRestriction {

	private static final AggregateAccessRestriction NOT_RESTRICTED = new AggregateAccessRestriction(false, null);

	private final boolean aggregateOnly;
	private final AggregateDataConfiguration configuration;

	private AggregateAccessRestriction(boolean aggregateOnly, AggregateDataConfiguration configuration) {
		this.aggregateOnly = aggregateOnly;
		this.configuration = configuration;
	}

	/**
	 * @return A restriction indicating the user has full (non-aggregate) read access.
	 */
	public static AggregateAccessRestriction notRestricted() {
		return NOT_RESTRICTED;
	}

	/**
	 * @return An aggregate-only restriction bound to the provided configuration.
	 */
	public static AggregateAccessRestriction aggregateOnly(AggregateDataConfiguration configuration) {
		ValidateArgument.required(configuration, "configuration");
		return new AggregateAccessRestriction(true, configuration);
	}

	public boolean isAggregateOnly() {
		return aggregateOnly;
	}

	/**
	 * @return The {@link AggregateDataConfiguration} bound to the aggregate-only
	 *         source, or empty when not aggregate-only. PLFM-9757 applies the bound
	 *         facet post-processing configuration at query time.
	 */
	public Optional<AggregateDataConfiguration> getConfiguration() {
		return Optional.ofNullable(configuration);
	}

	/**
	 * @return The suppression threshold used to gate the query, or empty when not
	 *         aggregate-only.
	 */
	public Optional<Long> getSuppressionThreshold() {
		return getConfiguration().map(AggregateDataConfiguration::getSuppressionThreshold);
	}

	@Override
	public int hashCode() {
		return Objects.hash(aggregateOnly, configuration);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof AggregateAccessRestriction)) {
			return false;
		}
		AggregateAccessRestriction other = (AggregateAccessRestriction) obj;
		return aggregateOnly == other.aggregateOnly && Objects.equals(configuration, other.configuration);
	}

	@Override
	public String toString() {
		return "AggregateAccessRestriction [aggregateOnly=" + aggregateOnly + ", configuration=" + configuration + "]";
	}

}
