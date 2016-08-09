package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.web.service.discussion.DiscussionService;
import org.sagebionetworks.repo.web.service.subscription.SubscriptionService;
import org.sagebionetworks.repo.web.service.table.TableServices;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * ServiceProvider is a single class which can be autowired to provide access
 * to all Services. This class should be used to support all Controllers.
 * 
 * @author bkng
 */
public class ServiceProviderImpl implements ServiceProvider {
	
	@Autowired
	private AccessApprovalService accessApprovalService;
	@Autowired
	private AccessRequirementService accessRequirementService;
	@Autowired
	private AdministrationService administrationService;
	@Autowired
	private ConceptService conceptService;
	@Autowired
	private EntityService entityService;
	@Autowired
	private EntityBundleService entityBundleService;
	@Autowired
	private NodeQueryService nodeQueryService;
	@Autowired
	private UserGroupService userGroupService;
	@Autowired
	private UserProfileService userProfileService;
	@Autowired
	SearchService searchService;
	@Autowired
	private ActivityService activityService;
	@Autowired
	private MessageService messageService;
	@Autowired
	private EvaluationService competitionService;
	@Autowired
	private WikiService wikiService;
	@Autowired
	private V2WikiService v2WikiService;
	@Autowired
	private TrashService trashService;
	@Autowired
	private DoiService doiService;
	@Autowired
	private MigrationService migrationService;
	@Autowired
	private TableServices tableServices;
	@Autowired
	private TeamService teamService;
	@Autowired
	private MembershipInvitationService membershipInvitationService;
	@Autowired
	private MembershipRequestService membershipRequestService;
	@Autowired
	private PrincipalService principalService;
	@Autowired
	private CertifiedUserService certifiedUserService;
	@Autowired
	private AsynchronousJobServices asynchronousJobServices;
	@Autowired
	private LogService logService;
	@Autowired
	private ProjectSettingsService projectSettingsService;
	@Autowired
	private ChallengeService challengeService;
	@Autowired
	private VerificationService verificationService;
	@Autowired
	private DiscussionService discussionService;
	@Autowired
	private SubscriptionService subscriptionService;
	@Autowired
	private DockerService dockerService;
	
	public AccessApprovalService getAccessApprovalService() {
		return accessApprovalService;
	}
	public AccessRequirementService getAccessRequirementService() {
		return accessRequirementService;
	}
	public AdministrationService getAdministrationService() {
		return administrationService;
	}
	public ConceptService getConceptService() {
		return conceptService;
	}
	public EntityService getEntityService() {
		return entityService;
	}
	public EntityBundleService getEntityBundleService() {
		return entityBundleService;
	}
	public NodeQueryService getNodeQueryService() {
		return nodeQueryService;
	}
	public UserGroupService getUserGroupService() {
		return userGroupService;
	}
	public UserProfileService getUserProfileService() {
		return userProfileService;
	}
	@Override
	public SearchService getSearchService() {
		return searchService;
	}
	@Override
	public ActivityService getActivityService() {
		return activityService;
	}
	@Override
	public MessageService getMessageService() {
		return messageService;
	}
	@Override
	public EvaluationService getEvaluationService() {
		return competitionService;
	}
	@Override
	public WikiService getWikiService() {
		return wikiService;
	}
	@Override
	public V2WikiService getV2WikiService() {
		return v2WikiService;
	}
	@Override
	public TrashService getTrashService() {
		return trashService;
	}
	@Override
	public DoiService getDoiService() {
		return doiService;
	}
	@Override
	public MigrationService getMigrationService() {
		return migrationService;
	}
	@Override
	public TableServices getTableServices() {
		return tableServices;
	}
	@Override
	public TeamService getTeamService() {
		return teamService;
	}
	@Override
	public MembershipInvitationService getMembershipInvitationService() {
		return membershipInvitationService;
	}
	@Override
	public MembershipRequestService getMembershipRequestService() {
		return membershipRequestService;
	}
	@Override
	public PrincipalService getPrincipalService() {
		return principalService;
	}
	@Override
	public CertifiedUserService getCertifiedUserService()  {
		return certifiedUserService;
	}
	@Override
	public AsynchronousJobServices getAsynchronousJobServices() {
		return asynchronousJobServices;
	}

	@Override
	public LogService getLogService() {
		return logService;
	}

	@Override
	public ProjectSettingsService getProjectSettingsService() {
		return projectSettingsService;
	}
	
	@Override
	public ChallengeService getChallengeService() {
		return challengeService;
	}
	
	@Override
	public VerificationService getVerificationService() {
		return verificationService;
	}
	@Override
	public DiscussionService getDiscussionService() {
		return discussionService;
	}
	@Override
	public SubscriptionService getSubscriptionService() {
		return subscriptionService;
	}
	@Override
	public DockerService getDockerService() {
		return dockerService;
	}
	
}
