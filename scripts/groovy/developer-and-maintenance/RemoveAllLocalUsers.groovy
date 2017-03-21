// Remove all local users
//
import com.reallysi.rsuite.client.api.*
import com.reallysi.rsuite.remote.api.*

// Invalidate all but current session before removing users; else users with valid sessions are not removed.
rsuite.login()
def currentSessionKey = rsuite.getSessionKey()
def sessionMapList = rsuite.getSessionInfos()
sessionMapList.each {
    if (it.get("sessionKey") != currentSessionKey) {
        println "Invalidating session " + it.get("sessionKey") + " for user " + it.get("userId")
        rsuite.killSession(it.get("sessionKey"))
    } else {
        println "Leaving session " + it.get("sessionKey") + " as this script is using it."
    }
}

// Now remove all local users.
// RCS-5018: not fixed until 5.1.0.
//rsuite.removeAllLocalUsers()

def rtn = rsuite.callPluginWebServiceAsString(
    "ama.main.webservice.rsuite5.RemoveAllLocalUsers",
    ["testArg":"testValue"])
println rtn

rsuite.logout()

