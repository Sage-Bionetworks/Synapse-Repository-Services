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
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;

public class BackupFileStreamImpl implements BackupFileStream {
	
	private static final String DOT = ".";
	private static final String FILE_NAME_TEMPLATE = "%1$s.%2$d.xml";
	
	@Autowired
	MigrationTypeProvider typeProvider;

	@Override
	public Iterable<RowData> readBackupFile(InputStream input, BackupAliasType backupAliasType) {
		ValidateArgument.required(input, "input");
		
		return new Iterable<RowData>() {
			@Override
			public Iterator<RowData> iterator() {
				try {
					return new InputStreamIterator(input, backupAliasType);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}


	/**
	 * This Iterator will read one file at a time from the provided zip file.
	 * Note: All data for any single file must fit in memory.
	 *
	 */
	private class InputStreamIterator implements Iterator<RowData> {

		BackupAliasType backupAliasType;
		ZipInputStream zipInputStream;
		MigrationType currentType;
		Iterator<DatabaseObject<?>> currentFile;

		InputStreamIterator(InputStream input, BackupAliasType backupAliasType) throws IOException{
			this.backupAliasType = backupAliasType;
			this.zipInputStream = new ZipInputStream(
					new BufferedInputStream(input));
			this.currentType = null;
			
		}
		
		@Override
		public boolean hasNext() {
			// If the current file exists then attempt to read from it.
			if(currentFile != null) {
				if(currentFile.hasNext()) {
					return true;
				}
			}
			// Attempt to read the next file
			try {
				ZipEntry entry = zipInputStream.getNextEntry();
				if(entry == null) {
					// No more files in the zip so there are no more rows to read.
					return false;
				}
				// Read the zip entry.
				this.currentType = getTypeFromFileName(entry.getName());
				// Lookup the object for the type.
				MigratableDatabaseObject<?,?> mdo = typeProvider.getObjectForType(currentType);
				String alias = getAlias(mdo, backupAliasType);
				
				XStream xstream = new XStream();
				xstream.alias(alias, mdo.getBackupClass());
				try{
					currentFile = ((List<DatabaseObject<?>>) xstream.fromXML(this.zipInputStream)).iterator();
				}catch(StreamException e){
					if(e.getMessage().contains("input contained no data")){
						currentFile = null;
					}else{
						throw new RuntimeException(e);
					}
				}
				// recursive has next
				return this.hasNext();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		}

		@Override
		public RowData next() {
			if(currentFile == null) {
				throw new IllegalStateException("HasNext() must be called before next()"); 
			}
			DatabaseObject<?> row = currentFile.next();
			return new RowData(currentType, row);
		}

	}
	
	/**
	 * Extract the migration type from a file's name.
	 * @param name
	 * @return
	 */
	public static MigrationType getTypeFromFileName(String name) {
		ValidateArgument.required(name, "Name");
		int index = name.indexOf(DOT);
		if(index < 0) {
			throw new IllegalArgumentException("Unexpected file name: "+name);
		}
		return MigrationType.valueOf(name.substring(0, index));
	}
	
	
	/**
	 * Create a FileName for zip entry.
	 * @param type
	 * @param index
	 * @return
	 */
	public static String createFileName(MigrationType type, int index) {
		ValidateArgument.required(type, "MigrationType");
		return String.format(FILE_NAME_TEMPLATE, type.name(), index);
	}
	
	/**
	 * Get the alias used to read the given MigratableDatabaseObject from an XML file.
	 * @param mdo
	 * @param backupAliasType
	 * @return
	 */
	public static String getAlias(MigratableDatabaseObject<?,?> mdo, BackupAliasType backupAliasType) {
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
	
	@Override
	public void writeBackupFile(OutputStream out, Iterable<RowData> stream, BackupAliasType backupAliasType, int maximumRowsPerFile) throws IOException {
		ValidateArgument.required(out, "OutputStream");
		ValidateArgument.required(stream, "Stream");
		ValidateArgument.required(backupAliasType, "BackupAliasType");
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(out));
		try {
			MigrationType currentType = null;
			int index = 0;
			
			List<DatabaseObject<?>> currentBatch = null;
			
			// Process all rows as a stream
			for(RowData row: stream) {
				if(currentType == null) {
					// first row
					currentType = row.getType();
					currentBatch = new LinkedList<>();
				}
				
				/*
				 * Each file added to the zip will only contain one type. Also, each file must
				 * fit within memory. Therefore, the current batch must be written to the zip file
				 * if the maximum batch size is exceeded or if there is a type change.
				 */
				if(currentType != row.getType() || currentBatch.size() >= maximumRowsPerFile) {
					writeBatchToZip(backupAliasType, zos, currentType, index, currentBatch);
					currentBatch.clear();
					index++;
				}
			}
			// Write the remaining data
			writeBatchToZip(backupAliasType, zos, currentType, index, currentBatch);
		}finally {
			IOUtils.closeQuietly(zos);
		}
	}


	/**
	 * Write a single batch of rows as a new sub-file in the zip stream.
	 * @param backupAliasType
	 * @param zos
	 * @param currentType
	 * @param index
	 * @param currentBatch
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	private void writeBatchToZip(BackupAliasType backupAliasType, ZipOutputStream zos, MigrationType currentType,
			int index, List<DatabaseObject<?>> currentBatch) throws IOException, UnsupportedEncodingException {
		ZipEntry entry;
		// Write the current batch as a sub-file to the zip
		entry = new ZipEntry(createFileName(currentType, index));
		zos.putNextEntry(entry);
		Writer zipWriter = new OutputStreamWriter(zos, "UTF-8");
		
		MigratableDatabaseObject<?,?> mdo = typeProvider.getObjectForType(currentType);
		String alias = getAlias(mdo, backupAliasType);
		
		XStream xstream = new XStream();
		xstream.alias(alias, mdo.getBackupClass());
		xstream.toXML(currentBatch, zipWriter);
		zipWriter.flush();
	}
	

}
