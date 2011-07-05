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
		JSONObject annotations = new JSONObject();
		JSONObject layer = Curation
				.formulateLayerMetadataFromTcgaUrl("http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz",
						annotations);

		assertEquals("E", layer.getString("type"));
		assertEquals("unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0", layer
				.getString("name"));
		assertEquals("unc.edu", annotations.getString("tcgaDomain"));
		assertEquals("COAD", annotations.getString("tcgaDiseaseStudy"));
		assertEquals("AgilentG4502A_07_3", layer.getString("platform"));
		assertEquals("Level_2", annotations.getString("format"));
		assertEquals("2.0.0", annotations.getString("tcgaRevision"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoCreateClinicalMetadata() throws Exception {
		JSONObject annotations = new JSONObject();
		JSONObject layer = Curation
				.formulateLayerMetadataFromTcgaUrl("http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/bcr/minbiotab/clin/clinical_patient_public_coad.txt",
						annotations);

		assertEquals("C", layer.getString("type"));
		assertEquals("clinical_patient_public_coad", layer.getString("name"));
		assertEquals("tab-delimited", annotations.getString("format"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoCreateGeneticMetadata() throws Exception {
		String geneticDataUrl = "http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/broad.mit.edu/genome_wide_snp_6/snp/broad.mit.edu_COAD.Genome_Wide_SNP_6.mage-tab.1.1007.0.tar.gz";

		JSONObject annotations = new JSONObject();
		JSONObject layer = Curation
				.formulateLayerMetadataFromTcgaUrl(geneticDataUrl, annotations);
		
		assertEquals("G", layer.getString("type"));
		assertEquals("broad.mit.edu_COAD.Genome_Wide_SNP_6.mage-tab.1.1007.0", layer
				.getString("name"));
		assertEquals("broad.mit.edu", annotations.getString("tcgaDomain"));
		assertEquals("COAD", annotations.getString("tcgaDiseaseStudy"));
		assertEquals("Genome_Wide_SNP_6", layer.getString("platform"));
		assertEquals("mage-tab", annotations.getString("format"));
		assertEquals("1.1007.0", annotations.getString("tcgaRevision"));
	}

}
