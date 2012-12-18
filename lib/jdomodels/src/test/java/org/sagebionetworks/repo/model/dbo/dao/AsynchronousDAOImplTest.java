package org.sagebionetworks.repo.model.dbo.dao;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.StorageLocations;
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
	StorageLocationDAO mockStorageLocationDao;
	AsynchronousDAOImpl testDao;
	NamedAnnotations annos;
	StorageLocations sl;
	Annotations forDb;
	Map<String, Set<Reference>> references;

	@Before
	public void before() throws DatastoreException, NotFoundException, UnsupportedEncodingException, JSONObjectAdapterException{
		// Build up the mocks
		mockNodeDao = Mockito.mock(NodeDAO.class);
		mockReferenceDao = Mockito.mock(DBOReferenceDao.class);
		mockAnnotationsDao = Mockito.mock(DBOAnnotationsDao.class);
		mockStorageLocationDao = Mockito.mock(StorageLocationDAO.class);
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
		sl = JDOSecondaryPropertyUtils.getStorageLocations(annos, nodeId, annos.getCreatedBy());
		forDb = JDOSecondaryPropertyUtils.prepareAnnotationsForDBReplacement(annos, nodeIdString);
		// Mock the node dao.
		when(mockNodeDao.getNodeReferences(nodeIdString)).thenReturn(references);
		when(mockNodeDao.getAnnotations(nodeIdString)).thenReturn(annos);
		
		testDao = new AsynchronousDAOImpl(mockNodeDao, mockReferenceDao, mockAnnotationsDao, mockStorageLocationDao);
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
		verify(mockStorageLocationDao, times(1)).replaceLocationData(sl);
		verify(mockAnnotationsDao, times(1)).replaceAnnotations(forDb);
	}

	@Test
	public void testDelete() throws NotFoundException{
		// Make the call
		testDao.deleteEntity(nodeIdString);
		// verify
		verify(mockReferenceDao, times(1)).deleteReferencesByOwnderId(nodeId);
		verify(mockStorageLocationDao, times(1)).deleteLocationDataByOwnerId(nodeId);
		verify(mockAnnotationsDao, times(1)).deleteAnnotationsByOwnerId(nodeId);
	}
}
