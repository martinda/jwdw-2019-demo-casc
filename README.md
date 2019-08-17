# Demo of Jenkins installed without the GUI

This is an example of how to setup Jenkins without ever touching the GUI.

Jenkins World Devops World 2019 [presentation link](https://drive.google.com/file/d/1sahuY3AHs7NjAi8EgPECKD8JueCJLUuh/view?usp=sharing).

## Requirements

* Bash
* Java
* Groovy
* Curl

Jenkins will be donwloaded automatically. Supports LTS versions only. Edit the `setup-jenkins.sh` script to support other versions.

## How to run this demo

The demo is configured to use Jenkins LTS 2.176.2.

```
./setup-jenkins
```

This will in order:

* Download Jenkins, verify the checksum
* Start Jenkins
* Setup plugins
* Restart Jenkins
* Configure Jenkins using the [JCasC](https://github.com/jenkinsci/configuration-as-code-plugin) plugin
* Restart Jenkins
* Configure a job
* Run the job

## In this repository

The files at a glance:

| File | Purpose |
|------|---------|
| setup-jenkins.sh | This is the main script |
| jenkins.yaml | The configuration as code data file to upload to Jenkins to configure it |
| setupPlugins.groovy | A groovy script to setup plugins using the Jenkins REST API |
| folder.xml | The xml file to create folders in Jenkins |
| myjob.xml | The xml file to create a job (aka project) in Jenkins |
| createJob.groovy | A groovy script to create a job in Jenkins |
| runJob.groovy | A groovy script to run a job in Jenkins |
| log4j.properties | A log configuration file to debug HTTP transactions between the groovy script and Jenkins |

## Additional information

The groovy scripts talk to Jenkins using the [jenkins-rest](https://github.com/cdancy/jenkins-rest) library.
This library needs to know the Jenkins URL and the user credentials to be able to communicate with Jenkins.
Those are provided by the `JENKINS_REST_ENDOPINT` and the `JENKINS_REST_CREDENTIALS` environment variables that are found in the `setup-script.sh`.

Sometimes you need to see the HTTP packets to debug. With curl, you can add `-v`.
For the groovy scripts, you need to activate logging, as it is disabled by default.
Look for the `SLF4JLoggingModule` in the `setupPlugins.groovy` script, and un-comment it out.
To control the logging verbosity, configure the `log4j.properties` file, and export `JAVA_OPTS` like so:

```
export JAVA_OPTS=-Dlog4j.configuration=file:log4j.properties
```

For more on this, see the [jenkins-rest wiki](https://github.com/cdancy/jenkins-rest/wiki).

