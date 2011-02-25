package org.sagebionetworks.web.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.sagebionetworks.web.shared.ColumnInfo;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.ColumnInfo.Type;

/**
 * Generates random data for each type of ColumnInfo
 * @author jmhill
 *
 */
public class RandomColumnData {
	
	/** 
	 * Using a fix seed for reproducibility. 
	 */
	private static Random random = new Random(123456);
	private static int numberWords = 2;
	private static int maxWordSize = 30;
	private static int maxArraySize = 4;

	/**
	 * Creates random values for each type.
	 * @param type
	 * @return
	 */
	public static Object createRandomValue(String typeString) {
		ColumnInfo.Type type = Type.valueOf(typeString);
		Object value = null;
		if(Type.String == type){
			value = RandomStrings.generateRandomString(numberWords, maxWordSize);
		}else if(Type.StringArray == type){
			int size = random.nextInt(maxArraySize);
			String[] arrayValue = new String[size];
			for(int i=0; i<size; i++){
				arrayValue[i] = RandomStrings.generateRandomString(numberWords, maxWordSize);
			}
			value = arrayValue;
		}else if(Type.Boolean == type){
			value = random.nextBoolean();
		}else if(Type.BooleanArray == type){
			int size = random.nextInt(maxArraySize);
			Boolean[] arrayValue = new Boolean[size];
			for(int i=0; i<size; i++){
				arrayValue[i] = random.nextBoolean();
			}
			value = arrayValue;
		}else if(Type.Long == type){
			value = random.nextLong();
		}else if(Type.LongArray == type){
			int size = random.nextInt(maxArraySize);
			Long[] arrayValue = new Long[size];
			for(int i=0; i<size; i++){
				arrayValue[i] = random.nextLong();
			}
			value = arrayValue;
		}else if(Type.Double == type){
			value = random.nextDouble();
		}else if(Type.DoubleArray == type){
			int size = random.nextInt(maxArraySize);
			Double[] arrayValue = new Double[size];
			for(int i=0; i<size; i++){
				arrayValue[i] = random.nextDouble();
			}
			value = arrayValue;
		}else if(Type.Integer == type){
			value = random.nextInt();
		}else if(Type.IntegerArray == type){
			int size = random.nextInt(maxArraySize);
			Integer[] arrayValue = new Integer[size];
			for(int i=0; i<size; i++){
				arrayValue[i] = random.nextInt();
			}
			value = arrayValue;
		}else {
			throw new IllegalArgumentException("Unknown type: "+typeString);
		}
		return value;
	}
	
	/**
	 * Create a list that contains one of every known column type.
	 * @return
	 */
	public static List<ColumnInfo> createListOfAllTypes(){
		// Create a list of rows that include every type
		List<ColumnInfo> allTypes = new ArrayList<ColumnInfo>();
		Type[] array = ColumnInfo.Type.values();
		for(int i=0; i<array.length; i++){
			ColumnInfo info = new ColumnInfo("id"+i, array[i].name(), "display"+i, "desc"+i);
			allTypes.add(info);
		}
		return allTypes;
	}
	
	/**
	 * Create a list of random rows using the provided column definitions.
	 * @return
	 */
	public static List<Map<String, Object>> createRandomRows(int numberRows, List<ColumnInfo> columns){
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
		// Create each row
		for(int i=0; i<numberRows; i++){
			Map<String, Object> row = new HashMap<String, Object>();
			rows.add(row);
			// Add a value for each column
			for(ColumnInfo column: columns){
				Object value = createRandomValue(column.getType());
				row.put(column.getId(), value);
			}
		}
		return rows;
	}
	
	/**
	 * Create a map of column headers to their id.
	 * @param list
	 * @return
	 */
	public static Map<String, HeaderData> createMap(List<ColumnInfo> list){
		Map<String, HeaderData> map = new HashMap<String, HeaderData>();
		if(list != null){
			for(ColumnInfo info: list){
				map.put(info.getId(), info);
			}			
		}
		return map;
	}

}
