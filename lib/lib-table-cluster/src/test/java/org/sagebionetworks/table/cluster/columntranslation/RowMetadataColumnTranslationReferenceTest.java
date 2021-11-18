package org.sagebionetworks.table.cluster.columntranslation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.Test;

public class RowMetadataColumnTranslationReferenceTest {

	@Test
	public void testEnums() {
		for (RowMetadataColumnTranslationReference reference : RowMetadataColumnTranslationReference.values()) {
			// user query name are not translated for these columns
			assertEquals(reference.getUserQueryColumnName(), reference.getTranslatedColumnName());
		}
	}

	@Test
	public void testLookupColumnReferenceWithEachType() {
		for (RowMetadataColumnTranslationReference reference : RowMetadataColumnTranslationReference.values()) {
			assertEquals(Optional.of(reference),
					RowMetadataColumnTranslationReference.lookupColumnReference(reference.name()));
		}
	}

	@Test
	public void testLookupColumnReferenceWithEachTypeWithLowerCase() {
		for (RowMetadataColumnTranslationReference reference : RowMetadataColumnTranslationReference.values()) {
			assertEquals(Optional.of(reference),
					RowMetadataColumnTranslationReference.lookupColumnReference(reference.name().toLowerCase()));
		}
	}
	
	@Test
	public void testLookupColumnReferenceWithNoMatch() {
		assertEquals(Optional.empty(),
				RowMetadataColumnTranslationReference.lookupColumnReference("foo"));
	}
}