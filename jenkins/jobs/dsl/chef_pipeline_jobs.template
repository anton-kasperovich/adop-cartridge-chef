// Folders
def workspaceFolderName = "${WORKSPACE_NAME}"
def projectFolderName = "${PROJECT_NAME}"

// Variables
def logRotatorNum = 10
def logRotatorArtifactNum = 3
def logRotatorDays = -1
def logRotatorArtifactDays = -1

def cookbookGitUrl = "ssh://jenkins@gerrit:29418/${PROJECT_NAME}/" + '${COOKBOOK}'
def cookbookGerritTriggerRegExp = (projectFolderName + '/.*cookbook.*').replaceAll("/", "\\\\/")
def chefUtilsGitUrl = "ssh://jenkins@gerrit:29418/${PROJECT_NAME}/cartridge-chef-scripts"

def chefServerUsername = "${CHEF_SERVER_USERNAME}"
def chefServerValidator = "${CHEF_SERVER_VALIDATOR}"
def chefServerOrganizationUrl = "${CHEF_SERVER_ORGANIZATION_URL}"

// Jobs
def chefGetCookboks = freeStyleJob(projectFolderName + '/Get_Cookbooks')
def chefSanityTest = freeStyleJob(projectFolderName + '/Sanity_Test')
def chefUnitTest = freeStyleJob(projectFolderName + '/Unit_Test')
def chefCodeAnalysis = freeStyleJob(projectFolderName + '/Code_Analysis')
def chefIntegrationTest = freeStyleJob(projectFolderName + '/Integration_Test')
def chefPromoteNonProdChefServer = freeStyleJob(projectFolderName + '/Promote_NonProd_Chef_Server')

// Views
def pipelineView = buildPipelineView(projectFolderName + '/Chef_Pipeline')

pipelineView.with {
    title('Chef Pipeline')
    displayedBuilds(5)
    selectedJob(projectFolderName + '/Get_Cookbooks')
    showPipelineParameters()
    showPipelineDefinitionHeader()
    refreshFrequency(5)
}

chefGetCookboks.with {
    description('This job downloads a cookbook.')
    parameters {
        stringParam('COOKBOOK', 'chef-cookbook-vim', "The name of the cookbook to package i.e. adop-cartridge-chef-reference - this job is invoked by a gerrit ref-updated hook")
    }
    wrappers {
        preBuildCleanup()
        injectPasswords()
        maskPasswords()
        sshAgent('adop-jenkins-master')
    }
    scm {
        git {
            remote {
                url(cookbookGitUrl)
                credentials('adop-jenkins-master')
            }
            branch('*/master')
        }
    }
    logRotator {
        numToKeep(logRotatorNum)
        artifactNumToKeep(logRotatorArtifactNum)
        daysToKeep(logRotatorDays)
        artifactDaysToKeep(logRotatorArtifactDays)
    }
    environmentVariables {
        env('WORKSPACE_NAME',workspaceFolderName)
        env('PROJECT_NAME',projectFolderName)
    }
    triggers {
        gerrit {
            events {
                refUpdated()
            }
            project('reg_exp:'+ cookbookGerritTriggerRegExp, 'plain:master')
            configure { node ->
                node / serverName('ADOP Gerrit')
            }
        }
    }
    label('docker')
    publishers{
        archiveArtifacts('**/*')
        downstreamParameterized {
            trigger(projectFolderName + '/Sanity_Test') {
                condition('UNSTABLE_OR_BETTER')
                parameters {
                    predefinedProp('B', '${BUILD_NUMBER}')
                    predefinedProp('PARENT_BUILD', '${JOB_NAME}')
                }
            }
        }
    }
}

chefSanityTest.with {
    description('This job runs sanity checks on a cookbook.')
    parameters {
        stringParam('B', '', 'Parent build number')
        stringParam('PARENT_BUILD', 'Get_Cookbooks', 'Parent build name')
    }
    logRotator {
        numToKeep(logRotatorNum)
        artifactNumToKeep(logRotatorArtifactNum)
        daysToKeep(logRotatorDays)
        artifactDaysToKeep(logRotatorArtifactDays)
    }
    environmentVariables {
        env('WORKSPACE_NAME',workspaceFolderName)
        env('PROJECT_NAME',projectFolderName)
    }
    scm {
        git {
            remote {
                url(chefUtilsGitUrl)
                credentials('adop-jenkins-master')
            }
            branch('*/master')
        }
    }
    wrappers {
        preBuildCleanup()
        injectPasswords()
        maskPasswords()
        sshAgent('adop-jenkins-master')
    }
    label('docker')
    steps {
        copyArtifacts('Get_Cookbooks') {
            buildSelector {
                buildNumber('${B}')
            }
        }
        shell('''set -x
                |docker run -t --rm -e affinity:container==jenkins-slave \\
                |   -v jenkins_slave_home:/jenkins_slave_home/ \\
                |   iniweb/adop-chef-test-alpine:0.0.1 \\
                |   /jenkins_slave_home/$JOB_NAME/ChefCI/chef_sanity_test.sh /jenkins_slave_home/$JOB_NAME/
                |'''.stripMargin())
    }
    publishers {
        archiveArtifacts('**/*')
        downstreamParameterized {
            trigger(projectFolderName + '/Unit_Test') {
                condition('UNSTABLE_OR_BETTER')
                parameters {
                    predefinedProp('B', '${B}')
                    predefinedProp('UTILS_B', '${BUILD_NUMBER}')
                    predefinedProp('PARENT_BUILD', '${JOB_NAME}')
                }
            }
        }
    }
}

chefUnitTest.with {
    description('This job runs sanity tests of a cookbook.')
    parameters {
        stringParam('B', '', 'Parent build number')
        stringParam('UTILS_B', '', 'Parent utils build number')
        stringParam('PARENT_BUILD', 'Sanity_Test', 'Parent build name')
    }
    logRotator {
        numToKeep(logRotatorNum)
        artifactNumToKeep(logRotatorArtifactNum)
        daysToKeep(logRotatorDays)
        artifactDaysToKeep(logRotatorArtifactDays)
    }
    environmentVariables {
        env('WORKSPACE_NAME',workspaceFolderName)
        env('PROJECT_NAME',projectFolderName)
    }
    wrappers {
        preBuildCleanup()
        injectPasswords()
        maskPasswords()
        sshAgent('adop-jenkins-master')
    }
    label('docker')
    steps {
        copyArtifacts('Sanity_Test') {
            buildSelector {
                buildNumber('${UTILS_B}')
            }
        }
        shell('''set -x
                |docker run -t --rm -e affinity:container==jenkins-slave \\
                |   -v jenkins_slave_home:/jenkins_slave_home/ \\
                |   iniweb/adop-chef-test-alpine:0.0.1 \\
                |   /jenkins_slave_home/$JOB_NAME/ChefCI/chef_unit_test.sh /jenkins_slave_home/$JOB_NAME/
                |'''.stripMargin())
    }
    publishers {
        downstreamParameterized {
            trigger(projectFolderName + '/Code_Analysis') {
                condition('UNSTABLE_OR_BETTER')
                parameters {
                    predefinedProp('B', '${B}')
                    predefinedProp('UTILS_B', '${UTILS_B}')
                    predefinedProp('PARENT_BUILD', '${JOB_NAME}')
                }
            }
        }
    }
}

chefCodeAnalysis.with {
    description('This job runs code analysis of a cookbook.')
    parameters {
        stringParam('B', '', 'Parent build number')
        stringParam('UTILS_B', '', 'Parent utils build number')
        stringParam('PARENT_BUILD', 'Unit_Test', 'Parent build name')
    }
    logRotator {
        numToKeep(logRotatorNum)
        artifactNumToKeep(logRotatorArtifactNum)
        daysToKeep(logRotatorDays)
        artifactDaysToKeep(logRotatorArtifactDays)
    }
    environmentVariables {
        env('WORKSPACE_NAME',workspaceFolderName)
        env('PROJECT_NAME',projectFolderName)
    }
    wrappers {
        preBuildCleanup()
        injectPasswords()
        maskPasswords()
        sshAgent('adop-jenkins-master')
    }
    label('docker')
    steps {
        copyArtifacts('Sanity_Test') {
            buildSelector {
                buildNumber('${UTILS_B}')
                includePatterns('ChefCI/**')
            }
        }
        copyArtifacts('Get_Cookbooks') {
            buildSelector {
                buildNumber('${B}')
                includePatterns('**')
                targetDirectory('cookbook')
            }
        }
        shell('''set -x
                |docker run -t --rm -e affinity:container==jenkins-slave \\
                |   -v jenkins_slave_home:/jenkins_slave_home/ \\
                |   iniweb/adop-chef-test-alpine:0.0.1 \\
                |   /jenkins_slave_home/$JOB_NAME/ChefCI/chef_code_analysis.sh /jenkins_slave_home/$JOB_NAME/cookbook
                |'''.stripMargin())
    }
    publishers {
        downstreamParameterized {
            trigger(projectFolderName + '/Integration_Test') {
                condition('UNSTABLE_OR_BETTER')
                parameters {
                    predefinedProp('B', '${B}')
                    predefinedProp('UTILS_B', '${UTILS_B}')
                    predefinedProp('PARENT_BUILD', '${JOB_NAME}')
                }
            }
        }
    }
}

chefIntegrationTest.with {
    description('This job runs integration tests with a cookbook')
    parameters {
        stringParam('B', '', 'Parent build number')
        stringParam('UTILS_B', '', 'Parent utils build number')
        stringParam('PARENT_BUILD', 'Unit_Test', 'Parent build name')
    }
    wrappers {
        preBuildCleanup()
        injectPasswords()
        maskPasswords()
        sshAgent('adop-jenkins-master')
    }
    logRotator {
        numToKeep(logRotatorNum)
        artifactNumToKeep(logRotatorArtifactNum)
        daysToKeep(logRotatorDays)
        artifactDaysToKeep(logRotatorArtifactDays)
    }
    environmentVariables {
        env('WORKSPACE_NAME',workspaceFolderName)
        env('PROJECT_NAME',projectFolderName)
    }
    label('docker')
    steps {
        copyArtifacts('Sanity_Test') {
            buildSelector {
                buildNumber('${UTILS_B}')
            }
        }
        shell('''set +e
                |
                |KITCHEN_FILE=.kitchen.docker.yml
                |if [ ! -f $KITCHEN_FILE ]; then
                |    KITCHEN_FILE=.kitchen.yml
                |fi
                |
                |docker run -t -P --net=host --rm \\
                |   -e affinity:container==jenkins-slave \\
                |   -v /var/run/docker.sock:/var/run/docker.sock \\
                |   -v jenkins_slave_home:/jenkins_slave_home/ \\
                |   -w="/jenkins_slave_home/${JOB_NAME}" \\
                |   iniweb/adop-chef-test-alpine:0.0.1 \\
                |   bash -c "KITCHEN_LOCAL_YAML=${KITCHEN_FILE} kitchen test -d never"
                |
                |EXIT_CODE=$?
                |set -e
                |
                |docker run --rm \\
                |   -e affinity:container==jenkins-slave \\
                |   -v /var/run/docker.sock:/var/run/docker.sock \\
                |   -v jenkins_slave_home:/jenkins_slave_home/ \\
                |   -w="/jenkins_slave_home/${JOB_NAME}" \\
                |   iniweb/adop-chef-test-alpine:0.0.1 \\
                |   bash -c "KITCHEN_LOCAL_YAML=${KITCHEN_FILE} kitchen destroy"
                |
                |exit ${EXIT_CODE}
                |
                |'''.stripMargin())
    }
    publishers {
        downstreamParameterized {
            trigger(projectFolderName + '/Promote_NonProd_Chef_Server') {
                parameters {
                    predefinedProp('B', '${B}')
                    predefinedProp('UTILS_B', '${UTILS_B}')
                    predefinedProp('PARENT_BUILD', '${JOB_NAME}')
                }
            }
        }
    }
}

chefPromoteNonProdChefServer.with {
    description('This job uploads a cookbook to a non-production Chef Server')
    parameters {
        stringParam('B', '', 'Parent build number')
        stringParam('UTILS_B', '', 'Parent utils build number')
        stringParam('PARENT_BUILD', 'Integration_Test', 'Parent build name')
    }
    wrappers {
        preBuildCleanup()
        injectPasswords()
        maskPasswords()
        sshAgent('adop-jenkins-master')
    }
    logRotator {
        numToKeep(logRotatorNum)
        artifactNumToKeep(logRotatorArtifactNum)
        daysToKeep(logRotatorDays)
        artifactDaysToKeep(logRotatorArtifactDays)
    }
    environmentVariables {
        env('WORKSPACE_NAME',workspaceFolderName)
        env('PROJECT_NAME',projectFolderName)
        env('CHEF_SERVER_URL',chefServerOrganizationUrl)
        env('CHEF_SERVER_USERNAME',chefServerUsername)
        env('CHEF_SERVER_VALIDATOR',chefServerValidator)
    }
    label('docker')
    steps {
        copyArtifacts('Sanity_Test') {
            buildSelector {
                buildNumber('${UTILS_B}')
                includePatterns('ChefCI/**')
            }
        }
        copyArtifacts('Get_Cookbooks') {
            buildSelector {
                buildNumber('${B}')
                includePatterns('**')
                targetDirectory('ChefCI/cookbooks/cookbook')
            }
        }
        systemGroovyCommand('''
                |import hudson.model.*
                |import jenkins.model.*
                |import com.cloudbees.plugins.credentials.*
                |import com.cloudbees.plugins.credentials.common.*
                |import com.cloudbees.plugins.credentials.domains.*
                |
                |private findCredentialsById(String cId) {
                |  def username_matcher = CredentialsMatchers.withId(cId)
                |  def available_credentials = CredentialsProvider.lookupCredentials(
                |    StandardUsernameCredentials.class,
                |    Jenkins.getInstance(),
                |    hudson.security.ACL.SYSTEM,
                |    new SchemeRequirement("ssh")
                |  )
                |
                |  return CredentialsMatchers.firstOrNull(available_credentials, username_matcher)
                |}
                |
                |envVars = []
                |
                |user = findCredentialsById(build.getEnvironment(listener).get('CHEF_SERVER_USERNAME'))
                |if (user) {
                |  envVars.add(new StringParameterValue("USERNAME", user.username))
                |  build.workspace.child("ChefCI/.chef/${user.username}.pem").write(user.privateKey, "UTF-8")
                |}
                |
                |validator = findCredentialsById(build.getEnvironment(listener).get('CHEF_SERVER_VALIDATOR'))
                |if (validator) {
                |  envVars.add(new StringParameterValue("VALIDATOR", validator.username))
                |  build.workspace.child("ChefCI/.chef/${validator.username}.pem").write(validator.privateKey, "UTF-8")
                |}
                |
                |Thread.currentThread().executable.addAction(new ParametersAction(envVars))
               '''.stripMargin())
        shell('''set +e
                |
                |docker run -t --rm -e affinity:container==jenkins-slave \\
                |-e "CHEF_SERVER_URL=${CHEF_SERVER_URL}" \\
                |-e "USERNAME=${USERNAME}" \\
                |-e "VALIDATOR=${VALIDATOR}" \\
                |-v jenkins_slave_home:/jenkins_slave_home/ \\
                |iniweb/adop-chef-test-alpine:0.0.1 \\
                |/jenkins_slave_home/$JOB_NAME/ChefCI/chef_berks_upload.sh /jenkins_slave_home/$JOB_NAME/ChefCI
                |'''.stripMargin())
    }

}
