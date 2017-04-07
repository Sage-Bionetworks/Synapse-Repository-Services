package org.sagebionetworks.ids;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:id-generator.spb.xml" })
public class IdGeneratorImplTest {
	
	@Autowired
	IdGenerator idGenerator;
		
	@Test
	public void testNewId(){
		assertNotNull(idGenerator);
		// Create a few IDs
		Set<Long> unique = new HashSet<Long>();
		int toCreate = 1;
		for(int i=0; i<toCreate;i++){
			long start = System.currentTimeMillis();
			Long id = idGenerator.generateNewId(IdType.ENTITY_ID);
			long end = System.currentTimeMillis();
			assertTrue(unique.add(id));
			assertTrue("All IDs must be larger than the starting ID.",id > IdType.ENTITY_ID.getStartingId());
			System.out.println("ID: "+id+" in "+(end-start)+" ms");
		}
	}
	
	@Test
	public void testReserveId(){
		// Start with the current ID.
		Long id = idGenerator.generateNewId(IdType.ENTITY_ID);
		// Reserve this ID + 10
		Long reserved = id+10;
		idGenerator.reserveId(reserved, IdType.ENTITY_ID);
		// Now get make sure the next ID is greater than the reserve
		Long next = idGenerator.generateNewId(IdType.ENTITY_ID);
		assertEquals(next.longValue(), reserved.longValue()+1);
	}
	
	@Test
	public void testReserveIdLessThan(){
		// Start with the current ID.
		Long id = idGenerator.generateNewId(IdType.ENTITY_ID);
		// Reserve this ID
		Long reserved = id;
		// This time the ID is already reserved so this method should be a wash.
		idGenerator.reserveId(reserved, IdType.ENTITY_ID);
		// The next ID should just be the ID + 1
		Long next = idGenerator.generateNewId(IdType.ENTITY_ID);
		assertEquals(next.longValue(), id.longValue()+1);
	}
	
	@Test
	public void testGenerateBatchNewIds(){
		Long startId = idGenerator.generateNewId(IdType.ENTITY_ID);
		int count = 3;
		// Call under test
		BatchOfIds range = idGenerator.generateBatchNewIds(IdType.ENTITY_ID, count);
		assertNotNull(range);
		assertEquals(new Long(startId+1L), range.getFirstId());
		assertEquals(new Long(startId+count), range.getLastId());
		// next Id should be after range
		Long nextId = idGenerator.generateNewId(IdType.ENTITY_ID);
		assertEquals(new Long(range.getLastId()+1), nextId);
	}
	
	@Test
	public void testGenerateBatchNewIdsSizeOfOne(){
		Long startId = idGenerator.generateNewId(IdType.ENTITY_ID);
		int count = 1;
		// Call under test
		BatchOfIds range = idGenerator.generateBatchNewIds(IdType.ENTITY_ID, count);
		assertNotNull(range);
		assertEquals(new Long(startId+1L), range.getFirstId());
		assertEquals(new Long(startId+1L), range.getLastId());
		// next Id should be after range
		Long nextId = idGenerator.generateNewId(IdType.ENTITY_ID);
		assertEquals(new Long(range.getLastId()+1), nextId);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGenerateBatchNewIdsCountTooSmall(){
		int count = 0;
		// Call under test
		idGenerator.generateBatchNewIds(IdType.ENTITY_ID, count);
	}

}
