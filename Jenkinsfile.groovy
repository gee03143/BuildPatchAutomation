import java.text.SimpleDataFormat
import groovy.json.JsonSlurperClassic

node
{
    lock('WorkSpace' + env.WORKSPACE)
    {
        def bSuccess = false
        try{
            withEnv([
                "UE4DIST_PATH=${params.UE4DIST_PATH?params.UE4DIST_PATH:env.UE4DIST_PATH}"
                )]
                {
                    withEnv([
                        "GIT_URL=https://github.com/gee03143/BuildPatchAutomation.git",
                        "GIT_BRANCH=${env.GIT_BRANCH?env.GIT_BRANCH:'main'}",
                        "SVN_URL=file:///C:/Users/gee03/Desktop/svn",
                        "SVN_BRANCH=${env.SVN_BRANCH?env.SVN_BRANCH:'trunk'}",
                        "CONFIGURAION=${env.CONFIGURAION?env.CONFIGURAION:'Development'}",
                        "PLATFORM=${env.PLATFORM?env.PLATFORM:'Win64'}",
                        "BUILD_TYPE=${env.BUILD_TYPE?env.BUILD_TYPE:'FullGame'}"
                    ])
                    {
                        stage('Preparation')
                        {
                            dir('git')
                            {
                                git branch: "${GIT_BRANCH}", url: "${GIT_URL}"
                                gitCommitHash = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
					            buildName = readFile(file:"Source/BuildName.txt").replace("\"", "").trim()
					            gameVersion = readFile(file:"Source/GameVersion.txt").replace("\"", "").trim()
                            }
                        }
                        if(BUILD_TYPE == 'FullGame' || (BUILD_TYPE == 'MinimalGame' && env.CreateReleaseVersion == 'true'))
                        {
                            stage('Build')
                            {
                                lock('UE4Dist')
                                {
                                }
                            }
                        }
                        
                        if(BUILD_TYPE == 'MinimalGame')
                        {
                            stage('Generate Patch')
                            {
                            }
                        }
                        
                        //do some upload task

                   }
               }
        }
        finally{   
        }
    }
  
}
