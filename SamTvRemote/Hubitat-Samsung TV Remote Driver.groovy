/*
Test only.  Light the programming fire for WS Samsung Remote.  Future:
a.	Add capability to open an installed application.
b.	Add a sendKey command buffer (if testing indicates).
c.	Add text box capability (if it works).
*/
import groovy.json.JsonOutput
metadata {
	definition (name: "Hubitat-Samsung TV Remote",
				namespace: "davegut",
				author: "David Gutheinz",
				importUrl: ""
			   ){
		command "hubCheck"				//	Checks hub's connectivity to Hubitat
		command "sendKey", ["string"]	//	Creates sendKey data
		command "connect"				//	Checks for token update then updates
		command "close"					//	command to close socket,60 sec lag
		//	Key commands programmed for convenience
		command "home"
		command "off"
		command "tools"
		command "menu"
		command "left"
		command "right"
		command "up"
		command "down"
		command "enter"
		attribute "wsStatus", "string"
	}
	preferences {
		input ("deviceIp", "text", title: "Samsung TV Ip")
		input ("hubIp", "text", title: "NodeJs Hub IP")
		input ("hubPort", "text", title: "NodeJs Hub Port", defaultValue: "8080")
	}
}
def installed() { state.token = "12345678" }
def updated() { 
	log.info "<b>Get Test Device Data</b>"
	httpGet([uri: "http://${deviceIp}:8001/api/v2/", timeout: 5]) { resp ->
		def message = "<b>Device Data:</b> "
		message += "name: <b>${resp.data.name}</b> || "
		message += "model: <b>${resp.data.device.model}</b> || "
		log.info message
		state.name = "Hubitat-Samsung TV Remote"
		state.name64 = "Hubitat-Samsung TV Remote".encodeAsBase64().toString()
	}
	runIn(2, connect)
}

//	Some example keys
def off() { sendKey("KEY_POWEROFF") }
def home() { sendKey("KEY_HOME") }
def tools() { sendKey("KEY_TOOLS") }
def menu() { sendKey("KEY_MENU") }
def left() { sendKey("KEY_LEFT") }
def right() { sendKey("KEY_RIGHT") }
def up() { sendKey("KEY_UP") }
def down() { sendKey("KEY_DOWN") }
def enter() { sendKey("KEY_ENTER") }

//	Hubitat to Hub interface
def hubCheck() {
	sendWsCmd("hubCheck")
}
def connect() { sendWsCmd("connect") }
def sendKey(key) {
	//	Create key command and send via sendMessage
	//	Key must be in format "KEY_" plus KEYVALUE (i.e., KEY_MENU)
	def data = """{"method":"ms.remote.control","params":{"Cmd":"Click",""" +
		""""DataOfCmd":"${key}","Option":"false","TypeOfRemote":"SendRemoteKey"}}"""
	sendWsCmd("sendMessage", data)
}
def close() { sendWsCmd("close") }
def sendWsCmd(command, data = null) {
	def token = state.token
	def name = state.name64
	def url = "wss://${deviceIp}:8002/api/v2/channels/samsung.remote.control?name=${name}&token=${token}"
	def headers = [HOST: "${hubIp}:${hubPort}", command: command, data: data, url: url]
	sendHubCommand(new hubitat.device.HubAction([headers: headers], device.deviceNetworkId, [callback: wsHubParse]))
}
def wsHubParse(response) {
	def command = response.headers.command
	def cmdResponse = response.headers.cmdResponse
//	logDebug("wsHubParse: ${command} || ${cmdResponse}")
	//	Note we are not sending close commands.  Will let the socket to expire naturally.
	runIn(60, close)
	switch(command) {
		case "hubCheck":
		log.trace "${command}: ${cmdResponse}"
			break
			
		case "connect":
//			log.trace "${command} | $cmdResponse"
			def resp = parseJson(cmdResponse)
			def token = parseJson(resp.respData).data.token
			if(token != state.token && token != null) {
				state.token = token
log.trace "setting token to $token"
			}
			sendEvent(name: "wsStatus", value: "open")
			break
		
		case "close":
			log.trace "${command} | $cmdResponse"
			sendEvent(name: "wsStatus", value: "closed")
			break
		
		case "sendMessage":
			log.trace "${command}: $cmdResponse"
		//	Futture:  Check if successful.  If so, call next message (if buffer implemented)
			break
		
		default:
			logTrace "DEFAULT (ERROR)"
	}
}
def logTrace(msg) { log.trace "websocket Test || ${msg}" }
def logInfo(msg) { log.info "websocket Test || ${msg}" }
def logDebug(msg) { log.debug "websocket Test || ${msg}" }
/*
Test only.  Light the programming fire for WS Samsung Remote.  Future:
a.	Add capability to open an installed application.
b.	Add a sendKey command buffer (if testing indicates).
c.	Add text box capability (if it works).
*/
import groovy.json.JsonOutput
metadata {
	definition (name: "Hubitat-Samsung TV Remote",
				namespace: "davegut",
				author: "David Gutheinz",
				importUrl: ""
			   ){
		command "hubCheck"				//	Checks hub's connectivity to Hubitat
		command "sendKey", ["string"]	//	Creates sendKey data
		command "connect"				//	Checks for token update then updates
		command "close"					//	command to close socket,60 sec lag
		//	Key commands programmed for convenience
		command "home"
		command "off"
		command "tools"
		command "menu"
		command "left"
		command "right"
		command "up"
		command "down"
		command "enter"
		attribute "wsStatus", "string"
	}
	preferences {
		input ("deviceIp", "text", title: "Samsung TV Ip")
		input ("hubIp", "text", title: "NodeJs Hub IP")
		input ("hubPort", "text", title: "NodeJs Hub Port", defaultValue: "8080")
	}
}
def installed() { state.token = "12345678" }
def updated() { 
	log.info "<b>Get Test Device Data</b>"
	httpGet([uri: "http://${deviceIp}:8001/api/v2/", timeout: 5]) { resp ->
		def message = "<b>Device Data:</b> "
		message += "name: <b>${resp.data.name}</b> || "
		message += "model: <b>${resp.data.device.model}</b> || "
		log.info message
		state.name = "Hubitat-Samsung TV Remote"
		state.name64 = "Hubitat-Samsung TV Remote".encodeAsBase64().toString()
	}
	runIn(2, connect)
}

//	Some example keys
def off() { sendKey("KEY_POWEROFF") }
def home() { sendKey("KEY_HOME") }
def tools() { sendKey("KEY_TOOLS") }
def menu() { sendKey("KEY_MENU") }
def left() { sendKey("KEY_LEFT") }
def right() { sendKey("KEY_RIGHT") }
def up() { sendKey("KEY_UP") }
def down() { sendKey("KEY_DOWN") }
def enter() { sendKey("KEY_ENTER") }

//	Hubitat to Hub interface
def hubCheck() {
	sendWsCmd("hubCheck")
}
def connect() { sendWsCmd("connect") }
def sendKey(key) {
	//	Create key command and send via sendMessage
	//	Key must be in format "KEY_" plus KEYVALUE (i.e., KEY_MENU)
	def data = """{"method":"ms.remote.control","params":{"Cmd":"Click",""" +
		""""DataOfCmd":"${key}","Option":"false","TypeOfRemote":"SendRemoteKey"}}"""
	sendWsCmd("sendMessage", data)
}
def close() { sendWsCmd("close") }
def sendWsCmd(command, data = null) {
	def token = state.token
	def name = state.name64
	def url = "wss://${deviceIp}:8002/api/v2/channels/samsung.remote.control?name=${name}&token=${token}"
	def headers = [HOST: "${hubIp}:${hubPort}", command: command, data: data, url: url]
	sendHubCommand(new hubitat.device.HubAction([headers: headers], device.deviceNetworkId, [callback: wsHubParse]))
}
def wsHubParse(response) {
	def command = response.headers.command
	def cmdResponse = response.headers.cmdResponse
//	logDebug("wsHubParse: ${command} || ${cmdResponse}")
	//	Note we are not sending close commands.  Will let the socket to expire naturally.
	runIn(60, close)
	switch(command) {
		case "hubCheck":
		log.trace "${command}: ${cmdResponse}"
			break
			
		case "connect":
//			log.trace "${command} | $cmdResponse"
			def resp = parseJson(cmdResponse)
			def token = parseJson(resp.respData).data.token
			if(token != state.token && token != null) {
				state.token = token
log.trace "setting token to $token"
			}
			sendEvent(name: "wsStatus", value: "open")
			break
		
		case "close":
			log.trace "${command} | $cmdResponse"
			sendEvent(name: "wsStatus", value: "closed")
			break
		
		case "sendMessage":
			log.trace "${command}: $cmdResponse"
		//	Futture:  Check if successful.  If so, call next message (if buffer implemented)
			break
		
		default:
			logTrace "DEFAULT (ERROR)"
	}
}
def logTrace(msg) { log.trace "websocket Test || ${msg}" }
def logInfo(msg) { log.info "websocket Test || ${msg}" }
def logDebug(msg) { log.debug "websocket Test || ${msg}" }
def logWarn(msg) { log.warn "websocket Test || ${msg}" }