package org.sagebionetworks.repo.service;

import org.sagebionetworks.repo.service.auth.OpenIDConnectService;
import org.sagebionetworks.repo.service.dataaccess.DataAccessService;
import org.sagebionetworks.repo.service.discussion.DiscussionService;
import org.sagebionetworks.repo.service.drs.DrsService;
import org.sagebionetworks.repo.service.statistics.StatisticsService;
import org.sagebionetworks.repo.service.subscription.SubscriptionService;
import org.sagebionetworks.repo.service.table.TableServices;
import org.sagebionetworks.repo.service.verification.VerificationService;

/**
 * Abstraction for the service providers.
 *
 */
public interface ServiceProvider {

	public AccessApprovalService getAccessApprovalService();
	
	public AccessRequirementService getAccessRequirementService();
	
	public AdministrationService getAdministrationService();
	
	public EntityService getEntityService();
	
	public EntityBundleService getEntityBundleService();
	
	public UserGroupService getUserGroupService();
	
	public UserProfileService getUserProfileService();

	public SearchService getSearchService();

	public ActivityService getActivityService();
	
	public MessageService getMessageService();

	public EvaluationService getEvaluationService();
	
	public WikiService getWikiService();
	
	public V2WikiService getV2WikiService();

	public TrashService getTrashService();

	public DoiServiceV2 getDoiServiceV2();

	public MigrationService getMigrationService();

	public TableServices getTableServices();
	
	public TeamService getTeamService();
	
	public MembershipInvitationService getMembershipInvitationService();
	
	public MembershipRequestService getMembershipRequestService();
	
	public PrincipalService getPrincipalService();
	
	public CertifiedUserService getCertifiedUserService();
	
	public AsynchronousJobServices getAsynchronousJobServices();

	public LogService getLogService();

	public ProjectSettingsService getProjectSettingsService();
	
	public ChallengeService getChallengeService();
	
	public VerificationService getVerificationService();

	public DiscussionService getDiscussionService();
	
	public FormService getFormService();

	public SubscriptionService getSubscriptionService();
	
	public DockerService getDockerService();

	public DataAccessService getDataAccessService();
	
	public OpenIDConnectService getOpenIDConnectService();
	
	public StatisticsService getStatisticsService();
	
	public JsonSchemaServices getSchemaServices();
	
	public DownloadListService getDownloadListService();

	DrsService getDrsService();
	
}
