package org.sagebionetworks.repo.model.dbo.dao.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.migration.BatchUtility;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * Test for the BatchUtility.
 * @author John
 *
 */
public class BatchUtilityTest {
	
	@Test
	public void testPrepareBatches(){
		List<SpaceFiller> batchToInsert = new LinkedList<SpaceFiller>();
		batchToInsert.add(new SpaceFiller(new byte[10]));
		batchToInsert.add(new SpaceFiller(new byte[20]));
		batchToInsert.add(new SpaceFiller(new byte[30]));
		// Prepare the batches
		int maxBytes = 40;
		List<SqlParameterSource[]> results = BatchUtility.prepareBatches(batchToInsert, maxBytes);
		// For this case the first two should be in a batch and the third should be in its own
		assertNotNull(results);
		assertEquals(2, results.size());
		assertEquals(2, results.get(0).length);
		assertEquals(1, results.get(1).length);
	}
	
	@Test
	public void testPrepareBatchesAllOver(){
		List<SpaceFiller> batchToInsert = new LinkedList<SpaceFiller>();
		batchToInsert.add(new SpaceFiller(new byte[10]));
		batchToInsert.add(new SpaceFiller(new byte[20]));
		batchToInsert.add(new SpaceFiller(new byte[30]));
		// Prepare the batches
		int maxBytes = 5;
		List<SqlParameterSource[]> results = BatchUtility.prepareBatches(batchToInsert, maxBytes);
		// For this case the first two should be in a batch and the third should be in its own
		assertNotNull(results);
		assertEquals(3, results.size());
		assertEquals(1, results.get(0).length);
		assertEquals(1, results.get(1).length);
		assertEquals(1, results.get(2).length);
	}
	
	@Test
	public void testPrepareBatchesOneOver(){
		List<SpaceFiller> batchToInsert = new LinkedList<SpaceFiller>();
		batchToInsert.add(new SpaceFiller(new byte[5]));
		batchToInsert.add(new SpaceFiller(new byte[5]));
		batchToInsert.add(new SpaceFiller(new byte[100]));
		batchToInsert.add(new SpaceFiller(new byte[5]));
		batchToInsert.add(new SpaceFiller(new byte[5]));
		// Prepare the batches
		int maxBytes = 10;
		List<SqlParameterSource[]> results = BatchUtility.prepareBatches(batchToInsert, maxBytes);
		// For this case the first two should be in a batch and the third should be in its own
		assertNotNull(results);
		assertEquals(3, results.size());
		assertEquals(2, results.get(0).length);
		assertEquals(1, results.get(1).length);
		assertEquals(2, results.get(2).length);
	}
	
	@Test
	public void testPrepareBatchesAllUnder(){
		List<SpaceFiller> batchToInsert = new LinkedList<SpaceFiller>();
		batchToInsert.add(new SpaceFiller(new byte[5]));
		batchToInsert.add(new SpaceFiller(new byte[5]));
		batchToInsert.add(new SpaceFiller(new byte[5]));
		batchToInsert.add(new SpaceFiller(new byte[5]));
		// Prepare the batches
		int maxBytes = 20;
		List<SqlParameterSource[]> results = BatchUtility.prepareBatches(batchToInsert, maxBytes);
		// For this case the first two should be in a batch and the third should be in its own
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(4, results.get(0).length);
	}
	
	/**
	 * Used as a batch parameter.
	 * 
	 * @author John
	 *
	 */
	public static class SpaceFiller {

		private byte[] bytes;
		
		public SpaceFiller(byte[] bytes) {
			super();
			this.bytes = bytes;
		}

		public byte[] getBytes() {
			return bytes;
		}

		public void setBytes(byte[] bytes) {
			this.bytes = bytes;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(bytes);
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
			SpaceFiller other = (SpaceFiller) obj;
			if (!Arrays.equals(bytes, other.bytes))
				return false;
			return true;
		}
		
	}
}
