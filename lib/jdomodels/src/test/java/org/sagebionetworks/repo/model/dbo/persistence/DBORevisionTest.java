package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBORevisionTest {
	
	@Autowired
	private DBOBasicDao dboBasicDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	private List<Long> toDelete = null;
	
	private DBONode node;

	@AfterEach
	public void after() throws DatastoreException {
		if(dboBasicDao != null && toDelete != null){
			for(Long id: toDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id);
				dboBasicDao.deleteObjectByPrimaryKey(DBONode.class, params);
			}
		}
	}
	
	@BeforeEach
	public void before() throws DatastoreException, UnsupportedEncodingException{
		toDelete = new LinkedList<Long>();
		// Create a node to create revisions of.
		node = new DBONode();
		node.setId(idGenerator.generateNewId(IdType.ENTITY_ID));
		toDelete.add(node.getId());
		Long createdById = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		node.setCreatedBy(createdById);
		node.setCreatedOn(System.currentTimeMillis());
		node.setCurrentRevNumber(NodeConstants.DEFAULT_VERSION_NUMBER);
		node.setMaxRevNumber(NodeConstants.DEFAULT_VERSION_NUMBER);
		node.setDescription("A basic description".getBytes("UTF-8"));
		node.seteTag("0");
		node.setName("DBORevisionTest.baseNode");
		node.setParentId(null);
		node.setType(EntityType.project.name());
		dboBasicDao.createNew(node);
	}
	
	@Test
	public void testRoundTrip() throws Exception {
		DBORevision rev = new DBORevision();
		rev.setOwner(node.getId());
		rev.setRevisionNumber(new Long(1));
		rev.setReference(null);
		rev.setComment(null);
		rev.setLabel(""+rev.getRevisionNumber());
		Long createdById = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		rev.setModifiedBy(createdById);
		rev.setModifiedOn(System.currentTimeMillis());
		rev.setUserAnnotationsJSON("{}");
		rev.setEntityPropertyAnnotations("some data".getBytes(StandardCharsets.UTF_8));
		// Now create it
		rev = dboBasicDao.createNew(rev);
		// Make sure we can get it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("owner", rev.getOwner());
		params.addValue("revisionNumber", rev.getRevisionNumber());
		DBORevision clone = dboBasicDao.getObjectByPrimaryKey(DBORevision.class, params);
		assertEquals(rev, clone);
		// Update with some values
		Reference ref = new Reference();
		byte[] blob = NodeUtils.compressReference(ref);
		clone.setReference(blob);
		clone.setComment("No comment!");
		boolean result = dboBasicDao.update(clone);
		assertTrue(result);
		// Fetch the updated
		DBORevision updatedClone = dboBasicDao.getObjectByPrimaryKey(DBORevision.class, params);
		assertEquals(clone, updatedClone);
	}

	@Test
	public void testUserAnnotationTranslator_differentJSONFormat(){
		//create a new DBORevision just to get the translator. Since getTranslator is not static, this ensures that
		// the translator is modifying passed in params instead of the fields of the DBORevision object that created it
		MigratableTableTranslation<DBORevision,DBORevision> translator = new DBORevision().getTranslator();

		String JSON = "{\"annotations\":{\"anno1\":{\"type\":\"DOUBLE\",\"value\":[\"1.2\"]}}}";


		DBORevision revision = new DBORevision();
		revision.setUserAnnotationsJSON(JSON);

		//noting should change if the json is in a different format.
		DBORevision modified = translator.createDatabaseObjectFromBackup(revision);
		assertEquals(JSON, modified.getUserAnnotationsJSON());
	}
}
