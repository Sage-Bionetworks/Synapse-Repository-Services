package org.sagebionetworks.repo.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class AnnotationJSONTest {
	
	@Test
	public void testBackwardsCompatibility() throws JsonGenerationException, JsonMappingException, IOException, JSONObjectAdapterException{
		ObjectMapper objectMapper = new ObjectMapper();
		Annotations annos = new Annotations();
		annos.setId("123");
		annos.setEtag("456");
		annos.addAnnotation("binary", "This will be binary".getBytes("UTF-8"));
		annos.addAnnotation("date", new Date(System.currentTimeMillis()));
		annos.addAnnotation("double", new Double(123.5));
		annos.addAnnotation("long", new Long(345));
		annos.addAnnotation("string", "String value");
		annos.getStringAnnotations().put("nullList", null);
		annos.getStringAnnotations().put("empty", new ArrayList<String>());
		List<String> withNull = new ArrayList<String>();
		withNull.add(null);
		annos.getStringAnnotations().put("nullValueInList", withNull);
		StringWriter writer = new StringWriter();
		objectMapper.writeValue(writer, annos);
		String objectMapperJson = writer.toString();
		System.out.println(objectMapperJson);
		// Make sure we are generating the same JSON as before
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		annos.writeToJSONObject(adapter);
		// The strings should be the same
		String fromAdapterJson = adapter.toJSONString();
		System.out.println(fromAdapterJson);
		// Make sure one can load the other
		Annotations cloneOne = objectMapper.readValue(fromAdapterJson, Annotations.class);
		assertEquals(annos, cloneOne);
		// Now go the other way
		Annotations cloneTwo = EntityFactory.createEntityFromJSONString(objectMapperJson, Annotations.class);
		assertEquals(annos, cloneTwo);
	}
	

}
