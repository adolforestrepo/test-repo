// Configure hot folders
// -----------------------------------------------------------------------
def projectDir = new File(scriptFile.absolutePath).parentFile.parentFile.parentFile
def props = propertyFactory.find(new File(projectDir, "conf/project.properties"))

def hotFolderToWorkflowMap = [
  (props.getNonEmpty("ingestion.taxonomy.hotfolder.dir")): "CTDO production",
  (props.getNonEmpty("ingestion.tdbackcontent.hotfolder.dir")): "ATD Article Word To XML To Indesign",
  (props.getNonEmpty("ingestion.tpm.hotfolder.dir")): "TPM",
]

rsuite.login()

hotFolderToWorkflowMap.each { entry -> rsuite.removeHotFoldersByProcessDefinition(entry.value)

	if (entry.key) { 
		println "[INFO] Set hotfolder for workflow ${entry.value}: ${entry.key}"; 
		rsuite.setHotFolder(entry.key, entry.value, true); 
	} 
};

rsuite.logout()
