package org.sagebionetworks.repo.model.query.entity;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.query.Expression;

public class ExpressionList extends SqlElement {
	
	List<SqlExpression> expressions;

	/**
	 * Create a new expression list from the given filters
	 * @param filters
	 */
	public ExpressionList(List<Expression> filters){
		expressions = new LinkedList<SqlExpression>();
		if(filters != null){
			int index = 0;
			for(Expression expression: filters){
				// Try to create a left-hand-side
				String fieldName = expression.getId().getFieldName();
				ColumnReference lhs = new ColumnReference(fieldName, index);;
				SqlExpression sqlExpression = new SqlExpression(
						lhs,
						expression.getCompare(),
						expression.getValue());
				expressions.add(sqlExpression);
				index++;
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
	
	/**
	 * Get the sub-set of expression that represent annotations.
	 * 
	 * @return
	 */
	public List<SqlExpression> getAnnotationExpressions(){
		List<SqlExpression> annos = new LinkedList<SqlExpression>();
		for(SqlExpression exp: expressions){
			if(exp.leftHandSide.getAnnotationAlias() != null){
				annos.add(exp);
			}
		}
		return annos;
	}
	
	/**
	 * Get the number of expressions.
	 * @return
	 */
	public int getSize(){
		return expressions.size();
	}

	@Override
	public void bindParameters(Parameters parameters) {
		for(SqlExpression exp: expressions){
			exp.bindParameters(parameters);
		}
	}

}
