package org.sagebionetworks.table.cluster.columntranslation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.Test;

public class RowMetadataColumnTranslationReferenceTest {

	@Test
	public void testEnums(){
		for(RowMetadataColumnTranslationReference reference : RowMetadataColumnTranslationReference.values()){
			//user query name are not translated for these columns
			assertEquals(reference.getUserQueryColumnName(), reference.getTranslatedColumnName());
		}
	}
}