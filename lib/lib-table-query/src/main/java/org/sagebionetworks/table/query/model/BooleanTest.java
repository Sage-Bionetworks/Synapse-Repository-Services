package org.sagebionetworks.table.query.model;

/**
 * This matches &ltboolean test&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class BooleanTest extends SQLElement {

	BooleanPrimary booleanPrimary;
	Boolean is;
	Boolean not;
	TruthValue truthValue;
	
	public BooleanTest(BooleanPrimary booleanPrimary, Boolean is, Boolean not,
			TruthValue truthValue) {
		super();
		this.booleanPrimary = booleanPrimary;
		this.is = is;
		this.not = not;
		this.truthValue = truthValue;
	}

	public BooleanPrimary getBooleanPrimary() {
		return booleanPrimary;
	}

	public Boolean getIs() {
		return is;
	}

	public Boolean getNot() {
		return not;
	}

	public TruthValue getTruthValue() {
		return truthValue;
	}
	
	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		booleanPrimary.toSql(builder, parameters);
		if(is != null){
			builder.append(" IS ");
			if(not != null){
				builder.append("NOT ");
			}
			builder.append(this.truthValue.name());
		}
	}
	
	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(booleanPrimary);
	}
	
}
