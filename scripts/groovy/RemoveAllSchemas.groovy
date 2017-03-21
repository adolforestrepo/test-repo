// Remove all schemas (DTDs and XSDs)
//
import com.reallysi.rsuite.client.api.*
import com.reallysi.rsuite.remote.api.*

rsuite.login();

println " + [INFO] Removing all existing schemas...";
def infos = rsuite.getSchemaInfos();

infos.each {
	rsuite.removeSchema(it.schemaId)
}

rsuite.logout();

