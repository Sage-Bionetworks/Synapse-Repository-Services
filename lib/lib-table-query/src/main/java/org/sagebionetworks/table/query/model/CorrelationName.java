package org.sagebionetworks.table.query.model;

/**
 * CorrelationName ::= {@link RegularIdentifier}
 * <p>
 * Note:
 * <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 * defines CorrelationName as an {@link Identifier}, however, MySQL does not
 * full support backtick and double quoted alias so we limit alias to
 * {@link RegularIdentifier}
 *
 */
public class CorrelationName extends SimpleBranch {

	public CorrelationName(RegularIdentifier regularIdentifier) {
		super(regularIdentifier);
	}

}
