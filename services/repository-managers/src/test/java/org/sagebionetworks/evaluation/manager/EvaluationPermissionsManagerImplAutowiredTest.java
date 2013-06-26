package org.sagebionetworks.evaluation.manager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EvaluationPermissionsManagerImplAutowiredTest {

	@Autowired
	private AccessControlListDAO accessControlListDAO;
	@Autowired
	private AccessControlListDAO aclDAO;
	@Autowired
	private AccessControlListDAO someDao;

	@Test
	public void test() {
		System.out.println(System.identityHashCode(accessControlListDAO));
		System.out.println(System.identityHashCode(aclDAO));
		System.out.println(System.identityHashCode(someDao));
	}
}
