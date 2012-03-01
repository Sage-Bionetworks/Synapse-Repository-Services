package org.sagebionetworks.workflow.curation;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.flow.ActivityWorker;


/**
 * @author deflaux
 *
 */
public class TcgaWorkflowActivityService {

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        AmazonSimpleWorkflow swfService = TcgaWorkflowConfigHelper.getSWFClient();
        String domain = TcgaWorkflowConfigHelper.getStack();

        final ActivityWorker worker = new ActivityWorker(swfService, domain, TcgaActivities.ACTIVITIES_TASK_LIST);

        // Create activity implementations
        TcgaActivities tcgaActivitiesImpl = new TcgaActivitiesImpl();
        worker.addActivitiesImplementation(tcgaActivitiesImpl);

        worker.start();

        System.out.println("Activity Worker Started for Task List: " + worker.getTaskListToPoll());

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
			public void run() {
                try {
                    worker.shutdownAndAwaitTermination(10, TimeUnit.MINUTES);
                    System.out.println("Activity Worker Exited.");
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        System.out.println("Please press any key to terminate service.");

        try {
            System.in.read();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
