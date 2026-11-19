package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.helper.NodeDaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class AggregateDataSaltDaoImplAutowiredTest {

	private static final String SALT_ONE = "6f1c4a9e-2b7d-4e3a-9c51-0d8e7f6a5b41";
	private static final String SALT_TWO = "a3e8d2c1-7f4b-4a96-b0e5-91c2d3f4e5a6";

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private NodeDaoObjectHelper nodeHelper;

	@Autowired
	private AggregateDataSaltDao dao;

	private long sourceOneId;
	private long sourceTwoId;

	@BeforeEach
	public void before() {
		dao.truncateAll();
		nodeDao.truncateAll();
		sourceOneId = createTable();
		sourceTwoId = createTable();
	}

	@AfterEach
	public void after() {
		dao.truncateAll();
		nodeDao.truncateAll();
	}

	private long createTable() {
		return KeyFactory.stringToKey(nodeHelper.create(node -> node.setNodeType(EntityType.table)).getId());
	}

	@Test
	public void testGetSaltWithMissing() {
		// call under test
		assertEquals(Optional.empty(), dao.getSalt(sourceOneId));
	}

	@Test
	public void testCreateSaltIfAbsentWithNoExistingRow() {
		// call under test
		String salt = dao.createSaltIfAbsent(sourceOneId, SALT_ONE);

		assertEquals(SALT_ONE, salt);
		assertEquals(Optional.of(SALT_ONE), dao.getSalt(sourceOneId));
		assertEquals(Optional.empty(), dao.getSalt(sourceTwoId));
	}

	@Test
	public void testCreateSaltIfAbsentWithExistingRow() {
		dao.createSaltIfAbsent(sourceOneId, SALT_ONE);

		// call under test
		String salt = dao.createSaltIfAbsent(sourceOneId, SALT_TWO);

		// The first salt is never replaced.
		assertEquals(SALT_ONE, salt);
		assertEquals(Optional.of(SALT_ONE), dao.getSalt(sourceOneId));
	}

	@Test
	public void testCreateSaltIfAbsentWithMultipleSources() {
		// call under test
		String saltOne = dao.createSaltIfAbsent(sourceOneId, SALT_ONE);
		String saltTwo = dao.createSaltIfAbsent(sourceTwoId, SALT_TWO);

		assertEquals(SALT_ONE, saltOne);
		assertEquals(SALT_TWO, saltTwo);
		assertEquals(Optional.of(SALT_ONE), dao.getSalt(sourceOneId));
		assertEquals(Optional.of(SALT_TWO), dao.getSalt(sourceTwoId));
	}

	@Test
	public void testCreateSaltIfAbsentWithNonExistentNode() {
		long missingNodeId = sourceTwoId + 1_000_000;

		assertThrows(DataIntegrityViolationException.class, () -> {
			// call under test
			dao.createSaltIfAbsent(missingNodeId, SALT_ONE);
		});

		assertEquals(Optional.empty(), dao.getSalt(missingNodeId));
	}

	@Test
	public void testCreateSaltIfAbsentWithBlankSalt() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.createSaltIfAbsent(sourceOneId, " ");
		}).getMessage();

		assertEquals("candidateSalt is required and must not be a blank string.", message);
		assertEquals(Optional.empty(), dao.getSalt(sourceOneId));
	}

	@Test
	public void testSaltDeletedWithNode() {
		dao.createSaltIfAbsent(sourceOneId, SALT_ONE);
		dao.createSaltIfAbsent(sourceTwoId, SALT_TWO);

		nodeDao.delete(KeyFactory.keyToString(sourceOneId));

		assertEquals(Optional.empty(), dao.getSalt(sourceOneId));
		assertEquals(Optional.of(SALT_TWO), dao.getSalt(sourceTwoId));
	}
}
