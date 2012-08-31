package org.sagebionetworks.sweeper.log4j;

import java.io.File;
import java.util.Arrays;
import java.util.Date;

import org.apache.log4j.Appender;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.pattern.PatternConverter;
import org.apache.log4j.rolling.RollingPolicyBase;
import org.apache.log4j.rolling.RolloverDescription;
import org.apache.log4j.rolling.RolloverDescriptionImpl;
import org.apache.log4j.rolling.TriggeringPolicy;
import org.apache.log4j.rolling.helper.Action;
import org.apache.log4j.rolling.helper.CompositeAction;
import org.apache.log4j.rolling.helper.FileRenameAction;
import org.apache.log4j.rolling.helper.GZCompressAction;
import org.apache.log4j.spi.LoggingEvent;

/**
 * <code>TimeBasedRollingToS3Policy</code> is a composite
 * Rolling/Triggering Policy that performs similar work to
 * {@link org.log4j.rolling.TimeBasedRollingPolicy}. In addition,
 * it also compresses the files and sweeps all of them into an S3 Bucket.
 *
 * <p>
 * You MUST specify an <code>ActiveFileName</code> property in the
 * configuration file.  See the TimeBasedRollingPolicy for more details.
 *
 * <p>
 * In order to use <code>TimeBasedRollingToS3Policy</code>, the
 * <b>FileNamePattern</b> option must be set. It basically specifies the name of
 * the rolled log files. The value <code>FileNamePattern</code> should consist
 * of the name of the file with a suitably placed <code>%d</code> conversion specifier.
 * The <code>%d</code> conversion specifier may contain a date and time pattern
 * as specified by the {@link java.text.SimpleDateFormat} class. If the date and
 * time pattern is omitted, then the default pattern of "yyyy-MM-dd" is assumed.
 * For examples, please see TimeBasedRollingPolicy.
 *
 * @author Geoff Shannon
 */
public final class TimeBasedRollingToS3Policy extends RollingPolicyBase
		implements TriggeringPolicy {

	private final String AFN_NOT_SET = "The ActiveFileName option must be set before using "+this.getClass().getSimpleName();;

	private StackConfigAccess stackConfigAccess;

	private AmazonS3Provider s3Provider;

	private String instanceId;

	private String s3BucketName = null;

	private String awsAccessKeyId = null;

	private String awsAccessSecretKey = null;

	private boolean sweeping = false;

	private boolean deleteAfterSweeping = false;

	/**
	 * Time for next determination if time for rollover.
	 */
	private long nextCheck = 0;

	private String lastFileName;


	public TimeBasedRollingToS3Policy() {
		this.stackConfigAccess = new StackConfigAccessImpl();
		this.s3Provider = new AmazonS3ProviderImpl();
	}

	public TimeBasedRollingToS3Policy(
				AmazonS3Provider s3Provider,
				StackConfigAccess stackConfigAccess) {
		this.s3Provider = s3Provider;
		this.stackConfigAccess = stackConfigAccess;
	}

	@Override
	public void activateOptions() {
		super.activateOptions();

		if (activeFileName == null) {
			LogLog.warn(AFN_NOT_SET);
			throw new IllegalStateException(AFN_NOT_SET);
		}
		PatternConverter dtc = getDatePatternConverter();

		if (dtc == null) {
			throw new IllegalStateException("FileNamePattern ["
					+ getFileNamePattern()
					+ "] does not contain a valid date format specifier");
		}

		long n = System.currentTimeMillis();
		StringBuffer buf = new StringBuffer();
		formatFileName(new Date(n), buf);
		lastFileName = buf.toString();

		getS3Configuration();
	}

	@Override
	public RolloverDescription initialize(String currentActiveFile, boolean append)
			throws SecurityException {
		long n = System.currentTimeMillis();
		nextCheck = ((n / 1000) + 1) * 1000;

		StringBuffer buf = new StringBuffer();
		formatFileName(new Date(n), buf);
		lastFileName = buf.toString();

		if (activeFileName != null) {
			return new RolloverDescriptionImpl(activeFileName, append, null,
					null);
		} else if (currentActiveFile != null) {
			return new RolloverDescriptionImpl(currentActiveFile, append, null,
					null);
		} else {
			return new RolloverDescriptionImpl(lastFileName, append, null, null);
		}
	}

	@Override
	public RolloverDescription rollover(String currentActiveFile)
			throws SecurityException {
		long n = System.currentTimeMillis();
		nextCheck = ((n / 1000) + 1) * 1000;

		StringBuffer buf = new StringBuffer();
		formatFileName(new Date(n), buf);

		String newFileName = buf.toString();

		//
		// if file names haven't changed, no rollover
		//
		if (newFileName.equals(lastFileName)) {
			return null;
		}

		Action renameAction = null;
		Action compressAction = null;
		Action asyncAction = null;


		renameAction = new FileRenameAction(new File(currentActiveFile),
											 new File(lastFileName), true);

		compressAction = new GZCompressAction(new File(lastFileName),
											   new File(lastFileName), true);

		if (sweeping) {
			Action sweepAction = new SweepAction(new File(lastFileName),
												  s3BucketName,
												  s3Provider.getS3Client(awsAccessKeyId,
														  				 awsAccessSecretKey),
												  this.deleteAfterSweeping);
			asyncAction = new CompositeAction(Arrays.asList(new Action[]{compressAction, sweepAction}), true);
		} else {
			asyncAction = compressAction;
		}

		lastFileName = newFileName;

		// currentActiveFile doesn't change because it MUST be decoupled from the rolled files
		return new RolloverDescriptionImpl(currentActiveFile, false, renameAction,
				asyncAction);
	}

	@Override
	public boolean isTriggeringEvent(Appender appender, LoggingEvent event,
			String filename, long fileLength) {
				return System.currentTimeMillis() >= nextCheck;
	}

	private void getS3Configuration() {
		this.awsAccessKeyId = stackConfigAccess.getIAMUserId();
		this.awsAccessSecretKey = stackConfigAccess.getIAMUserKey();
		this.s3BucketName = stackConfigAccess.getS3LogBucket();

		this.deleteAfterSweeping = stackConfigAccess.getDeleteAfterSweepingEnabled();
		this.sweeping = stackConfigAccess.getLogSweepingEnabled();
	}
}
