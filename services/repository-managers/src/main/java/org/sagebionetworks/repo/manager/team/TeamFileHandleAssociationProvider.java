package org.sagebionetworks.repo.manager.team;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_ICON;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTeam;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class TeamFileHandleAssociationProvider implements FileHandleAssociationProvider{

	private TeamDAO teamDao;
	private FileHandleAssociationScanner scanner;
	
	@Autowired
	public TeamFileHandleAssociationProvider(TeamDAO teamDao, NamedParameterJdbcTemplate jdbcTemplate) {
		this.teamDao = teamDao;
		this.scanner = new BasicFileHandleAssociationScanner(jdbcTemplate, new DBOTeam().getTableMapping(), COL_TEAM_ICON);
		
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

	@Override
	public FileHandleAssociationScanner getAssociationScanner() {
		return scanner;
	}

}
