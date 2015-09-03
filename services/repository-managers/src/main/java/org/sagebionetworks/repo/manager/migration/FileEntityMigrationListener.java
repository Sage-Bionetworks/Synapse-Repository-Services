package org.sagebionetworks.repo.manager.migration;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.beans.factory.annotation.Autowired;

public class FileEntityMigrationListener implements MigrationTypeListener {

	@Autowired
	NodeDAO nodeDAO;

	@Autowired
	FileHandleDao fileHandleDao;

	private static final String FILE_NAME_PROPERTY_NAME = "fileName";

	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(MigrationType type, List<D> delta) {
		// nothing here
		if (type!=MigrationType.NODE_REVISION) return;
		for (D elem : delta) {
			if (!(elem instanceof DBORevision)) continue;
			DBORevision dbo = (DBORevision)elem;
			try {
				NamedAnnotations annotations = JDOSecondaryPropertyUtils.decompressedAnnotations(dbo.getAnnotations());
				Annotations secondaryProperties = annotations.getPrimaryAnnotations();
				Map<String,List<String>> stringAnnotations = secondaryProperties.getStringAnnotations();
				if (stringAnnotations==null) {
					stringAnnotations = new HashMap<String,List<String>>();
					secondaryProperties.setStringAnnotations(stringAnnotations);
				}
				List<String> fileNameValues = stringAnnotations.get(FILE_NAME_PROPERTY_NAME);
				String fileName = (fileNameValues==null || fileNameValues.isEmpty()) ? null : fileNameValues.get(0);
				if (fileName==null) {
					FileHandle fileHandle = fileHandleDao.get(""+dbo.getFileHandleId());
					stringAnnotations.put(FILE_NAME_PROPERTY_NAME, Collections.singletonList(fileHandle.getFileName()));
					nodeDAO.updateAnnotations(""+dbo.getOwner(), annotations);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void beforeDeleteBatch(MigrationType type, List<Long> idsToDelete) {
		// nothing here

	}
}
