#!/bin/bash

set -o xtrace
set -e

function waitForJenkinsReady {
    local httpCode
    set +e
    # curl -s: silent
    # curl -w: write out the http_code
    http_code=$(curl -s -w "%{http_code}" -o /dev/null -X GET "$JENKINS_REST_ENDPOINT")
    while [[ $http_code != "200" ]]; do
        sleep 5
        http_code=$(curl -s -w "%{http_code}" -o /dev/null -X GET "$JENKINS_REST_ENDPOINT")
    done
    set -e
    echo "Jenkins is ready"
}

export JENKINS_COMMON_NAME=$(uname -n)
export JENKINS_REST_ENDPOINT=http://${JENKINS_COMMON_NAME}:8080

# Use latest unless otherwise specified by the user
${JENKINS_VERSION:=2.176.2}
JENKINS_DOWNLOAD_LINK=http://mirrors.jenkins.io/war-stable/${JENKINS_VERSION}/jenkins.war

echo "Downloading Jenkins."
if [[ ! -r "jenkins-${JENKINS_VERSION}.war" ]]; then
    # curl -L: follow redirection
    curl -L -X GET "${JENKINS_DOWNLOAD_LINK}" -o jenkins-${JENKINS_VERSION}.war
fi
if [[ ! -r "jenkins-${JENKINS_VERSION}.war.sha256" ]]; then
    curl -L -X GET "${JENKINS_DOWNLOAD_LINK}.sha256" -o jenkins-${JENKINS_VERSION}.war.sha256
fi
sha256sum jenkins-${JENKINS_VERSION}.war

export JENKINS_HOME=$(pwd)/jenkins_installation

trap 'cleanup' EXIT
function cleanup {
    kill $jenkinsPid
}

logfile=jenkins-${JENKINS_COMMON_NAME}.log
echo "Starting Jenkins."
rm -rf logfile
java \
    -Djenkins.install.runSetupWizard=false \
    -jar jenkins-${JENKINS_VERSION}.war \
    >$logfile 2>&1 & jenkinsPid=$!
echo "Jenkins running with pid $jenkinsPid"
waitForJenkinsReady

echo "Install plugins."
groovy setupPlugins \
    "$logfile" \
    "cloudbees-folder@1.0" \
    "extended-read-permission@1.0" \
    "git@1.0" \
    "greenballs@1.0" \
    "run-selector@1.0" \
    "workflow-aggregator@1.0" \
    "configuration-as-code@1.0" \
    "external-workspace-manager@1.0"

echo "Restarting."
curl -X POST "${JENKINS_REST_ENDPOINT}"/restart
waitForJenkinsReady

# Post configuration
echo "Configuring Jenkins"
export SomeUserId=martin
export SomeUserName=Martin
export SomeUserPassword=1234
# curl -i: include the http header in the output
http_code=$(curl -s -w "%{http_code}" -o /dev/null -X POST -T jenkins.yaml "$JENKINS_REST_ENDPOINT/configuration-as-code/check")
if [[ $http_code != "200" ]]; then
    exit 1
fi
http_code=$(curl -s -w "%{http_code}" -o /dev/null -X POST -T jenkins.yaml "$JENKINS_REST_ENDPOINT/configuration-as-code/apply")
if [[ $http_code != "200" ]]; then
    exit 1
fi
if grep -i error $logfile; then
    exit 1
fi
echo "Jenkins is ready"

# After configuration as code, we need to authenticate,
# export credentials to the jenkins-rest JClouds library
export JENKINS_REST_CREDENTIALS=$SomeUserId:$SomeUserPassword
groovy createJob "folder1/folder2/myjob"
groovy runJob "folder1/folder2/myjob"
