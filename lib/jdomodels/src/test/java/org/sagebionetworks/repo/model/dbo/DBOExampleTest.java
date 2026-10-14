package org.sagebionetworks.repo.model.dbo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.config.DatabaseInfrastructureConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Integration test for DBOBasicDaoImpl using only DBOExample as the registered DBO.
 * Uses real database connections but minimal Spring context.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { DBOExampleTest.TestConfig.class })
public class DBOExampleTest {

	@Configuration
	@Import({ DatabaseInfrastructureConfiguration.class })
	static class TestConfig {

		@Bean
		public DDLUtils ddlUtils(JdbcTemplate jdbcTemplate, StackConfiguration stackConfiguration) {
			return new DDLUtilsImpl(jdbcTemplate, stackConfiguration);
		}

		@Bean
		public DBOBasicDao dboBasicDao(DDLUtils ddlUtils, JdbcTemplate jdbcTemplate,
				NamedParameterJdbcTemplate namedJdbcTemplate) throws Exception {
			// Register only DBOExample
			List<DatabaseObject> databaseObjectRegister = Collections.singletonList(new DBOExample());

			// No MySQL functions needed for this test
			Map<String, String> functionMap = new HashMap<>();

			DBOBasicDaoImpl dao = new DBOBasicDaoImpl(ddlUtils, jdbcTemplate, namedJdbcTemplate,
					databaseObjectRegister, functionMap);

			// Initialize the DAO (validates tables and builds SQL cache)
			dao.afterPropertiesSet();

			return dao;
		}
	}

	@Autowired
	private DBOBasicDao dboBasicDao;

	private List<Long> toDelete;

	@BeforeEach
	public void before() {
		assertNotNull(dboBasicDao);
		toDelete = new LinkedList<>();
	}

	@AfterEach
	public void after() {
		if (dboBasicDao != null && toDelete != null) {
			for (Long id : toDelete) {
				try {
					MapSqlParameterSource params = new MapSqlParameterSource();
					params.addValue("id", id);
					dboBasicDao.deleteObjectByPrimaryKey(DBOExample.class, params);
				} catch (DatastoreException e) {
				}
			}
		}
	}

	@Test
	public void testInsertMissingValues() throws DatastoreException {
		DBOExample example = new DBOExample();
		assertThrows(IllegalArgumentException.class, () -> {
			dboBasicDao.createNew(example);
		});
	}

	@Test
	public void testInsert() throws DatastoreException, UnsupportedEncodingException {
		DBOExample example = new DBOExample();
		example.setNumber(45L);
		example.setModifiedBy("you");
		example.setModifiedOn(System.currentTimeMillis());
		example.setBlob("This string converts to a blob".getBytes("UTF-8"));

		// call under test
		example = dboBasicDao.createNew(example);

		assertNotNull(example);
		// This class is auto-increment so it should have an id now
		assertNotNull(example.getId());
		toDelete.add(example.getId());
	}

	@Test
	public void testInsertBatch() throws DatastoreException, UnsupportedEncodingException {
		int batchSize = 6;
		List<DBOExample> batch = new ArrayList<>();
		for (int i = 0; i < batchSize; i++) {
			DBOExample example = new DBOExample();
			example.setNumber((long) i);
			example.setModifiedBy("name" + i);
			example.setModifiedOn(System.currentTimeMillis());
			example.setBlob("This string converts to a blob".getBytes("UTF-8"));
			batch.add(example);
		}

		// call under test
		batch = dboBasicDao.createBatch(batch);

		// Make sure each has an id
		for (DBOExample created : batch) {
			assertNotNull(created.getId());
			toDelete.add(created.getId());
			// Check the results
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("id", created.getId());
			DBOExample clone = dboBasicDao.getObjectByPrimaryKey(DBOExample.class, params).get();
			assertNotNull(clone);
			assertEquals(created, clone);
		}
	}

	@Test
	public void testGetById() throws Exception {
		DBOExample example = new DBOExample();
		example.setNumber(103L);
		example.setModifiedBy("nobodyKnows");
		example.setModifiedOn(System.currentTimeMillis());
		example.setBlob("This string converts to a blob".getBytes("UTF-8"));
		example = dboBasicDao.createNew(example);
		assertNotNull(example);
		toDelete.add(example.getId());
		assertNotNull(example.getId());

		// call under test
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", example.getId());
		DBOExample clone = dboBasicDao.getObjectByPrimaryKey(DBOExample.class, params).get();

		assertNotNull(clone);
		assertEquals(example, clone);
	}

	@Test
	public void testGetByDoesNotExist() throws Exception {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", 33344L);

		// call under test
		assertEquals(Optional.empty(), dboBasicDao.getObjectByPrimaryKey(DBOExample.class, params));
	}

	@Test
	public void testDeleteId() throws DatastoreException, UnsupportedEncodingException {
		DBOExample example = new DBOExample();
		example.setNumber(133L);
		example.setModifiedBy("nobodyKnows");
		example.setModifiedOn(System.currentTimeMillis());
		example.setBlob("This string converts to a blob".getBytes("UTF-8"));
		example = dboBasicDao.createNew(example);
		assertNotNull(example);
		toDelete.add(example.getId());
		assertNotNull(example.getId());

		// call under test
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", example.getId());
		boolean result = dboBasicDao.deleteObjectByPrimaryKey(DBOExample.class, params);

		assertTrue(result);
	}

	@Test
	public void testUpdate() throws Exception {
		// First create the object.
		DBOExample example = new DBOExample();
		example.setNumber(456L);
		example.setModifiedBy("snoopy");
		example.setModifiedOn(System.currentTimeMillis());
		example.setBlob("This is the starting string".getBytes("UTF-8"));
		example = dboBasicDao.createNew(example);
		assertNotNull(example);
		toDelete.add(example.getId());
		// Get it back from the DB
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", example.getId());
		DBOExample fetched = dboBasicDao.getObjectByPrimaryKey(DBOExample.class, params).get();
		assertEquals(example, fetched);
		// Now change the value
		fetched.setBlob("I am the new string for the blob!".getBytes("UTF-8"));
		fetched.setModifiedOn(fetched.getModifiedOn() + 10);
		fetched.setModifiedBy("Sombody else");

		// call under test
		boolean result = dboBasicDao.update(fetched);

		assertTrue(result);
		// Fetch it back
		DBOExample clone = dboBasicDao.getObjectByPrimaryKey(DBOExample.class, params).get();
		assertEquals(fetched, clone);
	}
}
