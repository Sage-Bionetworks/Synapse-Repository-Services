package org.sagebionetworks.repo.manager.manager.entity.decider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_ACCESS_DENIED;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_CANNOT_REMOVE_ACL_OF_PROJECT;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_CERTIFIED_USER_CONTENT;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_ENTITY_IN_TRASH_TEMPLATE;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_THERE_ARE_UNMET_ACCESS_REQUIREMENTS;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_YOU_HAVE_NOT_YET_AGREED_TO_THE_SYNAPSE_TERMS_OF_USE;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_YOU_LACK_ACCESS_TO_REQUESTED_ENTITY_TEMPLATE;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.entity.decider.AccessContext;
import org.sagebionetworks.repo.manager.entity.decider.EntityDeciderFunctions;
import org.sagebionetworks.repo.manager.entity.decider.UsersEntityAccessInfo;
import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class EntityDeciderFunctionsTest {

	UserInfo adminUser;
	UserInfo nonAdminUser;
	UserInfo anonymousUser;
	UserInfo notCertifiedUser;
	UserInfo certifiedUser;

	UsersRestrictionStatus restrictionStatus;
	UserEntityPermissionsState permissionState;
	AccessContext context;

	@BeforeEach
	public void before() {
		Long entityId = 111L;
		permissionState = new UserEntityPermissionsState(entityId);
		adminUser = new UserInfo(true/* isAdmin */, 222L);
		nonAdminUser = new UserInfo(false/* isAdmin */, 333L);
		anonymousUser = new UserInfo(false/* isAdmin */,
				AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		notCertifiedUser = new UserInfo(false/* isAdmin */, 444L);
		certifiedUser = new UserInfo(false/* isAdmin */, 555L);
		certifiedUser.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		restrictionStatus = new UsersRestrictionStatus(entityId, nonAdminUser.getId());
		context = new AccessContext().withUser(nonAdminUser).withPermissionsState(permissionState)
				.withRestrictionStatus(restrictionStatus);
	}

	@Test
	public void testGrantIfAdminWithAdmin() {
		context = new AccessContext().withUser(adminUser).withPermissionsState(permissionState);
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_ADMIN.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context, AuthorizationStatus.authorized());
		assertEquals(expected, resultOptional.get());
	}

	@Test
	public void testGrantIfAdminWithNonAdmin() {
		context = new AccessContext().withUser(nonAdminUser).withPermissionsState(permissionState);
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_ADMIN.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}

	@Test
	public void testDenyIfInTrashWithInTrash() {
		permissionState.withBenefactorId(NodeConstants.BOOTSTRAP_NODES.TRASH.getId());
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_IN_TRASH
				.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context,
				AuthorizationStatus.accessDenied(new EntityInTrashCanException(
						String.format(ERR_MSG_ENTITY_IN_TRASH_TEMPLATE, permissionState.getEntityIdAsString()))));
		assertEquals(expected, resultOptional.get());
	}

	@Test
	public void testDenyIfInTrashWithNotInTrash() {
		permissionState.withBenefactorId(888L);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_IN_TRASH
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}

	@Test
	public void testDenyIfDoesNotExistWithExistsFalse() {
		permissionState.withtDoesEntityExist(false);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_DOES_NOT_EXIST
				.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context, AuthorizationStatus.accessDenied(
				new NotFoundException(ERR_MSG_THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND)));
		assertEquals(expected, resultOptional.get());
	}

	@Test
	public void testDenyIfDoesNotExistWithExistsTrue() {
		permissionState.withtDoesEntityExist(true);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_DOES_NOT_EXIST
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}

	@Test
	public void testDenyIfHasUnmetAccessRestrictionsWithUnmet() {
		restrictionStatus.setHasUnmet(true);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_HAS_UNMET_ACCESS_RESTRICTIONS
				.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context,
				AuthorizationStatus.accessDenied(ERR_MSG_THERE_ARE_UNMET_ACCESS_REQUIREMENTS));
		assertEquals(expected, resultOptional.get());
	}

	@Test
	public void testDenyIfHasUnmetAccessRestrictionsWithMet() {
		restrictionStatus.setHasUnmet(false);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_HAS_UNMET_ACCESS_RESTRICTIONS
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}

	@Test
	public void testGrantIfOpenDataWithBoth() {
		permissionState.withDataType(DataType.OPEN_DATA);
		permissionState.withHasRead(true);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_OPEN_DATA_WITH_READ
				.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context, AuthorizationStatus.authorized());
		assertEquals(expected, resultOptional.get());
	}

	@Test
	public void testGrantIfOpenDataWithSenstiveDataRead() {
		permissionState.withDataType(DataType.SENSITIVE_DATA);
		permissionState.withHasRead(true);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_OPEN_DATA_WITH_READ
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}

	@Test
	public void testGrantIfOpenDataWithOpenDataNoRead() {
		permissionState.withDataType(DataType.OPEN_DATA);
		permissionState.withHasRead(false);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_OPEN_DATA_WITH_READ
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}

	@Test
	public void testGrantIfHasDownloadWithDownloadTrue() {
		permissionState.withHasDownload(true);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_HAS_DOWNLOAD
				.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context, AuthorizationStatus.authorized());
		assertEquals(expected, resultOptional.get());
	}

	@Test
	public void testGrantIfHasDownloadWithDownloadFalse() {
		permissionState.withHasDownload(false);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_HAS_DOWNLOAD
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}

	@Test
	public void testDenyWithNullAccessType() {
		context.withAccessType(null);
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context,
				AuthorizationStatus.accessDenied(ERR_MSG_ACCESS_DENIED));
		assertEquals(expected, resultOptional.get());
	}

	@Test
	public void testDenyWithAccessType() {
		context.withAccessType(ACCESS_TYPE.DOWNLOAD);
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context, AuthorizationStatus.accessDenied(
				String.format(ERR_MSG_YOU_LACK_ACCESS_TO_REQUESTED_ENTITY_TEMPLATE, ACCESS_TYPE.DOWNLOAD.name())));
		assertEquals(expected, resultOptional.get());
	}

	@Test
	public void testGrantIfHasModerateWithModerateTrue() {
		permissionState.withHasModerate(true);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_HAS_MODERATE
				.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context, AuthorizationStatus.authorized());
		assertEquals(expected, resultOptional.get());
	}

	@Test
	public void testGrantIfHasModerateWithModerateFalse() {
		permissionState.withHasModerate(false);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_HAS_MODERATE
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}

	@Test
	public void testGrantIfHasChangeSettingsWithTrue() {
		permissionState.withHasChangeSettings(true);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_HAS_CHANGE_SETTINGS
				.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context, AuthorizationStatus.authorized());
		assertEquals(expected, resultOptional.get());
	}

	@Test
	public void testGrantIfHasChangeSettingsWithFalse() {
		permissionState.withHasChangeSettings(false);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_HAS_CHANGE_SETTINGS
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}

	@Test
	public void testGrantIfHasChangePermissionsWithTrue() {
		permissionState.withHasChangePermissions(true);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_HAS_CHANGE_PERMISSION
				.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context, AuthorizationStatus.authorized());
		assertEquals(expected, resultOptional.get());
	}

	@Test
	public void testGrantIfHasChangePermissionsWithFalse() {
		permissionState.withHasChangePermissions(false);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_HAS_CHANGE_PERMISSION
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}
	
	@Test
	public void testGrantIfHasUpdateWithFalse() {
		permissionState.withHasUpdate(false);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_HAS_UPDATE
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}
	
	@Test
	public void testGrantIfHasUpdateWithTrue() {
		permissionState.withHasUpdate(true);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_HAS_UPDATE
				.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context, AuthorizationStatus.authorized());
		assertEquals(expected, resultOptional.get());
	}

	@Test
	public void testGrantIfHasCreateWithFalse() {
		permissionState.withHasCreate(false);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_HAS_CREATE
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}
	
	@Test
	public void testGrantIfHasCreateWithTrue() {
		permissionState.withHasCreate(true);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_HAS_CREATE
				.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context, AuthorizationStatus.authorized());
		assertEquals(expected, resultOptional.get());
	}

	@Test
	public void testGrantIfHasReadWithFalse() {
		permissionState.withHasRead(false);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_HAS_READ
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}
	
	@Test
	public void testGrantIfHasReadWithTrue() {
		permissionState.withHasRead(true);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_HAS_READ
				.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context, AuthorizationStatus.authorized());
		assertEquals(expected, resultOptional.get());
	}

	@Test
	public void testGrantIfHasDeleteWithFalse() {
		permissionState.withHasDelete(false);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_HAS_DELETE
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}
	
	@Test
	public void testGrantIfHasDeleteWithTrue() {
		permissionState.withHasDelete(true);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_HAS_DELETE
				.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context, AuthorizationStatus.authorized());
		assertEquals(expected, resultOptional.get());
	}
	

	@Test
	public void testDenyIfAnonymousWithTrue() {
		context = new AccessContext().withUser(anonymousUser).withPermissionsState(permissionState);
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_ANONYMOUS
				.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context,
				AuthorizationStatus.accessDenied(ERR_MSG_ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION));
		assertEquals(expected, resultOptional.get());
	}

	@Test
	public void testDenyIfAnonymousWithFalse() {
		context = new AccessContext().withUser(nonAdminUser).withPermissionsState(permissionState);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_ANONYMOUS
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}

	@Test
	public void testDenyIfNotCertifiedWithNoCertification() {
		context = new AccessContext().withUser(notCertifiedUser).withPermissionsState(permissionState);
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_NOT_CERTIFIED
				.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context,
				AuthorizationStatus.accessDenied(ERR_MSG_CERTIFIED_USER_CONTENT));
		assertEquals(expected, resultOptional.get());
	}

	@Test
	public void testDenyIfNotCertifiedWithCertification() {
		context = new AccessContext().withUser(certifiedUser).withPermissionsState(permissionState);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_NOT_CERTIFIED
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}

	@Test
	public void testDenyIfHasNotAcceptedTermsOfUseWithNoAccept() {
		nonAdminUser.setAcceptsTermsOfUse(false);
		context = new AccessContext().withUser(nonAdminUser).withPermissionsState(permissionState);
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_HAS_NOT_ACCEPTED_TERMS_OF_USE
				.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context,
				AuthorizationStatus.accessDenied(ERR_MSG_YOU_HAVE_NOT_YET_AGREED_TO_THE_SYNAPSE_TERMS_OF_USE));
		assertEquals(expected, resultOptional.get());
	}

	@Test
	public void testDenyIfHasNotAcceptedTermsOfUseWithAccept() {
		nonAdminUser.setAcceptsTermsOfUse(true);
		context = new AccessContext().withUser(nonAdminUser).withPermissionsState(permissionState);

		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_HAS_NOT_ACCEPTED_TERMS_OF_USE
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}

	@Test
	public void testDenyIfCreateTypeIsNotProjectAndNotCertifiedWithNullCreateTypeCertifiedFalse() {
		context = new AccessContext().withUser(notCertifiedUser).withPermissionsState(permissionState)
				.withEntityCreateType(null);
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_CREATE_TYPE_IS_NOT_PROJECT_AND_NOT_CERTIFIED
				.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context,
				AuthorizationStatus.accessDenied(ERR_MSG_CERTIFIED_USER_CONTENT));
		assertEquals(expected, resultOptional.get());
	}
	
	@Test
	public void testDenyIfCreateTypeIsNotProjectAndNotCertifiedWithNullCertifiedTrue() {
		context = new AccessContext().withUser(certifiedUser).withPermissionsState(permissionState)
				.withEntityCreateType(null);
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_CREATE_TYPE_IS_NOT_PROJECT_AND_NOT_CERTIFIED
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}

	@Test
	public void testDenyIfCreateTypeIsNotProjectAndNotCertifiedWithProjectAndCertifiedTrue() {
		context = new AccessContext().withUser(certifiedUser).withPermissionsState(permissionState)
				.withEntityCreateType(EntityType.project);
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_CREATE_TYPE_IS_NOT_PROJECT_AND_NOT_CERTIFIED
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}
	
	@Test
	public void testDenyIfCreateTypeIsNotProjectAndNotCertifiedWithProjectAndCertifiedFalse() {
		context = new AccessContext().withUser(notCertifiedUser).withPermissionsState(permissionState)
				.withEntityCreateType(EntityType.project);
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_CREATE_TYPE_IS_NOT_PROJECT_AND_NOT_CERTIFIED
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}
	
	@Test
	public void testDenyIfCreateTypeIsNotProjectAndNotCertifiedWithFileAndCertifiedTrue() {
		context = new AccessContext().withUser(certifiedUser).withPermissionsState(permissionState)
				.withEntityCreateType(EntityType.file);
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_CREATE_TYPE_IS_NOT_PROJECT_AND_NOT_CERTIFIED
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}
	
	@Test
	public void testDenyIfCreateTypeIsNotProjectAndNotCertifiedWithFileAndCertifiedFalse() {
		context = new AccessContext().withUser(notCertifiedUser).withPermissionsState(permissionState)
				.withEntityCreateType(EntityType.file);
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_CREATE_TYPE_IS_NOT_PROJECT_AND_NOT_CERTIFIED
				.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context,
				AuthorizationStatus.accessDenied(ERR_MSG_CERTIFIED_USER_CONTENT));
		assertEquals(expected, resultOptional.get());
	}
	
	@Test
	public void testDenyIfNotProjectAndNotCertifiedWithProjectCertifiedFalse() {
		context = new AccessContext().withUser(notCertifiedUser)
				.withPermissionsState(permissionState.withEntityType(EntityType.project));
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_NOT_PROJECT_AND_NOT_CERTIFIED
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}
	
	@Test
	public void testDenyIfNotProjectAndNotCertifiedWithFileCertifiedFalse() {
		context = new AccessContext().withUser(notCertifiedUser)
				.withPermissionsState(permissionState.withEntityType(EntityType.file));
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_NOT_PROJECT_AND_NOT_CERTIFIED
				.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context,
				AuthorizationStatus.accessDenied(ERR_MSG_CERTIFIED_USER_CONTENT));
		assertEquals(expected, resultOptional.get());
	}
	
	@Test
	public void testDenyIfNotProjectAndNotCertifiedWithProjectCertifiedTrue() {
		context = new AccessContext().withUser(certifiedUser)
				.withPermissionsState(permissionState.withEntityType(EntityType.project));
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_NOT_PROJECT_AND_NOT_CERTIFIED
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}
	
	@Test
	public void testDenyIfNotProjectAndNotCertifiedWithFileCertifiedTrue() {
		context = new AccessContext().withUser(certifiedUser)
				.withPermissionsState(permissionState.withEntityType(EntityType.file));
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_NOT_PROJECT_AND_NOT_CERTIFIED
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}
	
	@Test
	public void testDenyIfParentIsRootOrNullWithParentRoot() {
		context = new AccessContext().withUser(certifiedUser)
				.withPermissionsState(permissionState.withEntityParentId(NodeConstants.BOOTSTRAP_NODES.ROOT.getId()));
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_PARENT_IS_ROOT_OR_NULL
				.determineAccess(context);
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context,
				AuthorizationStatus.accessDenied(ERR_MSG_CANNOT_REMOVE_ACL_OF_PROJECT));
		assertEquals(expected, resultOptional.get());
	}
	
	@Test
	public void testDenyIfParentIsRootOrNullWithNullParent() {
		context = new AccessContext().withUser(certifiedUser)
				.withPermissionsState(permissionState.withEntityParentId(null));
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_PARENT_IS_ROOT_OR_NULL
				.determineAccess(context);
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context,
				AuthorizationStatus.accessDenied(ERR_MSG_CANNOT_REMOVE_ACL_OF_PROJECT));
		assertEquals(expected, resultOptional.get());
	}
	
	@Test
	public void testDenyIfParentIsRootOrNullWithParentNotRoot() {
		context = new AccessContext().withUser(certifiedUser)
				.withPermissionsState(permissionState.withEntityParentId(123L));
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.DENY_IF_PARENT_IS_ROOT_OR_NULL
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}
	
	@Test
	public void testGrantIfUserIsCreatorWithCreator() {
		context = new AccessContext().withUser(nonAdminUser)
				.withPermissionsState(permissionState.withEntityCreatedBy(nonAdminUser.getId()));
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_USER_IS_CREATOR
				.determineAccess(context);
		assertTrue(resultOptional.isPresent());
		UsersEntityAccessInfo expected = new UsersEntityAccessInfo(context, AuthorizationStatus.authorized());
		assertEquals(expected, resultOptional.get());
	}
	
	@Test
	public void testGrantIfUserIsCreatorWithNotCreator() {
		context = new AccessContext().withUser(nonAdminUser)
				.withPermissionsState(permissionState.withEntityCreatedBy(nonAdminUser.getId()+1));
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_USER_IS_CREATOR
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}
	
	@Test
	public void testGrantIfUserIsCreatorWithNullCreator() {
		context = new AccessContext().withUser(nonAdminUser)
				.withPermissionsState(permissionState.withEntityCreatedBy(null));
		// call under test
		Optional<UsersEntityAccessInfo> resultOptional = EntityDeciderFunctions.GRANT_IF_USER_IS_CREATOR
				.determineAccess(context);
		assertFalse(resultOptional.isPresent());
	}
}
