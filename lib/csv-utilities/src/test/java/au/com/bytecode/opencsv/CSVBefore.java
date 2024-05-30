package au.com.bytecode.opencsv;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class CSVBefore {
	
	public static void main(String[] args) {
		String fileName = "test.csv";
		try {
			// duplicate full set of settings of CSV file format
			CSVWriter writer = new CSVWriter(new OutputStreamWriter(
					new FileOutputStream(fileName), "UTF-8"),
					';', '\'');
			try {
				// we have to create arrays manually
				writer.writeNext(new String[] {"Header1", "Header2"});
				writer.writeNext(new String[] {"v11", "v12"});
				writer.writeNext(new String[] {"v21", "v22"});
			} finally {
				// we have to close writer manually
				writer.close();	
			}
		} catch (IOException e) {
			// we have to process exceptions when it is not required
			e.printStackTrace();
		}

		try {
			// duplicate full set of settings of CSV file format
			CSVReader reader = new CSVReader(new InputStreamReader(
					new FileInputStream(fileName), "UTF-8"), 
					';', '\'', 1); // it is not clear what arguments means 
			try {
				
				String[] values = reader.readNext();
				while ( values != null ) {
					values = reader.readNext();
				}
			} finally {
				// we have to close reader manually
				reader.close();
			}
		} catch (IOException e) {
			// we have to process exceptions when it is not required
			e.printStackTrace();
		}
	}
}
