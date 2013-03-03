package org.sagebionetworks.repo.manager.backup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;

/**
 * A simple stub implementation of MigratableManager.
 * 
 * It will capture the ID of the 
 * 
 * @author John
 *
 */
public class MigratableManagerStub implements MigratableManager{
	
	private Set<String> idSet = new HashSet<String>();

	@Override
	public void writeBackupToOutputStream(String idToBackup, OutputStream zos) {
		// Just write the id to the String
		try {
			idSet.add(idToBackup);
			zos.write(idToBackup.getBytes("UTF-8"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String createOrUpdateFromBackupStream(InputStream zin) {
		//Assume the stream only contains the ID.
		try {
			StringWriter writer = new StringWriter();
			IOUtils.copy(zin, writer, "UTF-8");
			String theString = writer.toString();
			idSet.add(theString);
			return theString;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void deleteByMigratableId(String id) {
		idSet.remove(id);
	}

	public Set<String> getIdSet() {
		return idSet;
	}

	public void setIdSet(Set<String> idSet) {
		this.idSet = idSet;
	}

}
