/*	===== HUBITAT INTEGRATION VERSION =====================================================
UpNp Media Player and MainTVAgent Discovery
This is a test application to discover UPnP devices related to Media Player
as well as Samsung devices with MainTvAgent2.  It does a discovery process
and then identifies the devices by name, IP, and Port into two state:
Media Player and MainTvAgent2
===== HUBITAT INTEGRATION VERSION =======================================================*/
//import org.json.JSONObject
def appVersion() { return "TEST" }
def appName() { return "UPnP TV Test" }
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
	logInfo("mainPage")
	setInitialStates()
	ssdpSubscribe()
	def page1 = "0.  Turn on devices you wish to check for at least 1 minute.\n"
	page1 += "1.  Press 'Next' to find UPnP devices.\n"
	page1 += "2.  When done, open the App info page (gear icon), Application States.\n"
	page1 += "    I need the <b>devices</b> data from the states section.\n"
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
	logInfo("discovery")
	def devices = state.devices
	def devList = ""
	devices.each {
		devList += "${it}\n\n"
	}
	ssdpDiscover()
	def text2 = "<b>Allow at least two minutes to discover your devices</b>\n\r\n\r"
	return dynamicPage(
		name: "discovery", 
		title: "Device Discovery",
		nextPage: "", 
		refreshInterval: 10, 
		install: true, 
		uninstall: true){
			section("<b>Allow at least 2 minutes for discovery</b>") {
				paragraph "<b>UPnP Devices Discovery Data</b>"
				paragraph "<textarea rows=30 cols=50 readonly='true'>${devList}</textarea>"
			}
	}
}

//	===== Start up Functions =====
def setInitialStates() {
	state.ssdpDevices = [:]
	state.devices = [:]
}
def installed() { initialize() }
def updated() { initialize() }
def initialize() { unschedule() }

//	===== Device Discovery =====
void ssdpSubscribe() {
	logInfo("ssdpSubscribe")
	unsubscribe()
	subscribe(location, "ssdpTerm.urn:schemas-upnp-org:device:MediaRenderer:1", ssdpHandler)
	subscribe(location, "ssdpTerm.urn:samsung.com:device:MainTVServer2:1", ssdpHandler)
}
void ssdpDiscover() {
	logInfo("ssdpDiscover")
	sendHubCommand(new hubitat.device.HubAction("lan discovery urn:schemas-upnp-org:device:MediaRenderer:1", hubitat.device.Protocol.LAN))
	pauseExecution(1000)
	sendHubCommand(new hubitat.device.HubAction("lan discovery urn:samsung.com:device:MainTVServer2:1", hubitat.device.Protocol.LAN))
	pauseExecution(1000)
	getDevData()
}
def ssdpHandler(evt) {
	def parsedEvent = parseLanMessage(evt.description)
	def ip = convertHexToIP(parsedEvent.networkAddress)
	def path = parsedEvent.ssdpPath
	def port = convertHexToInt(parsedEvent.deviceAddress)
	def key = "${ip}:${path}${port}"

	def devices = state.devices
	device = [:]
	device["dni"] = parsedEvent.mac
	device["ip"] = ip
	devices << ["${parsedEvent.mac}": device]
	logInfo("ssdpHandler: found device dni = ${parsedEvent.mac}")

	def ssdpDevices = state.ssdpDevices
	ssdpDevice = [:]
	ssdpDevice["ip"] = ip
	ssdpDevice["port"] = port
	ssdpDevice["path"] = path
	ssdpDevices << ["${key}": ssdpDevice]
}

def getDevData() {
	def ssdpDevices = state.ssdpDevices
	ssdpDevices.each {
		logInfo("getDeviceData: path = ${it.value.ip}:${it.value.port}${it.value.path}")
		sendCmd(it.value.path, it.value.ip, it.value.port, "addDeviceData")
		pauseExecution(300)
	}
}
void addDeviceData(resp) {
	def devices = state.devices
	def device = devices.find { it.key == resp.mac }
	if (device) {
		def type = resp.xml.device.deviceType
		def port = convertHexToInt(resp.port)
		device.value << [manufacturer: "${resp.xml.device.manufacturer.text()}"]
		def services = resp.xml.device.serviceList.service
		services.each {
			if (it.serviceType.text() == "urn:samsung.com:service:MainTVAgent2:1") {
				device.value << [tvPort: "${port}",
								 tvUrn: "${it.serviceType.text()}",
								 tvPath: "${it.controlURL.text()}"]
			} else if (it.serviceType.text() == "urn:schemas-upnp-org:service:AVTransport:1") {
				device.value << [avPort: "${port}", 
								 avUrn: "${it.serviceType.text()}",
								 avPath: "${it.controlURL.text()}"]
			} else if (it.serviceType.text() == "urn:schemas-upnp-org:service:RenderingControl:1") {
				device.value << [rcPort: "${port}",
								 rcUrn: "${it.serviceType.text()}",
								 rcPath: "${it.controlURL.text()}"]
			}
		}
	}
	logInfo("addDeviceData: found data for device ${resp.mac}")
}
private sendCmd(command, deviceIP, devicePort, action){
	def host = "${deviceIP}:${devicePort}"
    sendHubCommand(new hubitat.device.HubAction("""GET ${command} HTTP/1.1\r\nHOST: ${host}\r\n\r\n""",
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