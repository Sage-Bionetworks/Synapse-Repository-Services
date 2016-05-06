package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.table.query.model.visitors.ToNameStringVisitor;
import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;


/**
 * This matches &ltactual identifier&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class ActualIdentifier extends SQLElement implements HasUnquotedValue {
	
	private final String regularIdentifier;
	private final String delimitedIdentifier;
	public ActualIdentifier(String regularIdentifier, String delimitedIdentifier) {
		if(regularIdentifier != null && delimitedIdentifier != null) throw new IllegalArgumentException("An actual identifier must be either a regular-identifier or a delimited-identifier but not both"); 
		this.regularIdentifier = regularIdentifier;
		this.delimitedIdentifier = delimitedIdentifier;
	}
	public String getRegularIdentifier() {
		return regularIdentifier;
	}
	public String getDelimitedIdentifier() {
		return delimitedIdentifier;
	}

	public void visit(Visitor visitor) {
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		// We do not
		if(regularIdentifier != null){
			// Regular identifiers can be written without modification.
			visitor.append(regularIdentifier);
		}else{
			// Delimited identifiers must be within double quotes.
			// And double quote characters must be escaped with another double quote.
			visitor.append("\"");
			visitor.append(delimitedIdentifier.replaceAll("\"", "\"\""));
			visitor.append("\"");
		}
	}

	public void visit(ToNameStringVisitor visitor) {
		if (regularIdentifier != null) {
			visitor.append(regularIdentifier);
		} else {
			visitor.append(delimitedIdentifier);
		}
	}
	@Override
	public void toSql(StringBuilder builder) {
		if(regularIdentifier != null){
			// Regular identifiers can be written without modification.
			builder.append(regularIdentifier);
		}else{
			// Delimited identifiers must be within double quotes.
			// And double quote characters must be escaped with another double quote.
			builder.append("\"");
			builder.append(delimitedIdentifier.replaceAll("\"", "\"\""));
			builder.append("\"");
		}
	}
	@Override
	public String getUnquotedValue() {
		if (regularIdentifier != null) {
			return regularIdentifier;
		} else {
			return delimitedIdentifier;
		}
	}
	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		// this element does not contain any SQLElements
	}
}
