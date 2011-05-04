package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FieldTypeDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.jdo.persistence.JDOAnnotationType;
import org.sagebionetworks.repo.model.query.FieldType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodles-test-context.xml" })
public class JDOFieldTypeDAOImplTest {

	@Autowired
	private FieldTypeDAO fieldTypeDao;
	private List<String> toDelete;

	@Before
	public void before() {
		assertNotNull(fieldTypeDao);
		toDelete = new ArrayList<String>();
	}

	@After
	public void after() {
		if (fieldTypeDao != null && toDelete != null) {
			for (String name : toDelete) {
				try {
					fieldTypeDao.delete(name);
				} catch (Exception e) {

				}
			}
		}
	}

	@Test
	public void testNodePrimaryFields() {
		Field[] fields = Node.class.getDeclaredFields();
		for (Field field : fields) {
			System.out.println(field.getName());
			FieldType type = fieldTypeDao.getTypeForName(field.getName());
			assertEquals(FieldType.PRIMARY_FIELD, type);
		}
	}

	@Test
	public void testAdd() throws DatastoreException {
		// First make sure the type does not exist
		String name = "JDOAnnotationTypeDAOImplTest.addDate";
		toDelete.add(name);
		FieldType exists = fieldTypeDao.getTypeForName(name);
		assertEquals(FieldType.DOES_NOT_EXIST, exists);
		// Add a new mapping
		fieldTypeDao.addNewType(name, FieldType.DATE_ATTRIBUTE);
		// Make sure we can get it
		FieldType type = fieldTypeDao.getTypeForName(name);
		assertNotNull(type);
		// Make sure we can delete it
		fieldTypeDao.delete(name);
		// Make sure it not longer exists
		FieldType fetchedType = fieldTypeDao.getTypeForName(name);
		assertEquals(FieldType.DOES_NOT_EXIST, fetchedType);
	}

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

}
