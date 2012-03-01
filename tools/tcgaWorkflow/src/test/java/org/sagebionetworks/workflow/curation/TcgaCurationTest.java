package org.sagebionetworks.workflow.curation;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * @author deflaux
 * 
 */
public class TcgaCurationTest {

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
		Map<String, String> metadata = TcgaCuration
				.formulateMetadataFromTcgaUrl("http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz");

		assertEquals("E", metadata.get("type"));
		assertEquals("unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0", metadata
				.get("name"));
		assertEquals("raw", metadata.get("status"));
		assertEquals("unc.edu", metadata.get("tcgaDomain"));
		assertEquals("COAD", metadata.get("tcgaDiseaseStudy"));
		assertEquals("AgilentG4502A_07_3", metadata.get("platform"));
		assertEquals("Level_2", metadata.get("tcgaLevel"));
		assertEquals("2", metadata.get("tcgaArchiveSerialIndex"));
		assertEquals("0", metadata.get("tcgaRevision"));
		assertEquals("0", metadata.get("tcgaSeries"));
		assertEquals("tsv", metadata.get("format"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoCreateClinicalMetadata() throws Exception {
		Map<String, String> metadata = TcgaCuration
				.formulateMetadataFromTcgaUrl("http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/bcr/minbiotab/clin/clinical_patient_public_coad.txt");

		assertEquals("C", metadata.get("type"));
		assertEquals("clinical_patient_public_coad", metadata.get("name"));
		assertEquals("raw", metadata.get("status"));
		assertEquals("tsv", metadata.get("format"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoCreateGeneticMetadata() throws Exception {
		String geneticDataUrl = "http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/broad.mit.edu/genome_wide_snp_6/snp/broad.mit.edu_COAD.Genome_Wide_SNP_6.mage-tab.1.1007.0.tar.gz";

		Map<String, String> metadata = TcgaCuration
				.formulateMetadataFromTcgaUrl(geneticDataUrl);

		assertEquals("G", metadata.get("type"));
		assertEquals("broad.mit.edu_COAD.Genome_Wide_SNP_6.mage-tab.1.1007.0",
				metadata.get("name"));
		assertEquals("raw", metadata.get("status"));
		assertEquals("tsv", metadata.get("format"));
		assertEquals("broad.mit.edu", metadata.get("tcgaDomain"));
		assertEquals("COAD", metadata.get("tcgaDiseaseStudy"));
		assertEquals("Genome_Wide_SNP_6", metadata.get("platform"));
		assertEquals("mage-tab", metadata.get("tcgaLevel"));
		assertEquals("1", metadata.get("tcgaArchiveSerialIndex"));
		assertEquals("1007", metadata.get("tcgaRevision"));
		assertEquals("0", metadata.get("tcgaSeries"));
	}

}
