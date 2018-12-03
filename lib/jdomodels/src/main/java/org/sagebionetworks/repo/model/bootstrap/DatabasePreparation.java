package org.sagebionetworks.repo.model.bootstrap;


import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.dbcp2.BasicDataSource;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;


/**
 * Allows the build to prepare the database.
 * 
 * @author jmhill
 *
 */
public class DatabasePreparation {
	
	
	public static void main(String[] args) throws Exception {
		// This property is not in stack config beacause it is only used by Bamboo/Hudson builds
		String prop = System.getProperties().getProperty("org.sagebionetworks.database.drop.schema");
		if(prop != null && Boolean.parseBoolean(prop)){
			// Drop the current schema
			StackConfiguration config = StackConfigurationSingleton.singleton();
			// This is a safety check to prevent droping prod-database.
			if(isNonProductionStack(config.getStack())){
				// Make the database connection.
				BasicDataSource ds = new BasicDataSource();
				ds.setDriverClassName(config.getRepositoryDatabaseDriver());
				ds.setUsername(config.getRepositoryDatabaseUsername());
				ds.setPassword(config.getRepositoryDatabasePassword());
				// Setup a URL
				String url = config.getRepositoryDatabaseConnectionUrl();
				String dbName = config.getStack()+config.getStackInstance();
				String subURL = url.substring(0, url.length()-dbName.length());
				System.out.println(subURL);
				ds.setUrl(subURL);
				// Now make a connection
				Connection con =  ds.getConnection();
				try{
					Statement statement = con.createStatement();
					try{
						// This will fail if the DB does not exist.
						statement.execute("DROP SCHEMA `"+dbName+"`");
					}catch(SQLException e){
						// This can happen.
					}
					// Create the schema
					statement.execute("CREATE SCHEMA `"+dbName+"`");
				}finally{
					// Close the connection.
					ds.close();
				}
			}
		}
		
	}
	
	/**
	 * Is this a non-production stack?
	 * @param stack
	 * @return
	 */
	public static boolean isNonProductionStack(String stack){
		if("bamboo".equals(stack)) return true;
		if("dev".equals(stack)) return true;
		if("hudson".equals(stack)) return true;
		if("hud".equals(stack)) return true;
		return false;
	}

}
