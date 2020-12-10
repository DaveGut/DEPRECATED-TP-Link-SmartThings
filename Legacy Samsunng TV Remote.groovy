/*	===== HUBITAT INTEGRATION VERSION =====================================================
Hubitat - Samsung Legacy TV Remote Driver
		Copyright 2020 Dave Gutheinz

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this  file
except in compliance with the License. You may obtain a copy of the License at:
		http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the
License is distributed on an  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
either express or implied. See the License for the specific language governing permissions
and limitations under the  License.
===== DISCLAIMERS =========================================================================
		THE AUTHOR OF THIS INTEGRATION IS NOT ASSOCIATED WITH SAMSUNG. THIS CODE USES
		TECHNICAL DATA DERIVED FROM GITHUB SOURCES AND AS PERSONAL INVESTIGATION.
===== RELEASE NOTES =======================================================================
1.0.0	12.10.20	Initial release
*/
import org.json.JSONObject
import groovy.util.*

def driverVer() { return "1.0.0" }
metadata {
	definition (name: "Samsung Legacy TV Control",
				namespace: "davegut",
				author: "David Gutheinz",
				importUrl: ""
			   ){
		capability "Switch"	//	For use with an external switch
							//	Use Rule Machine to detect on/off
							//	then command external switch.
		capability "MusicPlayer"
		command "levelUp"
		command "levelDown"
		command "setSource", ["STRING"]
		command "sourceTV"
		command "sourceHDMI1"
		command "sourceHDMI2"
		command "sourceHDMI3"
		command "sourceHDMI4"
		command "sourceComponent"
		attribute "inputSource", "string"
		attribute "validSources", "string"
		capability "SpeechSynthesis"
		capability "Refresh"
		command "kickStartQueue"
	}
	preferences {
		input ("deviceIP", "string", title: "Samsung UPnP Device IP")
		input ("MY2015", "bool", title: "Is this a 2015 Model?", defaultValue: true)
		input ("debug", "bool",  title: "Enable debug logging for 30 minutes", defaultValue: false)
		input ("descriptionText", "bool",  title: "Enable description text logging", defaultValue: true)
		def ttsLanguages = ["en-au":"English (Australia)","en-ca":"English (Canada)", "en-gb":"English (Great Britain)",
							"en-us":"English (United States)", "en-in":"English (India)","ca-es":"Catalan",
							"zh-cn":"Chinese (China)", "zh-hk":"Chinese (Hong Kong)","zh-tw":"Chinese (Taiwan)",
							"da-dk":"Danish", "nl-nl":"Dutch","fi-fi":"Finnish","fr-ca":"French (Canada)",
							"fr-fr":"French (France)","de-de":"German","it-it":"Italian","ja-jp":"Japanese",
							"ko-kr":"Korean","nb-no":"Norwegian","pl-pl":"Polish","pt-br":"Portuguese (Brazil)",
							"pt-pt":"Portuguese (Portugal)","ru-ru":"Russian","es-mx":"Spanish (Mexico)",
							"es-es":"Spanish (Spain)","sv-se":"Swedish (Sweden)"]
		input ("ttsApiKey", "string", title: "TTS Site Key", description: "From http://www.voicerss.org/registration.aspx")
		input ("ttsLang", "enum", title: "TTS Language", options: ttsLanguages, defaultValue: "en-us")
	}
}
def installed() {
	log.info "Installing .."
	updated()
}
def updated() {
	log.info "Updating .."
	unschedule()
	state.speaking = false
	state.playQueue = []
	setUpnpData()
	pauseExecution(2000)
	getSources()
	if (debug == true) { runIn(1800, debugLogOff) }
	logInfo("Debug logging is: ${debug}.")
	logInfo("Description text logging is ${descriptionText}.")
	runEvery15Minutes(refresh)
	refresh()
}
def setUpnpData() {
	logInfo("setUpnpData")
	if (MY2015 == true) {
		updateDataValue("rcUrn", "urn:schemas-upnp-org:service:RenderingControl:1")
		updateDataValue("rcPath", "/upnp/control/RenderingControl1")
		updateDataValue("rcPort", "9197")
		updateDataValue("avUrn", "urn:schemas-upnp-org:service:AVTransport:1")
		updateDataValue("avPath", "/upnp/control/AVTransport1")
		updateDataValue("avPort", "9197")
		updateDataValue("tvUrn", "urn:samsung.com:service:MainTVAgent2:1")
		updateDataValue("tvPath", "/MainTVServer2/control/MainTVAgent2")
		updateDataValue("tvPort", "7677")
	} else {
		updateDataValue("rcUrn", "urn:schemas-upnp-org:service:RenderingControl:1")
		updateDataValue("rcPath", "/smp_20_")
		updateDataValue("rcPort", "7676")
		updateDataValue("avUrn", "urn:schemas-upnp-org:service:AVTransport:1")
		updateDataValue("avPath", "/smp_26_")
		updateDataValue("avPort", "7676")
		updateDataValue("tvUrn", "urn:samsung.com:service:MainTVAgent2:1")
		updateDataValue("tvPath", "/smp_4_")
		updateDataValue("tvPort", "7676")
	}
}

//	========== Capability Switch ==========
def on() { sendEvent(name: "switch", value: "on") }
def off() { sendEvent(name: "switch", value: "off") }
/*def switchStatus() { sendEvent(name: "switch", value: "unknown") }*/

//	========== Capability Music Player ==========
def play() {
	logDebug("play")
	sendCmd("AVTransport",
			"Play",
			["InstanceID" :0,
			 "Speed": "1"])
}
def pause() {
	logDebug("pause")
	sendCmd("AVTransport",
			"Pause",
			["InstanceID" :0])
}
def stop() {
	logDebug("stop")
	sendCmd("AVTransport",
			"Stop",
			["InstanceID" :0])
}
def getPlayStatus() {
	logDebug("getPlayStatus")
	sendCmd("AVTransport",
			"GetTransportInfo",
			["InstanceID" :0])
}
def updatePlayStatus(body) {
	def status = body.CurrentTransportState.text()
	switch(status) {
		case "PLAYING":
			status = "playing"
			break
		case "PAUSED_PLAYBACK":
			status = "paused"
			break
		case "STOPPED":
			status = "stopped"
			break
		default:
			status = "no media"
	}
	sendEvent(name: "status", value: status)
	logDebug("updatePlayStatus: ${status}")
}

def setLevel(volume) {
	logDebug("setVolume: volume = ${volume}")
	volume = volume.toInteger()
	if (volume <= 0 || volume >= 100) { return }
	sendCmd("RenderingControl",
			"SetVolume",
			["InstanceID" :0,
			 "Channel": "Master",
			 "DesiredVolume": volume])
	runIn(1, getVolume)
}
def levelUp() {
	def curVol = device.currentValue("level").toInteger()
	logDebug("levelUp: curVol = ${curVol}")
	def volIncrement = 3
	def newVolume = curVol + volIncrement
	if (newVolume > 100) { newVolume = 100 }
	setLevel(newVolume)
}
def levelDown() {
	def curVol = device.currentValue("level").toInteger()
	logDebug("levelUp: curVol = ${curVol}")
	def volIncrement = 3
	def newVolume = curVol - volIncrement
	if (newVolume > 100) { newVolume = 100 }
	setLevel(newVolume)
}
def getVolume() {
	logDebug("getVolume")
	sendCmd("RenderingControl",
			"GetVolume",
			["InstanceID" :0,
			 "Channel": "Master"])
}
def updateVolume(body) {
	def status = body.CurrentVolume.text()
	sendEvent(name: "level", value: status.toInteger())
	logDebug("updateVolume: volume = ${status}")
}

def mute() { setMute(true) }
def unmute() { setMute(false) }
def setMute(muteState) {
	logDebug("setMute: mute = ${muteState}")
	mute = "1"
	if (muteState == false) { mute = "0" }
	sendCmd("RenderingControl",
			"SetMute",
			["InstanceID" :0,
			 "Channel": "Master",
			 "DesiredMute": mute])
}
def getMute(muteState) {
	logDebug("setMute: mute = ${muteState}")
	sendCmd("RenderingControl",
			"GetMute",
			["InstanceID" :0,
			 "Channel": "Master"])
}
def updateMuteStatus(body) {
	def status = body.CurrentMute.text()
	def mute = "unmuted"
	if (status == "1") { mute = "muted" }
	sendEvent(name: "mute", value: mute)
	logDebug("updateMuteStatus.GetMuteStatus: ${mute}")
}

def playText(text) { speak(text) }
def playTrack(trackUri) { execPlay(trackUri) }

def nextTrack(trackUri=null) { logDebug("nextTrack not implemented") }
def previousTrack(trackUri=null) { logDebug("previousTrack not implemented") }
def restoreTrack(trackUri=null) { logDebug("restoreTrack not implemented") }
def resumeTrack(trackUri=null) { logDebug("resumeTrack not implemented") }
def setTrack(trackUri=null) { logDebug("setTrack not implemented") }

//	========== Capability Speech Synthesis ==========
def speak(text) {
	logDebug("speak: text = ${text}")
	def track = convertToTrack(text)
	addToQueue(track.uri, track.duration)
}
def convertToTrack(text) {
	def uriText = URLEncoder.encode(text, "UTF-8").replaceAll(/\+/, "%20")
	trackUri = "http://api.voicerss.org/?" +
		"key=${ttsApiKey.trim()}" +
		"&f=48khz_16bit_mono" +
		"&c=MP3" +
		"&hl=${ttsLang}" +
		"&src=${uriText}"
	def duration = (1 + text.length() / 10).toInteger()
	return [uri: trackUri, duration: duration]
}

//	========== Play Queue Execution ==========
def addToQueue(trackUri, duration){
	logDebug("addToQueue: ${trackUri},${duration}") 
	duration = duration + 1
	playData = ["trackUri": trackUri, 
				"duration": duration]
	state.playQueue.add(playData)

	if (state.speaking == false) {
		state.speaking = true
		runInMillis(100, startPlayViaQueue)
	}
}
def startPlayViaQueue() {
	logDebug("startPlayViaQueue: queueSize = ${state.playQueue.size()}")
	if (state.playQueue.size() == 0) { return }
	playViaQueue()
}
def playViaQueue() {
	logDebug("playViaQueue: queueSize = ${state.playQueue.size()}")
	if (state.playQueue.size() == 0) {
		resumePlayer()
		return
	}
	def playData = state.playQueue.get(0)
	state.playQueue.remove(0)
	logDebug("playViaQueue: playData = ${playData}")

	execPlay(playData.trackUri)
	runIn(playData.duration, resumePlayer)
	runIn(30, kickStartQueue)
}
def execPlay(trackUri) {
	sendCmd("AVTransport",
				 "SetAVTransportURI",
				 [InstanceID: 0,
				  CurrentURI: trackUri,
				  CurrentURIMetaData: ""])
}
def resumePlayer() {
	if (state.playQueue.size() > 0) {
		playViaQueue()
		return
	}
	logDebug("resumePlayer}")
	state.speaking = false
}
def kickStartQueue() {
	logInfo("kickStartQueue")
	if (state.playQueue.size() > 0) {
		resumePlayer()
	} else {
		state.speaking = false
	}
}

//	========== Input Source Methods ==========
def setSource(source) {
	source = source.toUpperCase()
	def sources = new JSONObject(getDataValue("inputSources"))
	def newSource = sources[source]
	sendCmd("MainTVAgent2",
			"SetMainTVSource",
			["Source": newSource.source,
			 "ID": newSource.id,
			 "UiID": "-1"])
}
def getSource() {
	sendCmd("MainTVAgent2",
			"GetCurrentExternalSource")
}
def updateSource(body) {
	if (body.Result != "OK") {
		logWarn("updateSource: Response not OK, body = ${body}")
		return
	}
	def status = body.CurrentExternalSource.text()
	sendEvent(name: "inputSource", value: status)
	sendEvent(name: "trackDescription", value: status)
	logDebug("updateSource: ${status}")
}
def getSources() {
	logInfo("getSources")
	sendCmd("MainTVAgent2",
			"GetSourceList")
}
def updateSourceList(body) {
	if (body.Result != "OK") {
		logWarn("updateSourceList: Response not OK, body = ${body}")
		return
	}
	def sourceList = new XmlSlurper().parseText(body.SourceList.text())
	def inputSource = sourceList.CurrentSourceType
	sendEvent(name: "inputSource", value: inputSource)
	sendEvent(name: "trackDescription", value: inputSource)
	logDebug("updateSourceList: source = ${inputSource}")
	def sources = "{"
	def validSources = ""
	sourceList.Source.each {
		def source = it.SourceType.text()
		if (source.contains("/DVI")) { source = source.replace("/DVI","") }
		sources += "${source}: {source: ${source}, id: ${it.ID}}, "
		validSources += "${source}, "
	}
	sources = sources.substring(0, sources.length()-2) +"}"
	sendEvent(name: "validSources", value: validSources)
	updateDataValue("inputSources", sources.toString())
	logDebug("updateSourceList: ${sources}")
}
def sourceTV() { setSource("TV") }
def sourceHDMI1() { setSource("HDMI1") }
def sourceHDMI2() { setSource("HDMI2") }
def sourceHDMI3() { setSource("HDMI3") }
def sourceHDMI4() { setSource("HDMI4") }
def sourceComponent() { setSource("Component") }

//	========== Capability Refresh ==========
def refresh() {
	logDebug("refresh")
	getSource()
	getVolume()
	getMute()
	getPlayStatus()
	//	Add get switch status?
}

//	========== SEND Commands to Devices ==========
private sendCmd(type, action, body = []){
	logDebug("sendCmd: type = ${type}, upnpAction = ${action}, upnpBody = ${body}")
	def cmdPort
	def cmdPath
	def cmdUrn
	if (type == "AVTransport") {
		cmdPort = getDataValue("avPort")
		cmdUrn = getDataValue("avUrn")
		cmdPath = getDataValue("avPath")
	} else if (type == "RenderingControl") {
		cmdPort = getDataValue("rcPort")
		cmdUrn = getDataValue("rcUrn")
		cmdPath = getDataValue("rcPath")
	} else if (type == "MainTVAgent2") {
		cmdPort = getDataValue("tvPort")
		cmdUrn = getDataValue("tvUrn")
		cmdPath = getDataValue("tvPath")
	} else { logWarn("sendCmd: Invalid UPnP Type = ${type}") }
	
	def host = "${deviceIP}:${cmdPort}"
	Map params = [path:	cmdPath,
				  urn:	 cmdUrn,
				  action:  action,
				  body:	body,
				  headers: [Host: host,
							CONNECTION: "close"]]
	def hubCmd = new hubitat.device.HubSoapAction(params)
	sendHubCommand(hubCmd)
}

def parse(resp) {
	resp = parseLanMessage(resp)
	logDebug("parse: ${groovy.xml.XmlUtil.escapeXml(resp.body)}")
//logTrace("parse: ${groovy.xml.XmlUtil.escapeXml(resp.body)}")
//return
	def body = resp.xml.Body
	if (!body.size()) {
		logWarn("parse: No XML Body in resp: ${resp}")
		return
	}
	else if (body.GetVolumeResponse.size()){ updateVolume(body.GetVolumeResponse) }
	else if (body.GetTransportInfoResponse.size()){ updatePlayStatus(body.GetTransportInfoResponse) }
	else if (body.GetMuteResponse.size()){ updateMuteStatus(body.GetMuteResponse) }
	else if (body.GetCurrentExternalSourceResponse.size()){ updateSource(body.GetCurrentExternalSourceResponse) }
	else if (body.GetSourceListResponse.size()){ updateSourceList(body.GetSourceListResponse) }
	//	===== Get status after command
	else if (body.SetAVTransportURIResponse.size()){ play() }
	else if (body.PlayResponse.size()){ runIn(5,getPlayStatus) }
	else if (body.PauseResponse.size()){ runIn(5,getPlayStatus) }
	else if (body.StopResponse.size()){ runIn(5,getPlayStatus) }
	else if (body.SetVolumeResponse.size()){ getVolume() }
	else if (body.SetMuteResponse.size()){ getMute() }
	else if (body.SetMainTVSourceResponse.size()){ getSource() }
	//	===== Fault Code =====
	else if (body.Fault.size()){
		def desc = body.Fault.detail.UPnPError.errorDescription
		if (desc == "Transition not available") {
			desc = "TV in incorrect mode."
		}
		logInfo("parse: Fault = ${desc}")
	}
	//	===== Unhandled response =====
	else { logWarn("parse: unhandled response: ${resp}") }
}

//	===== Utility Methods =====
def logTrace(msg) { log.trace"${device.label} ${driverVer()} || ${msg}" }
def logInfo(msg) {
	if (descriptionText == true) { log.info "${device.label} ${driverVer()} || ${msg}" }
}
def debugLogOff() {
	device.updateSetting("debug", [type:"bool", value: false])
	logInfo("Debug logging is false.")
}
def logDebug(msg){
	if(debug == true) { log.debug "${device.label} ${driverVer()} || ${msg}" }
}
def logWarn(msg){ log.warn "${device.label} ${driverVer()} || ${msg}" }

//	End-of-File