package org.sagebionetworks.table.query.model;

import java.util.List;

public class TruthSpecification extends SQLElement {
	
	private TruthValue truthValue;
	
	public TruthSpecification(TruthValue truthValue) {
		this.truthValue = truthValue;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(truthValue.name());
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		// TODO Auto-generated method stub

	}

}
