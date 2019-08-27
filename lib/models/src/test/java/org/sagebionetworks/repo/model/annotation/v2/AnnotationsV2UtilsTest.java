package org.sagebionetworks.repo.model.annotation.v2;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Link;
import org.sagebionetworks.repo.model.table.AnnotationDTO;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

class AnnotationsV2UtilsTest {

	private AnnotationsV2 annotationsV2;
	private AnnotationsV2Value annotationsV2Value;
	private String key;

	@BeforeEach
	public void setUp(){
		key = "myKey";
		annotationsV2 = new AnnotationsV2();
		annotationsV2.setEtag("etag");
		annotationsV2Value = new AnnotationsV2Value();
		annotationsV2.setAnnotations(new HashMap<>());
		annotationsV2Value.setType(AnnotationsV2ValueType.DOUBLE);
		annotationsV2Value.setValue(new LinkedList<>());
	}

	@Test
	public void getSingleValue_AnnotationsV2Value_null(){
		assertNull(AnnotationsV2Utils.getSingleValue(null));
	}

	@Test
	public void getSingleValue_AnnotationsV2Value_nullList(){
		//Can't explicitly set list to null, but uninitialized AnnotationsV2Value will have null value.
		annotationsV2Value = new AnnotationsV2Value();
		assertNull(AnnotationsV2Utils.getSingleValue(annotationsV2Value));
	}

	@Test
	public void getSingleValue_AnnotationsV2Value_emptyList(){
		//Can't explicitly set list to null, but uninitialized AnnotationsV2Value will have null value.
		annotationsV2Value.setValue(Collections.emptyList());
		assertNull(AnnotationsV2Utils.getSingleValue(annotationsV2Value));
	}

	@Test
	public void getSingleValue_AnnotationsV2Value_nonEmptyList(){
		List<String> list = Arrays.asList("first", "second", "third");
		annotationsV2Value.setValue(list);
		assertEquals(list.get(0), AnnotationsV2Utils.getSingleValue(annotationsV2Value));
	}

	@Test
	public void getSingleValue_AnnotationsV2_nullKey(){
		assertThrows(IllegalArgumentException.class, ()->{
			AnnotationsV2Utils.getSingleValue(annotationsV2, null);
		});
	}

	@Test
	public void getSingleValue_AnnotationsV2_nullAnnotations(){
		assertThrows(IllegalArgumentException.class, ()->{
			AnnotationsV2Utils.getSingleValue(null, key);
		});
	}

	@Test
	public void getSingleValue_AnnotationsV2_nullAnnotationsMap(){
		//Can't explicitly set map to null, but uninitialized AnnotationsV2 will have null value.
		annotationsV2 = new AnnotationsV2();
		assertNull(AnnotationsV2Utils.getSingleValue(annotationsV2, key));
	}

	@Test
	public void getSingleValue_AnnotationsV2_keyExists(){
		List<String> list = Arrays.asList("first", "second", "third");
		annotationsV2Value.setValue(list);
		annotationsV2.setAnnotations(Collections.singletonMap(key,annotationsV2Value));
		assertEquals(list.get(0), AnnotationsV2Utils.getSingleValue(annotationsV2, key));
	}

	@Test
	public void getSingleValue_AnnotationsV2_keyDoesNotExist(){
		List<String> list = Arrays.asList("first", "second", "third");
		annotationsV2Value.setValue(list);
		assertNull(AnnotationsV2Utils.getSingleValue(annotationsV2, "nonExistentKey"));
	}


	@Test
	public void toJSONStringForStorage_nullAnnotationV2() throws JSONObjectAdapterException {
		assertNull(AnnotationsV2Utils.toJSONStringForStorage(null));
	}


	@Test
	public void toJSONStringForStorage_nullAnnotationsMap() throws JSONObjectAdapterException {
		AnnotationsV2 annotationsV2 = new AnnotationsV2();
		assertNull(AnnotationsV2Utils.toJSONStringForStorage(annotationsV2));
	}

	@Test
	public void toJSONStringForStorage_EmptyAnnotationsMap() throws JSONObjectAdapterException {
		AnnotationsV2 annotationsV2 = new AnnotationsV2();
		annotationsV2.setAnnotations(Collections.emptyMap());
		assertNull(AnnotationsV2Utils.toJSONStringForStorage(annotationsV2));
	}

	@Test
	public void toJSONStringForStorage_AnnotationsMapWithEntries() throws JSONObjectAdapterException {
		AnnotationsV2 annotationsV2 = new AnnotationsV2();
		annotationsV2.setAnnotations(Collections.singletonMap("key", AnnotationsV2TestUtils.createNewValue(AnnotationsV2ValueType.STRING, "value1")));
		assertEquals("{\"annotations\":[{\"key\":\"key\",\"value\":{\"type\":\"STRING\",\"value\":[\"value1\"]}}]}", AnnotationsV2Utils.toJSONStringForStorage(annotationsV2));
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
				AnnotationsV2Utils.checkKeyName(invalidNames[i]);
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
			AnnotationsV2Utils.checkKeyName(vlaidNames.get(i));
		}
	}


	@Test
	public void testUpdateValidateAnnotations_nullAnnotations(){
		assertThrows(IllegalArgumentException.class, () -> {
			AnnotationsV2Utils.validateAnnotations(null);
		});
	}

	@Test
	public void testUpdateValidateAnnotations_nullEtag(){
		annotationsV2.setEtag(null);
		assertThrows(IllegalArgumentException.class, () -> {
			AnnotationsV2Utils.validateAnnotations(annotationsV2);
		});
	}

	@Test
	public void testUpdateValidateAnnotations_emptyEtag(){
		annotationsV2.setEtag("");
		assertThrows(IllegalArgumentException.class, () -> {
			AnnotationsV2Utils.validateAnnotations(annotationsV2);
		});
	}

	@Test
	public void testUpdateValidateAnnotations_nullMap(){
		annotationsV2 = new AnnotationsV2();
		annotationsV2.setEtag("etag");
		assertThrows(IllegalArgumentException.class, () -> {
			AnnotationsV2Utils.validateAnnotations(annotationsV2);
		});
	}

	@Test
	public void testUpdateValidateAnnotations_ExceedMaxKeys(){
		for(int i = 0; i < AnnotationsV2Utils.MAX_ANNOTATION_KEYS + 1; i++){
			annotationsV2.getAnnotations().put("" + i, new AnnotationsV2Value());
		}
		assertThrows(IllegalArgumentException.class, () -> {
			AnnotationsV2Utils.validateAnnotations(annotationsV2);
		});
	}

	@Test
	public void testUpdateValidateAnnotations_MapInvalidKeys(){
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, "validKey1", "validValue", AnnotationsV2ValueType.STRING);
		//oh no
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, "ṯ͉͑̿͒͡h͙̜͚͎̗̥̒̊ͩ̐̿͛͡i̶̳̖̻̳͍̐̾ͬ́ͬͅs̢̖̦̓̂͌ ̥̯̐ͣͧk̛̪̬͙͊̇͗̾ͨͦ̚e͇̭͠y̵̩͒̓̌̎ͫ ̛̥̣̟͍̤͉ͦͪ̎̀͐́ïͭs̜̣̖̣ ͑ͥͯ͘i͇͖̝̮̹͞n͈̖̞͍̣͕̲͗ͩ̈́vͣ̒̐̾ͭ̃̍a͋͏l̸̩͙̭̮̗̯ͫ̈̇̅̿ͤí̲͛ͩd̮̞̐̓͂ͦ", "infinity", AnnotationsV2ValueType.DOUBLE);

		assertThrows(InvalidModelException.class, () -> {
			AnnotationsV2Utils.validateAnnotations(annotationsV2);
		});
	}

	@Test
	public void testUpdateValidateAnnotations_MapInvalidValues(){
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, "validKey1", "validValue", AnnotationsV2ValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, "validKey2", "NotADouble", AnnotationsV2ValueType.DOUBLE);

		assertThrows(IllegalArgumentException.class, () -> {
			AnnotationsV2Utils.validateAnnotations(annotationsV2);
		});
	}

	@Test
	public void testUpdateValidateAnnotations_MapValid(){
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, "validKey1", "validValue", AnnotationsV2ValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, "validKey2", "infinity", AnnotationsV2ValueType.DOUBLE);

		assertDoesNotThrow(() -> {
			AnnotationsV2Utils.validateAnnotations(annotationsV2);
		});
	}


	@Test
	public void testValidateAnnotations(){
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, "one", "1", AnnotationsV2ValueType.TIMESTAMP_MS);
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, "two", "1.2", AnnotationsV2ValueType.DOUBLE);
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, "three", "1", AnnotationsV2ValueType.LONG);
		annotationsV2.setEtag("etag");
		AnnotationsV2Utils.validateAnnotations(annotationsV2);
	}



	@Test
	public void testTranslate(){
		long entityId = 123;
		int maxAnnotationChars = 6;
		AnnotationsV2 annos = new AnnotationsV2();
		AnnotationsV2TestUtils.putAnnotations(annos, "aString", "someString", AnnotationsV2ValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annos, "aLong", "123", AnnotationsV2ValueType.LONG);
		AnnotationsV2TestUtils.putAnnotations(annos, "aDouble", "1.22", AnnotationsV2ValueType.DOUBLE);
		AnnotationsV2TestUtils.putAnnotations(annos, "aDate", "444", AnnotationsV2ValueType.TIMESTAMP_MS);

		List<AnnotationDTO> expected = Lists.newArrayList(
				new AnnotationDTO(entityId, "aString", AnnotationType.STRING, "someSt"),
				new AnnotationDTO(entityId, "aLong", AnnotationType.LONG, "123"),
				new AnnotationDTO(entityId, "aDouble", AnnotationType.DOUBLE, "1.22"),
				new AnnotationDTO(entityId, "aDate", AnnotationType.DATE, "444")
		);

		List<AnnotationDTO> results = AnnotationsV2Utils.translate(entityId, annos, maxAnnotationChars);
		assertNotNull(results);

		Assertions.assertEquals(expected, results);
	}

	/**
	 * See PLFM_4184
	 */
	@Test
	public void testTranslateEmptyList(){
		long entityId = 123;
		int maxAnnotationChars = 6;
		AnnotationsV2 annos = new AnnotationsV2();
		AnnotationsV2TestUtils.putAnnotations(annos, "emptyList", Collections.emptyList(), AnnotationsV2ValueType.STRING);
		List<AnnotationDTO> results = AnnotationsV2Utils.translate(entityId, annos, maxAnnotationChars);
		assertNotNull(results);
		Assertions.assertEquals(0, results.size());
	}

	@Test
	public void testTranslateNullValueInList(){
		long entityId = 123;
		int maxAnnotationChars = 6;
		AnnotationsV2 annos = new AnnotationsV2();
		AnnotationsV2TestUtils.putAnnotations(annos, "listWithNullValue", Collections.singletonList(null), AnnotationsV2ValueType.STRING);
		List<AnnotationDTO> results = AnnotationsV2Utils.translate(entityId, annos, maxAnnotationChars);
		assertNotNull(results);
		Assertions.assertEquals(0, results.size());
	}

	@Test
	public void testCheckValue_ValueListNull(){
		assertThrows(IllegalArgumentException.class, ()-> {
			AnnotationsV2Utils.checkValue(key, new AnnotationsV2Value());
		});
	}

	@Test
	public void testCheckValue_ValueListExceedMaxSize(){
		List<String> valuesList = Collections.nCopies(AnnotationsV2Utils.MAX_VALUES_PER_KEY + 1, "value");
		annotationsV2Value.setValue(valuesList);
		assertThrows(IllegalArgumentException.class, ()-> {
			AnnotationsV2Utils.checkValue(key, annotationsV2Value);
		});
	}

	@Test
	public void testCheckValue_ValueTypeNull(){
		// Currently there is a bug with schema-to-pojo
		// where even though "reqiured:true" is set in the JSON schema,
		// no errors are thrown when attempting to set the value type to null.
		// So we must manually check that the type exists

		annotationsV2Value.setType(null);

		assertThrows(IllegalArgumentException.class, ()-> {
			AnnotationsV2Utils.checkValue(key, annotationsV2Value);
		});
	}

	@Test
	public void testCheckValue_ContainsInvalidValues(){
		annotationsV2Value.setValue(Arrays.asList("1.2", "2.3", "notADouble", "3.4"));
		assertThrows(IllegalArgumentException.class, ()-> {
			AnnotationsV2Utils.checkValue(key, annotationsV2Value);
		});
	}

	@Test
	public void testCheckValue_ContainsValidValues(){
		annotationsV2Value.setValue(Arrays.asList("1.2", "2.3", "3.4"));
		assertDoesNotThrow(()-> {
			AnnotationsV2Utils.checkValue(key, annotationsV2Value);
		});
	}

}