package org.sagebionetworks.dataaccess.workers;

import static org.sagebionetworks.repo.model.util.AccessControlListUtil.createResourceAccess;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.RequestManager;
import org.sagebionetworks.repo.manager.dataaccess.ResearchProjectManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
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
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.helper.ManagedACTAccessRequirementObjectHelper;
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


	private UserInfo admin;
	private UserInfo user;

	@BeforeEach
	public void before() {
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		NewUser newUser = new NewUser();
		newUser.setEmail(UUID.randomUUID().toString() + "@test.com");
		newUser.setUserName(UUID.randomUUID().toString());
		user = userManager.getUserInfo(userManager.createUser(newUser));
	}

	@AfterEach
	public void after() {
		dataAccessRequestManager.truncateAll();
		researchProjectManager.truncateAll();
		aclHelper.truncateAll();
		managedAccessRequiremetHelper.truncateAll();
		nodeDaoHelper.truncateAll();
	}

	@Test
	public void testWorkerRun() {

		Node node = nodeDaoHelper.create((n) -> {
			n.setNodeType(EntityType.folder);
		});
		
		ManagedACTAccessRequirement ar = managedAccessRequiremetHelper.create((a)->{
			a.setName("some ar");
			a.getSubjectIds().get(0).setId(node.getId());
		});
		
		aclHelper.create((a) -> {
			a.setId(ar.getId().toString());
			a.getResourceAccess().add(createResourceAccess(user.getId(), ACCESS_TYPE.REVIEW_SUBMISSIONS));
		}, ObjectType.ACCESS_REQUIREMENT);


		ResearchProject rp = new ResearchProject();
		rp.setAccessRequirementId(ar.getId().toString());
		rp.setInstitution("sage");
		rp.setIntendedDataUseStatement("for testing");
		rp.setProjectLead("Somebody");
		rp = researchProjectManager.create(user, rp);

		// create a request
		AccessorChange ac = new AccessorChange();
		ac.setUserId(user.getId().toString());
		ac.setType(AccessType.GAIN_ACCESS);
		List<AccessorChange> accessorChanges = Arrays.asList(ac);
		RequestInterface request = new Request();
		request.setAccessRequirementId(ar.getId().toString());
		request.setAccessorChanges(accessorChanges);
		request.setResearchProjectId(rp.getId());
		
		// call under test
		RequestInterface created = dataAccessRequestManager.createOrUpdate(user, request);
		
		System.out.println(created);

//		TimeUtils.waitFor(WORKER_TIMEOUT, 1000L, () -> {
//			Optional<DBODataAccessNotification> result = notificationDao.find(notificationType, ar.getId(), user.getId());
//			
//			result.ifPresent(notification -> {
//				assertEquals(ar.getId(), notification.getRequirementId());
//				assertEquals(approval.getId(), notification.getAccessApprovalId());
//				assertEquals(user.getId(), notification.getRecipientId());
//				// Makes sure that it didn't actually send a message
//				assertEquals(AccessApprovalNotificationManager.NO_MESSAGE_TO_USER, notification.getMessageId());
//			});
//			
//			return new Pair<>(result.isPresent(), null);
//		});

	}


}
