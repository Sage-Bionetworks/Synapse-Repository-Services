package org.sagebionetworks.table.query.model;

/**
 * This matches &ltlike predicate&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class LikePredicate {
	
	MatchValue matchValue;
	Boolean not;
	Pattern pattern;
	EscapeCharacter escapeCharacter;
	
	public LikePredicate(MatchValue matchValue, Boolean not, Pattern pattern,
			EscapeCharacter escapeCharacter) {
		super();
		this.matchValue = matchValue;
		this.not = not;
		this.pattern = pattern;
		this.escapeCharacter = escapeCharacter;
	}
	
	public MatchValue getMatchValue() {
		return matchValue;
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
	
}
