package org.sagebionetworks.repo.manager.authentication;

import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.migration.MigrationTypeListener;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.TermsOfServiceAgreement;
import org.sagebionetworks.repo.model.dbo.auth.DBOTermsOfServiceAgreementMigrationDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.util.TemporaryCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@TemporaryCode(author = "marco", comment = "This needs to be removed after a release, together with the DBOTermsOfUseAgreement")
public class TermsOfServiceMigration implements MigrationTypeListener<DBOTermsOfUseAgreement> {

	private DBOTermsOfServiceAgreementMigrationDao migrationDao;
	
	public TermsOfServiceMigration(DBOTermsOfServiceAgreementMigrationDao migrationDao) {
		this.migrationDao = migrationDao;
	}

	@Override
	public boolean supports(MigrationType type) {
		return MigrationType.TERMS_OF_USE_AGREEMENT.equals(type);
	}

	@Override
	public void beforeCreateOrUpdate(JdbcTemplate jdbcTemplate, List<DBOTermsOfUseAgreement> batch) {}

	@Override
	public void afterCreateOrUpdate(JdbcTemplate jdbcTemplate, List<DBOTermsOfUseAgreement> batch) {
		if (batch == null || batch.isEmpty()) {
			return;
		}
		
		List<Long> userIds = batch.stream()
			.filter(DBOTermsOfUseAgreement::getAgreesToTermsOfUse)
			.map(DBOTermsOfUseAgreement::getPrincipalId)
			.filter(Predicate.not(BOOTSTRAP_PRINCIPAL::isBootstrapPrincipalId))
			.collect(Collectors.toList());
		
		if (userIds.isEmpty()) {
			return;
		}
		
		Date now = new Date();
				
		List<TermsOfServiceAgreement> addList = migrationDao.getUsersWithoutAgreement(jdbcTemplate, userIds).stream().map( u -> new TermsOfServiceAgreement()
			.setUserId(Long.valueOf(u.getId()))
			.setAgreedOn(u.getCreationDate() == null ? now : u.getCreationDate())
			.setVersion(AuthenticationDAO.DEFAULT_TOS_REQUIREMENTS.getMinimumTermsOfServiceVersion())
		).collect(Collectors.toList());
		
		migrationDao.batchAddTermsOfServiceAgreement(jdbcTemplate, addList);
	}
}
