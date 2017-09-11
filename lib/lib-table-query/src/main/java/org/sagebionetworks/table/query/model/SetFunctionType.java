package org.sagebionetworks.table.query.model;

/**
 * This matches &ltset function type&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public enum SetFunctionType {
	AVG,
	MAX,
	MIN,
	SUM,
	COUNT
}
