package org.sagebionetworks.evaluation.dbo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class EvaluationDBOTest {
 
    @Autowired
	private DBOBasicDao dboBasicDao;
 
    private long ownerId;
    private long id = 1000;
    private String name = "Foo";
    private String eTag = "Bar";
    private Long contentSource = KeyFactory.ROOT_ID;
 
    @BeforeEach
    public void before() {
    	ownerId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
    }
    
    @AfterEach
    public void after() throws DatastoreException {
        if(dboBasicDao != null){
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("id", id);
            dboBasicDao.deleteObjectByPrimaryKey(EvaluationDBO.class, params);
        }
    }
    
    @Test
    public void testCRUD() throws Exception {
        // Initialize a new competition
        EvaluationDBO eval = new EvaluationDBO();
        eval.setId(id);
        eval.seteTag(eTag);
        eval.setName(name);
        eval.setOwnerId(ownerId);
        eval.setCreatedOn(System.currentTimeMillis());
        eval.setContentSource(contentSource);
        eval.setStatusEnum(EvaluationStatus.PLANNED);
        eval.setDescription("my description".getBytes());
        eval.setSubmissionInstructionsMessage("foo".getBytes());
        eval.setSubmissionReceiptMessage("bar".getBytes());
 
        // Create it
        EvaluationDBO clone = dboBasicDao.createNew(eval);
        assertNotNull(clone);
        assertEquals(eval, clone);
        
        // Fetch it
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        clone = dboBasicDao.getObjectByPrimaryKey(EvaluationDBO.class, params);
        assertNotNull(clone);
        assertEquals(eval.getId(), clone.getId());
        assertEquals(eval.getName(), clone.getName());
        
		// Update it
		clone.setDescription("This is a new description".getBytes("UTF-8"));
		clone.setSubmissionInstructionsMessage("baz".getBytes());
		boolean result = dboBasicDao.update(clone);
		assertTrue(result);
		
		// Verify it
		params = new MapSqlParameterSource();
		params.addValue("id", clone.getId());
		EvaluationDBO clone2 = dboBasicDao.getObjectByPrimaryKey(EvaluationDBO.class, params);
		assertEquals(clone, clone2);
        
        // Delete it
        result = dboBasicDao.deleteObjectByPrimaryKey(EvaluationDBO.class,  params);
        assertTrue(result, "Failed to delete the type created"); 
    }
    
    @Test
    public void testSameName() throws Exception{
        // Initialize a new competition
        EvaluationDBO competition = new EvaluationDBO();
        competition.setId(id);
        competition.seteTag(eTag);
        competition.setName(name);
        competition.setOwnerId(ownerId);
        competition.setCreatedOn(System.currentTimeMillis());
        competition.setContentSource(contentSource);
        competition.setStatusEnum(EvaluationStatus.PLANNED);
 
        // Create it
        EvaluationDBO clone = dboBasicDao.createNew(competition);
        clone.setId(id + 1);
        
        assertThrows(IllegalArgumentException.class, () -> {
        	dboBasicDao.createNew(clone);
        });
    }
    
    @Test
    public void testTableMappingWithNullNullStartAndEndTimestamps() {
    	
        EvaluationDBO eval = new EvaluationDBO();
        
        eval.setId(id);
        eval.seteTag(eTag);
        eval.setName(name);
        eval.setOwnerId(ownerId);
        eval.setCreatedOn(System.currentTimeMillis());
        eval.setContentSource(contentSource);
        eval.setStatusEnum(EvaluationStatus.OPEN);
        eval.setDescription("my description".getBytes());
        eval.setSubmissionInstructionsMessage("foo".getBytes());
        eval.setSubmissionReceiptMessage("bar".getBytes());
        eval.setStartTimestamp(null);
        eval.setEndTimestamp(null);
        
        eval = dboBasicDao.createNew(eval);
        
 		SqlParameterSource params = new SinglePrimaryKeySqlParameterSource(eval.getId());
 		
 		// Call under test, this will in turn use the table mapping for the evaluation
 		EvaluationDBO fromDB = dboBasicDao.getObjectByPrimaryKey(EvaluationDBO.class, params);
 		
 		// Makes sure that the start and end timestamps are actually read as null (instead of 0) 
 		assertNull(fromDB.getStartTimestamp());
 		assertNull(fromDB.getEndTimestamp());
    }
 
}
