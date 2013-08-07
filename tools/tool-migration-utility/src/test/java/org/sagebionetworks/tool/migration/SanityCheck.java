package org.sagebionetworks.tool.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.LocationData;

@Ignore
public class SanityCheck {


	private static Synapse synapse;
	
	private List<Entity> toDelete = null;

	@BeforeClass
	public static void beforeClass() throws Exception {
		// Use the synapse client to do some of the work for us.
		synapse = new Synapse();
		synapse.setAuthEndpoint("https://auth-prod-c.sagebase.org/auth/v1");
		synapse.setRepositoryEndpoint("https://repo-prod-c.sagebase.org/repo/v1");

	}
	
	@After
	public void after() throws Exception {
		if(synapse != null && toDelete != null){
			for(Entity e: toDelete){
				synapse.deleteAndPurgeEntity(e);
			}
		}
	}
	
	@Test
	public void testS3() throws SynapseException{
		Study ds = synapse.getEntity("4494", Study.class);
		assertNotNull(ds);
		assertEquals("ac9ceeafae9e8ccde9059c509a50d38d", ds.getMd5());
//		assertNotNull(ds.getContentType());
		assertNotNull(ds.getLocations());
		assertEquals(1, ds.getLocations().size());
		LocationData locationData = ds.getLocations().iterator().next();
		assertNotNull(locationData);
		System.out.println(locationData);
		String path = locationData.getPath();
		String start = path.split("\\?")[0];
		System.out.println(start);
		assertEquals("https://s3.amazonaws.com/proddata.sagebase.org/4494/4495/mskcc_prostate_cancer.zip", start);
	}
	
	@Test
	public void testExternal() throws SynapseException{
		Data layer = synapse.getEntity("13353", Data.class);
		assertNotNull(layer);
		assertEquals("e231b80b7e69a78636a7c67e88262003", layer.getMd5());
		assertNotNull(layer.getLocations());
		assertEquals(1, layer.getLocations().size());
		LocationData locationData = layer.getLocations().iterator().next();
		assertNotNull(locationData);
		System.out.println(locationData);
		assertEquals("http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/brca/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_BRCA.AgilentG4502A_07_3.Level_1.2.0.0.tar.gz", locationData.getPath());
	}

}
