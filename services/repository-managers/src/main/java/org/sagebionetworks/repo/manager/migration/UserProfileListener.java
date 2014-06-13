package org.sagebionetworks.repo.manager.migration;

import java.util.List;

import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.dao.UserProfileUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This listener ensures that usernames are represented as aliases and
 * not as entries in UserProfiles
 * @author brucehoff
 *
 */
public class UserProfileListener implements MigrationTypeListener {
	
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	
	@Autowired
	private UserProfileDAO userProfileDAO;
	
	// for testing
	public UserProfileListener(PrincipalAliasDAO principalAliasDAO, UserProfileDAO userProfileDAO) {
		this.principalAliasDAO = principalAliasDAO;
		this.userProfileDAO = userProfileDAO;
	}

	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(
			MigrationType type, List<D> delta) {
		if (!type.equals(MigrationType.USER_PROFILE)) return;
		for (D item : delta) {
			if (!(item instanceof DBOUserProfile)) continue;
			DBOUserProfile dbo = (DBOUserProfile)item;
			long principalId = dbo.getOwnerId();
			UserProfile profile = UserProfileUtils.deserialize(dbo.getProperties());
			String userNameFromProfile = profile.getUserName();
			if (userNameFromProfile==null || userNameFromProfile.length()==0) continue; // nothing to do
			List<PrincipalAlias> aliases = principalAliasDAO.listPrincipalAliases(principalId, AliasType.USER_NAME);
			if (aliases.size()>1) throw new IllegalStateException("Expected 0-1 but found "+aliases.size()+
					" user names for "+principalId);
			if (aliases.size()==1) {
				String userNameFromAlias = aliases.get(0).getAlias();
				if (!userNameFromAlias.equals(userNameFromProfile)) 
					throw new IllegalStateException("For user "+principalId+" user profile has user name "+userNameFromProfile+
							" but PrincipalAlias table has "+userNameFromAlias);
			} else { // aliases.size()==0
				PrincipalAlias alias = new PrincipalAlias();
				alias.setAlias(userNameFromProfile);
				alias.setPrincipalId(principalId);
				alias.setType(AliasType.USER_NAME);
				try {
					principalAliasDAO.bindAliasToPrincipal(alias);
				} catch (NotFoundException e) {
					throw new RuntimeException(e);
				}
			}
			
			// finally, strip the field out of the user profile
			profile.setUserName(null);
			try {
				userProfileDAO.update(profile);
			} catch (NotFoundException e) {
				throw new RuntimeException(e);
			}
 		}

	}

	@Override
	public void beforeDeleteBatch(MigrationType type, List<Long> idsToDelete) {
		// Nothing to do
	}

}
