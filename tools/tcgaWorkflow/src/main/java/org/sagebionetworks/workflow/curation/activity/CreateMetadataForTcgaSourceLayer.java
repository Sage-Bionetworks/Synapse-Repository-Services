package org.sagebionetworks.workflow.curation.activity;

import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.workflow.UnrecoverableException;
import org.sagebionetworks.workflow.curation.ConfigHelper;

public class CreateMetadataForTcgaSourceLayer {
	private static final int datasetIndex = 6;
	private static final int layerTypeIndex = 7;
	private static final int platformIndex = 9;
	private static final int filenameIndex = 11;

	public static Integer doCreateMetadataForTcgaSourceLayer(Integer datasetId,
			String tcgaUrl) throws Exception {
		URL parsedUrl = new URL(tcgaUrl);
		String pathComponents[] = parsedUrl.getPath().split("/");

		if (! "coad".equals(pathComponents[datasetIndex])) {
			throw new UnrecoverableException(
					"only able to handle coad data right now: "
							+ pathComponents[datasetIndex]);
		}

		if (! "cgcc".equals(pathComponents[layerTypeIndex])) {
			throw new UnrecoverableException(
					"only able to handle expression data right now: "
							+ pathComponents[layerTypeIndex]);
		}

		if (!pathComponents[filenameIndex].endsWith(".tar.gz")) {
			throw new UnrecoverableException("malformed filename: "
					+ pathComponents[filenameIndex]);
		}

		String layerName = pathComponents[filenameIndex].substring(0,
				pathComponents[filenameIndex].length() - ".tar.gz".length());

		Synapse synapse = ConfigHelper.createConfig().createSynapseClient();
		
		// TODO get rid of servlet prefix
		String layerUri = "/repo/v1/dataset/" + datasetId + "/layer";
		JSONObject layers = synapse.getEntity(layerUri);
		JSONArray results = layers.getJSONArray("results");
		// TODO query for existing layer
		
		JSONObject layer = new JSONObject();
		layer.put("name", layerName);
		layer.put("type", "E");

		JSONObject storedLayer = synapse.createEntity(layerUri, layer);

		return storedLayer.getInt("id");
	}
}
//	
// private static final Pattern urlPattern =
// Pattern.compile("^((http[s]?):\\/)#?\/?([^:\/\s]+)((\/[\w\-\.]+)*\/)*([\w\-\.]+[^#?\s]+)")
// match = urlPattern.match(url)
// if(match):
// self.protocol = match.group(1)
// self.host = match.group(3)
// self.path = match.group(4)
// self.file = match.group(6)
//
// if(re.search('/bcr/', url)):
// self.type = 'C'
// elif(re.search('/cgcc/', url)):
// self.type = 'E'
//        
// def getPlatform(self):
// parts = re.split('/', self.path)
// return parts[-3]
//
// def getName(self):
// parts = re.split('.tar.gz', self.file)
// return parts[0]
//
// def getType(self):
// return self.type
//
// def getFilename(self):
// return self.file
//
// #------- UNIT TESTS -----------------
// if __name__ == '__main__':
// import java.io.IOException;
//
// import org.sagebionetworks.workflow.UnrecoverableException;
// import
// org.sagebionetworks.workflow.curation.activity.DownloadFromTcga.DownloadResult;
//
// import unittest
//
// class TestTcgaMetadata(unittest.TestCase):
//
// def setUp(self):
// self.meta =
// TcgaMetadata('http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz')
//
// def test_init(self):
// self.assertEqual('http:/', self.meta.protocol)
// self.assertEqual('tcga-data.nci.nih.gov', self.meta.host)
// self.assertEqual('/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/',
// self.meta.path)
// self.assertEqual('unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz',
// self.meta.file)
//	
// }
