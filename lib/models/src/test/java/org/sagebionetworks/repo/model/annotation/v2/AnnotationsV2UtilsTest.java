package org.sagebionetworks.repo.model.annotation.v2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

class AnnotationsV2UtilsTest {

	private AnnotationsV2 annotationsV2;
	private AnnotationsV2Value annotationsV2Value;
	private String key;

	@BeforeEach
	public void setUp(){
		key = "myKey";
		annotationsV2 = new AnnotationsV2();
		annotationsV2Value = new AnnotationsV2Value();
		annotationsV2.setAnnotations(Collections.singletonMap(key,annotationsV2Value));
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
}