package org.sagebionetworks;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Test to validate that the expected spring beans are there for configuration.
 * @author jmhill
 *
 */
public class ExpectedStackBeansTest {
	
	BeanFactory factory;
	
	String[] expectedBeanNames = new String[]{
			"stackConfiguration",
			"stackConfiguration.stackAndStackInstancePrefix",
			// Id generator Properties
			"stackConfiguration.idGeneratorDatabaseDriver",
			"stackConfiguration.idGeneratorDatabaseConnectionUrl",
			"stackConfiguration.idGeneratorDatabaseUsername",
			"stackConfiguration.idGeneratorDatabasePassword",
			// Repository properties
			"stackConfiguration.repositoryDatabaseDriver",
			"stackConfiguration.repositoryDatabaseConnectionUrl",
			"stackConfiguration.repositoryDatabaseUsername",
			"stackConfiguration.repositoryDatabasePassword",
			// Connection pool properties.
			"stackConfiguration.databaseConnectionPoolShouldValidate",
			"stackConfiguration.databaseConnectionPoolValidateSql",
			"stackConfiguration.databaseConnectionPoolMinNumberConnections",
			"stackConfiguration.databaseConnectionPoolMaxNumberConnections",
			// Migration
			"stackConfiguration.migrationBackupBatchMax",
			"stackConfiguration.migrationMaxAllowedPacketBytes",
			
			"stackConfiguration.oAuth2GoogleClientId",
			"stackConfiguration.oAuth2GoogleClientSecret",			

			"stackConfiguration.oAuth2ORCIDClientId",
			"stackConfiguration.oAuth2ORCIDClientSecret",			

			"stackConfiguration.tableRowChangeBucketName",
			"stackConfiguration.tableMaxBytesPerRequest",
			"stackConfiguration.tableUpdateQueueName",
			"stackConfiguration.tableUpdateDeadLetterQueueName",
			"stackConfiguration.tableCurrentCacheUpdateQueueName",
			"stackConfiguration.tableReadTimeoutMS",
			"stackConfiguration.semaphoreGatedMaxRunnersTableCluster",
			"stackConfiguration.tableMaxBytesPerChangeSet",
			
			"stackConfiguration.allowCreationOfOldEntities",
			
			// Semaphore gated runner configuration.
			"stackConfiguration.semaphoreExclusiveMaxTimeoutMS",
			"stackConfiguration.semaphoreSharedMaxTimeoutMS",
			
			"stackConfiguration.searchEnabled",
			
			"stackConfiguration.fileMultipartUploadDaemonTimeoutMS",
			"stackConfiguration.fileMultipartUploadDaemonMainMaxThreads",
			"stackConfiguration.fileMultipartUploadDaemonCopyPartMaxThreads",
			
			"stackConfiguration.auditRecordBucketName",
			"stackConfiguration.stackInstanceNumber",
			"stackConfiguration.stack",
			
			"stackConfiguration.logBucketName",
			
			"stackConfiguration.repositoryChangeTopic[ACCESS_CONTROL_LIST]",
			"stackConfiguration.repositoryChangeTopic[EVALUATION_SUBMISSIONS]",
			"stackConfiguration.repositoryChangeTopic[FILE]",
			"stackConfiguration.repositoryChangeTopic[PRINCIPAL]",
			"stackConfiguration.repositoryChangeTopic[MESSAGE]",
			"stackConfiguration.repositoryChangeTopic[ENTITY]",
			"stackConfiguration.repositoryChangeTopic[WIKI]",
			"stackConfiguration.repositoryChangeTopic[TABLE]",
			"stackConfiguration.repositoryChangeTopic[PROJECT_SETTING]",
			
			"stackConfiguration.asyncQueueName[BULK_FILE_DOWNLOAD]"
	};
	
	@Before
	public void before(){
		// Load the stack configuration bean.
		ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext(
		        new String[] {"stack-configuration.spb.xml"});
		// of course, an ApplicationContext is just a BeanFactory
		factory = (BeanFactory) appContext;
	}
	
	@Test
	public void testExpectedBeans(){
		// Make sure we can load each of the expected beans
		for(String beanName: expectedBeanNames){
			Object bean = factory.getBean(beanName);
//			System.out.println(beanName+"="+bean);
		}
	}

}
