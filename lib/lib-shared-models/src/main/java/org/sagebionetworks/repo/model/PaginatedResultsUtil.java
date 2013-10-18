package org.sagebionetworks.repo.model;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.schema.adapter.JSONEntity;

public class PaginatedResultsUtil {
	/**
	 * Compute the sub-list indices corresponding to the given limit and offset
	 * @param listLength
	 * @param limit
	 * @param offset
	 * @return a pair of long for the start index (inclusive) and end index (exclusive)
	 */
	public static Long[] getStartAndEnd(long listLength, long limit, long offset) {
		if (offset<0) throw new IllegalArgumentException("'offset' must be at least zero.");
		if (limit<0) throw new IllegalArgumentException("'limit' must be at least zero.");
		if (offset>=listLength) return new Long[]{0L,0L};
		Long start = offset;
		Long end = offset+limit;
		if (end>listLength) end=listLength;
		return new Long[]{start, end};
	}
	
	/**
	 * 
	 * @param list
	 * @param limit
	 * @param offset
	 * @return
	 */
	public static <T extends JSONEntity> PaginatedResults<T> createPaginatedResults(List<T> list, long limit, long offset) {
		PaginatedResults<T> pr = new PaginatedResults<T>();
		Long[] startAndEnd = getStartAndEnd(list.size(), limit, offset);
		pr.setResults(list.subList(startAndEnd[0].intValue(), startAndEnd[1].intValue()));
		pr.setTotalNumberOfResults(list.size());
		return pr;
	}
}
