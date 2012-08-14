package org.sagebionetworks.sweeper;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.s3.AmazonS3;

/**
 * Unit test for simple Sweeper.
 */
public class SweeperTest {

	private static final String TEST_RESOURCES_DIR = "src/test/resources/logSweeper/";
	AmazonS3 mockS3;
	Sweeper sweeper;
	private EC2IdProvider mockIdProvider;

	@Before
	public void setUp() {
		mockS3 = mock(AmazonS3.class);
		mockIdProvider = mock(EC2IdProvider.class);
		this.sweeper = new Sweeper(mockS3, mockIdProvider, null, false);
	}

	@Test
	public void testSweepNullConfig() {
		List<File> files = new ArrayList<File>();

		sweeper.sweep(null, files);
		verify(mockS3, never()).putObject(anyString(), anyString(), (File) anyObject());
	}

	@Test
	public void testBaseDirNull() {
		SweepConfiguration config = new SweepConfiguration(TEST_RESOURCES_DIR, "no-logs.log", "", "");
		List<File> foundFiles = sweeper.findFiles(config);
		assertEquals(0, foundFiles.size());
	}

	@Test
	public void testFindFilesNull() {
		runFindFiles("log-not-here");
	}

	@Test
	public void testFindFiles() {
		runFindFiles("repo-trace-profile");
		runFindFiles("repo-slow-profile");
		runFindFiles("repo-activity");
	}

	private void runFindFiles(String logBaseName) {
		List<File> actualFiles = buildFileList(TEST_RESOURCES_DIR
				+ logBaseName, ".2012-08-11-", subRange("0", 0, 10), ".gz");

		String logExpression = logBaseName + "\\.\\d{4}-\\d{2}-\\d{2}-\\d{2}\\.gz";

		SweepConfiguration config = new SweepConfiguration(TEST_RESOURCES_DIR, logBaseName+".log", logExpression, "");
		List<File> files = sweeper.findFiles(config);

		Comparator<File> c = new Comparator<File>() {

			public int compare(File o1, File o2) {
				return o1.getName().compareTo(o2.getName());
			}

		};
		// We should have found the same number of files
		assertEquals(actualFiles.size(), files.size());

		Collections.sort(actualFiles, c);
		Collections.sort(files, c);

		for (int i = 0; i < actualFiles.size() && i < files.size(); i++) {
			File validation = actualFiles.get(i);
			File test = files.get(i);
			assertEquals(validation.getAbsolutePath(), test.getAbsolutePath());
		}
	}

	private Sweeper setupSweeper() {
		Set<SweepConfiguration> configList = new HashSet<SweepConfiguration>();
		SweepConfiguration configActivity = new SweepConfiguration(
				TEST_RESOURCES_DIR, "repo-activity.log", "repo-activity\\.\\d{4}-\\d{2}-\\d{2}-\\d{2}\\.gz", "");
		SweepConfiguration configSlow = new SweepConfiguration(
				TEST_RESOURCES_DIR, "repo-slow-profile.log", "repo-slow-profile\\.\\d{4}-\\d{2}-\\d{2}-\\d{2}\\.gz", "");
		SweepConfiguration configTrace = new SweepConfiguration(
				TEST_RESOURCES_DIR, "repo-trace-profile.log", "repo-trace-profile\\.\\d{4}-\\d{2}-\\d{2}-\\d{2}\\.gz", "");

		configList.add(configActivity);
		configList.add(configSlow);
		configList.add(configTrace);

		Sweeper localSweeper = new Sweeper(mockS3, mockIdProvider, configList, false);
		return localSweeper;
	}

	@Test
	public void testSweepActive() {
		Sweeper localSweeper = setupSweeper();

		localSweeper.sweepActiveLogFiles();
		verify(mockS3, times(3)).putObject(anyString(), anyString(), (File) anyObject());
	}

	@Test
	public void testSweepRolled() {
		Sweeper localSweeper = setupSweeper();

		localSweeper.sweepRolledLogFiles();
		verify(mockS3, times(27)).putObject(anyString(), anyString(), (File) anyObject());
	}

	private String[] subRange(String prefix, int start, int end) {
		String[] arr = new String[end - start];
		for (int i = start; i < end; i++) {
			arr[i] = String.format("%s%d", prefix, i);
		}
		return arr;
	}

	private List<File> buildFileList(String baseName, String datePrefix,
			String[] dateSubRange, String dateSuffix) {
		List<File> fileList = new ArrayList<File>();

		for (String subDate : dateSubRange) {
			File file = new File(String.format("%s%s%s%s", baseName, datePrefix, subDate, dateSuffix));
			if (file.exists()) {
				fileList.add(file);
			}
		}

		return fileList;
	}
}
