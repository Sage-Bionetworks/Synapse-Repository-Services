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
	public AccessApprovalService accessApprovalService;
	@Autowired
	public AccessRequirementService accessRequirementService;
	@Autowired
	public AdministrationService administrationService;
	@Autowired
	public ConceptService conceptService;
	@Autowired
	public EntityService entityService;
	@Autowired
	public EntityBundleService entityBundleService;
	@Autowired
	public S3TokenService s3TokenService;
	@Autowired
	public SearchService searchService;
	@Autowired
	public UserGroupService userGroupService;
	@Autowired
	public UserProfileService userProfileService;
	
}
