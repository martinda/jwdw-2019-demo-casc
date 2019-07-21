@Grab(group='com.cdancy', module='jenkins-rest', version='0.0.19')

import com.cdancy.jenkins.rest.JenkinsClient
import com.cdancy.jenkins.rest.domain.common.Error
import com.cdancy.jenkins.rest.domain.common.IntegerResponse
import com.cdancy.jenkins.rest.domain.common.RequestStatus
import com.cdancy.jenkins.rest.domain.job.BuildInfo
import com.cdancy.jenkins.rest.domain.queue.QueueItem

void dealWithErrors(String msg, List<Error> errors) {
    if (errors.size() > 0) {
        for (Error error : errors) {
            System.err.println("Exception: " + error.exceptionName())
        }
        throw new RuntimeException(msg)
    }
}

// Process command line arguments
List<String> jobPath = this.args[0].split('/')
String job = jobPath[-1]
List<String> folders = jobPath[0..-2]
String folderString = folders ? folders.join('/') : null

// Create a client instance
JenkinsClient client = JenkinsClient.builder().build()

// Submit a build
IntegerResponse queueId = client.api().jobsApi().build(folderString, job)
dealWithErrors("Unable to submit build", queueId.errors())
println("Build successfuly submitted with queue id: " + queueId.value())

// Poll the Queue, check for the queue item status
QueueItem queueItem = client.api().queueApi().queueItem(queueId.value())
while (true) {
    if (queueItem.cancelled()) {
        throw new RuntimeException("Queue item cancelled")
    }

    if (queueItem.executable()) {
        println("Build is executing with build number: " + queueItem.executable().number())
        break
    }

    Thread.sleep(2000)
    queueItem = client.api().queueApi().queueItem(queueId.value())
}

// Get the build info of the queue item being built and poll until it is done
BuildInfo buildInfo = client.api().jobsApi().buildInfo(folderString, job, queueItem.executable().number())
while (buildInfo.result() == null) {
    Thread.sleep(2000)
    buildInfo = client.api().jobsApi().buildInfo(folderString, job, queueItem.executable().number())
}
println("Build status: " + buildInfo.result())
if (buildInfo.result() != "SUCCESS") {
    throw new RuntimeException("Job ${this.args[0]} is not successful.")
}
