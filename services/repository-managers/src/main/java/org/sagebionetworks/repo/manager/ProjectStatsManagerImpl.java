package org.sagebionetworks.repo.manager;

import com.google.common.collect.Sets;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ProjectStat;
import org.sagebionetworks.repo.model.ProjectStatsDAO;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

public class ProjectStatsManagerImpl implements ProjectStatsManager {

	@Autowired
	ProjectStatsDAO projectStatDao;
	@Autowired
	AuthorizationManager authorizationManager;
	@Autowired
	NodeDAO nodeDao;
	@Autowired
	V2WikiPageDao v2wikiPageDao;
	@Autowired
	GroupMembersDAO groupMemberDao;
	@Autowired
	UserGroupDAO userGroupDao;
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.ProjectStatsManager#getProjectForObject(java.lang.String, org.sagebionetworks.repo.model.ObjectType)
	 */
	@Override
	public Optional<String> getProjectForObject(String objectId, ObjectType objectType) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
			switch (objectType) {
			case ENTITY:
			case TABLE:
				return nodeDao.getProjectId(objectId);
			case WIKI:
				// Lookup the owner id and type for this wiki.
				WikiPageKey key = v2wikiPageDao.lookupWikiKey(objectId);
				// lookup the project of the owner.
				return getProjectForObject(key.getOwnerObjectId(), key.getOwnerObjectType());
			default:
				return Optional.empty();
			}
	}


	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.ProjectStatsManager#updateProjectStats(java.lang.Long, java.lang.String, org.sagebionetworks.repo.model.ObjectType, java.util.Date)
	 */
	@WriteTransaction
	@Override
	public void updateProjectStats(Long principalId, String objectId,
			ObjectType objectType, Date activityDate) {
		ValidateArgument.required(principalId, "userId");
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(objectType, "objectType");
		
		// Lookup the projectId for this object
		Optional<String> projectIdString = getProjectForObject(objectId, objectType);
		if(projectIdString.isPresent()){
			long projectId = KeyFactory.stringToKey(projectIdString.get());
			if(userGroupDao.isIndividual(principalId)){
				// user.
				projectStatDao.updateProjectStat(new ProjectStat(projectId, principalId, activityDate));
			}else{
				// team so update for each member
				Set<Long> memberIds = groupMemberDao.getMemberIds(principalId);
				if(!memberIds.isEmpty()){
					ProjectStat[] update = new ProjectStat[memberIds.size()];
					int index = 0;
					for(Long memberId: memberIds){
						update[index] = new ProjectStat(projectId, memberId, activityDate);
						index++;
					}
					// batch update
					projectStatDao.updateProjectStat(update);
				}
			}
		}
	}


	@WriteTransaction
	@Override
	public void memberAddedToTeam(Long teamId, Long memberId, Date activityDate) {
		ValidateArgument.required(teamId, "teamId");
		ValidateArgument.required(memberId, "memberId");
		// Lookup all projects that are visible by this team
		Set<Long> visibleProjectIds = authorizationManager.getAccessibleProjectIds(Sets.newHashSet(teamId));
		if(!visibleProjectIds.isEmpty()){
			ProjectStat[] update = new ProjectStat[visibleProjectIds.size()];
			int index = 0;
			for(Long projectId: visibleProjectIds){
				// Bump this user's stats for this project
				update[index] = new ProjectStat(projectId, memberId, activityDate);
				index++;
			}
			// batch update
			projectStatDao.updateProjectStat(update);
		}
	}

}
