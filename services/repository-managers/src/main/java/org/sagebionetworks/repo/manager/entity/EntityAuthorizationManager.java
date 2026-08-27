package org.sagebionetworks.repo.manager.entity;

import java.util.List;

import org.sagebionetworks.repo.manager.entity.decider.UsersEntityAccessInfo;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.file.download.v2.FileActionRequired;
import org.sagebionetworks.repo.web.NotFoundException;

public interface EntityAuthorizationManager {

	/**
	 * Determine if the user is authorized to access the given entity. Each access
	 * type will be tested in the order provided. The first 'access-denied'
	 * encountered will be returned. An 'authorized' status will only be returned if
	 * the user has access to each of the provided types.
	 * 
	 * This method should not throw any exceptions, instead, if there is an error
	 * the resulting AuthorizationStatus will contain the error message/exception.
	 * 
	 * @param userInfo
	 * @param entityId
	 * @param accessType The permission/permissions check to be checked against the
	 *                   entity.
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	AuthorizationStatus hasAccess(UserInfo userInfo, String entityId, ACCESS_TYPE...accessType);

	/**
	 * Determine if the user is authorized access the given batch of entityIds. This
	 * method should not throw any exceptions, instead, if there is an error with
	 * any individual entity, the results for that entity will include the error.
	 * 
	 * @param userInfo
	 * @param entityIds  The batch of entity IDs.
	 * @param accessType The permission check to be checked against each entity in
	 *                   the batch.
	 * @return
	 */
	List<UsersEntityAccessInfo> batchHasAccess(UserInfo userInfo, List<Long> entityIds, ACCESS_TYPE accessType);

	/**
	 * Get a bundle of all of the permission that the user has on a single entity.
	 * 
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo, String entityId)
			throws NotFoundException, DatastoreException;
	

	/**
	 * Get a bundle of all the permission that the user has on a single entity.
	 * @param userInfo
	 * @param entityId
	 * @param stateProvider
	 * @return
	 */
	UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo, String entityId, EntityStateProvider stateProvider);
	
	/**
	 * Can the user create an entity within the given parent and of the given entity type?
	 * @param parentId
	 * @param entityCreateType
	 * @param userInfo
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	AuthorizationStatus canCreate(String parentId, EntityType entityCreateType, UserInfo userInfo) throws DatastoreException, NotFoundException;

	/**
	 * Can the user delete the ACL on the given entity?
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	AuthorizationStatus canDeleteACL(UserInfo userInfo, String entityId);

	/**
	 * Can the user create a wiki for the given entity?  The user must have the CREATE permission
	 * on the entity.  In addition, the user must be certified to create a wiki on any entity that
	 * is not a project.
	 * @param entityId
	 * @param userInfo
	 * @return
	 */
	AuthorizationStatus canCreateWiki(String entityId, UserInfo userInfo);

	/**
	 * For the given batch of entity ids create a list of actions that
	 * the user will need to take in order to download any file that they are
	 * currently not authorized to download.
	 *
	 * @param userInfo
	 * @param entityIds
	 * @return
	 */
	List<FileActionRequired> getActionsRequiredForDownload(UserInfo userInfo, List<Long> entityIds);

	/**
	 * Determine if the user can query the given table/view together with all of its
	 * dependencies. Every node requires READ; a {@link TableType#table} or
	 * {@link TableType#recordset} node additionally requires DOWNLOAD to read
	 * row-level data. The caller is expected to supply the queried table/view first,
	 * followed by every table/view it depends on (transitively).
	 * <p>
	 * The result is the combined status over the whole set:
	 * <ul>
	 * <li>{@link AuthorizationStatus#isAuthorized()} — the user may read row-level
	 * data from every node.</li>
	 * <li>Denied but {@link AuthorizationStatus#isAggregateAccessAllowed()} — the
	 * user is blocked from row-level data only because at least one node is bound to
	 * {@code AGGREGATE_DATA} where they hold DOWNLOAD but have unmet access
	 * restrictions; they remain eligible for a gated aggregate-only read (see Cohort
	 * Builder).</li>
	 * <li>Denied and not aggregate-allowed — the user cannot query the table/view at
	 * all.</li>
	 * </ul>
	 *
	 * @param userInfo
	 * @param tableAndDependencies The queried table/view and all of its dependencies.
	 * @return
	 */
	AuthorizationStatus canQueryTableOrView(UserInfo userInfo, List<TableIdAndType> tableAndDependencies);

	public record TableIdAndType(String tableId, TableType type) {
	};

}
