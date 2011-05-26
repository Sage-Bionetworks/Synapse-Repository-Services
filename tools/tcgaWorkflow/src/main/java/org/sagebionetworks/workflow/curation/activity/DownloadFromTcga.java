package org.sagebionetworks.workflow.curation.activity;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.sagebionetworks.utils.MD5ChecksumHelper;
import org.sagebionetworks.workflow.UnrecoverableException;

/**
 * @author deflaux
 *
 */
public class DownloadFromTcga {

	/**
	 * @author deflaux
	 *
	 */
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

	/**
	 * @param tcgaUrl
	 * @return DownloadResult
	 * @throws IOException
	 * @throws UnrecoverableException
	 * @throws NoSuchAlgorithmException
	 * @throws HttpClientHelperException
	 */
	public static DownloadResult doDownloadFromTcga(String tcgaUrl)
			throws IOException, UnrecoverableException,
			NoSuchAlgorithmException, HttpClientHelperException {

		String filename;
		String remoteMd5 = null;
		try {
			String md5FileContents = HttpClientHelper.getFileContents(tcgaUrl
					+ ".md5");

			// TODO put a real regexp here to validate the format
			String fileInfo[] = md5FileContents.split("\\s+");
			if (2 != fileInfo.length) {
				throw new UnrecoverableException(
						"malformed md5 file from tcga: " + md5FileContents);
			}
			remoteMd5 = fileInfo[0];
			filename = fileInfo[1];
		} catch (HttpClientHelperException e) {
			if (404 == e.getHttpStatus()) {
				// 404s are okay, not all TCGA files have a corresponding md5
				// file (e.g., clinical data)
				String pathComponents[] = tcgaUrl.split("/");
				filename = pathComponents[pathComponents.length - 1];
			} else {
				throw e;
			}
		}

		File dataFile = new File("./" + filename);
		if (dataFile.exists() && dataFile.canRead() && dataFile.isFile()) {
			// Ensure that the local copy of the file we have cached is the
			// right one
			String localMd5 = MD5ChecksumHelper.getMD5Checksum(dataFile
					.getAbsolutePath());
			if ((null == remoteMd5) || (localMd5.equals(remoteMd5))) {
				return new DownloadResult(dataFile.getAbsolutePath(),
						localMd5);
			}
		}

		HttpClientHelper.downloadFile(tcgaUrl, dataFile.getAbsolutePath());
		String localMd5 = MD5ChecksumHelper.getMD5Checksum(dataFile
				.getAbsolutePath());
		if ((null != remoteMd5) || (localMd5.equals(remoteMd5))) {
			throw new UnrecoverableException(
					"md5 of downloaded file does not match that reported by TCGA");
		}

		return new DownloadResult(dataFile.getAbsolutePath(), localMd5);
	}

}
