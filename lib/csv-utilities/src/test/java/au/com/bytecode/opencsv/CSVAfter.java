package au.com.bytecode.opencsv;

import java.io.IOException;
import java.util.Arrays;

public class CSVAfter {
	
	// define format of CSV file one time and use everywhere
	// human readable configuration 
	private static final CSV csv = CSV
			.separator(';')
			.quote('\'')
			.skipLines(1)
			.charset("UTF-8")
			.create();

	// do not throw checked exceptions
	public static void main(String[] args) {
		String fileName = "test.csv";
		
		// CSVWriter will be closed after end of processing
		csv.write(fileName, new CSVWriteProc() {
			public void process(CSVWriter out) {
				try {
					out.writeNext("Header1", "Header2");
					out.writeNext("v11", "v12");
					out.writeNext("v21", "v22");
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

			}
		});
		
		// CSVReader will be closed after end of processing
		// Less code to process CSV content -> less bugs
		csv.read(fileName, new CSVReadProc() {
			public void procRow(int rowIndex, String... values) {
				System.out.println(rowIndex + "# " + Arrays.asList(values));	
			}
		});
	}
}
