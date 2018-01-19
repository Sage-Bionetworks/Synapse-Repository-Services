package org.sagebionetworks.repo.manager.migration;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigrationTypeProvider;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;

public class BackupFileStreamImpl implements BackupFileStream {

	private static final String UTF_8 = "UTF-8";
	private static final String INPUT_CONTAINED_NO_DATA = "input contained no data";
	private static final String DOT = ".";
	private static final String FILE_NAME_TEMPLATE = "%1$s.%2$d.xml";

	@Autowired
	MigrationTypeProvider typeProvider;

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.migration.BackupFileStream#readBackupFile(java.io.InputStream, org.sagebionetworks.repo.model.daemon.BackupAliasType)
	 */
	@Override
	public Iterable<MigratableDatabaseObject<?,?>> readBackupFile(InputStream input, BackupAliasType backupAliasType) {
		ValidateArgument.required(input, "input");
		try {
			return new InputStreamIterator(input, backupAliasType);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * This Iterator will read one file at a time from the provided zip file. Note:
	 * All data for any single file must fit in memory.
	 *
	 */
	private class InputStreamIterator
			implements Iterable<MigratableDatabaseObject<?, ?>>, Iterator<MigratableDatabaseObject<?,?>> {

		BackupAliasType backupAliasType;
		ZipInputStream zipInputStream;
		Iterator<MigratableDatabaseObject<?,?>> currentFile;

		InputStreamIterator(InputStream input, BackupAliasType backupAliasType) throws IOException {
			this.backupAliasType = backupAliasType;
			this.zipInputStream = new ZipInputStream(new BufferedInputStream(input));
			this.currentFile = null;

		}

		@Override
		public boolean hasNext() {
			// If the current file exists then attempt to read from it.
			if (currentFile != null) {
				if (currentFile.hasNext()) {
					return true;
				}
			}
			// read the next file from the zip.
			this.currentFile = readBatchFromZip(this.zipInputStream, this.backupAliasType).iterator();
			// The current file will be empty at the end of the stream.
			return this.currentFile.hasNext();
		}

		@Override
		public MigratableDatabaseObject<?,?> next() {
			if (currentFile == null) {
				throw new IllegalStateException("hasNext() must be called before next()");
			}
			return currentFile.next();
		}

		@Override
		public Iterator<MigratableDatabaseObject<?, ?>> iterator() {
			return this;
		}
	}

	/**
	 * Extract the migration type from a file's name.
	 * 
	 * @param name
	 * @return
	 */
	public static MigrationType getTypeFromFileName(String name) {
		ValidateArgument.required(name, "Name");
		int index = name.indexOf(DOT);
		if (index < 0) {
			throw new IllegalArgumentException("Unexpected file name: " + name);
		}
		return MigrationType.valueOf(name.substring(0, index));
	}

	/**
	 * Create a FileName for zip entry.
	 * 
	 * @param type
	 * @param index
	 * @return
	 */
	public static String createFileName(MigrationType type, int index) {
		ValidateArgument.required(type, "MigrationType");
		return String.format(FILE_NAME_TEMPLATE, type.name(), index);
	}

	/**
	 * Get the alias used to read the given MigratableDatabaseObject from an XML
	 * file.
	 * 
	 * @param mdo
	 * @param backupAliasType
	 * @return
	 */
	public static String getAlias(MigratableDatabaseObject<?, ?> mdo, BackupAliasType backupAliasType) {
		ValidateArgument.required(mdo, "MigratableDatabaseObject");
		ValidateArgument.required(backupAliasType, "BackupAliasType");
		if (backupAliasType == BackupAliasType.TABLE_NAME) {
			return mdo.getTableMapping().getTableName();
		} else if (backupAliasType == BackupAliasType.MIGRATION_TYPE_NAME) {
			return mdo.getMigratableTableType().name();
		} else {
			throw new IllegalStateException("Unknown type: " + backupAliasType);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.migration.BackupFileStream#writeBackupFile(java.io.OutputStream, java.lang.Iterable, org.sagebionetworks.repo.model.daemon.BackupAliasType, int)
	 */
	@Override
	public void writeBackupFile(OutputStream out, Iterable<MigratableDatabaseObject<?,?>> stream, BackupAliasType backupAliasType,
			long maximumRowsPerFile) throws IOException {
		ValidateArgument.required(out, "OutputStream");
		ValidateArgument.required(stream, "Stream");
		ValidateArgument.required(backupAliasType, "BackupAliasType");
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(out));
		try {
			MigrationType currentType = null;
			int index = 0;

			List<MigratableDatabaseObject<?,?>> currentBatch = null;

			// Process all rows as a stream
			for (MigratableDatabaseObject<?,?> row : stream) {
				if (currentType == null) {
					// first row
					currentType = row.getMigratableTableType();
					currentBatch = new LinkedList<>();
				}

				/*
				 * Each file added to the zip will only contain one type. Also, each file must
				 * fit within memory. Therefore, the current batch must be written to the zip
				 * file if the maximum batch size is exceeded or if there is a type change.
				 */
				if (currentType != row.getMigratableTableType() || currentBatch.size() >= maximumRowsPerFile) {
					writeBatchToZip(zos, currentBatch, index, currentType, backupAliasType);
					currentBatch.clear();
					index++;
				}
				// add this row to the current batch
				currentBatch.add(row);
				currentType = row.getMigratableTableType();
			}
			// Write the remaining data
			writeBatchToZip(zos, currentBatch, index, currentType, backupAliasType);
		} finally {
			IOUtils.closeQuietly(zos);
		}
	}

	/**
	 * Write a single batch of rows as a new sub-file in the zip stream.
	 * 
	 * @param backupAliasType
	 * @param zos
	 * @param currentType
	 * @param index
	 * @param currentBatch
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	public<D extends DatabaseObject<D>, B> void writeBatchToZip(ZipOutputStream zos, List<MigratableDatabaseObject<?,?>> currentBatch, int index, MigrationType currentType,
			BackupAliasType backupAliasType) throws IOException {
		// Write the current batch as a sub-file to the zip
		String fileName = createFileName(currentType, index);
		ZipEntry entry = new ZipEntry(fileName);
		zos.putNextEntry(entry);
		Writer zipWriter = new OutputStreamWriter(zos, UTF_8);

		MigratableDatabaseObject<D, B> mdo = typeProvider.getObjectForType(currentType);
		String alias = getAlias(mdo, backupAliasType);
		MigratableTableTranslation<D,B> translator = mdo.getTranslator();
		
		// translate to the backup objects
		List<B> backupObjects = new LinkedList<>();
		for(MigratableDatabaseObject<?,?> migrationOjbect: currentBatch) {
			B backupObject = translator.createBackupFromDatabaseObject((D) migrationOjbect);
			backupObjects.add(backupObject);
		}

		XStream xstream = new XStream();
		xstream.alias(alias, mdo.getBackupClass());
		xstream.toXML(backupObjects, zipWriter);
		zipWriter.flush();
	}

	/**
	 * Read a batch of rows from the ZipInputStream.  If an empty list is returned then there is no
	 * more data within the zip.
	 * @param zipStream
	 * @param backupAliasType
	 * @return Will return the contents of the next file in the zip.  An empty list will be returned if 
	 * no more data could be read from the stream.
	 */
	public <D extends DatabaseObject<D>, B> List<MigratableDatabaseObject<?,?>> readBatchFromZip(ZipInputStream zipStream, BackupAliasType backupAliasType) {
		try {
			// Keep reading files until new data is found.
			ZipEntry entry;
			while((entry = zipStream.getNextEntry()) != null) {
				// Read the zip entry.
				MigrationType type = getTypeFromFileName(entry.getName());
				// Lookup the object for the type.
				MigratableDatabaseObject<D, B> mdo = typeProvider.getObjectForType(type);
				String alias = getAlias(mdo, backupAliasType);
				MigratableTableTranslation<D,B> translator = mdo.getTranslator();

				XStream xstream = new XStream();
				xstream.alias(alias, mdo.getBackupClass());
				List<B> backupObjects;
				try {
					backupObjects = (List<B>) xstream.fromXML(zipStream);
				} catch (StreamException e) {
					if (!e.getMessage().contains(INPUT_CONTAINED_NO_DATA)) {
						throw new RuntimeException(e);
					}
					// This file is empty so move to the next file...
					continue;
				}
				// Translate the results
				List<MigratableDatabaseObject<?,?>> translated = new LinkedList<>();
				for(B backupObject: backupObjects) {
					D databaseObject = translator.createDatabaseObjectFromBackup(backupObject);
					translated.add((MigratableDatabaseObject<?, ?>) databaseObject);
				}
				return translated;
			}
			// No new data was found in the zip
			return new LinkedList<>();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}

}
