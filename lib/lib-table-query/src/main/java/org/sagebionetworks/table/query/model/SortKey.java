package org.sagebionetworks.table.query.model;


/**
 * SortKey ::= {%link ValueExpression}
 * <p>
 * SortKey was extend to to have a ValueExpression child to support functions
 * in the order by clause (see: PLFM-4566).
 */
public class SortKey extends SimpleBranch {
	

	public SortKey(ValueExpression valueExpression) {
		super(valueExpression);
	}

}
