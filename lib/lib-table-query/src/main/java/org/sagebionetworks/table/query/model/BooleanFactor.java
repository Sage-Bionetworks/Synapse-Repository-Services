package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;


/**
 * This matches &ltboolean factor&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class BooleanFactor extends SQLElement {

	Boolean not;
	BooleanTest booleanTest;
	
	public BooleanFactor(Boolean not, BooleanTest booleanTest) {
		super();
		this.not = not;
		this.booleanTest = booleanTest;
	}
	public Boolean getNot() {
		return not;
	}
	public BooleanTest getBooleanTest() {
		return booleanTest;
	}

	public void visit(Visitor visitor) {
		visit(booleanTest, visitor);
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		if(not != null){
			visitor.append("NOT ");
		}
		visit(booleanTest, visitor);
	}
	@Override
	public void toSql(StringBuilder builder) {
		if(not != null){
			builder.append("NOT ");
		}
		booleanTest.toSql(builder);
	}
	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, booleanTest);
	}
}
