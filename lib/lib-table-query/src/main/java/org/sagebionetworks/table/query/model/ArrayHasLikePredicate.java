package org.sagebionetworks.table.query.model;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Iterables;

/**
 * Custom "HAS_LIKE" predicate for searching patterns in multi-value columns.
 *
 * <HAS_LIKE predicate> ::= <row value constructor> [ NOT ] IN <in predicate value> [ESCAPE <escape character>]
 *
 * Examples:
 * columnName HAS_LIKE ("value1%", "value2", "value3") ESCAPE '_'
 *
 * See  https://sagebionetworks.jira.com/wiki/spaces/PLFM/pages/817168468/Multiple+Value+Annotations
 *
 * Related: {@link ArrayFunctionSpecification}
 *
 * NOTE the implemented {@link HasPredicate} interface is not for the "HAS" keyword, but, instead an interface for any predicate
 */
public class ArrayHasLikePredicate extends ArrayHasPredicate {
	
	private static final String KEYWORD = "HAS_LIKE";

	private EscapeCharacter escapeCharacter;

	public ArrayHasLikePredicate(ColumnReference columnReferenceLHS, Boolean not, InPredicateValue inPredicateValue,
			EscapeCharacter escapeCharacter) {
		super(columnReferenceLHS, not, inPredicateValue);
		this.escapeCharacter = escapeCharacter;
	}
	
	@Override
	public String getKeyWord() {
		return KEYWORD;
	}
	
	public EscapeCharacter getEscapeCharacter() {
		return escapeCharacter;
	}
	
	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		super.toSql(builder, parameters);
		if (escapeCharacter != null) {
			builder.append(" ESCAPE ");
			escapeCharacter.toSql(builder, parameters);
		}
	}
	
	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		super.addElements(elements, type);
		checkElement(elements, type, escapeCharacter);
	}
	
	@Override
	public Iterable<UnsignedLiteral> getRightHandSideValues() {
		Iterable<UnsignedLiteral> valuesIterable = super.getRightHandSideValues();

		if (escapeCharacter != null) {
			UnsignedLiteral escapeLiteral = escapeCharacter.getFirstElementOfType(UnsignedLiteral.class);
			
			valuesIterable = Iterables.concat(valuesIterable, Collections.singletonList(escapeLiteral));
		}
		
		return valuesIterable;

	}

}
