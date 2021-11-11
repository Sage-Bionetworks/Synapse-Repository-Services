package org.sagebionetworks.table.query.model;

/**
 * This matches &ltcorrelation specification&gt in:
 * <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 * <p>
 * CorrelationSpecification ::= [ AS ]  {@link CorrelationName} 
 *
 */
public class CorrelationSpecification extends SQLElement {
	
	private final boolean hasAs;
	private final CorrelationName correlationName;
	
	public CorrelationSpecification(boolean hasAs, CorrelationName correlationName) {
		super();
		this.hasAs = hasAs;
		this.correlationName = correlationName;
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(correlationName);
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if(hasAs) {
			builder.append("AS ");
		}
		correlationName.toSql(builder, parameters);
	}

}
