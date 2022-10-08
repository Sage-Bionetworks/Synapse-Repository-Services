package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
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
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.jdo.AnnotationUtils;
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
		DBORevision clone = dboBasicDao.getObjectByPrimaryKey(DBORevision.class, params).get();
		assertEquals(rev, clone);
		// Update with some values
		Reference ref = new Reference();
		byte[] blob = NodeUtils.compressReference(ref);
		clone.setReference(blob);
		clone.setComment("No comment!");
		boolean result = dboBasicDao.update(clone);
		assertTrue(result);
		// Fetch the updated
		DBORevision updatedClone = dboBasicDao.getObjectByPrimaryKey(DBORevision.class, params).get();
		assertEquals(clone, updatedClone);
	}
	
	@Test
	public void testDescriptionMigration() throws IOException {
		MigratableTableTranslation<DBORevision,DBORevision> translator = new DBORevision().getTranslator();

		Annotations entityProperties = new Annotations();
		
		entityProperties.addAnnotation("description", "a description");
		
		DBORevision backup = new DBORevision();
		
		backup.setEntityPropertyAnnotations(AnnotationUtils.compressAnnotationsV1(entityProperties));
		
		// Call under test
		backup = translator.createDatabaseObjectFromBackup(backup);
		
		assertEquals("a description", backup.getDescription());
		assertNull(AnnotationUtils.decompressedAnnotationsV1(backup.getEntityPropertyAnnotations()).getSingleValue("description"));

	}
	
	@Test
	public void testDescriptionMigrationWithNotString() throws IOException {
		MigratableTableTranslation<DBORevision,DBORevision> translator = new DBORevision().getTranslator();

		Annotations entityProperties = new Annotations();
		
		entityProperties.addAnnotation("description", 123L);
		
		DBORevision backup = new DBORevision();
		
		backup.setEntityPropertyAnnotations(AnnotationUtils.compressAnnotationsV1(entityProperties));
		
		// Call under test
		backup = translator.createDatabaseObjectFromBackup(backup);
		
		assertNull(backup.getDescription());
		assertEquals(123L, AnnotationUtils.decompressedAnnotationsV1(backup.getEntityPropertyAnnotations()).getSingleValue("description"));

	}
	
	@Test
	public void testDescriptionMigrationWithBadEntityProperties() throws IOException {
		MigratableTableTranslation<DBORevision,DBORevision> translator = new DBORevision().getTranslator();

		Annotations entityProperties = new Annotations();
		
		entityProperties.addAnnotation("description", "a description");
		
		DBORevision backup = new DBORevision();
		
		backup.setEntityPropertyAnnotations(new byte[] {1, 2, 3});
		
		// Call under test
		backup = translator.createDatabaseObjectFromBackup(backup);
		
		assertNull(backup.getDescription());
		assertArrayEquals(new byte[] {1, 2, 3}, backup.getEntityPropertyAnnotations());

	}
	
	@Test
	public void testDescriptionMigrationWithNoDescription() throws IOException {
		MigratableTableTranslation<DBORevision,DBORevision> translator = new DBORevision().getTranslator();

		Annotations entityProperties = new Annotations();
		
		entityProperties.addAnnotation("something", "else");
		
		DBORevision backup = new DBORevision();
		
		backup.setEntityPropertyAnnotations(AnnotationUtils.compressAnnotationsV1(entityProperties));
		
		// Call under test
		backup = translator.createDatabaseObjectFromBackup(backup);
		
		assertNull(backup.getDescription());
		assertNull(AnnotationUtils.decompressedAnnotationsV1(backup.getEntityPropertyAnnotations()).getSingleValue("description"));

	}
	
	@Test
	public void testDescriptionMigrationWithNoEntityProperties() throws IOException {
		MigratableTableTranslation<DBORevision,DBORevision> translator = new DBORevision().getTranslator();
		
		DBORevision backup = new DBORevision();
		
		backup.setEntityPropertyAnnotations(null);
		
		// Call under test
		backup = translator.createDatabaseObjectFromBackup(backup);
		
		assertNull(backup.getDescription());
		assertNull(backup.getEntityPropertyAnnotations());

	}
	
	@Test
	public void testDescriptionMigrationWithExisting() throws IOException {
		MigratableTableTranslation<DBORevision,DBORevision> translator = new DBORevision().getTranslator();
		
		DBORevision backup = new DBORevision();
		backup.setDescription("existing");
		
		Annotations entityProperties = new Annotations();
		
		entityProperties.addAnnotation("description", "a description");
		
		backup.setEntityPropertyAnnotations(AnnotationUtils.compressAnnotationsV1(entityProperties));
		
		// Call under test
		backup = translator.createDatabaseObjectFromBackup(backup);
		
		assertEquals("existing", backup.getDescription());
		assertEquals("a description", AnnotationUtils.decompressedAnnotationsV1(backup.getEntityPropertyAnnotations()).getSingleValue("description"));

	}
}
