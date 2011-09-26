package org.sagebionetworks.workflow.curation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.utils.WebCrawler;
import org.sagebionetworks.utils.SimpleObserver;
import org.sagebionetworks.workflow.Constants;
import org.sagebionetworks.workflow.UnrecoverableException;
import org.sagebionetworks.workflow.activity.Curation;
import org.sagebionetworks.workflow.activity.DataIngestion;
import org.sagebionetworks.workflow.activity.Notification;
import org.sagebionetworks.workflow.activity.Processing;
import org.sagebionetworks.workflow.activity.Storage;
import org.sagebionetworks.workflow.activity.DataIngestion.DownloadResult;
import org.sagebionetworks.workflow.activity.Processing.ScriptResult;
import org.sagebionetworks.workflow.curation.TcgaWorkflowInitiator.ArchiveObserver;

import com.amazonaws.AmazonServiceException;

/**
 * Note that this integration test should pass when the system is clean (no
 * files downloaded, no metadata created) and also when the tests have already
 * been run once. All these activities are supposed to be idempotent and it is
 * an error if they are not.
 * 
 * @author deflaux
 * 
 */
public class TcgaWorkflowCrawlerITCase {

	private static final Logger log = Logger.getLogger(TcgaWorkflowCrawlerITCase.class
			.getName());

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

	/**
	 * Test crawler with version collapse logic
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDoVersionConsolidationTcgaCrawl() throws Exception {
		WebCrawler archiveCrawler = new WebCrawler();
		ArchiveObserver observer = new TcgaWorkflowInitiator().new ArchiveObserver();
		archiveCrawler.addObserver(observer);
		archiveCrawler
				.doCrawl(
						"http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/",
						true);
		Collection<String> urls = observer.getResults();
		for (String dataLayerUrl : urls) {
			log.warn(dataLayerUrl);
		}
		assertTrue(267 <= urls.size());
	}

}
