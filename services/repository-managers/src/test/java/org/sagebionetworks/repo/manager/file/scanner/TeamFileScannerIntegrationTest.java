package org.sagebionetworks.repo.manager.file.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.IdRange;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TeamFileScannerIntegrationTest {
	
	@Autowired
	private FileHandleDao fileHandleDao;
			
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private DaoObjectHelper<Team> teamHelper;
	
	@Autowired
	private TeamDAO teamDao;
		
	@Autowired
	private FileHandleAssociationScannerTestUtils utils;
	
	@Autowired
	private FileHandleAssociationManager manager;
	
	private FileHandleAssociateType associationType = FileHandleAssociateType.TeamAttachment;
	
	private List<String> teamToDelete;
	
	private UserInfo user;
	
	@BeforeEach
	public void before() {
		teamToDelete = new ArrayList<>();
		
		fileHandleDao.truncateTable();
		
		user = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}
	
	@AfterEach
	public void after() {
		teamToDelete.forEach(teamDao::delete);
		fileHandleDao.truncateTable();
	}
	
	@Test
	public void testScanner() {
		
		Team t1 = createTeam(true);
		
		createTeam(false);
		
		Team t3 = createTeam(true);
		
		// Call under test
		IdRange range = manager.getIdRange(associationType);
		
		assertEquals(range.getMaxId(), Long.valueOf(t3.getId()));
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(
			new ScannedFileHandleAssociation(Long.valueOf(t1.getId()), Long.valueOf(t1.getIcon())),
			new ScannedFileHandleAssociation(Long.valueOf(t3.getId()), Long.valueOf(t3.getIcon()))
		);
				
		
		// Call under test
		List<ScannedFileHandleAssociation> result = StreamSupport.stream(
				manager.scanRange(associationType, new IdRange(Long.valueOf(t1.getId()), Long.valueOf(t3.getId()))
		).spliterator(), false).collect(Collectors.toList());
		
		assertEquals(expected, result);
		
	}
	
	private Team createTeam(boolean withIcon) {
		Team result = teamHelper.create(team -> {
			team.setName("TestTeamScanner_" + UUID.randomUUID().toString());
			team.setCreatedBy(user.getId().toString());
			
			if (withIcon) {
				team.setIcon(utils.generateFileHandle(user));
			}	
		});
		
		teamToDelete.add(result.getId());
		
		return result;
	}
}
