package org.sagebionetworks.utils;


import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * This is an integration test and will not be run by the surefire plugin because of the 'IT' in the filename.
 * 
 * @author deflaux
 * 
 */
public class WebCrawlerITCase {

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoTcgaCrawl() throws Exception {

		class CrawlObserver implements SimpleObserver<String> {
			int numUrlsFound = 0;
			int numArchivesFound = 0;
			Boolean foundExpression = false;
			Boolean foundClinical = false;

			@Override
			public void update(String url) {
				numUrlsFound++;
				if (url.endsWith("tar.gz")) {
					numArchivesFound++;
				}
				if (url.endsWith("/clinical_public_coad.tar.gz")) {
					// Make sure we are not returning the same terminal url more
					// than once
					assertTrue(!foundClinical);
					foundClinical = true;
				}
				if (url
						.endsWith("/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz")) {
					// Make sure we are not returning the same terminal url more
					// than once
					assertTrue(!foundExpression);
					foundExpression = true;
				}

			}

		}

		CrawlObserver testObserver = new CrawlObserver();
		WebCrawler crawler = new WebCrawler();
		crawler.addObserver(testObserver);
		crawler
				.doCrawl(
						"http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/",
						true);
		// The amount of data for this dataset should only grow
		assertTrue(3719 <= testObserver.numUrlsFound);
		assertTrue(64 <= testObserver.numArchivesFound);
		assertTrue(testObserver.foundClinical);
		assertTrue(testObserver.foundExpression);
	}

}
