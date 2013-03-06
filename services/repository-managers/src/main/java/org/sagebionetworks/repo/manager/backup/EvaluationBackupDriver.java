package org.sagebionetworks.repo.manager.backup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.ParticipantDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.repo.model.EvaluationBackup;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

@Deprecated // This needs to be replaced with GenericBackupDriverImpl and should not be copied.
public class EvaluationBackupDriver implements GenericBackupDriver {

	@Autowired
	private EvaluationDAO evaluationDAO;
	@Autowired
	private ParticipantDAO participantDAO;

	public EvaluationBackupDriver() { }

	// for testing
	public EvaluationBackupDriver(EvaluationDAO evaluationDAO, ParticipantDAO participantDAO) {
		this.evaluationDAO = evaluationDAO;
		this.participantDAO = participantDAO;
	}

	static private Log log = LogFactory.getLog(EvaluationBackupDriver.class);

	private static final String ZIP_ENTRY_SUFFIX = ".xml";

	@Override
	public boolean writeBackup(File destination, Progress progress,
			Set<String> competitionIdsToBackup) throws IOException,
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
		progress.setTotalCount(competitionIdsToBackup.size());

		// write to the file
		FileOutputStream fos = new FileOutputStream(destination);
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
		try {
			progress.appendLog("Processing Evaluations:");
			for (String idToBackup : competitionIdsToBackup) {
				Thread.yield();
				progress.appendLog(idToBackup);
				progress.setMessage(idToBackup);
				
				EvaluationBackup compBackup = new EvaluationBackup();
				compBackup.setEvaluation(evaluationDAO.get(idToBackup));
				compBackup.setParticipants(participantDAO.getAllByEvaluation(idToBackup, Long.MAX_VALUE, 0L));
				ZipEntry entry = new ZipEntry(idToBackup + ZIP_ENTRY_SUFFIX);
				zos.putNextEntry(entry);
				NodeSerializerUtil.writeCompetitionBackup(compBackup, zos);
				progress.incrementProgress();
				if (progress.shouldTerminate()) {
					throw new InterruptedException(
							"Evaluations Backup terminated by the user.");
				}
			}
			zos.close();
			progress.appendLog("Finished processing Evaluations.");
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
			progress.appendLog("Processing Evaluations:");
			while ((entry = zin.getNextEntry()) != null) {
				progress.setMessage(entry.getName());
				// Check for termination.
				if (progress.shouldTerminate()) {
					throw new InterruptedException(
							"Evaluations restoration terminated by the user.");
				}

				// This is a backup file.
				EvaluationBackup backup = NodeSerializerUtil.readCompetitionBackup(zin);
				createOrUpdate(backup);

				// Append this id to the log.
				progress.appendLog(backup.getEvaluation().getId().toString());

				progress.incrementProgressBy(entry.getCompressedSize());
				if (log.isTraceEnabled()) {
					log.trace(progress.toString());
				}
				// This is run in a tight loop so to be CPU friendly we should
				// yield
				Thread.yield();
			}
			progress.appendLog("Finished processing Evaluations.");
		} finally {
			if (fis != null) {
				fis.close();
			}
		}
		return true;
	}

	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		evaluationDAO.delete(id);
	}

	// returns true if a create/update was performed; false if NOOP
	private void createOrUpdate(EvaluationBackup backup)
			throws DatastoreException, NotFoundException,
			InvalidModelException, ConflictingUpdateException {
		
		Evaluation evaluation = backup.getEvaluation();
		List<Participant> participants = backup.getParticipants();
		
		// create the Competition
		Evaluation existingComp = null;
		try {
			existingComp = evaluationDAO.get(evaluation.getId().toString());
		} catch (NotFoundException e) {}
		if (null == existingComp) {
			// create
			evaluationDAO.createFromBackup(evaluation, Long.parseLong(evaluation.getOwnerId()));
		} else {
			// update only when backup is different from the current system
			if (existingComp.getEtag().equals(evaluation.getEtag())) {
				return;
			}
			evaluationDAO.updateFromBackup(evaluation);
		}
		
		// create the Participants
		for (Participant p : participants) {
			try {
				participantDAO.create(p);
			} catch (DatastoreException e) {
				
			}
		}
	}
}
