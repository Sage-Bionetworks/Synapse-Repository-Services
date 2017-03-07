package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dbo.dao.AccessRequirementUtilsTest;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOResearchProjectDAOImplTest {

	@Autowired
	UserGroupDAO userGroupDAO;

	@Autowired
	NodeDAO nodeDao;

	@Autowired
	AccessRequirementDAO accessRequirementDAO;

	@Autowired
	private ResearchProjectDAO researchProjectDao;

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private PlatformTransactionManager txManager;
	private TransactionTemplate transactionTemplate;

	private UserGroup individualGroup = null;
	private Node node = null;
	private ACTAccessRequirement accessRequirement = null;
	private ResearchProject dto = null;

	@Before
	public void before() {
		// create a user
		individualGroup = new UserGroup();
		individualGroup.setIsIndividual(true);
		individualGroup.setCreationDate(new Date());
		individualGroup.setId(userGroupDAO.create(individualGroup).toString());

		// create a node
		node = NodeTestUtils.createNew("foo", Long.parseLong(individualGroup.getId()));
		node.setId(nodeDao.createNew(node));

		// create an ACTAccessRequirement
		accessRequirement = new ACTAccessRequirement();
		accessRequirement.setCreatedBy(individualGroup.getId());
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy(individualGroup.getId());
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setEtag("10");
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		RestrictableObjectDescriptor rod = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId());
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod, rod}));
		accessRequirement.setConcreteType("com.sagebionetworks.repo.model.ACTAccessRequirements");
		accessRequirement = accessRequirementDAO.create(accessRequirement);

		transactionTemplate = new TransactionTemplate(txManager);
	}

	@After
	public void after() {
		if (dto != null) {
			researchProjectDao.delete(dto.getId());
		}
		if (accessRequirement != null) {
			accessRequirementDAO.delete(accessRequirement.getId().toString());
		}
		if (node != null) {
			nodeDao.delete(node.getId());
			node = null;
		}
		if (individualGroup != null) {
			userGroupDAO.delete(individualGroup.getId());
		}
	}

	@Test (expected=NotFoundException.class)
	public void testNotFound() {
		ResearchProject dto = ResearchProjectTestUtils.createNewDto();
		researchProjectDao.get(dto.getAccessRequirementId(), dto.getOwnerId());
	}

	@Test
	public void testCRUD() {
		dto = ResearchProjectTestUtils.createNewDto();
		dto.setId(idGenerator.generateNewId(TYPE.RESEARCH_PROJECT_ID).toString());
		dto.setAccessRequirementId(accessRequirement.getId().toString());
		assertEquals(dto, researchProjectDao.create(dto));

		// should get back the same object
		ResearchProject created = researchProjectDao.get(dto.getAccessRequirementId(), dto.getOwnerId());
		assertEquals(dto, created);

		// update
		dto.setProjectLead("new projectLead");
		assertEquals(dto, researchProjectDao.update(dto));

		// change ownership
		String newOwnerId = "333";
		String modifiedBy = "999";
		long modifiedOn = System.currentTimeMillis();
		String newEtag = "newEtag";
		ResearchProject newDto = researchProjectDao.changeOwnership(dto.getId(), newOwnerId, modifiedBy, modifiedOn, newEtag);
		assertEquals(dto.getId(), newDto.getId());
		assertEquals(newOwnerId, newDto.getOwnerId());
		assertEquals(modifiedBy, newDto.getModifiedBy());
		assertEquals(modifiedOn, newDto.getModifiedOn().getTime());
		assertEquals(newEtag, newDto.getEtag());

		// insert another one with the same accessRequirementId & createdBy
		try {
			researchProjectDao.create(dto);
			fail("should fail because of uniqueness constraint");
		} catch (IllegalArgumentException e){
			// as expected
		}

		// test get for update
		ResearchProject locked = getForUpdateInTransaction(newDto.getId(), newDto.getEtag());
		assertEquals(newDto, locked);

		try {
			getForUpdateInTransaction(dto.getId(), "some fake etag");
			fail("should fail because etag does not match");
		} catch (ConflictingUpdateException e) {
			// as expected
		}
	}

	private ResearchProject getForUpdateInTransaction(final String id, final String etag) {
		return transactionTemplate.execute(new TransactionCallback<ResearchProject>() {
			@Override
			public ResearchProject doInTransaction(TransactionStatus status) {
				// Try to lock both nodes out of order
				return researchProjectDao.getForUpdate(id, etag);
			}
		});
	}
}
