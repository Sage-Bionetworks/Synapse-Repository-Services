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
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRenewal;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dbo.dao.AccessRequirementUtilsTest;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBODataAccessRequestDAOImplTest {

	@Autowired
	UserGroupDAO userGroupDAO;

	@Autowired
	NodeDAO nodeDao;

	@Autowired
	AccessRequirementDAO accessRequirementDAO;

	@Autowired
	private ResearchProjectDAO researchProjectDao;

	@Autowired
	private DataAccessRequestDAO dataAccessRequestDao;

	@Autowired
	private IdGenerator idGenerator;

	private UserGroup individualGroup = null;
	private Node node = null;
	private ACTAccessRequirement accessRequirement = null;
	private ResearchProject researchProject = null;

	@Before
	public void before() {
		dataAccessRequestDao.truncateAll();

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

		// create a ResearchProject
		researchProject = ResearchProjectTestUtils.createNewDto();
		researchProject.setId(idGenerator.generateNewId(TYPE.RESEARCH_PROJECT_ID).toString());
		researchProject.setAccessRequirementId(accessRequirement.getId().toString());
		researchProject = researchProjectDao.create(researchProject);
	}

	@After
	public void after() {
		dataAccessRequestDao.truncateAll();

		if (researchProject != null) {
			researchProjectDao.delete(researchProject.getId());
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
		DataAccessRenewal dto = DataAccessRequestTestUtils.createNewDataAccessRenewal();
		dataAccessRequestDao.getCurrentRequest(dto.getAccessRequirementId(), dto.getCreatedBy());
	}

	@Test
	public void testCRUD() {
		DataAccessRenewal dto = DataAccessRequestTestUtils.createNewDataAccessRenewal();
		dto.setAccessRequirementId(accessRequirement.getId().toString());
		dto.setResearchProjectId(researchProject.getId());
		dto.setId(idGenerator.generateNewId(TYPE.DATA_ACCESS_REQUEST_ID).toString());
		assertEquals(dto, dataAccessRequestDao.create(dto));

		// should get back the same object
		DataAccessRenewal created = (DataAccessRenewal) dataAccessRequestDao.getCurrentRequest(dto.getAccessRequirementId(), dto.getCreatedBy());
		assertEquals(dto, created);

		// update
		dto.setSummaryOfUse("new summaryOfUse");
		assertEquals(dto, dataAccessRequestDao.update(dto));

		// insert another one with the same accessRequirementId & createdBy
		try {
			dataAccessRequestDao.create(dto);
			fail("should fail because of uniqueness constraint");
		} catch (IllegalArgumentException e){
			// as expected
		}
	}

}
