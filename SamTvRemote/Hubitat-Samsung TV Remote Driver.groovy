/*
Test only.  Light the programming fire for WS Samsung Remote.  Future:
a.	Add capability to open an installed application.
b.	Add a sendKey command buffer (if testing indicates).
c.	Add text box capability (if it works).
*/
import groovy.json.JsonOutput
metadata {
	definition (name: "Hubitat-Samsung Remote",
				namespace: "davegut",
				author: "David Gutheinz",
				importUrl: ""
			   ){
		capability "Switch"
		command "hubCheck"				//	Checks hub's connectivity to Hubitat
		command "sendKey", ["string"]	//	Creates sendKey data
		command "connect"				//	Checks for token update then updates
		command "close"					//	command to close socket,60 sec lag
		//	===== TV Input Modes
		command "HDMI"		//	toggles through occupied HDMI ports
		command "TV"
		command "frameSet"
		command "toggleArt"
		command "ambientMode"
		//	===== Physical Remote Keys =====
		command "volumeUp"
		command "volumeDown"
		command "mute"
		command "channelUp"
		command "channelDown"
		command "home"
		command "guide"
		command "exit"
		command "left"
		command "right"
		command "up"
		command "down"
		command "enter"
		//	===== Utility Keys =====
		command "tools"
		command "menu"

		attribute "wsStatus", "string"
		attribute "hubComms", "bool"
	}
	preferences {
		input ("deviceIp", "text", title: "Samsung TV Ip")
		input ("hubIp", "text", title: "NodeJs Hub IP")
		input ("hubPort", "text", title: "NodeJs Hub Port", defaultValue: "8080")
		def tvModes = ["TV", "HDMI", "AMBIENT", "ART_MODE"]
		input ("tvPwrOnMode", "enum", title: "TV Startup Display", options: tvModes)
//		input ("extPower", "bool", title: "Use External Device for Power Control", defaultValue: false)
	}
}
def installed() {
	state.token = "12345678"
	updateDataValue("name", "Hubitat Samsung Remote")
	updateDataValue("name64", "Hubitat Samsung Remote".encodeAsBase64().toString())
}
def updated() {
	logInfo("updated: get device data and model year")
	def tokenSupport
//	if (!getDataValue("tokenSupport") {
		tokenSupport = getDeviceData()
//	}
//	if (getDataValue("tokenSupport") == "true") {
//		hubCheck()
//		runEvery15Minutes(hubCheck)
//	}
	runIn(2, connect)
}
def getDeviceData() {
	def tokenSupport = "false"
	httpGet([uri: "http://${deviceIp}:8001/api/v2/", timeout: 5]) { resp ->
		updateDataValue("deviceMac", resp.data.device.wifiMac)
		def modelYear = "20" + resp.data.device.model[0..1]
		updateDataValue("modelYear", modelYear)
		def frameTv = "false"
		if (resp.data.device.FrameTvSupport) {
			frameTv = resp.data.device.FrameTvSupport
		}
		updateDataValue("frameTv", frameTv)
		if (resp.data.device.TokenAuthSupport) {
			tokenSupport = resp.data.device.TokenAuthSupport
		}
		def uuid = resp.data.device.duid.substring(5)
		updateDataValue("uuid", uuid)
		updateDataValue("tokenSupport", tokenSupport)
	}
	logDebug("getDeviceData: Updated Device Data.")
	return tokenSupport
}


//	===== Key Definitions =====
def on() {
	sendEvent(name: "switch", value: "on")
	wakeOnLan()
	if(tvPwrOnMode) {
		pauseExecution(15000)
		if(tvPwrOnMode == "ART_MODE") { toggleArt() }
		else { sendKey(tvPwrOnMode) }
	}
}
def off() {
	sendEvent(name: "switch", value: "off")
	if (getDataValue("frameTv") == "false") { sendKey("POWER") }
	else {
		sendKey("POWER", "Press")
		pauseExecution(3000)
		sendKey("POWER", "Release")
	}
	pauseExecution(200)
	close()
}

//	===== TV Source Modes
def toggleArt() {
	if (getDataValue("frameTv") == "true") { sendKey("POWER") }
}
def ambientMode() { sendKey("AMBIENT") }
def HDMI() { sendKey("HDMI") }
def TV() { sendKey("TV") }

	   
def tools() { sendKey("TOOLS") }
def menu() { sendKey("MENU") }

//	===== Standard Remote Keys"
def home() { sendKey("HOME") }
def left() { sendKey("LEFT") }
def right() { sendKey("RIGHT") }
def up() { sendKey("UP") }
def down() { sendKey("DOWN") }
def enter() { sendKey("ENTER") }
def volumeUp() { sendKey("VOLUP") }
def volumeDown() { sendKey("VOLDOWN") }
def mute() { sendKey("MUTE") }
def channelUp() { sendKey("CHUP") }
def channelDown() { sendKey("CHDOWN") }
def guide() { sendKey("GUIDE") }
def exit() { sendKey("EXIT") }

//	===== Hubitat to Hub interface =====

//	===== WebSocket Interface =====
//def getToken() { sendWsCmd("getToken") }
def connect() { sendWsCmd("connect") }
def sendKey(key, cmd = "Click") {
	key = "KEY_${key.toUpperCase()}"
	def data = """{"method":"ms.remote.control","params":{"Cmd":"${cmd}",""" +
		""""DataOfCmd":"${key}","Option":"false","TypeOfRemote":"SendRemoteKey"}}"""
	sendWsCmd("sendMessage", data)
}
def close() {
	sendEvent(name: "wsStatus", value: "closed")
	sendWsCmd("close")
}

//	===== Comms and Control for NodeJS Servr =====
def hubCheck() {
	sendWsCmd("hubCheck")
	runIn(5, hubCheckFail)
}
def hubCheckFail() { sendEvent(name: "hubComms", value: false) }
def sendWsCmd(command, data = "") {
	logDebug("sendWsCmd: ${command} | ${data}")
	def name = getDataValue("name64")
	def token = state.token
	if (device.currentValue("wsStatus") == "closed") { command = connect }
	url = "wss://${deviceIp}:8002/api/v2/channels/samsung.remote.control?name=${name}&token=${token}"
	def headers = [HOST: "${hubIp}:${hubPort}", command: command, data: data, url: url]
	sendHubCommand(new hubitat.device.HubAction([headers: headers],
												device.deviceNetworkId,
												[callback: wsHubParse]))
}
def wsHubParse(response) {
	def command = response.headers.command
	def cmdResponse = response.headers.cmdResponse
	def statusMsg = ""
	def resp
	def event = "hubCheck: OK"
	resp = parseJson(cmdResponse)
	if(resp.error) {
		statusMsg = "ERROR: ${resp.error}"
	}
	switch(command) {
		case "hubCheck":
			unschedule(hubCheckFail)
			sendEvent(name: "hubComms", value: true)
			statusMsg = "Hub Check Successful."
			break
		case "getToken":
		case "connect":
			def wsStatus = "open"
			if (resp.wsStatus == "open") {
				statusMsg = "Connect successful. "
			} else {
				wsStatus = "closed"
				statusMsg = "Connect failed."
			}
			sendEvent(name: "wsStatus", value: wsStatus)
			def token = parseJson(resp.respData).data.token
			if(token != state.token && token != null) {
				state.token = token
				statusMsg += "Token Updated"
			}
			break
		case "close":
			if (resp.wsStatus == "closed") {
				statusMsg = "Close successful."
			} else {
				statusMsg = "Close failed."
			}
			sendEvent(name: "wsStatus", value: resp.wsStatus)
			break
		case "sendMessage":
			statusMsg = "Send Message Successful."
			break
		default:
			statusMessage = "DEFAULT ERROR"
	}
	logDebug("wsHubParse - ${command}: status = ${statusMsg}")
}

//	===== Wake On Lan =====
def wakeOnLan() {
	def mac = getDataValue("deviceMac").replaceAll(":","").replaceAll("-","")
	logDebug("wakeOnLan: sending WOL packet to ${mac}")
    def result = new hubitat.device.HubAction (
		"wake on lan ${mac}", hubitat.device.Protocol.LAN, null)
}

//	===== Logging =====
def logTrace(msg) { log.trace "websocket V2 || ${msg}" }
def logInfo(msg) { log.info "websocket V2 || ${msg}" }
def logDebug(msg) { log.debug "websocket V2 || ${msg}" }
def logWarn(msg) { log.warn "websocket V2 || ${msg}" }