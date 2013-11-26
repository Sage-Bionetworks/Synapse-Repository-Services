package org.sagebionetworks.repo.model;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.schema.adapter.JSONEntity;

public class PaginatedResultsUtil {
	/**
	 * Compute the sub-list indices corresponding to the given limit and offset
	 * @param listLength
	 * @param limit
	 * @param offset
	 * @return a pair of long for the start index (inclusive) and end index (exclusive)
	 */
	public static long[] getStartAndEnd(long listLength, long limit, long offset) {
		if (offset<0) throw new IllegalArgumentException("'offset' must be at least zero.");
		if (limit<0) throw new IllegalArgumentException("'limit' must be at least zero.");
		if (offset >= listLength)
			return new long[] { 0L, 0L };
		long start = offset;
		long end = offset + limit;
		if (end>listLength) end=listLength;
		return new long[] { start, end };
	}
	
	/**
	 * 
	 * @param list
	 * @param limit
	 * @param offset
	 * @return
	 */
	public static <T extends JSONEntity> PaginatedResults<T> createPaginatedResults(List<T> list, long limit, long offset) {
		List<T> paginatedList = prePaginate(list, limit, offset);
		return createPrePaginatedResults(paginatedList, list.size());
	}

	/**
	 * Limit the input list to the page
	 * 
	 * @param list all the objects
	 * @param limit the max number of objects
	 * @param offset the first object to return
	 * @return
	 */
	public static <T> List<T> prePaginate(List<T> list, long limit, long offset) {
		long[] startAndEnd = getStartAndEnd(list.size(), limit, offset);
		return list.subList((int) startAndEnd[0], (int) startAndEnd[1]);
	}

	/**
	 * Create a paginated results object
	 * 
	 * @param paginatedList one page of objects
	 * @param totalSize the total number of objects
	 * @return
	 */
	public static <T extends JSONEntity> PaginatedResults<T> createPrePaginatedResults(List<T> paginatedList, long totalSize) {
		return new PaginatedResults<T>(paginatedList, totalSize);
	}
}
