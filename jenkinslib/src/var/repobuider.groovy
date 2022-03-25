
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import com.cloudbees.plugins.credentials.CredentialsProvider
import hudson.util.Secret
import hudson.plugins.git.*
import hudson.model.BuildAuthorizationToken
import org.apache.commons.lang.RandomStringUtils
import hudson.tasks.Mailer
import com.cloudbees.hudson.plugins.folder.Folder
import hudson.scm.SCM
import hudson.model.ListView
import hudson.views.ListViewColumn
import hudson.triggers.SCMTrigger
import hudson.*
import hudson.security.*
import java.util.*
    
def createNewJenkinsJobWithMultiBranch(String projectsFolder, String projectName, String destProject, String destGit, String githubid) {
    // Get GIT Creds and URL, need to replace everything from /scm/.* and https
    scmCredsID = scm.getUserRemoteConfigs()[0].getCredentialsId()
    scmUrl = scm.getUserRemoteConfigs()[0].getUrl()
    gitServerHost = scmUrl.replaceAll("http://", "").replaceAll("https://", "").replaceAll("/scm/.*", "")
    StandardUsernamePasswordCredentials creds = CredentialsProvider.findCredentialById("${scmCredsID}",
            StandardUsernamePasswordCredentials.class, currentBuild.rawBuild)
    def gitUser = creds.getUsername()

    jobDsl additionalParameters: [
        projectsFolder: projectsFolder,
        projectName: projectName,g
        destProject: destProject,
        destGit: destGit,
        gitUserUri: gitUser.replace("@", "%40"),
        gitServerHost: gitServerHost,
        scmCredsID: scmCredsID
    ], scriptText: '''
    multibranchPipelineJob("${projectsFolder}/${destProject}") {
    branchSources {
        github {
            id("${githubid}") // IMPORTANT: use a constant and unique identifier
            scanCredentialsId("${scmCredsID}") // GITHUB_ACCESS
            repoOwner("${projectName}")
            repository("${destProject}")
            includes("master feature/* bugfix/* hotfix/* release/*")
            excludes("donotbuild/*")
        }
    }
  	factory {
        workflowBranchProjectFactory {
            scriptPath("jenkinsFile.groovy")
        }
    }
    triggers {
        periodicFolderTrigger {
            interval("2m")
        }
    }
    orphanedItemStrategy {
        discardOldItems {
            numToKeep(10)
        }
    }
}'''
}
