// Script to import and configure the ASTD DTDs into RSuite
//
import com.reallysi.rsuite.admin.importer.*
import com.reallysi.rsuite.client.api.*

// -----------------------------------------------------------------------

def loadTopicDtds        = true;
def loadMapDtds          = true;
def loadStyle2TagMapDtds = true; 
def loadTaxDtds          = true;

def otHome = System.getenv("DITA_OT_HOME");
if (otHome == null){
    println "Environment variable DITA_OT_HOME not set. Set to the directory";
	println "that contains your DITA Open Toolkit, e.g., the 'dita/DITA-OT' directory";
	println "under the OxygenXML frameworks/ directory.";
	
	println "Trying to use ditaotdir param from the LoadDTDs ant taget";
	try
	{
        otHome = ditaotdir;
    }
	catch (Exception ex) {
	    println "ditaotdir was not set. YOu can pass it a parameter -Dditaotdir=/path/to/dita/ot";
		return;
	}
  
  
}

if (otHome == null) {
	println "ERROR: Failed to set property: otHome"
	return;
}
println "Dita OT Home Set to: "+otHome
def File otDir = new File(otHome);
def File catalogFile = new File(otDir, "catalog-dita.xml");
def catalog = catalogFile.getAbsolutePath();
println "catalog=\"" + catalog + "\"";
def projectDir = new File(scriptFile.absolutePath).parentFile.parentFile.parentFile;
def doctypesDir = new File(projectDir, "doctypes");
def File xsltDir = new File(projectDir, "src/xslt");
def File previewXslFile = new File(xsltDir, "preview/dita-preview-shell.xsl");

def baseTopicTypeURI = "urn:pubid:astd.com/doctypes/dita/";
def baseMapTypeURI = "urn:pubid:astd.com/doctypes/dita/";

def taxDir = new File(projectDir, "taxonomy");
def taxXmlDir = new File(taxDir, "xml");
def taxDtd = new File(taxXmlDir, "taxonomy.dtd");


def loadAndConfigureTopicDtd(dtdFile, dtdPublicId, topicTypes, otherMoTypes, previewXslFile, catalog)
{
	def moDefList = [];
	topicTypes.each {
		moDefList.add(new ManagedObjectDefinition(['name' : it, 
		                                              'displayNameXPath': "*[contains(@class, ' topic/title')]", 
		                                              'versionable': 'true', 
		                                              'reusable': 'true']))
		
	}
	
	otherMoTypes.each { nameTitle ->
	moDefList.add(new ManagedObjectDefinition(['name' : nameTitle[0], 
                                               'displayNameXPath': nameTitle[1], 
                                               'versionable': 'true', 
                                               'reusable': 'true']))
	}
	
	loadAndConfigureDtd(dtdFile, dtdPublicId, moDefList, previewXslFile, catalog);
}

def loadAndConfigureMapDtd(dtdFile, dtdPublicId, mapType, previewXslFile, catalog)
{
    def moDefList = [];
    moDefList.add(new ManagedObjectDefinition(['name' : mapType, 
	   'displayNameXPath': 
		   "if (*[contains(@class, ' topic/title')]) " +
              " then *[contains(@class, ' topic/title')] " +
              " else string(@title)", 
	   'versionable': 'true', 
	   'reusable': 'true']))
    
    loadAndConfigureDtd(dtdFile, dtdPublicId, moDefList, previewXslFile, catalog);
}

def loadAndConfigureDtd(dtdFile, dtdPublicId, moDefList, previewXslFile, catalog)
{
    println " + [INFO] Importing DTD " + dtdFile.name + ", public ID \"" + dtdPublicId + "\"...";
    importer = importerFactory.generateImporter("DTD", new SchemaInputSource(dtdFile, dtdFile.name, dtdPublicId));
    importer.setCatalogNames((String[])[catalog])
    uuid = importer.importDtd()
    
	rsuite.setManagedObjectDefinitions(uuid, false, moDefList)
	rsuite.loadStylesheetForSchema(uuid, previewXslFile)

}

def loadAndConfigureTaxDtd(dtdFile)
{
    println " + [INFO] Importing DTD " + dtdFile.name + "\"...";
    importer = importerFactory.generateImporter("DTD",
        new SchemaInputSource(dtdFile, dtdFile.name, "urn:pubid:astd.com:doctypes:taxonomy"));
    uuid = importer.execute();
    def moDefList = [
        new ManagedObjectDefinition(['name' : 'taxonomy', 'displayNameXPath': "'taxonomy'", 'versionable': 'true', 'reusable': 'true']),
        new ManagedObjectDefinition(['name' : 'taxonomy-options', 'displayNameXPath': "@name", 'versionable': 'true', 'reusable': 'true']),
    ];
    rsuite.setManagedObjectDefinitions(uuid, false, moDefList);
}

	
def topicTypes = [
                  'article', 
                  'chapter', 
                  'concept', 
                  'feature', 
                  'interview-question',
                  'reference', 
                  'sidebar', 
                  'subsection', 
                  'task', 
                  'topic', 
                  'pubmap', 
				   ]
	
def mapTypes = ['map', 'pubmap']	
	
def otherMoTypes = 	[ 
                   	  ['art',"'Art '"],
                   	  //['classification',"'Classification '"],
                   	]
	


if (loadTopicDtds) {
  topicTypes.each {
  	loadAndConfigureTopicDtd(new File(doctypesDir, it + "/dtd/" + it + ".dtd"), 
  	        baseTopicTypeURI + it, 
  	        topicTypes, 
  	        otherMoTypes,
  	        previewXslFile,
  	        catalog);
  	
  }
} else {
  println "";
  println " + [INFO] Skipping import of topic doctypes."
  println "";
}

if (loadMapDtds) {	
  mapTypes.each {
      loadAndConfigureMapDtd(new File(doctypesDir, it + "/dtd/" + it + ".dtd"), 
              baseTopicTypeURI + it, 
              it, 
              previewXslFile,
              catalog);
      
  }
} else {
  println "";
  println " + [INFO] Skipping import of map doctypes."
  println "";
}
    
if (loadStyle2TagMapDtds) {
  // NOTE: This doctype is provided by the DITA4Publishers project.
  println "";
  println " + [INFO] Importing style2tagmap.xsd..."
  println "";
  def schemaFile = new File(otDir, "plugins/net.sourceforge.dita4publishers.doctypes/doctypes/style2tagmap/xsd/style2tagmap.xsd") 
  def importer = importerFactory.generateImporter("XMLSchema", new SchemaInputSource(schemaFile));
  uuid = importer.execute()   
  def moDefList = [];
  
  def namespaceDecls = (String[])[ "s2t=" + "urn:public:/dita4publishers.org/namespaces/word2dita/style2tagmap"];
  
  moDefList.add(new ManagedObjectDefinition(['name' : '{urn:public:/dita4publishers.org/namespaces/word2dita/style2tagmap}:style2tagmap', 
                                             'displayNameXPath': "s2t:title", 
                                             'versionable': 'true', 
                                             'reusable': 'true']))
  rsuite.setManagedObjectDefinitions(uuid, false, namespaceDecls, moDefList); 
  
  def tagmapPreviewXslFile = new File(xsltDir, "style2tagmap/preview/style2tagmap-preview.xsl")
  if (tagmapPreviewXslFile != null && tagmapPreviewXslFile.exists()) {
      rsuite.loadStylesheetForSchema(uuid, tagmapPreviewXslFile)
  }
} else {
  println "";
  println " + [INFO] Skipping import of style2tagmap doctype."
  println "";
}

if (loadTaxDtds) {
  loadAndConfigureTaxDtd(taxDtd);

} else {
  println "";
  println " + [INFO] Skipping import of taxonomy doctype."
  println "";
}


// End of script
