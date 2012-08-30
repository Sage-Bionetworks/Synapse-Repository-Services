package org.sagebionetworks.sweeper.log4j;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.rolling.RollingPolicyBase;
import org.apache.log4j.rolling.RolloverDescription;
import org.apache.log4j.rolling.RolloverDescriptionImpl;
import org.apache.log4j.rolling.SizeBasedTriggeringPolicy;
import org.apache.log4j.rolling.TimeBasedRollingPolicy;
import org.apache.log4j.rolling.TriggeringPolicy;
import org.apache.log4j.rolling.helper.Action;
import org.apache.log4j.rolling.helper.CompositeAction;
import org.apache.log4j.rolling.helper.FileRenameAction;
import org.apache.log4j.rolling.helper.GZCompressAction;
import org.apache.log4j.rolling.helper.ZipCompressAction;
import org.apache.log4j.spi.LoggingEvent;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * <code>TimeAndSizeBasedRollingToS3Policy</code> is a composite
 * Rolling/Triggering Policy that performs and integrates the work done by
 * {@link org.log4j.rolling.TimeBasedRollingPolicy}, and SizeBasedTriggeringPolicy. In addition, it also
 * sweeps all of the files it rolls into an S3 Bucket.
 *
 * <p>
 * The default max file size is 10MB. To change this setting, simply set the
 * <b>MaxFileSize</b> option in bytes.
 *
 * <p>
 * In order to use <code>TimeAndSizeBasedRollingToS3Policy</code>, the
 * <b>FileNamePattern</b> option must be set. It basically specifies the name of
 * the rolled log files. The value <code>FileNamePattern</code> should consist
 * of the name of the file, a suitably placed <code>%i</code> conversion
 * specifier, and a suitably placed <code>%d</code> conversion specifier. The
 * <code>%i</code> conversion specifier will be replaced by an integer, starting
 * at 0, and counting upwards. This will only occur if the date specifier has
 * not changed since the last file rollover. The <code>%d</code> conversion
 * specifier may contain a date and time pattern as specified by the
 * {@link java.text.SimpleDateFormat} class. If the date and time pattern is
 * ommitted, then the default pattern of "yyyy-MM-dd" is assumed. For examples,
 * please see TimeBasedRollingPolicy.
 *
 * <p>
 * Also, like TimeBasedRollingPolicy, you can specify an <code>ActiveFileName</code>
 * property in the configuration file.  See that file for more details.
 *
 * @author Geoff Shannon
 */
public final class TimeAndSizeBasedRollingToS3Policy extends RollingPolicyBase
		implements TriggeringPolicy {

	private TimeBasedRollingPolicy timePolicy = new TimeBasedRollingPolicy();

	private SizeBasedTriggeringPolicy sizePolicy = new SizeBasedTriggeringPolicy();

	private String s3BucketName = null;

	private String awsAccessKeyId = null;

	private String awsAccessSecretKey = null;

	private boolean sweeping = false;

	private boolean deleteAfterSweeping = false;

	private StackConfigAccess stackConfigAccess = null;

	public void setMaxFileSize(long l) {
		sizePolicy.setMaxFileSize(l);
	}

	public long getMaxFileSize() {
		return sizePolicy.getMaxFileSize();
	}

	/**
	 * Set file name pattern.
	 *
	 * @param fnp file name pattern.
	 */
	@Override
	public void setFileNamePattern(String fnp) {
		timePolicy.setFileNamePattern(fnp);
	}

	/**
	 * Get file name pattern.
	 *
	 * @return file name pattern.
	 */
	@Override
	public String getFileNamePattern() {
		return timePolicy.getFileNamePattern();
	}

	/**
	 * ActiveFileName can be left unset, i.e. as null.
	 *
	 * @param afn active file name.
	 */
	@Override
	public void setActiveFileName(String afn) {
		activeFileName = afn;
		timePolicy.setActiveFileName(afn);
	}

	/**
	 * Return the value of the <b>ActiveFile</b> option.
	 *
	 * @return active file name.
	 */
	@Override
	public String getActiveFileName() {
		return activeFileName;
	}

	public TimeAndSizeBasedRollingToS3Policy() {
		this.stackConfigAccess = new StackConfigAccessImpl();
	}

	public TimeAndSizeBasedRollingToS3Policy(StackConfigAccess stackConfigAccess) {
		this.stackConfigAccess = stackConfigAccess;
	}

	@Override
	public void activateOptions() {
		timePolicy.activateOptions();
		sizePolicy.activateOptions();
		getS3Configuration();
	}

	@Override
	public RolloverDescription initialize(String file, boolean append)
			throws SecurityException {
		return timePolicy.initialize(file, append);
	}

	@Override
	public RolloverDescription rollover(String activeFile)
			throws SecurityException {
		RolloverDescription rollover = timePolicy.rollover(activeFile);

		String targetFileName = discoverTargetFileName(rollover, activeFile);

		return addSweepAction(rollover, targetFileName);
	}

	@Override
	public boolean isTriggeringEvent(Appender appender, LoggingEvent event,
			String filename, long fileLength) {
		return timePolicy.isTriggeringEvent(appender, event, filename, fileLength)
				|| sizePolicy.isTriggeringEvent(appender, event, filename, fileLength);
	}

	private String discoverTargetFileName(RolloverDescription rollover,
			String activeFile) {
		Action synchronous = rollover.getSynchronous();
		Action asynchronous = rollover.getAsynchronous();

		// Neither rename action nor compress is happening, so the file to sweep
		// is the currentActiveFile
		if (synchronous == null &&
				asynchronous == null) {
			return activeFile;

		// Rename action only, extract filename from synchronous action
		} else if (synchronous != null  &&
				asynchronous == null) {
			return extractFileName(synchronous);
		// No rename, but yes compress, extract filename from compress action
		} else if (synchronous == null &&
				asynchronous != null) {
			return extractFileName(asynchronous);
		// Both actions, but compress happens last, so extract from compress action
		} else if (synchronous != null &&
				asynchronous != null) {
			return extractFileName(asynchronous);
		} else {
			throw new IllegalStateException("Arrived at an impossible conditional branch");
		}
	}

	private String extractFileName(Action action) {
		Class<?> clazz;

		if (action instanceof GZCompressAction)
			clazz = GZCompressAction.class;
		else if (action instanceof ZipCompressAction)
			clazz = ZipCompressAction.class;
		else if (action instanceof FileRenameAction)
			clazz = FileRenameAction.class;
		else
			return null;

		return getDestinationField(action, clazz);
	}

	private String getDestinationField(Action compressAction,
			Class<?> clazz) {
		Field field;
		try {
			field = clazz.getDeclaredField("destination");
			field.setAccessible(true);
			Object object = field.get(compressAction);
			if (object instanceof String) {
				return (String) object;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private RolloverDescription addSweepAction(RolloverDescription rollover, String targetFileName) {
		Action compressAction = rollover.getAsynchronous();
		Action compressAndSweep = makeCompressAndSweepAction(compressAction, targetFileName);

		if (compressAction != compressAndSweep) {
			rollover = new RolloverDescriptionImpl(rollover.getActiveFileName(),
					rollover.getAppend(), rollover.getSynchronous(), compressAndSweep);
		}

		return rollover;
	}

	private Action makeCompressAndSweepAction(Action compressAction,
			String targetFileName) {
		if (!sweeping)
			return compressAction;

		AmazonS3 s3Client = new AmazonS3Client(new BasicAWSCredentials(awsAccessKeyId, awsAccessSecretKey));
		SweepAction sweepAction = new SweepAction(new File(targetFileName), s3BucketName, s3Client, deleteAfterSweeping);

		if (compressAction == null)
			return sweepAction;

		List<Action> actions = new ArrayList<Action>();
		actions.add(compressAction);
		actions.add(sweepAction);

		return new CompositeAction(actions, true);
	}

	private void getS3Configuration() {
		this.awsAccessKeyId = stackConfigAccess.getIAMUserId();
		this.awsAccessSecretKey = stackConfigAccess.getIAMUserKey();
		this.sweeping = stackConfigAccess.getLogSweepingEnabled();
		this.s3BucketName = stackConfigAccess.getS3LogBucket();
	}
}
