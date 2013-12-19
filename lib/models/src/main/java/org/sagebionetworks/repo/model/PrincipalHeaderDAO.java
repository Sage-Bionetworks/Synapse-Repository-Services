package org.sagebionetworks.repo.model;

import java.util.List;
import java.util.Set;

/**
 * Holds a mapping between principal IDs and various parts of their identification(s)
 */
public interface PrincipalHeaderDAO {

	/**
	 * Inserts a row into the PrincipalHeader table for each fragment
	 * All fields are required
	 */
	public void insertNew(long principalId, Set<String> fragments, PrincipalType pType, DomainType dType);
	
	/**
	 * Deletes the ID to fragment mapping for the given principal
	 * 
	 * @return How many rows were deleted
	 */
	public long delete(long principalId);

	/**
	 * Performs a prefix search over the entries within the table
	 * 
	 * @param nameFilter The string to match.  If null/empty and the exactMatch=false, then all results will be returned
	 * @param exactMatch Should the result be an exact match?
	 * @param principals The type(s) of principal to include.  If null or empty, all principals are included
	 * @param domains The type(s) of domain to include.  If null or empty, all domains are included
	 */
	public List<Long> query(String nameFilter, boolean exactMatch,
			Set<PrincipalType> principals, Set<DomainType> domains, long limit,
			long offset);
	
	/**
	 * Returns the total number of results of the given query
	 * 
	 * See {@link #query(String, boolean, Set, Set, Set, long, long)}
	 */
	public long countQueryResults(String nameFilter, boolean exactMatch,
			Set<PrincipalType> principals, Set<DomainType> domains);
}
