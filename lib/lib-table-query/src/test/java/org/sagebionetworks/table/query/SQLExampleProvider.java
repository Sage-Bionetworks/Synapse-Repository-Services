package org.sagebionetworks.table.query;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Provides a list of SQLExamples that can be used for documentation an testing.
 * 
 * @author John
 *
 */
public class SQLExampleProvider {
	
	private static SingletonHolder singleton;

	/**
	 * 
	 *Use the classloader for lazy initialization.
	 *
	 */
	private static class SingletonHolder{
		private List<SQLExample> examples;
		private SingletonHolder(){
			// Load the data from the classpath
			String fileName = "SQLSpecification2.csv";
			InputStream in = SQLExampleProvider.class.getClassLoader().getResourceAsStream(fileName);
			if(in == null) throw new IllegalStateException("Cannot find: "+fileName+" on the classpath");
			examples = new LinkedList<SQLExample>();
			try{
				CSVReader reader = new CSVReader(new InputStreamReader(in));
				// Read all the rows
				List<String[]> csvdata = reader.readAll();
				for(String[] row: csvdata){
					if(row != null){
						SQLExample example = new SQLExample(row[0], row[1], row[2]);
						examples.add(example);
					}
				}				
				// Replace the List with an unmodifiable List 
				this.examples = Collections.unmodifiableList(examples);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}finally{
				try {
					in.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
	
	/**
	 * Get singleton list of SQL examples.
	 * 
	 * @return
	 */
	public static List<SQLExample> getSQLExamples(){
		if(singleton == null){
			singleton = new SingletonHolder();
		}
		return singleton.examples;
	}
	
}
