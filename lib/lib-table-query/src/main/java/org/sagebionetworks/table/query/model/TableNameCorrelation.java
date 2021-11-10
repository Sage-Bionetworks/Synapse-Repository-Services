package org.sagebionetworks.table.query.model;

/**
 * Note: TableNameCorrelation is not part of the specification. The issue with
 * the original specification is {@link TableReference} can contain a
 * {@link QualifiedJoin} which in turn can contain a {@link TableReference}.
 * This circular recursion is not compatible with JavaCC. To break the cycle
 * both TableRefrence and QualifiedJoin depend on this new layer which acts as a
 * {@link TableName} with an optional AS {@link CorrelationSpecification}.
 *
 */
public class TableNameCorrelation extends SQLElement {

	private final TableName tableName;
	private final CorrelationSpecification correlationSpecification;

	public TableNameCorrelation(TableName tableName, CorrelationSpecification correlationSpecification) {
		super();
		this.tableName = tableName;
		this.correlationSpecification = correlationSpecification;
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(tableName, correlationSpecification);
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		tableName.toSql(builder, parameters);
		if (correlationSpecification != null) {
			builder.append(" ");
			correlationSpecification.toSql(builder, parameters);
		}
	}

}
