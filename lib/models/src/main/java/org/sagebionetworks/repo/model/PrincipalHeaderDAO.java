package org.sagebionetworks.repo.model;

import java.util.List;
import java.util.Set;

/**
 * Holds a mapping between principal IDs and various parts of their identification(s)
 */
public interface PrincipalHeaderDAO {
	
	/**
	 * Determines what criterion is used to query on the PrincipalHeader table
	 */
	public enum MATCH_TYPE {
		/**
		 * Matches on the prefix of the supplied filter
		 */
		PREFIX, 
		
		/**
		 * Matches on the supplied filter exactly
		 */
		EXACT, 
		
		/**
		 * Matches on the Soundex of the supplied filter
		 */
		SOUNDEX
	}

	/**
	 * Inserts a row into the PrincipalHeader table for each fragment
	 * All fields are required
	 */
	public void insertNew(long principalId, Set<String> fragments, PrincipalType pType);
	
	/**
	 * Deletes the ID to fragment mapping for the given principal
	 * 
	 * @return How many rows were deleted
	 */
	public long delete(long principalId);

	/**
	 * Performs a prefix search over the entries within the table
	 * 
	 * Note: There is no FK on UserGroup so not all results are guaranteed to have an associated UserGroup
	 *   See comments in DBOPrincipalHeader for more info.  
	 * 
	 * @param nameFilter The string to match.  If null/empty and the exactMatch=false, then all results will be returned
	 * @param mType How should the name filter be applied?  See {@link #MATCH_TYPE}
	 * @param principals The type(s) of principal to include.  If null or empty, all principals are included
	 */
	public List<Long> query(String nameFilter, MATCH_TYPE mType,
			Set<PrincipalType> principals, long limit, long offset);
	
	/**
	 * Returns the total number of results of the given query
	 * 
	 * See {@link #query(String, boolean, Set, Set, Set, long, long)}
	 */
	public long countQueryResults(String nameFilter, MATCH_TYPE mType,
			Set<PrincipalType> principals);
}
