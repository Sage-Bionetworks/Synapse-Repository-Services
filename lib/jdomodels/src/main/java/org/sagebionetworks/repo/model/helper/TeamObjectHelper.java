package org.sagebionetworks.repo.model.helper;

import java.util.function.Consumer;

import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TeamObjectHelper implements DaoObjectHelper<Team> {
	
	private UserGroupDoaObjectHelper userGroupHelper;
	private TeamDAO teamDao;

	@Autowired
	public TeamObjectHelper(UserGroupDoaObjectHelper userGroupHelper, TeamDAO teamDao) {
		this.userGroupHelper = userGroupHelper;
		this.teamDao = teamDao;
	}
	
	@Override
	public Team create(Consumer<Team> consumer) {
		
		UserGroup ug = userGroupHelper.create((g) -> {
			g.setIsIndividual(false);
		});
		
		Team team = new Team();
		team.setId(ug.getId());
		team.setName("TestTeam");
		team.setCreatedBy("123");
		
		consumer.accept(team);
		
		return teamDao.create(team);
	}

}
