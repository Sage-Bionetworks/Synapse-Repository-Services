package org.sagebionetworks.table.cluster.columntranslation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;

class ColumnTranslationReferenceLookupTest {

	ColumnModel fooModel;
	ColumnModel barModel;
	List<ColumnModel> schema;

	SchemaColumnTranslationReference fooReference;
	SchemaColumnTranslationReference barReference;

	ColumnTranslationReferenceLookup lookup;

	@BeforeEach
	public void setup(){
		fooModel = new ColumnModel();
		fooModel.setName("foo");
		fooModel.setId("123");
		fooModel.setColumnType(ColumnType.STRING);
		fooReference = new SchemaColumnTranslationReference(fooModel);

		barModel= new ColumnModel();
		barModel.setName("bar");
		barModel.setId("456");
		barModel.setColumnType(ColumnType.DOUBLE);
		barReference = new SchemaColumnTranslationReference(barModel);

		schema = Arrays.asList(fooModel, barModel);
		lookup = new ColumnTranslationReferenceLookup(schema);
	}

	@Test
	public void testLookupForUserQueryColumnName(){
		assertEquals(fooReference, lookup.forUserQueryColumnName("foo").orElse(null));
		assertEquals(barReference, lookup.forUserQueryColumnName("bar").orElse(null));
	}

	@Test
	public void testLookupForTranslatedColumnName(){
		assertEquals(fooReference, lookup.forTranslatedColumnName("_C123_").orElse(null));
		assertEquals(barReference, lookup.forTranslatedColumnName("_C456_").orElse(null));
	}
}