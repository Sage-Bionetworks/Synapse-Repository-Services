package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;


/**
 * This matches &ltrow value constructor&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class RowValueConstructor extends SQLElement {

	RowValueConstructorElement rowValueConstructorElement;
	RowValueConstructorList rowValueConstructorList;
	public RowValueConstructor(
			RowValueConstructorElement rowValueConstructorElement) {
		super();
		this.rowValueConstructorElement = rowValueConstructorElement;
	}
	public RowValueConstructor(RowValueConstructorList rowValueConstructorList) {
		super();
		this.rowValueConstructorList = rowValueConstructorList;
	}
	public RowValueConstructorElement getRowValueConstructorElement() {
		return rowValueConstructorElement;
	}
	public RowValueConstructorList getRowValueConstructorList() {
		return rowValueConstructorList;
	}

	public void visit(Visitor visitor) {
		if (rowValueConstructorElement != null) {
			visit(rowValueConstructorElement, visitor);
		} else {
			visit(rowValueConstructorList, visitor);
		}
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		if(rowValueConstructorElement != null){
			visit(rowValueConstructorElement, visitor);
		}else{
			visitor.append("( ");
			visit(rowValueConstructorList, visitor);
			visitor.append(" )");
		}
	}
}
