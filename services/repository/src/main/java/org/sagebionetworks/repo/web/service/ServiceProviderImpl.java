package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.table.ColumnModel;
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
	private S3TokenService s3TokenService;
	@Autowired
	private StorageUsageService storageUsageService;
	@Autowired
	private UserGroupService userGroupService;
	@Autowired
	private UserProfileService userProfileService;
	@Autowired
	SearchService searchService;
	@Autowired
	private ActivityService activityService;
	@Autowired
	private NodeTreeQueryService nodeLineageQueryService;
	@Autowired
	private EvaluationService competitionService;
	@Autowired
	private WikiService wikiService;
	@Autowired
	private TrashService trashService;
	@Autowired
	private DoiService doiService;
	@Autowired
	private MigrationService migrationService;
	@Autowired
	private TableServices tableServices;

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
	public S3TokenService getS3TokenService() {
		return s3TokenService;
	}
	public StorageUsageService getStorageUsageService() {
		return storageUsageService;
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
	public NodeTreeQueryService getNodeTreeQueryService() {
		return nodeLineageQueryService;
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
}
