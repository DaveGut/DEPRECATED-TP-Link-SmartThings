/*
TP-Link SmartThings Manager and TP-Link Cloud Connect, 2018 Version 4

Lite version with less information and no gitHub Icons.

	Copyright 2019 Dave Gutheinz, Anthony Ramirez
    
Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at:

	http://www.apache.org/licenses/LICENSE-2.0
    
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License.

Discalimer: This Service Manager and the associated Device
Handlers are in no way sanctioned or supported by TP-Link. All
development is based upon open-source data on the TP-Link Kasa Devices;
primarily various users on GitHub.com.
	====== Application History ================================
11-14-2018	Finalized version 3.5 for public release.  Changes
			a.  Added support for hub-based device installation and
            	management.
            b.	Added capability to set device preferences (not
            	available in new phone app).
            c.	Added capability to remove devices.
            d.	Reworked HMI and improved information.
12-03-2018	Updated to finalize multi-plug integration and
			(attempt) to ease login issues.
	====== Application Information ==========================*/
    def traceLogging() { return true }
//	def traceLogging() { return false }
	def appVersion() { return "3.6.01" }
	def driverVersion() { return "3.6" }
    def hubVersion() { return "3.6.01" }
//	===========================================================

definition (
	name: "UPDATE TP-Link SmartThings Manager (lite)", 
	namespace: "davegut", 
	author: "Dave Gutheinz, Anthony Ramirez", 
	description: "SmartThings TP-Link/Kasa Service Manager.", 
	category: "Convenience", 
	iconUrl: "https://raw.githubusercontent.com/davegut/TP-Link-SmartThings/master/images/kasa.png",
	iconX2Url: "https://raw.githubusercontent.com/davegut/TP-Link-SmartThings/master/images/kasa.png",
	iconX3Url: "https://raw.githubusercontent.com/davegut/TP-Link-SmartThings/master/images/kasa.png",
	singleInstance: true
)

preferences {
	page(name: "startPage")
	page(name: "welcomePage")
	page(name: "hubEnterIpPage")
	page(name: "kasaAuthenticationPage")
    page(name: "addDevicesPage")
	page(name: "removeDevicesPage")
	page(name: "devicePreferencesPage")
    page(name: "listDevicesPage")
	page(name: "flowDirector")
}

def setInitialStates() {
	if (!state.TpLinkToken) { state.TpLinkToken = null }
	if (!state.devices) { state.devices = [:] }
	if (!state.currentError) { state.currentError = null }
	if (!state.errorCount) { state.errorCount = 0 }
    if (!state.hubVersion) { state.hubVersion = null }
	state.flowType = "default"
}

//	===== Pages =====================
def startPage() {
	traceLog("startPage: installType = ${installType}")
	setInitialStates()
	if (installType) {
		if (installType == "Kasa Account" && !userName) {
			return kasaAuthenticationPage()       	
		} else if (installType == "Node Applet" && !bridgeIp) {
			return hubEnterIpPage()
        } else {
        	return welcomePage()
        }
	}

    def page1Text = ""
	def page2Text = ""
    page1Text += "Kasa Account: Kasa cloud based integration requiring your Kasa account credentials."
    page2Text += "Node Applet: node.js applet based integration requiring a stand-alone server in you house."

	return dynamicPage (name: "startPage", title: "Select Installation Type", uninstall: true) {
		errorSection()
 		section("Instructions", hideable: true, hidden: false) {
            paragraph page1Text
            paragraph page2Text
		}
		section("") {
			input ("installType", "enum", 
            	title: "Select Installation Type", 
                required: true, 
                multiple: false,
                submitOnChange: true,
				options: ["Kasa Account", "Node Applet"])
            if (installType == "Kasa Account") {
            	return kasaAuthenticationPage()
            } else if (installType == "Node Applet") {
            	return hubEnterIpPage()
            }
 		}
		copyRightSection()
	}
}

def welcomePage() {
	if (installType == "Kasa Account") {
    	kasaGetDevices()
    } else {
		hubCheck()
    	runIn(5,hubGetDevices)
    }
	app.deleteSetting("selectedAddDevices")
	app.deleteSetting("selectedDeleteDevices")
	app.deleteSetting("selectedUpdateDevices")
	app.deleteSetting("userLightTransTime")
	app.deleteSetting("userRefreshRate")

	return dynamicPage (name: "welcomePage", title: "Welcome to TP-Link Service Manager", uninstall: true) {
		errorSection()

		section("Device Management Functions", hideable: true) {
        	href "addDevicesPage", title: "Install Kasa Devices", description: "Go to Install Devices"
 			href "removeDevicesPage", title: "Remove Installed Kasa Devices", description: "Go to Remove Devices"
			href "devicePreferencesPage", title: "Update Device Preferences", description: "Go to Set Device Preferences"
            if (installType == "Kasa Account") {
				href "kasaAuthenticationPage", title: "Kasa Login and Token Update", description: "Go to Kasa Login Update"
            }	else {
				href "hubEnterIpPage", title: "Node.js Hub IP = ${bridgeIp}", description: "Update Node.js Hub and Device IPs"
			}
			href "listDevicesPage", title: "List Drivers and Programs", description: "List Drivers and Programs"
		}

		copyRightSection()
	}
}

def kasaAuthenticationPage() {
	return dynamicPage (name: "kasaAuthenticationPage", title: "Initial Kasa Login Page", install: true) {
            
        errorSection()
        
		state.flowType = "updateKasaToken"
		section("Enter Kasa Account Credentials: ") {
			input ("userName", "email", title: "TP-Link Kasa Email Address", required: true, submitOnChange: true)
			input ("userPassword", "password", title: "Kasa Account Password", required: true, submitOnChange: true)
			if (userName && userPassword) { paragraph "To update Kasa Account, select SAVE in upper right-hand corner." }
			href "welcomePage", title: "Return to Main Page without Logging ON", description: "Don't Log onto Kasa Cloud."
		}
	}
}

def hubEnterIpPage() {
	return dynamicPage (name: "hubEnterIpPage", title: "Set/Update Node IP", install: true) {
        
        errorSection()
            
		state.flowType = "updateNodeIp"
		section("") {
			input ("bridgeIp", "text", title: "Enter the Node.js Hub IP", required: true, multiple: false, submitOnChange: true)
            if (bridgeIp) { paragraph "To update IPs, select SAVE in upper right-hand corner." }
			href "welcomePage", title: "Return to Main Page without changing or updating IPs", description: "Don't Update IPs"
		}
	}
}

def addDevicesPage() {
	def devices = state.devices
	def errorMsgDev = null
	def newDevices = [:]
	devices.each {
		def isChild = getChildDevice(it.value.deviceNetworkId)
		if (!isChild) {
			newDevices["${it.value.deviceNetworkId}"] = "${it.value.alias} model ${it.value.deviceModel}"
		}
	}
	if (devices == [:]) {
		errorMsgDev = "Looking for devices.  If this message persists, we have been unable to find " +
        "TP-Link devices on your wifi.  Check: 1) Hubitat Environment logs, 2) node.js logfile."
	} else if (newDevices == [:]) {
		errorMsgDev = "No new devices to add. Are you sure they are in Remote Control Mode?"
	}

	return dynamicPage (name: "addDevicesPage", title: "Add Kasa Devices", install: true) {
        
        errorSection()

  		section("Select Devices to Add (${newDevices.size() ?: 0} found)", hideable: true, hidden: false) {
			input ("selectedAddDevices", "enum", required: true, multiple: true, submitOnChange: true, title: null, options: newDevices)
 			if (selectedAddDevices) { paragraph "To add devices, select SAVE in upper right-hand corner." }
			href "welcomePage", title: "Return to Main Page without adding devices", description: "Don't Add Devices"
        }
	}
}

def removeDevicesPage() {
	def devices = state.devices
	def errorMsgDev = null
	def oldDevices = [:]
	devices.each {
		def isChild = getChildDevice(it.value.deviceNetworkId)
		if (isChild) {
			oldDevices["${it.value.deviceNetworkId}"] = "${it.value.alias} model ${it.value.deviceModel}"
		}
	}
	if (devices == [:]) {
		errorMsgDev = "Devices database was cleared in-error.  Run Device Installer Page to correct " +
        "then try again.  You can also remove devices using the Environment app."
	}
	if (oldDevices == [:]) {
		errorMsgDev = "There are no devices to remove from the SmartThings app at this time.  This " +
        "implies no devices are installed."
	}
        
	return dynamicPage (name: "removeDevicesPage", title: "Kasa Device Uninstall", install: true) {
        
        errorSection()
        
		section("Select Devices to Remove (${oldDevices.size() ?: 0} found)", hideable: true) {
			input ("selectedDeleteDevices", "enum", required: true, multiple: true, submitOnChange: true, title: null, options: oldDevices)
 			if (selectedDeleteDevices) { paragraph "To remove devices, select SAVE in upper right-hand corner." }
			href "welcomePage", title: "Return to Main Page without removing devices", description: "Don't Remove Devices"
		}
	}
}

def devicePreferencesPage() {
	def devices = state.devices
	def oldDevices = [:]
	devices.each {
		def isChild = getChildDevice(it.value.deviceNetworkId)
		if (isChild) {
			oldDevices["${it.value.deviceNetworkId}"] = "${it.value.alias} model ${it.value.deviceModel}"
		}
	}

	return dynamicPage (name: "devicePreferencesPage", title: "Device Preferences Page", install: true) {
        
        errorSection()
        
		section("Device Configuration: ") {
			input ("selectedUpdateDevices", "enum",
            	required: false,
                multiple: true,
                submitOnChange: true,
                title: "Select Devices to Update (${oldDevices.size() ?: 0} found)",
	            options: oldDevices)
			input ("userLightTransTime", "number", 
            	required: false, 
                multiple: false, 
	            submitOnChange: false,
                title: "Lighting Transition Time in Seconds (Bulbs Only)")
			input ("userRefreshRate", "enum", 
            	required: false, 
                multiple: false,
                submitOnChange: false,
                title: "Device Refresh Rate",
                metadata: [values:["1" : "Refresh every minute", 
               					   "5" : "Refresh every 5 minutes", 
                                   "10" : "Refresh every 10 minutes", 
                                   "15" : "Refresh every 15 minutes"]])
			if (selectedUpdateDevices) { paragraph "To update devices, select SAVE in upper right-hand corner." }
			href "welcomePage", title: "Return to Main Page without changing preferences", description: "Don't Update Preferences"
		}
	}
}

def listDevicesPage() {
	def devices = state.devices
	def errorMsgDev = null
	def kasaDevices = [:]
    kasaDevices["Label"] = "  Model : Installed : Driver\n\r"
	devices.each {
    	def installed = "No"
        def devHandler = ""
		def isChild = getChildDevice(it.value.deviceNetworkId)
		if (isChild) {
        	installed = true
            devHandler = isChild.devVer()
		}
        kasaDevices["${it.value.alias}"] = " ${it.value.deviceModel} : ${installed} : ${devHandler}\n\r"
	}
	if (devices == [:]) {
		errorMsgDev = "Devices database was cleared in-error.  Run Device Installer Page to correct " +
        "then try again.  You can also remove devices using the Environment app."
	}
	if (kasaDevices == [:]) {
		errorMsgDev = "There are no devices to remove from the SmartThings app at this time.  This " +
        "implies no devices are installed."
	}
        
	return dynamicPage (name: "listDevicesPage", title: "List of Kasa Devices and Handlers", install: false) {
        
        errorSection()
        
		section("Kasa Devices and Device Handlers", hideable: true) {
        	if (installType == "Node Applet") {
            	paragraph "Recommended Node App: ${hubVersion()} / Actual ${state.hubVersion}"
            }
			paragraph "Recommended Minimum Driver: ${driverVersion()}\n\r\n\r${kasaDevices}"
			href "welcomePage", title: "Return to Main Page", description: "Return to Main Page"
		}
	}
}

def copyRightSection() {
	section("Copyright Dave Gutheinz and Anthony Rameriz", hideable: true) {
		paragraph "TP-Link/Kasa user created application."
		paragraph "Application Version: ${appVersion()}"
		paragraph "Recommended Driver Version: ${driverVersion()} and above"
	}
}

def errorSection() {
	section("") {
		if (state.currentError != null) {
			paragraph "ERROR:  ${state.currentError}! Correct before continuing."
		} else if (errorMsgDev != null) {
			paragraph "ERROR:  ${errorMSgDev}"
		} else {
			paragraph "No detected program errors!"
		}
	}
}

//	===== Action Methods ===========
def flowDirector() {
	traceLog("flowDirector ${state.flowType}")
	switch(state.flowType) {
		case "updateNodeIp":
            hubGetDevices()
			break
		case "updateKasaToken":
	        getToken()
			break
        default:
        	break
    }
	state.flowType = "default"
}

def addDevices() {
	traceLog("addDevices ${selectedAddDevices}")
	def tpLinkModel = [:]
	//	Plug-Switch Devices (no energy monitor capability)
	tpLinkModel << ["HS100" : "TP-Link Smart Plug"]
	tpLinkModel << ["HS103" : "TP-Link Smart Plug"]
	tpLinkModel << ["HS105" : "TP-Link Smart Plug"]
	tpLinkModel << ["HS200" : "TP-Link Smart Switch"]
	tpLinkModel << ["HS210" : "TP-Link Smart Switch"]
	tpLinkModel << ["KP100" : "TP-Link Smart Plug"]
	//	WiFi Range Extender with smart plug.
	tpLinkModel << ["RE270" : "TP-Link Smart Plug"]
	tpLinkModel << ["RE370" : "TP-Link Smart Plug"]
	//	Miltiple Outlet Plug
	tpLinkModel << ["HS107" : "TP-Link Smart Multi-Plug"]
	tpLinkModel << ["HS300" : "TP-Link Smart Multi-Plug"]
	tpLinkModel << ["KP200" : "TP-Link Smart Multi-Plug"]
	tpLinkModel << ["KP400" : "TP-Link Smart Multi-Plug"]
	//	Dimming Switch Devices
	tpLinkModel << ["HS220" : "TP-Link Smart Dimming Switch"]
	//	Energy Monitor Plugs
	tpLinkModel << ["HS110" : "TP-Link Smart Energy Monitor Plug"]
	//	Soft White Bulbs
	tpLinkModel << ["KB100" : "TP-Link Smart Soft White Bulb"]
	tpLinkModel << ["LB100" : "TP-Link Smart Soft White Bulb"]
	tpLinkModel << ["LB110" : "TP-Link Smart Soft White Bulb"]
	tpLinkModel << ["KL110" : "TP-Link Smart Soft White Bulb"]
	tpLinkModel << ["LB200" : "TP-Link Smart Soft White Bulb"]
	//	Tunable White Bulbs
	tpLinkModel << ["LB120" : "TP-Link Smart Tunable White Bulb"]
	tpLinkModel << ["KL120" : "TP-Link Smart Tunable White Bulb"]
	//	Color Bulbs
	tpLinkModel << ["KB130" : "TP-Link Smart Color Bulb"]
	tpLinkModel << ["LB130" : "TP-Link Smart Color Bulb"]
	tpLinkModel << ["KL130" : "TP-Link Smart Color Bulb"]
	tpLinkModel << ["LB230" : "TP-Link Smart Color Bulb"]

		def hub = location.hubs[0]
		def hubId = hub.id
	selectedAddDevices.each { dni ->
		try {
			def isChild = getChildDevice(dni)
			if (!isChild) {
				def device = state.devices.find { it.value.deviceNetworkId == dni }
				def deviceModel = device.value.deviceModel
			def deviceData
			if (installType == "Kasa Account") {
				deviceData = [
					"installType" : installType,
					"deviceId" : device.value.deviceId,
					"plugId" : device.value.plugId,
					"appServerUrl" : device.value.appServerUrl,
					"appVersion" : appVersion()
				]
			} else {
				deviceData = [
					"installType" : installType,
					"deviceId" : device.value.deviceId,
					"plugId" : device.value.plugId,
					"deviceIP" : device.value.deviceIP,
					"gatewayIP" : bridgeIp,
					"appVersion" : appVersion()
				]
			}

				addChildDevice(
                	"davegut", 
                	tpLinkModel["${deviceModel}"],
                    device.value.deviceNetworkId,
                    hubId, [
                    	"label" : device.value.alias,
                    	"name" : deviceModel,
						"data" : deviceData
					]
                )
				log.info "Installed TP-Link ${deviceModel} ${device.value.alias}"
			}
		} catch (e) {
			log.debug "Error Adding ${deviceModel} ${device.value.alias}: ${e}"
		}
	}
    runIn(2, updateDeviceData)
}

def removeDevices() {
	traceLog("removeDevices ${selectedDeleteDevices}")
	selectedDeleteDevices.each { dni ->
		try{
			def isChild = getChildDevice(dni)
			if (isChild) {
				def delete = isChild
				delete.each { deleteChildDevice(it.deviceNetworkId, true) }
			}
		} catch (e) {
			log.debug "Error deleting ${it.deviceNetworkId}: ${e}"
		}
	}
}

def updatePreferences() {
	traceLog("updatePreferences ${selectedUpdateDevices}, ${userLightTransTime}, ${userRefreshRate}")
	selectedUpdateDevices.each {
		def child = getChildDevice(it)
        if (userLightTransTime) { child.setLightTransTime(userLightTransTime)}
        if (userRefreshRate) { child.setRefreshRate(userRefreshRate) }
		log.info "Kasa device ${child} preferences updated"
	}
}

def updateDevices(deviceNetworkId, alias, deviceModel, plugId, deviceId, appServerUrl, deviceIP) {
	def devices = state.devices
	def device = [:]
	device["deviceNetworkId"] = deviceNetworkId
	device["alias"] = alias
	device["deviceModel"] = deviceModel
	device["plugId"] = plugId
	device["deviceId"] = deviceId
	device["appServerUrl"] = appServerUrl
	device["deviceIP"] = deviceIP
	devices << ["${deviceNetworkId}" : device]
	def isChild = getChildDevice(deviceNetworkId)
	if (isChild) {
    	if (installType == "Kasa Account") {
	        isChild.setAppVersion(appVersion())
			isChild.setAppServerUrl(appServerUrl)
        } else {
	        isChild.setAppVersion(appVersion())
            isChild.setHubVersion(state.hubVersion)
			isChild.setDeviceIP(deviceIP)
			isChild.setGatewayIP(bridgeIp)
		}
    }
	log.info "Device ${alias} added to devices array"
}

//	===== Kasa Account Methods ===========
def getToken() {
	traceLog("getToken ${userName}, ${userPassword}")
	def hub = location.hubs[0]
	def cmdBody = [
		method: "login",
		params: [
			appType: "Kasa_Android",
			cloudUserName: "${userName}",
			cloudPassword: "${userPassword}",
			terminalUUID: "${hub.id}"
		]
	]
	def getTokenParams = [
		uri: "https://wap.tplinkcloud.com",
		requestContentType: 'application/json',
		contentType: 'application/json',
		headers: ['Accept':'application/json; version=1, */*; q=0.01'],
		body : new groovy.json.JsonBuilder(cmdBody).toString()
	]
	httpPostJson(getTokenParams) {resp ->
		if (resp.status == 200 && resp.data.error_code == 0) {
			state.TpLinkToken = resp.data.result.token
			log.info "TpLinkToken updated to ${state.TpLinkToken}"
			sendEvent(name: "TokenUpdate", value: "tokenUpdate Successful.")
			if (state.currentError != null) {
				state.currentError = null
			}
		} else if (resp.status != 200) {
			state.currentError = resp.statusLine
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in getToken: ${state.currentError}"
			sendEvent(name: "TokenUpdate", value: state.currentError)
		} else if (resp.data.error_code != 0) {
			state.currentError = resp.data
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in getToken: ${state.currentError}"
			sendEvent(name: "TokenUpdate", value: state.currentError)
		}
	}
}

def kasaGetDevices() {
	traceLog("kasaGetDevices ${state.TpLinkToken}")
	def currentDevices = ""
	def cmdBody = [method: "getDeviceList"]
	def getDevicesParams = [
		uri: "https://wap.tplinkcloud.com?token=${state.TpLinkToken}",
		requestContentType: 'application/json',
		contentType: 'application/json',
		headers: ['Accept':'application/json; version=1, */*; q=0.01'],
		body : new groovy.json.JsonBuilder(cmdBody).toString()
	]
	httpPostJson(getDevicesParams) {resp ->
		if (resp.status == 200 && resp.data.error_code == 0) {
			currentDevices = resp.data.result.deviceList
				state.currentError = null
		} else if (resp.status != 200) {
			state.currentError = resp.statusLine
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in getDeviceData: ${state.currentError}"
            return
		} else if (resp.data.error_code != 0) {
			state.currentError = resp.data
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in getDeviceData: ${state.currentError}"
            return
		}
	}
	state.devices = [:]
	currentDevices.each {
		def deviceModel = it.deviceModel.substring(0,5)
        def plugId = ""
        def deviceIP = ""
		if (deviceModel == "HS107" || deviceModel == "HS300") {
			def totalPlugs = 2
			if (deviceModel == "HS300") {
				totalPlugs = 6
			}
			for (int i = 0; i < totalPlugs; i++) {
				def deviceNetworkId = "${it.deviceMac}_0${i}"
				plugId = "${it.deviceId}0${i}"
				def sysinfo = sendDeviceCmd(it.appServerUrl, it.deviceId, '{"system" :{"get_sysinfo" :{}}}')
				def children = sysinfo.system.get_sysinfo.children
				def alias
				children.each {
					if (it.id == plugId) {
						alias = it.alias
					}
				}
                updateDevices(deviceNetworkId, alias, deviceModel, plugId, it.deviceId, it.appServerUrl, deviceIP)
			}
		} else {
            updateDevices(it.deviceMac, it.alias, deviceModel, plugId, it.deviceId, it.appServerUrl, deviceIP)
		}
	}
}

//	===== Node Applet Methods ============
def hubGetDevices() {
	traceLog("pollForDevices")
    hubSendCommand("pollForDevices")
}

def hubCheck() {
	traceLog("hubCheck")
    hubSendCommand("hubCheck")
}

def hubSendCommand(action) {
	traceLog("hubSendCommand ${bridgeIp}, ${action}")
	state.currentError = null
	runIn(10, createBridgeError)
	def headers = [:]
	headers.put("HOST", "${bridgeIp}:8082")	//	Same as on Hub.
	headers.put("command", action)
	sendHubCommand(new physicalgraph.device.HubAction([headers: headers], null, [callback: hubExtractData]))
}

def hubExtractData(response) {
	traceLog("hubExtractData")
    unschedule(createBridgeError)
	def action = response.headers["action"]
    if (action == "hubCheck") {
	    state.hubVersion = response.headers["cmd-response"]
    } else {
		def currentDevices =  parseJson(response.headers["cmd-response"])
	    if (currentDevices == []) {
	    	return 
	    } else if (currentDevices == "TcpTimeout") {
			log.error "Communications Error"
			sendEvent(name: "currentError", value: "TCP Timeout in Hub")
	        return
		}
		state.devices = [:]
		currentDevices.each {
		    def appServerUrl = ""
	        updateDevices(it.deviceMac, it.alias, it.deviceModel, it.plugId, it.deviceId, appServerUrl, it.deviceIP)
		}
    }
    log.info "Node Applet Hub Status: OK"
	state.currentError = null
	sendEvent(name: "currentError", value: null)
}

def createBridgeError() {
    log.error "Node Applet Bridge Status: Not Accessible"
	state.currentError = "Node Applet not acessible"
	sendEvent(name: "currentError", value: "Node Applet Not Accessible")
}

//	===== General Utility Methods ====
def sendDeviceCmd(appServerUrl, deviceId, command) {
	def cmdResponse = ""
	def cmdBody = [
		method: "passthrough",
		params: [
			deviceId: deviceId,
			requestData: "${command}"
		]
	]
	def sendCmdParams = [
		uri: "${appServerUrl}/?token=${state.TpLinkToken}",
		requestContentType: 'application/json',
		contentType: 'application/json',
		headers: ['Accept':'application/json; version=1, */*; q=0.01'],
		body : new groovy.json.JsonBuilder(cmdBody).toString()
	]
	httpPostJson(sendCmdParams) {resp ->
		if (resp.status == 200 && resp.data.error_code == 0) {
			def jsonSlurper = new groovy.json.JsonSlurper()
			cmdResponse = jsonSlurper.parseText(resp.data.result.responseData)
			if (state.errorCount != 0) {
				state.errorCount = 0
			}
			if (state.currentError != null) {
				state.currentError = null
				sendEvent(name: "currentError", value: null)
				log.debug "state.errorCount = ${state.errorCount}"
			}
		} else if (resp.status != 200) {
			state.currentError = resp.statusLine
			cmdResponse = "ERROR: ${resp.statusLine}"
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in sendDeviceCmd: ${state.currentError}"
		} else if (resp.data.error_code != 0) {
			state.currentError = resp.data
			cmdResponse = "ERROR: ${resp.data.msg}"
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in sendDeviceCmd: ${state.currentError}"
		}
	}
	return cmdResponse
}

def checkError() {
	if (state.currentError == null || state.currentError == "none") {
		log.info "No errors detected."
		if (state.currentError == "none") {
			state.currentError = null
		}
		return
	}
	def errMsg = state.currentError.msg
	log.info "Attempting to solve error: ${errMsg}"
	state.errorCount = state.errorCount +1
	if (errMsg == "Token expired" && state.errorCount < 6) {
		sendEvent (name: "ErrHandling", value: "Handle comms error attempt ${state.errorCount}")
		getDevices()
		if (state.currentError == null) {
			log.info "getDevices successful. apiServerUrl updated and token is good."
			return
		}
		log.error "${errMsg} error while attempting getDevices. Will attempt getToken"
		getToken()
		if (state.currentError == null) {
			log.info "getToken successful. Token has been updated."
			getDevices()
			return
		}
	} else {
		log.error "checkError: No auto-correctable errors or exceeded Token request count."
	}
	log.error "checkError residual: ${state.currentError}"
}

//	===== Generic Utility Methods ========
def installed() { initialize() }

def updated() { initialize() }

def initialize() {
	traceLog("initialize ${state.flowType}")
	unsubscribe()
	unschedule()
	if (installType == "Kasa Account"){
		schedule("0 30 2 ? * WED", getToken)
		runEvery5Minutes(checkError)
    } else if (installType == "Node Applet") { runEvery15Minutes(hubGetDevices) }
	if (selectedAddDevices) {
    	addDevices()
    } else if (selectedDeleteDevices) {
    	removeDevices()
    } else if (selectedUpdateDevices) {
    	updatePreferences()
    } else {
    	flowDirector()
    }
}

def uninstalled() { }

def traceLog(logMsg) {
	if (traceLogging() == true) { log.trace "${logMsg}" }
}

//end-of-file