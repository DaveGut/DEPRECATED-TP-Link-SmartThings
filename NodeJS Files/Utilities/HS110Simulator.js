/*
TP-LinkHub - Version 2.0

COMPATABILITY KEY:  HubVersion 2.0

This java script uses node.js functionality to provide a hub between SmartThings and TP-Link devices.  It works with the following TP-Link integrations:
a.	TP-Link Connect (including Discovery)
b.	TP-Link Smart Things Integration
c.	TP-Link Bridge (OPTIONAL)

01-31-2018	Release of Version 2 Hub
*/

//##### Options for this program ###################################
var logFile = "yes"	//	Set to no to disable error.log file.
var hubPort = 8082	//	Synched with Device Handlers.
//##################################################################

//---- Determine if old Node version, act accordingly -------------
console.log("Node.js Version Detected:   " + process.version)
var oldNode = "no"
if (process.version == "v6.0.0-pre") {
	oldNode ="yes"
	logFile = "no"
}

//---- Program set up and global variables -------------------------
var http = require('http')
var net = require('net')
var fs = require('fs')
var server = http.createServer(onRequest)

//---- Start the HTTP Server Listening to SmartThings --------------
server.listen(hubPort)
console.log("TP-Link Hub Console Log")
logResponse("\n\r" + new Date() + "\rTP-Link Hub Error Log")

//---- Command interface to Smart Things ---------------------------
function onRequest(request, response){
	var command = request.headers["command"]
	var deviceIP = request.headers["tplink-iot-ip"]
	var cmdRcvd = "\n\r" + new Date() + "\r\nIP: " + deviceIP + " sent command " + command
	console.log(" ")
	console.log(cmdRcvd)
	switch(command) {
		//---- (BridgeDH - Poll for Server APP ------------------
		case "pollServer":
			response.setHeader("cmd-response", "ok")
			response.end()
			var respMsg = "Server Poll response sent to SmartThings"
			console.log(respMsg)
		break

		//---- TP-Link Device Command ---------------------------
		case "deviceCommand":
			//----- Simulated power data Selection ----------------------
			var command = request.headers["tplink-command"]
			var action = request.headers["action"]
			if (action == "onWatts") {
				response.setHeader("action", "energyMeterResponse")
				response.setHeader("cmd-response", onWatts)
				response.end()
			} else if (action == "offWatts") {
				response.setHeader("action", "energyMeterResponse")
				response.setHeader("cmd-response", offWatts)
				response.end()
			} else {
			//----- Real data ----------------------
				processDeviceCommand(request, response)
			}
			break
	
		//---- Energy Meter Simulated Data Return ---------------
		case "emeterCmd":
			var action = request.headers["action"]
			var engrCmd = action.substring(0,3)
console.log("engrCmd = " + engrCmd)
			var engrData = action.substring(3)
console.log("engrData = " + engrData)
			if (engrCmd == "Con") {
				response.setHeader("action", "useTodayResponse")
			} else {
				response.setHeader("action", "engrStatsResponse")
			}
			var respData = ""
			switch(engrData) {
				case "DecWatts":
					respData = DecWatts
					break
				case "DecMWatts":
					respData = DecMWatts
					break
				case "JanWatts":
					respData = JanWatts
					break
				case "JanMWatts":
					respData = JanMWatts
					break
				case "FebWatts":
					respData = FebWatts
					break
				case "FebMWatts":
					respData = FebMWatts
					break
				case "MarWatts":
					respData = MarWatts
					break
				case "MarMWatts":
					respData = MarMWatts
					break
				case "Day1Watts":
					respData = Day1Watts
					break
				case "Day1MWatts":
					respData = Day1MWatts
					break
				case "Day2Watts":
					respData = Day2Watts
					break
				case "Day2MWatts":
					respData = Day2MWatts
					break
				case "Day3Watts":
					respData = Day3Watts
					break
				case "Day3MWatts":
					respData = Day3MWatts
					break
				default:
					break
			}
console.log(respData)
			response.setHeader("cmd-response", respData)
			response.end()
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

//---- Send deviceCommand and send response to SmartThings ---------
function processDeviceCommand(request, response) {
	var command = request.headers["tplink-command"]
	var deviceIP = request.headers["tplink-iot-ip"]
	var respMsg = "deviceCommand sending to IP: " + deviceIP + " Command: " + command
	console.log(respMsg)
	var action = request.headers["action"]
	response.setHeader("action", action)
	var socket = net.connect(9999, deviceIP)
	socket.setKeepAlive(false)
	socket.setTimeout(6000)
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

//----- Simulator Data Value -----

//----- WATT / KW HR ====
var DecWatts = '{"emeter":{"get_daystat":{"day_list":[{"year":2017,"month":12,"day":19,"energy":3.77},{"year":2017,"month":12,"day":20,"energy":4.956},{"year":2017,"month":12,"day":21,"energy":0.481},{"year":2017,"month":12,"day":22,"energy":4.608},{"year":2017,"month":12,"day":23,"energy":1.538},{"year":2017,"month":12,"day":24,"energy":0.304},{"year":2017,"month":12,"day":25,"energy":0.976},{"year":2017,"month":12,"day":26,"energy":3.595},{"year":2017,"month":12,"day":27,"energy":2.125},{"year":2017,"month":12,"day":28,"energy":1.372},{"year":2017,"month":12,"day":29,"energy":1.403},{"year":2017,"month":12,"day":30,"energy":3.405},{"year":2017,"month":12,"day":31,"energy":2.924}],"err_code":0}}}'

var DecMWatts = '{"smartlife.iot.common.emeter":{"get_daystat":{"day_list":[{"year":2017,"month":12,"day":19,"energy_wh":3770},{"year":2017,"month":12,"day":20,"energy_wh":4956},{"year":2017,"month":12,"day":21,"energy_wh":481},{"year":2017,"month":12,"day":22,"energy_wh":4608},{"year":2017,"month":12,"day":23,"energy_wh":1538},{"year":2017,"month":12,"day":24,"energy_wh":304},{"year":2017,"month":12,"day":25,"energy_wh":976},{"year":2017,"month":12,"day":26,"energy_wh":3595},{"year":2017,"month":12,"day":27,"energy_wh":2125},{"year":2017,"month":12,"day":28,"energy_wh":1372},{"year":2017,"month":12,"day":29,"energy_wh":1403},{"year":2017,"month":12,"day":30,"energy_wh":3405},{"year":2017,"month":12,"day":31,"energy_wh":2924}],"err_code":0}}}'

var JanWatts = '{"emeter":{"get_daystat":{"day_list":[{"year":2018,"month":1,"day":1,"energy":2.599},{"year":2018,"month":1,"day":2,"energy":3.392},{"year":2018,"month":1,"day":3,"energy":4.163},{"year":2018,"month":1,"day":4,"energy":1.207},{"year":2018,"month":1,"day":5,"energy":3.786},{"year":2018,"month":1,"day":6,"energy":2.17},{"year":2018,"month":1,"day":7,"energy":4.606},{"year":2018,"month":1,"day":8,"energy":1.56},{"year":2018,"month":1,"day":9,"energy":2.664},{"year":2018,"month":1,"day":10,"energy":4.567},{"year":2018,"month":1,"day":11,"energy":3.77},{"year":2018,"month":1,"day":12,"energy":4.956},{"year":2018,"month":1,"day":13,"energy":0.481},{"year":2018,"month":1,"day":14,"energy":4.608},{"year":2018,"month":1,"day":15,"energy":1.538},{"year":2018,"month":1,"day":16,"energy":0.304},{"year":2018,"month":1,"day":17,"energy":0.976},{"year":2018,"month":1,"day":18,"energy":3.595},{"year":2018,"month":1,"day":19,"energy":2.125},{"year":2018,"month":1,"day":20,"energy":1.372},{"year":2018,"month":1,"day":21,"energy":1.403},{"year":2018,"month":1,"day":22,"energy":3.405},{"year":2018,"month":1,"day":23,"energy":2.924},{"year":2018,"month":1,"day":24,"energy":2.599},{"year":2018,"month":1,"day":25,"energy":2.839},{"year":2018,"month":1,"day":26,"energy":1.329},{"year":2018,"month":1,"day":27,"energy":4.928},{"year":2018,"month":1,"day":28,"energy":4.282},{"year":2018,"month":1,"day":29,"energy":3.392},{"year":2018,"month":1,"day":30,"energy":4.163},{"year":2018,"month":1,"day":31,"energy":1.207}],"err_code":0}}}'

var JanMWatts = '{"smartlife.iot.common.emeter":{"get_daystat":{"day_list":[{"year":2018,"month":1,"day":1,"energy_wh":2599},{"year":2018,"month":1,"day":2,"energy_wh":3392},{"year":2018,"month":1,"day":3,"energy_wh":4163},{"year":2018,"month":1,"day":4,"energy_wh":1207},{"year":2018,"month":1,"day":5,"energy_wh":3786},{"year":2018,"month":1,"day":6,"energy_wh":2170},{"year":2018,"month":1,"day":7,"energy_wh":4606},{"year":2018,"month":1,"day":8,"energy_wh":1560},{"year":2018,"month":1,"day":9,"energy_wh":2664},{"year":2018,"month":1,"day":10,"energy_wh":4567},{"year":2018,"month":1,"day":11,"energy_wh":3770},{"year":2018,"month":1,"day":12,"energy_wh":4956},{"year":2018,"month":1,"day":13,"energy_wh":481},{"year":2018,"month":1,"day":14,"energy_wh":4608},{"year":2018,"month":1,"day":15,"energy_wh":1538},{"year":2018,"month":1,"day":16,"energy_wh":304},{"year":2018,"month":1,"day":17,"energy_wh":976},{"year":2018,"month":1,"day":18,"energy_wh":3595},{"year":2018,"month":1,"day":19,"energy_wh":2125},{"year":2018,"month":1,"day":20,"energy_wh":1372},{"year":2018,"month":1,"day":21,"energy_wh":1403},{"year":2018,"month":1,"day":22,"energy_wh":3405},{"year":2018,"month":1,"day":23,"energy_wh":2924},{"year":2018,"month":1,"day":24,"energy_wh":2599},{"year":2018,"month":1,"day":25,"energy_wh":2839},{"year":2018,"month":1,"day":26,"energy_wh":1329},{"year":2018,"month":1,"day":27,"energy_wh":4928},{"year":2018,"month":1,"day":28,"energy_wh":4282},{"year":2018,"month":1,"day":29,"energy_wh":3392},{"year":2018,"month":1,"day":30,"energy_wh":4163},{"year":2018,"month":1,"day":31,"energy_wh":1207}],"err_code":0}}}'

var FebWatts = '{"emeter":{"get_daystat":{"day_list":[{"year":2018,"month":2,"day":1,"energy":3.786},{"year":2018,"month":2,"day":2,"energy":2.17},{"year":2018,"month":2,"day":3,"energy":4.606},{"year":2018,"month":2,"day":4,"energy":1.56},{"year":2018,"month":2,"day":5,"energy":2.664},{"year":2018,"month":2,"day":6,"energy":4.567},{"year":2018,"month":2,"day":7,"energy":3.77},{"year":2018,"month":2,"day":8,"energy":4.956},{"year":2018,"month":2,"day":9,"energy":0.481},{"year":2018,"month":2,"day":10,"energy":4.608},{"year":2018,"month":2,"day":11,"energy":1.538},{"year":2018,"month":2,"day":12,"energy":0.304},{"year":2018,"month":2,"day":13,"energy":0.976},{"year":2018,"month":2,"day":14,"energy":3.595},{"year":2018,"month":2,"day":15,"energy":2.125},{"year":2018,"month":2,"day":16,"energy":1.372},{"year":2018,"month":2,"day":17,"energy":1.403},{"year":2018,"month":2,"day":18,"energy":3.405},{"year":2018,"month":2,"day":19,"energy":4.282},{"year":2018,"month":2,"day":20,"energy":3.392},{"year":2018,"month":2,"day":21,"energy":4.163},{"year":2018,"month":2,"day":22,"energy":1.207},{"year":2018,"month":2,"day":23,"energy":3.786},{"year":2018,"month":2,"day":24,"energy":2.17},{"year":2018,"month":2,"day":25,"energy":4.606},{"year":2018,"month":2,"day":26,"energy":1.56},{"year":2018,"month":2,"day":27,"energy":2.664},{"year":2018,"month":2,"day":28,"energy":4.567}],"err_code":0}}}'

var FebMWatts = '{"smartlife.iot.common.emeter":{"get_daystat":{"day_list":[{"year":2018,"month":2,"day":1,"energy_wh":3786},{"year":2018,"month":2,"day":2,"energy_wh":2170},{"year":2018,"month":2,"day":3,"energy_wh":4606},{"year":2018,"month":2,"day":4,"energy_wh":1560},{"year":2018,"month":2,"day":5,"energy_wh":2664},{"year":2018,"month":2,"day":6,"energy_wh":4567},{"year":2018,"month":2,"day":7,"energy_wh":3770},{"year":2018,"month":2,"day":8,"energy_wh":4956},{"year":2018,"month":2,"day":9,"energy_wh":481},{"year":2018,"month":2,"day":10,"energy_wh":4608},{"year":2018,"month":2,"day":11,"energy_wh":1538},{"year":2018,"month":2,"day":12,"energy_wh":304},{"year":2018,"month":2,"day":13,"energy_wh":976},{"year":2018,"month":2,"day":14,"energy_wh":3595},{"year":2018,"month":2,"day":15,"energy_wh":2125},{"year":2018,"month":2,"day":16,"energy_wh":1372},{"year":2018,"month":2,"day":17,"energy_wh":1403},{"year":2018,"month":2,"day":18,"energy_wh":3405},{"year":2018,"month":2,"day":19,"energy_wh":4282},{"year":2018,"month":2,"day":20,"energy_wh":3392},{"year":2018,"month":2,"day":21,"energy_wh":4163},{"year":2018,"month":2,"day":22,"energy_wh":1207},{"year":2018,"month":2,"day":23,"energy_wh":3786},{"year":2018,"month":2,"day":24,"energy_wh":2170},{"year":2018,"month":2,"day":25,"energy_wh":4606},{"year":2018,"month":2,"day":26,"energy_wh":1560},{"year":2018,"month":2,"day":27,"energy_wh":2664},{"year":2018,"month":2,"day":28,"energy_wh":4567}],"err_code":0}}}'

var MarWatts = '{"emeter":{"get_daystat":{"day_list":[{"year":2018,"month":3,"day":1,"energy":3.77},{"year":2018,"month":3,"day":2,"energy":4.956},{"year":2018,"month":3,"day":3,"energy":0.481},{"year":2018,"month":3,"day":4,"energy":4.608},{"year":2018,"month":3,"day":5,"energy":1.538},{"year":2018,"month":3,"day":6,"energy":0.304},{"year":2018,"month":3,"day":7,"energy":0.976},{"year":2018,"month":3,"day":8,"energy":3.595},{"year":2018,"month":3,"day":9,"energy":2.125},{"year":2018,"month":3,"day":10,"energy":1.372},{"year":2018,"month":3,"day":11,"energy":1.403},{"year":2018,"month":3,"day":12,"energy":3.405},{"year":2018,"month":3,"day":13,"energy":3.94},{"year":2018,"month":3,"day":14,"energy":3.77},{"year":2018,"month":3,"day":15,"energy":4.956},{"year":2018,"month":3,"day":16,"energy":0.481},{"year":2018,"month":3,"day":17,"energy":4.608},{"year":2018,"month":3,"day":18,"energy":1.538},{"year":2018,"month":3,"day":19,"energy":0.304},{"year":2018,"month":3,"day":20,"energy":1.56},{"year":2018,"month":3,"day":21,"energy":2.664},{"year":2018,"month":3,"day":22,"energy":4.567}],"err_code":0}}}'

var MarMWatts = '{"smartlife.iot.common.emeter":{"get_daystat":{"day_list":[{"year":2018,"month":3,"day":1,"energy_wh":3770},{"year":2018,"month":3,"day":2,"energy_wh":4956},{"year":2018,"month":3,"day":3,"energy_wh":481},{"year":2018,"month":3,"day":4,"energy_wh":4608},{"year":2018,"month":3,"day":5,"energy_wh":1538},{"year":2018,"month":3,"day":6,"energy_wh":304},{"year":2018,"month":3,"day":7,"energy_wh":976},{"year":2018,"month":3,"day":8,"energy_wh":3595},{"year":2018,"month":3,"day":9,"energy_wh":2125},{"year":2018,"month":3,"day":10,"energy_wh":1372},{"year":2018,"month":3,"day":11,"energy_wh":1403},{"year":2018,"month":3,"day":12,"energy_wh":3405},{"year":2018,"month":3,"day":13,"energy_wh":3940},{"year":2018,"month":3,"day":14,"energy_wh":3770},{"year":2018,"month":3,"day":15,"energy_wh":4956},{"year":2018,"month":3,"day":16,"energy_wh":481},{"year":2018,"month":3,"day":17,"energy_wh":4608},{"year":2018,"month":3,"day":18,"energy_wh":1538},{"year":2018,"month":3,"day":19,"energy_wh":304},{"year":2018,"month":3,"day":20,"energy_wh":1560},{"year":2018,"month":3,"day":21,"energy_wh":2664},{"year":2018,"month":3,"day":22,"energy_wh":4567}],"err_code":0}}}'

var Day1Watts = '{"emeter":{"get_daystat":{"day_list":[{"year":2018,"month":3,"day":1,"energy":3.77}],"err_code":0}}}'

var Day1MWatts = '{"smartlife.iot.common.emeter":{"get_daystat":{"day_list":[{"year":2018,"month":3,"day":1,"energy_wh":3770}],"err_code":0}}}'

var Day2Watts = '{"emeter":{"get_daystat":{"day_list":[{"year":2018,"month":3,"day":1,"energy":3.77},{"year":2018,"month":3,"day":2,"energy":4.956}],"err_code":0}}}'

var Day2MWatts = '{"smartlife.iot.common.emeter":{"get_daystat":{"day_list":[{"year":2018,"month":3,"day":1,"energy_wh":3770},{"year":2018,"month":3,"day":2,"energy_wh":4956}],"err_code":0}}}'

var Day3Watts = '{"emeter":{"get_daystat":{"day_list":[{"year":2018,"month":3,"day":1,"energy":3.77},{"year":2018,"month":3,"day":2,"energy":4.956},{"year":2018,"month":3,"day":3,"energy":0.481}],"err_code":0}}}'

var Day3MWatts = '{"smartlife.iot.common.emeter":{"get_daystat":{"day_list":[{"year":2018,"month":3,"day":1,"energy_wh":3770},{"year":2018,"month":3,"day":2,"energy_wh":4956},{"year":2018,"month":3,"day":3,"energy_wh":481}],"err_code":0}}}'

var Null = '{"emeter":{"get_daystat":{"day_list":[{}],"err_code":0}}}'

var onWatts = '{"emeter":{"get_realtime":{"power":1234.567,"err_code":0}}}'

var offWatts = '{"emeter":{"get_realtime":{"power":0,"err_code":0}}}'