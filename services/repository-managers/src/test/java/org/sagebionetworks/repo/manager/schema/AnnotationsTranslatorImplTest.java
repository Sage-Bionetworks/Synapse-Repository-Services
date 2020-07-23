package org.sagebionetworks.repo.manager.schema;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.schema.BindSchemaRequest;
import org.sagebionetworks.repo.model.dbo.schema.JsonSchemaDao;
import org.sagebionetworks.repo.model.dbo.schema.NewSchemaVersionRequest;
import org.sagebionetworks.repo.model.dbo.schema.OrganizationDao;
import org.sagebionetworks.repo.model.dbo.schema.SchemaDependency;
import org.sagebionetworks.repo.model.schema.BoundObjectType;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaInfo;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoResponse;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoResponse;
import org.sagebionetworks.repo.model.schema.ListOrganizationsRequest;
import org.sagebionetworks.repo.model.schema.ListOrganizationsResponse;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.id.SchemaId;
import org.sagebionetworks.schema.parser.SchemaIdParser;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class AnnotationsTranslatorImplTest {
	
	AnnotationsTranslatorImpl translator;
	
	@BeforeEach
	public void before() {
		translator = new AnnotationsTranslatorImpl();
	}
	
	@Test
	public void testAttemptToReadAsString() {
		String key = "theKey";
		String value = "a string value";
		JSONObject json = new JSONObject();
		json.putOpt(key, value);
		
		// call under test
		Optional<AnnotationsValue> optional = translator.attemptToReadAsString(key, json);
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		AnnotationsValue annoValue = optional.get();
		assertEquals(AnnotationsValueType.STRING, annoValue.getType());
		assertNotNull(annoValue.getValue());
		assertEquals(1, annoValue.getValue().size());
		assertEquals(value, annoValue.getValue().get(0));
	}
	
	@Test
	public void testAttemptToReadAsStringWithEmpty() {
		String key = "theKey";
		String value = "";
		JSONObject json = new JSONObject();
		json.putOpt(key, value);
		
		// call under test
		Optional<AnnotationsValue> optional = translator.attemptToReadAsString(key, json);
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		AnnotationsValue annoValue = optional.get();
		assertEquals(AnnotationsValueType.STRING, annoValue.getType());
		assertNotNull(annoValue.getValue());
		assertEquals(1, annoValue.getValue().size());
		assertEquals(value, annoValue.getValue().get(0));
	}
	
	@Test
	public void testAttemptToReadAsStringWithNull() {
		String key = "theKey";
		String value = null;
		JSONObject json = new JSONObject();
		json.putOpt(key, value);
		
		// call under test
		Optional<AnnotationsValue> optional = translator.attemptToReadAsString(key, json);
		assertNotNull(optional);
		assertFalse(optional.isPresent());
	}
	
	@Test
	public void testAttemptToReadAsLong() {
		String key = "theKey";
		Long value = 123L;
		JSONObject json = new JSONObject();
		json.putOpt(key, value);
		
		// call under test
		Optional<AnnotationsValue> optional = translator.attemptToReadAsLong(key, json);
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		AnnotationsValue annoValue = optional.get();
		assertEquals(AnnotationsValueType.LONG, annoValue.getType());
		assertNotNull(annoValue.getValue());
		assertEquals(1, annoValue.getValue().size());
		assertEquals(value.toString(), annoValue.getValue().get(0));
	}
	
	@Test
	public void testAttemptToReadAsLongWithInt() {
		String key = "theKey";
		Integer value = 123;
		JSONObject json = new JSONObject();
		json.putOpt(key, value);
		
		// call under test
		Optional<AnnotationsValue> optional = translator.attemptToReadAsLong(key, json);
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		AnnotationsValue annoValue = optional.get();
		assertEquals(AnnotationsValueType.LONG, annoValue.getType());
		assertNotNull(annoValue.getValue());
		assertEquals(1, annoValue.getValue().size());
		assertEquals(value.toString(), annoValue.getValue().get(0));
	}
	
	@Test
	public void testAttemptToReadAsLongWithDouble() {
		String key = "theKey";
		Double value = 3.14;
		JSONObject json = new JSONObject();
		json.putOpt(key, value);
		
		// call under test
		Optional<AnnotationsValue> optional = translator.attemptToReadAsLong(key, json);
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		AnnotationsValue annoValue = optional.get();
		assertEquals(AnnotationsValueType.LONG, annoValue.getType());
		assertNotNull(annoValue.getValue());
		assertEquals(1, annoValue.getValue().size());
		// Note the data loss.  Doubles should be attempted before longs.
		assertEquals("3", annoValue.getValue().get(0));
	}
	
	@Test
	public void testAttemptToReadAsLongWithString() {
		String key = "theKey";
		String value = "not a long";
		JSONObject json = new JSONObject();
		json.putOpt(key, value);
		
		// call under test
		Optional<AnnotationsValue> optional = translator.attemptToReadAsLong(key, json);
		assertNotNull(optional);
		assertFalse(optional.isPresent());
	}
	
	@Test
	public void testAttemptToReadAsDouble() {
		String key = "theKey";
		Double value = 3.14;
		JSONObject json = new JSONObject();
		json.putOpt(key, value);
		
		// call under test
		Optional<AnnotationsValue> optional = translator.attemptToReadAsDouble(key, json);
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		AnnotationsValue annoValue = optional.get();
		assertEquals(AnnotationsValueType.DOUBLE, annoValue.getType());
		assertNotNull(annoValue.getValue());
		assertEquals(1, annoValue.getValue().size());
		// Note the data loss.  Doubles should be attempted before longs.
		assertEquals(value.toString(), annoValue.getValue().get(0));
	}
	
	@Test
	public void testAttemptToReadAsDoubleWithNoDecimals() {
		String key = "theKey";
		Double value = new Double(3);
		JSONObject json = new JSONObject();
		json.putOpt(key, value);
		System.out.println(json.toString(5));
		
		// call under test
		Optional<AnnotationsValue> optional = translator.attemptToReadAsDouble(key, json);
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		AnnotationsValue annoValue = optional.get();
		assertEquals(AnnotationsValueType.DOUBLE, annoValue.getType());
		assertNotNull(annoValue.getValue());
		assertEquals(1, annoValue.getValue().size());
		// Note the data loss.  Doubles should be attempted before longs.
		assertEquals(value.toString(), annoValue.getValue().get(0));
	}
	
	@Test
	public void testReadFromJsonObject() {
		JSONObject json = new JSONObject();
		json.put("name", "ignoreMe!");
		json.put("id", "syn123");
		json.put("etag", "the-etag");
		
		// call under test
		translator.readFromJsonObject(Project.class, jsonObject)
	}


}
