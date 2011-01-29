package org.sagebionetworks.web.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sagebionetworks.web.shared.Dataset;


/**
 * Helper utils for lists of datasets.
 * @author jmhill
 *
 */
public class ListUtils {
	
	/**
	 * Make a sub-list form the passed list.
	 * @param offset
	 * @param limit
	 * @param original
	 * @return
	 */
	public static List<Dataset> getSubList(int offset, int limit, List<Dataset> original){
		// Make sure the offest is in range
		if(offset < 1){
			offset = 1;
		}
		int fromIndex = offset-1;
		if(fromIndex < 0 || fromIndex >= original.size()){
			// return an empty list.
			return new ArrayList<Dataset>();
		}
		int toIndex = fromIndex + limit;
		if(toIndex >= original.size()){
			toIndex = original.size()-1;
		}
		if(fromIndex >= toIndex){
			// return an empty list.
			return new ArrayList<Dataset>();
		}
		// Create a sub-list from the 
		return original.subList(fromIndex, toIndex);
	}
	
	/**
	 * Creates a sorted copy of the passed datasets list based on the given column.
	 * @param sortColumn
	 * @param ascending
	 * @param original
	 * @return
	 */
	public static List<Dataset> getSortedCopy(String sortColumn, boolean ascending, List<Dataset> original){
		// First make a copy
		List<Dataset> copy = new ArrayList<Dataset>(original.size());
		copy.addAll(original);
		// We are done if the sort column is null.
		if(sortColumn == null) return copy;
		// Create the comparator
		FieldComparator<Dataset> comparator = new FieldComparator<Dataset>(Dataset.class, sortColumn);
		Collections.sort(copy, comparator);
		if(!ascending){
			// Flip if needed
			Collections.reverse(copy);			
		}
		return copy;
	}

}
