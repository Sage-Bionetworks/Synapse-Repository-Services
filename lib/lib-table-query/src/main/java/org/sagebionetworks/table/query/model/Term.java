package org.sagebionetworks.table.query.model;


public class Term extends SQLElement {

	private Factor factor;

	public Term(Factor factor) {
		this.factor = factor;
	}

	public boolean isAggregate() {
		return factor.isAggregate();
	}

	public Factor getFactor() {
		return factor;
	}

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		this.factor.toSQL(builder, columnConvertor);
	}
}
