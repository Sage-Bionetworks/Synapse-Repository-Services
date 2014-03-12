package org.sagebionetworks.repo.web.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import org.sagebionetworks.repo.model.UserGroupHeader;

public class PrefixCacheHelper {

	/**
	 * The Trie contains collections of UserGroupHeaders for a given name. This
	 * method flattens the collections into a single list of UserGroupHeaders.
	 * 
	 * @param prefixMap
	 * @return
	 */
	public static <T> List<T> flatten (
			SortedMap<String, Collection<T>> prefixMap, Comparator<T> comparator) {
		//gather all unique UserGroupHeaders
		Set<T> set = new HashSet<T>();
		for (Collection<T> headersOfOneName : prefixMap.values()) {
			for (T header : headersOfOneName) {
				set.add(header);
			}
		}
		//put them in a list
		List<T> returnList = new ArrayList<T>();
		returnList.addAll(set);
		//return in a logical order
		Collections.sort(returnList, comparator);
		return returnList;
	}
	
	private static Comparator<UserGroupHeader> userGroupHeaderComparator = new Comparator<UserGroupHeader>() {
		@Override
		public int compare(UserGroupHeader o1, UserGroupHeader o2) {
			return o1.getOwnerId().compareTo(o2.getOwnerId());
		}
	};
	
	public static List<UserGroupHeader> flatten(SortedMap<String, Collection<UserGroupHeader>> prefixMap) {
		return PrefixCacheHelper.flatten(prefixMap, userGroupHeaderComparator);
	}

	/**
	 * Extract all of the prefixes out of the UserGroupHeader.
	 * 
	 * @param header
	 * @return
	 */
	public static List<String> getPrefixes(UserGroupHeader header){
		List<String> prefixes = new LinkedList<String>();
		if(header.getUserName() != null){
			prefixes.addAll(getPrefixes(header.getUserName()));
		}
		if(header.getFirstName() != null){
			prefixes.addAll(getPrefixes(header.getFirstName()));
		}
		if(header.getLastName() != null){
			prefixes.addAll(getPrefixes(header.getLastName()));
		}
		return prefixes;
	}
	
	public static List<String> getPrefixes(String s) {
		List<String> prefixes = new ArrayList<String>();
		String lowerCaseName = s.toLowerCase();
		String[] namePrefixes = lowerCaseName.split(" ");
		
		for (String namePrefix : namePrefixes) {
			prefixes.add(namePrefix);				
		}
		//if it was split, also include the entire name
		if (prefixes.size() > 1) {
			prefixes.add(lowerCaseName);
		}
		return prefixes;
	}
	


}
