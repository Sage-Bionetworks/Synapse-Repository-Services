package org.sagebionetworks.repo.model.dbo.schema;

import java.sql.ResultSet;
import java.util.Optional;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.Keys;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DerivedAnnotationDaoImpl implements DerivedAnnotationDao {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DBOBasicDao basicDao;

	@Override
	public void saveDerivedAnnotations(String entityId, Annotations annotations) {
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(annotations, "annotations");
		ValidateArgument.required(annotations.getAnnotations(), "annotations");
		if (annotations.getAnnotations().size() < 1) {
			throw new IllegalArgumentException("Annotations must include at least one annotation.");
		}
		DBODerivedAnnotations dbo = new DBODerivedAnnotations();
		dbo.setObjectId(KeyFactory.stringToKey(entityId));
		dbo.setKeys(new JSONObject(annotations.getAnnotations().keySet()).toString());
		try {
			dbo.setKeys(EntityFactory.createJSONStringForEntity(
					new Keys().setKeys(annotations.getAnnotations().keySet().stream().collect(Collectors.toList()))));
			dbo.setAnnotations(EntityFactory.createJSONStringForEntity(annotations));
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		basicDao.createOrUpdate(dbo);
	}

	@Override
	public Optional<Annotations> getDerivedAnnotations(String entityId) {
		ValidateArgument.required(entityId, "entityId");
		try {
			return Optional.of(jdbcTemplate.queryForObject(
					"SELECT ANNOTATIONS FROM DERIVED_ANNOTATIONS WHERE OBJECT_ID = ?", (ResultSet rs, int rowNum) -> {
						try {
							return EntityFactory.createEntityFromJSONString(rs.getString(1), Annotations.class);
						} catch (JSONObjectAdapterException e) {
							throw new RuntimeException(e);
						}
					}, KeyFactory.stringToKey(entityId)));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public Optional<Keys> getDerivedAnnotationKeys(String entityId) {
		ValidateArgument.required(entityId, "entityId");

		try {
			return Optional.of(jdbcTemplate.queryForObject("SELECT ANNO_KEYS FROM DERIVED_ANNOTATIONS WHERE OBJECT_ID = ?",
					(ResultSet rs, int rowNum) -> {
						try {
							return EntityFactory.createEntityFromJSONString(rs.getString(1), Keys.class);
						} catch (JSONObjectAdapterException e) {
							throw new RuntimeException(e);
						}
					}, KeyFactory.stringToKey(entityId)));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public void clearDerivedAnnotations(String entityId) {
		ValidateArgument.required(entityId, "entityId");
		jdbcTemplate.update("DELETE FROM DERIVED_ANNOTATIONS WHERE OBJECT_ID = ?",
				KeyFactory.stringToKey(entityId));

	}

	@Override
	public void clearAll() {
		jdbcTemplate.update("DELETE FROM DERIVED_ANNOTATIONS WHERE OBJECT_ID > -1");
	}

}
