package org.sagebionetworks.evaluation.dbo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class SubmissionDBOTest {
 
    @Autowired
    DBOBasicDao dboBasicDao;
	@Autowired
	NodeDAO nodeDAO;
	@Autowired
	FileHandleDao fileHandleDAO;
    @Autowired
    IdGenerator idGenerator;
 
    private String nodeId;
    private long userId;
    
    private long submissionId = 2000;
    private long evalId;
    private String fileHandleId;
    private String name = "test submission";
    
    @Before
    public void setUp() throws DatastoreException, InvalidModelException, NotFoundException {
    	userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
    	
    	// create a file handle
		PreviewFileHandle meta = new PreviewFileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy("" + userId);
		meta.setFileName("preview.jpg");
		meta.setEtag(UUID.randomUUID().toString());
		meta.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		fileHandleId = fileHandleDAO.createFile(meta).getId();
		
    	// create a node
  		Node toCreate = NodeTestUtils.createNew(name, userId);
    	toCreate.setVersionComment("This is the first version of the first node ever!");
    	toCreate.setVersionLabel("0.0.1");
    	toCreate.setFileHandleId(fileHandleId);
    	nodeId = nodeDAO.createNew(toCreate).substring(3); // trim "syn" from node ID    
    	
        // Initialize a new Evaluation
        EvaluationDBO evaluation = new EvaluationDBO();
        evaluation.setId(evalId);
        evaluation.seteTag("etag");
        evaluation.setName("name");
        evaluation.setOwnerId(userId);
        evaluation.setContentSource(KeyFactory.ROOT_ID);
        evaluation.setCreatedOn(System.currentTimeMillis());
        evaluation.setStatusEnum(EvaluationStatus.PLANNED);
        evalId = dboBasicDao.createNew(evaluation).getId();
    }
    
    @After
    public void after() throws DatastoreException {
        if(dboBasicDao != null) {
        	// delete submission
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("id", submissionId);
            dboBasicDao.deleteObjectByPrimaryKey(SubmissionDBO.class, params);
            
            // delete Evaluation
            params = new MapSqlParameterSource();
            params.addValue("id", evalId);
            dboBasicDao.deleteObjectByPrimaryKey(EvaluationDBO.class, params);
        }
    	try {
    		nodeDAO.delete(nodeId);
    	} catch (NotFoundException e) {};
    	fileHandleDAO.delete(fileHandleId);
    }
    
    @Test
    public void testCRD() throws Exception{
        // Initialize a new Submission
        SubmissionDBO submission = new SubmissionDBO();
        submission.setId(submissionId);
        submission.setName(name);
        submission.setEntityId(Long.parseLong(nodeId));
        submission.setVersionNumber(1L);
        submission.setUserId(userId);
        submission.setSubmitterAlias("Team Awesome");
        submission.setEvalId(evalId);
        submission.setCreatedOn(System.currentTimeMillis());
        submission.setEntityBundle(JDOSecondaryPropertyUtils.compressObject(submission));
 
        // Create it
        SubmissionDBO clone = dboBasicDao.createNew(submission);
        assertNotNull(clone);
        assertEquals(submission, clone);
        
        // Fetch it
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id",submissionId);
        SubmissionDBO clone2 = dboBasicDao.getObjectByPrimaryKey(SubmissionDBO.class, params);
        assertNotNull(clone2);
        assertEquals(submission, clone2); 
        
        // Delete it
        boolean result = dboBasicDao.deleteObjectByPrimaryKey(SubmissionDBO.class,  params);
        assertTrue("Failed to delete the entry created", result); 
    }
    
    @Test
    public void testPLFM4379() throws Exception {
    	String entityBundleJSON = "{\"entity\":{\"s3Token\":\"/repo/v1/entity/syn1896000/s3Token\",\"concreteType\":\"org.sagebionetworks.repo.model.Data\",\"versionLabel\":\"1\",\"etag\":\"00000000-0000-0000-0000-000000000000\",\"accessControlList\":\"/repo/v1/entity/syn1896000/acl\",\"versionUrl\":\"/repo/v1/entity/syn1896000/version/1\",\"modifiedBy\":\"Yuan Yuan\",\"contentType\":\"application/zip\",\"entityType\":\"org.sagebionetworks.repo.model.Data\",\"uri\":\"/repo/v1/entity/syn1896000\",\"id\":\"syn1896000\",\"createdOn\":\"2013-05-25T22:14:53.364Z\",\"versions\":\"/repo/v1/entity/syn1896000/version\",\"modifiedOn\":\"2013-05-25T22:15:03.591Z\",\"parentId\":\"syn1720419\",\"createdBy\":\"Yuan Yuan\",\"locations\":[{\"path\":\"https://s3.amazonaws.com/proddata.sagebase.org/1896000/1896001/archive.zip?Expires=1369606511&x-amz-security-token=AQoDYXdzEN%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEa0ALTNkPxlNDJWvqMh6p9SI%2Ff4bfQtnzDEtLGMBx6YhDXz3RtqX2YFAsoMuieR2UpV4Gmv7qmzP2CINv1nrJDALCnQ%2BTO5SHEa%2Fino3idLc5yTKLaVaBtiDc2rhDDHhIJHVv2EeH2mc6ceSqEXsj553uSO7n4gUm3Ai0NYJMCkyNFoRDoYe3pPR4Hf%2F5H9nBa32LXQ9BwW43vsXHHOiOTwmVXUzVK0Vg8NIPfcTT%2F3r3QjwJIYZinLgOiz8V4xsPaVtbpNH2bNmL%2BBnIewMUQbzU9G3y%2B%2Bd7I5vSJP4maPjXk3nDuGP503sRfO4FBUjrKIRUoTnOPwwUrPJddJ%2Bh7w9s5ZiqbciHnfwwx0S0IrKUasMCA66aLnPrJ1td5XT9tRIUHA4hPPBDa3bxPWJxe6r0mV13JQWq0ZBp6CetfGCRA6ZBNdBdkCvvpuJuM%2B8FDuZkg7%2B%2BEjQU%3D&AWSAccessKeyId=ASIAJ4E2JSTIT7ZPB7NQ&Signature=c4b%2B7HpoAp2NSE5l9vU98z0cOaM%3D\",\"type\":\"awss3\"}],\"name\":\"Cox LUSC miRNA\",\"md5\":\"e0ea6f7c54549a897d629dead8b08241\",\"annotations\":\"/repo/v1/entity/syn1896000/annotations\",\"versionNumber\":1},\"fileHandles\":[],\"annotations\":{\"id\":\"syn1896000\",\"creationDate\":\"1369520093364\",\"stringAnnotations\":{\"dataType\":[\"miRNA\"],\"featureSelection\":[\"LASSO\"],\"normalization\":[\"None\"],\"clinicalUsed\":[\"No\"],\"method\":[\"LASSO + cox\"],\"cancer\":[\"LUSC\"]},\"dateAnnotations\":{},\"etag\":\"00000000-0000-0000-0000-000000000000\",\"doubleAnnotations\":{},\"longAnnotations\":{},\"blobAnnotations\":{},\"uri\":\"/entity/syn1896000/annotations\"},\"entityType\":\"org.sagebionetworks.repo.model.Data\"}";
    
    	SubmissionDBO backup = new SubmissionDBO();
    	backup.setDockerRepositoryName(null);
    	backup.setEntityBundle(entityBundleJSON.getBytes());

    	SubmissionDBO dbo = backup.getTranslator().createDatabaseObjectFromBackup(backup);
    	assertNull(dbo.getDockerRepositoryName());
     }

    @Test
    public void testMigrator() throws Exception {
    	String dockerRepositoryName = "docker.synapse.org/syn123/arepo";
    	SubmissionDBO backup = new SubmissionDBO();
    	backup.setDockerRepositoryName(null);

    	EntityBundle bundle = new EntityBundle();
    	DockerRepository entity = new DockerRepository();
    	entity.setRepositoryName(dockerRepositoryName);
    	bundle.setEntity(entity);
    	JSONObjectAdapter joa = new JSONObjectAdapterImpl();
    	bundle.writeToJSONObject(joa);

    	backup.setEntityBundle(joa.toJSONString().getBytes());
    	SubmissionDBO dbo = backup.getTranslator().createDatabaseObjectFromBackup(backup);
    	assertEquals(dockerRepositoryName, dbo.getDockerRepositoryName());
    }
 
    @Test
    public void testMigratorAlreadyMigrated() throws Exception {
    	String dockerRepositoryName = "docker.synapse.org/syn123/arepo";
    	SubmissionDBO backup = new SubmissionDBO();
    	backup.setDockerRepositoryName(dockerRepositoryName);

    	EntityBundle bundle = new EntityBundle();
    	DockerRepository entity = new DockerRepository();
    	entity.setRepositoryName(dockerRepositoryName);
    	bundle.setEntity(entity);
    	JSONObjectAdapter joa = new JSONObjectAdapterImpl();
    	bundle.writeToJSONObject(joa);

    	backup.setEntityBundle(joa.toJSONString().getBytes());
    	SubmissionDBO dbo = backup.getTranslator().createDatabaseObjectFromBackup(backup);
    	assertEquals(dockerRepositoryName, dbo.getDockerRepositoryName());
    }
 
    @Test
    public void testMigratorNotADockerRepo() throws Exception {
    	SubmissionDBO backup = new SubmissionDBO();
    	backup.setDockerRepositoryName(null);

    	EntityBundle bundle = new EntityBundle();
    	FileEntity entity = new FileEntity();
    	bundle.setEntity(entity);
    	JSONObjectAdapter joa = new JSONObjectAdapterImpl();
    	bundle.writeToJSONObject(joa);

    	backup.setEntityBundle(joa.toJSONString().getBytes());
    	SubmissionDBO dbo = backup.getTranslator().createDatabaseObjectFromBackup(backup);
    	assertEquals(backup, dbo);
    }
 

}
