package org.sagebionetworks.table.cluster.columntranslation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;

public class ColumnTranslationReferenceLookupTest {

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
	public void testLookupForUserQueryColumnName_caseInsensitiveMetadataColumns(){
		assertEquals(RowMetadataColumnTranslationReference.ROW_ID, lookup.forUserQueryColumnName("row_ID").orElse(null));
		assertEquals(RowMetadataColumnTranslationReference.ROW_VERSION, lookup.forUserQueryColumnName("rOw_Version").orElse(null));
		assertEquals(RowMetadataColumnTranslationReference.ROW_ETAG, lookup.forUserQueryColumnName("roW_ETaG").orElse(null));
		assertEquals(RowMetadataColumnTranslationReference.ROW_BENEFACTOR, lookup.forUserQueryColumnName("rOW_BENEFACTOR").orElse(null));

	}

	@Test
	public void testLookupForTranslatedColumnName(){
		assertEquals(fooReference, lookup.forTranslatedColumnName("_C123_").orElse(null));
		assertEquals(barReference, lookup.forTranslatedColumnName("_C456_").orElse(null));
	}

	@Test
	public void testLookupForTranslatedColumnName_caseInsensitiveMetadataColumns(){
		assertEquals(RowMetadataColumnTranslationReference.ROW_ID, lookup.forTranslatedColumnName("row_ID").orElse(null));
		assertEquals(RowMetadataColumnTranslationReference.ROW_VERSION, lookup.forTranslatedColumnName("rOw_Version").orElse(null));
		assertEquals(RowMetadataColumnTranslationReference.ROW_ETAG, lookup.forTranslatedColumnName("roW_ETaG").orElse(null));
		assertEquals(RowMetadataColumnTranslationReference.ROW_BENEFACTOR, lookup.forTranslatedColumnName("rOW_BENEFACTOR").orElse(null));
	}
}