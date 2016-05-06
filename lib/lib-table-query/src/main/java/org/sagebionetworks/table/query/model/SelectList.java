package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor.SQLClause;
import org.sagebionetworks.table.query.model.visitors.Visitor;
/**
 * This matches &ltselect list&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class SelectList extends SQLElement {
	
	Boolean asterisk;
	List<DerivedColumn> columns;
	
	public SelectList(List<DerivedColumn> columns) {
		super();
		this.columns = columns;
	}

	public SelectList(Boolean asterisk) {
		super();
		this.asterisk = asterisk;
	}

	public Boolean getAsterisk() {
		return asterisk;
	}

	public List<DerivedColumn> getColumns() {
		return columns;
	}

	public void visit(Visitor visitor) {
		if (asterisk == null) {
			for (DerivedColumn dc : columns) {
				visit(dc, visitor);
			}
		}
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		// select is either star or a list of derived columns
		visitor.pushCurrentClause(SQLClause.SELECT);
		if(asterisk != null){
			visitor.append("*");
		}else{
			boolean first = true;
			for(DerivedColumn dc: columns){
				if(!first){
					visitor.append(", ");
				}
				visit(dc, visitor);
				first = false;
			}
		}
		visitor.popCurrentClause(SQLClause.SELECT);
	}

	@Override
	public void toSql(StringBuilder builder) {
		if(asterisk != null){
			builder.append("*");
		}else{
			boolean first = true;
			for(DerivedColumn dc: columns){
				if(!first){
					builder.append(", ");
				}
				dc.toSql(builder);
				first = false;
			}
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		if(columns != null){
			for(DerivedColumn dc: columns){
				checkElement(elements, type, dc);
			}
		}
	}
}
