package org.sagebionetworks.evaluation.dbo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.evaluation.model.EvaluationRound;
import org.sagebionetworks.evaluation.model.EvaluationRoundLimit;
import org.sagebionetworks.evaluation.model.EvaluationRoundLimitType;

class EvaluationRoundDBOUtilTest {

	EvaluationRound evaluationRound;

	@BeforeEach
	public void setUp(){
		evaluationRound = new EvaluationRound();
		evaluationRound.setId("1234");
		evaluationRound.setEtag("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
		evaluationRound.setEvaluationId("9876");
		evaluationRound.setRoundStart(new Date());
		evaluationRound.setRoundEnd(new Date(System.currentTimeMillis() + 420));
		evaluationRound.setLimits(Arrays.asList(newLimit(EvaluationRoundLimitType.TOTAL, 54), newLimit(EvaluationRoundLimitType.WEEKLY, 34)));
	}

	@Test
	void testRoundTrip() {

		EvaluationRoundDBO dbo = EvaluationRoundDBOUtil.toDBO(evaluationRound);
		EvaluationRound roundTrip = EvaluationRoundDBOUtil.toDTO(dbo);

		assertEquals(evaluationRound, roundTrip);
	}

	@Test
	public void testToDBO_NullOrEmptyId(){
		evaluationRound.setId(null);
		String message = assertThrows(IllegalArgumentException.class, () ->
			EvaluationRoundDBOUtil.toDBO(evaluationRound)
		).getMessage();
		assertEquals("id is required and must not be the empty string.", message);

		evaluationRound.setId("");
		message = assertThrows(IllegalArgumentException.class, () ->
				EvaluationRoundDBOUtil.toDBO(evaluationRound)
		).getMessage();
		assertEquals("id is required and must not be the empty string.", message);
	}

	@Test
	public void testToDBO_NullOrEmptyEtag(){
		evaluationRound.setEtag(null);
		String message = assertThrows(IllegalArgumentException.class, () ->
				EvaluationRoundDBOUtil.toDBO(evaluationRound)
		).getMessage();
		assertEquals("etag is required and must not be the empty string.", message);

		evaluationRound.setEtag("");
		message = assertThrows(IllegalArgumentException.class, () ->
				EvaluationRoundDBOUtil.toDBO(evaluationRound)
		).getMessage();
		assertEquals("etag is required and must not be the empty string.", message);
	}

	@Test
	public void testToDBO_EmptyEvaluationId(){
		//not null guaranteed by json schema-to-pojo parser. only case where it could be null if it is constructed in java code and never set
		evaluationRound.setEvaluationId("");
		String message = assertThrows(IllegalArgumentException.class, () ->
				EvaluationRoundDBOUtil.toDBO(evaluationRound)
		).getMessage();
		assertEquals("evaluationId is required and must not be the empty string.", message);
	}

	@Test
	public void testToDBO_NullOrEmptyLimitsList(){
		evaluationRound.setLimits(null);
		EvaluationRoundDBO dbo = EvaluationRoundDBOUtil.toDBO(evaluationRound);
		assertNull(dbo.getLimitsJson());

		evaluationRound.setLimits(Collections.emptyList());
		dbo = EvaluationRoundDBOUtil.toDBO(evaluationRound);
		assertNull(dbo.getLimitsJson());
	}

	@Test
	public void testToDTO_NullOrEmptyLimitsJsonString(){
		EvaluationRoundDBO dbo = EvaluationRoundDBOUtil.toDBO(evaluationRound);
		assertNotNull(dbo.getLimitsJson());

		dbo.setLimitsJson(null);
		EvaluationRound dto = EvaluationRoundDBOUtil.toDTO(dbo);
		assertNull(dto.getLimits());

		dbo.setLimitsJson("");
		dto = EvaluationRoundDBOUtil.toDTO(dbo);
		assertNull(dto.getLimits());
	}

	@Test
	public void testToDTO_JsonStringContainsNullElement(){
		EvaluationRoundDBO dbo = EvaluationRoundDBOUtil.toDBO(evaluationRound);
		assertNotNull(dbo.getLimitsJson());

		dbo.setLimitsJson("[{\"limitType\":\"TOTAL\", \"maximumSubmissions\":42},null]");
		String message = assertThrows(IllegalStateException.class, () ->
				EvaluationRoundDBOUtil.toDTO(dbo)
		).getMessage();

		assertEquals("null value should not have been stored", message);
	}

	private EvaluationRoundLimit newLimit(EvaluationRoundLimitType type, long maximumSubmissions){
		EvaluationRoundLimit limit = new EvaluationRoundLimit();
		limit.setLimitType(EvaluationRoundLimitType.TOTAL);
		limit.setMaximumSubmissions(57L);
		return limit;
	}
}