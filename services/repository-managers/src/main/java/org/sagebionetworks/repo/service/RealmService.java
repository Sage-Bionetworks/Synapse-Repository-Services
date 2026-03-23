package org.sagebionetworks.repo.service;

import org.sagebionetworks.repo.model.auth.Realm;
import org.sagebionetworks.repo.model.auth.RealmIdList;
import org.sagebionetworks.repo.model.auth.RealmPrincipal;

/**
 * Generic service class to support controllers accessing Realms.
 *
 */
public interface RealmService {
	/**
	 * 
	 * @return
	 */
	public RealmIdList listRealmIds();

	/**
	 * 
	 * @param realmId
	 * @return
	 */
	public Realm getRealm(String realmId);
	
	/**
	 * 
	 * @param realmId
	 * @return
	 */
	public RealmPrincipal getRealmPrincipals(String realmId);
	
	/**
	 * 
	 * @param userId
	 * @return
	 */
	public RealmPrincipal getRealmPrincipals(Long userId);
}
