/* groovylint-disable LineLength, DuplicateNumberLiteral, DuplicateStringLiteral, ImplementationAsType, ImplicitClosureParameter, InvertedCondition, LineLength, MethodReturnTypeRequired, MethodSize, NestedBlockDepth, NglParseError, NoDef, NoJavaUtilDate, NoWildcardImports, ParameterReassignment, UnnecessaryGString, UnnecessaryObjectReferences, UnnecessaryToString, UnusedImport, VariableTypeRequired */
/**
 *  Unifi Integration Manager
 *
 *  Copyright 2018 CRAIG KING
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License
 */

definition(
    name: 'Unifi Integration Manager (Snell)',
    namespace: 'Mavrrick',
    author: 'CRAIG KING',
    description: 'Unifi Integration for HE',
    category: 'Lighting',
    documentationLink: "https://docs.google.com/document/d/e/2PACX-1vRsjfv0eefgPGKLYffNpbZWydtp0VqxFL_Xcr-xjRKgl8vga18speyGITyCQOqlQmyiO0_xLJ9_wRqU/pub",
    iconUrl: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg',
    iconX2Url: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg',
    iconX3Url: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg',
    singleThreaded: true,
    singleInstance: true)

/*
* Initial release v1.0.0 
*/

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field
import hubitat.helper.HexUtils

@Field static List child = []
@Field static List childDNI = []
@Field static String statusMessage = ""
@Field static String BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"


preferences
{
    page(name: 'mainPage', title: 'Unifi Integration Manager')
    page(name: 'setup', title: 'Setup values for your Unifi Integrations')
    page(name: 'pageEnableAPI')
    page(name: "pageDisableAPI")
 //   page(name: 'about', title: 'About')
    mappings {
        path("/") {
            action: [
            GET: "webHook"
            ]
        }
    }
}

/*
    mainPage

    UI Page: Main menu for the app.
*/
def mainPage() {
    atomicState.backgroundActionInProgress = null
    statusMessage = ""

    def int childCount = child.size()
    dynamicPage(name: 'mainPage', title: 'Unifi integration Main menu', uninstall: true, install: true, submitOnChange: true)
    {
        section('<b>Unifi Setup Menu</b>') {
                href 'setup', title: 'Unifi Environmnt Setup', description: 'Click to load values for Unifi Integrations.'
            }
        section('<b>Inbound Websocket setup</b>') {
         if (state.accessToken == null) {
                paragraph("API is not yet Initialized!")
                href(name: "hrefPageEnableAPI", title: "Enable API", description: "", page: "pageEnableAPI")
            } else {
		        section("Instructions:", hideable: true, hidden: true) {
                    paragraph("Put the following URL into Unifi Protect Alert manager. This app will interpreate the additional parms to update the appropriate integrated device.")
                }
                
  		        section("URLs") {
                    String localURL = "${state.localAPIEndpoint}/?access_token=${state.accessToken}&dni=%DEVICE_DNI%&type=%DETECTION_TYPE%&value=%Additional_PARM%"
                    String remoteURL = "${state.remoteAPIEndpoint}/?access_token=${state.accessToken}&dni=%DEVICE_DNI%&type=%DETECTION_TYPE%&value=%Additional_PARM%"
                    paragraph("LOCAL API (devices): <a href=\"$localURL\" target=\"_blank\">$localURL</a>")
                    paragraph("REMOTE API: <a href=\"$remoteURL\" target=\"_blank\">$remoteURL</a>")
                }
            }
        }
            
        section("<b>Outbound Webhook Calls</b>") {
            paragraph "Outbound Webhook Trigger Child Apps"
            if (unifiApiToken){
                app(name: "Outbound Webhook App", appName: "Unifi Outbound-Webhook", namespace: "Mavrrick", title: "Add Alarm Manager Webhook app", multiple: true)
            } else {
                paragraph "<b>No API Token Configured for integration. Please setup API token for Outbound Webhook Child apps to be avaliable</b>"
            }
        }     
        section('<b>Logging Options</b>') {
            input(
                name: 'configLoggingLevelIDE',
                title: 'IDE Live Logging Level:\nMessages with this level and higher will be logged to the IDE.',
                type: 'enum',
                options: [
                    '0' : 'None',
                    '1' : 'Error',
                    '2' : 'Warning',
                    '3' : 'Info',
                    '4' : 'Debug',
                    '5' : 'Trace'
                ],
                defaultValue: '1',
                displayDuringSetup: true,
                required: false
            )
        }
 /*       section('About') {
            href 'about', title: 'About Information menu', description: 'About Menu for Govee Integration'
        } */
    }
}

/* def setup() {

    logger('setup() Credential Controll', 'debug')
    dynamicPage(name: 'setup', title: 'Unifi Integrations configuration', uninstall: false, install: false, submitOnChange: true, nextPage: "mainPage")
    {
        section('Environment') 
        {
            input 'unifiControllerType', 'enum', title: 'Please select the controller type', required: true, submitOnChange: true, options:[ "Unifi Dream Machine (inc Pro)", "Other Unifi Controllers" ]
            input 'unifiControllerIP', 'string', title: 'Please enter the IP of your controller', required: true, submitOnChange: true
        }
        section('Integrations to enable') 
        {
            input 'unifiNetwork', 'bool', title: 'Unifi Network Integration', required: false, submitOnChange: true
            input 'unifiProtect', 'bool', title: 'Unifi Protect Integration', required: false, submitOnChange: true
        }
        section('Unifi Credential')
        {
            paragraph "Please provide your Unifi Credentials setup on Controller for use with Hubitat."
            input 'unifiUserID', 'string', title: 'Please enter your controller User ID', required: false, submitOnChange: true
            input 'unifiPassword', 'password', title: 'Please enter your controller password', required: false, submitOnChange: true            
        }
        section('API Token')
        {
        paragraph "Please provide your Unifi Integration API Token."
            input 'unifiApiToken', 'string', title: 'Please enter your API Token', required: true, submitOnChange: true
        }
    }
} */

def setup() {

    logger('setup() Integration setup', 'debug')
    dynamicPage(name: 'setup', title: '<b>Unifi Integrations configuration</b>', uninstall: false, install: false, submitOnChange: true, nextPage: "mainPage")
    {
        section('Avaliable Integrations') 
        {
            
            input 'unifiNetwork', 'enum', title: 'Unifi Network Integration', required: true, submitOnChange: true, options:[ "Not Enabled", "Managed", "External" ], default: "Not Enabled"
            if (unifiNetwork == "Managed"){
                input 'unifiNetControllerType', 'enum', title: 'Please select the controller type Protect', required: true, submitOnChange: false, options:[ "Unifi Dream Machine (inc Pro)", "Other Unifi Controllers" ]
                input 'unifiNetControllerIP', 'string', title: 'Please enter the IP of your Unifi Network controller', required: true, submitOnChange: false
                input 'unifiNetUserID', 'string', title: 'Please enter your controller User ID', required: false, submitOnChange: false
                input 'unifiNetPassword', 'password', title: 'Please enter your controller password', required: false, submitOnChange: false            
            } else if (unifiNetwork == "External"){
                input name: "unifiNetDevice", type: "device.UnifiNetworkAPI", title: "Choose device"
            }
            input 'unifiProtect', 'enum', title: 'Unifi Protect Integration', required: true, submitOnChange: true, options:[ "Not Enabled", "Managed", "External" ]
            if (unifiProtect == "Managed"){
                input 'unifiProControllerType', 'enum', title: 'Please select the controller type for Portect', required: true, submitOnChange: false, options:[ "Unifi Dream Machine (inc Pro)", "Other Unifi Controllers" ]
                input 'unifiProControllerIP', 'string', title: 'Please enter the IP of your Protect Controllercontroller', required: true, submitOnChange: false
                input 'unifiProUserID', 'string', title: 'Please enter your controller User ID', required: false, submitOnChange: false
                input 'unifiProPassword', 'password', title: 'Please enter your controller password', required: false, submitOnChange: false            
            } else if (unifiProtect == "External"){
                input name: "unifiProDevice", type: "device.UnifiProtectAPI", title: "Choose device"
            }
            input 'unifiConnect', 'enum', title: 'Unifi Connect Integration', required: true, submitOnChange: true, options:[ "Not Enabled", "Managed", "External" ]
            if (unifiConnect == "Managed"){
                input 'unifiConControllerType', 'enum', title: 'Please select the controller type Connect', required: true, submitOnChange: false, options:[ "Unifi Dream Machine (inc Pro)", "Other Unifi Controllers" ]
                input 'unifiConControllerIP', 'string', title: 'Please enter the IP of your Connect controller', required: true, submitOnChange: false
                input 'unifiConUserID', 'string', title: 'Please enter your controller User ID', required: false, submitOnChange: false
                input 'unifiConPassword', 'password', title: 'Please enter your controller password', required: false, submitOnChange: false            
            } else if (unifiProtect == "External"){
                input name: "unifiConDevice", type: "device.UnifiConnectAPI", title: "Choose device"
            }
       }
        section('API Token')
        {
        paragraph "Please provide your Unifi Integration API Token."
            input 'unifiApiToken', 'string', title: 'Please enter your API Token', required: false, submitOnChange: true
        }
    }
}

String initializeAPIEndpoint() {
    if(!state.accessToken) {
        if(createAccessToken() != null) {
            state.endpoint = getApiServerUrl()
            state.localAPIEndpoint = getFullLocalApiServerUrl()
            state.remoteAPIEndpoint = getFullApiServerUrl()
        }
    }
    return state.accessToken
}

/* Pages */
Map pageDisableAPI() {
    dynamicPage(name: "pageDisableAPI") {
        section() {
            if (state.accessToken != null) {
                state.accessToken = null
                state.endpoint = null
                paragraph("SUCCESS: API Access Token REVOKED! Tap Done to continue")
            }
        }
    }
}

Map pageEnableAPI() {
    dynamicPage(name: "pageEnableAPI", title: "", nextPage: "mainPage") {
        section() {
            if(state.accessToken == null) {
                initializeAPIEndpoint()
            }
            if (state.accessToken == null){
                paragraph("FAILURE: API NOT Initialized!")
            } else {
                paragraph("SUCCESS: API Initialized! Tap Done to continue")
            }
        }
    }
}


def about() {
    dynamicPage(name: 'about', title: 'About Govee Integration with HE', uninstall: false, install: false, nextPage: "mainPage")
    {
        section()
        {
            paragraph image: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg', 'Govee Integration'
        }
        section('Support the Project')
        {
            paragraph 'Govee is provided free for personal and non-commercial use.  I have worked on this app in my free time to fill the needs I have found for myself and others like you.  I will continue to make improvements where I can. If you would like you can donate to continue to help with development please use the link below.'
            href(name: 'donate', style:'embedded', title: "Consider making a \$5 or \$10 donation today to support my ongoing effort to continue improving this integration.", url: 'https://www.paypal.me/mavrrick58')
            paragraph("<style>/* The icon */ .help-tip{ 	position: absolute; 	top: 50%; 	left: 50%; 	transform: translate(-50%, -50%); 	margin: auto; 	text-align: center; 	border: 2px solid #444; 	border-radius: 50%; 	width: 40px; 	height: 40px; 	font-size: 24px; 	line-height: 42px; 	cursor: default; } .help-tip:before{     content:'?';     font-family: sans-serif;     font-weight: normal;     color:#444; } .help-tip:hover p{     display:block;     transform-origin: 100% 0%;     -webkit-animation: fadeIn 0.3s ease;     animation: fadeIn 0.3s ease; } /* The tooltip */ .help-tip p {    	display: none; 	font-family: sans-serif; 	text-rendering: optimizeLegibility; 	-webkit-font-smoothing: antialiased; 	text-align: center; 	background-color: #FFFFFF; 	padding: 12px 16px; 	width: 178px; 	height: auto; 	position: absolute; 	left: 50%; 	transform: translate(-50%, 5%); 	border-radius: 3px; /* 	border: 1px solid #E0E0E0; */ 	box-shadow: 0 0px 20px 0 rgba(0,0,0,0.1); 	color: #37393D; 	font-size: 12px; 	line-height: 18px; 	z-index: 99; } .help-tip p a { 	color: #067df7; 	text-decoration: none; } .help-tip p a:hover { 	text-decoration: underline; } /* The pointer of the tooltip */ .help-tip p:before { 	position: absolute; 	content: ''; 	width: 0; 	height: 0; 	border: 10px solid transparent; 	border-bottom-color:#FFFFFF; 	top: -9px; 	left: 50%; 	transform: translate(-50%, -50%); }  /* Prevents the tooltip from being hidden */ .help-tip p:after { 	width: 10px; 	height: 40px; 	content:''; 	position: absolute; 	top: -40px; 	left: 0; } /* CSS animation */ @-webkit-keyframes fadeIn {     0% { opacity:0; }     100% { opacity:100%; } } @keyframes fadeIn {     0% { opacity:0; }     100% { opacity:100%; } }</style><div class='help-tip'><p>This is the inline help tip! It can contain all kinds of HTML. Style it as you please.<br /><a href='#'>Here is a link</a></p></div>")
        }
    }
}

def installed() {
    if (unifiNetwork == "Managed"){
        unifiNetInstall()
    }
    if (unifiProtect == "Managed"){
        unifiProInstall()
    }
    if (unifiConnect == "Managed"){
        unifiConInstall()
    }
    state.isInstalled = true
}

def updated() {
    if (unifiNetwork == "Managed"){
        unifiNetInstall()
    }
    if (unifiProtect == "Managed"){
        unifiProInstall()
    }
    if (unifiConnect == "Managed"){
        unifiConInstall()
    }
}

def uninstalled() {
    // external cleanup. No need to unsubscribe or remove scheduled jobs
    // 1.4 Remove dead virtual devices
    getChildDevices()?.each
    { childDevice ->
            deleteChildDevice(childDevice.deviceNetworkId)
    }
}

def webHook () {
    log.debug("Processing a webHook() $params")
    log.debug("Processing a webHook() $params.dni $params.type $params.value")
    String devicedni = params.dni.toString()
    String type = params.type.toString()
    Stringvalue = params.value.toString()
	unifiProDevice.ApplyWebHook(devicedni, type, value)
}

void unifiNetInstall() {
    List childDNI = getChildDevices().deviceNetworkId
    if (childDNI.contains("UnifiNetworkAPI") == false) {
        logger("unifiNetInstall()  configuring Govee v2 Device Manager", 'info')
        addChildDevice('Snell', 'UnifiNetworkAPI', "UnifiNetworkAPI" , location.hubs[0].id, [
            'name': 'UnifiNetworkAPI',
            'label': 'UnifiNetworkAPI',
             'data': [
                'apiKey': settings.unifiApiToken
             ],
             'completedSetup': true,
         ])
    }
    device = getChildDevice('UnifiNetworkAPI')
    device.updateSettings (unifiNetControllerType, unifiNetControllerIP, unifiNetUserID, unifiNetPassword)
}

void unifiProInstall() {
    List childDNI = getChildDevices().deviceNetworkId
    if (childDNI.contains("UnifiProtectAPI") == false) {
        logger("unifiProInstall()  configuring Govee v2 Device Manager", 'info')
        addChildDevice('Snell', 'UnifiProtectAPI', "UnifiProtectAPI" , location.hubs[0].id, [
            'name': 'UnifiProtectAPI',
            'label': 'UnifiProtectAPI',
             'data': [
                'apiKey': settings.unifiApiToken
             ],
             'completedSetup': true,
         ])
    }
    device = getChildDevice('UnifiProtectAPI')
    device.updateSettings (unifiProControllerType, unifiProControllerIP, unifiProUserID, unifiProPassword)    
}

void unifiConInstall() {
    List childDNI = getChildDevices().deviceNetworkId
    if (childDNI.contains("UnifiConnectAPI") == false) {
        logger("unifiConInstall()  configuring Govee v2 Device Manager", 'info')
        addChildDevice('Snell', 'UnifiConnectAPI', "UnifiConnectAPI" , location.hubs[0].id, [
            'name': 'UnifiConnectAPI',
            'label': 'UnifiConnectAPI',
             'data': [
                'apiKey': settings.unifiApiToken
             ],
             'completedSetup': true,
         ])
    } 
    device = getChildDevice('UnifiProtectAPI')
    device.updateSettings (unifiConControllerType, unifiConControllerIP, unifiConUserID, unifiConPassword)     
}

def goveeSceneRetrieve(String model) {
    if (goveeDevPtURL.contains(model)) {
        logger("goveeSceneRetrieve() Device is not eligiable to be extracted ignoring", 'debug')                                   
    } else {
    logger("goveeSceneRetrieve() Processing Scene retrieval for models ${model}", 'debug')
    def params = [
        uri   : 'https://app2.govee.com',
        path  : '/appsku/v1/light-effect-libraries',
        headers: [ 'appVersion': '9999999'],
        query: ['sku': model],
        ]
    logger("goveeSceneRetrieve(): Calling HTTP server with ${params}", 'debug')
    try {
        httpGet(params) { resp ->
            goveeApiRespons = resp.data.data.categories.scenes
            sceneNames = []
            sceneCodes = []
            sceneParms = []
            String convrtCmd = "" 
            goveeApiRespons.forEach {     
                sceneNames = sceneNames.plus(it.sceneName)                
                sceneCodes = sceneCodes.plus(it.lightEffects.sceneCode)
                sceneParms = sceneParms.plus(it.lightEffects.scenceParam)
            }
            logger("goveeSceneRetrieve(): Size of scene data fields ${sceneNames.size()} size ${sceneCodes.size()} size ${sceneParms.size()}", 'trace')
            recNum = 0
            sceneNames.forEach {
                logger("goveeSceneRetrieve(): records for each variable Name: ${sceneNames.get(recNum)} Scene Code: ${sceneCodes.get(recNum).get(0)} Parm: ${sceneParms.get(recNum).get(0)}", 'debug')                                
				String strSceneParm = sceneParms.get(recNum).get(0)
                def sccode = HexUtils.integerToHexString(sceneCodes.get(recNum).get(0),2)
                def hexString = base64ToHex(strSceneParm)
                def hexSize = hexString.length() // each line is 35 charters except the first one which is 6 less
				if (goveeDevOffsets.containsKey(model)) { // if present subtract offset value from string for calculations
                    hexSize = hexSize - goveeDevOffsets."${model}".offset
            	}
                int splits = 0
                if (isWholeNumber((hexSize - 28) / 34)) {
                    logger("goveeSceneRetrieve(): Split is a whole number ${(hexSize - 28) / 34}", 'trace')
                    splits = (int) Math.floor(((hexSize - 28) / 34) -1)
                } else {
                    logger("goveeSceneRetrieve(): Split is not whole number ${(hexSize - 28) / 34}", 'trace')
                    splits = (int) Math.floor((hexSize - 28) / 34) 
                }                              
                int action = 0
                def position = 28
                if (goveeDevOffsets.containsKey(model)) { // if present set position of next line to start at appropriate location
                    position =  goveeDevOffsets."${model}".line1End
            	}
                convrtCmd = ""
                logger("goveeSceneRetrieve(): SceneParm converted to hex:  ${hexString} Lenght: ${hexSize} Splits ${splits}", 'trace')
                if (strSceneParm != null && strSceneParm != "") {
                	while(splits + 1 >= action) {
                    	logger("goveeSceneRetrieve(): SceneParm converted to on total splits:  ${splits} on action : ${action} ", 'trace')
                    	if (action == 0) {
                        	String section = ""
                            String id = ""
                        	String lineHeader = "a"+ (300 + action)
                            if (deviceTag.containsKey(model)) {
                            	id = ("01" + HexUtils.integerToHexString(splits+2,1) + deviceTag."${model}").toLowerCase()
                                if (hexSize < 28) {
                                    section = hexString.substring(goveeDevOffsets."${model}".start)
                                } else {
                                	section = hexString.substring(goveeDevOffsets."${model}".start,goveeDevOffsets."${model}".line1End)
                                }
                            } else {
                                id = ("01" + HexUtils.integerToHexString(splits+2,1) +"02").toLowerCase()
                                if (hexSize < 28) {
                                    section = hexString.substring(0)
                                } else {
                                	section = hexString.substring(0,28)
                                }
                            }
                        	action = action + 1
                            String minusChkSum = lineHeader+id+section
                            logger("goveeSceneRetrieve(): Minus Checksum :  ${minusChkSum} ", 'trace')
                            checksum = calculateChecksum8Xor(minusChkSum).toLowerCase()
                            hexConvString = lineHeader+id+section+checksum
                        	logger("goveeSceneRetrieve(): Parsing first line :  ${hexConvString} ", 'trace')                        
                        	logger("goveeSceneRetrieve(): Parsing first line :  ${lineHeader}${id}${section}${checksum} ", 'trace')
                            base64String = hexToBase64(hexConvString)
                            logger("goveeSceneRetrieve(): Base64 Command first line :  ${base64String} ", 'trace')
                            convrtCmd = '"'+ base64String  +'"'                        
                    	} else if (action > 0 && action <= (splits )) {
                        	String section = hexString.substring(position , position+34)
                        	String lineHeader = "a3" + (HexUtils.integerToHexString(action,1)) 
                        	action = action +1
                        	position = position + 34
                            String minusChkSum = lineHeader+section
                            checksum = calculateChecksum8Xor(minusChkSum).toLowerCase()
                            hexConvString = lineHeader+section+checksum                       
                        	logger("goveeSceneRetrieve(): Parsing Middle line :  ${lineHeader}${section}${checksum} ", 'trace')
                            base64String = hexToBase64(hexConvString)
                            logger("goveeSceneRetrieve(): Base64 Command Middle line :  ${base64String} ", 'trace')
                            convrtCmd = convrtCmd + ',"' + base64String + '"'
                    	}  else if (action > splits) {
                        	action = action + 1
                        	String section = hexString.substring(position)
                        	def sectionLen = section.length()
                        	def needLen  = 37 - sectionLen
                        	def sectionPad = section.padRight(34,'0')
                        	String lineHeader = "a3ff"
                            String minusChkSum = lineHeader+sectionPad
                            checksum = calculateChecksum8Xor(minusChkSum).toLowerCase()
                            hexConvString = lineHeader+sectionPad+checksum
                        	logger("goveeSceneRetrieve(): Parsing last line padding review : Section data${section}, Section Length ${sectionLen}, padding needed ${needLen}, padded value ${sectionPad} ", 'trace')                        
                        	logger("goveeSceneRetrieve(): Parsing last line :  ${lineHeader}${sectionPad}${checksum} ", 'trace')
                            base64String = hexToBase64(hexConvString)
                            logger("goveeSceneRetrieve(): Base64 Command last command line :  ${base64String} ", 'trace')
                            convrtCmd = convrtCmd + ',"' + base64String + '"'
                        } else {
                        	logger("goveeSceneRetrieve(): Parsing error aborting ", 'trace')
                        }
                    }    
                }
                logger("goveeSceneRetrieve(): scene code :  ${sccode} ", 'trace')
                String lastLine = ""
                if (deviceTagll.containsKey(model)) {
                    lastLine = ("330504"+sccode.substring(2)+sccode.substring(0,2)+"00"+deviceTagll."${model}"+"000000000000000000000000").toLowerCase()
                } else {
                    lastLine = ("330504"+sccode.substring(2)+sccode.substring(0,2)+"0000000000000000000000000000").toLowerCase()
                }
                checksum = calculateChecksum8Xor(lastLine).toLowerCase()
                hexConvString = lastLine+checksum
                logger("goveeSceneRetrieve(): final line to complete command is needed. :  ${lastLine}${checksum} ", 'trace')
                base64String = hexToBase64(hexConvString)
                logger("goveeSceneRetrieve(): Base64 Command fine line :  ${base64String} ", 'trace')
                if (convrtCmd == "") {
                   diyAddManual = '["'+ base64String + '"]' 
                } else {               
                	diyAddManual = "["+convrtCmd + ',"' + base64String + '"]'
                }
                logger("goveeSceneRetrieve(): Base64 command list:  ${diyAddManual} ", 'debug')
                sceneFileCreate(model, sceneNames.get(recNum), diyAddManual)
                recNum = recNum + 1
                }
            if  (resp.data.data.categories.isEmpty()) {
                logger("goveeSceneRetrieve(): Device ${model} does not have scenes. Ignoring", 'debug')
            } else {
		    sceneFileMax(model, sceneNames.size())
            }                
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        logger("goveeSceneRetrieve() Error: e.statusCode ${e.statusCode}", 'error')
        logger("goveeSceneRetrieve() ${e}", 'error')

        return 'unknown'
    }
    logger("goveeSceneRetrieve(): Device Extraction complete for model ${model} ", 'debug')
    }
}

def sceneFileCreate(devSKU, diyName, command) {
    logger("sceneFileCretae(): Attempting add DIY Scene ${devSKU}:${diyName}:${command}", 'trace')
//    command = command.inspect().replaceAll("\'", "\"")
	Map diyEntry = [:]
    diyEntry.put("name", diyName)
    diyEntry.put("cmd", command)
    logger("sceneFileCretae(): Trying to add ${diyEntry}", 'debug')
    logger("sceneFileCretae(): keys are  ${goveeScene.keySet()}", 'trace')
    diySize = goveeScene.size()
    if (diySize == 0){
        int diyAddNum = 101
        Map diyEntry2 = [:]
        diyEntry2.put(diyAddNum,diyEntry)
        goveeScene.put(devSKU,diyEntry2)
    } else {
        diySize = goveeScene."${devSKU}".size()
        int diyAddNum = (diySize + 101).toInteger()
        goveeScene."${devSKU}".put(diyAddNum,diyEntry)
    }
    writeGoveeSceneFile(devSKU)
}

def sceneFileMax(devSKU, int maxNum) {
    Map diyEntry = [:]
    int maxScene = 100 + maxNum
    diyEntry.put("maxScene", maxScene)
    int diyAddNum = 999
    goveeScene."${devSKU}".put(diyAddNum, diyEntry)
    writeGoveeSceneFile(devSKU)
}

def diyAdd(devSKU, diyName, command) {
    def slurper = new JsonSlurper()
    logger("diyAdd(): Attempting add DIY Scene ${devSKU}:${diyName}:${command}", 'trace')
    command = command.inspect().replaceAll("\'", "\"")
    Map diyEntry = [:]
    diyEntry.put("name", diyName)
    diyEntry.put("cmd", command)
    logger("diyAdd(): Trying to add ${diyEntry}", 'debug')
    logger("diyAdd(): keys are  ${state.diyEffects.keySet()}", 'debug')
    if (state.diyEffects.containsKey(devSKU) == false) {
        logger("diyAdd(): Device ${devSKU} not found", 'debug')
        logger("diyAdd(): New Device. Starting at 1001", 'debug')
        int diyAddNum = 1001
        Map diyEntry2 = [:]
        diyEntry2.put(diyAddNum,diyEntry)
        state.diyEffects.put(devSKU,diyEntry2)
    } else {
        logger("diyAdd(): keys are  ${state.diyEffects."${devSKU}".keySet()}", 'debug')
        nameList  = []
        scenelist = state.diyEffects."${devSKU}".keySet()
        scenelist.forEach {
            logger("diyAdd(): Adding Scene ${state.diyEffects."${devSKU}"."${it}".name} to compare list", 'debug')
            nameList.add(state.diyEffects."${devSKU}"."${it}".name)    
        }
        logger("diyAdd(): Scene Name Compare list ${nameList}", 'debug')
       
        if (nameList.contains(diyName)) {
            logger("diyAdd(): Scene with same name already present", 'debug')
            } else {
            logger("diyAdd(): Device ${devSKU} was found. Adding Scene to existing scene list", 'debug')
            diySize = state.diyEffects."${devSKU}".size()
            diyAddNum = (diySize + 1001).toInteger()
            logger("diyAdd(): Current DiY size is ${diySize}", 'debug')
            state.diyEffects."${devSKU}".put(diyAddNum,diyEntry)
        }
    }
    writeDIYFile()
}

/**
 *  diyAddManual()
 *
 *  Method to manually add shared Scenes to Hubitat.
 **/

def diyAddManual(String devSKU, String diyName, String command) {
    logger("diyAdd(): Attempting add DIY Scene ${devSKU}:${diyName}:${command}", 'trace')
    Map diyEntry = [:]
    diyEntry.put("name", diyName)
    diyEntry.put("cmd", command)
    logger("diyAdd(): Trying to add ${diyEntry}", 'debug')
    logger("diyAdd(): keys are  ${state.diyEffects.keySet()}", 'debug')
    if (state.diyEffects.containsKey(devSKU) == false) {
        logger("diyAdd(): Device ${devSKU} not found", 'debug')
        logger("diyAdd(): New Device. Starting at 1001", 'debug')
        int diyAddNum = 1001
        Map diyEntry2 = [:]
        diyEntry2.put(diyAddNum,diyEntry)
        state.diyEffects.put(devSKU,diyEntry2)
    } else {
        logger("diyAdd(): keys are  ${state.diyEffects."${devSKU}".keySet()}", 'debug')
        nameList  = []
        scenelist = state.diyEffects."${devSKU}".keySet()
        scenelist.forEach {
            logger("diyAdd(): Adding Scene ${state.diyEffects."${devSKU}"."${it}".name} to compare list", 'debug')
            nameList.add(state.diyEffects."${devSKU}"."${it}".name)    
        }
        logger("diyAdd(): Scene Name Compare list ${nameList}", 'debug')
       
        if (nameList.contains(diyName)) {
            logger("diyAdd(): Scene with same name already present", 'debug')
            } else {
            logger("diyAdd(): Device ${devSKU} was found. Adding Scene to existing scene list", 'debug')
            diySize = state.diyEffects."${devSKU}".size()
            diyAddNum = (diySize + 1001).toInteger()
            logger("diyAdd(): Current DiY size is ${diySize}", 'debug')
            state.diyEffects."${devSKU}".put(diyAddNum,diyEntry)
        }
    }
    writeDIYFile()
}

/**
 *  diyUpdateManual()
 *
 *  Method to manually add shared Scenes to Hubitat.
 **/
def diyUpdateManual(String devSKU, int diyAddNum, String diyName, String command) {
    logger("diyUpdateManual(): Attempting add DIY Scene ${devSKU}:${diyAddNum}:${diyName}:${command}", 'trace')
    Map diyEntry = [:]
    diyEntry.put("name", diyName)
    diyEntry.put("cmd", command)
    logger("diyUpdateManual(): Trying to add ${diyEntry}", 'debug')
    logger("diyUpdateManual(): keys are  ${state.diyEffects.keySet()}", 'debug')
    if (state.diyEffects.containsKey(devSKU) == false) {
        logger("diyUpdateManual(): Device ${devSKU} not found.", 'debug')
        Map diyEntry2 = [:]
        diyEntry2.put(diyAddNum,diyEntry)
        state.diyEffects.put(devSKU,diyEntry2)
    } else {
            logger("diyUpdateManual(): Device ${devSKU} was found. Updating scene", 'debug')
            state.diyEffects."${devSKU}".put(diyAddNum,diyEntry)
    }
    writeDIYFile()
}

/**
 *  logger()
 *
 *  Wrapper function for all logging.
 **/
private logger(msg, level = 'debug') {
    switch (level) {
        case 'error':
            if (state.loggingLevelIDE >= 1) { log.error msg };
            break;
        case 'warn':
            if (state.loggingLevelIDE >= 2)  { log.warn msg };
            break;
        case 'info':
            if (state.loggingLevelIDE >= 3) { log.info msg };
            break;
        case 'debug':
            if (state.loggingLevelIDE >= 4) { log.debug msg };
            break;
        case 'trace':
            if (state.loggingLevelIDE >= 5) { log.trace msg };
            break;
        default:
            log.debug msg;
            break;
    }
}


/**
 *  goveeDevAdd()
 *
 *  Wrapper function to create devices.
 **/
def goveeDevAdd() { //testing

// private goveeDevAdd(goveeAdd) {
//    def goveeAdd = settings.goveeDev - child.label    // testing
//    def devices = goveeAdd
    if (settings.goveeDev != null) {
    def devices = settings.goveeDev - child.label
    def drivers = getDriverList()
    mqttDevice = getChildDevice('Govee_v2_Device_Manager')
    logger("goveeDevAdd() drivers detected are ${drivers}", 'debug')
    logger("goveeDevAdd() Childred DNI  ${childDNI} MQTT device DNI ${mqttChildredDNI}", 'debug')
    logger("goveeDevAdd() $devices are selcted to be integrated", 'info')
    logger('goveeDevAdd() DEVICE INFORMATION', 'info')
    state.goveeAppAPI.each {
        def String dniCompare = "Govee_"+it.device
        def String deviceName = it.deviceName        
        if (childDNI.contains(dniCompare) == false) {
            logger("goveeDevAdd(): ${deviceName} is a new DNI. Passing to driver setup if selected.", 'debug')
            if (devices.contains(deviceName) == true) {
                def String deviceID = it.device
                def String deviceModel = it.sku
                def String devType = it.type
                def commands = []
                def capType = []
                def int ctMin = 0
                def int ctMax = 0
                it.capabilities.each {
                    logger ("goveeDevAdd(): ${it} instance is ${it.instance}",'trace')
                    commands.add(it.instance)
                    capType.add(it.type)
                    if (it.instance == "colorTemperatureK") {
                        logger ("goveeDevAdd(): ${it} instance is ${it.instance} Parms is ${it.parameters} range is ${it.parameters.range} min is ${it.parameters.range.min}",'trace')
                        ctMin = it.parameters.range.min
                        ctMax = it.parameters.range.max
                        logger ("goveeDevAdd(): Min is ${ctMin} Max is ${ctMax}",'trace')
                    }
                }
                logger ("goveeDevAdd(): ${deviceID} ${deviceModel} ${deviceName} ${devType} ${commands}",'trace')  
//                setBackgroundStatusMessage("Processing device ${deviceName}")
                if (devType == "devices.types.light") {
                    if (commands.contains("colorRgb") && commands.contains("colorTemperatureK") && commands.contains("segmentedBrightness") && commands.contains("segmentedColorRgb") && commands.contains("dreamViewToggle")) {
                        String driver = "Govee v2 Color Lights Dreamview Sync"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                        setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    } else if (commands.contains("colorRgb") && commands.contains("colorTemperatureK") && commands.contains("segmentedBrightness") && commands.contains("segmentedColorRgb")) {
                        String driver = "Govee v2 Color Lights 3 Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    } else if (commands.contains("colorRgb") && commands.contains("colorTemperatureK")  && commands.contains("segmentedBrightness")) {
                        String driver = "Govee v2 Color Lights 2 Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    } else if (commands.contains("colorRgb") && commands.contains("colorTemperatureK")  && commands.contains("segmentedColorRgb") && commands.contains("dreamViewToggle")) {
                        String driver = "Govee v2 Color Lights 4 Dreamview Sync"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    } else if (commands.contains("colorRgb") && commands.contains("colorTemperatureK")  && commands.contains("segmentedColorRgb")) {
                        String driver = "Govee v2 Color Lights 4 Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    } else if (commands.contains("colorRgb") == true && commands.contains("colorTemperatureK")) {
                        String driver = "Govee v2 Color Lights Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        } 
                    } else if (commands.contains("colorTemperatureK")) {
                        String driver = "Govee v2 White Lights with CT Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }     
                    } else if (deviceModel == "H6091" || deviceModel == "H6092") {
                        String driver = "Govee v2 Galaxy Projector"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    } else if (deviceModel == "H6093") {
                        String driver = "Govee v2 H6093 Starlight Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    } else {
                        String driver = "Govee v2 White Light Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)              
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }                    
                    }    
                } else if (devType == "devices.types.air_purifier") {
                    if (deviceModel == "H7120") {
                        String driver = "Govee v2 H7120 Air Purifier"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    } else if (deviceModel == "H7122") {
                        String driver = "Govee v2 H7122 Air Purifier"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    } else if (deviceModel == "H7123") {
                        String driver = "Govee v2 H7123 Air Purifier"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    } else if (deviceModel == "H7126") {
                        String driver = "Govee v2 H7126 Air Purifier"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    }else if (deviceModel == "H712C") {
                        String driver = "Govee v2 H712C Air Purifier"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    }  else {                    
                        String driver = "Govee v2 Air Purifier Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    }    
                } else if (devType == "devices.types.heater") {                    
                    if (deviceModel == "H7131" || deviceModel == "H7134") {
                        String driver = "Govee v2 H7131 Space Heater"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    } else if (deviceModel == "H7133") {
                        String driver = "Govee v2 H7133 Space Heater Pro"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    }  else {                                                
                        String driver = "Govee v2 Heating Appliance Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    } 
                } else if (devType == "devices.types.humidifier") {
                        String driver = "Govee v2 Humidifier Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                } else if (devType == "devices.types.fan") {
                    if (deviceModel == "H7102") {
                        String driver = "Govee v2 H7102 Tower Fan"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    }  else if (deviceModel == "H7106") {
                        String driver = "Govee v2 H7106 Tower Fan"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    }  else { 
                        String driver = "Govee v2 Fan Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else { 
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    }
                } else if (devType == "devices.types.socket") {
                    String driver = "Govee v2 Sockets Driver"
                    if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                        setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                    }         
                } else if (devType == "devices.types.ice_maker") {
                    String driver = "Govee v2 Ice Maker"
                    if (drivers.contains(driver)) {
                        logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                        setBackgroundStatusMessage("Installing device ${deviceName}")
                        mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                        setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                    }     
                } else if (devType == "devices.types.kettle") {
                    String driver = "Govee v2 Kettle Driver"
                    if (drivers.contains(driver)) {
                        logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                        setBackgroundStatusMessage("Installing device ${deviceName}")
                        mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                        setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                    } 
                } else if (devType == "devices.types.thermometer") {
                    String driver = "Govee v2 Thermo/Hygrometer Driver"
                    if (drivers.contains(driver)) {
                        logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                        setBackgroundStatusMessage("Installing device ${deviceName}")
                        mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                        setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                    }
                } else if (devType == "devices.types.sensor") {
                    String driver = "Govee v2 Presence Sensor"
                    if (drivers.contains(driver)) {
                        logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                        setBackgroundStatusMessage("Installing device ${deviceName}")
                        mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                        setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                    }    
                } else if (devType == "devices.types.aroma_diffuser") {
                    String driver = "Govee v2 Aroma Diffuser Driver with Lights"
                    if (drivers.contains(driver)) {
                        logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                        setBackgroundStatusMessage("Installing device ${deviceName}")
                        mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                        setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                    }     
                } else if (!devType) {
                    String driver = "Govee v2 Group Light Driver"
                    if (drivers.contains(driver)) {
                        logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                        setBackgroundStatusMessage("Installing device ${deviceName}")
                        mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                        setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                    }     
                } else {
                    String driver = "Govee v2 Research Driver"
                    if (drivers.contains(driver)) {
                        logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                        mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        setBackgroundStatusMessage("Device ${deviceName} was selected for install and is of Unknown device type. Installing with research driver.")
                    } else {
                    logger('goveeDevAdd(): The device does not have a driver and you do not have the '+driver+' loaded. Please load it and forward the device details to the developer', 'info')    
                    }
                }
            } else {
                logger("goveeDevAdd(): Device is not selected to be added. ${deviceName} not being installed", 'debug')
            }
        } else {
            logger("goveeDevAdd(): Device ID matches child DNI. ${deviceName} already installed", 'debug')
            setBackgroundStatusMessage("Device ${deviceName} is already installed. Ignored")
        }                
    }
    } else { 
        setBackgroundStatusMessage("No devices selected. No action")
    }
    state?.installDev = goveeDev
    atomicState.backgroundActionInProgress = false
    logger('goveeDevAdd() Govee devices integrated', 'info')
}


/**
 *  goveeLightManAdd()
 *
 *  Wrapper function for all logging.
 **/
private goveeLightManAdd(String model, String ip, String name) {
    def newDNI = "Govee_" + ip
    mqttDevice = getChildDevice('Govee_v2_Device_Manager')
    logger("goveeLightManAdd() Adding ${name} Model: ${model} at ${ip} with ${newDNI}", 'info')
    logger('goveeLightManAdd() DEVICE INFORMATION', 'info')
    if (childDNI.contains(newDNI) == false) {
        String driver = "Govee Manual LAN API Device"
        logger("goveeLightManAdd(): ${deviceName} is a new DNI. Passing to driver setup if selected.", 'debug') 
        logger("goveeLightManAdd():  configuring ${deviceName}", 'info')
        mqttDevice.addManLightDeviceHelper(driver, ip, name, model)
      //  mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
        } else { 
        logger("goveeLightManAdd(): Manual add request ignored as device is already added.", 'info')
        }
    }



/**
 *  appButtonHandler()
 *
 *  Handler for when Buttons are pressed in App. These are used for the Scene extract page.
 **/

private def appButtonHandler(button) {
    if (button == "sceneDIYInitialize") {
        state?.diyEffects = [:]
        writeDIYFile()
    } else if (button == "goveeHomeLogin") {
        if (settings.goveeEmail && settings.goveePassword) {
            bodyParm = '{"email": "'+settings.goveeEmail+'", "password": "'+settings.goveePassword+'"}'
            logger("appButtonHandler(): bodyparm to be passed:  ${bodyParm}", 'trace')
            def params = [
                uri   : 'https://community-api.govee.com',
                path  : '/os/v1/login',
                headers: ['Content-Type': 'application/json'],
                body: bodyParm
            ]
            logger("appButtonHandler(): parms to be passed:  ${params}", 'trace')
            try {
                httpPost(params) { resp ->
                    logger("appButtonHandler(): response is ${resp.data}", 'trace')
                    status = resp.data.status
                    msg = resp.data.message
                    logger("appButtonHandler(): status is ${status}: Message ${msg}", 'info')
                    if (status == 200) {
                        state.goveeHomeToken = resp.data.data.token
                        state.goveeHomeExpiry = resp.data.data.expiredAt.value
                        logger("appButtonHandler(): response is ${resp.data}", 'trace')
                        logger("appButtonHandler(): token is ${state.goveeHomeToken} and expires at ${state.goveeHomeExpiry}", 'info')
                    } else {
                        logger("appButtonHandler(): Login failed check error above and correct", 'info')
                    }
                }
                } catch (groovyx.net.http.HttpResponseException e) {
                    logger("appButtonHandler(): Error: e.statusCode ${e.statusCode}", 'error')
                    logger("appButtonHandler(): ${e}", 'error')

                return 'unknown'
            }
        }        
    } else if (button == "goveeHomeTokenClear") {
        state?.goveeHomeToken = null
        state?.goveeHomeExpiry = 0
    } else if (button == "deviceListRefresh") {
        retrieveGoveeAPIData()
    } else if (button == "savDIYScenes") {
        saveFile()
    } else if (button == "resDIYScenes") {
        loadFile()
    } else if (button == "resGovScenes") {
        mqttDevice = getChildDevice('Govee_v2_Device_Manager')
        models = (mqttDevice.getChildDevices().data.deviceModel).unique()
        logger("appButtonHandler(): child device models are ${models}", 'info')
        models.forEach {
            logger("appButtonHandler(): Processing  ${it}", 'info')
            goveeScene.clear()
            goveeSceneRetrieve(it)
        }
    }
    
}

def apiRateLimits(type, value) {
    logger("apiRateLimits($type, $value)", 'info')
    if (type == 'DailyLimitRemaining') {
        state.dailyLimit = value
        if ( state.dailyLimit.toInteger() < apiV1threshold) {
            sendnotification('Govee Lights, Plugs, Switches APi Rate Limit', state.dailyAppLimit)
        }
    }
    else if (type == 'DailyLimitRemainingV2') {
        state.dailyAppLimit = value
        log.debug "${state.dailyAppLimit}"
        if ( state.dailyAppLimit.toInteger() < apiV2threshold) {
            log.debug 'validated api limit to low. Sending notificatoin'
            sendnotification('Govee Appliance API rate Limit', state.dailyAppLimit)
        }
    }
}


///////////////////////////////////////////
// Helper methods for certain tasks // 
///////////////////////////////////////////


private String escapeStringForPassword(String str) {
    //logger("$str", "info")
    if (str) {
//        str = str.replaceAll(" ", "\\\\ ") // Escape spaces.
//        str = str.replaceAll(",", "\\\\,") // Escape commas.
        str = str.replaceAll("=", "\u003D") // Escape equal signs.
//        str = str.replaceAll("\"", "\u0022") // Escape double quotes.
//    str = str.replaceAll("'", "_")  // Replace apostrophes with underscores.
    }
    else {
        str = 'null'
    }
    return str
}

def getDriverList() {
    logger('getDriverList(): Attempting to obtain Driver List', 'debug')
	def result = []
	if (location.hub.firmwareVersionString >= "2.3.6.126") {
		def params = [
			uri: getBaseUrl(),
			path: "/hub2/userDeviceTypes",
			headers: [
				Cookie: state.cookie
			],
		  ignoreSSLIssues: true
		  ]

		try {
			httpGet(params) { resp ->
			resp.data.each { 
                if (it.namespace == "Mavrrick") {
                result += it.name
                    }
                } 
			}
		} catch (e) {
			log.error "Error retrieving installed drivers: ${e}"
		}

	}
	return result
}

def getBaseUrl() {
	def scheme = sslEnabled ? "https" : "http"
	def port = sslEnabled ? "8443" : "8080"
	return "$scheme://127.0.0.1:$port"
}

/* private def addManLightDeviceHelper( String driver, String ip, String deviceName, String deviceModel) {
    
//    mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
    
            addChildDevice('Mavrrick', driver, "Govee_${ip}" , location.hubs[0].id, [
                'name': 'Govee Manual LAN API Device',
                'label': deviceName,
                'data': [
                    'IP': ip,
                    'deviceModel': deviceModel,
                    'ctMin': 2000,
                    'ctMax': 9000
                        ],
                'completedSetup': true,
                ])    
}  */


///////////////////////////////////////////////////////////////////////////
// Method to return the Govee API Data for specific device from Prent App //
///////////////////////////////////////////////////////////////////////////

def retrieveGoveeAPI(deviceid) {
    if (debugLog) "retrieveGoveeAPI(): ${deviceid}"
    def goveeAppAPI = state.goveeAppAPI.find{it.device==deviceid}
    return goveeAppAPI
}

def retrieveGoveeAPIData() {
        logger('appButtonHandler() DEVICE INFORMATION', 'debug')
        def params = [
            uri   : 'https://openapi.api.govee.com',
            path  : '/router/api/v1/user/devices',
            headers: ['Govee-API-Key': settings.APIKey, 'Content-Type': 'application/json'],
            ]

        try {
            httpGet(params) { resp ->
                //List each device assigned to current API key
                state.goveeAppAPI = resp.data.data
                state.goveeAppAPIdate = now()
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        logger("appButtonHandler() Error: e.statusCode ${e.statusCode}", 'error')
        logger("appButtonHandler() ${e}", 'error')

        return 'unknown'
    }
}


void saveFile() {
    log.debug ("saveFile: Backing up ${state.diyEffects} for DIY Scene data")
    String listJson = "["+JsonOutput.toJson(state.diyEffects)+"]" as String
    uploadHubFile("$goveeDIYScenesFileBackup",listJson.getBytes())
}

void loadFile() {
    byte[] dBytes = downloadHubFile("$goveeDIYScenesFileBackup")
    tmpEffects = (new JsonSlurper().parseText(new String(dBytes))) as List
    log.debug ("loadFile: Restored ${tmpEffects.get(0)} from ${goveeDIYScenesFile }")
    state.diyEffects = tmpEffects.get(0)
    log.debug ("loadFile: Restored ${state.diyEffects?.size() ?: 0} disabled records")
    writeDIYFile()
}

void writeDIYFile() {
    log.debug ("writeDIYFile: Writing DIY Scenes to flat file for Drivers")
    String listJson = "["+JsonOutput.toJson(state.diyEffects)+"]" as String
    uploadHubFile("$goveeDIYScenesFile",listJson.getBytes())
}

void writeGoveeSceneFile(model) { // create and store lan scene files from Govee API
    log.debug ("writeDIYFile: Writing DIY Scenes to flat file for Drivers")
    String listJson = "["+JsonOutput.toJson(goveeScene)+"]" as String
    uploadHubFile("GoveeLanScenes_$model"+".json",listJson.getBytes())
}

def setBackgroundStatusMessage(msg, level="info") {
	if (statusMessage == null)
		statusMessage = ""
	if (level == "warn") log.warn msg
	if (settings?.txtEnable != false && level == "info") log.info msg
	statusMessage += "${msg}<br>"
}

def getBackgroundStatusMessage() {
	return statusMessage
}

def showHideNextButton(show) {
	if(show) paragraph "<script>\$('button[name=\"_action_next\"]').show()</script>"
	else paragraph "<script>\$('button[name=\"_action_next\"]').hide()</script>"
}

def base64ToHex(base64Str) { // Proper conversion from Base64 to Hex for scene creation.
    // Decode Base64 string to byte array
    byte[] decodedBytes = base64Str.decodeBase64()

    // Convert byte array to hex string
    def hexString = decodedBytes.collect { String.format("%02x", it) }.join('')
    
    return hexString
}


def calculateChecksum8Xor(String hexString) {
    int checksum = 0
    for (int i = 0; i < hexString.length(); i += 2) {
        String byteStr = hexString.substring(i, Math.min(i + 2, hexString.length()))
        int byteValue = Integer.parseInt(byteStr, 16)
        checksum ^= byteValue
    }
    return String.format("%02X", checksum) // Format as two-digit hex
}

def hexToBase64(String hexString) {
    if (!hexString) {
        return null
    }

    try {
        byte[] bytes = new byte[hexString.length() / 2]
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hexString.substring(i, i + 2), 16)
        }

        return base64Encode(bytes)

    } catch (NumberFormatException e) {
        log.error "Invalid hex string: ${e.message}"
        return null
    } catch (IllegalArgumentException e) {
        log.error "Invalid hex string length: ${e.message}"
        return null
    }
}



private String base64Encode(byte[] data) {
    StringBuilder sb = new StringBuilder();
    int b;
    int dataLen = data.length;
    int i = 0;
    while (i < dataLen) {
        b = data[i++] & 0xff;
        sb.append(BASE64_CHARS.charAt(b >> 2));
        if (i == dataLen) {
            sb.append(BASE64_CHARS.charAt((b & 0x3) << 4));
            sb.append("==");
            break;
        }
        b = (b & 0x3) << 4 | (data[i] & 0xff) >> 4;
        sb.append(BASE64_CHARS.charAt(b));
        if (++i == dataLen) {
            sb.append(BASE64_CHARS.charAt((data[i - 1] & 0xf) << 2));
            sb.append("=");
            break;
        }
        b = (data[i - 1] & 0xf) << 2 | (data[i] & 0xff) >> 6;
        sb.append(BASE64_CHARS.charAt(b));
        sb.append(BASE64_CHARS.charAt(data[i++] & 0x3f));
    }
    return sb.toString();
}

def isWholeNumber(number) {
    if (number == null) {
        return false // Handle null case
    }
    return number == number.intValue() // Compare to int value
}
