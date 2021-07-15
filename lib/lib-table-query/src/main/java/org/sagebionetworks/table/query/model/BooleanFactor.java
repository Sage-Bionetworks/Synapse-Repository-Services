package org.sagebionetworks.table.query.model;

/**
 * This matches &ltboolean factor&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
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
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if(not != null){
			builder.append("NOT ");
		}
		booleanTest.toSql(builder, parameters);
	}
	
	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(booleanTest);
	}
	
}
