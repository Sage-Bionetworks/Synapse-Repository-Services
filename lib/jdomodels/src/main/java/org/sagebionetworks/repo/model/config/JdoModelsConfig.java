package org.sagebionetworks.repo.model.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.AwsClientFactory;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.aws.v2.AwsClientFactoryV2;
import org.sagebionetworks.database.semaphore.SemaphoreConfig;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGeneratorConfig;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACL_SCHEME;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.RealmDao;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.bootstrap.AccessBootstrapData;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapData;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapper;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapperImpl;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDaoImpl;
import org.sagebionetworks.repo.model.dbo.DDLUtils;
import org.sagebionetworks.repo.model.dbo.DDLUtilsImpl;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.auth.RealmDaoImpl;
import org.sagebionetworks.repo.model.dbo.dao.DBOGroupMembersDAOImpl;
import org.sagebionetworks.repo.model.dbo.dao.DBOUserGroupDAOImpl;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableDAO;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableDAOImpl;
import org.sagebionetworks.repo.model.dbo.migration.MigrationTypeProvider;
import org.sagebionetworks.repo.model.dbo.migration.MigrationTypeProviderImpl;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.principal.BootstrapAlias;
import org.sagebionetworks.repo.model.principal.BootstrapGroup;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.sagebionetworks.repo.model.principal.BootstrapUser;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.DefaultClock;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.util.FileProviderImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.amazonaws.services.s3.transfer.TransferManager;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.glue.GlueClient;

@Configuration
@EnableAspectJAutoProxy
@Import({ DatabaseInfrastructureConfiguration.class, SemaphoreConfig.class, IdGeneratorConfig.class })
@ComponentScan(basePackages = {
	"org.sagebionetworks.repo.model.dbo.auth",
	"org.sagebionetworks.repo.model.dbo.dao",
	"org.sagebionetworks.repo.model.dbo.principal",
	"org.sagebionetworks.repo.model.dbo.wikiV2",
	"org.sagebionetworks.repo.model.dbo.asynch",
	"org.sagebionetworks.repo.model.dbo.file",
	"org.sagebionetworks.repo.model.message",
	"org.sagebionetworks.repo.model.query",
	"org.sagebionetworks.repo.model.jdo",
	"org.sagebionetworks.repo.model.bootstrap",
	"org.sagebionetworks.evaluation.dao",
	"org.sagebionetworks.repo.throttle"
})
public class JdoModelsConfig {

	@Bean
	public DDLUtils ddlUtils(JdbcTemplate jdbcTemplate, StackConfiguration stackConfiguration) {
		return new DDLUtilsImpl(jdbcTemplate, stackConfiguration);
	}

	@Bean
	List<DatabaseObject> getAllDBOs() {
		return DboAutoDiscovery.discoverAllDatabaseObjects();
	}

	@Bean
	public DBOBasicDao dboBasicDao(DDLUtils ddlUtils, JdbcTemplate jdbcTemplate,
			NamedParameterJdbcTemplate namedJdbcTemplate, List<DatabaseObject> alldbos) {
		return new DBOBasicDaoImpl(ddlUtils, jdbcTemplate, namedJdbcTemplate, alldbos, createFunctionMap());
	}

	/**
	 * Creates the map of MySQL functions to be created/updated. These are custom
	 * MySQL functions used by the application.
	 */
	private Map<String, String> createFunctionMap() {
		Map<String, String> functionMap = new HashMap<>();
		functionMap.put("getEntityBenefactorId", "schema/functions/GetEntityBenefactorId.ddl.sql");
		functionMap.put("getEntityProjectId", "schema/functions/GetEntityProjectId.ddl.sql");
		functionMap.put("getEntityHierarchy", "schema/functions/GetEntityHierarchy.ddl.sql");
		return functionMap;
	}

	/**
	 * Creates the MigratableTableDAO bean with primary migration objects. The order
	 * of this list determines migration order - dependencies first!
	 */
	@Bean(initMethod = "initialize")
	@DependsOn("dboBasicDao")
	public MigratableTableDAO migratableTableDAO(@Qualifier("migrationJdbcTemplate") JdbcTemplate migrationJdbcTemplate,
			StackConfiguration stackConfiguration, List<DatabaseObject> allDbos) {
		MigratableTableDAOImpl dao = new MigratableTableDAOImpl(migrationJdbcTemplate, stackConfiguration);
		dao.setDatabaseObjectRegister(DboAutoDiscovery.discoverPrimaryMigratableDatabaseObjects(allDbos));
		return dao;
	}

	@Bean
	public Clock clock() {
		return new DefaultClock();
	}

	/**
	 * Bootstrap principals that are created on system initialization. DO NOT CHANGE
	 * ANY OF THESE IDS as they represent real objects in production.
	 */
	@Bean
	public List<BootstrapPrincipal> bootstrapPrincipals() {
		// @formatter:off
		return List.of(
			new BootstrapUser()
				.setId(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId())
				.setEmail(new BootstrapAlias().setAliasName("migrationAdmin@sagebase.org").setAliasId(1L))
				.setUserName(new BootstrapAlias().setAliasName("migrationAdmin").setAliasId(11866L)),

			new BootstrapGroup()
				.setId(BOOTSTRAP_PRINCIPAL.ADMINISTRATORS_GROUP.getPrincipalId())
				.setGroupAlias(new BootstrapAlias().setAliasName("Administrators").setAliasId(2L)),

			new BootstrapGroup()
				.setId(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId())
				.setGroupAlias(new BootstrapAlias().setAliasName("AUTHENTICATED_USERS").setAliasId(3L)),

			new BootstrapGroup()
				.setId(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId())
				.setGroupAlias(new BootstrapAlias().setAliasName("PUBLIC").setAliasId(4L)),

			new BootstrapUser()
				.setId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId())
				.setEmail(new BootstrapAlias().setAliasName("anonymous@sagebase.org").setAliasId(5L))
				.setUserName(new BootstrapAlias().setAliasName("anonymous").setAliasId(11867L)),

			new BootstrapGroup()
				.setId(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId())
				.setGroupAlias(new BootstrapAlias().setAliasName("CERTIFIED_USERS").setAliasId(6L)),

			new BootstrapGroup()
				.setId(BOOTSTRAP_PRINCIPAL.SYNAPSE_TESTING_GROUP.getPrincipalId())
				.setGroupAlias(new BootstrapAlias().setAliasName("TESTING_USERS").setAliasId(7L)),

			new BootstrapUser()
				.setId(BOOTSTRAP_PRINCIPAL.DATA_ACCESS_NOTFICATIONS_SENDER.getPrincipalId())
				.setEmail(new BootstrapAlias().setAliasName("act@sagebase.org").setAliasId(172631L))
				.setUserName(new BootstrapAlias().setAliasName("ACTAccessManagement").setAliasId(172630L)),

			new BootstrapGroup()
				.setId(BOOTSTRAP_PRINCIPAL.SAGE_BIONETWORKS.getPrincipalId())
				.setGroupAlias(new BootstrapAlias().setAliasName("Sage Bionetworks").setAliasId(11577L))
		);
		// @formatter:on
	}

	/**
	 * Creates MigrationTypeProvider from the MigratableTableDAO. This bean remains
	 * here because it depends on MigratableTableDAO which is defined in
	 * lib-jdomodels.
	 */
	@Bean
	public MigrationTypeProvider createMigrationTypeProvider(MigratableTableDAO migratableTableDao) {
		return new MigrationTypeProviderImpl(migratableTableDao.getAllMigratableTypes());
	}

	@Bean(name = "realmDao")
	public RealmDao getRealmDao(DBOBasicDao basicDao, IdGenerator idGenerator,
			NamedParameterJdbcTemplate namedJdbcTemplate, JdbcTemplate jdbcTemplate) {
		RealmDaoImpl dao = new RealmDaoImpl(basicDao, idGenerator, namedJdbcTemplate, jdbcTemplate);
		dao.bootstrapDefaultRealm();
		return dao;
	}

	@Bean(name = "userGroupDAO")
	public UserGroupDAO getUserGroupDAO(RealmDao realmDao, DBOBasicDao basicDao, IdGenerator idGenerator,
			TransactionalMessenger transactionalMessenger, NamedParameterJdbcTemplate namedJdbcTemplate,
			JdbcTemplate jdbcTemplate, List<BootstrapPrincipal> bootstrapPrincipals) {
		DBOUserGroupDAOImpl dao = new DBOUserGroupDAOImpl(basicDao, idGenerator, transactionalMessenger,
				namedJdbcTemplate, jdbcTemplate, bootstrapPrincipals);
		try {
			dao.bootstrapUsers();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		java.util.Map<String, Long> principalIdToRealmPrincipalDboId = new java.util.HashMap<>();
		principalIdToRealmPrincipalDboId.put(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString(), 1L);
		principalIdToRealmPrincipalDboId.put(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId().toString(),
				2L);
		principalIdToRealmPrincipalDboId.put(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId().toString(), 3L);
		principalIdToRealmPrincipalDboId.put(BOOTSTRAP_PRINCIPAL.ADMINISTRATORS_GROUP.getPrincipalId().toString(), 4L);

		realmDao.addPrincipalsToDefaultRealm(principalIdToRealmPrincipalDboId);
		return dao;
	}

	/**
	 * Creates GroupMembersDAO with dependency on UserGroupDAO to ensure proper
	 * initialization order. Note: The bootstrapGroups() method is currently a no-op
	 * - actual group bootstrap happens in TeamManagerImpl.bootstrapTeams().
	 */
	@Bean
	public GroupMembersDAO groupMembersDAO(NamedParameterJdbcTemplate namedJdbcTemplate, JdbcTemplate jdbcTemplate,
			UserGroupDAO userGroupDAO, TransactionalMessenger transactionalMessenger) {
		DBOGroupMembersDAOImpl dao = new DBOGroupMembersDAOImpl(namedJdbcTemplate, jdbcTemplate, userGroupDAO,
				transactionalMessenger);
		try {
			dao.bootstrapGroups();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return dao;
	}

	@Bean
	public SynapseS3Client amazonS3Client() {
		return AwsClientFactory.createAmazonS3Client();
	}

	@Bean
	public AthenaClient amazonAthenaClient() {
		return AwsClientFactoryV2.createAthenaClient();
	}

	@Bean
	public GlueClient amazonGlueClient() {
		return AwsClientFactoryV2.createGlueClient();
	}

	@Bean
	public TransferManager transferManager() {
		return AwsClientFactory.createTransferManager();
	}

	@Bean
	public FileProvider fileProvider() {
		return new FileProviderImpl();
	}

	@Bean
	public EntityBootstrapData rootFolderBootstrapData(StackConfiguration stackConfiguration) {
		EntityBootstrapData data = new EntityBootstrapData();
		data.setEntityPath(stackConfiguration.getRootFolderEntityPath());
		data.setEntityId(Long.valueOf(stackConfiguration.getRootFolderEntityId()));
		data.setEntityDescription("The root Synapse folder containing all other entities.");
		data.setEntityType(EntityType.folder);
		data.setDefaultChildAclScheme(ACL_SCHEME.GRANT_CREATOR_ALL);
		AccessBootstrapData access = new AccessBootstrapData();
		access.setGroup(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP);
		access.setAccessTypeList(List.of(ACCESS_TYPE.CREATE));
		data.setAccessList(List.of(access));
		return data;
	}

	@Bean
	public EntityBootstrapData trashFolderBootstrapData(StackConfiguration stackConfiguration) {
		EntityBootstrapData data = new EntityBootstrapData();
		data.setEntityPath(stackConfiguration.getTrashFolderEntityPath());
		data.setEntityId(Long.valueOf(stackConfiguration.getTrashFolderEntityId()));
		data.setEntityDescription("The trash can folder.");
		data.setEntityType(EntityType.folder);
		data.setDefaultChildAclScheme(ACL_SCHEME.INHERIT_FROM_PARENT);
		AccessBootstrapData access = new AccessBootstrapData();
		access.setGroup(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP);
		access.setAccessTypeList(List.of(ACCESS_TYPE.CREATE));
		data.setAccessList(List.of(access));
		return data;
	}

	@Bean(initMethod = "bootstrapAll")
	public EntityBootstrapper entityBootstrapper(List<EntityBootstrapData> bootstrapEntities) {
		EntityBootstrapperImpl bootstrapper = new EntityBootstrapperImpl();
		bootstrapper.setBootstrapEntities(bootstrapEntities);
		return bootstrapper;
	}

}
