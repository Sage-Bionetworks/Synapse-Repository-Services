package org.sagebionetworks.repo.manager.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Collectors;

import org.everit.json.schema.ArraySchema;
import org.everit.json.schema.CombinedSchema;
import org.everit.json.schema.ConstSchema;
import org.everit.json.schema.ObjectSchema;
import org.everit.json.schema.ReferenceSchema;
import org.everit.json.schema.Schema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;

@ExtendWith(MockitoExtension.class)
public class DerivedAnnotationVisitorImplTest {

	@Test
	public void testMergeArrayAnnotations() {
		AnnotationsValue newValue = AnnotationsV2TestUtils.createNewValue(AnnotationsValueType.LONG, "111");
		AnnotationsValue existing = AnnotationsV2TestUtils.createNewValue(AnnotationsValueType.LONG, "222");
		// call under test
		AnnotationsValue result = DerivedAnnotationVisitorImpl.mergeArrayAnnotations(existing, newValue);
		AnnotationsValue expected = AnnotationsV2TestUtils.createNewValue(AnnotationsValueType.LONG, "222", "111");
		assertEquals(expected, result);
	}

	@Test
	public void testMergeArrayAnnotationsWithDuplicates() {
		AnnotationsValue newValue = AnnotationsV2TestUtils.createNewValue(AnnotationsValueType.LONG, "111", "222");
		AnnotationsValue existing = AnnotationsV2TestUtils.createNewValue(AnnotationsValueType.LONG, "222", "333");
		// call under test
		AnnotationsValue result = DerivedAnnotationVisitorImpl.mergeArrayAnnotations(existing, newValue);
		AnnotationsValue expected = AnnotationsV2TestUtils.createNewValue(AnnotationsValueType.LONG, "222", "333",
				"111");
		assertEquals(expected, result);
	}

	@Test
	public void testMergeArrayAnnotationsWithExistingNull() {
		AnnotationsValue newValue = AnnotationsV2TestUtils.createNewValue(AnnotationsValueType.LONG, "111");
		AnnotationsValue existing = null;
		// call under test
		AnnotationsValue result = DerivedAnnotationVisitorImpl.mergeArrayAnnotations(existing, newValue);
		AnnotationsValue expected = AnnotationsV2TestUtils.createNewValue(AnnotationsValueType.LONG, "111");
		assertEquals(expected, result);
	}

	@Test
	public void testMergeArrayAnnotationsWithMixedTypes() {
		AnnotationsValue newValue = AnnotationsV2TestUtils.createNewValue(AnnotationsValueType.DOUBLE, "3.14");
		AnnotationsValue existing = AnnotationsV2TestUtils.createNewValue(AnnotationsValueType.LONG, "222");
		// call under test
		AnnotationsValue result = DerivedAnnotationVisitorImpl.mergeArrayAnnotations(existing, newValue);
		AnnotationsValue expected = AnnotationsV2TestUtils.createNewValue(AnnotationsValueType.STRING, "222", "3.14");
		assertEquals(expected, result);
	}
	
	@Test
	public void testMergeArrayAnnotationsNullNew() {
		AnnotationsValue newValue = null;
		AnnotationsValue existing = AnnotationsV2TestUtils.createNewValue(AnnotationsValueType.LONG, "222");
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			DerivedAnnotationVisitorImpl.mergeArrayAnnotations(existing, newValue);
		}).getMessage();
		assertEquals("newValue is required.", message);
	}
	
	@Test
	public void testMergeArrayAnnotationsNewTypeNull() {
		AnnotationsValue newValue = AnnotationsV2TestUtils.createNewValue(AnnotationsValueType.LONG, "111");
		newValue.setType(null);
		AnnotationsValue existing = AnnotationsV2TestUtils.createNewValue(AnnotationsValueType.LONG, "222");
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			DerivedAnnotationVisitorImpl.mergeArrayAnnotations(existing, newValue);
		}).getMessage();
		assertEquals("newValue.type is required.", message);
	}
	
	@Test
	public void testStreamOverSubschemasWithCombindedSchema() {

		Schema objectSchemaOne =  new ObjectSchema.Builder()
				.addPropertySchema("one", ConstSchema.builder().permittedValue(1L).build())
				.addPropertySchema("two", ConstSchema.builder().permittedValue(2L).build()).build();
		
		Schema objectSchemaTwo =  new ObjectSchema.Builder()
				.addPropertySchema("three", ConstSchema.builder().permittedValue(3L).build())
				.addPropertySchema("four", ConstSchema.builder().permittedValue(4L).build()).build();
		
		Schema schema = CombinedSchema.allOf(List.of(objectSchemaOne, objectSchemaTwo)).build();
		// call under test
		List<Schema> results =DerivedAnnotationVisitorImpl.streamOverSubschemas(schema).collect(Collectors.toList());
		// Note: The properties of the object schemas are not include as sub schemas.
		List<Schema> expected = List.of(objectSchemaOne, objectSchemaTwo);
		assertEquals(expected, results);
	}
	
	@Test
	public void testStreamOverSubschemasWithCombindedSchemaNested() {

		Schema one = new ObjectSchema.Builder()
				.addPropertySchema("one", ConstSchema.builder().permittedValue(1L).build())
				.addPropertySchema("two", ConstSchema.builder().permittedValue(2L).build()).build();

		Schema two = new ObjectSchema.Builder()
				.addPropertySchema("three", ConstSchema.builder().permittedValue(3L).build())
				.addPropertySchema("four", ConstSchema.builder().permittedValue(4L).build()).build();

		Schema combineOne = CombinedSchema.allOf(List.of(one, two)).build();

		Schema three = new ObjectSchema.Builder()
				.addPropertySchema("five", ConstSchema.builder().permittedValue(5L).build())
				.addPropertySchema("six", ConstSchema.builder().permittedValue(6L).build()).build();

		Schema schema = CombinedSchema.anyOf(List.of(combineOne, three)).build();

		// call under test
		List<Schema> results = DerivedAnnotationVisitorImpl.streamOverSubschemas(schema).collect(Collectors.toList());
		List<Schema> expected = List.of(one, two, three);
		assertEquals(expected, results);
	}
	
	@Test
	public void testStreamOverSubschemasWithArraySchema() {
		Schema constant = ConstSchema.builder().permittedValue(1L).build();
		Schema array = ArraySchema.builder().containsItemSchema(constant).build();

		// call under test
		List<Schema> results =DerivedAnnotationVisitorImpl.streamOverSubschemas(array).collect(Collectors.toList());
		List<Schema> expected = List.of(constant);
		assertEquals(expected, results);
	}
	
	@Test
	public void testStreamOverSubschemasWithReference() {
		Schema constant = ConstSchema.builder().permittedValue(1L).build();
		ReferenceSchema ref = ReferenceSchema.builder().build();
		ref.setReferredSchema(constant);

		// call under test
		List<Schema> results =DerivedAnnotationVisitorImpl.streamOverSubschemas(ref).collect(Collectors.toList());
		List<Schema> expected = List.of(constant);
		assertEquals(expected, results);
	}
	
	@Test
	public void testStreamOverSubschemasWithCombindedReferenceArray() {

		Schema one = new ObjectSchema.Builder()
				.addPropertySchema("one", ConstSchema.builder().permittedValue(1L).build())
				.addPropertySchema("two", ConstSchema.builder().permittedValue(2L).build()).build();

		Schema two = new ObjectSchema.Builder()
				.addPropertySchema("three", ConstSchema.builder().permittedValue(3L).build())
				.addPropertySchema("four", ConstSchema.builder().permittedValue(4L).build()).build();

		Schema combineOne = CombinedSchema.allOf(List.of(one, two)).build();

		Schema three = new ObjectSchema.Builder()
				.addPropertySchema("five", ConstSchema.builder().permittedValue(5L).build())
				.addPropertySchema("six", ConstSchema.builder().permittedValue(6L).build()).build();
		
		Schema four = ConstSchema.builder().permittedValue(12L).build();
		ReferenceSchema ref = ReferenceSchema.builder().build();
		ref.setReferredSchema(four);
		
		Schema five = ConstSchema.builder().permittedValue(13L).build();
		Schema array = ArraySchema.builder().containsItemSchema(five).build();

		Schema subs = CombinedSchema.anyOf(List.of(combineOne, three, ref, array)).build();
		
		ReferenceSchema refToAll = ReferenceSchema.builder().build();
		refToAll.setReferredSchema(subs);

		// call under test
		List<Schema> results = DerivedAnnotationVisitorImpl.streamOverSubschemas(refToAll).collect(Collectors.toList());
		List<Schema> expected = List.of(one, two, three, four, five);
		assertEquals(expected, results);
	}

}
