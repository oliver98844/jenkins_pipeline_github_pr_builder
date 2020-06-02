pipeline {
    agent { label "master" }
    
    options {
        timestamps()
        buildDiscarder(logRotator(daysToKeepStr: '7', artifactNumToKeepStr: '30'))
    }

    environment {
        OUTPUT_DIR = 'output/'
    }

    stages {
        stage('Init') {
            steps {
                CheckoutSCM()
            }
        }
        stage('Checkers') {
            parallel {
                stage('Checker1') {
                    steps {
                        waitUntil {
                            setBuildStatus("checker-1", "Checker1", "PENDING")
                            script {
                                if (TestWithFlaky()) {
                                    setBuildStatus("checker-1", "Checker1", "SUCCESS")
                                    return true
                                }
                                setBuildStatus("checker-1", "Checker1", "FAILURE")
                                input "Retry the job?"
                                return false
                            }
                        }
                    }
                }
                stage('Checker2') {
                    steps {
                        waitUntil {
                            setBuildStatus("checker-2", "Checker2", "PENDING")
                            script {
                                if (TestWithFlaky()) {
                                    setBuildStatus("checker-2", "Checker2", "SUCCESS")
                                    return true
                                }
                                setBuildStatus("checker-2", "Checker2", "FAILURE")
                                input "Retry the job?"
                                return false
                            }
                        }
                    }
                }
            }
        }
    }
}

def CheckoutSCM() {
    // clean previous build
    dir(OUTPUT_DIR) {
        deleteDir()
    }
    
    sh '''#!/bin/bash -l
    set -ex
    echo ${sha1}

    ls -l
    '''   
}

def TestWithFlaky() {
    def result = sh returnStatus: true, script: '''#!/bin/bash -l
    set -ex
    num=$(( ( RANDOM % 5 ) ))
    echo $num

    sleep $(( ( RANDOM % 10 ) + 1 ))

    exit $num
    '''
    return result == 0
}

void setBuildStatus(String context, String message, String state) {
  step([
      $class: "GitHubCommitStatusSetter",
      contextSource: [$class: "ManuallyEnteredCommitContextSource", context: context],
      statusResultSource: [ $class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: message, state: state]] ]
  ]);
}
