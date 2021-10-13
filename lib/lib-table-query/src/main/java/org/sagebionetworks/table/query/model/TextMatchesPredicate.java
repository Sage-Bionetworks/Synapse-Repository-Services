package org.sagebionetworks.table.query.model;

import java.util.Collections;

import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.util.SqlElementUntils;

/**
 * TextMatchesPredicate ::= <text_matches> <left_paren> {@link CharacterStringLiteral} <right_paren>
 */
public class TextMatchesPredicate extends SQLElement implements HasPredicate {
		
	private static final String KEYWORD = "TEXT_MATCHES";
	private static final ColumnReference SEARCH_CONTENT_REF;
	
	static {
		try {
			SEARCH_CONTENT_REF = new ColumnReference(null, SqlElementUntils.createColumnName(TableConstants.ROW_SEARCH_CONTENT), ColumnType.STRING);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	private UnsignedLiteral valueExpression;

	public TextMatchesPredicate(CharacterStringLiteral valueExpression) {
		this.valueExpression = new UnsignedLiteral(new GeneralLiteral(valueExpression));
	}
	
	public UnsignedLiteral getValueExpression() {
		return valueExpression;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(KEYWORD).append("(");
		valueExpression.toSql(builder, parameters);
		builder.append(")");
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(valueExpression);
	}

	@Override
	public ColumnReference getLeftHandSide() {
		return SEARCH_CONTENT_REF;
	}
	
	@Override
	public Iterable<UnsignedLiteral> getRightHandSideValues() {
		return Collections.singleton(valueExpression);
	}

}
