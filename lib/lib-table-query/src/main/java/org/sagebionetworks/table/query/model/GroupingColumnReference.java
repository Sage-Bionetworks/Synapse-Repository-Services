package org.sagebionetworks.table.query.model;


/**
 * To support function in a group by this was change to have a ValueExpression child (see: PLFM-4566).
 * <p>
 *  GroupingColumnReference ::= {@link ValueExpression}
 */
public class GroupingColumnReference extends SimpleBranch {

	public GroupingColumnReference(ValueExpression valueExpression) {
		super(valueExpression);
	}
}
