package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sagebionetworks.repo.manager.backup.migration.MigrationDriver;
import org.sagebionetworks.repo.manager.backup.migration.MigrationDriverImpl;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.EntityType;

/**
 * Validate that we can migrate from all version of the xml.
 * still read the previous version of the revision objects.
 * 
 * @author John
 * 
 */
@RunWith(Parameterized.class)
public class MigrationDriverImplTest {
	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				// We must be able to load the V0 xml files into the current version
				{ "node-revision-dataset-v0.xml", SerializationUseCases.createV1DatasetRevision(), EntityType.dataset },
				{ "node-revision-project-v0.xml", SerializationUseCases.createV1ProjectRevision(), EntityType.project  },
				// We must be able to load the V1 xml files into the current version
				{ "node-revision-dataset-v1.xml", SerializationUseCases.createV1DatasetRevision(), EntityType.dataset  },
				{ "node-revision-project-v1.xml", SerializationUseCases.createV1ProjectRevision(), EntityType.project  },
				{ "node-revision-step-v1.xml", SerializationUseCases.createV1StepRevision(), EntityType.step  },
		});
	}

	String fileNameToLoad;
	NodeRevisionBackup expectedRevision;
	EntityType type;
	MigrationDriver migrationDriver = new MigrationDriverImpl();
	

	public MigrationDriverImplTest(String fileNameToLoad, NodeRevisionBackup expectedRevision, EntityType type) {
		this.fileNameToLoad = fileNameToLoad;
		this.expectedRevision = expectedRevision;
		this.type = type;
	}
	

	@Test
	public void testMigrateToCurrent(){
		// validate that we can load and migrate each version.
		InputStream in = MigrationDriverImplTest.class.getClassLoader().getResourceAsStream(fileNameToLoad);
		if(in == null) throw new IllegalArgumentException("Failed to find file: "+fileNameToLoad+" on the classpath");
		NodeRevisionBackup revision = NodeSerializerUtil.readNodeRevision(in);
		// Migrate the revision to the current version
		System.out.println("Starting with version: "+revision.getXmlVersion()+"...");
		revision = migrationDriver.migrateToCurrentVersion(revision, this.type);
		System.out.println("Migrated to: "+revision.getXmlVersion());
		// Make sure the revision is on the current version
		assertEquals(NodeRevisionBackup.CURRENT_XML_VERSION, revision.getXmlVersion());
		assertEquals(expectedRevision, revision);
	}


}
