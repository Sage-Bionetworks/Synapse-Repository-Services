package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.Realm;
import org.sagebionetworks.repo.model.auth.RealmIdList;
import org.sagebionetworks.repo.model.auth.RealmPrincipal;

public interface RealmManager {

	/**
	 * Create a new realm and a set of associated principals
	 * @param userInfo the identity of the user.  Only an admin' can make this request
	 * @param realm the client specifies the name and the list of identity providers.
	 *   The name must be unique, as must be each identity provider listed.
	 * @return
	 */
	Realm createRealm(UserInfo userInfo, Realm realm);
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	Realm getRealm(String id);
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	RealmPrincipal getRealmPrincipals(String id);
	
	
	/**
	 * List the IDS of the existing realms.
	 * @return
	 */
	RealmIdList listRealmIds();
	
	/**
	 * Delete a realm.
	 * @param userInfo the identity of the user.  Only an admin' can make this request
	 * @param realmId
	 */
	void deleteRealm(UserInfo userInfo, String realmId);
}
