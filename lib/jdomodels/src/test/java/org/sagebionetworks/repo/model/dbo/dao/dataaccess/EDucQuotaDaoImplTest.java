package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException; // wrapped in IllegalArgumentException by DBOBasicDao
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:jdomodels-test-context.xml")
public class EDucQuotaDaoImplTest {

	@Autowired
	private EDucQuotaDao eDucQuotaDao;

	@Autowired
	private AccessRequirementDAO accessRequirementDao;

	@Autowired
	private UserGroupDAO userGroupDao;

	private UserGroup user;
	private Long accessRequirementId;

	@BeforeEach
	public void before() {
		eDucQuotaDao.truncateAll();
		accessRequirementDao.truncateAll();

		user = new UserGroup();
		user.setIsIndividual(true);
		user.setCreationDate(new Date());
		user.setRealmId(AuthorizationConstants.DEFAULT_REALM_ID);
		user.setId(userGroupDao.create(user).toString());

		ManagedACTAccessRequirement ar = new ManagedACTAccessRequirement();
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setCreatedBy(user.getId());
		ar.setCreatedOn(new Date());
		ar.setModifiedBy(user.getId());
		ar.setModifiedOn(new Date());
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId("syn123");
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Collections.singletonList(rod));
		AccessRequirement created = accessRequirementDao.create(ar);
		accessRequirementId = created.getId();
	}

	@AfterEach
	public void after() {
		eDucQuotaDao.truncateAll();
		accessRequirementDao.truncateAll();
		userGroupDao.delete(user.getId());
	}

	@Test
	public void testCreateAndGetCount() {
		Long userId = Long.parseLong(user.getId());

		// call under test
		DBOEDucQuota result = eDucQuotaDao.create(userId, accessRequirementId, "env-001");

		assertNotNull(result.getId());
		assertEquals(userId, result.getUserId());
		assertEquals(accessRequirementId, result.getAccessRequirementId());
		assertEquals("env-001", result.getEnvelopeId());

		long count = eDucQuotaDao.getCount(userId, accessRequirementId, 0L, Long.MAX_VALUE);
		assertEquals(1L, count);
	}

	@Test
	public void testGetCountWithDateRangeFiltering() {
		Long userId = Long.parseLong(user.getId());

		eDucQuotaDao.create(userId, accessRequirementId, "env-a");
		eDucQuotaDao.create(userId, accessRequirementId, "env-b");

		long now = System.currentTimeMillis();

		// call under test — range includes all records
		long count = eDucQuotaDao.getCount(userId, accessRequirementId, 0L, now + 60000);
		assertEquals(2L, count);

		// call under test — range in the future excludes all records
		count = eDucQuotaDao.getCount(userId, accessRequirementId, now + 60000, now + 120000);
		assertEquals(0L, count);
	}

	@Test
	public void testGetCountWithDifferentUser() {
		Long userId = Long.parseLong(user.getId());
		Long otherUserId = 999999L;

		eDucQuotaDao.create(userId, accessRequirementId, "env-x");

		// call under test
		long count = eDucQuotaDao.getCount(otherUserId, accessRequirementId, 0L, Long.MAX_VALUE);
		assertEquals(0L, count);
	}

	@Test
	public void testDelete() {
		Long userId = Long.parseLong(user.getId());
		DBOEDucQuota created = eDucQuotaDao.create(userId, accessRequirementId, "env-del");

		// call under test
		eDucQuotaDao.delete(created.getId());

		long count = eDucQuotaDao.getCount(userId, accessRequirementId, 0L, Long.MAX_VALUE);
		assertEquals(0L, count);
	}

	@Test
	public void testDeleteByUserAndAccessRequirement() {
		Long userId = Long.parseLong(user.getId());

		// A second access requirement to verify the delete is scoped to the given AR
		ManagedACTAccessRequirement ar2 = new ManagedACTAccessRequirement();
		ar2.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar2.setCreatedBy(user.getId());
		ar2.setCreatedOn(new Date());
		ar2.setModifiedBy(user.getId());
		ar2.setModifiedOn(new Date());
		RestrictableObjectDescriptor rod2 = new RestrictableObjectDescriptor();
		rod2.setId("syn456");
		rod2.setType(RestrictableObjectType.ENTITY);
		ar2.setSubjectIds(Collections.singletonList(rod2));
		Long accessRequirementId2 = accessRequirementDao.create(ar2).getId();

		eDucQuotaDao.create(userId, accessRequirementId, "env-r1");
		eDucQuotaDao.create(userId, accessRequirementId, "env-r2");
		eDucQuotaDao.create(userId, accessRequirementId2, "env-r3");

		// call under test
		int deleted = eDucQuotaDao.deleteByUserAndAccessRequirement(userId, accessRequirementId);

		assertEquals(2, deleted);
		assertEquals(0L, eDucQuotaDao.getCount(userId, accessRequirementId, 0L, Long.MAX_VALUE));
		// the other access requirement's records are untouched
		assertEquals(1L, eDucQuotaDao.getCount(userId, accessRequirementId2, 0L, Long.MAX_VALUE));
	}

	@Test
	public void testDeleteByUserAndAccessRequirementWithNoRecords() {
		Long userId = Long.parseLong(user.getId());

		// call under test — nothing to delete
		int deleted = eDucQuotaDao.deleteByUserAndAccessRequirement(userId, accessRequirementId);

		assertEquals(0, deleted);
	}

	@Test
	public void testGetGlobalCount() {
		Long userId = Long.parseLong(user.getId());
		eDucQuotaDao.create(userId, accessRequirementId, "env-g1");
		eDucQuotaDao.create(userId, accessRequirementId, "env-g2");

		long now = System.currentTimeMillis();

		// call under test — range includes all records
		long count = eDucQuotaDao.getGlobalCount(0L, now + 60000);
		assertEquals(2L, count);

		// call under test — range in the future excludes all records
		count = eDucQuotaDao.getGlobalCount(now + 60000, now + 120000);
		assertEquals(0L, count);
	}

	@Test
	public void testCreateWithDuplicateEnvelopeId() {
		Long userId = Long.parseLong(user.getId());
		eDucQuotaDao.create(userId, accessRequirementId, "env-dup");

		// call under test — DBOBasicDao wraps DuplicateKeyException in IllegalArgumentException
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> eDucQuotaDao.create(userId, accessRequirementId, "env-dup"));
		assertTrue(ex.getCause() instanceof DuplicateKeyException);
	}
}
