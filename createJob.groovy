@Grab(group='com.cdancy', module='jenkins-rest', version='0.0.19')

import com.cdancy.jenkins.rest.JenkinsClient
import com.cdancy.jenkins.rest.domain.common.Error
import com.cdancy.jenkins.rest.domain.common.IntegerResponse
import com.cdancy.jenkins.rest.domain.common.RequestStatus
import com.cdancy.jenkins.rest.domain.job.BuildInfo
import com.cdancy.jenkins.rest.domain.queue.QueueItem

void dealWithErrors(String msg, List<Error> errors) {
    boolean abort = false
    if (errors.size() > 0) {
        for (Error error : errors) {
            if (error.exceptionName() == "org.jclouds.rest.ResourceAlreadyExistsException")
                continue
            System.err.println("Exception: " + error.exceptionName())
            abort = true
        }
        if (abort) throw new RuntimeException(msg)
    }
}

// Create a client instance
class Global {
    static JenkinsClient client = JenkinsClient.builder().build()
    static String folderConfig = new File("folder.xml").text
}

public void createFolders(List<String> folders) {
    // Create the nested folders
    List<String> previousFolders = []
    for (String folder in folders) {
        println("Creating ${folder}")
        RequestStatus status = Global.client.api().jobsApi().create(
            previousFolders ? previousFolders.join('/') : null,
            folder,
            Global.folderConfig)
        dealWithErrors("Unable to create folder", status.errors())
        println("Folder ${folder} created successfully")
        previousFolders.add(folder)
    }
}

for (String arg in this.args) {
    List<String> jobPath = arg.split('/')
    println(jobPath)
    String jobName = jobPath[-1]
    List<String> folders = jobPath[0..-2]
    createFolders(folders)

    String jobConfig = new File("${jobName}.xml").text

    // Create a job
    // TODO: Deal with update (when the jobs already exists we mask the exception)
    RequestStatus status = Global.client.api().jobsApi().create(
        folders ? folders.join('/') : null,
        jobName,
        jobConfig)
    dealWithErrors("Unable to create folder", status.errors())
    println("Job ${arg} successfuly created")
}

