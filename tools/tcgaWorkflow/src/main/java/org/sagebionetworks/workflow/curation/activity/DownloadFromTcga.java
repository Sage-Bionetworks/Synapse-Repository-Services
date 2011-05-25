package org.sagebionetworks.workflow.curation.activity;

import java.io.File;
import java.io.IOException;

import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.workflow.UnrecoverableException;

public class DownloadFromTcga {

	public static class DownloadResult {

		private String localFilepath;
		private String md5;

		/**
		 * @param localFilepath
		 * @param md5
		 */
		public DownloadResult(String localFilepath, String md5) {
			super();
			this.localFilepath = localFilepath;
			this.md5 = md5;
		}

		/**
		 * @return the localFilepath
		 */
		public String getLocalFilepath() {
			return localFilepath;
		}

		/**
		 * @param localFilepath
		 *            the localFilepath to set
		 */
		public void setLocalFilepath(String localFilepath) {
			this.localFilepath = localFilepath;
		}

		/**
		 * @return the md5
		 */
		public String getMd5() {
			return md5;
		}

		/**
		 * @param md5
		 *            the md5 to set
		 */
		public void setMd5(String md5) {
			this.md5 = md5;
		}

	}

	public static DownloadResult doDownloadFromTcga(String tcgaUrl) throws IOException, UnrecoverableException {
		
		String md5FileContents = HttpClientHelper.getFileContents(tcgaUrl
				+ ".md5");
		if (null == md5FileContents) {
			throw new UnrecoverableException("unable to download " + tcgaUrl + ".md5");
		}

		// TODO put a real regexp here to validate the format
		String md5Info[] = md5FileContents.split("\\s+");
		if (2 != md5Info.length) {
			throw new UnrecoverableException("malformed md5 file from tcga: "
					+ md5FileContents);
		}

		File dataFile = new File("./" + md5Info[1]);
		if (dataFile.exists() && dataFile.canRead() && dataFile.isFile()) {
			// TODO check md5 still matches
		} else {
			HttpClientHelper.downloadFile(tcgaUrl, dataFile.getAbsolutePath());
			// TODO check for errors
		}

		return new DownloadResult(dataFile.getAbsolutePath(), md5Info[0]);
	}

}
