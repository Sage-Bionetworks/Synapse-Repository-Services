package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * This matches &ltin predicate value&gt  in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class InPredicateValue extends SimpleBranch {
	
	public InPredicateValue(InValueList inValueList) {
		super(inValueList);
	}

	//NOTE: this is currently only used for translating a ArrayHasPredicate into an InPredicate. DO NOT EXPOSE IN PARSER
	public InPredicateValue(QuerySpecification subQuery){
		super(subQuery);
	}

}
