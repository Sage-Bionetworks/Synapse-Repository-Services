package org.sagebionetworks.repo.model.entity.query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Utilities for building EntityQueries.
 * 
 * @author John
 *
 */
public class EntityQueryUtils {

	/**
	 * Build a condition from parameters.
	 * @param name
	 * @param op
	 * @param values
	 * @return
	 */
	public static EntityFieldCondition buildCondition(EntityFieldName name, Operator op, Object...values){
		EntityFieldCondition condition = new EntityFieldCondition();
		condition.setLeftHandSide(name);
		condition.setOperator(op);
		condition.setRightHandSide(buildValue(values));
		return condition;
	}
	
	/**
	 * Build a list of values from an array.
	 * @param values
	 * @return
	 */
	public static List<Value> buildValue(Object...values){
		List<Value> result = new ArrayList<Value>(values.length);
		for(Object value: values){
			result.add(buildSingleValue(value));
		}
		return result;
	}
	
	/**
	 * Build a single value.
	 * @param value
	 * @return
	 */
	public static Value buildSingleValue(Object value){
		if(value instanceof String){
			StringValue sv = new StringValue();
			sv.setValue((String) value);
			return sv;
		}else if(value instanceof Date){
			DateValue dv = new DateValue();
			dv.setValue((Date) value);
			return dv;
		}else if(value instanceof Long){
			IntegerValue iv = new IntegerValue();
			iv.setValue((Long) value);
			return iv;
		}else{
			throw new IllegalArgumentException("Unknown value: "+value.getClass());
		}
	}

}
