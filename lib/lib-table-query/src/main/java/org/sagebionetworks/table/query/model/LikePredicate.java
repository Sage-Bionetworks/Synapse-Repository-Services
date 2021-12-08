package org.sagebionetworks.table.query.model;

import java.util.LinkedList;
import java.util.List;


/**
 * This matches &ltlike predicate&gt  in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class LikePredicate extends SQLElement implements HasPredicate {
	
	ReplaceableBox<ColumnReference> columnReferenceLHS;
	Boolean not;
	Pattern pattern;
	EscapeCharacter escapeCharacter;
	
	public LikePredicate(ColumnReference columnReferenceLHS, Boolean not, Pattern pattern, EscapeCharacter escapeCharacter) {
		this.columnReferenceLHS = new ReplaceableBox<ColumnReference>(columnReferenceLHS);
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
		return columnReferenceLHS.getChild();
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		columnReferenceLHS.toSql(builder, parameters);
		if (not != null) {
			builder.append(" NOT");
		}
		builder.append(" LIKE ");
		pattern.toSql(builder, parameters);
		if (escapeCharacter != null) {
			builder.append(" ESCAPE ");
			escapeCharacter.toSql(builder, parameters);
		}
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(columnReferenceLHS, pattern, escapeCharacter);
	}

	@Override
	public ColumnReference getLeftHandSide() {
		return columnReferenceLHS.getChild();
	}

	@Override
	public Iterable<UnsignedLiteral> getRightHandSideValues() {
		List<UnsignedLiteral> results = new LinkedList<UnsignedLiteral>();
		results.add(pattern.getFirstElementOfType(UnsignedLiteral.class));
		if(escapeCharacter != null){
			results.add(escapeCharacter.getFirstElementOfType(UnsignedLiteral.class));
		}
		return results;
	}

}
