package org.sagebionetworks.web.util;

import java.util.Comparator;
import java.util.Map;

/**
 * Compares two maps based on a single key
 * 
 * @author jmhill
 *
 */

@SuppressWarnings("rawtypes")
public class MapComarator implements Comparator<Map<String, Object>>{
	
	private String compareKey;


	public MapComarator(String compareKey){
		if(compareKey == null) throw new IllegalArgumentException("Compare key cannot be null");
		this.compareKey = compareKey;
	}


	@Override
	public int compare(Map<String, Object> o1, Map<String, Object> o2) {
		if (o1 == null) {
			if (o2 == null)
				return 0;
			else
				return -1;
		}
		if (o2 == null)
			return 1;
		
		Object one = o1.get(this.compareKey);
		Object two = o2.get(this.compareKey);
		if (one == null) {
			if (two == null)
				return 0;
			else
				return -1;
		}
		if (two == null)
			return 1;
		
		// At this point we have 2 non-null values to compare
		if(one.getClass().isArray()){
			return compareArrays((Comparable[])one, (Comparable[])two);
		}else{
			return ((Comparable) one).compareTo((Comparable) two);			
		}

	}
	
	private int compareArrays(Comparable[] one, Comparable[] two) {
		if (one == null) {
			if (two == null)
				return 0;
			else
				return -1;
		}
		if (two == null)
			return 1;
		// We have two non-null arrays
		if(one.length < two.length) return -1;
		if(one.length > two.length) return 1;
		// compare each value since they are the same length
		for(int i=0; i<one.length; i++){
			int sub = one[i].compareTo(two[i]);
			if(sub == 0) continue;
			else return sub;
		}
		// At this point they are equal
		return 0;
	}

}
