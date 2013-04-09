package org.sagebionetworks.repo.manager.backup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.dao.SubmissionStatusDAO;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.SubmissionBackup;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;

@Deprecated // This needs to be replaced with GenericBackupDriverImpl and should not be copied.
public class SubmissionBackupDriver implements GenericBackupDriver {

	@Autowired
	private SubmissionDAO submissionDAO;
	@Autowired
	private SubmissionStatusDAO submissionStatusDAO;

	public SubmissionBackupDriver() { }

	// for testing
	public SubmissionBackupDriver(SubmissionDAO submissionDAO, SubmissionStatusDAO submissionStatusDAO) {
		this.submissionDAO = submissionDAO;
		this.submissionStatusDAO = submissionStatusDAO;
	}

	static private Log log = LogFactory.getLog(SubmissionBackupDriver.class);

	private static final String ZIP_ENTRY_SUFFIX = ".xml";

	@Override
	public boolean writeBackup(File destination, Progress progress,
			Set<String> submissionIdsToBackup) throws IOException,
			DatastoreException, NotFoundException, InterruptedException {
		if (destination == null)
			throw new IllegalArgumentException(
					"Destination file cannot be null");
		if (!destination.exists())
			throw new IllegalArgumentException(
					"Destination file does not exist: "
							+ destination.getAbsolutePath());

		log.info("Starting a backup to file: " + destination.getAbsolutePath());
		progress.appendLog("Starting a backup to file: "
				+ destination.getAbsolutePath());
		progress.setTotalCount(submissionIdsToBackup.size());

		// write to the file
		FileOutputStream fos = new FileOutputStream(destination);
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
		try {
			progress.appendLog("Processing Submissions:");
			for (String idToBackup : submissionIdsToBackup) {
				Thread.yield();
				progress.appendLog(idToBackup);
				progress.setMessage(idToBackup);

				SubmissionBackup subBackup = new SubmissionBackup();
				subBackup.setSubmission(submissionDAO.get(idToBackup));
				subBackup.setSubmissionStatus(submissionStatusDAO.get(idToBackup));
				ZipEntry entry = new ZipEntry(idToBackup + ZIP_ENTRY_SUFFIX);
				zos.putNextEntry(entry);
				NodeSerializerUtil.writeSubmissionBackup(subBackup, zos);
				progress.incrementProgress();
				if (progress.shouldTerminate()) {
					throw new InterruptedException(
							"Submissions backup terminated by the user.");
				}
			}
			zos.close();
			progress.appendLog("Finished processing Submissions.");
		} finally {
			if (fos != null) {
				fos.flush();
				fos.close();
			}
		}
		return true;
	}

	@Override
	public boolean restoreFromBackup(File source, Progress progress)
			throws IOException, InterruptedException, DatastoreException,
			NotFoundException, InvalidModelException,
			ConflictingUpdateException {
		if (source == null)
			throw new IllegalArgumentException("Source file cannot be null");
		if (!source.exists())
			throw new IllegalArgumentException("Source file dose not exist: "
					+ source.getAbsolutePath());
		if (progress == null)
			throw new IllegalArgumentException("Progress cannot be null");
		FileInputStream fis = new FileInputStream(source);

		try {
			log.info("Restoring: " + source.getAbsolutePath());
			progress.appendLog("Restoring: " + source.getAbsolutePath());
			// First clear all data
			ZipInputStream zin = new ZipInputStream(
					new BufferedInputStream(fis));
			progress.setMessage("Reading: " + source.getAbsolutePath());
			progress.setTotalCount(source.length());

			ZipEntry entry;
			progress.appendLog("Processing Submissions:");
			while ((entry = zin.getNextEntry()) != null) {
				progress.setMessage(entry.getName());
				// Check for termination.
				if (progress.shouldTerminate()) {
					throw new InterruptedException(
							"Submissions restoration terminated by the user.");
				}

				// This is a backup file.
				SubmissionBackup backup = NodeSerializerUtil.readSubmissionBackup(zin);
				createOrUpdate(backup);

				// Append this id to the log.
				progress.appendLog(backup.getSubmission().getId().toString());

				progress.incrementProgressBy(entry.getCompressedSize());
				if (log.isTraceEnabled()) {
					log.trace(progress.toString());
				}
				// This is run in a tight loop so to be CPU friendly we should
				// yield
				Thread.yield();
			}
			progress.appendLog("Finished processing Competitions.");
		} finally {
			if (fis != null) {
				fis.close();
			}
		}
		return true;
	}

	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		submissionDAO.delete(id);
	}

	private void createOrUpdate(SubmissionBackup backup)
			throws DatastoreException, NotFoundException,
			InvalidModelException, ConflictingUpdateException {
		Submission submission = backup.getSubmission();
		SubmissionStatus submissionStatus = backup.getSubmissionStatus();
		
		// create the Submission
		SubmissionStatus existing = null;
		try {
			existing = submissionStatusDAO.get(submissionStatus.getId().toString());
		} catch (NotFoundException e) {}
		if (null == existing) {
			// create
			try {
				submissionDAO.create(submission, null);
			} catch (JSONObjectAdapterException e) {
				// this will never happen, since we're not actually serializing
				// an EntityBundle
				e.printStackTrace();
			}
			submissionStatusDAO.createFromBackup(submissionStatus);
		} else {
			// Update only when backup is different from the current system
			if (!submissionStatus.getEtag().equals(existing.getEtag())) {
				submissionStatusDAO.updateFromBackup(submissionStatus);
			}
		}
	}

}
