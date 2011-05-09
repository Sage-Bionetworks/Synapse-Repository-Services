package org.sagebionetworks.web.server.servlet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.sagebionetworks.web.shared.ColumnInfo;
import org.sagebionetworks.web.shared.ColumnInfo.Type;
import org.sagebionetworks.web.shared.HeaderData;

/**
 * 
* Marshaling to/from JSON can result in a wide variety of types.  
* For example, a long might be marshaled to an Integer, Long, or BigInteger. 
* Therefore we convert it to the defined type for the columns when there is variation.
* 
 */
public class TypeValidation {
	
	private static Logger logger = Logger.getLogger(TypeValidation.class.getName());
	/**
	 * Creates a new list of rows based on the expected columns.
	 * Any column type not found in the expected columns will be filtered out.
	 * Any column that does not match the type of the expected column will be converted
	 * to the expected type.
	 * 
	 * @param rows
	 */
	public static List<Map<String, Object>> validateTypes(List<Map<String, Object>> rows, Map<String, HeaderData> expectedColumns) {
		 List<Map<String, Object>> results = new ArrayList<Map<String, Object>>(rows.size());
		if(rows != null){
			// If the object type does not match the column type then convert it.
			for(Map<String, Object> row: rows){
				// This is the new row
				Map<String, Object> newRow = new TreeMap<String, Object>();
				results.add(newRow);
				// Process each column.
				Iterator<String> keyIt = row.keySet().iterator();
				while(keyIt.hasNext()){
					String key = keyIt.next();
					//Filter out columns that are not expected
					HeaderData header = expectedColumns.get(key);
					if(header != null){
						// We only need to change the types of columnInfo objects
						if(header instanceof ColumnInfo){
							ColumnInfo column = (ColumnInfo) header;
							Object currentValue = row.get(key);
							try{
								Object convertedValue = convert(currentValue, column.fetchType());
								newRow.put(key, convertedValue);
							}catch(IllegalArgumentException e){
								logger.info("Failed to convert: "+key+" with value class: "+currentValue.getClass().getName()+" to "+column.getType());
							}
						}else{
							newRow.put(key, row.get(key));
						}
					}
				}
			}
		}
		return results;
	}

	/**
	 * 
	 * @param <T>
	 * @param currentValue
	 * @param type
	 * @return
	 */
	public static Object convert(Object currentValue, Type type) {
		if(type == null) throw new IllegalArgumentException("Type cannot be null");
		if(currentValue == null) return null;
		// If the value is a list then first we need to convert it to an array
		if(currentValue instanceof List){
			List list = (List) currentValue;
			// Convert to an array
			currentValue = list.toArray();
		}
		
		// First deal with arrays
		if(Type.LongArray == type){
			if(currentValue instanceof Long[]) return currentValue;
			return convertLongArray((Object[]) currentValue);
		}else if(Type.DoubleArray == type){
			if(currentValue instanceof Double[]) return currentValue;
			return convertDoubleArray((Object[]) currentValue);
		}else if(Type.IntegerArray == type){
			if(currentValue instanceof Integer[]) return currentValue;
			return convertIntegerArray((Object[]) currentValue);
		}else if(Type.BooleanArray == type){
			if(currentValue instanceof Boolean[]) return currentValue;
			return convertBooleanArray((Object[]) currentValue);
		}else if(Type.StringArray == type){
			if(currentValue instanceof String[]) return currentValue;
			return convertStringArray((Object[]) currentValue);
		}else if(Type.Long == type){
			if(currentValue instanceof Long) return  currentValue;
			else return Long.parseLong(currentValue.toString());
		}else if(Type.Integer == type){
			if(currentValue instanceof Integer) return  currentValue;
			else return Integer.parseInt(currentValue.toString());
		}else if(Type.String == type){
			if(currentValue instanceof String) return  currentValue;
			throw new IllegalArgumentException("Cannot convert from "+currentValue.getClass().getName()+" to String");
		}else if(Type.Double == type){
			if(currentValue instanceof Double) return  currentValue;
			else return Double.parseDouble(currentValue.toString());
		}else if(Type.Boolean == type){
			if(currentValue instanceof Boolean) return  currentValue;
			throw new IllegalArgumentException("Cannot convert from "+currentValue.getClass().getName()+" to Boolean");
		}else{
			throw new IllegalArgumentException("Unknown type: "+type.name());
		}
	}
	
	
	/**
	 * Convert an array of objects to Long[]
	 * @param currentArray
	 * @return
	 */
	public static Long[] convertLongArray(Object[] currentArray){
		Long[] converted = new Long[currentArray.length];
		for(int i=0; i<converted.length; i++){
			converted[i] = (Long) convert(currentArray[i], Type.Long);
		}
		return converted;
	}
	
	/**
	 * Convert an array of objects to Double[]
	 * @param currentArray
	 * @return
	 */
	public static Double[] convertDoubleArray(Object[] currentArray){
		Double[] converted = new Double[currentArray.length];
		for(int i=0; i<converted.length; i++){
			converted[i] = (Double) convert(currentArray[i], Type.Double);
		}
		return converted;
	}
	
	/**
	 * Convert an array of objects to Integer[]
	 * @param currentArray
	 * @return
	 */
	public static Integer[] convertIntegerArray(Object[] currentArray){
		Integer[] converted = new Integer[currentArray.length];
		for(int i=0; i<converted.length; i++){
			converted[i] = (Integer) convert(currentArray[i], Type.Integer);
		}
		return converted;
	}
	
	public static Boolean[] convertBooleanArray(Object[] currentArray){
		Boolean[] converted = new Boolean[currentArray.length];
		for(int i=0; i<converted.length; i++){
			converted[i] = (Boolean) convert(currentArray[i], Type.Boolean);
		}
		return converted;
	}
	
	public static String[] convertStringArray(Object[] currentArray){
		String[] converted = new String[currentArray.length];
		for(int i=0; i<converted.length; i++){
			converted[i] = (String) convert(currentArray[i], Type.String);
		}
		return converted;
	}

}
