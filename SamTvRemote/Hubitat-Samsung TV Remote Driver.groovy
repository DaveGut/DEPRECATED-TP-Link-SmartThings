/*
Test only.  Light the programming fire for WS Samsung Remote.  Future:
a.	Add capability to open an installed application.
b.	Add a sendKey command buffer (if testing indicates).
c.	Add text box capability (if it works).
*/
def driverVer() { return "WS V2" }
import groovy.json.JsonOutput
metadata {
	definition (name: "Hubitat-Samsung Remote",
				namespace: "davegut",
				author: "David Gutheinz",
				importUrl: ""
			   ){
//		capability "Switch"
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
//		attribute "wsStatus", "string"
	}
	preferences {
		input ("deviceIp", "text", title: "Samsung TV Ip")
		input ("hubIp", "text", title: "NodeJs Hub IP")
		input ("hubPort", "text", title: "NodeJs Hub Port", defaultValue: "8080")
		def tvModes = ["TV", "HDMI", "AMBIENT", "ART_MODE"]
		input ("tvPwrOnMode", "enum", title: "TV Startup Display", options: tvModes)
		input ("debug", "bool",  title: "Enable debug logging", defaultValue: false)
		input ("info", "bool",  title: "Enable description text logging", defaultValue: false)
	}
}
def installed() {
	state.token = "12345678"
	updateDataValue("name", "Hubitat Samsung Remote")
	updateDataValue("name64", "Hubitat Samsung Remote".encodeAsBase64().toString())
}
def updated() {
	logInfo("updated: get device data and model year")
	unschedule()
	def tokenSupport = getDeviceData()
	runIn(2, checkInstall)
}
def getDeviceData() {
	logInfo("getDeviceData: Updating Device Data.")
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
//		logDebug("getDeviceData: year = $modelYear, frameTv = $frameTv, tokenSupport = $tokenSupport")
		logTrace("getDeviceData: year = $modelYear, frameTv = $frameTv, tokenSupport = $tokenSupport")
	}
	return tokenSupport
}

def checkInstall() {
	logInfo("<b>Performing test using tokenSupport = ${getDataValue("tokenSupport")}")
	getDeviceData()
	pauseExecution(2000)
	connect()
	pauseExecution(10000)
	menu()
	pauseExecution(2000)
	close()
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
}

//	===== TV Source Modes
def toggleArt() {
	if (getDataValue("frameTv") == "true") {
		sendKey("POWER")
	} else {
		logDebug("toggleArt only works for Frame TV's")
	}
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
def sendWsCmd(command, data = "") {
//	logDebug("sendWsCmd: ${command} | ${data}")
	logTrace("sendWsCmd: ${command} | ${data}")
	def name = getDataValue("name64")
	def token = state.token
	def url = "wss://${deviceIp}:8002/api/v2/channels/samsung.remote.control?name=${name}&token=${token}"
	if (getDataValue("tokenSupport") == "true") {
		def headers = [HOST: "${hubIp}:${hubPort}", command: command, data: data, url: url]
		sendHubCommand(new hubitat.device.HubAction([headers: headers],
												device.deviceNetworkId,
												[callback: wsHubParse]))
	} else {
		switch(command) {
			case "connect":
				url = "ws://${deviceIp}:8001/api/v2/channels/samsung.remote.control?name=${name}"
				interfaces.webSocket.connect(url)
				break
			case "sendMessage":
				interfaces.webSocket.sendMessage(data)
				break
			case "close":
				interfaces.webSocket.close()
				break
			default:
				logWarn("sendWsCmd: Invalid command")
		}
	}
}
def wsHubParse(response) {
	//	===== NodeJs Return Parse =====
	def command = response.headers.command
	def cmdResponse = response.headers.cmdResponse
	def hubStatus = response.headers.hubStatus
	def wsDeviceStatus = response.headers.wsDeviceStatus
	def data = "command = ${command} || hubStatus = ${hubStatus} || "
	data += "wsDeviceStatus = ${wsDeviceStatus} || cmdResponse = ${cmdResponse}"
//	logDebug("wsHubParse: ${data}")
	logTrace("wsHubParse: ${data}")
	if (cmdResponse == "{}") { return }
	//	===== Check connect response for token update.
	def resp = parseJson(cmdResponse)
	def respData = parseJson(resp.cmdData)
	def newToken = respData.data.token
	if (newToken != state.token && newToken) {
//		logDebug("wsHubParse: token updated to ${newToken}")
		logTrace("wsHubParse: token updated to ${newToken}")
		state.token = newToken
	}
}
def parse(message) {
	logTrace("parse: ${message}")
}
def webSocketStatus(message) {
	logTrace("webSocketStatus: ${message}")
}

//	===== Wake On Lan =====
def wakeOnLan() {
	def mac = getDataValue("deviceMac").replaceAll(":","").replaceAll("-","")
	logDebug("wakeOnLan: sending WOL packet to ${mac}")
    def result = new hubitat.device.HubAction (
		"wake on lan ${mac}", hubitat.device.Protocol.LAN, null)
}

//	===== Logging =====
def logTrace(msg) { log.trace "${driverVer()} || ${msg}" }
def logInfo(msg) { 
	if (info == true) {
		log.info "${driverVer()} || ${msg}"
	}
}
def logDebug(msg) {
	if (debug == true) {
		log.debug "${driverVer()} || ${msg}"
	}
}
def logWarn(msg) { log.warn "${driverVer()} || ${msg}" }