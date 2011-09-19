package org.sagebionetworks.repopipeline;


import java.io.File;

import org.junit.Ignore;
import org.junit.Test;
import org.tmatesoft.svn.core.wc.SVNRevision;


public class ScriptDownloaderTest {
	
	@Ignore
	@Test
	public void testScriptDownload() throws Exception {
		String url = "https://sagebionetworks.jira.com/svn/PLFM/trunk/tools/bamboo/";
		String username = "bruce.hoff";
		String password = "xxxx";
		SVNRevision revision = SVNRevision.HEAD;
		File destPath = new File("\\Temp");
		
		ScriptDownloader.checkout(url, username, password, revision, destPath);
	}

}
