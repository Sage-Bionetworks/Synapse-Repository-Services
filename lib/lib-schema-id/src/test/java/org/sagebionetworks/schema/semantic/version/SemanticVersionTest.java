package org.sagebionetworks.schema.semantic.version;


import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.schema.parser.ParseException;
import org.sagebionetworks.schema.parser.SchemaIdParser;

import nl.jqno.equalsverifier.EqualsVerifier;

public class SemanticVersionTest {

	VersionCore core;
	Prerelease prerelease;
	Build build;
	
	@BeforeEach
	public void before() throws ParseException {
		SchemaIdParser parser = new SchemaIdParser("1.2.3");
		core = parser.versionCore(); 
		parser = new SchemaIdParser("alpha");
		prerelease = parser.prerelease();
		parser = new SchemaIdParser("123f45");
		build = parser.build();
	}
	
	@Test
	public void testToString() {
		SemanticVersion version = new SemanticVersion(core, prerelease, build);
		assertEquals("1.2.3-alpha+123f45", version.toString());
	}
	
	@Test
	public void testToStringNullPrerelease() {
		prerelease = null;
		SemanticVersion version = new SemanticVersion(core, prerelease, build);
		assertEquals("1.2.3+123f45", version.toString());
	}
	
	@Test
	public void testToStringNullBuild() {
		build = null;
		SemanticVersion version = new SemanticVersion(core, prerelease, build);
		assertEquals("1.2.3-alpha", version.toString());
	}
	
	@Test
	public void testNullCore() {
		core = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			new SemanticVersion(core, prerelease, build);
		}).getMessage();
		assertEquals("Core cannot be null", message);
	}
	
	@Test
	public void testHashAndEquals() {
		EqualsVerifier.forClass(SemanticVersion.class).verify();
	}
}
