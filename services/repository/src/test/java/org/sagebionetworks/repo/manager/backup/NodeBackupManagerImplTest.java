package org.sagebionetworks.repo.manager.backup;

import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Unit tests for NodeBackupManagerImpl
 * @author jmhill
 *
 */
public class NodeBackupManagerImplTest {
	
	@Test
	public void testForPLFM_844() throws DatastoreException, NotFoundException{
		NodeBackupManagerImpl manager = new NodeBackupManagerImpl();
		// We want to use a mock dao here since we do not really want to delete all entites just for this test.
		NodeDAO mockNodDao = Mockito.mock(NodeDAO.class);
		manager.nodeDao = mockNodDao;
		when(mockNodDao.getNodeIdForPath((NodeConstants.ROOT_FOLDER_PATH))).thenReturn(null);
		when(mockNodDao.delete(null)).thenThrow(new IllegalArgumentException("Id cannot be null"));
		// Call clear all
		manager.clearAllData();
	}

}
