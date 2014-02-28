package org.sagebionetworks.table.query.model;

/**
 * This matches &ltcomparison predicate&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class ComparisonPredicate {

	RowValueConstructor rowValueConstructorLHS;
	CompOp compOp;
	RowValueConstructor rowValueConstructorRHS;
}
