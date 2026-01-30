package org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter;

import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.ValueMultiplicity;

public enum CellValueOperatorElement {
	//
	EQUALS("=", ValueMultiplicity.one),
	//
	NOT_EQUALS("<>", ValueMultiplicity.one),
	//
	GREATER_THAN(">", ValueMultiplicity.one),
	//
	LESS_THAN("<", ValueMultiplicity.one),
	//
	GREATER_THAN_OR_EQUALS(">=", ValueMultiplicity.one),
	//
	LESS_THAN_OR_EQUALS("<=", ValueMultiplicity.one),
	//
	LIKE("LIKE", ValueMultiplicity.one),
	//
	NOT_LIKE("NOT LIKE", ValueMultiplicity.one),
	//
	IN("IN", ValueMultiplicity.many),
	//
	NOT_IN("NOT IN", ValueMultiplicity.many),
	//
	IS_NULL("= JSON_ARRAY(null)", ValueMultiplicity.none),
	//
	IS_NOT_NULL("!= JSON_ARRAY(null)", ValueMultiplicity.none),
	//
	IS_UNDEFINED("= JSON_ARRAY(0,0)", ValueMultiplicity.none),
	//
	IS_DEFINED("!= JSON_ARRAY(0,0)", ValueMultiplicity.none);

	final private String sql;
	final private ValueMultiplicity valueMultiplicity;

	CellValueOperatorElement(String sql, ValueMultiplicity valueMultiplicity) {
		this.sql = sql;
		this.valueMultiplicity = valueMultiplicity;
	}

	public String toSql() {
		return sql;
	}

	public ValueMultiplicity getValueMultiplicity() {
		return valueMultiplicity;
	}

}
