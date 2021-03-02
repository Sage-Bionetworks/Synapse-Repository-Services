package org.sagebionetworks.repo.manager.team;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TeamFileHandleAssociationProvider implements FileHandleAssociationProvider{

	private TeamDAO teamDao;
	
	@Autowired
	public TeamFileHandleAssociationProvider(TeamDAO teamDao) {
		this.teamDao = teamDao;
	}
	
	@Override
	public FileHandleAssociateType getAssociateType() {
		return FileHandleAssociateType.TeamAttachment;
	}

	@Override
	public Set<String> getFileHandleIdsDirectlyAssociatedWithObject(List<String> fileHandleIds, String objectId) {
		Set<String> associatedIds = new HashSet<String>();
		try {
			Team team = teamDao.get(objectId);
			if (team.getIcon() != null && fileHandleIds.contains(team.getIcon())) {
				associatedIds.add(team.getIcon());
			}
		} catch (NotFoundException e){
			//The team does not exist
		}
		return associatedIds;
	}

	@Override
	public ObjectType getAuthorizationObjectTypeForAssociatedObjectType() {
		return ObjectType.TEAM;
	}

}
