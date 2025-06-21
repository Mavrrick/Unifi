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
    name: 'Unifi Integration Manager',
    namespace: 'Mavrrick',
    author: 'CRAIG KING',
    description: "Unifi Integration Manager, helps manage devices and drivers based on @Snell's Unifi API project.",
    category: 'Networking',
    importUrl: "https://raw.githubusercontent.com/Mavrrick/Unifi/refs/heads/main/unifiIntegrationManager.groovy",
    iconUrl: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg',
    iconX2Url: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg',
//    iconX3Url: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg',
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
/* def mainPage() {
    atomicState.backgroundActionInProgress = null
    statusMessage = ""

    def int childCount = child.size()
    dynamicPage(name: 'mainPage', title: 'Main menu', uninstall: true, install: true, submitOnChange: true)
    {
        section('<b>Unifi Setup Menu</b>') {
                href 'setup', title: 'Unifi Environment Setup', description: 'Click to load values for Unifi Integrations.'
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
    }
} */

def mainPage() {
    atomicState.backgroundActionInProgress = null
    statusMessage = ""

    def int childCount = child.size()
    dynamicPage(name: 'mainPage', title: 'Main menu', uninstall: true, install: true, submitOnChange: true)
    {
        section('<b>Integration Configuration</b>') {
            paragraph('When selecting the option for the type of setup to use, you can select the following options: Not Enabled, Managed, External.')
            paragraph('<ul><li>Not Enabled - Will not be used at all.</li><li>Managed - Integration will manage all aspects of the setup. Best for new setups.</li><li>External - Integration will allow additional features but not manage setup. Best for already configured setups. (Maybe rename this to "Unmanaged"?)</li></ul>')
//                href 'setup', title: 'Unifi Environment Setup', description: 'Click to load values for Unifi Integrations.'
            input 'unifiNetwork', 'enum', title: 'Unifi Network Integration', required: true, submitOnChange: true, options:[ "Not Enabled", "Managed", "External" ], default: "Not Enabled"
            if (unifiNetwork == "Managed"){
                input 'unifiNetControllerType', 'enum', title: 'Please select the controller type Protect', required: true, submitOnChange: true, options:[ "Unifi Dream Machine (inc Pro)", "Other Unifi Controllers" ]
                if (unifiNetControllerType == "Other Unifi Controllers") {
                    input 'unifiNetControllerPort', 'string', title: 'Please enter the port of your Unifi Network controller', required: true, submitOnChange: false
                }
                input 'unifiNetControllerIP', 'string', title: 'Please enter the IP of your Unifi Network controller', required: true, submitOnChange: false
                input 'unifiNetUserID', 'string', title: 'Please enter your controller User ID', required: false, submitOnChange: false
                input 'unifiNetPassword', 'password', title: 'Please enter your controller password', required: false, submitOnChange: false            
            } else if (unifiNetwork == "External"){
                input name: "unifiNetDevice", type: "device.UnifiNetworkAPI", title: "Choose device"
            }
            input 'unifiProtect', 'enum', title: 'Unifi Protect Integration', required: true, submitOnChange: true, options:[ "Not Enabled", "Managed", "External" ]
            if (unifiProtect == "Managed"){
                input 'unifiProControllerType', 'enum', title: 'Please select the controller type for Portect', required: true, submitOnChange: true, options:[ "Unifi Dream Machine (inc Pro)", "Other Unifi Controllers" ]
                if (unifiProControllerType == "Other Unifi Controllers") {
                    input 'unifiProControllerPort', 'string', title: 'Please enter the port of your Unifi Protect controller', required: true, submitOnChange: false
                }
                input 'unifiProControllerIP', 'string', title: 'Please enter the IP of your Protect Controllercontroller', required: true, submitOnChange: false
                input 'unifiProUserID', 'string', title: 'Please enter your controller User ID', required: false, submitOnChange: false
                input 'unifiProPassword', 'password', title: 'Please enter your controller password', required: false, submitOnChange: false            
            } else if (unifiProtect == "External"){
                input name: "unifiProDevice", type: "device.UnifiProtectAPI", title: "Choose device"
            }
            input 'unifiConnect', 'enum', title: 'Unifi Connect Integration', required: true, submitOnChange: true, options:[ "Not Enabled", "Managed", "External" ]
            if (unifiConnect == "Managed"){
                input 'unifiConControllerType', 'enum', title: 'Please select the controller type Connect', required: true, submitOnChange: true, options:[ "Unifi Dream Machine (inc Pro)", "Other Unifi Controllers" ]
                if (unifiConControllerType == "Other Unifi Controllers") {
                    input 'unifiConControllerPort', 'string', title: 'Please enter the port of your Unifi Connect controller', required: true, submitOnChange: false
                }
                input 'unifiConControllerIP', 'string', title: 'Please enter the IP of your Connect controller', required: true, submitOnChange: false
                input 'unifiConUserID', 'string', title: 'Please enter your controller User ID', required: false, submitOnChange: false
                input 'unifiConPassword', 'password', title: 'Please enter your controller password', required: false, submitOnChange: false            
            } else if (unifiProtect == "External"){
                input name: "unifiConDevice", type: "device.UnifiConnectAPI", title: "Choose device"
            }

            }
        section('<b>Unifi Integration API Token</b>')
        {
        paragraph "API Token is only required for outbound webhook calls from Hubitat to Unifi Alarm Manager. You will not be able to enable those functions until this is entered. "
            input 'unifiApiToken', 'string', title: 'Please enter your Unifi Integration API Token here.', required: false, submitOnChange: true
        }
        
        section("<b>Outbound Webhook Calls</b>") {
            paragraph "Outbound Webhook Trigger Child Apps"
            if (unifiApiToken){
                app(name: "Outbound Webhook App", appName: "Unifi Outbound-Webhook", namespace: "Mavrrick", title: "Add Alarm Manager Webhook app", multiple: true)
            } else {
                paragraph "<b>No API Token Configured for integration. Please setup API token for Outbound Webhook Child apps to be avaliable</b>"
            }
        }
        
        section("") {
         if (state.accessToken == null) {
                paragraph("API is not yet Initialized!")
                href(name: "hrefPageEnableAPI", title: "Enable API", description: "", page: "pageEnableAPI")
            } else { 
		        section("Inbound Webhook:", hideable: true, hidden: true) {
                    paragraph """This url is to allow you to send information from Unifi Alarm manager to Hubitat based on known alarm manager events. You will use the below URLs with updated params for dni, type, and value to convey what the Alarm Manager event means. <br><br><ul><li>Replace %DEVICE_DNI% with the Hubitat Device DNI intended to recieve the event.</li> <li>Replace %DETECTION_TYPE% with the Detection type from Alarm Manager.</li> <li>Replace %Additional_PARM% with any additional relevant info for the Alarm Manager event like the person or license plate detected</li></ul>"""
                    paragraph """Valid Detection types are:<br><br><ul><li>Face</li><li>LicensePlate</li><li>NFCCardScan</li><li>FingerprintScan</li><li>Sound</li><li>PersonOfInterest</li><li>KnownFace</li><li>UnknownFace</li><li>VehicleOfInterest</li><li>KnownVehicle</li><li>UnknownVehicle</li><li>Person</li><li>Vehicle</li><li>Package</li><li>Animal</li><li>LineCrossing</li><li>Loitering</li><li>DoorbellRings</li><li>Motion</li></ul> Enter exactly as shown here with proper case"""
                } 
                
  		        section("Inbound Webook URLs") {
                    String localURL = "${state.localAPIEndpoint}/?access_token=${state.accessToken}&dni=%DEVICE_DNI%&type=%DETECTION_TYPE%&value=%Additional_PARM%"
//                    String remoteURL = "${state.remoteAPIEndpoint}/?access_token=${state.accessToken}&dni=%DEVICE_DNI%&type=%DETECTION_TYPE%&value=%Additional_PARM%"
                    paragraph("LOCAL API (devices): <a href=\"$localURL\" target=\"_blank\">$localURL</a>")
//                    paragraph("REMOTE API: <a href=\"$remoteURL\" target=\"_blank\">$remoteURL</a>")
                }             
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
    }
}

def setup() {

    logger('setup() Integration setup', 'debug')
    dynamicPage(name: 'setup', title: '<b>Unifi Integrations configuration</b>', uninstall: false, install: false, submitOnChange: true, nextPage: "mainPage")
    {
        section('Avaliable Integrations') 
        {
            
            input 'unifiNetwork', 'enum', title: 'Unifi Network Integration', required: true, submitOnChange: true, options:[ "Not Enabled", "Managed", "External" ], default: "Not Enabled"
            if (unifiNetwork == "Managed"){
                input 'unifiNetControllerType', 'enum', title: 'Please select the controller type Protect', required: true, submitOnChange: true, options:[ "Unifi Dream Machine (inc Pro)", "Other Unifi Controllers" ]
                if (unifiNetControllerType == "Other Unifi Controllers") {
                    input 'unifiNetControllerPort', 'string', title: 'Please enter the port of your Unifi Network controller', required: true, submitOnChange: false
                }
                input 'unifiNetControllerIP', 'string', title: 'Please enter the IP of your Unifi Network controller', required: true, submitOnChange: false
                input 'unifiNetUserID', 'string', title: 'Please enter your controller User ID', required: false, submitOnChange: false
                input 'unifiNetPassword', 'password', title: 'Please enter your controller password', required: false, submitOnChange: false            
            } else if (unifiNetwork == "External"){
                input name: "unifiNetDevice", type: "device.UnifiNetworkAPI", title: "Choose device"
            }
            input 'unifiProtect', 'enum', title: 'Unifi Protect Integration', required: true, submitOnChange: true, options:[ "Not Enabled", "Managed", "External" ]
            if (unifiProtect == "Managed"){
                input 'unifiProControllerType', 'enum', title: 'Please select the controller type for Portect', required: true, submitOnChange: true, options:[ "Unifi Dream Machine (inc Pro)", "Other Unifi Controllers" ]
                if (unifiProControllerType == "Other Unifi Controllers") {
                    input 'unifiProControllerPort', 'string', title: 'Please enter the port of your Unifi Protect controller', required: true, submitOnChange: false
                }
                input 'unifiProControllerIP', 'string', title: 'Please enter the IP of your Protect Controllercontroller', required: true, submitOnChange: false
                input 'unifiProUserID', 'string', title: 'Please enter your controller User ID', required: false, submitOnChange: false
                input 'unifiProPassword', 'password', title: 'Please enter your controller password', required: false, submitOnChange: false            
            } else if (unifiProtect == "External"){
                input name: "unifiProDevice", type: "device.UnifiProtectAPI", title: "Choose device"
            }
            input 'unifiConnect', 'enum', title: 'Unifi Connect Integration', required: true, submitOnChange: true, options:[ "Not Enabled", "Managed", "External" ]
            if (unifiConnect == "Managed"){
                input 'unifiConControllerType', 'enum', title: 'Please select the controller type Connect', required: true, submitOnChange: true, options:[ "Unifi Dream Machine (inc Pro)", "Other Unifi Controllers" ]
                if (unifiConControllerType == "Other Unifi Controllers") {
                    input 'unifiConControllerPort', 'string', title: 'Please enter the port of your Unifi Connect controller', required: true, submitOnChange: false
                }
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
    List childDNI = getChildDevices().deviceNetworkId
    if (childDNI.contains("UnifiNetworkAPI") == false) {
        if (unifiNetwork == "Managed"){
            unifiNetInstall()
        }
    } else {
        device = getChildDevice('UnifiNetworkAPI')
        if (unifiProControllerType == "Other Unifi Controllers") {
            device.updateSettings (unifiProControllerType, unifiProControllerPort, unifiProControllerIP, unifiProUserID, unifiProPassword)    
        } else {
            device.updateSettings (unifiProControllerType, unifiProControllerIP, unifiProUserID, unifiProPassword) 
        }
    }
    if (childDNI.contains("UnifiProtectAPI") == false) {
        if (unifiProtect == "Managed"){
            unifiProInstall()
        }
    } else {
        device = getChildDevice('UnifiProtectAPI')
        if (unifiProControllerType == "Other Unifi Controllers") {
            device.updateSettings (unifiProControllerType, unifiProControllerPort, unifiProControllerIP, unifiProUserID, unifiProPassword)    
        } else {
            device.updateSettings (unifiProControllerType, unifiProControllerIP, unifiProUserID, unifiProPassword) 
        }
    }
    if (childDNI.contains("UnifiConnectAPI") == false) {
        if (unifiConnect == "Managed"){
            unifiConInstall()
        }
    } else {
        if (unifiProControllerType == "Other Unifi Controllers") {
            device.updateSettings (unifiConControllerType, unifiConControllerPort, unifiConControllerIP, unifiConUserID, unifiConPassword)    
        } else {
            device.updateSettings (unifiConControllerType, unifiConControllerIP, unifiConUserID, unifiConPassword)
        }
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

/**
*
* Routine called by using inbound Webook with at 
*
*/

def webHook () {
    log.debug("Processing a webHook() $params")
    log.debug("Processing a webHook() $params.dni $params.type $params.value")
    String devicedni = params.dni.toString()
    String type = params.type.toString()
    Stringvalue = params.value.toString()
    if (unifiProtect == "Managed"){
        device = getChildDevice('UnifiProtectAPI')
        device.ApplyWebHook(devicedni, type, value)
    } else {
	    unifiProDevice.ApplyWebHook(devicedni, type, value)
    }
}

/**
 *  Device Instal Wrapper functions
 *
 *  
 **/

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
    if (unifiNetControllerType == "Other Unifi Controllers") {
        device.updateSettings (unifiNetControllerType, unifiNetControllerPort, unifiNetControllerIP, unifiNetUserID, unifiNetPassword)    
    } else {    
        device.updateSettings (unifiNetControllerType, unifiNetControllerIP, unifiNetUserID, unifiNetPassword)
    }
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
    if (unifiProControllerType == "Other Unifi Controllers") {
        device.updateSettings (unifiProControllerType, unifiProControllerPort, unifiProControllerIP, unifiProUserID, unifiProPassword)    
    } else {
        device.updateSettings (unifiProControllerType, unifiProControllerIP, unifiProUserID, unifiProPassword) 
    }
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
    if (unifiProControllerType == "Other Unifi Controllers") {
        device.updateSettings (unifiConControllerType, unifiConControllerPort, unifiConControllerIP, unifiConUserID, unifiConPassword)    
    } else {
        device.updateSettings (unifiConControllerType, unifiConControllerIP, unifiConUserID, unifiConPassword)
    }
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


