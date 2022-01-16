/**
 *  Xiaomi Humidifier(v.0.0.1)
 *
 * MIT License
 *
 * Copyright (c) 2018 fison67@nate.com
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

import groovy.json.JsonSlurper
import groovy.transform.Field

metadata {
	definition (name: "Xiaomi Humidifier", namespace: "fison67", author: "fison67") {
        capability "Switch"
        capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
		capability "Refresh"
		capability "Sensor"

        attribute "buzzer", "enum", ["on", "off"]
        attribute "water2", "string"
        attribute "water", "number"
        attribute "use_time", "string"
        attribute "dry", "enum", ["on", "off"]

        attribute "lastCheckin", "Date"

        command "buzzerOn"
        command "buzzerOff"
        command "setDryOn"
        command "setDryOff"
        command "dummy"
	}
}

def parse(String description) {
	log.debug "Parsing '${description}'"
}

def setInfo(String app_url, String id) {
	log.debug "${app_url}, ${id}"
	state.app_url = app_url
    state.id = id
}

def setStatus(params){
    log.debug "${params.key} : ${params.data}"

 	switch(params.key){
    case "relativeHumidity":
    	sendEvent(name:"humidity", value: params.data, unit: "%")
    	break;
    case "mode":
    	if(model == "Humidifier1") {
    	sendEvent(name:"mode", value: params.data + "1" )
        } else {
    	sendEvent(name:"mode", value: params.data )
	}
    	break;
    case "power":
    	if(params.data == "true") {
    	sendEvent(name:"switch", value:"on")
        }
        else if(params.data == "false") {
    		if(model == "Humidifier1") {
    		sendEvent(name:"mode", value: "off1")
	    	sendEvent(name:"switch", value:"off")
        	} else {
    		sendEvent(name:"mode", value: "off")
	    	sendEvent(name:"switch", value:"off")
		}
        }
        else { }
    	break;
    case "temperature":
		def para = "${params.data}"
		String data = para
		def st = data.replace("C","");
		def stf = Float.parseFloat(st)
		def tem = Math.round(stf*10)/10
        sendEvent(name:"temperature", value: tem, unit:"C")
        sendEvent(name:"temperature2", value: state.temp + ": " + tem, unit:"C")
    	break;
    case "useTime":
		def para = "${params.data}"
		String data = para
		def stf = Float.parseFloat(data)
		def hour = Math.round(stf/3600)
		int leftday = Math.floor(stf/3600/24)
		int lefthour = hour - leftday*24
        sendEvent(name:"use_time", value: leftday + "d " + lefthour + "h" )
    	break;
    case "targetHumidity":
        sendEvent(name:"level", value: params.data, unit: "%")
    	break;
    case "depth":
		def para = "${params.data}"
		String data = para
		def stf = Float.parseFloat(data)
		def water = Math.round(stf/12*10)
        sendEvent(name:"water", value: water )
        sendEvent(name:"water2", value: state.wdep + ": " + water )
    	break;
    case "buzzer":
    	sendEvent(name:"buzzer", value: (params.data == "true" ? "on" : "off") )
        break;
    case "dry":
    	sendEvent(name:"dry", value: params.data )
        break;
    }

    def nowT = new Date().format("HH:mm:ss", location.timeZone)
    def nowD = new Date().format("yyyy-MM-dd", location.timeZone)
    sendEvent(name: "lastCheckin", value: nowD + "\n" + nowT)
}

def refresh(){
	log.debug "Refresh"
    def options = [
     	"method": "GET",
        "path": "/devices/get/${state.id}",
        "headers": [
        	"HOST": parent._getServerURL(),
            "Content-Type": "application/json"
        ]
    ]
    sendCommand(options, callback)
}

def buzzerOn(){
	log.debug "buzzerOn >> ${state.id}"
    def body = [
        "id": state.id,
        "cmd": "buzzer",
        "data": "on"
    ]
    def options = makeCommand(body)
    sendCommand(options, null)
}

def buzzerOff(){
	log.debug "buzzerOff >> ${state.id}"
    def body = [
        "id": state.id,
        "cmd": "buzzer",
        "data": "off"
    ]
    def options = makeCommand(body)
    sendCommand(options, null)
}

def on(){
	log.debug "ON >> ${state.id}"
    def body = [
        "id": state.id,
        "cmd": "power",
        "data": "on"
    ]
    def options = makeCommand(body)
    sendCommand(options, null)
}

def off(){
	log.debug "Off >> ${state.id}"
	def body = [
        "id": state.id,
        "cmd": "power",
        "data": "off"
    ]
    def options = makeCommand(body)
    sendCommand(options, null)
}

def setDryOn(){
	log.debug "Dry ON >> ${state.id}"
    def body = [
        "id": state.id,
        "cmd": "dry",
        "data": "on"
    ]
    def options = makeCommand(body)
    sendCommand(options, null)
}

def setDryOff(){
	log.debug "Dry Off >> ${state.id}"
	def body = [
        "id": state.id,
        "cmd": "dry",
        "data": "off"
    ]
    def options = makeCommand(body)
    sendCommand(options, null)
}



def updated() {
    refresh()
}

def callback(physicalgraph.device.HubResponse hubResponse){
	def msg
    try {
        msg = parseLanMessage(hubResponse.description)
		def jsonObj = new JsonSlurper().parseText(msg.body)
        log.debug jsonObj
    	if(model == "Humidifier1") {
		if(jsonObj.properties.power == true){
			sendEvent(name:"mode", value: jsonObj.state.mode + "1")
			sendEvent(name:"switch", value: "on" )
		} else {
			sendEvent(name:"mode", value: "off1" )
			sendEvent(name:"switch", value: "off" )
		}
       		sendEvent(name:"water2", value: "N/A" )
		sendEvent(name:"dry", value: "dummy" )
        } else {
		if(jsonObj.properties.power == true){
			sendEvent(name:"mode", value: jsonObj.state.mode)
			sendEvent(name:"switch", value: "on" )
		} else {
			sendEvent(name:"mode", value: "off" )
			sendEvent(name:"switch", value: "off" )
		}
	    	sendEvent(name:"dry", value: jsonObj.state.dry )
	        sendEvent(name:"water", value: Math.round(jsonObj.properties.depth/12*10))
	        sendEvent(name:"water2", value: state.wdep + ": " + Math.round(jsonObj.properties.depth/12*10))
        }
        sendEvent(name:"temperature", value: jsonObj.properties.temperature.value, unit:"C")
        sendEvent(name:"temperature2", value: state.temp + ": " + jsonObj.properties.temperature.value, unit:"C")
        sendEvent(name:"relativeHumidity", value: jsonObj.properties.relativeHumidity, unit: "%")
        sendEvent(name:"buzzer", value: (jsonObj.state.buzzer == true ? "on" : "off"))
        sendEvent(name:"level", value: jsonObj.properties.targetHumidity)

    def nowT = new Date().format("HH:mm:ss", location.timeZone)
    def nowD = new Date().format("yyyy-MM-dd", location.timeZone)
    sendEvent(name: "lastCheckin", value: nowD + "\n" + nowT)
    } catch (e) {
        log.error "Exception caught while parsing data: "+e;
    }
}

def sendCommand(options, _callback){
	def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: _callback])
    sendHubCommand(myhubAction)
}

def makeCommand(body){
	def options = [
     	"method": "POST",
        "path": "/control",
        "headers": [
        	"HOST": parent._getServerURL(),
            "Content-Type": "application/json"
        ],
        "body":body
    ]
    return options
}
