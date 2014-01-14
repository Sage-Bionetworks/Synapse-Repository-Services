package org.sagebionetworks.repo.manager.migration;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.manager.principal.UserProfileUtillity;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.dao.UserProfileUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is a temporary listener that will ensure each profile is converted to an acceptable state.
 * @author jmhill
 *
 */
public class UserProfileMigrationTypeListener implements MigrationTypeListener {
	
	@Autowired
	PrincipalAliasDAO principalAliasDAO;

	@Autowired
	private DBOBasicDao basicDao;
	
	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(MigrationType type, List<D> delta) {
		if(MigrationType.USER_PROFILE.equals(type)){
			for(D dbo: delta){
				if(dbo instanceof DBOUserProfile){
					DBOUserProfile dboProfile = (DBOUserProfile) dbo;
					// We need to validate that the profile has all of the expected data
					UserProfile dtoProfile = UserProfileUtils.convertDboToDto(dboProfile);
					// Set the emails
					// We need to convert this profile
					List<PrincipalAlias> aliases = principalAliasDAO.listPrincipalAliases(dboProfile.getOwnerId(), AliasType.USER_EMAIL);
					if(aliases == null || aliases.isEmpty()) continue;
					List<String> emails = new LinkedList<String>();
					for(PrincipalAlias alias: aliases){
						emails.add(alias.getAlias());
					}
					dtoProfile.setEmails(emails);
					// clear the old email
					dtoProfile.setEmail(null);
					dtoProfile.setDisplayName(null);
					// Set the username
					dtoProfile.setUserName(UserProfileUtillity.createTempoaryUserName(dboProfile.getOwnerId()));
					// Convert it back
					DBOUserProfile newDbo = new DBOUserProfile();
					UserProfileUtils.copyDtoToDbo(dtoProfile, newDbo);
					// Save the new dbo
					basicDao.createOrUpdate(newDbo);
				}
			}
		}
	}

	@Override
	public void beforeDeleteBatch(MigrationType type, List<Long> idsToDelete) {
		// We have nothing to here for this case.
		
	}

}
