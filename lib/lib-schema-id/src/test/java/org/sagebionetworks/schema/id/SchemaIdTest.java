package org.sagebionetworks.schema.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.schema.parser.ParseException;
import org.sagebionetworks.schema.parser.SchemaIdParser;
import org.sagebionetworks.schema.semantic.version.SemanticVersion;

import nl.jqno.equalsverifier.EqualsVerifier;

public class SchemaIdTest {

	private OrganizationName organizationName;
	private SchemaName schemaName;
	private SemanticVersion semanticVersion;

	@BeforeEach
	public void before() throws ParseException {
		SchemaIdParser parser = new SchemaIdParser("org.myorg");
		organizationName = parser.organizationName();
		parser = new SchemaIdParser("path.ClassName");
		schemaName = parser.schemaName();
		parser = new SchemaIdParser("1.0.3");
		semanticVersion = parser.semanticVersion();
	}

	@Test
	public void testToString() {
		SchemaId id = new SchemaId(organizationName, schemaName, semanticVersion);
		assertEquals("org.myorg-path.ClassName-1.0.3", id.toString());
	}
	
	@Test
	public void testToStringNoVersion() {
		semanticVersion = null;
		SchemaId id = new SchemaId(organizationName, schemaName, semanticVersion);
		assertEquals("org.myorg-path.ClassName", id.toString());
	}
	
	@Test
	public void testNullOrganizationName() {
		organizationName = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			new SchemaId(organizationName, schemaName, semanticVersion);
		}).getMessage();
		assertEquals("OrganizationName cannot be null", message);
	}
	
	@Test
	public void testNullSchemaName() {
		schemaName = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			new SchemaId(organizationName, schemaName, semanticVersion);
		}).getMessage();
		assertEquals("SchemaName cannot be null", message);
	}
	
	@Test
	public void testHashEquals() {
		EqualsVerifier.forClass(SchemaId.class).verify();
	}
}
