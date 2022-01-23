package org.sagebionetworks.table.cluster.columntranslation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.Test;

public class RowMetadataColumnTranslationReferenceTest {

	@Test
	public void testLookupColumnReferenceWithEachType() {
		for (RowMetadataColumnTranslationReference reference : RowMetadataColumnTranslationReference.values()) {
			assertEquals(Optional.of(reference.getColumnTranslationReference()),
					RowMetadataColumnTranslationReference.lookupColumnReference(reference.name()));
		}
	}

	@Test
	public void testLookupColumnReferenceWithEachTypeWithLowerCase() {
		for (RowMetadataColumnTranslationReference reference : RowMetadataColumnTranslationReference.values()) {
			assertEquals(Optional.of(reference.getColumnTranslationReference()),
					RowMetadataColumnTranslationReference.lookupColumnReference(reference.name().toLowerCase()));
		}
	}
	
	@Test
	public void testLookupColumnReferenceWithStartsWithRowBenefactor() {
		assertEquals(Optional.of(new RowMetadataReferenceWrapper("ROW_BENEFACTOR_A0", RowMetadataColumnTranslationReference.ROW_BENEFACTOR)),
				RowMetadataColumnTranslationReference.lookupColumnReference("ROW_BENEFACTOR_A0"));
	}
	
	@Test
	public void testLookupColumnReferenceWithStartsWithRowBenefactorLower() {
		assertEquals(Optional.of(new RowMetadataReferenceWrapper("ROW_BENEFACTOR_A0", RowMetadataColumnTranslationReference.ROW_BENEFACTOR)),
				RowMetadataColumnTranslationReference.lookupColumnReference("row_benefactor_a0"));
	}
	
	@Test
	public void testLookupColumnReferenceWithNoMatch() {
		assertEquals(Optional.empty(),
				RowMetadataColumnTranslationReference.lookupColumnReference("foo"));
	}
}