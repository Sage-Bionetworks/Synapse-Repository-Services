package org.sagebionetworks.repo.model;

import java.util.Map;
import java.util.Optional;

import org.sagebionetworks.repo.model.auth.IdentityProvider;
import org.sagebionetworks.repo.model.auth.Realm;
import org.sagebionetworks.repo.model.auth.RealmIdList;
import org.sagebionetworks.repo.model.auth.RealmPrincipal;

public interface RealmDao {
	
	/**
	 * Create a new Realm and associated identity providers, ensuring the name and identity providers are unique.
	 * @param realm
	 * @return
	 */
	public Realm createRealm(Realm realm);
	
	/**
	 * Set the principals for the Realm
	 * 
	 * @param dto list of principals to associate with the realm
	 */
	public RealmPrincipal createRealmPrincipals(RealmPrincipal dto);
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	public Realm getRealm(String id);
	
	/**
	 * 
	 * @param identityProvider
	 * @return the ID of realm associated with the given identity provider, or null if there is none
	 */
	public Optional<String> getRealmIdForIdentityProvider(IdentityProvider identityProvider);
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	public RealmPrincipal getRealmPrincipals(String id);
	
	/**
	 * List the IDs of the existing realms.  Note, since the number of realms is small, this is not paginated.
	 * @return
	 */
	public RealmIdList listRealmIds();
	
	
	/**
	 * Disassociate the Realm's principals, a necessary step ahead of deleting the realm itself
	 * @param id
	 */
	public void deleteRealmPrincipals(String id);
	
	/**
	 * Delete the given realm
	 */
	public void deleteRealm(String id);
	
	/**
	 * Create default realm, with id 0
	 */
	public void bootstrapDefaultRealm();

	/**
	 * Add principals to default realm
	 */
	public void addPrincipalsToDefaultRealm(Map<String,Long> principalIdToRealmPrincipalDboId);
	
	/**
	 * 
	 * @param principalId
	 * @return the ID of the realm in which this principal is the Anonymous,
	 * or empty if the principalId is not the ID of the anonymous user in any realm.
	 */
	public Optional<String >getRealmForAnonymousPrincipal(String principalId);

}
