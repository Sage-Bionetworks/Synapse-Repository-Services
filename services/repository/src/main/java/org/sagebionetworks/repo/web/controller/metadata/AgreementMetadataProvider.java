package org.sagebionetworks.repo.web.controller.metadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.PermissionsManager;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.Agreement;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Eula;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Compartor;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 */
public class AgreementMetadataProvider implements
		TypeSpecificMetadataProvider<Agreement> {

	private static final String PARENT_AGREEMENT_NAME = "ParentAgreement";
	private static final Logger log = Logger
			.getLogger(AgreementMetadataProvider.class.getName());

	private static final Long fakeVersionNumber = -1L;
	private volatile static String parentAgreementId = null;

	@Autowired
	EntityManager entityManager;

	@Autowired
	PermissionsManager permissionsManager;

	@Autowired
	UserGroupDAO userGroupDAO;

	@Autowired
	UserManager userManager;

	@Autowired
	NodeQueryDao nodeQueryDao;

	@Override
	public void addTypeSpecificMetadata(Agreement entity,
			HttpServletRequest request, UserInfo userInfo, EventType eventType)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		if (EventType.CREATE == eventType) {
			// The system is responsible for setting up correct ACLs for
			// agreements
			// Any user should be able to read all agreements in the system
			try {
				addPublicReadToEntity(entity.getId(), getLocalAdminUserInfo());
			} catch (ConflictingUpdateException e) {
				throw new DatastoreException(e);
			} catch (InvalidModelException e) {
				throw new DatastoreException(e);
			}
		}
	}

	@Override
	public void validateEntity(Agreement entity, EntityEvent event)
			throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException {
		if (null == entity.getDatasetId()) {
			throw new InvalidModelException("datasetId cannot be null");
		}
		if (null == entity.getEulaId()) {
			throw new InvalidModelException("eulaId cannot be null");
		}
		if ((null != entity.getCreatedBy()) && !entity.getCreatedBy().equals(event.getUserInfo().getUser().getId())) {
			throw new InvalidModelException("createdBy must be " + event.getUserInfo().getUser().getId());			
		}

		// The system is responsible for setting the versions of the dataset and
		// eula once those objects are versionable PLFM-326
		Dataset dataset = (Dataset) entityManager.getEntity(event.getUserInfo(), entity
				.getDatasetId(), ObjectType.dataset.getClassForType());
		Eula eula = (Eula) entityManager.getEntity(event.getUserInfo(),
				entity.getEulaId(), ObjectType.eula.getClassForType());
		// entity.setDatasetVersionNumber(dataset.getVersionNumber());
		// entity.setEulaVersionNumber(eula.getVersionNumber());
		entity.setDatasetVersionNumber(fakeVersionNumber);
		entity.setEulaVersionNumber(fakeVersionNumber);

		// The system is responsible for setting up correct ACLs for agreements
		// Any user should be able to create an agreement for herself
		entity.setParentId(getAgreementParentId());
	}

	@Override
	public void entityDeleted(Agreement deleted) {
		// TODO Auto-generated method stub
	}

	private String getAgreementParentId() throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException {
		
		// TODO this will be refactored when we have a better spot for system-created parents
		if (null == parentAgreementId) {
			synchronized (AgreementMetadataProvider.class) {
				if (null == parentAgreementId) {
					UserInfo userInfo = getLocalAdminUserInfo();

					BasicQuery query = new BasicQuery();
					query.setFrom(ObjectType.agreement);
					List<Expression> filters = new ArrayList<Expression>();
					filters.add(new Expression(new CompoundId(null, "name"),
							Compartor.EQUALS, PARENT_AGREEMENT_NAME));
					query.setFilters(filters);

					NodeQueryResults agreements = nodeQueryDao.executeQuery(
							query, userInfo);
					if (1 < agreements.getTotalNumberOfResults()) {
						// This isn't fatal, but it is actionable, we should
						// clean up the database
						log.warning("Multiple parent agreements found.");
					}

					if (0 < agreements.getTotalNumberOfResults()) {
						parentAgreementId = agreements.getResultIds().get(0);
					} else {
						Agreement agreement = new Agreement();
						agreement.setName(PARENT_AGREEMENT_NAME);
						parentAgreementId = entityManager.createEntity(
								userInfo, agreement);
						try {
							addPublicCreateToEntity(parentAgreementId, userInfo);
						} catch (ConflictingUpdateException e) {
							throw new DatastoreException(e);
						}
					}
				}
			}
		}
		return parentAgreementId;
	}

	/***
	 * Everything below here should be refactored, what is the larger vision for
	 * this sort of system manipulation of ACLs?
	 * 
	 * @throws NotFoundException
	 * @throws DatastoreException
	 ***/

	private UserInfo getLocalAdminUserInfo() throws DatastoreException,
			NotFoundException {
		// Not sure how to get a working local admin user
		// Horrible Hack to be fix with PLFM-327
		String localAdminUserId = null;
		if("alpha".equals(StackConfiguration.getStack()) || "staging".equals(StackConfiguration.getStack())) {
			localAdminUserId = "nicole.deflaux@sagebase.org";
		}
		else {
		localAdminUserId = TestUserDAO.ADMIN_USER_NAME;
		}
		return userManager.getUserInfo(localAdminUserId);
	}

	private void addPublicReadToEntity(String nodeId, UserInfo userInfo)
			throws NotFoundException, DatastoreException,
			InvalidModelException, UnauthorizedException,
			ConflictingUpdateException {
		AccessControlList acl = permissionsManager.getACL(nodeId, userInfo);
		UserGroup publicGroup = userGroupDAO.findGroup(
				AuthorizationConstants.PUBLIC_GROUP_NAME, false);
		addToACL(acl, publicGroup, ACCESS_TYPE.READ);
		permissionsManager.updateACL(acl, userInfo);
	}

	private void addPublicCreateToEntity(String nodeId, UserInfo userInfo)
			throws NotFoundException, DatastoreException,
			InvalidModelException, UnauthorizedException,
			ConflictingUpdateException {
		AccessControlList acl = permissionsManager.getACL(nodeId, userInfo);
		UserGroup publicGroup = userGroupDAO.findGroup(
				AuthorizationConstants.PUBLIC_GROUP_NAME, false);
		addToACL(acl, publicGroup, ACCESS_TYPE.CREATE);
		// TODO remove these two lines once PLFM-325 is fixed
		addToACL(acl, publicGroup, ACCESS_TYPE.UPDATE);
		addToACL(acl, publicGroup, ACCESS_TYPE.READ);
		permissionsManager.updateACL(acl, userInfo);
	}

	// Code copied from AuthorizationHelper in the test src
	private AccessControlList addToACL(AccessControlList acl, UserGroup ug,
			ACCESS_TYPE at) {
		Set<ResourceAccess> ras = null;
		if (acl.getResourceAccess() == null) {
			ras = new HashSet<ResourceAccess>();
		} else {
			ras = new HashSet<ResourceAccess>(acl.getResourceAccess());
		}
		acl.setResourceAccess(ras);
		ResourceAccess ra = null;
		for (ResourceAccess r : ras) {
			if (r.getUserGroupId() == ug.getId()) {
				ra = r;
				break;
			}
		}
		if (ra == null) {
			ra = new ResourceAccess();
			ra.setUserGroupId(ug.getId());
			Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
			ra.setAccessType(ats);
			ras.add(ra);
		}
		ra.getAccessType().add(at);
		return acl;
	}


}
