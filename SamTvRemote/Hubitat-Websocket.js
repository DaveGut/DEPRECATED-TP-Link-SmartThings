/*
Hubitat SSL NodeJs Server
Purpose is to support WSS and HTTPS webSocket commands from Hubitat.
These are not support on systems that issue invalid certificates,
like the Samsung TV's.  It may also work for other servers.
Installation Target:  Node.js capable PC, raspberryPi.  It may also
work on Amazon devices (TBD).
Release notes:
Version 1.0.1 - Update process connect decision within the device.
*/

//----- Program set up and global variables -------------------------
console.log("Node.js Version Detected:   " + process.version)
var nodeAppVer = "1.0.0"
var logFile = "yes"					//	Log File
var hubPort = 8080					//	Must be same as in driver
var http = require('http')
var fs = require('fs')
var server = http.createServer(onRequest)
const WebSocket = require('ws')
var cmdResponse = {}
var wsDevice
var response
var wsDeviceStatus = "closed"

//----- Start the HTTP Server Listening to Smart Hub --------------
server.listen(hubPort)
console.log("Hubitat WebSocket Version " + nodeAppVer)
logResponse("\n\r" + new Date() + "\rHubitat WebSocket Error Log")

//----- Command interface to Smart Things ---------------------------
function onRequest(request, response){
	var command = request.headers.command
	var data = request.headers.data
	var url = request.headers.url
	var cmdRcvd = "\n\rCommand Data = " + command + " | " + data + " | " + url + " | " + wsDeviceStatus
//	logResponse(" ")
	logResponse(cmdRcvd)
//	console.log(" ")
	console.log(cmdRcvd)
	cmdResponse = {}
	if (wsDeviceStatus == "closed" && command != "hubCheck") {
		command = "connect"
	}
	response.setHeader("command", command)
	response.setHeader("hubStatus", "OK")
	switch(command) {
		case "hubCheck":
			endResponse()
			break
		case "connect":
			setTimeout(endResponse, 9000)
			wsDeviceOpen(url, data, response)
			break
		case "sendMessage":
			setTimeout(endResponse, 1000)
			wsDevice.send(data)
			break
		case "close":
			setTimeout(endResponse, 1000)
			wsDevice.close()
			break
		default:
			var respMsg = "#### Invalid Command " + command + " ####"
			console.log(respMsg)
			logResponse(respMsg)
			endResponse()
	}
	function endResponse() {
		response.setHeader("wsDeviceStatus", wsDeviceStatus)
		response.setHeader("cmdResponse", JSON.stringify(cmdResponse))
		response.end()
	}
}


function wsDeviceOpen(connectUrl, data) {
	wsDevice = new WebSocket(connectUrl, { rejectUnauthorized: false });
	wsDevice.on('open', () => {
		wsDeviceStatus = "open"
		if (data != "") {
			wsDevice.send(data)
		}
	});
	wsDevice.on('close', (code, reason) => {
		wsDeviceStatus = "closed"
	});
	wsDevice.on('error', (err) => {
		var message = "\n\rwsDevice.on(error): error = " + err
//		logResponse(" ")
		logResponse(cmdRcvd)
//		console.log(" ")
		console.log(cmdRcvd)
		wsDeviceStatus = "error"
		cmdResponse["cmdStatus"] = "error"
		cmdResponse["cmdData"] = err
	});
	wsDevice.on('message', (data) => {
		cmdResponse["cmdStatus"] = "OK"
		cmdResponse["cmdData"] = data
	});
}


//----- Utility - Response Logging Function ------------------------
function logResponse(respMsg) {
	if (logFile == "yes") {
		fs.appendFileSync("Hubitat-Websocket.log", "\r" + respMsg)
	}
}
