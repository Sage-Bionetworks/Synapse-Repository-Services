package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.auth.services.OpenIDConnectService;
import org.sagebionetworks.repo.web.service.dataaccess.DataAccessService;
import org.sagebionetworks.repo.web.service.discussion.DiscussionService;
import org.sagebionetworks.repo.web.service.statistics.StatisticsService;
import org.sagebionetworks.repo.web.service.subscription.SubscriptionService;
import org.sagebionetworks.repo.web.service.table.TableServices;
import org.sagebionetworks.repo.web.service.verification.VerificationService;

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
	
}
