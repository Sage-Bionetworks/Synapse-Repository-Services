package org.sagebionetworks.ids;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.NamedIdGenerator.NamedType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:id-generator.spb.xml" })
public class NamedIdGeneratorTest {

	@Autowired
	NamedIdGenerator namedIdGenerator;
	
	@Before
	public void before(){
		namedIdGenerator.truncateTable(NamedType.NAME_TEST_ID);
	}
	
	@Test
	public void testGenerateNewId(){
		String name = "namedIdGeneratorTestUser";
		Long id = namedIdGenerator.generateNewId(name, NamedType.NAME_TEST_ID);
		assertNotNull(id);
		// If we generate another with the same name we should get back the same ID.
		Long idTwo = namedIdGenerator.generateNewId(name, NamedType.NAME_TEST_ID);
		assertEquals("The same name should have generated the same ID",id, idTwo);
	}
	
	@Test
	public void testUnconditionallyAssignIdToName(){
		// First assign this name to an ID
		String name = "testUnconditionallyAssignIdToName";
		// Now assign the same name to a different ID;
		Long id = new Long(123);
		namedIdGenerator.unconditionallyAssignIdToName(123l, name,  NamedType.NAME_TEST_ID);
		Long newId = namedIdGenerator.generateNewId(name, NamedType.NAME_TEST_ID);
		assertEquals(id, newId);
	}
	
	@Test
	public void testUnconditionallyAssignIdToNameWithNameAlreadyAssigned(){
		// First assign this name to an ID
		String name = "testUnconditionallyAssignIdToNameWithNameAlreadyAssigned";
		Long id = new Long(123);
		// Now assign the same name to a different ID;
		namedIdGenerator.unconditionallyAssignIdToName(id, name,  NamedType.NAME_TEST_ID);
		// Now assign the same name to a different ID;
		namedIdGenerator.unconditionallyAssignIdToName(id+1, name,  NamedType.NAME_TEST_ID);
		Long newId = namedIdGenerator.generateNewId(name, NamedType.NAME_TEST_ID);
		assertEquals(new Long(id+1), newId);
	}
	
	@Test
	public void testUnconditionallyAssignIdToNameWithIdAlreadyAssigned(){
		// First assign this name to an ID
		String name = "one";
		Long id = new Long(123);
		// Now assign the same name to a different ID;
		namedIdGenerator.unconditionallyAssignIdToName(id, name,  NamedType.NAME_TEST_ID);
		// Now assign the same ID to a different name
		namedIdGenerator.unconditionallyAssignIdToName(id, "two",  NamedType.NAME_TEST_ID);
		Long newId = namedIdGenerator.generateNewId("two", NamedType.NAME_TEST_ID);
		assertEquals(id, newId);
	}
	
	@Test
	public void testUnconditionallyAssignIdToNameIdempotent(){
		// First assign this name to an ID
		String name = "one";
		Long id = new Long(123);
		// Now assign the same name to a different ID;
		namedIdGenerator.unconditionallyAssignIdToName(id, name,  NamedType.NAME_TEST_ID);
		// Assigning this again should not fail.
		namedIdGenerator.unconditionallyAssignIdToName(id, name,  NamedType.NAME_TEST_ID);
	}
}
