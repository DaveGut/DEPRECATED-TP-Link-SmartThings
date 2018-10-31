/*
TP-LinkHub - Version 3.0

This java script uses node.js functionality to provide a hub between SmartThings and TP-Link devices.  It works with the the SmartThings application TP-Link SmartThins Manager.
01-31-2018	Release of Version 2 Hub
11-0102018 Release of version 3 Hub
*/

//---- Determine if old Node version, act accordingly -------------
console.log("Node.js Version Detected:   " + process.version)
var oldNode = "no"
if (process.version == "v6.0.0-pre") {
	oldNode ="yes"
	logFile = "no"
}

//---- Program set up and global variables -------------------------
var logFile = "yes"	//	Set to no to disable error.log file.
var bridgePort = 8082	//	Synched with Device Handlers.
var hubPort = 8082	//	Synched with Device Handlers.
var http = require('http')
var net = require('net')
var fs = require('fs')
var server = http.createServer(onRequest)
var dgram = require('dgram')
var os = require('os')
var dgram = require('dgram')
var pollPort = 9999
var nodeApp = "TP-Link Bridge"
var interfaces = os.networkInterfaces()
for (var k in interfaces) {
	for (var k2 in interfaces[k]) {
		var address = interfaces[k][k2]
		if (address.family === 'IPv4' && !address.internal) {
			var bridgeIP = address.address
			var mac = address.mac.replace(/:/g, "")
			var bridgeMac = mac.toUpperCase()
		}
	}
}
var tplinkDeviceList = []

//---- Start the HTTP Server Listening to SmartThings --------------
server.listen(hubPort)
console.log("TP-Link Hub Application Console Log")
logResponse("\n\r" + new Date() + "\rTP-Link Hub Error Log")

//---- Command interface to Smart Things ---------------------------
function onRequest(request, response){
	var command = request.headers["command"]
	var deviceIP = request.headers["tplink-iot-ip"]
	if (command == "pollForDevices" || command == "pollBridge") {
		var cmdRcvd = "\n\r" + new Date() + "\r\nIP: " + command + " being executed"
	} else {
		var cmdRcvd = "\n\r" + new Date() + "\r\nIP: " + deviceIP + " sent command " + command
	}
	console.log(" ")
	console.log(cmdRcvd)
	switch(command) {
		//---- TP-Link Device Command ---------------------------
		case "deviceCommand":
			processDeviceCommand(request, response)
			break
	
		//---- Connected - Device Discovery ---------------------
		case "pollForDevices":
			discoverTPLinkDevices()
			setTimeout(setPollHeader, 4000)
			function setPollHeader() {
				response.setHeader("cmd-response", JSON.stringify(tplinkDeviceList))
				response.end()
				console.log("Sending TP-Link Device List to SmartThings")
			}
			break

		//---- Special Case for Energy Meter --------------------
		case "emeterCmd":
			processEmeterCommand(request, response)
			break

		case "pollBridge":
			response.setHeader("cmd-response", "ok")
			response.end()
			console.log("Bridge poll response sent to SmartThings")
			break

		default:
			response.setHeader("cmd-response", "InvalidHubCmd")
			response.end()
			var respMsg = "#### Invalid Command ####"
			var respMsg = new Date() + "\n\r#### Invalid Command from IP" + deviceIP + " ####\n\r"
			console.log(respMsg)
			logResponse(respMsg)
	}
}

//---- UDP Discovery of TP-Link Devices ----------------------------
function discoverTPLinkDevices() {
	tplinkDeviceList = []
	var socket = dgram.createSocket('udp4')
	socket.on('error', function (err) {
		socket.close()
	}.bind())
	socket.on('message', function (msg, rinfo) {
		var tplinkDevice = {}
		var device = JSON.parse(decrypt(msg).toString('ascii')).system.get_sysinfo
		if (device.mic_type == "IOT.SMARTBULB") {
			tplinkDNI = device.mic_mac
		} else {
			tplinkDNI = device.mac.replace(/:/g, "")
		}
		tplinkDevice['deviceMac'] = tplinkDNI
		tplinkDevice['deviceIP'] = rinfo.address
		tplinkDevice['deviceModel'] = device.model
		tplinkDevice['alias'] = device.alias
		tplinkDevice['gatewayIP'] = bridgeIP
		tplinkDeviceList.push(tplinkDevice)
	}.bind())
	socket.bind(bridgePort, bridgeIP, () => {
		socket.setBroadcast(true)
	})
	var msgBuf = UdpEncrypt('{"system":{"get_sysinfo":{}}}')
	socket.send(msgBuf, 0, msgBuf.length, pollPort, '255.255.255.255')
	setTimeout(closeSocket, 4000)
	function closeSocket() {
		socket.close()
	}
}

//---- Send deviceCommand and return response to SmartThings ---------
function processDeviceCommand(request, response) {
	var command = request.headers["tplink-command"]
	var deviceIP = request.headers["tplink-iot-ip"]
	var respMsg = "deviceCommand sending to IP: " + deviceIP + " Command: " + command
	console.log(respMsg)
	var action = request.headers["action"]
	response.setHeader("action", action)
	var socket = net.connect(9999, deviceIP)
	socket.setKeepAlive(false)
	socket.setTimeout(6000)  // 6 seconds timeout.  TEST WITHOUT
	socket.on('connect', () => {
		socket.write(TcpEncrypt(command))
	})
	socket.on('data', (data) => {
		socket.end()
		data = decrypt(data.slice(4)).toString('ascii')
		response.setHeader("cmd-response", data)
		response.end()
		var respMsg = "Command Response sent to SmartThings"
		console.log(respMsg)
	}).on('timeout', () => {
		response.setHeader("cmd-response", "TcpTimeout")
		response.end()
		socket.end()
		var respMsg = new Date() + "\n#### TCP Timeout in deviceCommand for IP: " + deviceIP + " ,command: " + command
		console.log(respMsg)
		logResponse(respMsg)
	}).on('error', (err) => {
		socket.end()
		var respMsg = new Date() + "\n#### Socket Error in deviceCommand for IP: " + deviceIP + " ,command: " + command
		console.log(respMsg)
		logResponse(respMsg)
	})
}

//---- Send EmeterCmd and return response to SmartThings -------------
function processEmeterCommand(request, response) {
	var command = request.headers["tplink-command"]
	var deviceIP = request.headers["tplink-iot-ip"]
	var respMsg = "EmeterCmd sending to IP:" + deviceIP + " command: " + command
	console.log(respMsg)
	var action = request.headers["action"]
	response.setHeader("action", action)
	var socket = net.connect(9999, deviceIP)
	socket.setKeepAlive(false)
	socket.setTimeout(4000)
	socket.on('connect', () => {
  		socket.write(TcpEncrypt(command))
	})
	var concatData = ""
	var resp = ""
	setTimeout(mergeData, 3000)  // 3 seconds to capture response
	function mergeData() {
		if (concatData != "") {
			socket.end()
			data = decrypt(concatData.slice(4)).toString('ascii')
			response.setHeader("cmd-response", data)
			response.end()
			var respMsg = "Command Response sent to SmartThings"
			console.log(respMsg)
		} else {
			socket.end()
			response.setHeader("cmd-response", "TcpTimeout")
			response.end()
			var respMsg = new Date() + "\n#### Comms Timeout in EmeterCmd for IP: " + deviceIP + " ,command: " + command
		console.log(respMsg)
		logResponse(respMsg)
		}
	}
	socket.on('data', (data) => {
		concatData += data.toString('ascii')
	}).on('timeout', () => {
		socket.end()
		var respMsg = new Date() + "\n#### TCP Timeout in EmeterCmd for IP: " + deviceIP + " ,command: " + command
		console.log(respMsg)
		logResponse(respMsg)
	}).on('error', (err) => {
		socket.end()
		var respMsg = new Date() + "\n\r#### TCP Error in EmeterCmd for IP: " + deviceIP + " ,command: " + command
		console.log(respMsg)
		logResponse(respMsg)
	})
}

//----- Utility - Response Logging Function ------------------------
function logResponse(respMsg) {
	if (logFile == "yes") {
		fs.appendFileSync("error.log", "\r" + respMsg)
	}
}

//----- Utility - Encrypt TCP Commands to Devices ------------------
function TcpEncrypt(input) {
	if (oldNode == "no"){
		var buf = Buffer.alloc(input.length + 4)
	} else {
		var buf = new Buffer(input.length + 4)
	}
	buf[0] = null
	buf[1] = null
	buf[2] = null
	buf[3] = input.length
	var key = 0xAB
	for (var i = 4; i < input.length+4; i++) {
		buf[i] = input.charCodeAt(i-4) ^ key
		key = buf[i]
	}
	return buf
}

//----- Utility - Encrypt UDP Commands to Devices ------------------
function UdpEncrypt (input) {
	var buf = new Buffer(input.length)
	var key = 0xAB
	for (var i = 0; i < input.length; i++) {
		buf[i] = input.charCodeAt(i) ^ key
		key = buf[i]
	}
	return buf
}

//----- Utility - Decrypt Returns from  Devices --------------------
function decrypt(input, firstKey) {
	if (oldNode == "no") {
		var buf = Buffer.from(input)
	} else {
		var buf = new Buffer(input)
	}
	var key = 0x2B
	var nextKey
	for (var i = 0; i < buf.length; i++) {
		nextKey = buf[i]
		buf[i] = buf[i] ^ key
		key = nextKey
	}
	return buf
}
