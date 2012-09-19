package org.sagebionetworks.search.controller;

import java.util.List;

import org.sagebionetworks.asynchronous.workers.swf.Task;
import org.sagebionetworks.asynchronous.workers.swf.WorkFlow;
import org.sagebionetworks.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class ConsoleController extends BaseController{

	
	@Autowired
	SearchService searchService;
	
//	/**
//	 * 
//	 * @return
//	 */
//	@ResponseStatus(HttpStatus.OK)
//	@RequestMapping(value = { "/console" }, method = RequestMethod.GET)
//	@ResponseBody
//	public String getConsole(){
//		StringBuilder builder = new StringBuilder();
//		builder.append("<html><body>");
//		// The work flows
//		builder.append("<h1>Registered workflows:</h1>");
//		List<WorkFlow> list = swfRegister.getWorkFlowList();
//		writeWorkFlowsToHTML(list, builder);
//		// The task list
//		builder.append("<h1>Listening for Tasks:</h1>");
//		writeTaskListToHTML(swfRegister.getTaskList(), builder);
//		builder.append("</body></html>");
//		return builder.toString();
//	}
	
	/**
	 * Create a table of the work flows.
	 * @param list
	 * @param builder
	 */
	public void writeWorkFlowsToHTML(List<WorkFlow> list, StringBuilder builder){
		builder.append("<table border=\"1\">");
		writeHeaders(new String[] {"Name", "Version"}, builder);
		for(WorkFlow wf: list){
			builder.append("<tr>");
			writeTableCell(wf.getType().getName(), builder);
			writeTableCell(wf.getType().getVersion(), builder);
			builder.append("</tr>");
		}
		builder.append("</table>");
	}
	
	/**
	 * Create a table of the tasks
	 * @param list
	 * @param builder
	 */
	public void writeTaskListToHTML(List<Task> list, StringBuilder builder){
		builder.append("<table border=\"1\">");
		writeHeaders(new String[] {"Domain Name", "Name", "Type" }, builder);
		for(Task task: list){
			builder.append("<tr>");
			writeTableCell(task.getDomainName(), builder);
			writeTableCell(task.getTaskList().getName(), builder);
			writeTableCell(task.getClass().getName(), builder);
			builder.append("</tr>");
		}
		builder.append("</table>");
	}
	
	/**
	 * Helper for a cell
	 * @param cell
	 * @param builder
	 */
	public void writeTableCell(String cell, StringBuilder builder){
		builder.append("<td>");
		builder.append(cell);
		builder.append("</td>");
	}
	
	/**
	 * Helper for writing the headers.
	 * @param headers
	 * @param builder
	 */
	public void writeHeaders(String[] headers, StringBuilder builder){
		for(String header: headers){
			builder.append("<th>");
			builder.append(header);
			builder.append("</th>");
		}
	}
	
}
