package org.sagebionetworks.repo.manager.table;

import org.junit.Before;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.springframework.test.util.ReflectionTestUtils;

public class TableRowManagerImplTest {
	
	TableRowTruthDAO mockTruthDao;
	AuthorizationManager mockAuthManager;
	TableRowManagerImpl manager;
	
	@Before
	public void before(){
		mockTruthDao = Mockito.mock(TableRowTruthDAO.class);
		mockAuthManager = Mockito.mock(AuthorizationManager.class);
		manager = new TableRowManagerImpl();
		ReflectionTestUtils.setField(manager, "tableRowTruthDao", mockTruthDao);
	}

}
