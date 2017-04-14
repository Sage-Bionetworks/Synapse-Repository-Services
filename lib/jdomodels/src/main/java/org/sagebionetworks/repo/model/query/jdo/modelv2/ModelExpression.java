package org.sagebionetworks.repo.model.query.jdo.modelv2;

import org.sagebionetworks.repo.model.query.Comparator;

public class ModelExpression {

	Column column;
	Comparator compare;
	Object value;
	
	public ModelExpression(Column column, Comparator compare,
			Object value) {
		this.column = column;
		this.compare = compare;
		this.value = value;
	}

	
}
