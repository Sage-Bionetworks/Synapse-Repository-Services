package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
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
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.jdo.persistence.JDOAnnotationType;
import org.sagebionetworks.repo.model.query.FieldType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
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
	public void testAdd() throws Exception {
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
	public void testInvalidNames() {
		// There are all invalid names
		String[] invalidNames = new String[] { "~", "!", "@", "#", "$", "%",
				"^", "&", "*", "(", ")", "\"", "\n\t", "'", "?", "<", ">", "/",
				";", "{", "}", "|", "=", "+", "-", "White\n\t Space", null, "" };
		for (int i = 0; i < invalidNames.length; i++) {
			try {
				// These are all bad names
				JDOFieldTypeDAOImpl.checkKeyName(invalidNames[i]);
				fail("Name: " + invalidNames[i] + " is invalid");
			} catch (InvalidModelException e) {
				// Expected
			}
		}
	}

	@Test
	public void testValidNames() throws InvalidModelException {
		// There are all invalid names
		List<String> vlaidNames = new ArrayList<String>();
		// All lower
		for (char ch = 'a'; ch <= 'z'; ch++) {
			vlaidNames.add("" + ch);
		}
		// All upper
		for (char ch = 'A'; ch <= 'Z'; ch++) {
			vlaidNames.add("" + ch);
		}
		// all numbers
		for (char ch = '0'; ch <= '9'; ch++) {
			vlaidNames.add("" + ch);
		}
		// underscore
		vlaidNames.add("_");
		vlaidNames.add(" Trimable ");
		vlaidNames.add("A1_b3po");
		for (int i = 0; i < vlaidNames.size(); i++) {
			// These are all bad names
			JDOFieldTypeDAOImpl.checkKeyName(vlaidNames.get(i));
		}
	}
}
