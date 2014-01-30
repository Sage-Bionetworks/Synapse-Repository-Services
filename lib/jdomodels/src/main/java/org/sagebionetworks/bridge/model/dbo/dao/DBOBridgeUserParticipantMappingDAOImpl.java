package org.sagebionetworks.bridge.model.dbo.dao;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.bridge.model.BridgeUserParticipantMappingDAO;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataStatus;
import org.sagebionetworks.bridge.model.dbo.persistence.DBOUserParticipantMap;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class DBOBridgeUserParticipantMappingDAOImpl implements BridgeUserParticipantMappingDAO {

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	private Encryptor encryptor = new Encryptor();

	@SuppressWarnings("unchecked")
	@Override
	public List<String> getParticipantIdsForUser(Long userId) throws IOException, GeneralSecurityException {
		DBOUserParticipantMap userParticipantMap;
		try {
			userParticipantMap = basicDao.getObjectByPrimaryKey(DBOUserParticipantMap.class, new SinglePrimaryKeySqlParameterSource(userId));
		} catch (NotFoundException e) {
			// not an error, just means no mapping yet
			return Collections.<String> emptyList();
		}
		byte[] mapBlob = userParticipantMap.getMapBlob();
		mapBlob = encryptor.decrypt(mapBlob, StackConfiguration.getBridgeDataMappingEncryptionKey(), userId);

		return (List<String>) JDOSecondaryPropertyUtils.decompressedObject(mapBlob);

	}

	@Override
	public void setParticipantIdsForUser(Long userId, List<String> participantIds) throws IOException, GeneralSecurityException {
		if (!(participantIds instanceof ArrayList)) {
			// make sure we don't serialize anything but simple arraylist
			participantIds = new ArrayList<String>(participantIds);
		}
		byte[] mapBlob = JDOSecondaryPropertyUtils.compressObject(participantIds);
		mapBlob = encryptor.encrypt(mapBlob, StackConfiguration.getBridgeDataMappingEncryptionKey(), userId);
		DBOUserParticipantMap userParticipantMap = new DBOUserParticipantMap();
		userParticipantMap.setUserId(userId);
		userParticipantMap.setMapBlob(mapBlob);
		basicDao.createOrUpdate(userParticipantMap);
	}
}
