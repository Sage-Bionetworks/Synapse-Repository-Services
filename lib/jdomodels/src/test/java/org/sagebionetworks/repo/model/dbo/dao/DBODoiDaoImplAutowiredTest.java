package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DoiAdminDao;
import org.sagebionetworks.repo.model.DoiDao;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBODoiDaoImplAutowiredTest {

	@Autowired
	private DoiDao doiDao;
	
	@Autowired
	private DoiAdminDao doiAdminDao;

	private Doi dto;
	private String userId;
	private final String objectId = KeyFactory.keyToString(112233L);
	private final ObjectType objectType = ObjectType.ENTITY;
	private final Long versionNumber = 1L;
	private final DoiStatus doiStatus = DoiStatus.IN_PROCESS;


	@Before
	public void before() throws Exception {
		assertNotNull(doiDao);
		assertNotNull(doiAdminDao);
		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();

		// Create a DOI DTO with fields necessary to create a new one.
		dto = new Doi();
		dto.setCreatedBy(userId);
		dto.setObjectId(objectId);
		dto.setObjectType(objectType);
		dto.setObjectVersion(versionNumber);
		dto.setDoiStatus(doiStatus);
	}

	@After
	public void after() throws Exception {
		doiAdminDao.clear();
	}

	@Test
	public void testCreateDoi() {
		// dto is set up in before()
		// Call under test
		Doi createdDto = doiDao.createDoi(dto);

		assertNotNull(createdDto);
		assertNotNull(createdDto.getId());
		assertNotNull(createdDto.getEtag());
		assertNotEquals(NodeConstants.ZERO_E_TAG, createdDto.getEtag());
		assertEquals(objectId, createdDto.getObjectId());
		assertEquals(versionNumber, createdDto.getObjectVersion());
		assertEquals(objectType, createdDto.getObjectType());
		assertEquals(doiStatus, createdDto.getDoiStatus());
		assertEquals(userId, createdDto.getCreatedBy());
		assertNotNull(createdDto.getCreatedOn());
		assertNotNull(createdDto.getUpdatedOn());
	}

	@Test
	public void testCreateTwoVersions() {
		Doi createdDto = doiDao.createDoi(dto);
		assertNotNull(createdDto);
		assertNotNull(createdDto.getId());
		final String id1 = createdDto.getId();
		// Create another DOI of null object version
		// This should be treated as a separate DOI
		dto.setObjectVersion(null);
		// Call under test
		createdDto = doiDao.createDoi(dto);
		assertNotNull(createdDto);
		assertNotNull(createdDto.getId());
		final String id2 = createdDto.getId();
		assertNotEquals(id1, id2);
	}

	@Test
	public void testGet() {
		Doi createdDto = doiDao.createDoi(dto);
		// Call under test
		Doi retrievedDto = doiDao.getDoi(dto);
		assertNotNull(retrievedDto);
		assertEquals(createdDto.getId(), retrievedDto.getId());
		assertEquals(createdDto.getEtag(), retrievedDto.getEtag());
		assertEquals(createdDto.getObjectId(), retrievedDto.getObjectId());
		assertEquals(createdDto.getObjectVersion(), retrievedDto.getObjectVersion());
		assertEquals(createdDto.getObjectType(), retrievedDto.getObjectType());
		assertEquals(createdDto.getDoiStatus(), retrievedDto.getDoiStatus());
		assertEquals(createdDto.getCreatedBy(), retrievedDto.getCreatedBy());
		assertEquals(createdDto.getCreatedOn(), retrievedDto.getCreatedOn());
		assertEquals(createdDto.getUpdatedOn(), retrievedDto.getUpdatedOn());
	}

	@Test
	public void testGetNullVersion() {
		dto.setObjectVersion(null);
		Doi createdDto = doiDao.createDoi(dto);
		// Call under test
		Doi retrievedDto = doiDao.getDoi(dto);
		assertNotNull(retrievedDto);
		assertEquals(createdDto.getId(), retrievedDto.getId());
		assertEquals(createdDto.getEtag(), retrievedDto.getEtag());
		assertEquals(createdDto.getObjectId(), retrievedDto.getObjectId());
		assertEquals(createdDto.getObjectVersion(), retrievedDto.getObjectVersion());
		assertEquals(createdDto.getObjectType(), retrievedDto.getObjectType());
		assertEquals(createdDto.getDoiStatus(), retrievedDto.getDoiStatus());
		assertEquals(createdDto.getCreatedBy(), retrievedDto.getCreatedBy());
		assertEquals(createdDto.getCreatedOn(), retrievedDto.getCreatedOn());
		assertEquals(createdDto.getUpdatedOn(), retrievedDto.getUpdatedOn());
	}

	@Test
	public void testUpdate() {
		Doi createdDto = doiDao.createDoi(dto);
		createdDto.setDoiStatus(DoiStatus.CREATED);
		// Call under test
		Doi updatedDto = doiDao.updateDoiStatus(createdDto);
		assertNotNull(updatedDto);
		assertEquals(createdDto.getId(), updatedDto.getId());
		assertNotEquals(createdDto.getEtag(), updatedDto.getEtag());
		assertEquals(createdDto.getObjectId(), updatedDto.getObjectId());
		assertEquals(createdDto.getObjectVersion(), updatedDto.getObjectVersion());
		assertEquals(createdDto.getObjectType(), updatedDto.getObjectType());
		assertEquals(createdDto.getDoiStatus(), updatedDto.getDoiStatus());
		assertEquals(createdDto.getCreatedBy(), updatedDto.getCreatedBy());
		assertEquals(createdDto.getCreatedOn(), updatedDto.getCreatedOn());
		assertEquals(createdDto.getUpdatedOn(), updatedDto.getUpdatedOn());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateDuplicateVersion() {
		doiDao.createDoi(dto);
		// Call under test
		doiDao.createDoi(dto);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateDuplicateNoVersion() {
		dto.setObjectVersion(null);
		doiDao.createDoi(dto);
		// Call under test
		doiDao.createDoi(dto);
	}


	@Test(expected=NotFoundException.class)
	public void testGetNotFoundException() {
		// Note dto is never created
		doiDao.getDoi(dto);
	}
}
