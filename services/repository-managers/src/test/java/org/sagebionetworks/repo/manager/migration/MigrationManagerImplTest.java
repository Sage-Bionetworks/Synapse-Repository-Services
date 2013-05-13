package org.sagebionetworks.repo.manager.migration;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigatableTableDAO;

/**
 * The Unit test for MigrationManagerImpl;
 * @author jmhill
 *
 */
public class MigrationManagerImplTest {
	
	private MigatableTableDAO mockDao;
	MigrationManagerImpl manager;
	
	@Before
	public void before(){
		mockDao = Mockito.mock(MigatableTableDAO.class);
		manager = new MigrationManagerImpl(mockDao, 10);
	}
	
	@Test
	public void testGetBackupDataBatched(){
		// Set the batch size to be two for this test
		manager.setBackupBatchMax(2);
		// This is the full list of data we expect
		List<Long> fullList = Arrays.asList(1l,2l,3l,4l,5l);
		List<StubDatabaseObject> expected = createExpected(fullList);
		List<Long> batchOne = Arrays.asList(1l,2l);
		List<Long> batchTwo = Arrays.asList(3l,4l);
		List<Long> batchThree = Arrays.asList(5l);
		// We expect the dao to be called three times, once for each batch.
		when(mockDao.getBackupBatch(StubDatabaseObject.class, batchOne)).thenReturn(expected.subList(0, 2));
		when(mockDao.getBackupBatch(StubDatabaseObject.class, batchTwo)).thenReturn(expected.subList(2, 4));
		when(mockDao.getBackupBatch(StubDatabaseObject.class, batchThree)).thenReturn(expected.subList(4, 5));
		// Make the real call
		List<StubDatabaseObject> resutls = manager.getBackupDataBatched(StubDatabaseObject.class, fullList);
		assertNotNull(resutls);
		// The result should match the expected
		assertEquals(expected, resutls);
	}
	
	/**
	 * Build a list of objects from a list of IDs
	 * @param fullList
	 * @return
	 */
	private List<StubDatabaseObject> createExpected(List<Long> fullList) {
		List<StubDatabaseObject> result = new LinkedList<StubDatabaseObject>();
		for(Long id: fullList){
			result.add(new StubDatabaseObject(id));
		}
		return result;
	}

	/**
	 * Simple Stub DatabaseObject for testing.
	 * @author jmhill
	 *
	 */
	public static class StubDatabaseObject implements DatabaseObject<StubDatabaseObject>{
		
		Long id;
		
		public StubDatabaseObject(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Override
		public TableMapping<StubDatabaseObject> getTableMapping() {
			return null;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			StubDatabaseObject other = (StubDatabaseObject) obj;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "StubDatabaseObject [id=" + id + "]";
		}
		
	}

}
