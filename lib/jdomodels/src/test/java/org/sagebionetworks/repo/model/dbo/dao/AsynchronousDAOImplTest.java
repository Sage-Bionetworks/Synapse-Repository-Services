package org.sagebionetworks.repo.model.dbo.dao;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.test.util.ReflectionTestUtils;
/**
 * A unit test for AsynchronousDAOImpl.
 * @author John
 *
 */
public class AsynchronousDAOImplTest {

	Long nodeId = new Long(123);
	String nodeIdString = KeyFactory.keyToString(nodeId);

	@Mock
	NodeDAO mockNodeDao;
	@Mock
	DBOAnnotationsDao mockAnnotationsDao;
	@Mock
	FileHandleDao mockFileMetadataDao;
	AsynchronousDAOImpl testDao;
	NamedAnnotations annos;
	Annotations forDb;
	Reference ref;

	@Before
	public void before() throws DatastoreException, NotFoundException, UnsupportedEncodingException, JSONObjectAdapterException{
		MockitoAnnotations.initMocks(this);

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

		testDao = new AsynchronousDAOImpl();
		ReflectionTestUtils.setField(testDao, "nodeDao", mockNodeDao);
		ReflectionTestUtils.setField(testDao, "dboAnnotationsDao", mockAnnotationsDao);
		ReflectionTestUtils.setField(testDao, "fileMetadataDao", mockFileMetadataDao);
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
		verify(mockAnnotationsDao, times(1)).replaceAnnotations(forDb);
	}

	@Test
	public void testDelete() throws NotFoundException{
		// Make the call
		testDao.deleteEntity(nodeIdString);
		// verify
		verify(mockAnnotationsDao, times(1)).deleteAnnotationsByOwnerId(nodeId);
	}
}
