package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.jdo.persistence.JDOAnnotationType;
import org.sagebionetworks.repo.model.query.FieldType;
import org.springframework.orm.jdo.JdoTemplate;

public class JDOFieldTypeDAOImplUnitTest {

	@Test
	public void testLocalCache() {
		// Create a mock template
		JdoTemplate mockTemplate = Mockito.mock(JdoTemplate.class);
		JDOFieldTypeDAOImpl dao = new JDOFieldTypeDAOImpl(mockTemplate);
		// The first time we call this it will not be in the cache
		String name = "JDOAnnotationTypeDAOImplTest.testLocalCache";
		FieldType type = FieldType.DOUBLE_ATTRIBUTE;
		JDOAnnotationType jdoType = new JDOAnnotationType();
		jdoType.setAttributeName(name);
		jdoType.setTypeClass(type.name());
		when(mockTemplate.getObjectById(JDOAnnotationType.class, name))
				.thenReturn(jdoType);
		// The first time we call this it should hit the database
		FieldType fetchedType = dao.getTypeForName(name);
		assertNotNull(fetchedType);
		// Now calling it again should get the value from the cache
		when(mockTemplate.getObjectById(JDOAnnotationType.class, name))
				.thenThrow(
						new IllegalStateException(
								"The value should have come from the cache not the DB"));
		fetchedType = dao.getTypeForName(name);
		assertEquals(FieldType.DOUBLE_ATTRIBUTE, fetchedType);
	}
	
	/**
	 * This is a test for PLFM-852.
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	@Test
	public void testLocalCacheAddNewType() throws DatastoreException, InvalidModelException {
		// Create a mock template
		JdoTemplate mockTemplate = Mockito.mock(JdoTemplate.class);
		JDOFieldTypeDAOImpl dao = new JDOFieldTypeDAOImpl(mockTemplate);
		// The first time we call this it will not be in the cache
		String name = "JDOAnnotationTypeDAOImplTest.testLocalCache";
		FieldType type = FieldType.DOUBLE_ATTRIBUTE;
		JDOAnnotationType jdoType = new JDOAnnotationType();
		jdoType.setAttributeName(name);
		jdoType.setTypeClass(type.name());
		when(mockTemplate.getObjectById(JDOAnnotationType.class, name)).thenReturn(jdoType);
		// The first time we call this it should hit the database
		dao.addNewType(name, type);
		// If a null pointer is thrown then it tried to get the value from the Database.
		dao.setTempalte(null);
		// Add it again
		// The above exception will be thrown if the data is not in the cache.
		dao.addNewType(name, type);
	}
}
