package org.sagebionetworks.table.query.model;

public class TruthSpecification extends LeafElement {
	
	private TruthValue truthValue;
	
	public TruthSpecification(TruthValue truthValue) {
		this.truthValue = truthValue;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(truthValue.name());
	}

}
