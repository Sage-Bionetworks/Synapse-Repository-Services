package org.sagebionetworks.repo.web.service;

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

}
