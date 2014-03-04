package org.sagebionetworks.bridge.model.dbo.dao;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.bridge.model.BridgeUserParticipantMappingDAO;
import org.sagebionetworks.bridge.model.ParticipantDataId;
import org.sagebionetworks.bridge.model.dbo.persistence.DBOUserParticipantMap;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import com.google.common.collect.Lists;

public class DBOBridgeUserParticipantMappingDAOImpl implements BridgeUserParticipantMappingDAO {

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	private Encryptor encryptor = new Encryptor();

	@Override
	public List<ParticipantDataId> getParticipantIdsForUser(Long userId) throws IOException, GeneralSecurityException {
		DBOUserParticipantMap userParticipantMap;
		try {
			userParticipantMap = basicDao.getObjectByPrimaryKey(DBOUserParticipantMap.class, new SinglePrimaryKeySqlParameterSource(userId));
		} catch (NotFoundException e) {
			// not an error, just means no mapping yet
			return Collections.<ParticipantDataId> emptyList();
		}
		byte[] mapBlob = userParticipantMap.getMapBlob();
		mapBlob = encryptor.decrypt(mapBlob, StackConfiguration.getBridgeDataMappingEncryptionKey(), userId);
		long[] participantIdList = (long[]) JDOSecondaryPropertyUtils.decompressedObject(mapBlob);

		List<ParticipantDataId> participantDataIds = Lists.newArrayListWithCapacity(participantIdList.length);
		for (int i = 0; i < participantIdList.length; i++) {
			participantDataIds.add(new ParticipantDataId(participantIdList[i]));
		}
		return participantDataIds;
	}

	@Override
	public void setParticipantIdsForUser(Long userId, List<ParticipantDataId> participantDataIds) throws IOException,
			GeneralSecurityException {
		long[] participantIdList = new long[participantDataIds.size()];
		for (int i = 0; i < participantIdList.length; i++) {
			participantIdList[i] = participantDataIds.get(i).getId();
		}

		byte[] mapBlob = JDOSecondaryPropertyUtils.compressObject(participantIdList);
		mapBlob = encryptor.encrypt(mapBlob, StackConfiguration.getBridgeDataMappingEncryptionKey(), userId);
		DBOUserParticipantMap userParticipantMap = new DBOUserParticipantMap();
		userParticipantMap.setUserId(userId);
		userParticipantMap.setMapBlob(mapBlob);
		basicDao.createOrUpdate(userParticipantMap);
	}
}
