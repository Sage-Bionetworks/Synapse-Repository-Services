package org.sagebionetworks.repo.model.dbo.dao;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.StorageUsageQueryDao;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
/**
 * A unit test for AsynchronousDAOImpl.
 * @author John
 *
 */
public class AsynchronousDAOImplTest {

	Long nodeId = new Long(123);
	String nodeIdString = KeyFactory.keyToString(nodeId);

	NodeDAO mockNodeDao;
	DBOReferenceDao mockReferenceDao;
	DBOAnnotationsDao mockAnnotationsDao;
	StorageUsageQueryDao mockStorageLocationDao;
	FileHandleDao mockFileMetadataDao;
	WikiPageDao mockWikiPageDao;
	AsynchronousDAOImpl testDao;
	NamedAnnotations annos;
	Annotations forDb;
	Map<String, Set<Reference>> references;

	@Before
	public void before() throws DatastoreException, NotFoundException, UnsupportedEncodingException, JSONObjectAdapterException{
		// Build up the mocks
		mockNodeDao = Mockito.mock(NodeDAO.class);
		mockReferenceDao = Mockito.mock(DBOReferenceDao.class);
		mockAnnotationsDao = Mockito.mock(DBOAnnotationsDao.class);
		mockStorageLocationDao = Mockito.mock(StorageUsageQueryDao.class);
		mockFileMetadataDao = Mockito.mock(FileHandleDao.class);
		mockWikiPageDao = Mockito.mock(WikiPageDao.class);

		// Setup the references
		references = new HashMap<String, Set<Reference>>();
		Set<Reference> set = new HashSet<Reference>();
		Reference ref = new Reference();
		ref.setTargetId("syn444");
		ref.setTargetVersionNumber(1l);
		set.add(ref);
		references.put("some_key", set);
		// The annotations
		annos = new NamedAnnotations();
		annos.setId(nodeIdString);
		annos.setEtag("456");
		annos.setCreatedBy(999l);
		annos.setCreationDate(new Date(123));
		forDb = JDOSecondaryPropertyUtils.prepareAnnotationsForDBReplacement(annos, nodeIdString);
		// Only save distinct values in the DB.
		forDb = JDOSecondaryPropertyUtils.buildDistinctAnnotations(forDb);
		// Mock the node dao.
		when(mockNodeDao.getNodeReferences(nodeIdString)).thenReturn(references);
		when(mockNodeDao.getAnnotations(nodeIdString)).thenReturn(annos);

		testDao = new AsynchronousDAOImpl(mockNodeDao, mockReferenceDao, mockAnnotationsDao, mockStorageLocationDao,mockFileMetadataDao, mockWikiPageDao );
	}

	@Test (expected=IllegalArgumentException.class)
	public void testReplaceAllNull() throws NotFoundException{
		// Test replace add
		testDao.replaceAll(null);
	}

	@Test
	public void testReplaceAll() throws NotFoundException{
		// Make the call
		testDao.replaceAll(nodeIdString);
		// verify
		verify(mockReferenceDao, times(1)).replaceReferences(nodeId, references);
		verify(mockAnnotationsDao, times(1)).replaceAnnotations(forDb);
	}

	@Test
	public void testDelete() throws NotFoundException{
		// Make the call
		testDao.deleteEntity(nodeIdString);
		// verify
		verify(mockReferenceDao, times(1)).deleteReferencesByOwnderId(nodeId);
		verify(mockAnnotationsDao, times(1)).deleteAnnotationsByOwnerId(nodeId);
	}
}
