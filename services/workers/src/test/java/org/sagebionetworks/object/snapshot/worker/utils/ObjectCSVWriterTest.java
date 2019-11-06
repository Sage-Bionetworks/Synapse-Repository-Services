package org.sagebionetworks.object.snapshot.worker.utils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.csv.utils.ObjectCSVReader;
import org.sagebionetworks.csv.utils.ObjectCSVWriter;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.quiz.MultichoiceQuestion;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.Question;
import org.sagebionetworks.repo.model.quiz.ResponseCorrectness;

public class ObjectCSVWriterTest {

	@Test
	public void test() throws IOException {
		PassingRecord pr = new PassingRecord();
		ResponseCorrectness response = new ResponseCorrectness();
		Question question = new MultichoiceQuestion();
		question.setPrompt("If I am not logged into Synapse (\"anonymous user\"), I can:");
		response.setQuestion(question );
		List<ResponseCorrectness> corrections = Arrays.asList(response);
		pr.setCorrections(corrections);
		
		ObjectRecord or = ObjectRecordBuilderUtils.buildObjectRecord(pr, System.currentTimeMillis());
		System.out.println(or.toString());
		System.out.println(or.getJsonString());

		String[] HEADERS = new String[] { "timestamp", "jsonClassName", "jsonString" };

		StringWriter sw = new StringWriter();
		ObjectCSVWriter<ObjectRecord> writer = new ObjectCSVWriter<ObjectRecord>(sw, ObjectRecord.class, HEADERS);
		// Write all of the data
		writer.append(or);
		String output = sw.toString();
		System.out.println(output);
		writer.close();
		sw.close();

		StringReader sr = new StringReader(output);
		ObjectCSVReader<ObjectRecord> reader = new ObjectCSVReader<ObjectRecord>(sr, ObjectRecord.class, HEADERS);
		ObjectRecord back = reader.next();
		System.out.println(back);
		assertEquals(or, back);
		sr.close();
		reader.close();
	}

}
