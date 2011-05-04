package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sagebionetworks.repo.model.jdo.persistence.JDODateAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDODoubleAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOLongAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.sagebionetworks.repo.model.jdo.persistence.JDOStringAnnotation;
import org.sagebionetworks.repo.model.query.FieldType;

public class QueryUtilsTest {

	@Test
	public void testGetTableNameForClass() {
		// Test each class that we expect to use
		// Node
		Class clazz = JDONode.class;
		String tableName = QueryUtils.getTableNameForClass(clazz);
		System.out.println("Table name for: " + clazz.getName() + " = "
				+ tableName);
		assertNotNull(tableName);

		// String annotation.
		clazz = JDOStringAnnotation.class;
		tableName = QueryUtils.getTableNameForClass(clazz);
		System.out.println("Table name for: " + clazz.getName() + " = "
				+ tableName);
		assertNotNull(tableName);

		// Long annotation.
		clazz = JDOLongAnnotation.class;
		tableName = QueryUtils.getTableNameForClass(clazz);
		System.out.println("Table name for: " + clazz.getName() + " = "
				+ tableName);
		assertNotNull(tableName);

		// Date annotation.
		clazz = JDODateAnnotation.class;
		tableName = QueryUtils.getTableNameForClass(clazz);
		System.out.println("Table name for: " + clazz.getName() + " = "
				+ tableName);
		assertNotNull(tableName);

		// Double annotation.
		clazz = JDODoubleAnnotation.class;
		tableName = QueryUtils.getTableNameForClass(clazz);
		System.out.println("Table name for: " + clazz.getName() + " = "
				+ tableName);
		assertNotNull(tableName);
	}


	@Test
	public void testGetTableNameForFieldType() {
		// Only test for the types that are valid
		// String
		String tableName = QueryUtils
				.getTableNameForFieldType(FieldType.STRING_ATTRIBUTE);
		assertNotNull(tableName);
		// Date
		tableName = QueryUtils.getTableNameForFieldType(FieldType.DATE_ATTRIBUTE);
		assertNotNull(tableName);
		// Long
		tableName = QueryUtils.getTableNameForFieldType(FieldType.LONG_ATTRIBUTE);
		assertNotNull(tableName);
		// Double
		tableName = QueryUtils
				.getTableNameForFieldType(FieldType.DOUBLE_ATTRIBUTE);
		assertNotNull(tableName);
	}
}
