package org.sagebionetworks.dataaccess.workers;

import static org.sagebionetworks.repo.model.util.AccessControlListUtil.createResourceAccess;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.RequestManager;
import org.sagebionetworks.repo.manager.dataaccess.ResearchProjectManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessRequestNotificationDao;
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.helper.ManagedACTAccessRequirementObjectHelper;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DataAccessRequestNotificationWorkerIntegrationTest {

	private static final long WORKER_TIMEOUT = 3 * 60 * 1000;

	@Autowired
	private UserManager userManager;

	@Autowired
	private RequestManager dataAccessRequestManager;

	@Autowired
	private DaoObjectHelper<Node> nodeDaoHelper;

	@Autowired
	private ManagedACTAccessRequirementObjectHelper managedAccessRequiremetHelper;

	@Autowired
	private AccessControlListObjectHelper aclHelper;

	@Autowired
	private ResearchProjectManager researchProjectManager;

	@Autowired
	private DataAccessRequestNotificationDao dataAccessRequestNotificationDao;

	private UserInfo requester;
	private UserInfo reviewer;

	@BeforeEach
	public void before() {
		truncateAll();
		
		NewUser newUser = new NewUser();
		newUser.setEmail("requester@test.com");
		newUser.setUserName("requester");
		requester = userManager.getUserInfo(userManager.createUser(newUser));
		
		newUser = new NewUser();
		newUser.setEmail("reviewer@test.com");
		newUser.setUserName("reviewer");
		reviewer = userManager.getUserInfo(userManager.createUser(newUser));
	}
	

	@AfterEach
	public void after() {
		truncateAll();
	}
	
	private void truncateAll() {
		dataAccessRequestManager.truncateAll();
		researchProjectManager.truncateAll();
		aclHelper.truncateAll();
		managedAccessRequiremetHelper.truncateAll();
		nodeDaoHelper.truncateAll();
		userManager.truncateAll();
	}

	@Test
	public void testWorkerRun() throws Exception {

		Node node = nodeDaoHelper.create((n) -> {
			n.setNodeType(EntityType.folder);
		});

		ManagedACTAccessRequirement ar = managedAccessRequiremetHelper.create((a) -> {
			a.setName("some ar");
			a.getSubjectIds().get(0).setId(node.getId());
		});

		aclHelper.create((a) -> {
			a.setId(ar.getId().toString());
			a.getResourceAccess().add(createResourceAccess(reviewer.getId(), ACCESS_TYPE.REVIEW_SUBMISSIONS));
		}, ObjectType.ACCESS_REQUIREMENT);

		ResearchProject rp = new ResearchProject();
		rp.setAccessRequirementId(ar.getId().toString());
		rp.setInstitution("sage");
		rp.setIntendedDataUseStatement("for testing");
		rp.setProjectLead("Somebody");
		rp = researchProjectManager.create(requester, rp);

		// create a request
		AccessorChange ac = new AccessorChange();
		ac.setUserId(requester.getId().toString());
		ac.setType(AccessType.GAIN_ACCESS);
		List<AccessorChange> accessorChanges = Arrays.asList(ac);
		RequestInterface request = new Request();
		request.setAccessRequirementId(ar.getId().toString());
		request.setAccessorChanges(accessorChanges);
		request.setResearchProjectId(rp.getId());

		// call under test
		RequestInterface created = dataAccessRequestManager.createOrUpdate(requester, request);

		System.out.println(created);

		TimeUtils.waitFor(WORKER_TIMEOUT, 1000L, () -> {
			Optional<String> id = dataAccessRequestNotificationDao.getMessageId(created.getId(), reviewer.getId());
			return new Pair<>(id.isPresent(), null);
		});

	}

}
