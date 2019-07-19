package org.sagebionetworks.repo.model.annotation.v2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

class AnnotationsV2UtilsTest {

	@Test
	void createNewValue(){
		AnnotationsV2ValueType type = AnnotationsV2ValueType.DOUBLE;
		List<String> values = Arrays.asList("1.2", "2.3");

		//method under test
		AnnotationsV2Value created = AnnotationsV2Utils.createNewValue(type, values);

		assertEquals(type, created.getType());
		assertEquals(values, created.getValue());
	}

	@Test
	void toJSONStringForStorage_nullAnnotationV2() throws JSONObjectAdapterException {
		assertNull(AnnotationsV2Utils.toJSONStringForStorage(null));
	}


	@Test
	void toJSONStringForStorage_nullAnnotationsMap() throws JSONObjectAdapterException {
		AnnotationsV2 annotationsV2 = new AnnotationsV2();
		assertNull(AnnotationsV2Utils.toJSONStringForStorage(annotationsV2));
	}

	@Test
	void toJSONStringForStorage_EmptyAnnotationsMap() throws JSONObjectAdapterException {
		AnnotationsV2 annotationsV2 = new AnnotationsV2();
		annotationsV2.setAnnotations(Collections.emptyMap());
		assertNull(AnnotationsV2Utils.toJSONStringForStorage(annotationsV2));
	}

	@Test
	void toJSONStringForStorage_AnnotationsMapWithEntries() throws JSONObjectAdapterException {
		AnnotationsV2 annotationsV2 = new AnnotationsV2();
		annotationsV2.setAnnotations(Collections.singletonMap("key", AnnotationsV2Utils.createNewValue(AnnotationsV2ValueType.STRING, "value1")));
		assertEquals("{\"annotations\":[{\"key\":\"key\",\"value\":{\"type\":\"STRING\",\"value\":[\"value1\"]}}]}", AnnotationsV2Utils.toJSONStringForStorage(annotationsV2));
	}
}