package org.sagebionetworks.table.query.model;


public class IsAggregateVisitor implements SQLElement.Visitor {
	private boolean isAggregate = false;

	public void setIsAggregate() {
		this.isAggregate = true;
	}

	public boolean isAggregate() {
		return isAggregate;
	}
}
