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
                            try {
                                TestWithFlaky()
                                setBuildStatus("checker-1", "Checker1", "SUCCESS")
                            } catch (error) {
                                setBuildStatus("checker-1", "Checker1", "FAILURE")
                                input "Retry the job?"
                                false
                            }
                        }
                    }
                }
                stage('Checker2') {
                    steps {
                        waitUntil {
                            setBuildStatus("checker-2", "Checker2", "PENDING")
                            try {
                                TestWithFlaky()
                                setBuildStatus("checker-2", "Checker2", "SUCCESS")
                            } catch (error) {
                                setBuildStatus("checker-2", "Checker2", "FAILURE")
                                input "Retry the job?"
                                false
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
    
    sh '''
    '''   
}

def TestWithFlaky() {
    sh '''
    set -ex
    num=$(( ( RANDOM % 5 ) ))
    echo $num

    exit $num
    '''
}

void setBuildStatus(String context, String message, String state) {
  step([
      $class: "GitHubCommitStatusSetter",
      reposSource: [$class: "ManuallyEnteredRepositorySource", url: env.GIT_URL],
      contextSource: [$class: "ManuallyEnteredCommitContextSource", context: context],
      errorHandlers: [[$class: "ChangingBuildStatusErrorHandler", result: "UNSTABLE"]],
      statusResultSource: [ $class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: message, state: state]] ]
  ]);
}
