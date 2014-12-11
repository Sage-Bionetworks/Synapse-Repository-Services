package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;


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

	public void visit(Visitor visitor) {
		visit(this.booleanPrimary, visitor);
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		visit(this.booleanPrimary, visitor);
		if(is != null){
			visitor.append(" IS ");
			if(not != null){
				visitor.append("NOT ");
			}
			visitor.append(this.truthValue.name());
		}
	}
}
