package org.sagebionetworks.table.query.model;

import java.util.List;


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
