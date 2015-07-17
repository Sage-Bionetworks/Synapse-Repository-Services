package org.sagebionetworks.repo.model.dbo.dao;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.util.Date;

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
	AsynchronousDAOImpl testDao;
	NamedAnnotations annos;
	Annotations forDb;
	Reference ref;

	@Before
	public void before() throws DatastoreException, NotFoundException, UnsupportedEncodingException, JSONObjectAdapterException{
		// Build up the mocks
		mockNodeDao = Mockito.mock(NodeDAO.class);
		mockReferenceDao = Mockito.mock(DBOReferenceDao.class);
		mockAnnotationsDao = Mockito.mock(DBOAnnotationsDao.class);
		mockStorageLocationDao = Mockito.mock(StorageUsageQueryDao.class);
		mockFileMetadataDao = Mockito.mock(FileHandleDao.class);

		// Setup the reference
		ref = new Reference();
		ref.setTargetId("syn444");
		ref.setTargetVersionNumber(1l);
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
		when(mockNodeDao.getNodeReference(nodeIdString)).thenReturn(ref);
		when(mockNodeDao.getAnnotations(nodeIdString)).thenReturn(annos);

		testDao = new AsynchronousDAOImpl(mockNodeDao, mockReferenceDao, mockAnnotationsDao, mockStorageLocationDao,mockFileMetadataDao);
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
		verify(mockReferenceDao, times(1)).replaceReference(nodeId, ref);
		verify(mockAnnotationsDao, times(1)).replaceAnnotations(forDb);
	}

	@Test
	public void testDelete() throws NotFoundException{
		// Make the call
		testDao.deleteEntity(nodeIdString);
		// verify
		verify(mockReferenceDao, times(1)).deleteReferenceByOwnderId(nodeId);
		verify(mockAnnotationsDao, times(1)).deleteAnnotationsByOwnerId(nodeId);
	}
}
