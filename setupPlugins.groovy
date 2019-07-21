@Grab(group='com.cdancy', module='jenkins-rest', version='0.0.19')
@Grab(group='org.apache.jclouds.driver', module='jclouds-slf4j', version='2.1.2')
@Grab(group='org.slf4j', module='slf4j-log4j12', version='1.7.25')

import com.cdancy.jenkins.rest.JenkinsClient
import com.cdancy.jenkins.rest.domain.common.RequestStatus
import com.cdancy.jenkins.rest.domain.plugins.Plugins

import org.jclouds.logging.slf4j.config.SLF4JLoggingModule

JenkinsClient client = JenkinsClient.builder()
//    .modules(new SLF4JLoggingModule())
    .build()

def pm = client.api().pluginManagerApi()

String logFile = this.args[0]
for (String arg in this.args[1..-1]) {
    def (String name, String version) = arg.split('@')
    RequestStatus status = pm.installNecessaryPlugins(arg)
    if (status.errors()) throw new RuntimeException("Error installing ${args}: ${status.errors()}")

    Plugins plugins = pm.plugins(3, null)
    if (plugins.errors()) throw new RuntimeException("Error querying plugins: ${plugins.errors()}")

    // REST API silently fails, we have to look at the log
    new File(logFile).eachLine { line ->
        // Workaround https://issues.jenkins-ci.org/browse/JENKINS-58581
        if (line.contains("WARNING: No such plugin ${name} to upgrade")) {
            throw new RuntimeException("Version ${version} of plugin ${name} is invalid")
        }
        // Workaround https://issues.jenkins-ci.org/browse/JENKINS-58584
        if (line.contains("WARNING: No such plugin ${name} to install")) {
            throw new RuntimeException("Plugin name ${name} is invalid")
        }
    }

    while (!plugins.plugins().find { it.shortName() == name }) {
        sleep(5000)
        plugins = pm.plugins(3, null)
        if (plugins.errors()) throw new RuntimeException("Error querying plugins: ${plugins.errors()}")
    }
}
