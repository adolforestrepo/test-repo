// Remove roles
//
import com.reallysi.rsuite.client.api.*
import com.reallysi.rsuite.remote.api.*
import groovy.inspect.*

rsuite.login()

def roles = rsuite.getRoleInfos()
roles.each {
    def role = it.convertToMap()
    if (role["readOnly"] == "false" && role["name"] != "RSuiteAdministrator") {
        println "Removing role '" + role["name"] + "'..."
        rsuite.removeRole(role["name"])
    }
}

rsuite.logout()

