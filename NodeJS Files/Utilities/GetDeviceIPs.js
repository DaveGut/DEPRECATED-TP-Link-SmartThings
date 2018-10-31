const dgram = require('dgram')
var os = require('os')


//	-------------------------------------------------------------------
console.log("List of TP-Link Devices with IP, MAC, Alias, and Model")
var interfaces = os.networkInterfaces()
for (var k in interfaces) {
	for (var k2 in interfaces[k]) {
		var address = interfaces[k][k2]
		if (address.family === 'IPv4' && !address.internal) {
			var bridgeIP = address.address
		}
	}
}
var bridgePort = 8090
var socket = dgram.createSocket('udp4')
var tplinkDeviceList = {}
setTimeout(displayDevices, 4000)
socket.on('error', function (err) {
	console.error('Client UDP error')
	console.trace(err)
	socket.close()
	emit('error', err)
}.bind())
socket.on('message', function (msg, rinfo) {
	var tplinkDevice = {}
	var device = JSON.parse(decrypt(msg).toString('ascii')).system.get_sysinfo
	if (device.mic_type == "IOT.SMARTBULB") {
		tplinkDNI = device.mic_mac
	} else {
		tplinkDNI = device.mac.replace(/:/g, "")
	}
	tplinkDevice['deviceIP'] = rinfo.address
	tplinkDevice['deviceModel'] = device.model
	tplinkDevice['deviceAlias'] = device.alias
	tplinkDevice['deviceId'] = device.deviceId
	tplinkDeviceList[tplinkDNI] = tplinkDevice
}.bind())
socket.bind(bridgePort, bridgeIP, () => {
	socket.setBroadcast(true)
})
var msgBuf = UdpEncrypt('{"system":{"get_sysinfo":{}}}')
socket.send(msgBuf, 0, msgBuf.length, 9999, '255.255.255.255')
//	-------------------------------------------------------------------
function displayDevices() {
	socket.close()
	console.log(tplinkDeviceList)
}
//	-------------------------------------------------------------------
function UdpEncrypt (input) {
	var buf = new Buffer(input.length)
	var key = 0xAB
	for (var i = 0; i < input.length; i++) {
		buf[i] = input.charCodeAt(i) ^ key
		key = buf[i]
	}
	return buf
}
//	-------------------------------------------------------------------
function TcpEncrypt (input) {
	var buf = Buffer.alloc(input.length)
	var key = 0xAB
	for (var i = 0; i < input.length; i++) {
		buf[i] = input.charCodeAt(i) ^ key
		key = buf[i]
	}
	var bufLength = Buffer.alloc(4)
	bufLength.writeUInt32BE(input.length, 0)
	return Buffer.concat([bufLength, buf], input.length + 4)
}
//	-------------------------------------------------------------------
function decrypt (input, firstKey) {
	var buf = Buffer.from(input); // node v6: Buffer.from(input)
	var key = 0x2B
	var nextKey
	for (var i = 0; i < buf.length; i++) {
		nextKey = buf[i]
		buf[i] = buf[i] ^ key
		key = nextKey
	}
	return buf
}