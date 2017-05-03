package org.sagebionetworks.repo.manager.migration;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.test.util.ReflectionTestUtils;

public class ACLMigrationListenerTest {
	
	@Mock
	private AccessControlListDAO aclDAO;
	
	private ACLMigrationListener listener;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		listener = new ACLMigrationListener();

		ReflectionTestUtils.setField(listener, "aclDAO", aclDAO);
	}

	@Test
	public void testMigrationListener() {
		List<DBOAccessControlList> delta = new ArrayList<DBOAccessControlList>();
		
		// method under test
		listener.afterCreateOrUpdate(MigrationType.ACL, delta);
		
		verify(aclDAO, never()).update(any(AccessControlList.class), eq(ObjectType.ENTITY));
	}

}
