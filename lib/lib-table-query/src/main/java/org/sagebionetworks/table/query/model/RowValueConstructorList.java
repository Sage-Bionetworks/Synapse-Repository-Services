package org.sagebionetworks.table.query.model;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;

/**
 * This matches &ltrow value constructor list&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class RowValueConstructorList extends SQLElement {
	
	List<RowValueConstructorElement> rowValueConstructorElements;

	public RowValueConstructorList() {
		this.rowValueConstructorElements = new LinkedList<RowValueConstructorElement>();
	}
	
	public RowValueConstructorList(List<RowValueConstructorElement> list) {
		this.rowValueConstructorElements = list;
	}

	public void addRowValueConstructorElement(RowValueConstructorElement rowValueConstructorElement){
		this.rowValueConstructorElements.add(rowValueConstructorElement);
	}

	public List<RowValueConstructorElement> getRowValueConstructorElements() {
		return rowValueConstructorElements;
	}

	public void visit(Visitor visitor) {
		for (RowValueConstructorElement element : rowValueConstructorElements) {
			visit(element, visitor);
		}
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		boolean isFrist = true;
		for(RowValueConstructorElement element: rowValueConstructorElements){
			if(!isFrist){
				visitor.append(", ");
			}
			visit(element, visitor);
			isFrist = false;
		}
	}

	@Override
	public void toSql(StringBuilder builder) {
		boolean isFrist = true;
		for(RowValueConstructorElement element: rowValueConstructorElements){
			if(!isFrist){
				builder.append(", ");
			}
			element.toSql(builder);
			isFrist = false;
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		for(RowValueConstructorElement element: rowValueConstructorElements){
			checkElement(elements, type, element);
		}
	}
}
