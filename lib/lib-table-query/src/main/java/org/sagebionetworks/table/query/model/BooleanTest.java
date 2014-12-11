package org.sagebionetworks.table.query.model;


/**
 * This matches &ltboolean test&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
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
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		this.booleanPrimary.toSQL(builder, columnConvertor);
		if(is != null){
			builder.append(" IS ");
			if(not != null){
				builder.append("NOT ");
			}
			builder.append(this.truthValue.name());
		}
	}
	
}
