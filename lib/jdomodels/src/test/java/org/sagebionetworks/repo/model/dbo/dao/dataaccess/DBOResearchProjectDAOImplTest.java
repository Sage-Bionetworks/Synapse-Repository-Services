package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
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
import org.springframework.transaction.IllegalTransactionStateException;
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
	private PlatformTransactionManager txManager;
	private TransactionTemplate transactionTemplate;

	private UserGroup individualGroup = null;
	private Node node = null;
	private ManagedACTAccessRequirement accessRequirement = null;
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
		accessRequirement = new ManagedACTAccessRequirement();
		accessRequirement.setCreatedBy(individualGroup.getId());
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy(individualGroup.getId());
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setEtag("10");
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		RestrictableObjectDescriptor rod = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId());
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod, rod}));
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
		researchProjectDao.getUserOwnResearchProject(dto.getAccessRequirementId(), dto.getCreatedBy());
	}

	@Test
	public void testCRUD() {
		dto = ResearchProjectTestUtils.createNewDto();
		dto.setAccessRequirementId(accessRequirement.getId().toString());
		ResearchProject created = researchProjectDao.create(dto);
		dto.setId(created.getId());
		dto.setEtag(created.getEtag());
		assertEquals(dto, created);

		// should get back the same object
		assertEquals(dto, researchProjectDao.getUserOwnResearchProject(
				dto.getAccessRequirementId(), dto.getCreatedBy()));

		// update
		dto.setProjectLead("new projectLead");
		final ResearchProject updated = researchProjectDao.update(dto);
		dto.setEtag(updated.getEtag());
		assertEquals(dto, updated);

		// insert another one with the same accessRequirementId & createdBy
		try {
			researchProjectDao.create(dto);
			fail("should fail because of uniqueness constraint");
		} catch (IllegalArgumentException e){
			// as expected
		}

		// test get for update
		ResearchProject locked = transactionTemplate.execute(new TransactionCallback<ResearchProject>() {
			@Override
			public ResearchProject doInTransaction(TransactionStatus status) {
				// Try to lock both nodes out of order
				return researchProjectDao.getForUpdate(updated.getId());
			}
		});
		assertEquals(updated, locked);
		dto = updated;
	}

	@Test (expected = IllegalTransactionStateException.class)
	public void testGetForUpdateWithoutTransaction() {
		ResearchProject dto = ResearchProjectTestUtils.createNewDto();
		researchProjectDao.getForUpdate(dto.getId());
	}
}
