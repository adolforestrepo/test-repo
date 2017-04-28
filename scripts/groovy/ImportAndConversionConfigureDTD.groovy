// Groovy script to import and configure the JATS DTDs for journals

import com.reallysi.rsuite.admin.importer.*
import com.reallysi.rsuite.client.api.*

// -----------------------------------------------------------------------


def projectDir = new File(scriptFile.absolutePath).parentFile.parentFile.parentFile
def doctypesDir = new File(projectDir, "doctypes");
def doctypesWithIdDeclarationDir = new File(doctypesDir, "conversion_configuration/dtd");
def srcDir = new File(projectDir, "src");

println " + [INFO] Logging into RSuite...1";

rsuite.login();

println " + [INFO] Logging into RSuite...2";

def namespaceDecls = (String[])[
	"p=" + "http://somenamespaceuri.com/namespace"
	];
publicId = "";
   
xsltDir = new File(srcDir, "xslt")
previewXsltFile = new File(xsltDir, "preview.xsl");

println " + [INFO] Logging into RSuite...3";
   
//---------  training article  ----------

schemaType = "DTD";
schemaDir = doctypesWithIdDeclarationDir;
schemaName = "conversion_configuration.dtd";
publicId = "urn:pubid:org.dita4publishers:doctypes:dita:conversion_configuration";
				
// FIXME: Set up the proper JATS preview transform.
//def previewXsltFile = new File(new File(xsltDir, "jpub3-preview"), "jpub3-rsuite-preview-shell.xsl");

def previewXsltFile = new File(new File(xsltDir, "jpub3-preview"), "jats-html.xsl");

// moDefList = [ 
// new ManagedObjectDefinition(['name' : 'article',
// 'displayNameXPath':
// 'front/article-meta/title-group/article-title',
// 'versionable': 'true',
// 'reusable': 'true',
// 'browsable': 'true']),
// ]

moDefList = [ 
                      new ManagedObjectDefinition(['name' : 'conversion_configuration',
                      'displayNameXPath': "*[contains(@class, ' topic/title ')]",
                      'versionable': 'true',
                      'reusable': 'true',
                      'browsable': 'true']),
]

loadSchema(schemaType, schemaDir, schemaName, publicId, schemaName, previewXsltFile, null, moDefList);



//-----------------------------------------------------------------------


def loadSchema(schemaType, schemaDir, schemaName, publicId, systemId, htmlPreviewXsltFile, namespaceDecls, moDefList) 

{

    println "";

    println " + [INFO] loadSchema(): Loading \"" + schemaName + "\"";

    def uuid 

    def schemaFile = new File(schemaDir, schemaName);

    if (schemaType == "DTD") {

      def schemaSrc = new SchemaInputSource(schemaFile, systemId, publicId);

      def importer = importerFactory.generateImporter(schemaType, schemaSrc);

      uuid = importer.importDtd()

    } else {

      def schemaSrc = new SchemaInputSource(schemaFile);

      def importer = importerFactory.generateImporter(schemaType, schemaSrc);

      uuid = importer.execute()

    }

    

    if (moDefList != null) {

      println " + [INFO] loadSchema(): Setting managed object definitions...";

      println " + [DEBUG] loadSchema(): namespaceDecls=" + namespaceDecls;

      rsuite.setManagedObjectDefinitions(uuid, false, namespaceDecls, moDefList)

    }

    if (htmlPreviewXsltFile != null) {

      println " + [INFO] loadSchema(): Setting preview style sheet to \"" + htmlPreviewXsltFile.name + "\"...";

      rsuite.loadStylesheetForSchema(uuid, htmlPreviewXsltFile)

    }

    return uuid;

}

// -----------------------------------------------------------------------

// End of script              new ManagedObjectDefinition(['name' : 'titleGroup', 'displayNameXPath': 'concat(substring(title[1], 1, 80), "...")', 'versionable': 'true', 'reusable': 'true', 'browsable': 'true']),



