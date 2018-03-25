package org.sagebionetworks.table.query.model;

import java.util.LinkedList;
import java.util.List;

/**
 * 
 * MySqlFunction ::= {@link MySqlFunctionName} [ left_paren [ {@link ValueExpression} ( comma {@link ValueExpression} )* ] right_paren ]
 *
 */
public class MySqlFunction extends SQLElement implements HasFunctionReturnType {
	
	MySqlFunctionName functionName;
	List<ValueExpression> parameterValues;
	
	public MySqlFunction(MySqlFunctionName functionName) {
		this.functionName = functionName;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(functionName.name());
		// Not all functions have parentheses.
		if(parameterValues != null){
			builder.append("(");
			boolean first = true;
			for(ValueExpression param: parameterValues){
				if(!first){
					builder.append(",");
				}
				param.toSql(builder, parameters);
				first = false;
			}
			builder.append(")");
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		if(parameterValues != null){
			for(ValueExpression param: parameterValues){
				checkElement(elements, type, param);
			}
		}
	}

	/**
	 * Get the Function name for this element.
	 * 
	 * @return
	 */
	public MySqlFunctionName getFunctionName(){
		return functionName;
	}

	public void startParentheses() {
		parameterValues = new LinkedList<ValueExpression>();
	}

	/**
	 * Add a parameter to this function.
	 * @param valueExpression
	 */
	public void addParameter(ValueExpression valueExpression) {
		this.parameterValues.add(valueExpression);
	}

	@Override
	public FunctionReturnType getFunctionReturnType() {
		if(functionName == null) {
			return null;
		}else {
			return functionName.getFunctionReturnType();
		}
	}
}
