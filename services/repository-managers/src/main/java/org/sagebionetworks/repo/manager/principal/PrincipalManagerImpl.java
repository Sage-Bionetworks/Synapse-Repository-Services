package org.sagebionetworks.repo.manager.principal;

import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.Username;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.principal.AliasEnum;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Basic implementation of the PrincipalManager.
 * @author John
 *
 */
public class PrincipalManagerImpl implements PrincipalManager {
	
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;

	@Autowired
	private NotificationEmailDAO notificationEmailDao;

	@Override
	public boolean isAliasAvailable(String alias) {
		if(alias == null) throw new IllegalArgumentException("Alias cannot be null");
		return this.principalAliasDAO.isAliasAvailable(alias);
	}

	@Override
	public boolean isAliasValid(String alias, AliasType type) {
		if(alias == null) throw new IllegalArgumentException("Alias cannot be null");
		if(type == null) throw new IllegalArgumentException("AliasType cannot be null");
		// Check the value
		try {
			AliasEnum.valueOf(type.name()).validateAlias(alias);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
	
	/**
	 * Set the email which is used for notification.
	 * 
	 * @param principalId
	 * @param email
	 * @throws NotFoundException 
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void setNotificationEmail(UserInfo userInfo, String email) throws NotFoundException {
		List<PrincipalAlias> aliases = principalAliasDAO.listPrincipalAliases(userInfo.getId(), AliasType.USER_EMAIL);
		for (PrincipalAlias principalAlias : aliases) {
			if (!principalAlias.getAlias().equals(email)) continue;
			notificationEmailDao.update(principalAlias);
			return;
		}
		throw new NotFoundException("User ID "+userInfo.getId()+" does not own the email address: "+email);
	}
	
	/**
	 * Get the email which is used for notification.
	 * 
	 * @param userInfo
	 * @return
	 * @throws NotFoundException
	 */
	@Override
	public Username getNotificationEmail(UserInfo userInfo) throws NotFoundException {
		String email= notificationEmailDao.getNotificationEmailForPrincipal(userInfo.getId());
		Username dto = new Username();
		dto.setEmail(email);
		return dto;
	}

}
