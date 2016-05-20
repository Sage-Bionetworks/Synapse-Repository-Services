package org.sagebionetworks.table.query.model;

import java.util.LinkedList;
import java.util.List;


/**
 * This matches &ltlike predicate&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class LikePredicate extends SQLElement implements HasPredicate {
	
	ColumnReference columnReferenceLHS;
	Boolean not;
	Pattern pattern;
	EscapeCharacter escapeCharacter;
	
	public LikePredicate(ColumnReference columnReferenceLHS, Boolean not, Pattern pattern, EscapeCharacter escapeCharacter) {
		this.columnReferenceLHS = columnReferenceLHS;
		this.not = not;
		this.pattern = pattern;
		this.escapeCharacter = escapeCharacter;
	}
	
	public Boolean getNot() {
		return not;
	}
	public Pattern getPattern() {
		return pattern;
	}
	public EscapeCharacter getEscapeCharacter() {
		return escapeCharacter;
	}

	public ColumnReference getColumnReferenceLHS() {
		return columnReferenceLHS;
	}

	@Override
	public void toSql(StringBuilder builder) {
		columnReferenceLHS.toSql(builder);
		if (not != null) {
			builder.append(" NOT");
		}
		builder.append(" LIKE ");
		pattern.toSql(builder);
		if (escapeCharacter != null) {
			builder.append(" ESCAPE ");
			escapeCharacter.toSql(builder);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, columnReferenceLHS);
		checkElement(elements, type, pattern);
		checkElement(elements, type, escapeCharacter);
	}

	@Override
	public ColumnReference getLeftHandSide() {
		return columnReferenceLHS;
	}

	@Override
	public Iterable<HasQuoteValue> getRightHandSideValues() {
		List<HasQuoteValue> results = new LinkedList<HasQuoteValue>();
		for(HasQuoteValue value: pattern.createIterable(HasQuoteValue.class)){
			results.add(value);
		}
		if(escapeCharacter != null){
			for(HasQuoteValue value: escapeCharacter.createIterable(HasQuoteValue.class)){
				results.add(value);
			}
		}
		return results;
	}
}
