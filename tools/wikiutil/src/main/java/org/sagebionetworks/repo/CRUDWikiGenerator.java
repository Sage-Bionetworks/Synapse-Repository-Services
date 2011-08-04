package org.sagebionetworks.repo;

import java.util.Iterator;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONObject;

/**
 * WikiGenerator is used to auto-generate the wiki for the Platform Repository
 * Service.
 * 
 * All the log output goes to stdout. See generateRepositoryServiceWiki.sh for
 * how the log output is cleaned and turned into an actual wiki page. The reason
 * why this writes log output file instead of a normal output to stdout is
 * because I want to include the response headers logged by HttpClient and this
 * was a quick way to make that happen.
 * 
 * Also note that I originally wrote this against HtmlUnit so that I could
 * include a bit of testing to make sure the responses coming back were sane,
 * but HtmlUnit does not support PUT or DELETE.
 * 
 * {code} svn checkout
 * https://sagebionetworks.jira.com/svn/PLFM/trunk/tools/wikiutil cd wikiutil
 * ~/platform/trunk/tools/wikiutil>mvn clean compile
 * ~/platform/trunk/tools/wikiutil>./generateRepositoryServiceWiki.sh
 * http://localhost:8080 > wiki.txt {code}
 * 
 */
public class CRUDWikiGenerator {

	private static final Logger log = Logger.getLogger(WikiGenerator.class
			.getName());

	/**
	 * @param args
	 * @return the number of errors encountered during execution
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static int main(String[] args) {
		
		DateTime now = new DateTime();
		String timestamp = "(Wiki Generator " + now.toString().replace(":", "_").replace("-", "_") + ")";

		try {

			WikiGenerator wiki = WikiGenerator
					.createWikiGeneratorFromArgs(args);

			wiki
					.doLogin("h2. Log into Synapse",
							"You must have an account with permission to create entities in Synapse.");

			log.info("h2. Create/Update/Delete Examples");
			log
					.info("You can create entities, update entities, read entities, and delete entities.  More advanced querying is implemented as a separate API. "
							+ "Partial updates (e.g., just updating two fields in a dataset) are not supported.  In a nutshell, when you update something like a "
							+ "dataset, you GET the dataset first, modify the properties you want to change, and then send the entire object back to the service "
							+ "so that this revised entity overwrites the previously stored entity.  Conflicting updates are detected and rejected through the use of the ETag header.");

			JSONObject project = wiki
					.doPost(
							"/project",
							new JSONObject(
									"{\"name\": \"SageBioCuration " + timestamp + "\"}"),
							"h3. Create a Project",
							"Note that the request is a POST and the content type of the data we are sending to the service is json");

			JSONObject eula = wiki
					.doPost(
							"/eula",
							new JSONObject(
									"{\"name\": \"SageBioCurationEula " + timestamp + "\", \"agreement\": \"<p><b><larger>Copyright 2011 Sage Bionetworks</larger></b><br/><br/></p><p>Licensed under the Apache License, Version 2.0 ...\"}"),
							"h3. Create a new End-User Licence Agreement",
							"Create a new End-User License Agreement to specify the terms of use for a dataset and how the dataset should be cited.");

			JSONObject dataset = wiki
					.doPost(
							"/dataset",
							new JSONObject(
									"{\"status\": \"Pending\", \"description\": \"Genetic and epigenetic alterations have been identified that ...\", "
											+ "\"creator\": \"Charles Sawyers\", \"releaseDate\": \"2008-09-14\", \"version\": \"1.0.0\", \"name\": \"MSKCC Prostate Cancer\", \"parentId\":\""
											+ project.getString("id")
											+ "\", \"eulaId\": \""
											+ eula.getString("id") + "\"}"),
							"h3. Create a Dataset",
							"Note that the id of the End-User License Agreement we just created is passed as the value of {{eulaId}} for the dataset so that the End-User License Agreement is bound to the dataset");

			dataset.put("status", "Current");
			wiki
					.doPut(
							dataset.getString("uri"),
							dataset,
							"h3. Update a Dataset",
							"In this example status field was changed but all others remain the same. Note that the request is a PUT."
									+ "  Also note that the change in the URI to include the id of the dataset we wish to update and the ETag header using the "
									+ "value previously returned.");

			log.info("h3. Add Annotations to a Dataset");
			JSONObject storedAnnotations = wiki
					.doGet(dataset.getString("annotations"),
							"h4. Get the annotations",
							"First get the current annotations for your newly created dataset.");

			JSONObject cannedAnnotations = new JSONObject(
					"{\"doubleAnnotations\": {}, \"dateAnnotations\": {\"last_modified_date\": [\"2009-03-06\"]}, \"longAnnotations\": {\"number_of_downloads\": "
							+ "[32.0], \"number_of_followers\": [7.0], \"Number_of_Samples\": [218.0], \"pubmed_id\": [20579941.0]}, \"stringAnnotations\": "
							+ "{\"Posting_Restriction\": [\"unspecified\"], \"citation\": [\"Integrative genomic profiling of human prostate cancer. Taylor BS, Schultz N, "
							+ "Hieronymus H, Gopalan A, Xiao Y, Carver BS, Arora VK, Kaushik P, Cerami E, Reva B, Antipin Y, Mitsiades N, Landers T, Dolgalev I, "
							+ "Major JE, Wilson M, Socci ND, Lash AE, Heguy A, Eastham JA, Scher HI, Reuter VE, Scardino PT, Sander C, Sawyers CL, Gerald WL. Cancer "
							+ "Cell. 2010 Jul 13;18(1):11-22.  \"], \"Disease\": [\"Cancer\"], \"Species\": [\"Human\"], \"Internal_Name\": [\"Prostate cancer-MSKCC\"], "
							+ "\"Tissue_Tumor\": [\"Prostate\"], \"Type\": [\"GCD\"], \"Institution\": [\"Memorial Sloan Kettering Cancer Center\"]}}");

			// Merge our canned annotations to our annotations object
			Iterator<String> keyIter = cannedAnnotations.keys();
			while (keyIter.hasNext()) {
				String key = keyIter.next();
				// Annotations need to go one level deeper
				JSONObject storedAnnotationBucket = storedAnnotations
						.getJSONObject(key);
				JSONObject cannedAnnotationBucket = cannedAnnotations
						.getJSONObject(key);
				Iterator<String> annotationIter = cannedAnnotationBucket.keys();
				while (annotationIter.hasNext()) {
					String annotationKey = annotationIter.next();
					storedAnnotationBucket.put(annotationKey,
							cannedAnnotationBucket.get(annotationKey));
				}
			}

			wiki
					.doPut(
							dataset.getString("annotations"),
							storedAnnotations,
							"h4. Put the annotations",
							"Then you add new annotations to the existing annotations, or modify the existing annotations, and do a PUT. *Note that annotation values must always be arrays even if the "
									+ "array is only of length one.*");

			JSONObject layer = wiki
					.doPost(
							"/layer",
							new JSONObject(
									"{\"parentId\":\""
											+ dataset.getString("id")
											+ "\", \"status\": \"curated\", \"name\": \"phenotypes\", \"numSamples\": \"261\", \"platform\": \"\", \"version\": \"1.0.0\", \"type\": \"C\"}"),
							"h3. Add a Layer to a Dataset",
							"Create a new layer object and set its parentId to be that of the dataset");

			log.info("h3. Add a Location to a Layer");

			JSONObject cannedLocation = new JSONObject(
					"{\"parentId\":\""
							+ layer.getString("id")
							+ "\",\"path\": \"mskcc_prostate_cancer.phenotype.zip\", \"md5sum\": \"b513a23fc54b7b0d65312e1a900af5a6\", \"type\": \"awss3\"}");

			JSONObject s3Location = wiki
					.doPost(
							"/location",
							cannedLocation,
							"h4. First create the location to which to PUT your data.",
							"First get the presigned S3 URL to use for the upload.  You must specify the parentId of the Layer (or Dataset) to which this location belongs.");

			log.info("h4. The PUT the data to S3");
			log
					.info("Then PUT the data to S3 via an HTTP Client that supports multipart upload.  Note that you must: ");
			log.info("# Add header {{Content-Type: application/binary}}");
			log
					.info("# Add header {{Content-MD5: <the base64 encoded md5 checksum>}}");
			log.info("# Add header {{x-amz-acl: bucket-owner-full-control}}");
			log.info("\nHere is an example with curl:{code}");

			// TODO find a more direct way to go from hex to base64
			byte[] encoded = Base64
					.encodeBase64(Hex
							.decodeHex("b513a23fc54b7b0d65312e1a900af5a6"
									.toCharArray()));
			String base64Md5 = new String(encoded, "ASCII");

			log.info("curl -v -X PUT -H Content-MD5:" + base64Md5
					+ " -H x-amz-acl:bucket-owner-full-control \\\n"
					+ " -H Content-Type:application/binary "
					+ " --data-binary @<localFilepath> \\\n'"
					+ s3Location.getString("path") + "'{code}\n\n");

			String cannedPreview = "phenotype_id\tsample_type\tmetastatic_site\tethnicity\tpredxbxpsa\tage\tclinical_primary_gleason\tclinical_secondary_gleason\tclinical_gleason_score\tpre_treatment_psa\tclinical_tnm_stage_t\tneoadjradtx\tchemotx\thormtx\tradtxtype\trp_type\tsms\textra_capsular_extension\tseminal_vesicle_invasion\ttnm_stage_n\tnumber_nodes_removed\tnumber_nodes_positive\tpathologic_tnm_stage_t\tpathologic_primary_gleason\tpathologic_secondary_gleason\tpathologic_gleason_score\tbcr_freetime\tbcr_event\tmetsevent\tsurvtime\tevent\tnomogram_pfp_postrp\tnomogram_nomopred_extra_capsular_extension\tnomogram_nomopred_lni\tnomogram_nomopred_ocd\tnomogram_nomopred_seminal_vesicle_invasion\tcopy_number_cluster\texpression_array_tissue_source\r\nPCA0004\tPRIMARY\tNA\tWhite Non Hispanic\t27.5\t68.93\t3\t2\t5\t11.8\tT2B\tNA\tNA\tNA\tNA\tRP\tNegative\tESTABLISHED\tNegative\tNormal_N0\t13\t0\tT3A\t3\t4\t7\t152.55\tNO\tNO\t152.55\tNO\tNA\t37.937846\t3.593974\t55.082939\tNA\t1\tNA\r\nPCA0006\tPRIMARY\tNA\tWhite Non Hispanic\t15.7\t56.64\t3\t3\t6\t8.2\tT2B\tNA\tNA\tNeoadjuvant HORM\tNA\tRP\tNegative\tNONE\tNegative\tNormal_N0\t4\t0\tT2C\t3\t3\t6\t160.96\tNO\tNO\t160.96\tNO\tNA\tNA\tNA\tNA\tNA\t4\tNA\r\nPCA0016\tPRIMARY\tNA\tWhite Non Hispanic\t12\t67.36\t3\t3\t6\t12\tT2B\tNA\tNA\tNeoadjuvant HORM\tNA\tRP\tNegative\tNONE\tNegative\tNormal_N0\t2\t0\tT2C\t4\t4\t8\t74.22\tNO\tNO\t74.22\tNO\t99\tNA\tNA\tNA\t97.11015465\t2\tNA\r\nPCA0019\tPRIMARY\tNA\tWhite Non Hispanic\t6.6\t68.12\t3\t4\t7\t6.6\tT1C\tNA\tNA\tNA\tNA\tRP\tNegative\tNONE\tNegative\tNormal_N0\t1\t0\tT2C\t3\t3\t6\t110.33\tBCR_Algorithm\tNO\t123.67\tNO\tNA\tNA\tNA\tNA\t79.85545652\t2\tNA\r\nPCA0023\tPRIMARY\tNA\tBlack Non Hispanic\t4.3\t60.57\t4\t3\t7\t3.88\tT1C\tNA\tNA\tPostHORM\tNA\tRP\tPositive\tNONE\tNegative\tNormal_N0\t2\t0\tT2C\t4\t5\t9\t10.61\tBCR_Algorithm\tNO\t72.84\tDEATH FROM OTHER CANCER\t79.85546\t19.190208\t2.138938\t77.240045\t99\t4\tNA\r\n";
			// transfer our canned preview to our preview object
			JSONObject preview = new JSONObject();
			preview.put("parentId", layer.getString("id"));
			preview.put("previewString", cannedPreview);

			wiki
					.doPost("/preview", preview,
							"h3. Add a Preview to a Layer",
							"Create a new preview object and set its parentId to be that of the layer");

			JSONObject agreement = wiki
					.doPost(
							"/agreement",
							new JSONObject("{\"eulaId\": \""
									+ eula.getString("id")
									+ "\", \"datasetId\": \""
									+ dataset.getString("id") + "\"}"),
							"h3. Create an agreement between a user and the End-User Licence Agreement for a particular dataset",
							"Note that the userId is determined from the sessionToken");

			wiki
					.doDelete(
							project.getString("uri"),
							"h3. Delete a Project",
							"Note that the request is a DELETE and no content is returned.  Also note that this will delete all of the datasets layers, etc.");

			return (wiki.getNumErrors());
		} catch (Exception e) {
			log.info(e);
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}