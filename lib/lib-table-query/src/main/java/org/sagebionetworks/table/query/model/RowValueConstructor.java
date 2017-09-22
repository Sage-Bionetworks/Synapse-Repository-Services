package org.sagebionetworks.table.query.model;

/**
 * RowValueConstructor ::= {@link RowValueConstructorElement}
 * <p>
 * Note: '(' RowValueConstructorList ')' is not supported by MySql so this was excluded.
 */
public class RowValueConstructor extends SimpleBranch {

	public RowValueConstructor(
			RowValueConstructorElement rowValueConstructorElement) {
		super(rowValueConstructorElement);
	}

}
