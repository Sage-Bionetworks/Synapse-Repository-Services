package org.sagebionetworks.workflow.curation;

import static org.junit.Assert.assertEquals;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.workflow.activity.Curation;

/**
 * @author deflaux
 * 
 */
public class TcgaWorkflowTest {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoCreateExpressionMetadata() throws Exception {

		JSONObject layer = Curation
				.formulateLayerMetadataFromTcgaUrl("http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz");
		
		assertEquals("E", layer.getString("type"));
		assertEquals("unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0", layer.getString("name"));
		// TODO move commented out stuff to layer annotations
//		assertEquals("unc.edu", layer.getString("domain"));
//		assertEquals("COAD", layer.getString("diseaseStudy"));
		assertEquals("AgilentG4502A_07_3", layer.getString("platform"));
//		assertEquals("Level_2", layer.getString("level"));
//		assertEquals("2.0.0", layer.getString("revision"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoCreateClinicalMetadata() throws Exception {

		JSONObject layer = Curation
				.formulateLayerMetadataFromTcgaUrl("http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/bcr/minbiotab/clin/clinical_patient_public_coad.txt");
		
		assertEquals("C", layer.getString("type"));
		assertEquals("clinical_patient_public_coad", layer.getString("name"));
	}

}
