/*	===== HUBITAT INTEGRATION VERSION =====================================================
UpNp Media Player and MainTVAgent Discovery
This is a test application to discover UPnP devices related to Media Player
as well as Samsung devices with MainTvAgent2.  It does a discovery process
and then identifies the devices by name, IP, and Port into two state:
Media Player and MainTvAgent2
===== HUBITAT INTEGRATION VERSION =======================================================*/
//import org.json.JSONObject
def appVersion() { return "TEST" }
def appName() { return "UPnP Identification" }
definition(
	name: "UPnP Identification",
	namespace: "davegut",
	author: "Dave Gutheinz",
	description: "Application to identify Media Players.",
	category: "Convenience",
	iconUrl: "",
	iconX2Url: "",
	singleInstance: true,
	importUrl: ""
)
preferences {
	page(name: "mainPage")
	page(name: "discovery")
}

//	===== Page Definitions =====
def mainPage() {
	setInitialStates()
	ssdpSubscribe()
	def page1 = "0.  Turn on devices you wish to check.\n"
	page1 += "1.  Press 'Next' to find UPnP devices.\n"
	page1 += "2.  Select '<< App List' to return.\n"
	page1 += "3.  When done, open the App info page (gear icon), Application States.\n"
	page1 += "    a.  mediaPlayers contains the Media Player data.\n"
	page1 += "    b.  mainTvAgents contains the tv agent devices.\n"
	page1 += "    c.  remoteControlRxs contains th remote control receiver devices."
	return dynamicPage(
		name: "mainPage",
		title: "UPnP Identification", 
		nextPage: "discovery",
		install: false,
		uninstall: true){
		section(page1) {}
	}
}
def discovery() {
	def mrPlayers = "Device Name\tModel\t\tIPAddress"
	def mediaPlayers = state.mediaPlayers
	def mpCount = 0
	mediaPlayers.each {
		if (it.value.name) {
			mrPlayers += "\n${it.value.name.padRight(15)}\t${it.value.model.padRight(15)}\t${it.value.ip}"
			mpCount += 1
		}
	}

	def tvAgents = "Device Name\tModel\t\tIPAddress"
	def mainTvAgents = state.mainTvAgents
	def tvCount = 0
	mainTvAgents.each {
		if (it.value.name) {
			tvAgents += "\n${it.value.name.padRight(15)}\t${it.value.model.padRight(15)}\t${it.value.ip}"
			tvCount += 1
		}
	}

	def rcReceivers = "Device Name\tModel\t\tIPAddress"
	def remoteControlRxs = state.remoteControlRxs
	def rcrCount = 0
	remoteControlRxs.each {
		if (it.value.name) {
			rcReceivers += "\n${it.value.name.padRight(15)}\t${it.value.model.padRight(15)}\t${it.value.ip}"
			rcrCount += 1
		}
	}

	ssdpDiscover()
	def text2 = "Please wait while we discover your devices. Discovery can take "+
				"several minutes\n\r\n\r"
	return dynamicPage(
		name: "discovery", 
		title: "Device Discovery",
		nextPage: "", 
		refreshInterval: 10, 
		install: true, 
		uninstall: true){
			section() {
				paragraph "<b>${mpCount} Media Player Devices</b>"
				paragraph "<textarea rows=${mpCount + 1} cols=45 readonly='true'>${mrPlayers}</textarea>"
				paragraph "<b>${tvCount} TV Agent Devices</b>"
				paragraph "<textarea rows=${tvCount + 1} cols=45 readonly='true'>${tvAgents}</textarea>"
				paragraph "<b>${rcrCount} Remote Control Receiver Devices</b>"
				paragraph "<textarea rows=${rcrCount + 1} cols=45 readonly='true'>${rcReceivers}</textarea>"
			}
		section("Select App List (top left) to exit") {}
	}
}

//	===== Start up Functions =====
def setInitialStates() {
	state.mediaPlayers = [:]
	state.mainTvAgents = [:]
	state.remoteControlRxs = [:]
}
def installed() { initialize() }
def updated() { initialize() }
def initialize() { unschedule() }

//	===== Device Discovery =====
void ssdpSubscribe() {
	unsubscribe()
	subscribe(location, "ssdpTerm.urn:schemas-upnp-org:device:MediaRenderer:1", mrHandler)
	subscribe(location, "ssdpTerm.urn:samsung.com:device:MainTVServer2:1", tvHandler)
	subscribe(location, "ssdpTerm.urn:samsung.com:device:RemoteControlReceiver:1", rcrHandler)
}
void ssdpDiscover() {
	sendHubCommand(new hubitat.device.HubAction("lan discovery urn:schemas-upnp-org:device:MediaRenderer:1", hubitat.device.Protocol.LAN))
	sendHubCommand(new hubitat.device.HubAction("lan discovery urn:samsung.com:device:MainTVServer2:1", hubitat.device.Protocol.LAN))
	sendHubCommand(new hubitat.device.HubAction("lan discovery urn:samsung.com:device:RemoteControlReceiver:1", hubitat.device.Protocol.LAN))
	runIn(5, addDeviceData)
}
def mrHandler(evt) {
	def parsedEvent = parseLanMessage(evt.description)
	def dni = parsedEvent.mac
	def mediaPlayers = state.mediaPlayers
	
	def device = [:]
	device["ip"] = convertHexToIP(parsedEvent.networkAddress)
	device["ssdpPort"] = convertHexToInt(parsedEvent.deviceAddress)
	device["ssdpPath"] = parsedEvent.ssdpPath
	mediaPlayers << ["${dni}": device]
}
def tvHandler(evt) {
	def parsedEvent = parseLanMessage(evt.description)
	def dni = parsedEvent.mac
	def mainTvAgents = state.mainTvAgents
	
	def device = [:]
	device["ip"] = convertHexToIP(parsedEvent.networkAddress)
	device["ssdpPort"] = convertHexToInt(parsedEvent.deviceAddress)
	device["ssdpPath"] = parsedEvent.ssdpPath
	mainTvAgents << ["${dni}": device]
}
def rcrHandler(evt) {
	def parsedEvent = parseLanMessage(evt.description)
	def dni = parsedEvent.mac
	def remoteControlRxs = state.remoteControlRxs
	
	def device = [:]
	device["ip"] = convertHexToIP(parsedEvent.networkAddress)
	device["ssdpPort"] = convertHexToInt(parsedEvent.deviceAddress)
	device["ssdpPath"] = parsedEvent.ssdpPath
	remoteControlRxs << ["${dni}": device]
}

def addDeviceData() {
	def players = state.mediaPlayers.findAll { !it?.value?.model }
	players.each {
	 	sendCmd(it.value.ssdpPath, it.value.ip, it.value.ssdpPort, "addPlayerData")
	}
	def tvAgents = state.mainTvAgents.findAll { !it?.value?.model }
	players.each {
	 	sendCmd(it.value.ssdpPath, it.value.ip, it.value.ssdpPort, "addTvAgentData")
	}
	def receivers = state.remoteControlRxs.findAll { !it?.value?.model }
	players.each {
	 	sendCmd(it.value.ssdpPath, it.value.ip, it.value.ssdpPort, "addReceiverData")
	}
}
void addPlayerData(resp) {
	def mediaPlayers = state.mediaPlayers
	def device = mediaPlayers.find {it?.key?.contains("${resp.mac}")}
	if (device) {
		device.value << [model: "${resp.xml.device.modelName}",
						  name: "${resp.xml.device.friendlyName}"]
	 }
}
void addTvAgentData(resp) {
	def mainTvAgents = state.mainTvAgents
	def device = mainTvAgents.find {it?.key?.contains("${resp.mac}")}
	if (device) {
		device.value << [model: "${resp.xml.device.modelName}",
						  name: "${resp.xml.device.friendlyName}"]
	 }
}
void addReceiverData(resp) {
	def remoteControlRxs = state.remoteControlRxs
	def device = remoteControlRxs.find {it?.key?.contains("${resp.mac}")}
	if (device) {
		device.value << [model: "${resp.xml.device.modelName}",
						  name: "${resp.xml.device.friendlyName}"]
	 }
}

private sendCmd(command, deviceIP, devicePort, action){
    def host = "${deviceIP}:${devicePort}"
    def sendCmd =sendHubCommand(new hubitat.device.HubAction("""GET ${command} HTTP/1.1\r\nHOST: ${host}\r\n\r\n""",
															 hubitat.device.Protocol.LAN, host, [callback: action]))
}
def logWarn(message) {
	log.warn "${appName()} ${appVersion()}: ${message}"
}
def logInfo(message) {
	log.info "${appName()} ${appVersion()}: ${message}"
}
//	----- Utility Functions  SHOULD DISAPPEAR-----
private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}
private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}