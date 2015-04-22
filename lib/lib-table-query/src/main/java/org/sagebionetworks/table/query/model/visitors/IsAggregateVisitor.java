package org.sagebionetworks.table.query.model.visitors;


public class IsAggregateVisitor implements Visitor {
	private boolean isAggregate = false;

	public void setIsAggregate() {
		this.isAggregate = true;
	}

	public boolean isAggregate() {
		return isAggregate;
	}
}
