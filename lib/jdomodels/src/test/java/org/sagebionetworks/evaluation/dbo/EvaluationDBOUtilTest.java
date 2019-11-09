package org.sagebionetworks.evaluation.dbo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.SubmissionQuota;

public class EvaluationDBOUtilTest {
    
    @Test
    public void testDtoToDbo() {
    	Evaluation evalDTO = new Evaluation();
    	Evaluation evalDTOclone = new Evaluation();
    	EvaluationDBO evalDBO = new EvaluationDBO();
    	EvaluationDBO evalDBOclone = new EvaluationDBO();
    	
    	evalDTO.setContentSource("syn123");
    	evalDTO.setCreatedOn(new Date());
    	evalDTO.setDescription("description");
    	evalDTO.setEtag("eTag");
    	evalDTO.setId("123");
    	evalDTO.setName("name");
    	evalDTO.setOwnerId("456");
    	evalDTO.setStatus(EvaluationStatus.OPEN);
    	SubmissionQuota quota = new SubmissionQuota();
    	quota.setSubmissionLimit(10L);
    	Date firstRoundStart = new Date();
    	quota.setFirstRoundStart(firstRoundStart);
    	quota.setNumberOfRounds(100L);
    	quota.setRoundDurationMillis(60000L);
    	evalDTO.setQuota(quota);
    	evalDTO.setSubmissionInstructionsMessage("some instructions");
    	evalDTO.setSubmissionReceiptMessage("some receipt");
    	    	
    	// method under test
    	EvaluationDBOUtil.copyDtoToDbo(evalDTO, evalDBO);
    	// method under test
    	EvaluationDBOUtil.copyDboToDto(evalDBO, evalDTOclone);
    	// method under test
    	EvaluationDBOUtil.copyDtoToDbo(evalDTOclone, evalDBOclone);
    	
    	assertEquals(evalDTO, evalDTOclone);
    	assertEquals(evalDBO, evalDBOclone);
    	
    	assertEquals(firstRoundStart.getTime(), evalDBO.getStartTimestamp());
    	assertEquals(firstRoundStart.getTime()+60000L*100L, evalDBO.getEndTimestamp());
    	
    }
}
