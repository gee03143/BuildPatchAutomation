import java.text.SimpleDataFormat
import groovy.json.JsonSlurperClassic

node
{
    lock('WorkSpace' + env.WORKSPACE)
    {
        def bSuccess = false
        try{
            withEnv([
                "UE4DIST_PATH=C:\\Program Files\\Epic Games\\UE_4.27"
                )]
                {
                    withEnv([
                        "GIT_URL=https://github.com/gee03143/BuildPatchAutomation.git",
                        "GIT_BRANCH=${env.GIT_BRANCH?env.GIT_BRANCH:'main'}",
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
					withEnv([
						"COOK_FLAVOR=${env.COOK_FLAVOR?env.COOK_FLAVOR:'ASTC'}",
						"ARCHIVE_DIRECTORY=${env.ARCHIVE_DIRECTORY?env.ARCHIVE_DIRECTORY:env.WWW_ROOT + "\\${BUILD_TYPE}\\" + buildName}",
						"ARCHIVE_NAME=DEV${gameVersion}_${env.PLATFORM}_${env.CONIFGURATION}",
						"RELEASE_VERSION=${gameVersion}"
					])
					{
						bat "rmdir /s /q ${WORKSPACE}\\BuildPatchAutomation\\Binaries\\${env.PLATFORM} || true"
						bat "rmdir /s /q ${ARCHIVE_DIRECTORY}\\${ARCHIVE_NAME} || true"
						bat "mkdir ${ARCHIVE_DIRECTORY}\\${ARCHIVE_NAME} || true"
						
						def distributionSwitch = env.FOR_DISTRIBUTION == 'true' ? ' -distribution' : ''
						def createReleaseVersionSwitch = RELEASE_VERSION ? -CreateReleaseVersion=${RELEASE_VERSION}" : ""
						def buildCommandLine = "call ${UE4DIST_PATH}\\Engine\\Build\\BatchFiles\\RunUAT.bat BuildCookRun -project=\"${WORKSPACE}\\git\\BuildPatchAutomation\\PatchTest.uproject\" -build -noP4 -platform=${PLATFORM} -targetplatform=${PLATFORM} -cookflavor=${COOK_FLAVOR} -archivedirectory=\"${ARCHIVE_DIRECTORY}\\${ARCHIVE_NAME}\" -cook -archive -stage -package -compressed -pak -utf8output"
						
						buildCommandLine += createReleaseVersionSwitch
						buildCommandLine += distributionSwitch
						
						def defaultGamePath = "${WORKSPACE}\\git\\Config\\DefaultGame.ini"
						def defaultEnginePath = "${WORKSPACE}\\git\\Config\\DefaultEngine.ini"
						
						if(BUILD_TYPE == 'MinimalGame')
						{
							def originalDefaultGameContent = readFile file: defaultGamePath, encoding: "UTF-8"
							def originalDefaultEngineContent = readFile file: defaultEnginePath, encoding: "UTF-8"
							
							def readContent = readFile file: defaultGamePath, encoding: "UTF-8"
							readContent = readContent.replace("+MapsToCook", ";+MapsToCook")
							readContent = readContent.replace("+DirectoriesToAlwaysCook", ";+DirectoriesToAlwaysCook")
							readContent += "\r\n[/Script/UnrealEd.ProjectPackagingSettings]"
							readContent += "\r\n+MapsToCook=(FilePath=\"/Game/Patch/PatchMap\")"
							readContent += "\r\n+DirectoriesToAlwaysCook=(Path\"/Game/Patch\")"
							
							writeFile file: defaultGamePath, text: readContent, encoding: "UTF-8"
							
							readContent = readFile file: defaultEnginePath, encoding: "UTF-8"
							readContent += "\r\n[/Script/PatchTest.IntroMode]"
							readContent += "\r\nbDoPatch=true"
							
							writeFile file: defaultEnginePath, text: readContent, encoding: "UTF-8"
							
							builCommandLine += " -manifests"							
						}
						
						bat buildCommandLine
						
						if(BUILD_TYPE == 'MinimalGame')
						{
							writeFile file: defaultGamePath, text: originalDefaultGameContent, encoding: "UTF-8"
							writeFile file: defaultEnginePath, text: originalDefaultEngineContent, encoding: "UTF-8"
						}
						
						bSuccess = true
					}						
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
