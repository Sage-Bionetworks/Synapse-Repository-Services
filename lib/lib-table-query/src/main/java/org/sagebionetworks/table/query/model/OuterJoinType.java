package org.sagebionetworks.table.query.model;

/**
 * Modified subset of &ltouter join type&gt in:
 * <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 * Excludes FULL, as it is currently unsupported in MySQL.
 *
 */
public enum OuterJoinType {
	LEFT,
	RIGHT
}
