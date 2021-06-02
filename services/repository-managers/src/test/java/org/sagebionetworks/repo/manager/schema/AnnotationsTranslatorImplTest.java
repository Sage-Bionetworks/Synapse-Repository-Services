package org.sagebionetworks.repo.manager.schema;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.schema.FORMAT;
import org.sagebionetworks.schema.adapter.org.json.JsonDateUtils;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class AnnotationsTranslatorImplTest {

	Class<? extends Entity> entityClass;
	JSONObject jsonObject;
	JSONArray arrayOfLongs;
	String entityId;
	String etag;

	Project project;
	Annotations annotations;

	AnnotationsTranslatorImpl translator;

	@BeforeEach
	public void before() {
		translator = new AnnotationsTranslatorImpl();

		entityId = "syn123";
		etag = "some-etag";

		entityClass = Project.class;
		jsonObject = new JSONObject();
		// entity values
		jsonObject.put("id", entityId);
		jsonObject.put("etag", etag);
		jsonObject.put("concreteType", Project.class.getName());

		project = new Project();
		project.setId(entityId);
		project.setEtag(etag);
		project.setCreatedBy("123");
		project.setCreatedOn(new Date(1L));
		project.setModifiedBy("444");
		project.setModifiedOn(new Date(2L));
		project.setName("foo");
		project.setParentId("syn901");

		annotations = new Annotations();
		annotations.setId(project.getId());
		annotations.setEtag(project.getEtag());
		AnnotationsV2TestUtils.putAnnotations(annotations, "aString", "some string!", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "listOfLongs", Lists.newArrayList("222", "333"),
				AnnotationsValueType.LONG);
	}

	@Test
	public void testGetAnnotationValueFromJsonObjectWithString() {
		String key = "theKey";
		String value = "a string value";
		JSONObject json = new JSONObject();
		json.put(key, value);
		// call under test
		AnnotationsValue annoValue = translator.getAnnotationValueFromJsonObject(key, json);
		assertNotNull(annoValue);
		assertEquals(AnnotationsValueType.STRING, annoValue.getType());
		List<String> expected = Lists.newArrayList(value);
		assertEquals(expected, annoValue.getValue());
	}

	@Test
	public void testGetAnnotationValueFromJsonObjectWithEmtptyString() {
		String key = "theKey";
		String value = "";
		JSONObject json = new JSONObject();
		json.put(key, value);
		// call under test
		AnnotationsValue annoValue = translator.getAnnotationValueFromJsonObject(key, json);
		assertNotNull(annoValue);
		assertEquals(AnnotationsValueType.STRING, annoValue.getType());
		List<String> expected = Lists.newArrayList(value);
		assertEquals(expected, annoValue.getValue());
	}
	
	@Test
	public void testGetAnnotationValueFromJsonObjectWithBoolean() {
		String key = "theKey";
		Boolean value = Boolean.TRUE;
		JSONObject json = new JSONObject();
		json.put(key, value);
		// call under test
		AnnotationsValue annoValue = translator.getAnnotationValueFromJsonObject(key, json);
		assertNotNull(annoValue);
		assertEquals(AnnotationsValueType.BOOLEAN, annoValue.getType());
		List<String> expected = Lists.newArrayList(value.toString());
		assertEquals(expected, annoValue.getValue());
	}
	
	/**
	 * Not sure we want to support this.
	 */
	@Test
	public void testGetAnnotationValueFromJsonObjectWithObject() {
		String key = "theKey";
		Project value = new Project();
		JSONObject json = new JSONObject();
		json.put(key, value);
		// call under test
		AnnotationsValue annoValue = translator.getAnnotationValueFromJsonObject(key, json);
		assertNotNull(annoValue);
		assertEquals(AnnotationsValueType.STRING, annoValue.getType());
		List<String> expected = Lists.newArrayList(value.toString());
		assertEquals(expected, annoValue.getValue());
	}

	@Test
	public void testGetAnnotationValueFromJsonObjectWithLong() {
		String key = "theKey";
		Long value = 123456L;
		JSONObject json = new JSONObject();
		json.put(key, value);
		// call under test
		AnnotationsValue annoValue = translator.getAnnotationValueFromJsonObject(key, json);
		assertNotNull(annoValue);
		assertEquals(AnnotationsValueType.LONG, annoValue.getType());
		List<String> expected = Lists.newArrayList(value.toString());
		assertEquals(expected, annoValue.getValue());
	}

	@Test
	public void testGetAnnotationValueFromJsonObjectWithInteger() {
		String key = "theKey";
		Integer value = 123;
		JSONObject json = new JSONObject();
		json.put(key, value);
		// call under test
		AnnotationsValue annoValue = translator.getAnnotationValueFromJsonObject(key, json);
		assertNotNull(annoValue);
		assertEquals(AnnotationsValueType.LONG, annoValue.getType());
		List<String> expected = Lists.newArrayList(value.toString());
		assertEquals(expected, annoValue.getValue());
	}

	@Test
	public void testGetAnnotationValueFromJsonObjectWithDouble() {
		String key = "theKey";
		Double value = new Double(3.14);
		JSONObject json = new JSONObject();
		json.put(key, value);
		// call under test
		AnnotationsValue annoValue = translator.getAnnotationValueFromJsonObject(key, json);
		assertNotNull(annoValue);
		assertEquals(AnnotationsValueType.DOUBLE, annoValue.getType());
		List<String> expected = Lists.newArrayList(value.toString());
		assertEquals(expected, annoValue.getValue());
	}
	
	@Test
	public void testGetAnnotationValueFromJsonObjectWithTimestamp() {
		String key = "theKey";
		String value = JsonDateUtils.convertDateToString(FORMAT.DATE_TIME, new Date(222));
		JSONObject json = new JSONObject();
		json.put(key, value);
		// call under test
		AnnotationsValue annoValue = translator.getAnnotationValueFromJsonObject(key, json);
		assertNotNull(annoValue);
		assertEquals(AnnotationsValueType.TIMESTAMP_MS, annoValue.getType());
		List<String> expected = Lists.newArrayList("222");
		assertEquals(expected, annoValue.getValue());
	}

	@Test
	public void testGetAnnotationValueFromJsonObjectWithArrayOfStrings() {
		String key = "theKey";
		JSONArray value = new JSONArray();
		value.put(0, "one");
		value.put(1, "two");
		JSONObject json = new JSONObject();
		json.put(key, value);
		// call under test
		AnnotationsValue annoValue = translator.getAnnotationValueFromJsonObject(key, json);
		assertNotNull(annoValue);
		assertEquals(AnnotationsValueType.STRING, annoValue.getType());
		List<String> expected = Lists.newArrayList("one", "two");
		assertEquals(expected, annoValue.getValue());
	}
	
	@Test
	public void testGetAnnotationValueFromJsonObjectWithArrayOfTimestamps() {
		String key = "theKey";
		JSONArray value = new JSONArray();
		value.put(0, JsonDateUtils.convertDateToString(FORMAT.DATE_TIME, new Date(222)));
		value.put(1, JsonDateUtils.convertDateToString(FORMAT.DATE_TIME, new Date(333)));
		JSONObject json = new JSONObject();
		json.put(key, value);
		// call under test
		AnnotationsValue annoValue = translator.getAnnotationValueFromJsonObject(key, json);
		assertNotNull(annoValue);
		assertEquals(AnnotationsValueType.TIMESTAMP_MS, annoValue.getType());
		List<String> expected = Lists.newArrayList("222", "333");
		assertEquals(expected, annoValue.getValue());
	}

	@Test
	public void testGetAnnotationValueFromJsonObjectWithArrayOfLongs() {
		String key = "theKey";
		JSONArray value = new JSONArray();
		value.put(0, new Long(123));
		value.put(1, new Long(456));
		JSONObject json = new JSONObject();
		json.put(key, value);
		// call under test
		AnnotationsValue annoValue = translator.getAnnotationValueFromJsonObject(key, json);
		assertNotNull(annoValue);
		assertEquals(AnnotationsValueType.LONG, annoValue.getType());
		List<String> expected = Lists.newArrayList("123", "456");
		assertEquals(expected, annoValue.getValue());
	}

	@Test
	public void testGetAnnotationValueFromJsonObjectWithArrayOfDoubles() {
		String key = "theKey";
		JSONArray value = new JSONArray();
		value.put(0, new Double(3.14));
		value.put(1, new Double(4.56));
		JSONObject json = new JSONObject();
		json.put(key, value);
		// call under test
		AnnotationsValue annoValue = translator.getAnnotationValueFromJsonObject(key, json);
		assertNotNull(annoValue);
		assertEquals(AnnotationsValueType.DOUBLE, annoValue.getType());
		List<String> expected = Lists.newArrayList("3.14", "4.56");
		assertEquals(expected, annoValue.getValue());
	}

	@Test
	public void testGetAnnotationValueFromJsonObjectWithArrayOfMixed() {
		String key = "theKey";
		JSONArray value = new JSONArray();
		value.put(0, "a string");
		value.put(1, new Double(3.14));
		value.put(2, new Long(123));
		JSONObject json = new JSONObject();
		json.put(key, value);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getAnnotationValueFromJsonObject(key, json);
		}).getMessage();
		assertEquals("List of mixed types found for key: 'theKey'", message);
	}

	@Test
	public void testReadFromJsonObject() {
		jsonObject.put("aString", "some string");
		jsonObject.put("aLong", "123");
		arrayOfLongs = new JSONArray();
		arrayOfLongs.put(567);
		arrayOfLongs.put(789);
		jsonObject.put("listOfLongs", arrayOfLongs);
		// call under test
		Annotations annotations = translator.readFromJsonObject(entityClass, jsonObject);
		assertNotNull(annotations);
		assertEquals(entityId, annotations.getId());
		assertEquals(etag, annotations.getEtag());
		Map<String, AnnotationsValue> map = annotations.getAnnotations();
		assertNotNull(map);
		assertEquals(3, map.size());

		AnnotationsValue value = map.get("aString");
		assertNotNull(value);
		assertEquals(AnnotationsValueType.STRING, value.getType());
		assertEquals(Lists.newArrayList("some string"), value.getValue());

		value = map.get("aLong");
		assertNotNull(value);
		assertEquals(AnnotationsValueType.LONG, value.getType());
		assertEquals(Lists.newArrayList("123"), value.getValue());

		value = map.get("listOfLongs");
		assertNotNull(value);
		assertEquals(AnnotationsValueType.LONG, value.getType());
		assertEquals(Lists.newArrayList("567", "789"), value.getValue());
	}

	@Test
	public void testReadFromJsonObjectWithUpdatesToEntityFields() {
		jsonObject.put("id", "cannot override the 'id'");
		jsonObject.put("etag", "cannot override 'etag'");
		jsonObject.put("parentId", "cannot override 'parentId'");
		jsonObject.put("createdOn", "cannot override 'createdOn'");
		jsonObject.put("createdBy", "cannot override 'createdBy'");
		jsonObject.put("modifiedOn", "cannot override 'modifiedOn'");
		jsonObject.put("modifiedBy", "cannot override 'modifiedBy'");
		jsonObject.put("name", "cannot override 'name'");
		jsonObject.put("validKey", "validValue");
		jsonObject.put("_KEY_NAME", "valid name");
		// call under test
		Annotations annotations = translator.readFromJsonObject(entityClass, jsonObject);
		assertNotNull(annotations);
		Map<String, AnnotationsValue> map = annotations.getAnnotations();
		assertNotNull(map);
		// only the non-entity key should be in the annotations.
		assertEquals(2, map.size());

		AnnotationsValue value = map.get("validKey");
		assertNotNull(value);
		assertEquals(AnnotationsValueType.STRING, value.getType());
		assertEquals(Lists.newArrayList("validValue"), value.getValue());
		value = map.get("_KEY_NAME");
		assertNotNull(value);
		assertEquals(AnnotationsValueType.STRING, value.getType());
		assertEquals(Lists.newArrayList("valid name"), value.getValue());
	}

	@Test
	public void testReadFromJsonObjectWithNoId() {
		jsonObject.remove("id");
		String message = assertThrows(IllegalArgumentException.class, () -> {
			translator.readFromJsonObject(entityClass, jsonObject);
		}).getMessage();
		assertEquals("Expected JSON to include key: 'id'", message);
	}

	@Test
	public void testReadFromJsonObjectWithNoEtag() {
		jsonObject.remove("etag");
		String message = assertThrows(IllegalArgumentException.class, () -> {
			translator.readFromJsonObject(entityClass, jsonObject);
		}).getMessage();
		assertEquals("Expected JSON to include key: 'etag'", message);
	}

	@Test
	public void testReadFromJsonObjectWithNoConcreteType() {
		jsonObject.remove("concreteType");
		String message = assertThrows(IllegalArgumentException.class, () -> {
			translator.readFromJsonObject(entityClass, jsonObject);
		}).getMessage();
		assertEquals("Expected JSON to include key: 'concreteType'", message);
	}

	@Test
	public void testReadFromJsonObjectWithConcreteTypeDoesNotMatch() {
		jsonObject.put("concreteType", Folder.class.getName());
		String message = assertThrows(IllegalArgumentException.class, () -> {
			translator.readFromJsonObject(entityClass, jsonObject);
		}).getMessage();
		assertEquals("The value of 'concreteType' does not match the type of Entity: 'syn123'", message);
	}
	
	@Test
	public void testReadFromJsonObjectWithNullClass() {
		entityClass = null;
		assertThrows(IllegalArgumentException.class, () -> {
			translator.readFromJsonObject(entityClass, jsonObject);
		});
	}
	
	@Test
	public void testReadFromJsonObjectWithNullJsonObject() {
		jsonObject = null;
		assertThrows(IllegalArgumentException.class, () -> {
			translator.readFromJsonObject(entityClass, jsonObject);
		});
	}

	@Test
	public void testWriteToJsonObject() {
		// call under test
		JSONObject json = translator.writeToJsonObject(project, annotations);
		assertNotNull(json);
		assertEquals(project.getName(), json.getString("name"));
		assertEquals(project.getId(), json.getString("id"));
		assertEquals(project.getEtag(), json.getString("etag"));
		assertEquals(project.getParentId(), json.getString("parentId"));
		assertEquals(project.getCreatedBy(), json.getString("createdBy"));
		assertEquals(project.getModifiedBy(), json.getString("modifiedBy"));
		assertEquals(JsonDateUtils.convertDateToString(FORMAT.DATE_TIME, project.getCreatedOn()),
				json.getString("createdOn"));
		assertEquals(JsonDateUtils.convertDateToString(FORMAT.DATE_TIME, project.getModifiedOn()),
				json.getString("modifiedOn"));
		assertEquals(Project.class.getName(), json.getString("concreteType"));
		// annotations
		assertEquals("some string!", json.getString("aString"));
		JSONArray array = json.getJSONArray("listOfLongs");
		assertNotNull(array);
		assertEquals(2, array.length());
		assertEquals(new Long(222),array.getLong(0));
		assertEquals(new Long(333),array.getLong(1));
	}
	
	@Test
	public void testWriteToJsonObjectWithAnnotationConflictWithEntity() {
		
		// All keys that conflict with entity field names should be ignored.
		AnnotationsV2TestUtils.putAnnotations(annotations, "id", "ignore me!", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "name", "ignore me!", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "parentId", "ignore me!", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "createdBy", "ignore me!", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "createdOn", "ignore me!", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "modifiedOn", "ignore me!", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "modifiedBy", "ignore me!", AnnotationsValueType.STRING);
		
		// call under test
		JSONObject json = translator.writeToJsonObject(project, annotations);
		assertNotNull(json);
		assertEquals(project.getName(), json.getString("name"));
		assertEquals(project.getId(), json.getString("id"));
		assertEquals(project.getEtag(), json.getString("etag"));
		assertEquals(project.getParentId(), json.getString("parentId"));
		assertEquals(project.getCreatedBy(), json.getString("createdBy"));
		assertEquals(project.getModifiedBy(), json.getString("modifiedBy"));
		assertEquals(JsonDateUtils.convertDateToString(FORMAT.DATE_TIME, project.getCreatedOn()),
				json.getString("createdOn"));
		assertEquals(JsonDateUtils.convertDateToString(FORMAT.DATE_TIME, project.getModifiedOn()),
				json.getString("modifiedOn"));
		assertEquals(Project.class.getName(), json.getString("concreteType"));
		// annotations
		assertEquals("some string!", json.getString("aString"));
		JSONArray array = json.getJSONArray("listOfLongs");
		assertNotNull(array);
		assertEquals(2, array.length());
		assertEquals(new Long(222),array.getLong(0));
		assertEquals(new Long(333),array.getLong(1));
	}
	
	@Test
	public void testWriteToJsonObjectWithNullEntity() {
		project = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			translator.writeToJsonObject(project, annotations);
		});
	}
	
	@Test
	public void testWriteToJsonObjectWithNullAnnotations() {
		annotations = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			translator.writeToJsonObject(project, annotations);
		});
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithNullValue() {
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		map.put("nullValue", null);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json);
		assertFalse(json.has("nullValue"));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithNullWithEmptyListValue() {
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		AnnotationsValue annoValue = new AnnotationsValue();
		annoValue.setType(AnnotationsValueType.STRING);
		annoValue.setValue(Collections.emptyList());
		map.put("emptyList", annoValue);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json);
		assertEquals("", json.get("emptyList"));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithNullWithNullType() {
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		AnnotationsValue annoValue = new AnnotationsValue();
		annoValue.setType(null);
		annoValue.setValue(Collections.singletonList("123"));
		map.put("nullType", annoValue);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json);
		assertFalse(json.has("nullType"));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithString() {
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "aString", "someString", AnnotationsValueType.STRING);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json);
		assertEquals("someString", json.getString("aString"));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithLong() {
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "aLong", "123", AnnotationsValueType.LONG);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json);
		assertEquals(new Long(123), json.getLong("aLong"));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithTimeStamp() {
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "aTimestamp", "12345", AnnotationsValueType.TIMESTAMP_MS);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json);
		assertEquals(JsonDateUtils.convertDateToString(FORMAT.DATE_TIME, new Date(12345)), json.getString("aTimestamp"));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithDouble() {
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "aDouble", "3.14", AnnotationsValueType.DOUBLE);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json);
		assertEquals(new Double(3.14), json.getDouble("aDouble"));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithBoolean() {
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "hasBoolean", "true", AnnotationsValueType.BOOLEAN);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json);
		assertEquals(true, json.getBoolean("hasBoolean"));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithListOfStrings() {
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "aListOfStrings", Lists.newArrayList("one","two"), AnnotationsValueType.STRING);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json);
		JSONArray array = json.getJSONArray("aListOfStrings");
		assertNotNull(array);
		assertEquals(2, array.length());
		assertEquals("one", array.getString(0));
		assertEquals("two", array.getString(1));
	}

	@Test
	public void testWriteAnnotationsToJSONObjectWithListOfLongs() {
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "aListOfLongs", Lists.newArrayList("222","333"), AnnotationsValueType.LONG);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json);
		JSONArray array = json.getJSONArray("aListOfLongs");
		assertNotNull(array);
		assertEquals(2, array.length());
		assertEquals(new Long(222), array.getLong(0));
		assertEquals(new Long(333), array.getLong(1));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithListOfDoubles() {
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "aListOfDoubles", Lists.newArrayList("1.22","2.33"), AnnotationsValueType.DOUBLE);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json);
		JSONArray array = json.getJSONArray("aListOfDoubles");
		assertNotNull(array);
		assertEquals(2, array.length());
		assertEquals(new Double(1.22), array.getDouble(0));
		assertEquals(new Double(2.33), array.getDouble(1));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithListOfTimeStamps() {
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "aListOfDoubles", Lists.newArrayList("222","333"), AnnotationsValueType.TIMESTAMP_MS);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json);
		JSONArray array = json.getJSONArray("aListOfDoubles");
		assertNotNull(array);
		assertEquals(2, array.length());
		assertEquals(JsonDateUtils.convertDateToString(FORMAT.DATE_TIME, new Date(222)), array.getString(0));
		assertEquals(JsonDateUtils.convertDateToString(FORMAT.DATE_TIME, new Date(333)), array.getString(1));
	}
}
