package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.EntityGroup;
import org.sagebionetworks.repo.model.EntityGroupRecord;
import org.sagebionetworks.repo.model.Summary;

public class SummaryMarkdownUtils {

	/**
	 * Build wiki markdown for a summary object.
	 * @param summary
	 * @return
	 */
	public static String generateSummaryMarkdown(Summary summary){
		StringBuilder builder = new StringBuilder();
		if(summary.getGroups() != null){
			for(EntityGroup group: summary.getGroups()){
				generateGroupMarkdown(group, builder);
			}
		}
		return builder.toString();
	}
	
	/**
	 * Build wiki markdown for a group object.
	 * @param group
	 * @param builder
	 */
	public static void generateGroupMarkdown(EntityGroup group, StringBuilder builder){
		if(group != null){
			builder.append("\n");
			if(group.getName() != null){
				builder.append("### ").append(group.getName()).append("\n");
			}
			if(group.getDescription() != null){
				builder.append(group.getDescription()).append("\n");
			}
			if(group.getRecords() != null){
				builder.append("${entitylist?list=");
				for(int i=0; i<group.getRecords().size(); i++){
					EntityGroupRecord record = group.getRecords().get(i);
					generateRecordMarkdown(i, record, builder);
				}
				builder.append("}");
			}
		}
	}
	
	/**
	 * Generate the markdown for a record.
	 * @param record
	 * @param builder
	 */
	public static void generateRecordMarkdown(int index, EntityGroupRecord record, StringBuilder builder){
		if(index > 0){
			builder.append("%3B");
		}
		builder.append(record.getEntityReference().getTargetId());
		if(record.getEntityReference().getTargetVersionNumber() != null){
			builder.append("%2Fversion%2F").append(record.getEntityReference().getTargetVersionNumber());
		}
		if(record.getNote() != null){
			builder.append("%2C").append(record.getNote());
		}
	}
}
