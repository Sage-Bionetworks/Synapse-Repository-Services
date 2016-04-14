package org.sagebionetworks.repo.manager;

import java.util.Date;

import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ProjectStat;
import org.sagebionetworks.repo.model.ProjectStatsDAO;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class ProjectStatsManagerImpl implements ProjectStatsManager {

	@Autowired
	ProjectStatsDAO projectStatDao;
	
	@Autowired
	NodeDAO nodeDao;
	
	@Autowired
	V2WikiPageDao v2wikiPageDao;
	

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.ProjectStatsManager#getProjectForObject(java.lang.String, org.sagebionetworks.repo.model.ObjectType)
	 */
	@Override
	public String getProjectForObject(String objectId, ObjectType objectType) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		try {
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
				return null;
			}
		} catch (NotFoundException e) {
			// return null for not found
			return null;
		}
	}


	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.ProjectStatsManager#updateProjectStats(java.lang.Long, java.lang.String, org.sagebionetworks.repo.model.ObjectType, java.util.Date)
	 */
	@Override
	public void updateProjectStats(Long userId, String objectId,
			ObjectType objectType, Date activityDate) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(objectType, "objectType");
		
		// Lookup the projectId for this object
		String projectIdString = getProjectForObject(objectId, objectType);
		if(projectIdString != null){
			long projectId = KeyFactory.stringToKey(projectIdString);
			projectStatDao.update(new ProjectStat(projectId, userId, activityDate));
		}
	}

}
