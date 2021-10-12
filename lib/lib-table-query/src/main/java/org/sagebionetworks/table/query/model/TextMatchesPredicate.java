package org.sagebionetworks.table.query.model;

import java.util.Collections;

import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.util.SqlElementUntils;

/**
 * TextMatchesPredicate ::= <text_matches> <left_paren> {@link CharacterStringLiteral} <right_paren>
 *
 */
public class TextMatchesPredicate extends SQLElement implements HasPredicate {
	
	private static final ColumnReference SEARCH_COL_REFERENCE;
	
	static {
		try {
			SEARCH_COL_REFERENCE = SqlElementUntils.createColumnReference(TableConstants.ROW_SEARCH_CONTENT);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	private UnsignedLiteral valueExpression;

	public TextMatchesPredicate(CharacterStringLiteral valueExpression) {
		this.valueExpression = new UnsignedLiteral(new GeneralLiteral(valueExpression));
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("MATCH(").append(TableConstants.ROW_SEARCH_CONTENT).append(") AGAINST(");
		valueExpression.toSql(builder, parameters);
		builder.append(")");
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(valueExpression);
	}

	@Override
	public ColumnReference getLeftHandSide() {
		return SEARCH_COL_REFERENCE;
	}

	@Override
	public Iterable<UnsignedLiteral> getRightHandSideValues() {
		return Collections.singleton(valueExpression);
	}

}
