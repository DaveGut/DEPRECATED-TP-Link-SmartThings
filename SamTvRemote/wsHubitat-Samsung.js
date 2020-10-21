/*
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
var samsungTv

//----- Start the HTTP Server Listening to Smart Hub --------------
server.listen(hubPort)
console.log("Hubitat WebSocket Version " + nodeAppVer)
logResponse("\n\r" + new Date() + "\rHubitat WebSocket Error Log")

//----- Command interface to Smart Things ---------------------------
function onRequest(request, response){
	var command = request.headers.command
	var data = request.headers.data
	var url = request.headers.url
	var cmdRcvd = "\n\rCommand Data = " + command + " | " + data + " | " + url
	logResponse(" ")
	logResponse(cmdRcvd)
	console.log(" ")
	console.log(cmdRcvd)

	response.setHeader("command", command)
	switch(command) {
		case "hubCheck":
			setTimeout(returnResp, 100)
			cmdResponse['status'] = "OK"
			break
		case "connect":
			setTimeout(returnResp, 5000)
			wsSend(url, "")
			break
		case "sendMessage":
			setTimeout(returnResp, 100)
			samsungTv.send(data)
			break
		case "close":
			setTimeout(returnResp, 100)
			samsungTv.close()
			break
		default:
			var respMsg = "#### Invalid Command " + command + " ####"
			console.log(respMsg)
			logResponse(respMsg)
			response.setHeader("cmdResponse", "Invalid Command")
			response.end()
	}
	function returnResp() {
		console.log(cmdResponse)
		response.setHeader("cmdResponse", JSON.stringify(cmdResponse))
		response.end()
	}
}

function wsSend(connectUrl, data, response) {
	cmdResponse = {}
	samsungTv = new WebSocket(connectUrl, { rejectUnauthorized: false });
	samsungTv.on('open', () => {
		if (data != "") {
			samsungTv.send(data)
		}
		cmdResponse['wsStatus'] = "opened"
	});
	samsungTv.on('close', (code, reason) => {
		cmdResponse['wsStatus'] = "closed"
	});
	samsungTv.on('error', (err) => {
		cmdResponse['error'] = err
	});
	samsungTv.on('message', (data) => {
		cmdResponse['respData'] = data
	});
}


//----- Utility - Response Logging Function ------------------------
function logResponse(respMsg) {
	if (logFile == "yes") {
		fs.appendFileSync("error.log", "\r" + respMsg)
	}
}
