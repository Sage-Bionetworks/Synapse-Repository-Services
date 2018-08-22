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
import org.sagebionetworks.repo.model.DoiAssociationDao;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBODoiAssociationDaoImplAutowiredTest {

	@Autowired
	private DoiAssociationDao doiAssociationDao;
	
	@Autowired
	private DoiAdminDao doiAdminDao;

	private DoiAssociation dto;
	private String associatedById;
	private String updatedById;
	private final String objectId = KeyFactory.keyToString(112233L);
	private final ObjectType objectType = ObjectType.ENTITY;
	private final Long versionNumber = 1L;


	@Before
	public void before() throws Exception {
		assertNotNull(doiAssociationDao);
		assertNotNull(doiAdminDao);
		associatedById = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
		updatedById = "42";
		// Create a DOI DTO with fields necessary to create a new one.
		dto = new DoiAssociation();
		dto.setAssociatedBy(associatedById);
		dto.setUpdatedBy(updatedById);
		dto.setObjectId(objectId);
		dto.setObjectType(objectType);
		dto.setObjectVersion(versionNumber);
	}

	@After
	public void after() throws Exception {
		doiAdminDao.clear();
	}

	@Test
	public void testCreateDoi() {
		// dto is set up in before()
		// Call under test
		DoiAssociation createdDto = doiAssociationDao.createDoiAssociation(dto);

		assertNotNull(createdDto);
		assertNotNull(createdDto.getAssociationId());
		assertNotNull(createdDto.getEtag());
		assertEquals(objectId, createdDto.getObjectId());
		assertEquals(versionNumber, createdDto.getObjectVersion());
		assertEquals(objectType, createdDto.getObjectType());
		assertEquals(associatedById, createdDto.getAssociatedBy());
		assertEquals(updatedById, createdDto.getUpdatedBy());
		assertNotNull(createdDto.getAssociatedOn());
		assertNotNull(createdDto.getUpdatedOn());
	}

	@Test
	public void testCreateDoisOfDifferentVersions() {
		DoiAssociation createdDto = doiAssociationDao.createDoiAssociation(dto);
		assertNotNull(createdDto);
		assertNotNull(createdDto.getAssociationId());
		final String id1 = createdDto.getAssociationId();
		// Create another DOI of null object version
		// This should be treated as a separate DOI
		dto.setObjectVersion(null);
		// Call under test
		createdDto = doiAssociationDao.createDoiAssociation(dto);
		assertNotNull(createdDto);
		assertNotNull(createdDto.getAssociationId());
		final String id2 = createdDto.getAssociationId();
		assertNotEquals(id1, id2);
	}

	@Test
	public void testGetFromId() {
		DoiAssociation createdDto = doiAssociationDao.createDoiAssociation(dto);
		// Call under test
		DoiAssociation retrievedDto = doiAssociationDao.getDoiAssociation(createdDto.getAssociationId());
		assertNotNull(retrievedDto);
		assertEquals(createdDto.getAssociationId(), retrievedDto.getAssociationId());
		assertEquals(createdDto.getEtag(), retrievedDto.getEtag());
		assertEquals(createdDto.getObjectId(), retrievedDto.getObjectId());
		assertEquals(createdDto.getObjectVersion(), retrievedDto.getObjectVersion());
		assertEquals(createdDto.getObjectType(), retrievedDto.getObjectType());
		assertEquals(createdDto.getAssociatedBy(), retrievedDto.getAssociatedBy());
		assertEquals(createdDto.getAssociatedOn(), retrievedDto.getAssociatedOn());
		assertEquals(createdDto.getUpdatedBy(), retrievedDto.getUpdatedBy());
		assertEquals(createdDto.getUpdatedOn(), retrievedDto.getUpdatedOn());
	}

	@Test
	public void testGetFromTriple() {
		DoiAssociation createdDto = doiAssociationDao.createDoiAssociation(dto);
		// Call under test
		DoiAssociation retrievedDto = doiAssociationDao.getDoiAssociation(createdDto.getObjectId(), createdDto.getObjectType(), createdDto.getObjectVersion());
		assertNotNull(retrievedDto);
		assertEquals(createdDto.getAssociationId(), retrievedDto.getAssociationId());
	}

	@Test
	public void testGetNullVersionFromId() {
		dto.setObjectVersion(null);
		DoiAssociation createdDto = doiAssociationDao.createDoiAssociation(dto);
		// Call under test
		DoiAssociation retrievedDto = doiAssociationDao.getDoiAssociation(createdDto.getAssociationId());
		assertEquals(createdDto.getAssociationId(), retrievedDto.getAssociationId());
		assertEquals(createdDto.getObjectVersion(), retrievedDto.getObjectVersion());
	}

	@Test
	public void testGetNullVersionFromTriple() {
		dto.setObjectVersion(null);
		DoiAssociation createdDto = doiAssociationDao.createDoiAssociation(dto);
		// Call under test
		DoiAssociation retrievedDto = doiAssociationDao.getDoiAssociation(createdDto.getObjectId(), createdDto.getObjectType(), createdDto.getObjectVersion());
		assertEquals(createdDto.getAssociationId(), retrievedDto.getAssociationId());
		assertEquals(createdDto.getObjectVersion(), retrievedDto.getObjectVersion());
	}

	@Test(expected = NotFoundException.class)
	public void testGetZeroObjects() {
		// This ID shouldn't exist in the database
		doiAssociationDao.getDoiAssociation("12345");
	}

	@Test(expected = NotFoundException.class)
	public void testGetZeroObjectFromThreeTuple() {
		// This ID shouldn't exist in the database
		doiAssociationDao.getDoiAssociation("12345", ObjectType.ENTITY, null);
	}


	@Test
	public void testUpdate() {
		dto.setUpdatedBy(associatedById);
		DoiAssociation createdDto = doiAssociationDao.createDoiAssociation(dto);
		String oldEtag = createdDto.getEtag();
		String oldUpdatedBy = createdDto.getUpdatedBy();
		createdDto.setUpdatedBy(updatedById);
		// Call under test
		DoiAssociation updatedDto = doiAssociationDao.updateDoiAssociation(createdDto);
		assertEquals(createdDto.getAssociationId(), updatedDto.getAssociationId());
		assertNotEquals(oldEtag, updatedDto.getEtag());
		assertNotEquals(oldUpdatedBy, updatedDto.getUpdatedBy());
	}

	@Test
	public void getEtag() {
		DoiAssociation createdDto = doiAssociationDao.createDoiAssociation(dto);
		// Call under test
		String etag = doiAssociationDao.getEtagForUpdate(createdDto.getObjectId(), createdDto.getObjectType(), createdDto.getObjectVersion());
		assertEquals(createdDto.getEtag(), etag);
	}

	@Test
	public void getEtagNoObjectVersion() {
		dto.setObjectVersion(null);
		DoiAssociation createdDto = doiAssociationDao.createDoiAssociation(dto);
		// Call under test
		String etag = doiAssociationDao.getEtagForUpdate(createdDto.getObjectId(), createdDto.getObjectType(), createdDto.getObjectVersion());
		assertEquals(createdDto.getEtag(), etag);
	}

	@Test(expected = NotFoundException.class)
	public void getEtagNotFound() {
		// Note the DOI is never created.
		// Call under test
		String etag = doiAssociationDao.getEtagForUpdate(dto.getObjectId(), dto.getObjectType(), dto.getObjectVersion());
	}


	@Test(expected=IllegalArgumentException.class)
	public void testCreateDuplicateVersion() {
		doiAssociationDao.createDoiAssociation(dto);
		// This call should attempt to create a duplicate DOI for that DTO
		// This violates the schema, and should yield and IllegalArgumentException
		doiAssociationDao.createDoiAssociation(dto);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateDuplicateNoVersion() {
		dto.setObjectVersion(null);
		doiAssociationDao.createDoiAssociation(dto);
		// This call should attempt to create a duplicate DOI for that DTO
		// This violates the schema, and should yield and IllegalArgumentException
		doiAssociationDao.createDoiAssociation(dto);
	}

	@Test(expected=NotFoundException.class)
	public void testGetNotFoundException() {
		// Note that this DOI was never created
		doiAssociationDao.getDoiAssociation("8675309");
	}

	@Test(expected=NotFoundException.class)
	public void handleIncorrectResultSizeOfZero() {
		IncorrectResultSizeDataAccessException e = new IncorrectResultSizeDataAccessException(1, 0);
		// Call under test. If there are 0 items, we should rethrow a NotFoundException
		DBODoiAssociationDaoImpl.handleIncorrectResultSizeException(e);
	}
	@Test(expected=IllegalStateException.class)
	public void handleIncorrectResultSizeOfMoreThanOne() {
		IncorrectResultSizeDataAccessException e = new IncorrectResultSizeDataAccessException(1, 2);
		// Call under test. If there are 2+ items, we should rethrow an IllegalStateException.
		DBODoiAssociationDaoImpl.handleIncorrectResultSizeException(e);
	}
}
