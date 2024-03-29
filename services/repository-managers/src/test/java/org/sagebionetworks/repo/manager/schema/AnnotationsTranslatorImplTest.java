package org.sagebionetworks.repo.manager.schema;

import com.google.common.collect.Lists;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.SchemaDataType;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.schema.FORMAT;
import org.sagebionetworks.schema.adapter.org.json.JsonDateUtils;
import org.sagebionetworks.util.doubles.DoubleJSONStringWrapper;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
	
	Map<String, JsonSchema> properties;
	JsonSchema schema;

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
		
		properties = new HashMap<>();
		schema = new JsonSchema();
		schema.setProperties(properties);
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
	public void testGetAnnotationValueFromJsonObjectWithEmptyString() {
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

	@Test
	public void testGetAnnotationValueFromJsonObjectWithBooleanAsString() {
		String key = "theKey";
		String value = "true";
		JSONObject json = new JSONObject();
		json.put(key, value);
		// call under test
		AnnotationsValue annoValue = translator.getAnnotationValueFromJsonObject(key, json);
		assertNotNull(annoValue);
		assertEquals(AnnotationsValueType.STRING, annoValue.getType());
		List<String> expected = Lists.newArrayList(value);
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
	public void testGetAnnotationValueFromJsonObjectWithLongAsStringValue() {
		String key = "theKey";
		String value = "123456";
		JSONObject json = new JSONObject();
		json.put(key, "123456");
		// call under test
		AnnotationsValue annoValue = translator.getAnnotationValueFromJsonObject(key, json);
		assertNotNull(annoValue);
		assertEquals(AnnotationsValueType.STRING, annoValue.getType());
		List<String> expected = Lists.newArrayList(value);
		assertEquals(expected, annoValue.getValue());
	}

	@Test
	public void testGetAnnotationValueFromJsonObjectWithNullValue() {
		String key = "theKey";
		JSONObject json = new JSONObject();
		json.put(key, JSONObject.NULL);
		// call under test
		assertEquals("null is not allowed as a value for key: 'theKey'",
				assertThrows(IllegalArgumentException.class, () -> {
					translator.getAnnotationValueFromJsonObject(key, json);
		}).getMessage());
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
	public void testGetAnnotationValueFromJsonObjectWithDoubleAsString() {
		String key = "theKey";
		String value = "3.14";
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
	public void testGetAnnotationValueFromJsonObjectWithArrayHavingNull() {
		String key = "theKey";
		JSONArray value = new JSONArray();
		value.put(0, new Long(123));
		value.put(1, JSONObject.NULL);
		JSONObject json = new JSONObject();
		json.put(key, value);
		// call under test
		assertEquals("null is not allowed as a value in the list for key: 'theKey'",
				assertThrows(IllegalArgumentException.class, () -> {
					translator.getAnnotationValueFromJsonObject(key, json);
				}).getMessage());
	}

	@Test
	public void testGetAnnotationValueFromJsonObjectWithArrayOfLongsAsString() {
		String key = "theKey";
		JSONArray value = new JSONArray();
		value.put(0, "123");
		value.put(1, "456");
		JSONObject json = new JSONObject();
		json.put(key, value);
		// call under test
		AnnotationsValue annoValue = translator.getAnnotationValueFromJsonObject(key, json);
		assertNotNull(annoValue);
		assertEquals(AnnotationsValueType.STRING, annoValue.getType());
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
	public void testGetAnnotationValueFromJsonObjectWithArrayOfDoublesAsString() {
		String key = "theKey";
		JSONArray value = new JSONArray();
		value.put(0, "3.14");
		value.put(1, "4.56");
		JSONObject json = new JSONObject();
		json.put(key, value);
		// call under test
		AnnotationsValue annoValue = translator.getAnnotationValueFromJsonObject(key, json);
		assertNotNull(annoValue);
		assertEquals(AnnotationsValueType.STRING, annoValue.getType());
		List<String> expected = Lists.newArrayList("3.14", "4.56");
		assertEquals(expected, annoValue.getValue());
	}

	@Test
	public void testGetAnnotationValueFromJsonObjectWithArrayOfBooleans() {
		String key = "theKey";
		JSONArray value = new JSONArray();
		value.put(0, true);
		value.put(1, false);
		JSONObject json = new JSONObject();
		json.put(key, value);
		// call under test
		AnnotationsValue annoValue = translator.getAnnotationValueFromJsonObject(key, json);
		assertNotNull(annoValue);
		assertEquals(AnnotationsValueType.BOOLEAN, annoValue.getType());
		List<String> expected = Lists.newArrayList("true", "false");
		assertEquals(expected, annoValue.getValue());
	}

	@Test
	public void testGetAnnotationValueFromJsonObjectWithArrayOfBooleansASString() {
		String key = "theKey";
		JSONArray value = new JSONArray();
		value.put(0, "true");
		value.put(1, "false");
		JSONObject json = new JSONObject();
		json.put(key, value);
		// call under test
		AnnotationsValue annoValue = translator.getAnnotationValueFromJsonObject(key, json);
		assertNotNull(annoValue);
		assertEquals(AnnotationsValueType.STRING, annoValue.getType());
		List<String> expected = Lists.newArrayList("true", "false");
		assertEquals(expected, annoValue.getValue());
	}

	@Test
	public void testGetAnnotationValueFromJsonObjectWithArrayOfMixed() {
		String key = "theKey";
		JSONArray value = new JSONArray();
		value.put(0, "a string");
		value.put(1, Double.valueOf(3.14));
		value.put(2, Long.valueOf(123));
		JSONObject json = new JSONObject();
		json.put(key, value);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getAnnotationValueFromJsonObject(key, json);
		}).getMessage();
		assertEquals("List of mixed types found for key: 'theKey'", message);
	}
	
	@Test
	public void testGetAnnotationValueFromJsonObjectWithArrayOfMixedDoubleAndLongs() {
		String key = "theKey";
		JSONArray value = new JSONArray(
			Arrays.asList(0, 3.14, 1, 2, 3, 7.28, 9)
		);
		JSONObject json = new JSONObject();
		json.put(key, value);
		AnnotationsValue expected = new AnnotationsValue()
				.setType(AnnotationsValueType.DOUBLE)
				.setValue(Arrays.asList("0", "3.14", "1", "2", "3", "7.28", "9"));
		// call under test
		AnnotationsValue result = translator.getAnnotationValueFromJsonObject(key, json);
		
		assertEquals(expected, result);
		
	}

	@Test
	public void testReadFromJsonObject() {
		jsonObject.put("aString", "some string");
		jsonObject.put("aLong", 123L);
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
		schema = null;
		JSONObject json = translator.writeToJsonObject(project, annotations, schema);
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
		schema = null;
		
		// call under test
		JSONObject json = translator.writeToJsonObject(project, annotations, schema);
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
		schema = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			translator.writeToJsonObject(project, annotations, schema);
		});
	}
	
	@Test
	public void testWriteToJsonObjectWithNullAnnotations() {
		annotations = null;
		schema = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			translator.writeToJsonObject(project, annotations, schema);
		});
	}
	
	@Test
	public void testWriteToJsonObjectWithSchema() {
		// we put the annotations not as a list, but as a single value of 333
		AnnotationsV2TestUtils.putAnnotations(annotations, "listOfDoubles", "333",
				AnnotationsValueType.DOUBLE);
		JsonSchema doublesListTypeSchema = new JsonSchema();
		doublesListTypeSchema.setType(Type.array);
		properties.put("listOfDoubles", doublesListTypeSchema);
		// call under test
		JSONObject json = translator.writeToJsonObject(project, annotations, schema);
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
		// should be a list of doubles as defined by the schema
		array = json.getJSONArray("listOfDoubles");
		assertNotNull(array);
		assertEquals(1, array.length());
		assertEquals(new Double(333),array.getLong(0));
	}
	
	@Test
	public void testWriteToJsonObjectWithSchemaWithAnnotationConflictWithEntity() {
		
		// All keys that conflict with entity field names should be ignored.
		AnnotationsV2TestUtils.putAnnotations(annotations, "id", "ignore me!", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "name", "ignore me!", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "parentId", "ignore me!", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "createdBy", "ignore me!", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "createdOn", "ignore me!", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "modifiedOn", "ignore me!", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "modifiedBy", "ignore me!", AnnotationsValueType.STRING);
		
		AnnotationsV2TestUtils.putAnnotations(annotations, "modifiedBy", "list of modified by me",
				AnnotationsValueType.STRING);
		JsonSchema modifiedByListTypeSchema = new JsonSchema();
		modifiedByListTypeSchema.setType(Type.array);
		properties.put("modifiedBy", modifiedByListTypeSchema);
		
		// call under test
		JSONObject json = translator.writeToJsonObject(project, annotations, schema);
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
	public void testBuildJsonSchemaIsSingleMap() throws Exception {
		JsonSchema schema = SchemaTestUtils.loadSchemaFromClasspath("schemas/ComplexReferences.json");
		// call under test
		Map<String, SchemaDataType> map = translator.buildJsonSchemaIsSingleMap(schema);
		Map<String, SchemaDataType> expected = new HashMap<>(10);
		expected.put("simple-single-ref", SchemaDataType.SINGLE);
		expected.put("simple-array-ref", SchemaDataType.ARRAY);
		expected.put("array-items-ref", SchemaDataType.ARRAY);
		expected.put("ref-to-const", SchemaDataType.SINGLE);
		expected.put("ref-to-enum", SchemaDataType.SINGLE);
		expected.put("inside-if", SchemaDataType.SINGLE);
		expected.put("then-value", SchemaDataType.ARRAY);
		expected.put("else-value", SchemaDataType.SINGLE);
		assertEquals(expected, map);
	}
	
	@Test
	public void testBuildJsonSchemaIsSingleMapWithNullDefinitions() throws Exception {
		JsonSchema schema = SchemaTestUtils.loadSchemaFromClasspath("schemas/ComplexReferences.json");
		schema.setDefinitions(null);
		// call under test
		Map<String, SchemaDataType> map = translator.buildJsonSchemaIsSingleMap(schema);
		// without valid references everything is false.
		Map<String, SchemaDataType> expected = new HashMap<>(10);
		expected.put("simple-single-ref", SchemaDataType.NOT_DEFINED);
		expected.put("simple-array-ref", SchemaDataType.NOT_DEFINED);
		expected.put("array-items-ref", SchemaDataType.ARRAY);
		expected.put("ref-to-const", SchemaDataType.NOT_DEFINED);
		expected.put("ref-to-enum", SchemaDataType.NOT_DEFINED);
		expected.put("inside-if", SchemaDataType.NOT_DEFINED);
		expected.put("then-value", SchemaDataType.NOT_DEFINED);
		expected.put("else-value", SchemaDataType.NOT_DEFINED);
		assertEquals(expected, map);
	}
	
	@Test
	public void testGetRelative$Ref() {
		assertEquals("bar", translator.getRelative$Ref("#/definitions/bar"));
		assertEquals("foo", translator.getRelative$Ref("foo"));
		assertEquals("", translator.getRelative$Ref("#/definitions/"));
	}
	
	@Test
	public void testBuildJsonSchemaIsSingleMapWithBad$ref() throws Exception {
		JsonSchema schema = new JsonSchema().setProperties(new HashMap<String, JsonSchema>());
		schema.getProperties().put("has-bad-ref", new JsonSchema().set$ref("wrong"));
		
		schema.setDefinitions(null);
		// call under test
		Map<String, SchemaDataType> map = translator.buildJsonSchemaIsSingleMap(schema);
		Map<String, SchemaDataType> expected = new HashMap<>(1);
		expected.put("has-bad-ref", SchemaDataType.NOT_DEFINED);
		assertEquals(expected, map);
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithNullValue() {
		schema = null;
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		AnnotationsValue annoValue = null;
		map.put("nullValue", annoValue);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertFalse(json.has("nullValue"));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithNullWithEmptyListValue() {
		schema = null;
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		AnnotationsValue annoValue = new AnnotationsValue();
		annoValue.setType(AnnotationsValueType.STRING);
		annoValue.setValue(Collections.emptyList());
		map.put("emptyList", annoValue);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals("", json.get("emptyList"));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithNullWithNullType() {
		schema = null;
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		AnnotationsValue annoValue = new AnnotationsValue();
		annoValue.setType(null);
		annoValue.setValue(Collections.singletonList("123"));
		map.put("nullType", annoValue);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertFalse(json.has("nullType"));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithString() {
		schema = null;
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "aString", "someString", AnnotationsValueType.STRING);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals("someString", json.getString("aString"));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithLong() {
		schema = null;
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "aLong", "123", AnnotationsValueType.LONG);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals(new Long(123), json.getLong("aLong"));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithTimeStamp() {
		schema = null;
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "aTimestamp", "12345", AnnotationsValueType.TIMESTAMP_MS);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals(JsonDateUtils.convertDateToString(FORMAT.DATE_TIME, new Date(12345)), json.getString("aTimestamp"));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithDouble() {
		schema = null;
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "aDouble", "3.14", AnnotationsValueType.DOUBLE);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals(new Double(3.14), json.getDouble("aDouble"));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithBoolean() {
		schema = null;
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "hasBoolean", "true", AnnotationsValueType.BOOLEAN);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals(true, json.getBoolean("hasBoolean"));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithListOfStrings() {
		schema = null;
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "aListOfStrings", Lists.newArrayList("one","two"), AnnotationsValueType.STRING);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		JSONArray array = json.getJSONArray("aListOfStrings");
		assertNotNull(array);
		assertEquals(2, array.length());
		assertEquals("one", array.getString(0));
		assertEquals("two", array.getString(1));
	}

	@Test
	public void testWriteAnnotationsToJSONObjectWithListOfLongs() {
		schema = null;
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "aListOfLongs", Lists.newArrayList("222","333"), AnnotationsValueType.LONG);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		JSONArray array = json.getJSONArray("aListOfLongs");
		assertNotNull(array);
		assertEquals(2, array.length());
		assertEquals(new Long(222), array.getLong(0));
		assertEquals(new Long(333), array.getLong(1));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithListOfDoubles() {
		schema = null;
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "aListOfDoubles", Lists.newArrayList("1.22","2.33", "1.0"), AnnotationsValueType.DOUBLE);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		JSONArray array = json.getJSONArray("aListOfDoubles");
		assertEquals(Arrays.asList(
			new DoubleJSONStringWrapper(1.22),
			new DoubleJSONStringWrapper(2.33),
			new DoubleJSONStringWrapper(1.0)
		), array.toList());
		// https://sagebionetworks.jira.com/browse/PLFM-6890 Make sure that when the json array is serialized the trailing 0s are preserved
		assertEquals("[1.22,2.33,1.0]", array.toString());
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithListOfTimeStamps() {
		schema = null;
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "aListOfDoubles", Lists.newArrayList("222","333"), AnnotationsValueType.TIMESTAMP_MS);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		JSONArray array = json.getJSONArray("aListOfDoubles");
		assertNotNull(array);
		assertEquals(2, array.length());
		assertEquals(JsonDateUtils.convertDateToString(FORMAT.DATE_TIME, new Date(222)), array.getString(0));
		assertEquals(JsonDateUtils.convertDateToString(FORMAT.DATE_TIME, new Date(333)), array.getString(1));
	}

	@Test
	public void testWriteAnnotationsToJSONObjectWithListOfBooleans() {
		schema = null;
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "aListOfBooleans", Lists.newArrayList("true", "false"), AnnotationsValueType.BOOLEAN);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		JSONArray array = json.getJSONArray("aListOfBooleans");
		assertNotNull(array);
		assertEquals(2, array.length());
		assertEquals(true, array.getBoolean(0));
		assertEquals(false, array.getBoolean(1));
	}

	@Test
	public void testWriteAnnotationsToJSONObjectWithSchemaArrayAndAnnotationArray() {
		JsonSchema typeSchema = new JsonSchema();
		typeSchema.setType(Type.array);
		properties.put("key", typeSchema);
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		AnnotationsValue annoValue = new AnnotationsValue();
		annoValue.setType(AnnotationsValueType.STRING);
		annoValue.setValue(Arrays.asList("foo", "bar"));
		map.put("key", annoValue);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals(json.getJSONArray("key").getString(0), "foo");
		assertEquals(json.getJSONArray("key").getString(1), "bar");
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithSchemaSingleAndAnnotationArray() {
		JsonSchema typeSchema = new JsonSchema();
		typeSchema.setType(Type.string);
		properties.put("key", typeSchema);
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		AnnotationsValue annoValue = new AnnotationsValue();
		annoValue.setType(AnnotationsValueType.STRING);
		annoValue.setValue(Arrays.asList("foo", "bar"));
		map.put("key", annoValue);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals(json.getJSONArray("key").getString(0), "foo");
		assertEquals(json.getJSONArray("key").getString(1), "bar");
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithSchemaSingleAndAnnotationSingle() {
		JsonSchema typeSchema = new JsonSchema();
		typeSchema.setType(Type.string);
		properties.put("key", typeSchema);
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		AnnotationsValue annoValue = new AnnotationsValue();
		annoValue.setType(AnnotationsValueType.STRING);
		annoValue.setValue(Arrays.asList("foo"));
		map.put("key", annoValue);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals(json.getString("key"), "foo");
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithSchemaArrayAndAnnotationSingle() {
		JsonSchema typeSchema = new JsonSchema();
		typeSchema.setType(Type.array);
		properties.put("key", typeSchema);
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		AnnotationsValue annoValue = new AnnotationsValue();
		annoValue.setType(AnnotationsValueType.STRING);
		annoValue.setValue(Arrays.asList("foo"));
		map.put("key", annoValue);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals(json.getJSONArray("key").getString(0), "foo");
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithSchemaConstAndAnnotationSingle() {
		JsonSchema typeSchema = new JsonSchema();
		typeSchema.set_const("some value");
		properties.put("key", typeSchema);
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		AnnotationsValue annoValue = new AnnotationsValue();
		annoValue.setType(AnnotationsValueType.STRING);
		annoValue.setValue(Arrays.asList("foo"));
		map.put("key", annoValue);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals(json.getString("key"), "foo");
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithSchemaConstAndAnnotationArray() {
		JsonSchema typeSchema = new JsonSchema();
		typeSchema.set_const("some value");
		properties.put("key", typeSchema);
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		AnnotationsValue annoValue = new AnnotationsValue();
		annoValue.setType(AnnotationsValueType.STRING);
		annoValue.setValue(Arrays.asList("foo", "bar"));
		map.put("key", annoValue);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals(json.getJSONArray("key").getString(0), "foo");
		assertEquals(json.getJSONArray("key").getString(1), "bar");
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithSchemaEnumAndAnnotationSingle() {
		JsonSchema typeSchema = new JsonSchema();
		typeSchema.set_enum(Arrays.asList("enum1", "enum2"));
		properties.put("key", typeSchema);
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		AnnotationsValue annoValue = new AnnotationsValue();
		annoValue.setType(AnnotationsValueType.STRING);
		annoValue.setValue(Arrays.asList("foo"));
		map.put("key", annoValue);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals(json.getString("key"), "foo");
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithSchemaEnumAndAnnotationArray() {
		JsonSchema typeSchema = new JsonSchema();
		typeSchema.set_enum(Arrays.asList("enum1", "enum2"));
		properties.put("key", typeSchema);
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		AnnotationsValue annoValue = new AnnotationsValue();
		annoValue.setType(AnnotationsValueType.STRING);
		annoValue.setValue(Arrays.asList("foo", "bar"));
		map.put("key", annoValue);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals(json.getJSONArray("key").getString(0), "foo");
		assertEquals(json.getJSONArray("key").getString(1), "bar");
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithAllNullSchemaTypeAndAnnotationSingle() {
		// this is where type, enum, and const are all null
		JsonSchema typeSchema = new JsonSchema();
		properties.put("key", typeSchema);
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		AnnotationsValue annoValue = new AnnotationsValue();
		annoValue.setType(AnnotationsValueType.STRING);
		annoValue.setValue(Arrays.asList("foo"));
		map.put("key", annoValue);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals("foo", json.getString("key"));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithAllNullSchemaTypeAndAnnotationArray() {
		// this is where type, enum, and const are all null
		JsonSchema typeSchema = new JsonSchema();
		properties.put("key", typeSchema);
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		AnnotationsValue annoValue = new AnnotationsValue();
		annoValue.setType(AnnotationsValueType.STRING);
		annoValue.setValue(Arrays.asList("foo", "bar"));
		map.put("key", annoValue);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals(json.getJSONArray("key").getString(0), "foo");
		assertEquals(json.getJSONArray("key").getString(1), "bar");
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithReferencedSchemaAndSingleDefined() {
		// should not default to an array
		JsonSchema referencedSchema = new JsonSchema();
		referencedSchema.set$id("org-id");
		referencedSchema.setType(Type.string);
		JsonSchema reference = new JsonSchema();
		reference.set$ref("#/definitions/" + referencedSchema.get$id());
		properties.put("key", reference);
		Map<String, JsonSchema> definitions = new HashMap<>();
		definitions.put(referencedSchema.get$id(), referencedSchema);
		schema.setDefinitions(definitions);
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		AnnotationsValue annoValue = new AnnotationsValue();
		annoValue.setType(AnnotationsValueType.STRING);
		annoValue.setValue(Arrays.asList("foo"));
		map.put("key", annoValue);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals(json.getString("key"), "foo");
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithReferencedSchemaAndNoDefintions() {
		// should not default to an array
		JsonSchema referencedSchema = new JsonSchema();
		referencedSchema.set$id("org-id");
		referencedSchema.setType(Type.string);
		JsonSchema reference = new JsonSchema();
		reference.set$ref("#/definitions/" + referencedSchema.get$id());
		properties.put("key", reference);
		Map<String, JsonSchema> definitions = new HashMap<>();
		definitions.put(referencedSchema.get$id(), referencedSchema);
		schema.setDefinitions(null);
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		AnnotationsValue annoValue = new AnnotationsValue();
		annoValue.setType(AnnotationsValueType.STRING);
		annoValue.setValue(Arrays.asList("foo"));
		map.put("key", annoValue);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals("foo", json.getString("key"));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithReferencedSchemaAndArrayDefined() {
		// should stay as an array
		JsonSchema referencedSchema = new JsonSchema();
		referencedSchema.set$id("org-id");
		referencedSchema.setType(Type.array);
		JsonSchema reference = new JsonSchema();
		reference.set$ref("#/definitions/" + referencedSchema.get$id());
		properties.put("key", reference);
		Map<String, JsonSchema> definitions = new HashMap<>();
		definitions.put(referencedSchema.get$id(), referencedSchema);
		schema.setDefinitions(definitions);
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		AnnotationsValue annoValue = new AnnotationsValue();
		annoValue.setType(AnnotationsValueType.STRING);
		annoValue.setValue(Arrays.asList("foo"));
		map.put("key", annoValue);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals(json.getJSONArray("key").getString(0), "foo");
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithReferencedSchemaAndConstDefined() {
		// should not default to an array
		JsonSchema referencedSchema = new JsonSchema();
		referencedSchema.set$id("org-id");
		referencedSchema.set_const("foo");
		JsonSchema reference = new JsonSchema();
		reference.set$ref("#/definitions/" + referencedSchema.get$id());
		properties.put("key", reference);
		Map<String, JsonSchema> definitions = new HashMap<>();
		definitions.put(referencedSchema.get$id(), referencedSchema);
		schema.setDefinitions(definitions);
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		AnnotationsValue annoValue = new AnnotationsValue();
		annoValue.setType(AnnotationsValueType.STRING);
		annoValue.setValue(Arrays.asList("foo"));
		map.put("key", annoValue);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals(json.getString("key"), "foo");
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithReferencedSchemaAndEnumDefined() {
		// should not default to an array
		JsonSchema referencedSchema = new JsonSchema();
		referencedSchema.set$id("org-id");
		referencedSchema.set_enum(Arrays.asList("foo", "bar"));
		JsonSchema reference = new JsonSchema();
		reference.set$ref("#/definitions/" + referencedSchema.get$id());
		properties.put("key", reference);
		Map<String, JsonSchema> definitions = new HashMap<>();
		definitions.put(referencedSchema.get$id(), referencedSchema);
		schema.setDefinitions(definitions);
		Annotations toWrite = new Annotations();
		Map<String, AnnotationsValue> map = new LinkedHashMap<String, AnnotationsValue>();
		AnnotationsValue annoValue = new AnnotationsValue();
		annoValue.setType(AnnotationsValueType.STRING);
		annoValue.setValue(Arrays.asList("foo"));
		map.put("key", annoValue);
		toWrite.setAnnotations(map);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		assertEquals(json.getString("key"), "foo");
	}

	@Test
	public void testWriteAnnotationsToJSONObjectWithNaNValue() {
		// See PLFM-6872 -- NaN is not a valid JSON value, so we replace it with the string "NaN"
		schema = null;
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "aListOfDoubles", Lists.newArrayList("1.22","NaN", "nan"), AnnotationsValueType.DOUBLE);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		JSONArray array = json.getJSONArray("aListOfDoubles");
		assertNotNull(array);
		// If the array includes Double.NaN, it will throw an error on write.
		assertDoesNotThrow(() -> {array.write(new StringWriter());});
		assertEquals(3, array.length());
		assertEquals(new Double(1.22), array.getDouble(0));
		assertEquals("NaN", array.getString(1));
		assertEquals("NaN", array.getString(2));
	}
	
	@Test
	public void testWriteAnnotationsToJSONObjectWithInfinityValue() {
		// See PLFM-6872 -- NaN is not a valid JSON value, so we replace it with the string "NaN"
		schema = null;
		Annotations toWrite = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(toWrite, "aListOfDoubles", Lists.newArrayList("infinity", "Infinity", "Inf", "+Inf", "-Infinity", "-infinity", "-Inf"), AnnotationsValueType.DOUBLE);
		JSONObject json = new JSONObject();
		// call under test
		translator.writeAnnotationsToJSONObject(toWrite, json, schema);
		JSONArray array = json.getJSONArray("aListOfDoubles");
		assertNotNull(array);
		// If the array includes Double.NaN, it will throw an error on write.
		assertDoesNotThrow(() -> {array.write(new StringWriter());});
		assertEquals(7, array.length());
		assertEquals("Infinity", array.getString(0));
		assertEquals("Infinity", array.getString(1));
		assertEquals("Infinity", array.getString(2));
		assertEquals("Infinity", array.getString(3));
		assertEquals("-Infinity", array.getString(4));
		assertEquals("-Infinity", array.getString(5));
		assertEquals("-Infinity", array.getString(6));
	}
}
