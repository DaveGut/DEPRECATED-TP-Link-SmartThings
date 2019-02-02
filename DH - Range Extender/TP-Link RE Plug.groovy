/*
TP-Link Plug and Switch Device Handler, 2018, Version 3

	Copyright 2018 Dave Gutheinz and Anthony Ramirez

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this  file except in compliance with the
License. You may obtain a copy of the License at:

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an 
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific 
language governing permissions and limitations under the License.

Discalimer:  This Service Manager and the associated Device Handlers are in no way sanctioned or supported by TP-Link.  
All  development is based upon open-source data on the TP-Link devices; primarily various users on GitHub.com.

========= History ============================================
02.02.19	Created this special version for the Range Extender Plug (RE270, RE370) for which I could not find the
			control mechanism.  This Device Handler requires an IFTT account to turn the device on and off and then
            gets the device state from the Kasa Account or Cloud.
======== DO NOT EDIT LINES BELOW ===*/
//	======================================================================================================================
def devVer()	{ return "3.6.01" }
metadata {
	definition (name: "TP-Link Smart RE Plug", 
    			namespace: "davegut", 
                author: "Dave Gutheinz, Anthony Ramirez", 
                ocfDeviceType: "oic.d.smartplug", 
                mnmn: "SmartThings", 
                vid: "generic-switch") {
		capability "Switch"
		capability "refresh"
		capability "Health Check"
	}
	tiles(scale: 2) {
		multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action: "switch.off", icon: "st.Appliances.appliances17", backgroundColor: "#00a0dc",
				nextState: "off"
				attributeState "off", label:'${name}', action: "switch.on", icon: "st.Appliances.appliances17", backgroundColor: "#ffffff",
				nextState: "on"
			}
		}
		standardTile("refresh", "capability.refresh", width: 2, height: 1, decoration: "flat") {
			state "default", label: "Refresh", action: "refresh.refresh"
		}
		main("switch")
		details("switch", "refresh")
	}
    def refreshRate = [:]
    refreshRate << ["1" : "Refresh every minute"]
    refreshRate << ["5" : "Refresh every 5 minutes"]
	refreshRate << ["10" : "Refresh every 10 minutes"]
    refreshRate << ["15" : "Refresh every 15 minutes"]
	preferences {
		input ("refresh_Rate", "enum", title: "Device Refresh Rate", options: refreshRate)
		input ("device_IP", "text", title: "Device IP (Hub Only, NNN.NNN.N.NNN)")
		input ("gateway_IP", "text", title: "Hub IP (Hub Only, NNN.NNN.N.NNN)")
	}
}
//	===== Update when installed or setting changed =====
def installed() {
	log.info "Installing ${device.label}..."
    updateDataValue("refreshRate", "30")
	if(getDataValue("installType") == null) { updateDataValue("installType", "Manual") }
    update()
}
def update() {
    runIn(2, updated)
}
def updated() {
	log.info "Updating ${device.label}..."
	unschedule()
	if (!refresh_Rate) {
    	setRefreshRate(getDataValue("refreshRate"))
    } else {
    	setRefreshRate(refresh_Rate)
    }
    if (device_IP) { setDeviceIP(device_IP) }
    if (gateway_IP) { setGatewayIP(gateway_IP) }
	sendEvent(name: "DeviceWatch-Enroll", value: groovy.json.JsonOutput.toJson(["protocol":"cloud", "scheme":"untracked"]), displayed: false)
    if (getDataValue("installType") == "Manual") { updateDataValue("deviceDriverVersion", devVer())  }
	runIn(2, refresh)
}
def uninstalled() {
	log.info "${device.label} uninstalled.  Farewell!"
}
def on() {
	try {
	sendEvent(name: "switch", value: "on")
	} catch (error) {
    }
    runIn(3, refresh)
}
def off() {
	sendEvent(name: "switch", value: "off")
    runIn(3, refresh)
}
def refresh(){
	sendCmdtoServer('{"system" :{"get_sysinfo" :{}}}', "deviceCommand", "refreshResponse")
}
def refreshResponse(cmdResponse){
	def onOffState = cmdResponse.system.get_sysinfo.plug.relay_status
	if (onOffState == "ON") {
		sendEvent(name: "switch", value: "on")
		log.info "${device.label}: Power: on"
	} else {
		sendEvent(name: "switch", value: "off")
		log.info "${device.label}: Power: off"
	}
}
//	===== Send the Command =====
private sendCmdtoServer(command, hubCommand, action) {
	try {
		if (getDataValue("installType") == "Kasa Account") {
			sendCmdtoCloud(command, hubCommand, action)
		} else {
			sendCmdtoHub(command, hubCommand, action)
		}
	} catch (ex) {
		log.error "${device.label}: Sending Command Exception: ${ex}.  Communications error with device."
	}
}
private sendCmdtoCloud(command, hubCommand, action){
	def appServerUrl = getDataValue("appServerUrl")
	def deviceId = getDataValue("deviceId")
	def cmdResponse = parent.sendDeviceCmd(appServerUrl, deviceId, command)
	String cmdResp = cmdResponse.toString()
	if (cmdResp.substring(0,5) == "ERROR"){
		def errMsg = cmdResp.substring(7,cmdResp.length())
		log.error "${device.label}: ${errMsg}"
		sendEvent(name: "switch", value: "unavailable", descriptionText: errMsg)
		sendEvent(name: "deviceError", value: errMsg)
		sendEvent(name: "DeviceWatch-DeviceStatus", value: "offline", displayed: false, isStateChange: true)
		action = ""
	} else {
		sendEvent(name: "DeviceWatch-DeviceStatus", value: "online", displayed: false, isStateChange: true)
		sendEvent(name: "deviceError", value: "OK")
	}
	refreshResponse(cmdResponse)
}
private sendCmdtoHub(command, hubCommand, action){
	def gatewayIP = getDataValue("gatewayIP")
    def deviceIP = getDataValue("deviceIP")
	if (deviceIP =~ null && gatewayIP =~ null) {
		sendEvent(name: "switch", value: "unavailable", descriptionText: "Please input Device IP / Gateway IP")
		sendEvent(name: "deviceError", value: "No Hub Address Data")
		sendEvent(name: "DeviceWatch-DeviceStatus", value: "offline", displayed: false, isStateChange: true)
		log.error "${device.label}: Invalid IP.  Please check and update."
	}
	def headers = [:]
	headers.put("HOST", "$gatewayIP:8082")
	headers.put("tplink-iot-ip", deviceIP)
	headers.put("tplink-command", command)
	headers.put("action", action)
	headers.put("command", hubCommand)
	sendHubCommand(new physicalgraph.device.HubAction([headers: headers], device.deviceNetworkId, [callback: hubResponseParse]))
}
def hubResponseParse(response) {
	def action = response.headers["action"]
	def cmdResponse = parseJson(response.headers["cmd-response"])
	if (cmdResponse == "TcpTimeout") {
		log.error "${device.label}: Communications Error"
		sendEvent(name: "switch", value: "offline", descriptionText: "ERROR - OffLine in hubResponseParse")
		sendEvent(name: "deviceError", value: "TCP Timeout in Hub")
		sendEvent(name: "DeviceWatch-DeviceStatus", value: "offline", displayed: false, isStateChange: true)
	} else {
		sendEvent(name: "deviceError", value: "OK")
		sendEvent(name: "DeviceWatch-DeviceStatus", value: "online", displayed: false, isStateChange: true)
		refreshResponse(cmdResponse)
	}
}
def setAppServerUrl(newAppServerUrl) {
	updateDataValue("appServerUrl", newAppServerUrl)
	log.info "${device.label}: Updated appServerUrl."
}
def setLightTransTime(newTransTime) {
	switch (deviceType()) {
		case "Soft White Bulb":
		case "Tunable White Bulb":
		case "Color Bulb":
			def transitionTime = newTransTime.toInteger()
			def transTime = 1000*transitionTime
			updateDataValue("transTime", "${transTime}")
			log.info "${device.label}: Light Transition Time for set to ${transTime} milliseconds."
			break
		default:
			return
	}
}
def setRefreshRate(refreshRate) {
	updateDataValue("refreshRate", refreshRate)
	switch(refreshRate) {
		case "1" :
			runEvery1Minute(refresh)
			log.info "${device.label}: Refresh Scheduled for every minute."
			break
		case "5" :
			runEvery5Minutes(refresh)
			log.info "${device.label}: Refresh Scheduled for every 5 minutes."
			break
		case "10" :
			runEvery10Minutes(refresh)
			log.info "${device.label}: Refresh Scheduled for every 10 minutes."
			break
		default:
			runEvery15Minutes(refresh)
			log.info "${device.label}: Refresh Scheduled for every 15 minutes."
	}
}
def setDeviceIP(deviceIP) { 
	updateDataValue("deviceIP", deviceIP)
	log.info "${device.label}: device IP set to ${deviceIP}."
}
def setGatewayIP(gatewayIP) { 
	updateDataValue("gatewayIP", gatewayIP)
	log.info "${device.label}: hub IP set to ${gatewayIP}."
}
def setAppVersion(appVersion) {
	updateDataValue("appVersion", appVersion)
    updateDataValue("deviceVersion", devVer())
    log.info "${device.label}: Update appVersion and deviceVersion"
}
def setHubVersion(hubVersion) {
	updateDataValue("hubVersion", hubVersion)
    log.info "${device.label}: Updated Hub v.ersion"
}

//end-of-file
