package org.sagebionetworks.table.query.model;

/**
 * NonJoinQueryTerm ::= {@link NonJoinQueryPrimary}
 *
 */
public class NonJoinQueryTerm extends SimpleBranch {

	public NonJoinQueryTerm(NonJoinQueryPrimary child) {
		super(child);
	}

}
