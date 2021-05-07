package org.sagebionetworks.table.query.model;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Custom "HAS" and "HAS_LIKE" predicate for searching multi-value columns.
 *
 * <HAS predicate> ::= <row value constructor> [ NOT ] IN <in predicate value> |
 * <HAS_LIKE predicate> ::= <row value constructor> [ NOT ] IN <in predicate value> [<escape> <escape character>]
 *
 * Examples:
 * columnName HAS ("value1", "value2", "value3")
 * columnName HAS_LIKE ("value1") ESCAPE '_'
 *
 * See  https://sagebionetworks.jira.com/wiki/spaces/PLFM/pages/817168468/Multiple+Value+Annotations
 *
 * Related: {@link ArrayFunctionSpecification}
 *
 * NOTE the implemented {@link HasPredicate} interface is not for the "HAS" keyword, but, instead an interface for any predicate
 */
public class ArrayHasPredicate extends SQLElement implements HasPredicate {

	ColumnReference columnReferenceLHS;
	Boolean not;
	InPredicateValue inPredicateValue;
	ArrayHasLikeSpec hasLikeSpec;

	public ArrayHasPredicate(ColumnReference columnReferenceLHS, Boolean not, InPredicateValue inPredicateValue) {
		this(columnReferenceLHS, not, inPredicateValue, null);
	}
	
	public ArrayHasPredicate(ColumnReference columnReferenceLHS, Boolean not, InPredicateValue inPredicateValue, ArrayHasLikeSpec hasLikeSpec) {
		this.columnReferenceLHS = columnReferenceLHS;
		this.not = not;
		this.inPredicateValue = inPredicateValue;
		this.hasLikeSpec = hasLikeSpec;
	}

	public Boolean getNot() {
		return not;
	}
	
	public InPredicateValue getInPredicateValue() {
		return inPredicateValue;
	}
	
	public ArrayHasLikeSpec getHasLikeSpec() {
		return hasLikeSpec;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		columnReferenceLHS.toSql(builder, parameters);
		builder.append(" ");
		if (this.not != null) {
			builder.append("NOT ");
		}
		builder.append("HAS");
		if (this.hasLikeSpec != null) {
			builder.append("_LIKE");
		}
		builder.append(" ( ");
		inPredicateValue.toSql(builder, parameters);
		builder.append(" )");
		if (this.hasLikeSpec != null && this.hasLikeSpec.getEscapeCharacter() != null) {
			builder.append(" ESCAPE ");
			this.hasLikeSpec.getEscapeCharacter().toSql(builder, parameters);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, columnReferenceLHS);
		checkElement(elements, type, inPredicateValue);
		if (this.hasLikeSpec != null) {
			checkElement(elements, type, this.hasLikeSpec.getEscapeCharacter());
		}
	}

	@Override
	public ColumnReference getLeftHandSide() {
		return columnReferenceLHS;
	}

	@Override
	public Iterable<UnsignedLiteral> getRightHandSideValues() {
		Iterable<UnsignedLiteral> valuesIterable = inPredicateValue.createIterable(UnsignedLiteral.class);
		
		if (this.hasLikeSpec != null && this.hasLikeSpec.getEscapeCharacter() != null) {
			// The stream of values in the function
			Stream<UnsignedLiteral> valuesStream = StreamSupport.stream(valuesIterable.spliterator(), /*parallel*/ false);
			
			valuesIterable = Stream.concat(valuesStream, Stream.of(hasLikeSpec.getEscapeCharacter().getFirstElementOfType(UnsignedLiteral.class)))
					.collect(Collectors.toList());
		}
		
		return valuesIterable;
	}

}
