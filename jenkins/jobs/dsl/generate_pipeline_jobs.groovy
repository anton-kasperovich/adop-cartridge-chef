// Folders
def workspaceFolderName = "${WORKSPACE_NAME}"
def projectFolderName = "${PROJECT_NAME}"

// Jobs reference
def generateChefPipelineJobs = freeStyleJob(projectFolderName + "/Generate_Chef_Pipeline_Jobs")

generateChefPipelineJobs.with {
    parameters {
        stringParam('CHEF_SERVER_ORGANIZATION_URL','','Chef Server Organization URL i.e. https://<chef-server-ip>/organizations/<org-name>')
        credentialsParam('CHEF_SERVER_USERNAME') {
            type('com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey')
            required()
            description('Chef Server username. SSH Username with private key.')
        }
        credentialsParam('CHEF_SERVER_VALIDATOR') {
            type('com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey')
            required()
            description('Chef Server validator. SSH Username with private key.')
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
                |echo "CHEF_SERVER_ORGANIZATION_URL=$CHEF_SERVER_ORGANIZATION_URL" > env.properties
                |echo "CHEF_SERVER_USERNAME=$CHEF_SERVER_USERNAME" >> env.properties
                |echo "CHEF_SERVER_VALIDATOR=$CHEF_SERVER_VALIDATOR" >> env.properties
                |
                |set -x
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
