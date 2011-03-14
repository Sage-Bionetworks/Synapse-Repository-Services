package org.sagebionetworks.web.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.shared.WhereCondition;


/**
 * Helper utils for lists of datasets.
 * @author jmhill
 *
 */
public class ListUtils {
	
	/**
	 * Make a sub-list form the passed list.
	 * @param <T>
	 * @param offset
	 * @param limit
	 * @param original
	 * @return
	 */
	public static <T> List<T> getSubList(int offset, int limit, List<T> original){
		// Make sure the offest is in range
		if(offset < 1){
			offset = 1;
		}
		int fromIndex = offset-1;
		if(fromIndex < 0 || fromIndex >= original.size()){
			// return an empty list.
			return new ArrayList<T>();
		}
		int toIndex = fromIndex + limit;
		if(toIndex >= original.size()){
			toIndex = original.size()-1;
		}
		if(fromIndex >= toIndex){
			// return an empty list.
			return new ArrayList<T>();
		}
		// Create a sub-list from the 
		return original.subList(fromIndex, toIndex);
	}
	
	/**
	 * Creates a sorted copy of the passed datasets list based on the given column.
	 * @param <T>
	 * @param sortColumn
	 * @param ascending
	 * @param original
	 * @return
	 */
	public static <T> List<T> getSortedCopy(String sortColumn, boolean ascending, List<T> original, Class clazz){
		// First make a copy
		List<T> copy = new ArrayList<T>(original.size());
		copy.addAll(original);
		// We are done if the sort column is null.
		if(sortColumn == null) return copy;
		// Create the comparator
		Comparator<T> comparator = null;
		if(clazz == Map.class){
			comparator = (Comparator<T>) new MapComarator(sortColumn);
		}else{
			comparator = new FieldComparator<T>(clazz, sortColumn);
		}
		Collections.sort(copy, comparator);
		if(!ascending){
			// Flip if needed
			Collections.reverse(copy);			
		}
		return copy;
	}
	
	/**
	 * Filter all rows that meet the passed condition.
	 * @param condition
	 * @param original
	 * @return
	 */
	public static List<Map<String, Object>> getFilteredCopy(WhereCondition condition, List<Map<String, Object>> original){
		// First make a copy
		List<Map<String, Object>> filtered = new ArrayList<Map<String,Object>>();
		// Go through each row and look for matches to the condition
		for(int i=0; i<original.size(); i++){
			Map<String, Object> row = original.get(i);
			// First does the row have the value
			Object value = row.get(condition.getId());
			if(value != null){
				if(value instanceof Object[]){
					value = ((Object[])value)[0];
				}
				// only equals for now
				if(value.equals(condition.getValue())){
					filtered.add(row);
				}
			}
		}
		return filtered;
	}
	

}
