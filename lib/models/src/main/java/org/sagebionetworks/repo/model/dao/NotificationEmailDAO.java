package org.sagebionetworks.repo.model.dao;

import org.sagebionetworks.repo.model.principal.PrincipalAlias;

/**
 * 
 * Interface for managing the email alias chosen for notification.
 * 
 * @author brucehoff
 *
 */
public interface NotificationEmailDAO {
	
	/**
	 * 
	 * @param principalAlias the AliasType must be AliasType.USER_EMAIL
	 */
	public void create(PrincipalAlias principalAlias);
	
	/**
	 * 
	 * @param principalAlias the AliasType must be AliasType.USER_EMAIL
	 */
	public void update(PrincipalAlias principalAlias);
	
	/**
	 * 
	 * @param principalId
	 * @return the PrincipalAlias which is the chosen notification email for the given principal.
	 * The AliasType is guaranteed to be AliasType.USER_EMAIL
	 */
	public String getNotificationEmailForPrincipal(long principalId);

}
