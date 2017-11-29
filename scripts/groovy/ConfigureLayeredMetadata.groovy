// Configure layered metadata.
import com.reallysi.rsuite.admin.importer.*
import com.reallysi.rsuite.client.api.*
import com.reallysi.rsuite.remote.api.*

def info(msg) {
	println " + [INFO] ${msg}"
}

rsuite.login();
println"Configuring layered metadata..."
lmDefs = rsuite.getLayeredMetaDataDefinitionInfos();
fields = [:];

lmDefs.getMapList().each {
	def map = it.convertToMap()
	def fieldName = map["name"];
	fields[map["name"]] = map["type"];
	reportMetaDataMap(map);
}

def reportMetaDataMap(map) {
	def fieldName = map["name"];
	println"Metadata field: \"" + fieldName +"\""
	map.entrySet().each {
		def key = it.getKey();
		if (key !="name") {
			println" " + key +"=" + it.getValue();
		}
	}
}

def addOrReplaceLMDDefinition(lmdName, associatedElements, allowedValues, versioned, allowsMultiple, allowContextual) {
	println" + [INFO] Metadata field \"" + lmdName +":";
	if (fields.containsKey(lmdName)) {
		println" + [INFO]   Field exists, removing existing definition...";
		rsuite.removeLayeredMetaDataDefinition(lmdName);
	}
	println" + [INFO]   Creating new definition for \"" + lmdName +"\"...";
	println" + [INFO]     associated elements:" + associatedElements;
	println" + [INFO]     allowed values:" + allowedValues;
	println" + [INFO]     versioned:" + versioned;
	println" + [INFO]     allowsMultiple:" + allowsMultiple;
	println" + [INFO]     allowContextual:" + allowContextual;
	def lmd = new LayeredMetaDataDefinition(lmdName,"string", versioned, allowsMultiple, allowContextual, associatedElements, allowedValues);
	rsuite.addLayeredMetaDataDefinition(lmd);
}

println"Updating definitions"

def assemblyTypes = ['rs_ca', 'rs_canode']
def bookType = ['book']
def nonXmlMoTypes = ['nonxml']

addOrReplaceLMDDefinition("ca-type", assemblyTypes, null, true, false, false);
addOrReplaceLMDDefinition("article-process-id", assemblyTypes, null, true, false, false);
addOrReplaceLMDDefinition("conversion_config_id", assemblyTypes, null, false, false, true)

// Book metadata
addOrReplaceLMDDefinition("book_date", bookType, null, false, false, true)
addOrReplaceLMDDefinition("submitted_by", bookType, null, false, false, true)
addOrReplaceLMDDefinition("new_or_revision", bookType, null, false, false, true)
addOrReplaceLMDDefinition("pub_date", bookType, null, false, false, true)
addOrReplaceLMDDefinition("title", bookType, null, false, false, true)
addOrReplaceLMDDefinition("subtitle", bookType, null, false, false, true)
addOrReplaceLMDDefinition("edition", bookType, null, false, false, true)
addOrReplaceLMDDefinition("long_description", bookType, null, false, false, true)
addOrReplaceLMDDefinition("short_description", bookType, null, false, false, true)
addOrReplaceLMDDefinition("author_bio", bookType, null, false, false, true)
addOrReplaceLMDDefinition("keywords", bookType, null, false, false, true)
addOrReplaceLMDDefinition("list_price", bookType, null, false, false, true)
addOrReplaceLMDDefinition("member_price", bookType, null, false, false, true)
addOrReplaceLMDDefinition("trim_size", bookType, null, false, false, true)
addOrReplaceLMDDefinition("format", bookType, null, false, false, true)
addOrReplaceLMDDefinition("page_count", bookType, null, false, false, true)
addOrReplaceLMDDefinition("bisac", bookType, null, false, false, true)
addOrReplaceLMDDefinition("store_category", bookType, null, false, false, true)
addOrReplaceLMDDefinition("audience", bookType, null, false, false, true)
addOrReplaceLMDDefinition("table_of_contents", bookType, null, false, false, true)
addOrReplaceLMDDefinition("endorsements", bookType, null, false, false, true)
