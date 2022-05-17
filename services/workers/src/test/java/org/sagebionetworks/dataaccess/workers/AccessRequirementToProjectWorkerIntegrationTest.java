package org.sagebionetworks.dataaccess.workers;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class AccessRequirementToProjectWorkerIntegrationTest {

	private static final long WORKER_TIMEOUT = 1 * 60 * 1000;
	
	@Autowired
	private NodeDAO nodeDao;
	
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;

	@Autowired
	private DaoObjectHelper<Node> nodeDaoHelper;

	@Autowired
	private DaoObjectHelper<TermsOfUseAccessRequirement> termsOfUseHelper;
	
	@BeforeEach
	public void before() {
		nodeDao.truncateAll();
		accessRequirementDAO.clear();
	}
	
	@AfterEach
	public void after() {
		nodeDao.truncateAll();
		accessRequirementDAO.clear();
	}
	
	@Test
	public void testRoundTrip() throws Exception {
		
		Node project = nodeDaoHelper.create((n)->{
			n.setNodeType(EntityType.project);
		});
			
		TermsOfUseAccessRequirement ar = termsOfUseHelper.create((t)->{
			t.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor().setId(project.getId()).setType(RestrictableObjectType.ENTITY)));
		});
		
		List<Long> expectedProjects = Arrays.asList(KeyFactory.stringToKey(project.getId()));
		
		TimeUtils.waitFor(WORKER_TIMEOUT, 1000L, () -> {
			
			List<Long> projects = accessRequirementDAO.getAccessRequirementProjectsMap(Set.of(ar.getId())).get(ar.getId());
			
			return new Pair<>(expectedProjects.equals(projects), null);
		});
	}
}
