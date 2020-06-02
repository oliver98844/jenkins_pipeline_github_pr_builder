pipeline {
    agent { label "master" }
    
    options {
        timestamps()
        buildDiscarder(logRotator(daysToKeepStr: '7', artifactNumToKeepStr: '30'))
    }

    environment {
        OUTPUT_DIR = 'output/'
        GIT_SHA = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%h'").trim()
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
                        setBuildStatus("checker-1", "Checker1", "PENDING", GIT_SHA)
                        script {
                            if (TestWithFlaky()) {
                                setBuildStatus("checker-1", "Checker1", "SUCCESS", GIT_SHA)
                                return true
                            }
                            setBuildStatus("checker-1", "Checker1", "FAILURE", GIT_SHA)
                            error "Checker1 failed"
                        }
                    }
                }
                stage('Checker2') {
                    steps {
                        setBuildStatus("checker-2", "Checker2", "PENDING", GIT_SHA)
                        script {
                            if (TestWithFlaky()) {
                                setBuildStatus("checker-2", "Checker2", "SUCCESS", GIT_SHA)
                                return true
                            }
                            setBuildStatus("checker-2", "Checker2", "FAILURE", GIT_SHA)
                            error "Checker2 failed"
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

    sleep $(( num + 1 ))

    exit $num
    '''
    return result == 0
}

void setBuildStatus(String context, String message, String state, String sha) {
  step([
      $class: "GitHubCommitStatusSetter",
      reposSource: [$class: "ManuallyEnteredRepositorySource", url: "https://github.com/oliver98844/jenkins_pipeline_github_pr_builder.git"],
      contextSource: [$class: "ManuallyEnteredCommitContextSource", context: context],
      commitShaSource: [$class: "ManuallyEnteredShaSource", sha: sha ],
      statusBackrefSource: [$class: "ManuallyEnteredBackrefSource", backref: "${BUILD_URL}"],
      statusResultSource: [ $class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: message, state: state]] ]
  ]);
}
