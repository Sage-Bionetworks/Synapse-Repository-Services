package org.sagebionetworks.table.query.model;


/**
 * This matches &ltlike predicate&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class LikePredicate extends SQLElement {
	
	ColumnReference columnReferenceLHS;
	Boolean not;
	Pattern pattern;
	EscapeCharacter escapeCharacter;
	
	public LikePredicate(ColumnReference columnReferenceLHS, Boolean not, Pattern pattern,
			EscapeCharacter escapeCharacter) {
		super();
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
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		columnReferenceLHS.toSQL(builder, columnConvertor);
		if(not != null){
			builder.append(" NOT");
		}
		builder.append(" LIKE ");
		pattern.toSQL(builder, columnConvertor);
		if(escapeCharacter != null){
			builder.append(" ESCAPE ");
			escapeCharacter.toSQL(builder, columnConvertor);
		}
	}
	
}
