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
			// Id generator Properties
			"stackConfiguration.idGeneratorDatabaseDriver",
			"stackConfiguration.idGeneratorDatabaseConnectionUrl",
			"stackConfiguration.idGeneratorDatabaseUsername",
			"stackConfiguration.idGeneratorDatabasePassword",
			// Repository properties
			"stackConfiguration.repositoryJDOConfigurationMap",
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
			// Semaphore gated runner configuration.
			"stackConfiguration.semaphoreGatedLockTimeoutMS",
			"stackConfiguration.semaphoreGatedMaxRunnersRds",
			"stackConfiguration.semaphoreGatedMaxRunnersSearch",
			"stackConfiguration.semaphoreGatedMaxRunnersFilePreview",
			"stackConfiguration.semaphoreGatedMaxRunnersDynamoIndex",
			"stackConfiguration.semaphoreGatedMaxRunnersDynamoSynchronize",
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
