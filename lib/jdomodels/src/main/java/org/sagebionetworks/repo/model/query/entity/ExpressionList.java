package org.sagebionetworks.repo.model.query.entity;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.query.Expression;

public class ExpressionList extends SqlElement implements HasAnnotationReference {
	
	List<SqlExpression> expressions;

	/**
	 * Create a new expression list from the given filters
	 * @param filters
	 */
	public ExpressionList(List<Expression> filters, IndexProvider indexProvide){
		expressions = new LinkedList<SqlExpression>();
		if(filters != null){
			for(Expression expression: filters){
				// Try to create a left-hand-side
				String fieldName = expression.getId().getFieldName();
				ColumnReference lhs = new ColumnReference(fieldName, indexProvide.nextIndex());;
				SqlExpression sqlExpression = new SqlExpression(
						lhs,
						expression.getCompare(),
						expression.getValue());
				expressions.add(sqlExpression);
			}
		}
	}

	@Override
	public void toSql(StringBuilder builder) {
		if(!expressions.isEmpty()){
			builder.append(" WHERE ");
			boolean first = true;
			for(SqlExpression expression: expressions){
				if(!first){
					builder.append(" AND ");
				}
				expression.toSql(builder);
				first = false;
			}
		}
	}
	
	@Override
	public void bindParameters(Parameters parameters) {
		for(SqlExpression exp: expressions){
			exp.bindParameters(parameters);
		}
	}

	/**
	 * Get all annotation references in this list.
	 */
	@Override
	public List<ColumnReference> getAnnotationReferences() {
		List<ColumnReference> annos = new LinkedList<ColumnReference>();
		for(SqlExpression exp: expressions){
			if(exp.leftHandSide.getAnnotationAlias() != null){
				annos.add(exp.leftHandSide);
			}
		}
		return annos;
	}
	
	/**
	 * Get the expressions.
	 * @return
	 */
	public List<SqlExpression> getExpressions(){
		return expressions;
	}

}
