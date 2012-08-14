package org.sagebionetworks.sweeper;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SweepConfiguration {
	private class NameFilter implements FileFilter {

		final private Pattern pattern;

		private NameFilter(Pattern pattern) {
			this.pattern = pattern;
		}

		public boolean accept(File pathname) {
			if (pathname.isFile()) {
				Matcher m = pattern.matcher(pathname.getName());
				return m.matches();
			}
			return false;
		}

	}

	final private String logBaseDir;
	/**
	 * The unique portion of the S3 Bucket name that this log configuration
	 * should be swept to. Note that ".sagebionetworks.org" is appended to it
	 * automatically.
	 */
	final private String s3BucketName;

	/**
	 * The base filename for this log.  This should be the name of the file
	 * that is actively appended to (i.e. before it's rolled over).  This
	 * is only used in case of an unexpected shutdown, to roll the active
	 * logfiles over.
	 */
	final private String logBaseFile;

	/**
	 * The "singleton" instance of the NameFilter associated with this
	 * SweepConfig. It's filtering is based on the logExpression constructor
	 * parameter.
	 */
	final private SweepConfiguration.NameFilter filter;
	final private String pattern;

	/**
	 * Constructs a SweepConfiguration object that signifies one set of log
	 * files (that match logExpression) that are found under logBaseDir to be
	 * swept into the S3 bucket s3BucketName.
	 *
	 * @param logBaseDir
	 *            full pathname to the root log directory
	 * @param logExpression
	 *            a regular expression that matches all desired logs - see
	 *            {@link SweepConfiguration#filter}
	 * @param s3BucketName
	 *            see {@link SweepConfiguration#s3BucketName}
	 */
	public SweepConfiguration(String logBaseDir, String logBaseFile, String logExpression,
			String s3BucketName) {
		this.logBaseDir = logBaseDir;
		this.s3BucketName = s3BucketName + ".sagebase.org";
		this.logBaseFile = logBaseFile;
		this.pattern = logExpression;
		this.filter = new SweepConfiguration.NameFilter(
				Pattern.compile(logExpression));
	}

	public String getLogBaseDir() {
		return logBaseDir;
	}

	public String getS3BucketName() {
		return s3BucketName;
	}

	public String getLogBaseFile() {
		return logBaseFile;
	}

	public String getPattern() {
		return pattern;
	}

	public FileFilter getFilter() {
		return filter;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof SweepConfiguration))
			return false;

		SweepConfiguration sc = (SweepConfiguration)o;
		return this.logBaseDir.equals(sc.logBaseDir) &&
				this.s3BucketName.equals(sc.s3BucketName) &&
				this.pattern.equals(sc.pattern);
	}

	@Override
	public int hashCode() {
		int result = 42;
		result = 31 * result + this.logBaseDir.hashCode();
		result = 31 * result + this.s3BucketName.hashCode();
		result = 31 * result + this.pattern.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return String.format("[SweepConfiguration: s3Bucket=%s, logBaseDirectory=%s, pattern=%s]",
				s3BucketName, logBaseDir, pattern);
	}
}
