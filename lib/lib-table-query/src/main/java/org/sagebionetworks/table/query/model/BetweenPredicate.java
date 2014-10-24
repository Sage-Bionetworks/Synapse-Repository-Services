package org.sagebionetworks.table.query.model;


/**
 * This matches &ltbetween predicate&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */ 
public class BetweenPredicate extends SQLElement {
	
	ColumnReference columnReferenceLHS;
	Boolean not;
	RowValueConstructor betweenRowValueConstructor;
	RowValueConstructor andRowValueConstructorRHS;
	
	public BetweenPredicate(ColumnReference columnReferenceLHS,
			Boolean not, RowValueConstructor betweenRowValueConstructor,
			RowValueConstructor andRowValueConstructorRHS) {
		super();
		this.columnReferenceLHS = columnReferenceLHS;
		this.not = not;
		this.betweenRowValueConstructor = betweenRowValueConstructor;
		this.andRowValueConstructorRHS = andRowValueConstructorRHS;
	}

	public Boolean getNot() {
		return not;
	}
	public RowValueConstructor getBetweenRowValueConstructor() {
		return betweenRowValueConstructor;
	}
	public RowValueConstructor getAndRowValueConstructorRHS() {
		return andRowValueConstructorRHS;
	}
	
	public ColumnReference getColumnReferenceLHS() {
		return columnReferenceLHS;
	}

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		columnReferenceLHS.toSQL(builder, columnConvertor);
		if (columnConvertor != null) {
			columnConvertor.setLHSColumn(columnReferenceLHS);
		}
		if(not != null){
			builder.append(" NOT");
		}
		builder.append(" BETWEEN ");
		betweenRowValueConstructor.toSQL(builder, columnConvertor);
		builder.append(" AND ");
		andRowValueConstructorRHS.toSQL(builder, columnConvertor);
		if (columnConvertor != null) {
			columnConvertor.setLHSColumn(null);
		}
	}
	
}
