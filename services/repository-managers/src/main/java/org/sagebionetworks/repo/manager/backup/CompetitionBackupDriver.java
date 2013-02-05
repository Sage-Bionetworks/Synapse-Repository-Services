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
import org.sagebionetworks.competition.dao.CompetitionDAO;
import org.sagebionetworks.competition.dao.ParticipantDAO;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.repo.model.CompetitionBackup;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class CompetitionBackupDriver implements GenericBackupDriver {

	@Autowired
	private CompetitionDAO competitionDAO;
	@Autowired
	private ParticipantDAO participantDAO;

	public CompetitionBackupDriver() { }

	// for testing
	public CompetitionBackupDriver(CompetitionDAO competitionDAO, ParticipantDAO participantDAO) {
		this.competitionDAO = competitionDAO;
		this.participantDAO = participantDAO;
	}

	static private Log log = LogFactory.getLog(CompetitionBackupDriver.class);

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
			progress.appendLog("Processing Competitions:");
			for (String idToBackup : competitionIdsToBackup) {
				Thread.yield();
				progress.appendLog(idToBackup);
				progress.setMessage(idToBackup);
				
				CompetitionBackup compBackup = new CompetitionBackup();
				compBackup.setCompetition(competitionDAO.get(idToBackup));
				compBackup.setParticipants(participantDAO.getAllByCompetition(idToBackup, Long.MAX_VALUE, 0L));
				ZipEntry entry = new ZipEntry(idToBackup + ZIP_ENTRY_SUFFIX);
				zos.putNextEntry(entry);
				NodeSerializerUtil.writeCompetitionBackup(compBackup, zos);
				progress.incrementProgress();
				if (progress.shouldTerminate()) {
					throw new InterruptedException(
							"Competitions Backup terminated by the user.");
				}
			}
			zos.close();
			progress.appendLog("Finished processing Competitions.");
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
			progress.appendLog("Processing Competitions:");
			while ((entry = zin.getNextEntry()) != null) {
				progress.setMessage(entry.getName());
				// Check for termination.
				if (progress.shouldTerminate()) {
					throw new InterruptedException(
							"Competitions restoration terminated by the user.");
				}

				// This is a backup file.
				CompetitionBackup backup = NodeSerializerUtil.readCompetitionBackup(zin);
				createOrUpdate(backup);

				// Append this id to the log.
				progress.appendLog(backup.getCompetition().getId().toString());

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
		competitionDAO.delete(id);
	}

	// returns true if a create/update was performed; false if NOOP
	private void createOrUpdate(CompetitionBackup backup)
			throws DatastoreException, NotFoundException,
			InvalidModelException, ConflictingUpdateException {
		
		Competition competition = backup.getCompetition();
		List<Participant> participants = backup.getParticipants();
		
		// create the Competition
		Competition existingComp = null;
		try {
			existingComp = competitionDAO.get(competition.getId().toString());
		} catch (NotFoundException e) {}
		if (null == existingComp) {
			// create
			competitionDAO.create(competition, Long.parseLong(competition.getOwnerId()));
		} else {
			// update only when backup is different from the current system
			if (existingComp.getEtag().equals(competition.getEtag())) {
				return;
			}
			competitionDAO.updateFromBackup(competition);
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
