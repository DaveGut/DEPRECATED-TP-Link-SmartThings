/*
TP-Link Plug and Switch Device Handler, 2018, Version 3

	Copyright 2018 Dave Gutheinz and Anthony Ramirez

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this  file except in compliance with the
License. You may obtain a copy of the License at:

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, 
software distributed under the License is distributed on an 
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
either express or implied. See the License for the specific 
language governing permissions and limitations under the 
License.

Discalimer:  This Service Manager and the associated Device 
Handlers are in no way sanctioned or supported by TP-Link.  
All  development is based upon open-source data on the 
TP-Link devices; primarily various users on GitHub.com.

========= History ============================================
2018-10-23	Update to Version 3.5:
			a.	Compatibility with new SmartThings app.
			b.	Update capabilities per latest ST definitions
				1.	deleted capability polling (depreciated)
				2.	deleted capability sensor (depreciated)
				3.	update program to accommodate other items
			c.	Various changes for updated Service Manager
		   	With great appreciation to Anthony Ramirez for
			his assistance as well as leading the development
			of the new Service Manager.
12.07.18	3.5.03.	Corrected refresh rate update issue.
12.22.18	3.6.01.	Various updates to reduce code maintenance
			and better interact with SmartApp.
======== DO NOT EDIT LINES BELOW ===*/
//	===== Device Type Identifier =====
	def deviceType()	{ return "Plug" }
//	def deviceType()	{ return "Switch" }
//	def deviceType()	{ return "Dimming Switch" }	
//	def deviceType()	{ return "Multi-Plug" }	
	def ocfValue() { return (deviceType() == "Plug") ? "oic.d.smartplug" : "oic.d.switch" }
	def vidValue() { return (deviceType() == "Dimming Switch") ? "generic-dimmer" : "generic-switch" }
	def deviceIcon()	{ return (deviceType() == "Plug") ? "st.Appliances.appliances17" : "st.Home.home30" }
	def devVer()	{ return "3.6.01" }
//	======================================================================================================================

metadata {
	definition (name: "TP-Link Smart ${deviceType()}", 
				namespace: "davegut", 
				author: "Dave Gutheinz, Anthony Ramirez", 
				ocfDeviceType: "${ocfValue()}", 
				mnmn: "SmartThings", 
				vid: "${vidValue()}") {
		capability "Switch"
		capability "refresh"
		capability "Health Check"
		if (deviceType() == "Dimming Switch") {
			capability "Switch Level"
		}
	}
	tiles(scale: 2) {
		multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action: "switch.off", icon: "${deviceIcon()}", backgroundColor: "#00a0dc",
				nextState: "waiting"
				attributeState "off", label:'${name}', action: "switch.on", icon: "${deviceIcon()}", backgroundColor: "#ffffff",
				nextState: "waiting"
				attributeState "waiting", label:'${name}', action: "switch.on", icon: "${deviceIcon()}", backgroundColor: "#15EE10",
				nextState: "waiting"
				attributeState "Unavailable", label:'Unavailable', action: "switch.on", icon: "${deviceIcon()}", backgroundColor: "#e86d13",
				nextState: "waiting"
			}
			if (deviceType() == "Dimming Switch") {
				tileAttribute ("device.level", key: "SLIDER_CONTROL") {
					attributeState "level", label: "Brightness: ${currentValue}", action:"switch level.setLevel", range: "(1..100)"
				}
			}
			tileAttribute ("deviceError", key: "SECONDARY_CONTROL") {
				attributeState "deviceError", label: '${currentValue}'
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
		input ("plug_No", "text", title: "Number of the plug (00, 01, 02, etc) (Multi-Plug Only")
	}
}

//	===== Update when installed or setting changed =====
def installed() {
	log.info "Installing ${device.label}..."
	setRefreshRate(10)
	if(getDataValue("installType") == null) { updateDataValue("installType", "Manual") }
	update()
}

def ping() {
	refresh()
}

def update() {
	runIn(2, updated)
}

def updated() {
	log.info "Updating ${device.label}..."
	unschedule()
	if (refresh_Rate) { setRefreshRate(refresh_Rate) }
	if (device_IP) { setDeviceIP(device_IP) }
	if (gateway_IP) { setGatewayIP(gateway_IP) }
	if (getDataValue("InstallType")== "Manual" && getDataValue("deviceIP") && getDataValue("gatewayIP") && plugNo) {
		sendCmdtoServer('{"system" :{"get_sysinfo" :{}}}', "deviceCommand", "parsePlugId")
	}
	sendEvent(name: "DeviceWatch-Enroll", value: groovy.json.JsonOutput.toJson(["protocol":"cloud", "scheme":"untracked"]), displayed: false)
	if (getDataValue("installType") == "Manual") { updateDataValue("deviceDriverVersion", devVer())  }
	runIn(2, refresh)
}

def parsePlugId(cmdResponse) {
	def deviceData = cmdResponse.system.get_sysinfo
	def plugId = "${deviceData.deviceId}${plug_No}"
	updateDataValue("plugId", plugId)
	log.info "${device.name} ${device.label}: Plug ID set to ${plugId}"
}

def uninstalled() {
	log.info "${device.label} uninstalled.  Farewell!"
}

//	===== Basic Plug Control/Status =====
def on() {
	if (deviceType() != "Multi-Plug") {
		sendCmdtoServer("""{"system" :{"set_relay_state" :{"state" : 1}}}""", "deviceCommand", "commandResponse")
	} else {
		def plugId = getDataValue("plugId")
		sendCmdtoServer("""{"context":{"child_ids":["${plugId}"]},"system":{"set_relay_state":{"state": 1}}}""",
					"deviceCommand", "commandResponse")
	}
}

def off() {
	if (deviceType() != "Multi-Plug") {
		sendCmdtoServer("""{"system" :{"set_relay_state" :{"state" : 0}}}""", "deviceCommand", "commandResponse")
	} else {
		def plugId = getDataValue("plugId")
		sendCmdtoServer("""{"context":{"child_ids":["${plugId}"]},"system":{"set_relay_state":{"state": 0}}}""",
					"deviceCommand", "commandResponse")
	}
}

def setLevel(percentage) {
	sendCmdtoServer('{"system":{"set_relay_state":{"state": 1}}}', "deviceCommand", "commandResponse")
	if (percentage < 0 || percentage > 100) {
		log.error "$device.name $device.label: Entered brightness is not from 0...100"
		percentage = 50
	}
	percentage = percentage as int
	sendCmdtoServer("""{"smartlife.iot.dimmer" :{"set_brightness" :{"brightness" :${percentage}}}}""", "deviceCommand", "commandResponse")
}

def refresh(){
	sendCmdtoServer('{"system" :{"get_sysinfo" :{}}}', "deviceCommand", "refreshResponse")
}

def refreshResponse(cmdResponse){
	def onOff
	if (deviceType() != "Multi-Plug") {
		def onOffState = cmdResponse.system.get_sysinfo.relay_state
		if (onOffState == 1) {
			onOff = "on"
		} else {
			onOff = "off"
		}
	} else {
		def children = cmdResponse.system.get_sysinfo.children
		def plugId = getDataValue("plugId")
		children.each {
			if (it.id == plugId) {
				if (it.state == 1) {
					onOff = "on"
				} else {
					onOff = "off"
				}
			}
		}
	}
	sendEvent(name: "switch", value: onOff)
	if (deviceType() == "Dimming Switch") {
		def level = cmdResponse.system.get_sysinfo.brightness
	 	sendEvent(name: "level", value: level)
		log.info "${device.label}: Power: ${onOff} / Dimmer Level: ${level}%"
	} else {
		log.info "${device.label}: Power: ${onOff}"
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
	actionDirector(action, cmdResponse)
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
		actionDirector(action, cmdResponse)
	}
}

def actionDirector(action, cmdResponse) {
	switch(action) {
		case "commandResponse":
			refresh()
			break
		case "refreshResponse":
			refreshResponse(cmdResponse)
			break
		case "energyMeterResponse":
			energyMeterResponse(cmdResponse)
			break
		case "useTodayResponse":
			useTodayResponse(cmdResponse)
			break
		case "currentDateResponse":
			currentDateResponse(cmdResponse)
			break
		case "engrStatsResponse":
			engrStatsResponse(cmdResponse)
			break
		case "parsePlugId" :
			parsePlugId(cmdResponse)
			break
		default:
			log.info "${device.label}: Interface Error.	See SmartApp and Device error message."
	}
}

//	===== Child / Parent Interchange =====
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

//end-of-file
