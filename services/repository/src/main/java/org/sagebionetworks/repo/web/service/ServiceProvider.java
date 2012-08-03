package org.sagebionetworks.repo.web.service;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * ServiceProvider is a single class which can be autowired to provide access
 * to all Services. This class should be used to support all Controllers.
 * 
 * @author bkng
 */
public class ServiceProvider {
	
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
	private S3TokenService s3TokenService;
	@Autowired
	private SearchService searchService;
	@Autowired
	private UserGroupService userGroupService;
	@Autowired
	private UserProfileService userProfileService;
	
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
	public S3TokenService getS3TokenService() {
		return s3TokenService;
	}
	public SearchService getSearchService() {
		return searchService;
	}
	public UserGroupService getUserGroupService() {
		return userGroupService;
	}
	public UserProfileService getUserProfileService() {
		return userProfileService;
	}
	
}
