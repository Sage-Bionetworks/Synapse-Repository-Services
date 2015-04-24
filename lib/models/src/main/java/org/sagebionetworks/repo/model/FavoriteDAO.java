package org.sagebionetworks.repo.model;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.web.NotFoundException;


public interface FavoriteDAO {

	/**
	 * Adds an entity id to the ownerId's favorites list
	 * @param dto
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public Favorite add(Favorite dto) throws DatastoreException, InvalidModelException;
	
	/**
	 * Removes the specified entity id from the ownerId's favotires list, if exists
	 * @param principalId
	 * @param entityId
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public void remove(String principalId, String entityId) throws DatastoreException;
		
	/**
	 * Retrieve users list of favorites, paginated
	 * @param principalId
	 * @param limit
	 * @param offset
	 * @return
	 */
	public PaginatedResults<Favorite> getFavorites(String principalId, int limit, int offset) throws DatastoreException, InvalidModelException, NotFoundException;

	/**
	 * Retrieve users list of favorites, paginated
	 * @param principalId
	 * @param limit
	 * @param offset
	 * @return
	 */
	public PaginatedResults<EntityHeader> getFavoritesEntityHeader(String principalId, int limit, int offset) throws DatastoreException, InvalidModelException, NotFoundException;

	/**
	 * For backup purposes
	 * @param principalId
	 * @param entityId
	 * @return
	 */
	public Favorite getIndividualFavorite(String principalId, String entityId) throws DatastoreException, InvalidModelException, NotFoundException;

	long getCount() throws DatastoreException;

}
