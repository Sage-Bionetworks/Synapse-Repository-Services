package org.sagebionetworks.evaluation.dbo;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
class EvaluationRoundDBOTest {
	@Autowired
	private DBOBasicDao dboBasicDao;

	Long id;
	Long evaluationId;

	@BeforeEach
	void setUp() {
		id = 12345L;
		evaluationId = 9098770L;

		EvaluationDBO eval = new EvaluationDBO();
		eval.setId(evaluationId);
		eval.seteTag("eeeeeeeeeeeeeeeeeeeeee");
		eval.setName("ffffffffffffffffffffff");
		eval.setOwnerId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		eval.setCreatedOn(System.currentTimeMillis());
		eval.setStatusEnum(EvaluationStatus.PLANNED);
		eval.setDescription("my description".getBytes());
		eval.setContentSource(KeyFactory.ROOT_ID);

		dboBasicDao.createNew(eval);
	}

	@AfterEach
	void tearDown() {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", evaluationId);
		dboBasicDao.deleteObjectByPrimaryKey(EvaluationDBO.class, params);
	}

	@Test
	public void testCRUD(){
		EvaluationRoundDBO evaluationRoundDBO = new EvaluationRoundDBO();
		evaluationRoundDBO.setEtag("eeeeetag");
		evaluationRoundDBO.setId(id);
		evaluationRoundDBO.setEvaluationId(evaluationId);
		evaluationRoundDBO.setLimitsJson(null);
		Instant now = Instant.now();
		evaluationRoundDBO.setRoundStart(Timestamp.from(now));
		evaluationRoundDBO.setRoundEnd(Timestamp.from(now.plusSeconds(43 )));

		//create
		EvaluationRoundDBO created = dboBasicDao.createNew(evaluationRoundDBO);
		assertEquals(evaluationRoundDBO, created);

		//retrieve
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", id);
		EvaluationRoundDBO retrieved = dboBasicDao.getObjectByPrimaryKey(EvaluationRoundDBO.class, params);
		assertEquals(evaluationRoundDBO, retrieved);

		//update
		evaluationRoundDBO.setLimitsJson("[{\"limitType\": \"TOTAL\", \"maximumSubmissions\": 23}]");
		boolean wasUpdated = dboBasicDao.update(evaluationRoundDBO);
		assertTrue(wasUpdated);
		EvaluationRoundDBO updated = dboBasicDao.getObjectByPrimaryKey(EvaluationRoundDBO.class, params);
		assertEquals(evaluationRoundDBO, updated);

		//delete
		boolean wasDeleted = dboBasicDao.deleteObjectByPrimaryKey(EvaluationRoundDBO.class,  params);
		assertTrue(wasDeleted);

		Optional<EvaluationRoundDBO> notExists = dboBasicDao.getObjectByPrimaryKeyIfExists(EvaluationRoundDBO.class, params);
		assertFalse(notExists.isPresent());
	}
}