package org.sagebionetworks.table.query.model;

import java.util.Collections;

import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.util.SqlElementUtils;

/**
 * TextMatchesPredicate ::= <text_matches> <left_paren> {@link CharacterStringLiteral} <right_paren>
 */
public class TextMatchesPredicate extends SQLElement implements HasPredicate {
		
	public static final String KEYWORD = "TEXT_MATCHES";
	private static final PredicateLeftHandSide SEARCH_CONTENT_REF;
	
	static {
		try {
			SEARCH_CONTENT_REF = new PredicateLeftHandSide(new ColumnReference(null, SqlElementUtils.createColumnName(TableConstants.ROW_SEARCH_CONTENT), ColumnType.STRING));
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	private UnsignedLiteral searchExpression;

	public TextMatchesPredicate(CharacterStringLiteral searchExpression) {
		this.searchExpression = new UnsignedLiteral(new GeneralLiteral(searchExpression));
	}
	
	public UnsignedLiteral getSearchExpression() {
		return searchExpression;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(KEYWORD).append("(");
		searchExpression.toSql(builder, parameters);
		builder.append(")");
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(searchExpression);
	}

	@Override
	public PredicateLeftHandSide getLeftHandSide() {
		return SEARCH_CONTENT_REF;
	}
	
	@Override
	public Iterable<UnsignedLiteral> getRightHandSideValues() {
		return Collections.singleton(searchExpression);
	}

}
