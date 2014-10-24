package org.sagebionetworks.table.query.model;


/**
 * This matches &ltsort specification&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class SortSpecification extends SQLElement {
	
    SortKey sortKey;
    OrderingSpecification orderingSpecification;
	public SortSpecification(SortKey sortKey,
			OrderingSpecification orderingSpecification) {
		super();
		this.sortKey = sortKey;
		this.orderingSpecification = orderingSpecification;
	}
	public SortKey getSortKey() {
		return sortKey;
	}
	public OrderingSpecification getOrderingSpecification() {
		return orderingSpecification;
	}
	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		sortKey.toSQL(builder, columnConvertor);
		if(orderingSpecification != null){
			builder.append(" ").append(orderingSpecification.name());
		}
	}
    
}
