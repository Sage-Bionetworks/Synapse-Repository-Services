package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;


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

	public void visit(Visitor visitor) {
		visit(columnReferenceLHS, visitor);
		visit(betweenRowValueConstructor, visitor);
		visit(andRowValueConstructorRHS, visitor);
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		visit(columnReferenceLHS, visitor);
		visitor.setLHSColumn(columnReferenceLHS);
		if(not != null){
			visitor.append(" NOT");
		}
		visitor.append(" BETWEEN ");
		visit(betweenRowValueConstructor, visitor);
		visitor.append(" AND ");
		visit(andRowValueConstructorRHS, visitor);
		visitor.setLHSColumn(null);
	}
}
