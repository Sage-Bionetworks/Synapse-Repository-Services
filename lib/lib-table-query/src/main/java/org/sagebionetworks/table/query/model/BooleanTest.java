package org.sagebionetworks.table.query.model;

import java.util.List;


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
	public void toSql(StringBuilder builder) {
		booleanPrimary.toSql(builder);
		if(is != null){
			builder.append(" IS ");
			if(not != null){
				builder.append("NOT ");
			}
			builder.append(this.truthValue.name());
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, booleanPrimary);
	}
}
