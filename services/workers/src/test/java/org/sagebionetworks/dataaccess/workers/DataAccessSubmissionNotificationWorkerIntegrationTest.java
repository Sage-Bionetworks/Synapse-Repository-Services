package org.sagebionetworks.dataaccess.workers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sagebionetworks.repo.model.util.AccessControlListUtil.createResourceAccess;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.RequestManager;
import org.sagebionetworks.repo.manager.dataaccess.ResearchProjectManager;
import org.sagebionetworks.repo.manager.dataaccess.SubmissionManagerImpl;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.CreateSubmissionRequest;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
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
public class DataAccessSubmissionNotificationWorkerIntegrationTest {

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
	private SubmissionManagerImpl submissionManager;
	
	@Autowired
	private AsynchronousJobWorkerHelper asynchHelper;
	
	@Autowired
	private MessageDAO messageDao;

	private String requesterEmail;
	private String reviewerEmail;

	private UserInfo admin;
	private UserInfo requester;
	private UserInfo reviewer;
	private List<Long> usersToDelete;

	@BeforeEach
	public void before() {
		truncateAll();
		usersToDelete = new ArrayList<>(2);
		admin = userManager.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		NewUser newUser = new NewUser();
		String requesterName = String.format("requester-%s", UUID.randomUUID().toString());
		requesterEmail = requesterName+"@test.com";
		newUser.setEmail(requesterEmail);
		newUser.setUserName(requesterName);
		requester = userManager.getUserInfo(userManager.createUser(newUser));
		usersToDelete.add(requester.getId());

		newUser = new NewUser();
		String reviewerName = String.format("reviewer-%s", UUID.randomUUID().toString());
		reviewerEmail = reviewerName+"@test.com";
		newUser.setEmail(reviewerEmail);
		newUser.setUserName(reviewerName);
		reviewer = userManager.getUserInfo(userManager.createUser(newUser));
		usersToDelete.add(reviewer.getId());
	}

	@AfterEach
	public void after() {
		truncateAll();
		if (usersToDelete != null) {
			usersToDelete.stream().forEach(id -> userManager.deletePrincipal(admin, id));
		}
	}

	private void truncateAll() {
		messageDao.truncateAll();
		submissionManager.truncateAll();
		dataAccessRequestManager.truncateAll();
		researchProjectManager.truncateAll();
		aclHelper.truncateAll();
		managedAccessRequiremetHelper.truncateAll();
		nodeDaoHelper.truncateAll();
	}

	@Test
	public void testWorkerRun() throws Exception {

		Node node = nodeDaoHelper.create((n) -> {
			n.setNodeType(EntityType.folder);
		});

		ManagedACTAccessRequirement ar = managedAccessRequiremetHelper.create((a) -> {
			a.setName("some ar");
			a.getSubjectIds().get(0).setId(node.getId());
			a.setIsCertifiedUserRequired(false);
			a.setIsDUCRequired(false);
			a.setIsIDUPublic(false);
			a.setIsIRBApprovalRequired(false);
			a.setAreOtherAttachmentsRequired(false);
			a.setIsValidatedProfileRequired(false);
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

		request = dataAccessRequestManager.createOrUpdate(requester, request);

		// call under test
		SubmissionStatus status = submissionManager.create(requester,
				new CreateSubmissionRequest().setRequestEtag(request.getEtag()).setRequestId(request.getId())
						.setSubjectId("syn123").setSubjectType(RestrictableObjectType.ENTITY));

		System.out.println(status);
		
		String emailBody = asynchHelper.waitForEmailMessgae(reviewerEmail, WORKER_TIMEOUT);

		assertTrue(emailBody.contains(reviewerEmail));

	}

}
