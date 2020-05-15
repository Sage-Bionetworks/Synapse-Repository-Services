package org.sagebionetworks.table.cluster.columntranslation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;

public class SchemaColumnTranslationReferenceTest {

	ColumnModel columnModel;

	@BeforeEach
	public void setup(){
		columnModel = new ColumnModel();
		columnModel.setId("123");
		columnModel.setColumnType(ColumnType.DOUBLE);
		columnModel.setName("foo");

	}

	@Test
	public void testConstructor_NullColumnModel(){
		assertThrows(IllegalArgumentException.class, ()-> {
			new SchemaColumnTranslationReference(null);
		});
	}

	@Test
	public void testConstructor_ColumnModelNullId(){
		columnModel.setId(null);
		assertThrows(IllegalArgumentException.class, ()-> {
			new SchemaColumnTranslationReference(columnModel);
		});
	}

	@Test
	public void testConstructor_ColumnModelNullType(){
		columnModel.setColumnType(null);
		assertThrows(IllegalArgumentException.class, ()-> {
			new SchemaColumnTranslationReference(columnModel);
		});
	}

	@Test
	public void testConstructor_ColumnModelNullName(){
		columnModel.setName(null);
		assertThrows(IllegalArgumentException.class, ()-> {
			new SchemaColumnTranslationReference(columnModel);
		});
	}

	@Test
	public void testConstructor(){
		SchemaColumnTranslationReference reference = new SchemaColumnTranslationReference(columnModel);
		assertEquals(columnModel.getName(), reference.getUserQueryColumnName());
		assertEquals(columnModel.getId(), reference.getId());
		assertEquals(columnModel.getColumnType(), reference.getColumnType());
		assertEquals("_C123_", reference.getTranslatedColumnName());
	}
}