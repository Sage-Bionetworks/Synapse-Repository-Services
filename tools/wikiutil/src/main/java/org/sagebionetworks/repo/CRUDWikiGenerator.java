package org.sagebionetworks.repo;

import org.apache.log4j.Logger;
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

	private static String serviceEndpoint = "http://localhost:8080";

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		if (1 <= args.length) {
			serviceEndpoint = args[0];
		}

		WikiGenerator wiki = new WikiGenerator(serviceEndpoint);

		log.info("h1. REST API Examples (Create/Update/Delete)");
		log
				.info("You can create entities, update entities, read entities, and delete entities.  More advanced querying is implemented as a separate API. "
						+ "Partial updates (e.g., just updating two fields in a dataset) are not supported.  In a nutshell, when you update something like a "
						+ "dataset, you GET the dataset first, modify the properties you want to change, and then send the entire object back to the service "
						+ "so that this revised entity overwrites the previously stored entity.  Conflicting updates are detected and rejected through the use of the ETag header.");

		JSONObject dataset = wiki
				.doPost(
						"/repo/v1/dataset",
						new JSONObject(
								"{\"status\": \"Pending\", \"description\": \"Genetic and epigenetic alterations have been identified that ...\", "
										+ "\"creator\": \"Charles Sawyers\", \"releaseDate\": \"2008-09-14\", \"version\": \"1.0.0\", \"name\": \"MSKCC Prostate Cancer\"}"),
						"h2. Create a Dataset", "Note that the request is a POST and the content type of the data we are sending to the service is json");

	
		dataset.put("status", "Current");
		wiki
				.doPut(
						dataset.getString("uri"),
						dataset,
						"h2. Update a Dataset",
						"In this example status field was changed but all others remain the same. Note that the request is a PUT."
								+ "  Also note that the change in the URI to include the id of the dataset we wish to update and the ETag header using the "
								+ "value previously returned.");

		log.info("h2. Add Annotations to a Dataset");
		JSONObject annotations = wiki
				.doGet(dataset.getString("annotations"),
						"h3. Get the annotations",
						"First get the empty annotations container for your newly created dataset.");

		JSONObject cannedAnnotations = new JSONObject(
				"{\"doubleAnnotations\": {}, \"dateAnnotations\": {\"last_modified_date\": [\"2009-03-06\"]}, \"longAnnotations\": {\"number_of_downloads\": "
						+ "[32.0], \"number_of_followers\": [7.0], \"Number_of_Samples\": [218.0], \"pubmed_id\": [20579941.0]}, \"stringAnnotations\": "
						+ "{\"Posting_Restriction\": [\"unspecified\"], \"citation\": [\"Integrative genomic profiling of human prostate cancer. Taylor BS, Schultz N, "
						+ "Hieronymus H, Gopalan A, Xiao Y, Carver BS, Arora VK, Kaushik P, Cerami E, Reva B, Antipin Y, Mitsiades N, Landers T, Dolgalev I, "
						+ "Major JE, Wilson M, Socci ND, Lash AE, Heguy A, Eastham JA, Scher HI, Reuter VE, Scardino PT, Sander C, Sawyers CL, Gerald WL. Cancer "
						+ "Cell. 2010 Jul 13;18(1):11-22.  \"], \"Disease\": [\"Cancer\"], \"Species\": [\"Human\"], \"Internal_Name\": [\"Prostate cancer-MSKCC\"], "
						+ "\"Tissue_Tumor\": [\"Prostate\"], \"Type\": [\"GCD\"], \"Institution\": [\"Memorial Sloan Kettering Cancer Center\"]}}");
		// transfer our canned annotations to our annotations object
		annotations.put("dateAnnotations", cannedAnnotations.getJSONObject("dateAnnotations"));
		annotations.put("doubleAnnotations", cannedAnnotations.getJSONObject("doubleAnnotations"));
		annotations.put("longAnnotations", cannedAnnotations.getJSONObject("longAnnotations"));
		annotations.put("stringAnnotations", cannedAnnotations.getJSONObject("stringAnnotations"));
		
		wiki
				.doPut(
						dataset.getString("annotations"),
						annotations,
						"h3. Put the annotations",
						"Then you add/modify the annotations of interest and do a PUT. *Note that annotation values must always be arrays even if the "
								+ "array is only of length one.*");

		JSONObject layer = wiki.doPost(dataset.getString("layer"), new JSONObject("{\"status\": \"curated\", \"name\": \"phenotypes\", \"numSamples\": \"261\", \"platform\": \"\", \"version\": \"1.0.0\", \"type\": \"C\"}"),
				"h2. Add a Layer to a Dataset",
				"Note that the dataset id in the uri denotes to which dataset this layer belongs");

		log.info("h2. Add Locations to a Layer");
		JSONObject locations = wiki.doGet(layer.getJSONArray("locations").getString(0), 
				"h3. Get the locations",
				"First get the empty locations for your newly created layer");

		JSONObject cannedLocations = new JSONObject("{\"locations\": [{\"path\": \"/Shares/external-data/DAT_011__sawyers_prostate_cancer\", \"type\": \"sage\"}, "
				+ "{\"path\": \"mskcc_prostate_cancer.phenotype.zip\", \"md5sum\": \"b513a23fc54b7b0d65312e1a900af5a6\", \"type\": \"awss3\"}]}"); 
		// transfer our canned locations to our locations object
		locations.put("locations", cannedLocations.getJSONArray("locations"));
		
		wiki.doPut(layer.getJSONArray("locations").getString(0), locations, 
				"h3. Put the locations",
				"Then you add/modify the locations of interest and do a PUT.");

		log.info("h2. Add Preview to a Layer");
		JSONObject preview = wiki.doGet(layer.getString("preview"), 
				"h3. Get the preview",
				"First get the empty preview for your newly created layer");

		String cannedPreview = "phenotype_id\tsample_type\tmetastatic_site\tethnicity\tpredxbxpsa\tage\tclinical_primary_gleason\tclinical_secondary_gleason\tclinical_gleason_score\tpre_treatment_psa\tclinical_tnm_stage_t\tneoadjradtx\tchemotx\thormtx\tradtxtype\trp_type\tsms\textra_capsular_extension\tseminal_vesicle_invasion\ttnm_stage_n\tnumber_nodes_removed\tnumber_nodes_positive\tpathologic_tnm_stage_t\tpathologic_primary_gleason\tpathologic_secondary_gleason\tpathologic_gleason_score\tbcr_freetime\tbcr_event\tmetsevent\tsurvtime\tevent\tnomogram_pfp_postrp\tnomogram_nomopred_extra_capsular_extension\tnomogram_nomopred_lni\tnomogram_nomopred_ocd\tnomogram_nomopred_seminal_vesicle_invasion\tcopy_number_cluster\texpression_array_tissue_source\r\nPCA0004\tPRIMARY\tNA\tWhite Non Hispanic\t27.5\t68.93\t3\t2\t5\t11.8\tT2B\tNA\tNA\tNA\tNA\tRP\tNegative\tESTABLISHED\tNegative\tNormal_N0\t13\t0\tT3A\t3\t4\t7\t152.55\tNO\tNO\t152.55\tNO\tNA\t37.937846\t3.593974\t55.082939\tNA\t1\tNA\r\nPCA0006\tPRIMARY\tNA\tWhite Non Hispanic\t15.7\t56.64\t3\t3\t6\t8.2\tT2B\tNA\tNA\tNeoadjuvant HORM\tNA\tRP\tNegative\tNONE\tNegative\tNormal_N0\t4\t0\tT2C\t3\t3\t6\t160.96\tNO\tNO\t160.96\tNO\tNA\tNA\tNA\tNA\tNA\t4\tNA\r\nPCA0016\tPRIMARY\tNA\tWhite Non Hispanic\t12\t67.36\t3\t3\t6\t12\tT2B\tNA\tNA\tNeoadjuvant HORM\tNA\tRP\tNegative\tNONE\tNegative\tNormal_N0\t2\t0\tT2C\t4\t4\t8\t74.22\tNO\tNO\t74.22\tNO\t99\tNA\tNA\tNA\t97.11015465\t2\tNA\r\nPCA0019\tPRIMARY\tNA\tWhite Non Hispanic\t6.6\t68.12\t3\t4\t7\t6.6\tT1C\tNA\tNA\tNA\tNA\tRP\tNegative\tNONE\tNegative\tNormal_N0\t1\t0\tT2C\t3\t3\t6\t110.33\tBCR_Algorithm\tNO\t123.67\tNO\tNA\tNA\tNA\tNA\t79.85545652\t2\tNA\r\nPCA0023\tPRIMARY\tNA\tBlack Non Hispanic\t4.3\t60.57\t4\t3\t7\t3.88\tT1C\tNA\tNA\tPostHORM\tNA\tRP\tPositive\tNONE\tNegative\tNormal_N0\t2\t0\tT2C\t4\t5\t9\t10.61\tBCR_Algorithm\tNO\t72.84\tDEATH FROM OTHER CANCER\t79.85546\t19.190208\t2.138938\t77.240045\t99\t4\tNA\r\n";
		// transfer our canned preview to our preview object
		preview.put("preview", cannedPreview);
		
		wiki.doPut(layer.getString("preview"), preview, 
				"h3. Put the preview",
				"Then you add/modify the preview and do a PUT.");
				
		
		wiki.doDelete(dataset.getString("uri"), "h2. Delete a Dataset",
				"Note that the request is a DELETE and no content is returned.  Also note that this will delete all of the datasets layers, etc.");
	}
}