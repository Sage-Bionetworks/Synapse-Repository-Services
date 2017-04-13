package org.sagebionetworks.repo.model.dbo.principal;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_ALIAS_DISPLAY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NOTIFICATION_EMAIL_ALIAS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NOTIFICATION_EMAIL_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NOTIFICATION_EMAIL_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NOTIFICATION_EMAIL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PRINCIPAL_ALIAS;

import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sagebionetworks.repo.transactions.WriteTransaction;

public class NotificationEmailDaoImpl implements NotificationEmailDAO {
	private static final String UPDATE_FOR_PRINCIPAL = "UPDATE "+TABLE_NOTIFICATION_EMAIL+
			" SET "+COL_NOTIFICATION_EMAIL_ALIAS_ID+" = ?, "+COL_NOTIFICATION_EMAIL_ETAG+" = ? "+
			" WHERE "+COL_NOTIFICATION_EMAIL_PRINCIPAL_ID+" = ?";
	
	private static final String SELECT_NOTIFICATION_EMAIL_FOR_PRINCIPAL = 
			"SELECT a."+COL_BOUND_ALIAS_DISPLAY+" FROM "+TABLE_NOTIFICATION_EMAIL+" n, "+TABLE_PRINCIPAL_ALIAS+
			" a WHERE n."+COL_NOTIFICATION_EMAIL_ALIAS_ID+"=a."+COL_PRINCIPAL_ALIAS_ID+
			" AND n."+COL_NOTIFICATION_EMAIL_PRINCIPAL_ID+" = ?";
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public NotificationEmailDaoImpl() {}
	
	private static void validateDTO(PrincipalAlias dto) {
		if (dto.getAlias()==null) throw new IllegalArgumentException("email address is missing.");
		if (dto.getAliasId()==null) throw new IllegalArgumentException("aliasId is missing.");
		if (dto.getPrincipalId()==null) throw new IllegalArgumentException("principalId is missing.");
		if (!dto.getType().equals(AliasType.USER_EMAIL)) throw new IllegalArgumentException("Expected USER_EMAIL but found "+dto.getType());
	}
	
	private DBONotificationEmail copyDtoToDbo(PrincipalAlias dto) {
		DBONotificationEmail dbo = new DBONotificationEmail();
		dbo.setAliasId(dto.getAliasId());
		dbo.setPrincipalId(dto.getPrincipalId());
		return dbo;
	}

	@WriteTransaction
	@Override
	public void create(PrincipalAlias dto) {
		validateDTO(dto);
		DBONotificationEmail dbo = copyDtoToDbo(dto);
		dbo.setId(idGenerator.generateNewId(IdType.NOTIFICATION_EMAIL_ID));
		dbo.setEtag(UUID.randomUUID().toString());
		basicDao.createNew(dbo);
	}

	@WriteTransaction
	@Override
	public void update(PrincipalAlias dto) {
		validateDTO(dto);
		String etag = UUID.randomUUID().toString();
		jdbcTemplate.update(UPDATE_FOR_PRINCIPAL, new Object[]{dto.getAliasId(), etag, dto.getPrincipalId()});
	}

	@Override
	public String getNotificationEmailForPrincipal(long principalId) throws NotFoundException {
		try {
			return jdbcTemplate.queryForObject(SELECT_NOTIFICATION_EMAIL_FOR_PRINCIPAL, String.class, principalId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Cannot find notification email for "+principalId);
		}
	}

}
