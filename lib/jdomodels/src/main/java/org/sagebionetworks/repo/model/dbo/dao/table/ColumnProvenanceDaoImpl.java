package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.Optional;

import org.sagebionetworks.repo.model.dao.table.ColumnProvenanceDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOBoundColumnOrdinal;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOColumnProvenance;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.table.ColumnProvenance;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ColumnProvenanceDaoImpl implements ColumnProvenanceDao {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DBOBasicDao basicDao;

	@Override
	public void saveColumnProvenance(IdAndVersion object, ColumnProvenance provenance) {
		ValidateArgument.required(object, "object");
		ValidateArgument.required(provenance, "provenance");
		DBOColumnProvenance dbo = new DBOColumnProvenance();
		dbo.setObjectId(object.getId());
		dbo.setObjectVersion(getVersion(object));
		dbo.setProvenanceJson(JDOSecondaryPropertyUtils.createJSONFromObject(provenance));
		basicDao.createOrUpdate(dbo);
	}

	@Override
	public Optional<ColumnProvenance> getColumnProvenance(IdAndVersion object) {
		ValidateArgument.required(object, "object");
		try {
			String json = jdbcTemplate.queryForObject(
					"SELECT PROVENANCE_JSON FROM COLUMN_PROVENANCE WHERE OBJECT_ID = ? AND OBJECT_VERSION = ?",
					String.class, object.getId(), getVersion(object));
			return Optional.of(JDOSecondaryPropertyUtils.createObjectFromJSON(ColumnProvenance.class, json));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public void clear(IdAndVersion object) {
		ValidateArgument.required(object, "object");
		jdbcTemplate.update("DELETE FROM COLUMN_PROVENANCE WHERE OBJECT_ID = ? AND OBJECT_VERSION = ?", object.getId(),
				getVersion(object));
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM COLUMN_PROVENANCE WHERE OBJECT_ID > -1");
	}

	// A row is stored per object version; an unversioned (current) reference uses the shared null-version sentinel.
	private static long getVersion(IdAndVersion object) {
		return object.getVersion().orElse(DBOBoundColumnOrdinal.DEFAULT_NULL_VERSION);
	}

}
