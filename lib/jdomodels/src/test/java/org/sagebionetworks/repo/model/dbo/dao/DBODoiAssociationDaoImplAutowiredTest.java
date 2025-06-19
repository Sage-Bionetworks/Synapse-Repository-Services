package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DoiAdminDao;
import org.sagebionetworks.repo.model.DoiAssociationDao;
import org.sagebionetworks.repo.model.dbo.portals.DBOPortal;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.doi.v2.DoiObjectType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.IllegalTransactionStateException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBODoiAssociationDaoImplAutowiredTest {

	@Autowired
	private DoiAssociationDao doiAssociationDao;
	
	@Autowired
	private DoiAdminDao doiAdminDao;

	private DoiAssociation dto;
	private String associatedById;
	private final String portalId = DBOPortal.SYNAPSE_PORTAL_ID.toString();
	private final String objectId = KeyFactory.keyToString(112233L);
	private final DoiObjectType objectType = DoiObjectType.ENTITY;
	private final Long versionNumber = 1L;
	private final String etag = "etag";
	
	@BeforeEach
	public void before() throws Exception {
		assertNotNull(doiAssociationDao);
		assertNotNull(doiAdminDao);
		associatedById = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
		// Create a DOI DTO with fields necessary to create a new one.
		dto = new DoiAssociation();
		dto.setAssociatedBy(associatedById);
		dto.setUpdatedBy(associatedById);
		dto.setPortalId(portalId);
		dto.setObjectId(objectId);
		dto.setObjectType(objectType);
		dto.setObjectVersion(versionNumber);
		dto.setEtag(etag);
		dto.setAssociatedOn(new Date());
		dto.setUpdatedOn(new Date());
	}

	@AfterEach
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
		assertEquals(etag, createdDto.getEtag());
		assertEquals(portalId, createdDto.getPortalId());
		assertEquals(objectId, createdDto.getObjectId());
		assertEquals(versionNumber, createdDto.getObjectVersion());
		assertEquals(objectType, createdDto.getObjectType());
		assertEquals(associatedById, createdDto.getAssociatedBy());
		assertEquals(associatedById, createdDto.getUpdatedBy());
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
		assertEquals(createdDto.getPortalId(), retrievedDto.getPortalId());
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
		DoiAssociation retrievedDto = doiAssociationDao.getDoiAssociation(createdDto.getPortalId(), createdDto.getObjectId(), createdDto.getObjectType(), createdDto.getObjectVersion());
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
		DoiAssociation retrievedDto = doiAssociationDao.getDoiAssociation(createdDto.getPortalId(), createdDto.getObjectId(), createdDto.getObjectType(), createdDto.getObjectVersion());
		assertEquals(createdDto.getAssociationId(), retrievedDto.getAssociationId());
		assertEquals(createdDto.getObjectVersion(), retrievedDto.getObjectVersion());
	}

	@Test
	public void testGetZeroObjects() {
		assertThrows(NotFoundException.class, () -> {			
			// This ID shouldn't exist in the database
			doiAssociationDao.getDoiAssociation("12345");
		});
	}

	@Test
	public void testGetZeroObjectFromThreeTuple() {
		assertThrows(NotFoundException.class, () -> {
			// This ID shouldn't exist in the database
			doiAssociationDao.getDoiAssociation(portalId, "12345", DoiObjectType.ENTITY, null);
		});
	}


	@Test
	public void testUpdate() {
		// Create a DoiAssociation to update
		DoiAssociation createdDto = doiAssociationDao.createDoiAssociation(dto);

		// Save a field to check if it changes later
		// The manager is in charge of what the new value actually is
		String oldEtag = createdDto.getEtag();
		String newEtag = "new etag";
		createdDto.setEtag(newEtag);

		// Call under test
		DoiAssociation updatedDto = doiAssociationDao.updateDoiAssociation(createdDto);

		assertNotEquals(oldEtag, updatedDto.getEtag());
		assertEquals(newEtag, updatedDto.getEtag());
	}

	@Test
	public void getDoiAssociationForUpdateNotInTransaction() {
		try {
			// Call under test
			doiAssociationDao.getDoiAssociationForUpdate(dto.getPortalId(), dto.getObjectId(), dto.getObjectType(), dto.getObjectVersion());
			fail("Expected IllegalTransactionStateException");
		} catch (IllegalTransactionStateException e) {
			// expected IllegalTransactionStateException since we are not in a read committed transaction
		}
	}

	@Test
	public void getDoiAssociation() {
		dto.setObjectVersion(null);
		DoiAssociation createdDto = doiAssociationDao.createDoiAssociation(dto);
		// Call under test
		DoiAssociation retrievedDto = doiAssociationDao.getDoiAssociation(createdDto.getPortalId(), createdDto.getObjectId(), createdDto.getObjectType(), createdDto.getObjectVersion());
		assertEquals(createdDto, retrievedDto);
	}

	@Test
	public void testCreateDuplicateVersion() {
		doiAssociationDao.createDoiAssociation(dto);
		// This call should attempt to create a duplicate DOI for that DTO, and result in a DuplicateKeyException
		try {
			// Call under test
			doiAssociationDao.createDoiAssociation(dto);
			fail();
		} catch (DuplicateKeyException e) {
			// As expected
		}
	}

	@Test
	public void testCreateSomeOtherError() {
		// This call violates the schema, and should yield an IllegalArgumentException
		try {
			// Call under test
			doiAssociationDao.createDoiAssociation(new DoiAssociation());
			fail();
		} catch (IllegalArgumentException e) {
			// As expected
		}
	}

	@Test
	public void testCreateNullVersion() {
		dto.setObjectVersion(null);
		doiAssociationDao.createDoiAssociation(dto);
	}

	@Test
	public void testGetNotFoundException() {
		assertThrows(NotFoundException.class, () -> {
			// Note that this DOI was never created
			doiAssociationDao.getDoiAssociation("8675309");
		});
	}

	@Test
	public void handleIncorrectResultSizeOfZero() {
		IncorrectResultSizeDataAccessException e = new IncorrectResultSizeDataAccessException(1, 0);
		
		assertThrows(NotFoundException.class, () -> {
			// Call under test. If there are 0 items, we should rethrow a NotFoundException
			DBODoiAssociationDaoImpl.handleIncorrectResultSizeException(e);
		});
	}

	@Test
	public void handleIncorrectResultSizeOfMoreThanOne() {
		IncorrectResultSizeDataAccessException e = new IncorrectResultSizeDataAccessException(1, 2);
		assertThrows(IllegalStateException.class, () -> {
			// Call under test. If there are 2+ items, we should rethrow an IllegalStateException.
			DBODoiAssociationDaoImpl.handleIncorrectResultSizeException(e);
		});
	}
}
