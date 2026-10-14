package org.sagebionetworks.table.query.model;

import java.util.Optional;

/**
 * This matches &ltin predicate value&gt  in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 * extended with a {@link CohortReference} alternative.
 */
public class InPredicateValue extends SimpleBranch {

	public InPredicateValue(InValueList inValueList) {
		super(inValueList);
	}

	public InPredicateValue(CohortReference cohortReference) {
		super(cohortReference);
	}

	/**
	 * @return the cohort reference when this value is {@code COHORT(<name>)}, else empty.
	 */
	public Optional<CohortReference> getCohortReference() {
		return child instanceof CohortReference cohortReference ? Optional.of(cohortReference) : Optional.empty();
	}

}
