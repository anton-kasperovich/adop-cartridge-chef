// Folders
def workspaceFolderName = "${WORKSPACE_NAME}"
def projectFolderName = "${PROJECT_NAME}"

// Jobs reference
def generateChefPipelineJobs = freeStyleJob(projectFolderName + "/Generate_Chef_Pipeline_Jobs")

generateChefPipelineJobs.with {
    description('''This "Seed job" generates Chef cartridge Jenkins jobs and a pipeline view.
It is not necessary to specify values for this job parameters, you are free to use this cartridge just as "Quality Gate" for you cookbooks, without upload part (last step of the pipeline)
    ''')
    parameters {
        stringParam('CHEF_SERVER_ORGANIZATION_URL','','Chef Server Organization URL i.e. https://<chef-server-ip>/organizations/<org-name>')
        credentialsParam('CHEF_SERVER_SSH_USERNAME') {
            type('com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey')
            description('Chef Server username. SSH Username with private key.')
        }
        credentialsParam('CHEF_SERVER_SSH_VALIDATOR') {
            type('com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey')
            description('Chef Server validator. SSH Validator username with private key.')
        }
    }
    environmentVariables {
        env('WORKSPACE_NAME', workspaceFolderName)
        env('PROJECT_NAME', projectFolderName)
    }
    wrappers {
        preBuildCleanup()
        injectPasswords()
        maskPasswords()
        sshAgent('adop-jenkins-master')
    }
    steps {
        shell('''set +x
                |
                |echo "CHEF_SERVER_SSH_USERNAME_ID=$CHEF_SERVER_SSH_USERNAME" > env.properties
                |echo "CHEF_SERVER_SSH_VALIDATOR_ID=$CHEF_SERVER_SSH_VALIDATOR" >> env.properties
                |
                |set -x
                |
                '''.stripMargin())
        environmentVariables {
            propertiesFile('env.properties')
        }
    }
    steps {
        systemGroovyCommand('''
                |import hudson.model.*
                |import jenkins.model.*
                |import hudson.FilePath
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
                |
                |user = findCredentialsById(build.getEnvironment(listener).get('CHEF_SERVER_SSH_USERNAME_ID'))
                |if (user) {
                |  build.workspace.child("ChefCI/.chef/${user.username}.pem").write(user.privateKey, "UTF-8")
                |}
                |
                |validator = findCredentialsById(build.getEnvironment(listener).get('CHEF_SERVER_SSH_VALIDATOR_ID'))
                |if (validator) {
                |  build.workspace.child("ChefCI/.chef/${validator.username}.pem").write(validator.privateKey, "UTF-8")
                |}
                |
                |serverUrl = build.getEnvironment(listener).get('CHEF_SERVER_ORGANIZATION_URL')
                |if (user && validator) {
                |  build.workspace.child("env.properties").write("CHEF_SERVER_ORGANIZATION_URL=${serverUrl}\\nCHEF_SERVER_USERNAME=${user.username}\\nCHEF_SERVER_VALIDATOR=${validator.username}\\n", "UTF-8")
                |}
                |
               '''.stripMargin())
        environmentVariables {
            propertiesFile('env.properties')
        }
        dsl {
            text(readFileFromWorkspace('cartridge/jenkins/jobs/dsl/chef_pipeline_jobs.template'))
        }
    }
}
