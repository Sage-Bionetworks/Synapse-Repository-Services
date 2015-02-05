package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.web.service.table.TableServices;

/**
 * Abstraction for the service providers.
 *
 */
public interface ServiceProvider {

	public AccessApprovalService getAccessApprovalService();
	
	public AccessRequirementService getAccessRequirementService();
	
	public AdministrationService getAdministrationService();
	
	public ConceptService getConceptService();
	
	public EntityService getEntityService();
	
	public EntityBundleService getEntityBundleService();
	
	public NodeQueryService getNodeQueryService();
	
	public S3TokenService getS3TokenService();
	
	public StorageUsageService getStorageUsageService();
	
	public UserGroupService getUserGroupService();
	
	public UserProfileService getUserProfileService();

	public SearchService getSearchService();

	public ActivityService getActivityService();
	
	public MessageService getMessageService();

	public NodeTreeQueryService getNodeTreeQueryService();

	public EvaluationService getEvaluationService();
	
	public WikiService getWikiService();
	
	public V2WikiService getV2WikiService();

	public TrashService getTrashService();

	public DoiService getDoiService();
	
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
	
}
