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
def nonXmlMoTypes = ['nonxml']

