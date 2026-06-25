/*
* UnifiProtectChild-Doorbell
*
* Description:
* This Hubitat driver provides a spot to put data from Unifi Protect Doorbell-related devices. It does not belong on it's own and requires
* the UnifiProtectAPI driver as a parent device.
*
* Instructions for using Tile method:
* 1) In "Preferences -> Tile Template" enter your template (example below) and click "Save Preferences"
*   Ex: "[b]Temperature:[/b] @temperature@°@location.getTemperatureScale()@[/br]"
* 2) In a Hubitat dashboard, add a new tile, and select the child/sensor, in the center select "Attribute", and on the right select the "Tile" attribute
* 3) Select the Add Tile button and the tile should appear
* NOTE1: Put a @ before and after variable names
* NOTE2: Should accept most HTML formatting commands with [] instead of <>
* 
* Features List:
* Ability to control general device settings
* Ability to trigger device's locate function
* Ability to check a website (mine) to notify user if there is a newer version of the driver available
* 
* Licensing:
* Copyright 2026 David Snell
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
* in compliance with the License. You may obtain a copy of the License at: http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
* for the specific language governing permissions and limitations under the License.
*
* Version Control:
* 0.2.0 - Replaced all attributes with spaces
* 0.1.25 - Added isStateChange to ProcessEvents
* 0.1.24 - Change to doorbell notification delay as it was milliseconds, not seconds
* 0.1.23 - Change to doorbell notification null value, change to notification methods
* 0.1.22 - Additional change to version method and added image setting when preferences are saved
* 0.1.21 - Corrected version and moved to new version method
* 0.1.20 - Add ability to control recording and streaming statuses
* 0.1.19 - Update to Event logging
* 0.1.18 - Rework of ProcessEvent
* 0.1.17 - Added sound sensor capability
* 0.1.16 - Added ability to send a notification to the Doorbell
* 0.1.15 - Added RefreshRate and imageURL to assist with Easy Dashboard, as well as ImageAsOf and updated event/state handling
* 0.1.14 - Update to the Tile method and attributes for WebHook processing
* 0.1.13 - Inclusion of an attribute to store the userId associated with a fingerprint
* 0.1.12 - Changing refresh to try Camera status because Doorbell no longer works.
* 0.1.11 - Correction of isStateChange
* 0.1.10 - Change to remove old driver-specific variables and events
* 0.1.9 - Correct to button handling and removed space from Button Presses attribute name
* 0.1.8 - Added ReleaseableButton capability and changed driver-specific attribute names to remove spaces
* 0.1.7 - Update to refresh command method
* 0.1.6 - More button-related functionality, updated ProcessEvent, added info logging for push command
* 0.1.5 - Added attribute for Last Motion
* 0.1.4 - Added attribute for smartDetectType
* 0.1.3 - Command to get a snapshot image from the camera
* 0.1.2 - Removed capabilities not applicable to doorbells and added preferences for settings
* 0.1.1 - Added pushable button capability and additional attributes
* 0.1.0 - Initial version
* 
* Thank you(s):
* Thank you to @Cobra for inspiration of how I perform driver version checking
* Thank you to @mircolino for working out a parent/child method and pointing out other areas for significant improvement as well as coming up with the
*   original (no longer used) Tile/HTML Template method
*/

import groovy.transform.Field



@Field static final String DRIVER = "UnifiProtectChild-Doorbell"
// Returns the driver name
def DriverName(){
    return "UnifiProtectChild-Doorbell"
}

@Field static final String VERSION = "0.2.0"
// Returns the driver version
public static String version(){
    return "0.2.0"
}

// Driver Metadata
metadata{
	definition( name: "UnifiProtectChild-Doorbell", namespace: "Snell", author: "David Snell", importUrl: "https://www.drdsnell.com/projects/hubitat/drivers/UnifiProtectChild-Doorbell.groovy" ) {
        capability "Sensor"
        capability "Actuator"
        capability "MotionSensor"
        capability "ImageCapture"
        capability "ImageUrl"
        capability "PushableButton"
        capability "ReleasableButton"
        capability "Refresh"
        capability "Notification"
        capability "SoundSensor"
        
        // Commands
        //command "DoSomething" // For testing and development purposes only, it should not be uncommented for normal use
        command "push"
        command "release"
        command "SetMessageWithDuration", [ [ name: "Message*", type: "STRING", required: true, description: "REQUIRED: The custom message to use." ], [ name: "Duration*", type: "ENUM", required: true, constraints: [ "5 minutes", "30 minutes", "1 hour", "6 hours", "12 hours", "Always" ], defaultValue: "Always", description: "REQUIRED: Set the duration of the custom message." ] ]
        command "ClearNotification"
        command "SetRecordingStatus", [ [ name: "Value*", type: "ENUM", constraints: [ "Always", "Never" ], defaultValue: "Always", description: "REQUIRED: Always have camera record or never record." ] ]
        command "SetStreamingStatus", [ [ name: "Stream*", type: "ENUM", , constraints: [ "High", "Medium", "Low" ], description: "REQUIRED: Which video stream to set." ], [ name: "Value*", type: "ENUM", constraints: [ "Enabled", "Disabled" ], defaultValue: "Enabled", description: "REQUIRED: Whether the selected stream will be enabled or disabled." ] ]

        //command "Locate" // Meant to help identify/locate the particular device by flashing the light
        
        // Attributes for the driver itself
		attribute "DriverName", "string" // Identifies the driver being used for update purposes
		attribute "DriverVersion", "string" // Handles version for driver
        attribute "DriverStatus", "string" // Handles version notices for driver
        
        // General Device Attributes
        attribute "Type", "string" // The type of device, per Ubiquiti data
        
        // Attributes - Device Related
        attribute "Status", "string" // Show success/failure of commands performed
        attribute "DeviceStatus", "string" // Show the current state of the device as reported by the controller

        // Device Attributes
        attribute "Dark", "enum", [ "true", "false" ] // Whether the light level is dark
        attribute "IndicatorEnabled", "enum", [ "true", "false" ] // Whether the camera's indicator is enabled
        attribute "RecordingNow", "enum", [ "true", "false" ] // Whether the camera is recording this moment
        attribute "MotionEventsToday", "number" // Number of motion events today
        attribute "ButtonPresses", "number" // ButtonPresses is used to show the number of button presses
        attribute "SnapshotURL", "string"
        attribute "SnapshotImage", "string"
        attribute "smartDetectType", "string"
        attribute "LastMotion", "string"
        attribute "FingerprintUserID", "string" // Stores the userID received when a fingerprint is read
        attribute "ImageAsOf", "string"
        attribute "Notification", "string"
        attribute "Stream_High", "enum", [ "Enabled", "Disabled" ]  // Whether the High stream is enabled or disabled
        attribute "Stream_Medium", "enum", [ "Enabled", "Disabled" ]  // Whether the Medium stream is enabled or disabled
        attribute "Stream_Low", "enum", [ "Enabled", "Disabled" ]  // Whether the Low stream is enabled or disabled
        attribute "RecordingMode", "enum", [ "Always", "Never", "Schedule" ]  // What recording mode is being used
        attribute "Alarms", "string"
        attribute "Thumbnail", "string"
        
        // WebHook AI related Attributes
        attribute "AIRecognitionType", "string" // Stores the Type of AI detection triggered
        attribute "AIRecognitionValue", "string" // Stores the ID associated with a persons face as recognized by AI
        
        // Attributes to help with Easy Dashboards
        attribute "refreshRate", "string" // 
        
        // Tile Template attribute
        attribute "Tile", "string"; // Ex: "[b]Temperature:[/b] @temperature@°@location.getTemperatureScale()@[/br]"
        
    }
	preferences{
        if( ShowAllPreferences ){
            input( type: "enum", name: "RefreshRate", title: "<b>Image Refresh Rate</b>", required: true, multiple: false, options: [ "Manual", "30 seconds", "1 minute", "5 minutes", "10 minutes", "15 minutes", "30 minutes", "1 hour", "3 hours" ], defaultValue: "Manual" )
            input( type: "number", name: "MicVolume", title: "<b>Microphone Volume</b>", description: "<font size='2'>0 to 100</font>", required: true, defaultValue: 100, range: "0..100" )
            input( type: "bool", name: "StatusLED", title: "<b>Status LED On/Off</b>", defaultValue: false)
            input( type: "bool", name: "ExternalIR", title: "<b>External IR Lights On/Off</b>", defaultValue: false)
            input( type: "bool", name: "SystemSounds", title: "<b>System Status Sounds On/Off</b>", defaultValue: false)
            input( type: "int", name: "MaxPresses", title: "<b>Max button presses before rolling over</b>", required: false, defaultValue: 4 )
            input( type: "string", name: "DeviceName", title: "<b>Device Name</b>", description: "<font size='2'>If set it will change the device's name on the controller.</font>", defaultValue: "${ device.label }")
            input( name: "TileTemplate", type: "string", title: "<b>Tile Template</b>", description: "<font size='2'>Ex: [b]Temperature:[/b] @temperature@&deg;@location.getTemperatureScale()@[/br]</font>", defaultValue: "");
            input( type: "enum", name: "LogType", title: "<b>Enable Logging?</b>", required: false, multiple: false, options: [ "None", "Info", "Debug", "Trace" ], defaultValue: "Info" )
            input( type: "bool", name: "ShowAllPreferences", title: "<b>Show All Preferences?</b>", defaultValue: true )
        } else {
            input( type: "bool", name: "ShowAllPreferences", title: "<b>Show All Preferences?</b>", defaultValue: true )
        }
	}
}


// DoSomething is for testing and development purposes. It should not be uncommented for normal usage.
def DoSomething(){

}

// updated
def updated( boolean NewDevice = false ){
    if( LogType == null ){
        LogType = "Info"
    }
    
    if( NewDevice != true ){
        SendSettings()
    }
    
    // Set basic button info
    if( MaxPresses == null ){
        MaxPresses = 4
    }
    ProcessState( "MaxPresses", MaxPresses )
    
    ProcessEvent( "numberOfButtons", 1 )
    ProcessEvent( "ButtonPresses", 0 )
    
    ProcessEvent( "DriverName", DRIVER, null )
    ProcessEvent( "DriverVersion", VERSION, null )
    ProcessEvent( "DriverStatus", null, null )
    
    // Schedule daily check for driver updates to notify user
    def Hour = ( new Date().format( "h" ) as int )
    def Minute = ( new Date().format( "m" ) as int )
    def Second = ( new Date().format( "s" ) as int )
    
    unschedule()
    // Check what the refresh rate is set for then run it
    switch( RefreshRate ){
        case "30 seconds": // Schedule the camera to take a new image every 30 seconds
            schedule( "0/30 * * ? * *", "take" )
            break
        case "1 minute": // Schedule the camera to take a new image every minute
            schedule( "${ Second } * * ? * *", "take" )
            break
        case "5 minutes": // Schedule the camera to take a new image every 5 minutes
            schedule( "${ Second } 0/5 * ? * *", "take" )
            break
        case "10 minutes": // Schedule the camera to take a new image every 10 minutes
            schedule( "${ Second } 0/10 * ? * *", "take" )
            break
        case "15 minutes": // Schedule the camera to take a new image every 15 minutes
            schedule( "${ Second } 0/15 * ? * *", "take" )
            break
        case "30 minutes": // Schedule the camera to take a new image every 30 minutes
            schedule( "${ Second } 0/30 * ? * *", "take" )
            break
        case "1 hour": // Schedule the camera to take a new image every hour
            schedule( "${ Second } ${ Minute } * ? * *", "take" )
            break
        case "3 hours": // Schedule the camera to take a new image every 3 hours
            schedule( "${ Second } ${ Minute } 0/3 ? * *", "take" )
            break
        default:
            RefreshRate = "Manual"
            break
    }
    Logging( "Camera refresh rate: ${ RefreshRate }", 4 )
    ProcessEvent( "refreshRate", RefreshRate, null )
    
    // Schedule checks that are only performed once a day
    schedule( "${ Second } ${ Minute } ${ Hour } ? * *", "CheckForUpdate" )
    
    // If the device id is known, set default values for image, imageUrl, and Thumbnail since they will never actually change
    if( state.ID != null ){
        ProcessEvent( "image", "http://${ location.hub.localIP }/local/${ state.ID }_Image.jpg", null )
        ProcessEvent( "imageUrl", "http://${ location.hub.localIP }/local/${ state.ID }_Image.jpg", null )
        ProcessEvent( "Thumbnail", "<img width=\"20%\" height=\"20%\" src=\"http://${ location.hub.localIP }/local/Camera_${ state.ID }_Image.jpg\">" )
    }
    
    Logging( "Updated", 2 )
}

// Support the ability to send notifications to the doorbell
def deviceNotification( text ){
    //parent.DoorbellNotification( device.getDeviceNetworkId(), state.ID, "{\"lcdMessage:\"[\"type\":\"CUSTOM_MESSAGE\",\"text\":\"${ text }\",\"resetAt\":\"null\"]}" )
    parent.DoorbellNotification( device.getDeviceNetworkId(), state.ID, "{\"lcdMessage\":{\"type\":\"CUSTOM_MESSAGE\",\"text\":\"${ text }\",\"resetAt\":null}}" )
}
                                                 
// Support the ability to send notifications with duration to the doorbell
def SetMessageWithDuration( String Message, String Duration ){
    def Delay = null
    switch( Duration ){
        case "5 minutes": // Schedule message to be reset in 5 minutes
            Delay = 300000
            break
        case "30 minutes": // Schedule message to be reset in 30 minutes
            Delay = 1800000
            break
        case "1 hour": // Schedule message to be reset in 1 hour
            Delay = 3600000
            break
        case "6 hours": // Schedule message to be reset in 1 hour
            Delay = 21600000
            break
        case "12 hours": // Schedule message to be reset in 1 hour
            Delay = 43200000
            break
        default:
            Delay = null
            break
    }
    if( Delay != null ){
        Delay = ( ( new Date().time ) + ( Delay as long ) )
    }
    parent.DoorbellNotification( device.getDeviceNetworkId(), state.ID, "{\"lcdMessage\":{\"type\":\"CUSTOM_MESSAGE\",\"text\":\"${ Message }\",\"resetAt\":${ Delay }}}" )
}

// Support the ability to clear the notification on the doorbell
def ClearNotification(){
    parent.DoorbellNotification( device.getDeviceNetworkId(), state.ID, "{\"lcdMessage\":{\"resetAt\":0}}" )
}

// Set Recording Status
def SetRecordingStatus( Value ){
    if( state.ID != null ){
        parent.SetRecordingStatus( device.getDeviceNetworkId(), state.ID, Value )
    } else {
        Logging( "No ID for ${ device.getDeviceNetworkId() }, cannot set recording status", 5 )
    }
}

// Set Streaming Status
def SetStreamingStatus( Stream, Value ){
    if( state.ID != null ){
        if( state.Channels != null ){
            def StreamNum
            if( Stream == "High" ){
                StreamNum = 0
            } else if( Stream == "Medium" ){
                StreamNum = 1
            } else {
                StreamNum = 2
            }
            if( ( ( Value == "Enabled" ) && ( state.Channels[ StreamNum ].isRtspEnabled == true ) ) || ( ( Value == "Disabled" ) && ( state.Channels[ StreamNum ].isRtspEnabled == false ) ) ){
                Logging( "${ Stream } stream already set for ${ Value }", 2 )
            } else {
        		parent.SetStreamingStatus( device.getDeviceNetworkId(), state.ID, Stream, Value, state.Channels )
            }
        } else {
            Logging( "Channel data blank, cannot set streaming status. Try refreshing device first.", 5 )
        }
    } else {
        Logging( "No ID. Cannot set streaming status", 5 )
    }
}

// Handle any button push activities
def push( Number Button = 1 ){
    ProcessEvent( "pushed", 1, null )
//    state.pushed = 1
    //ProcessEvent( "pushed", Button, null )
    def Presses
    if( state.ButtonPresses != null ){
        Presses = ( state.ButtonPresses + 1 ) as int
    } else {
        Presses = 1
    }
    def Max
    if( MaxPresses != null ){
        Max = ( MaxPresses as int )
    } else {
        Max = 4
    }
    if( Presses > Max ){
        Presses = 1
    }
    ProcessEvent( "ButtonPresses", Presses, null ) 
    Logging( "Button ${ Button } pressed & number of presses = ${ Presses }", 2 )
}

// Handle any button release activities
def release( Number Button = 1 ){
    ProcessEvent( "released", Button, null )
    Logging( "Button ${ Button } released", 2 )
}

// Attempts to trigger a snapshot image from the doorbell
def take(){
    if( state.ID != null ){
        Logging( "Attempting to take a snapshot image", 2 )
        parent.GetSnapshot( state.ID, device.getDeviceNetworkId() )
    } else {
        Logging( "No ID for ${ device.getDeviceNetworkId() }, cannot get snapshot", 5 )
    } 
}

// refresh information on the specific child
def refresh(){
    if( state.ID != null ){
        parent.GetCameraStatus( device.getDeviceNetworkId(), state.ID )
    } else {
        Logging( "No ID for ${ device.getDeviceNetworkId() }, cannot refresh", 5 )
    }
}

// Turn on the device's locate function to help locate/identify it
def Locate(){
    if( state.ID != null ){
        parent.LocateDoorbell( device.getDeviceNetworkId(), state.ID )
    } else {
        Logging( "No ID for ${ device.getDeviceNetworkId() }, cannot activate identify function", 5 )
    }
}

// Configure device settings based on Preferences
def SendSettings(){
    if( state.ID != null ){
        if( DeviceName != null && DeviceName != device.label ){
            parent.SendCameraSettings( device.getDeviceNetworkId(), state.ID, "{\"micVolume\":${ MicVolume },\"name\":\"${ DeviceName }\",\"ledSettings\":{\"isEnabled\":${ StatusLED }},\"ispSettings\":{\"isExternalIrEnabled\":${ ExternalIR }},\"speakerSettings\":{\"areSystemSoundsEnabled\":${ SystemSounds }}}" )
        } else {
            parent.SendCameraSettings( device.getDeviceNetworkId(), state.ID, "{\"micVolume\":${ MicVolume },\"name\":\"${ device.label }\",\"ledSettings\":{\"isEnabled\":${ StatusLED }},\"ispSettings\":{\"isExternalIrEnabled\":${ ExternalIR }},\"speakerSettings\":{\"areSystemSoundsEnabled\":${ SystemSounds }}}" )
        }
    } else {
        Logging( "No ID for ${ device.getDeviceNetworkId() }, cannot send settings", 5 )
    }
}

// installed is called when the device is installed, all it really does is run updated
def installed(){
	Logging( "Installed", 2 )
	updated( true )
}

// initialize is called when the device is initialized, all it really does is run updated
def initialize(){
	Logging( "Initialized", 2 )
	updated( true )
}

// Return a state value
def ReturnState( Variable ){
    return state."${ Variable }"
}

// Tile method to produce HTML formatted string for dashboard use
private void UpdateTile( String val ){
    if( TileTemplate != null ){
        def TempString = ""
        Parsing = TileTemplate
        Parsing = Parsing.replaceAll( "\\[", "<" )
        Parsing = Parsing.replaceAll( "\\]", ">" )
        Count = Parsing.count( "@" )
        if( Count >= 1 ){
            def x = 1
            while( x <= Count ){
                TempName = Parsing.split( "@" )[ x ]
                switch( TempName ){
                    case "location.latitude":
                        Value = location.latitude
                        break
                    case "location.longitude":
                        Value = location.longitude
                        break
                    case "location.getTemperatureScale()":
                        Value = location.getTemperatureScale()
                        break
                    default:
                        Value = ReturnState( "${ TempName }" )
                        break
                }
                TempString = TempString + Parsing.split( "@" )[ ( x - 1 ) ] + Value
                x = ( x + 2 )
            }
            if( Parsing.split( "@" ).last() != Parsing.split( "@" )[ Count - 1 ] ){
                TempString = TempString + Parsing.split( "@" ).last()
            }
        } else if( Count == 1 ){
            TempName = Parsing.split( "@" )[ 1 ]
            switch( TempName ){
                case "location.latitude":
                    Value = location.latitude
                    break
                case "location.longitude":
                    Value = location.longitude
                    break
                case "location.getTemperatureScale()":
                    Value = location.getTemperatureScale()
                    break
                default:
                    Value = ReturnState( "${ TempName }" )
                    break
            }
            TempString = TempString + Parsing.split( "@" )[ 0 ] + Value
        } else {
            TempString = TileTemplate    
        }
        Logging( "Tile = ${ TempString }", 4 )
        sendEvent( name: "Tile", value: TempString )
    }
}

// Send event for device
def ProcessEvent( Variable, Value, Unit = null, Description = null ){
    sendEvent( name: Variable, value: Value, unit: Unit, descriptionText: Description, isStateChange: true )
    if( Unit != null ){
        if( Description != null ){
            Logging( "Event: ${ Variable } = ${ Value } Unit = ${ Unit } Description = ${ Description }", 4 )
        } else {
            Logging( "Event: ${ Variable } = ${ Value } Unit = ${ Unit }", 4 )
        }
    } else {
        if( Description != null ){
            Logging( "Event: ${ Variable } = ${ Value } Description = ${ Description }", 4 )
        } else {
            Logging( "Event: ${ Variable } = ${ Value }", 4 )
        }
    }
//    ProcessState( Variable, Value )
}

// Set a state variable to a value
def ProcessState( Variable, Value ){
    Logging( "State: ${ Variable } = ${ Value }", 4 )
    state."${ Variable }" = Value
    UpdateTile( "${ Value }" )
}

// Handles whether logging is enabled and thus what to put there.
def Logging( LogMessage, LogLevel ){
	// Add all messages as info logging
    if( ( LogLevel == 2 ) && ( LogType != "None" ) ){
        log.info( "${ device.displayName } - ${ LogMessage }" )
    } else if( ( LogLevel == 3 ) && ( ( LogType == "Debug" ) || ( LogType == "Trace" ) ) ){
        log.debug( "${ device.displayName } - ${ LogMessage }" )
    } else if( ( LogLevel == 4 ) && ( LogType == "Trace" ) ){
        log.trace( "${ device.displayName } - ${ LogMessage }" )
    } else if( LogLevel == 5 ){
        log.error( "${ device.displayName } - ${ LogMessage }" )
    }
}

// Checks drdsnell.com for the latest version of the driver
// Original inspiration from @cobra's version checking
def CheckForUpdate(){
    ProcessEvent( "DriverName", DRIVER )
    ProcessEvent( "DriverVersion", VERSION )
	httpGet( uri: "https://www.drdsnell.com/projects/hubitat/drivers/versions.json", contentType: "application/json" ){ resp ->
        switch( resp.status ){
            case 200:
                if( resp.data."${ DRIVER }" ){
                    CurrentVersion = VERSION.split( /\./ )
                    if( resp.data."${ DRIVER }".version == "REPLACED" ){
                       ProcessEvent( "DriverStatus", "Driver replaced, please use ${ resp.data."${ DRIVER }".file }" )
                    } else if( resp.data."${ DRIVER }".version == "REMOVED" ){
                       ProcessEvent( "DriverStatus", "Driver removed and no longer supported." )
                    } else {
                        SiteVersion = resp.data."${ DRIVER }".version.split( /\./ )
                        if( CurrentVersion == SiteVersion ){
                            Logging( "Driver version up to date", 2 )
				            ProcessEvent( "DriverStatus", "Up to date" )
                        } else if( ( CurrentVersion[ 0 ] as int ) > ( SiteVersion [ 0 ] as int ) ){
                            Logging( "Major development ${ CurrentVersion[ 0 ] }.${ CurrentVersion[ 1 ] }.${ CurrentVersion[ 2 ] } version", 4 )
				            ProcessEvent( "DriverStatus", "Major development ${ CurrentVersion[ 0 ] }.${ CurrentVersion[ 1 ] }.${ CurrentVersion[ 2 ] } version" )
                        } else if( ( CurrentVersion[ 1 ] as int ) > ( SiteVersion [ 1 ] as int ) ){
                            Logging( "Minor development ${ CurrentVersion[ 0 ] }.${ CurrentVersion[ 1 ] }.${ CurrentVersion[ 2 ] } version", 4 )
				            ProcessEvent( "DriverStatus", "Minor development ${ CurrentVersion[ 0 ] }.${ CurrentVersion[ 1 ] }.${ CurrentVersion[ 2 ] } version" )
                        } else if( ( CurrentVersion[ 2 ] as int ) > ( SiteVersion [ 2 ] as int ) ){
                            Logging( "Patch development ${ CurrentVersion[ 0 ] }.${ CurrentVersion[ 1 ] }.${ CurrentVersion[ 2 ] } version", 4 )
				            ProcessEvent( "DriverStatus", "Patch development ${ CurrentVersion[ 0 ] }.${ CurrentVersion[ 1 ] }.${ CurrentVersion[ 2 ] } version" )
                        } else if( ( SiteVersion[ 0 ] as int ) > ( CurrentVersion[ 0 ] as int ) ){
                            Logging( "New major release ${ SiteVersion[ 0 ] }.${ SiteVersion[ 1 ] }.${ SiteVersion[ 2 ] } available", 2 )
				            ProcessEvent( "DriverStatus", "New major release ${ SiteVersion[ 0 ] }.${ SiteVersion[ 1 ] }.${ SiteVersion[ 2 ] } available" )
                        } else if( ( SiteVersion[ 1 ] as int ) > ( CurrentVersion[ 1 ] as int ) ){
                            Logging( "New minor release ${ SiteVersion[ 0 ] }.${ SiteVersion[ 1 ] }.${ SiteVersion[ 2 ] } available", 2 )
				            ProcessEvent( "DriverStatus", "New minor release ${ SiteVersion[ 0 ] }.${ SiteVersion[ 1 ] }.${ SiteVersion[ 2 ] } available" )
                        } else if( ( SiteVersion[ 2 ] as int ) > ( CurrentVersion[ 2 ] as int ) ){
                            Logging( "New patch ${ SiteVersion[ 0 ] }.${ SiteVersion[ 1 ] }.${ SiteVersion[ 2 ] } available", 2 )
				            ProcessEvent( "DriverStatus", "New patch ${ SiteVersion[ 0 ] }.${ SiteVersion[ 1 ] }.${ SiteVersion[ 2 ] } available" )
                        }
                    }
                } else {
                    Logging( "${ DRIVER } is not published on drdsnell.com", 2 )
                    ProcessEvent( "DriverStatus", "${ DRIVER } is not published on drdsnell.com" )
                }
                break
            default:
                Logging( "Unable to check drdsnell.com for ${ DRIVER } driver updates.", 2 )
                break
        }
    }
}
// ~~~~~ start include (13) Mavrrick.Unifi_Protect_Child ~~~~~
library ( // library marker Mavrrick.Unifi_Protect_Child, line 1
 author: "Mavrrick", // library marker Mavrrick.Unifi_Protect_Child, line 2
 category: "Unifi", // library marker Mavrrick.Unifi_Protect_Child, line 3
 description: "Unifi Protect Child Devices", // library marker Mavrrick.Unifi_Protect_Child, line 4
 name: "Unifi_Protect_Child", // library marker Mavrrick.Unifi_Protect_Child, line 5
 namespace: "Mavrrick", // library marker Mavrrick.Unifi_Protect_Child, line 6
 documentationLink: "http://www.example.com/" // library marker Mavrrick.Unifi_Protect_Child, line 7
) // library marker Mavrrick.Unifi_Protect_Child, line 8

//@Field static Number procTime = 0 // library marker Mavrrick.Unifi_Protect_Child, line 10
//@Field static Number callCount = 0 // library marker Mavrrick.Unifi_Protect_Child, line 11
//@Field static Number updates = 0 // library marker Mavrrick.Unifi_Protect_Child, line 12


def childPost( Map Data ){ // library marker Mavrrick.Unifi_Protect_Child, line 15
    long startTime = now() // library marker Mavrrick.Unifi_Protect_Child, line 16
    long updates = 0 // library marker Mavrrick.Unifi_Protect_Child, line 17
    log.info "childPost() ChildPost call started at ${startTime}" // library marker Mavrrick.Unifi_Protect_Child, line 18
        def modelKey = Data.actionPacket.modelKey // library marker Mavrrick.Unifi_Protect_Child, line 19
        def EventID // library marker Mavrrick.Unifi_Protect_Child, line 20
        def TempID // library marker Mavrrick.Unifi_Protect_Child, line 21
        if( Data.actionPacket.recordId != null ){ // library marker Mavrrick.Unifi_Protect_Child, line 22
            TempID = Data.actionPacket.recordId // library marker Mavrrick.Unifi_Protect_Child, line 23
            if( TempID != null ){ // library marker Mavrrick.Unifi_Protect_Child, line 24
                if( TempID.indexOf( "-" ) != -1 ){ // library marker Mavrrick.Unifi_Protect_Child, line 25
                    TempID = Data.actionPacket.recordId.split( "-" ) // library marker Mavrrick.Unifi_Protect_Child, line 26
                    EventID = TempID[ 0 ] // library marker Mavrrick.Unifi_Protect_Child, line 27
                    TempID = TempID[ 1 ] // library marker Mavrrick.Unifi_Protect_Child, line 28
                } else { // library marker Mavrrick.Unifi_Protect_Child, line 29
                    EventID = TempID // library marker Mavrrick.Unifi_Protect_Child, line 30
                } // library marker Mavrrick.Unifi_Protect_Child, line 31
            } // library marker Mavrrick.Unifi_Protect_Child, line 32
        } else if( Data.actionPacket.id != null ){ // library marker Mavrrick.Unifi_Protect_Child, line 33
            TempID = Data.actionPacket.id // library marker Mavrrick.Unifi_Protect_Child, line 34
            if( TempID.indexOf( "-" ) != -1 ){ // library marker Mavrrick.Unifi_Protect_Child, line 35
                TempID = Data.actionPacket.id.split( "-" ) // library marker Mavrrick.Unifi_Protect_Child, line 36
                EventID = TempID[ 0 ] // library marker Mavrrick.Unifi_Protect_Child, line 37
                TempID = TempID[ 1 ] // library marker Mavrrick.Unifi_Protect_Child, line 38
            } else { // library marker Mavrrick.Unifi_Protect_Child, line 39
                EventID = TempID // library marker Mavrrick.Unifi_Protect_Child, line 40
            } // library marker Mavrrick.Unifi_Protect_Child, line 41
        } else if( Data.actionPacket.authorizationModelId != null ){ // library marker Mavrrick.Unifi_Protect_Child, line 42
            TempID = Data.actionPacket.authorizationModelId // library marker Mavrrick.Unifi_Protect_Child, line 43
            EventID = TempID // library marker Mavrrick.Unifi_Protect_Child, line 44
        } else if( Data.dataPacket?.metadata?.sensorId?.text != null ){ // library marker Mavrrick.Unifi_Protect_Child, line 45
            TempID = Data.dataPacket.metadata.sensorId.text // library marker Mavrrick.Unifi_Protect_Child, line 46
        } // library marker Mavrrick.Unifi_Protect_Child, line 47
        def LastAction = state.LastWSSAction // library marker Mavrrick.Unifi_Protect_Child, line 48
        def LastActionID = state.LastWSSID     // library marker Mavrrick.Unifi_Protect_Child, line 49
        def Device = device.getLabel() ? device.getLabel() : device.getName() // library marker Mavrrick.Unifi_Protect_Child, line 50
        if( Data.dataPacket != null ){ // library marker Mavrrick.Unifi_Protect_Child, line 51
            Logging( "${ Device } dataPacket = ${ Data.dataPacket }", 4 ) // library marker Mavrrick.Unifi_Protect_Child, line 52

            Data.dataPacket.each(){ // library marker Mavrrick.Unifi_Protect_Child, line 54
            Logging( "${ Device } DataPayload ${ it.key } = ${ it.value }", 4 ) // library marker Mavrrick.Unifi_Protect_Child, line 55
            switch( it.key ){ // library marker Mavrrick.Unifi_Protect_Child, line 56
                case "lastMotion": // library marker Mavrrick.Unifi_Protect_Child, line 57
                    if( it.value != null ){ // library marker Mavrrick.Unifi_Protect_Child, line 58
                        ProcessEvent( "${ Device }", "LastMotion", ConvertEpochToDate( "${ it.value }" ) ) // library marker Mavrrick.Unifi_Protect_Child, line 59
                    } // library marker Mavrrick.Unifi_Protect_Child, line 60
                    break // library marker Mavrrick.Unifi_Protect_Child, line 61
                case "lastRing": // library marker Mavrrick.Unifi_Protect_Child, line 62
                    if( it.value != null ){ // library marker Mavrrick.Unifi_Protect_Child, line 63
                        //getChildDevice( "${ Device }" ).push( 1 ) // library marker Mavrrick.Unifi_Protect_Child, line 64
                    } // library marker Mavrrick.Unifi_Protect_Child, line 65
                    break // library marker Mavrrick.Unifi_Protect_Child, line 66
                        case "isDark": // library marker Mavrrick.Unifi_Protect_Child, line 67
                        	ProcessEvent( "${ Device }", "Dark", ConvertBoolean( it.value ), null ) // library marker Mavrrick.Unifi_Protect_Child, line 68
                            if( Data.actionPacket?.actionPayload?.action?.toString() == "add" ){ // library marker Mavrrick.Unifi_Protect_Child, line 69
							    ProcessState( "LastWSSAction", "Dark" ) // library marker Mavrrick.Unifi_Protect_Child, line 70
                            } // library marker Mavrrick.Unifi_Protect_Child, line 71
                            ProcessState( "${ Device }", "LastWSSAction", "Dark" ) // library marker Mavrrick.Unifi_Protect_Child, line 72
                            updates++ // library marker Mavrrick.Unifi_Protect_Child, line 73
							break // library marker Mavrrick.Unifi_Protect_Child, line 74
						case "isMotionDetected": // library marker Mavrrick.Unifi_Protect_Child, line 75
                        	Logging( "isMotionDetected on ${ Device } is ${ it.value }", 4 ) // library marker Mavrrick.Unifi_Protect_Child, line 76
                    		if( it.value ){ // library marker Mavrrick.Unifi_Protect_Child, line 77
                        		ProcessEvent( "${ Device }", "motion", "active" ) // library marker Mavrrick.Unifi_Protect_Child, line 78
                        		if( Device.split( " " )[ 0 ] == "Sensor" ){ // library marker Mavrrick.Unifi_Protect_Child, line 79
                            		def MotionDuration = state.MotionDuration // library marker Mavrrick.Unifi_Protect_Child, line 80
//                					def ChildID = getChildDevice( "${ Device }" ).ReturnState( "ID" ) // library marker Mavrrick.Unifi_Protect_Child, line 81
                    				if( state.MotionDuration == null ){ // library marker Mavrrick.Unifi_Protect_Child, line 82
                        				state.MotionDuration = 15 // library marker Mavrrick.Unifi_Protect_Child, line 83
                            		} // library marker Mavrrick.Unifi_Protect_Child, line 84
                        			if( ChildID != null ){ // library marker Mavrrick.Unifi_Protect_Child, line 85
                        				runIn( state.MotionDuration, "GetSensorStatus", [ data: [ Device: "${ Device }", ChildID: "${ state.ID	 }" ] ] ) // may need to be looked at further. // library marker Mavrrick.Unifi_Protect_Child, line 86
                        			} // library marker Mavrrick.Unifi_Protect_Child, line 87
                    			} // library marker Mavrrick.Unifi_Protect_Child, line 88
                   			} else { // library marker Mavrrick.Unifi_Protect_Child, line 89
                        		ProcessEvent( "motion", "inactive" ) // library marker Mavrrick.Unifi_Protect_Child, line 90
                    		} // library marker Mavrrick.Unifi_Protect_Child, line 91
                            if( Data.actionPacket?.action?.toString() == "add" ){ // library marker Mavrrick.Unifi_Protect_Child, line 92
							    ProcessState( "LastWSSAction", "motionDetected" ) // library marker Mavrrick.Unifi_Protect_Child, line 93
                            } // library marker Mavrrick.Unifi_Protect_Child, line 94
                            ProcessState( "LastWSSAction", "motionDetected" ) // library marker Mavrrick.Unifi_Protect_Child, line 95
                            updates++ // library marker Mavrrick.Unifi_Protect_Child, line 96
							break // library marker Mavrrick.Unifi_Protect_Child, line 97
						case "isPirMotionDetected": // library marker Mavrrick.Unifi_Protect_Child, line 98
                        	Logging( "isPirMotionDetected on ${ Device } is ${ it.value }", 4 ) // library marker Mavrrick.Unifi_Protect_Child, line 99
							if( it.value ){ // library marker Mavrrick.Unifi_Protect_Child, line 100
								ProcessEvent( "motion", "active" ) // library marker Mavrrick.Unifi_Protect_Child, line 101
							} else { // library marker Mavrrick.Unifi_Protect_Child, line 102
								ProcessEvent( "motion", "inactive" ) // library marker Mavrrick.Unifi_Protect_Child, line 103
							} // library marker Mavrrick.Unifi_Protect_Child, line 104
                            if( Data.actionPacket?.action?.toString() == "add" ){ // library marker Mavrrick.Unifi_Protect_Child, line 105
							    ProcessState( "LastWSSAction", "motionDetected" ) // library marker Mavrrick.Unifi_Protect_Child, line 106
                            } // library marker Mavrrick.Unifi_Protect_Child, line 107
                            ProcessEvent( "LastWSSAction", "motionDetected" ) // library marker Mavrrick.Unifi_Protect_Child, line 108
                            updates++ // library marker Mavrrick.Unifi_Protect_Child, line 109
							break // library marker Mavrrick.Unifi_Protect_Child, line 110
						case "isSmartDetected": // library marker Mavrrick.Unifi_Protect_Child, line 111
                        	Logging( "isSmartDetected on ${ Device } is ${ it }", 4 ) // library marker Mavrrick.Unifi_Protect_Child, line 112
							if( it.value ){ // library marker Mavrrick.Unifi_Protect_Child, line 113
								ProcessEvent( "motion", "active" ) // library marker Mavrrick.Unifi_Protect_Child, line 114
							} else { // library marker Mavrrick.Unifi_Protect_Child, line 115
                                ProcessEvent( "motion", "inactive" ) // library marker Mavrrick.Unifi_Protect_Child, line 116
                                ProcessEvent( "smartDetectType", "none" ) // library marker Mavrrick.Unifi_Protect_Child, line 117
							} // library marker Mavrrick.Unifi_Protect_Child, line 118
                            if( Data.actionPacket?.action?.toString() == "add" ){ // library marker Mavrrick.Unifi_Protect_Child, line 119
							    ProcessState( "LastWSSAction", "motionDetected" ) // library marker Mavrrick.Unifi_Protect_Child, line 120
                            } // library marker Mavrrick.Unifi_Protect_Child, line 121
                            ProcessState( "LastWSSAction", "motionDetected" ) // library marker Mavrrick.Unifi_Protect_Child, line 122
							updates++ // library marker Mavrrick.Unifi_Protect_Child, line 123
                            break // library marker Mavrrick.Unifi_Protect_Child, line 124
						case "smartDetectTypes": // library marker Mavrrick.Unifi_Protect_Child, line 125
                        	def ActionType // library marker Mavrrick.Unifi_Protect_Child, line 126
                        	if( Data.actionPacket.modelKey != null ){ // library marker Mavrrick.Unifi_Protect_Child, line 127
                                ActionType = "modelKey ${ Data.actionPacket.modelKey }" // library marker Mavrrick.Unifi_Protect_Child, line 128
                            } else if( Data.actionPacket.authorizationModelKey != null ){ // library marker Mavrrick.Unifi_Protect_Child, line 129
                                ActionType = "authModelKey ${ Data.actionPacket.authorizationModelKey }" // library marker Mavrrick.Unifi_Protect_Child, line 130
                            } // library marker Mavrrick.Unifi_Protect_Child, line 131
                        	def Size = it.value.size() // library marker Mavrrick.Unifi_Protect_Child, line 132
                        	def TempString = "" // library marker Mavrrick.Unifi_Protect_Child, line 133
                            if( Size > 1 ){ // library marker Mavrrick.Unifi_Protect_Child, line 134
                                def Count = 0 // library marker Mavrrick.Unifi_Protect_Child, line 135
                                it.value.each{ // library marker Mavrrick.Unifi_Protect_Child, line 136
                                    TempString = TempString + "${ it.toString() }" // library marker Mavrrick.Unifi_Protect_Child, line 137
                                    Count++ // library marker Mavrrick.Unifi_Protect_Child, line 138
                                    if( Count < Size ){ // library marker Mavrrick.Unifi_Protect_Child, line 139
                                        TempString = TempString + ", " // library marker Mavrrick.Unifi_Protect_Child, line 140
                                    } // library marker Mavrrick.Unifi_Protect_Child, line 141
                                } // library marker Mavrrick.Unifi_Protect_Child, line 142
                                ProcessState( "LastWSSAction", "smartDetectType" ) // library marker Mavrrick.Unifi_Protect_Child, line 143
                            } else if( ( Size == 1 ) && ( "${ it.value[ 0 ] }" != null ) && ( "${ it.value[ 0 ] }" != "null" ) ){ // library marker Mavrrick.Unifi_Protect_Child, line 144
                                TempString = it.value[ 0 ].toString() // library marker Mavrrick.Unifi_Protect_Child, line 145

                            } else { // library marker Mavrrick.Unifi_Protect_Child, line 147
                                TempString = "none" // library marker Mavrrick.Unifi_Protect_Child, line 148
                            } // library marker Mavrrick.Unifi_Protect_Child, line 149
                            Logging( "WSS smartDetectType processed = ${ TempString }", 4 ) // library marker Mavrrick.Unifi_Protect_Child, line 150
                            ProcessEvent( "smartDetectType", TempString ) // library marker Mavrrick.Unifi_Protect_Child, line 151
                            if( Data.actionPacket?.action?.toString() == "add" ){ // library marker Mavrrick.Unifi_Protect_Child, line 152
                                ProcessState( "LastWSSAction", "smartDetectType" ) // library marker Mavrrick.Unifi_Protect_Child, line 153
                            } // library marker Mavrrick.Unifi_Protect_Child, line 154
                            ProcessState( "LastWSSAction", "smartDetectType" ) // library marker Mavrrick.Unifi_Protect_Child, line 155
                            updates++ // library marker Mavrrick.Unifi_Protect_Child, line 156
							break // library marker Mavrrick.Unifi_Protect_Child, line 157
						case "isLightOn": // library marker Mavrrick.Unifi_Protect_Child, line 158
							if( it.value ){ // library marker Mavrrick.Unifi_Protect_Child, line 159
								ProcessEvent( "switch", "on" ) // library marker Mavrrick.Unifi_Protect_Child, line 160
							} else { // library marker Mavrrick.Unifi_Protect_Child, line 161
								ProcessEvent( "switch", "off" ) // library marker Mavrrick.Unifi_Protect_Child, line 162
							} // library marker Mavrrick.Unifi_Protect_Child, line 163
                            if( Data.actionPacket?.action?.toString() == "add" ){ // library marker Mavrrick.Unifi_Protect_Child, line 164
							    ProcessState( "LastWSSAction", "Light" ) // library marker Mavrrick.Unifi_Protect_Child, line 165
                            } // library marker Mavrrick.Unifi_Protect_Child, line 166
                            ProcessState( "LastWSSAction", "Light" ) // library marker Mavrrick.Unifi_Protect_Child, line 167
                            updates++ // library marker Mavrrick.Unifi_Protect_Child, line 168
							break // library marker Mavrrick.Unifi_Protect_Child, line 169
						case "isRecording": // library marker Mavrrick.Unifi_Protect_Child, line 170
							if( it.value ){ // library marker Mavrrick.Unifi_Protect_Child, line 171
								ProcessEvent( "RecordingNow", "true" ) // library marker Mavrrick.Unifi_Protect_Child, line 172
							} else { // library marker Mavrrick.Unifi_Protect_Child, line 173
								ProcessEvent( "RecordingNow", "false" ) // library marker Mavrrick.Unifi_Protect_Child, line 174
							} // library marker Mavrrick.Unifi_Protect_Child, line 175
                            if( Data.actionPacket?.action?.toString() == "add" ){ // library marker Mavrrick.Unifi_Protect_Child, line 176
							    ProcessState( "LastWSSAction", "RecordingNow" ) // library marker Mavrrick.Unifi_Protect_Child, line 177
                            } // library marker Mavrrick.Unifi_Protect_Child, line 178
                            ProcessState( "${ Device }", "LastWSSAction", "RecordingNow" ) // library marker Mavrrick.Unifi_Protect_Child, line 179
							updates++ // library marker Mavrrick.Unifi_Protect_Child, line 180
                            break // library marker Mavrrick.Unifi_Protect_Child, line 181
						case "eventStats": // library marker Mavrrick.Unifi_Protect_Child, line 182
							if( it.value.motion.today != null ){ // library marker Mavrrick.Unifi_Protect_Child, line 183
								ProcessEvent( "MotionEventsToday", it.value.motion.today ) // library marker Mavrrick.Unifi_Protect_Child, line 184
							} // library marker Mavrrick.Unifi_Protect_Child, line 185
                            updates++ // library marker Mavrrick.Unifi_Protect_Child, line 186
							break // library marker Mavrrick.Unifi_Protect_Child, line 187
                        case "type": // library marker Mavrrick.Unifi_Protect_Child, line 188
/*                            if( Data.dataPacket != null ){ // library marker Mavrrick.Unifi_Protect_Child, line 189
                                if( Data.dataPacket.device != null ){ // library marker Mavrrick.Unifi_Protect_Child, line 190
                                    getChildDevices().each{ // library marker Mavrrick.Unifi_Protect_Child, line 191
                                        if( Data.dataPacket.device == getChildDevice( it.deviceNetworkId ).ReturnState( "ID" ) ){ // library marker Mavrrick.Unifi_Protect_Child, line 192
                                            Logging( "Changing Device from ${ Device } to ${ it.deviceNetworkId }", 4 ) // library marker Mavrrick.Unifi_Protect_Child, line 193
                                            Device = it.deviceNetworkId // library marker Mavrrick.Unifi_Protect_Child, line 194
                                        } // library marker Mavrrick.Unifi_Protect_Child, line 195
                                    } // library marker Mavrrick.Unifi_Protect_Child, line 196
                                } // library marker Mavrrick.Unifi_Protect_Child, line 197
                            } */ // library marker Mavrrick.Unifi_Protect_Child, line 198
                            switch( it.value ){ // library marker Mavrrick.Unifi_Protect_Child, line 199
                                case "motion": // library marker Mavrrick.Unifi_Protect_Child, line 200
                                	Logging( "SmartDetect Type Motion on ${ Device }", 4 ) // library marker Mavrrick.Unifi_Protect_Child, line 201
                                    ProcessEvent( "motion", "active" ) // library marker Mavrrick.Unifi_Protect_Child, line 202
                            		if( Data.actionPacket?.action?.toString() == "add" ){ // library marker Mavrrick.Unifi_Protect_Child, line 203
							            ProcessState( "LastWSSAction", "motion" ) // library marker Mavrrick.Unifi_Protect_Child, line 204
                                    } // library marker Mavrrick.Unifi_Protect_Child, line 205
                                    ProcessState( "LastWSSAction", "motion" ) // library marker Mavrrick.Unifi_Protect_Child, line 206
                                    if( Device.split( " " )[ 0 ] == "Sensor" ){ // library marker Mavrrick.Unifi_Protect_Child, line 207
                                        def MotionDuration = state.MotionDuration // library marker Mavrrick.Unifi_Protect_Child, line 208
//                                        def ChildID = getChildDevice( "${ Device }" ).ReturnState( "ID" ) // library marker Mavrrick.Unifi_Protect_Child, line 209
                                        if( state.MotionDuration == null ){ // library marker Mavrrick.Unifi_Protect_Child, line 210
                                            state.MotionDuration = 15 // library marker Mavrrick.Unifi_Protect_Child, line 211
                                        } // library marker Mavrrick.Unifi_Protect_Child, line 212
                                        if( ChildID != null ){ // library marker Mavrrick.Unifi_Protect_Child, line 213
                                            runIn( state.MotionDuration, "GetSensorStatus", [ data: [ Device: "${ Device }", ChildID: "${ state.ID }" ] ] ) // neeed to chek what this is doing. May need to make adjustments // library marker Mavrrick.Unifi_Protect_Child, line 214
                                        } // library marker Mavrrick.Unifi_Protect_Child, line 215
                                    } // library marker Mavrrick.Unifi_Protect_Child, line 216
                                    updates++ // library marker Mavrrick.Unifi_Protect_Child, line 217
                                    break // library marker Mavrrick.Unifi_Protect_Child, line 218
                                case "sensorMotion": // library marker Mavrrick.Unifi_Protect_Child, line 219
                                	Logging( "sensorMotion on ${ Device }", 4 ) // library marker Mavrrick.Unifi_Protect_Child, line 220
                                    ProcessEvent( "motion", "active" ) // library marker Mavrrick.Unifi_Protect_Child, line 221
                            		if( Data.actionPacket?.action?.toString() == "add" ){ // library marker Mavrrick.Unifi_Protect_Child, line 222
							            ProcessState( "LastWSSAction", "sensorMotion" ) // library marker Mavrrick.Unifi_Protect_Child, line 223
                                    } // library marker Mavrrick.Unifi_Protect_Child, line 224
                                    ProcessState( "LastWSSAction", "sensorMotion" ) // library marker Mavrrick.Unifi_Protect_Child, line 225
                                	if( Device.split( " " )[ 0 ] == "Sensor" ){ // library marker Mavrrick.Unifi_Protect_Child, line 226
                                        def MotionDuration = state.MotionDuration // library marker Mavrrick.Unifi_Protect_Child, line 227
//                                        def ChildID = getChildDevice( "${ Device }" ).ReturnState( "ID" ) // library marker Mavrrick.Unifi_Protect_Child, line 228
                                        if( MotionDuration == null ){ // library marker Mavrrick.Unifi_Protect_Child, line 229
                                            MotionDuration = 15 // library marker Mavrrick.Unifi_Protect_Child, line 230
                                        } // library marker Mavrrick.Unifi_Protect_Child, line 231
                                        if( ChildID != null ){ // library marker Mavrrick.Unifi_Protect_Child, line 232
                                            runIn( MotionDuration, "GetSensorStatus", [ data: [ Device: "${ Device }", ChildID: "${ state.ID }" ] ] ) // library marker Mavrrick.Unifi_Protect_Child, line 233
                                        } // library marker Mavrrick.Unifi_Protect_Child, line 234
                                    } // library marker Mavrrick.Unifi_Protect_Child, line 235
                                    updates++ // library marker Mavrrick.Unifi_Protect_Child, line 236
                                    break // library marker Mavrrick.Unifi_Protect_Child, line 237
                                case "sensorOpened": // library marker Mavrrick.Unifi_Protect_Child, line 238
                                    ProcessEvent( "contact", "open" ) // library marker Mavrrick.Unifi_Protect_Child, line 239
                            		if( Data.actionPacket?.action?.toString() == "add" ){ // library marker Mavrrick.Unifi_Protect_Child, line 240
							            ProcessState( "LastWSSAction", "sensorOpened" ) // library marker Mavrrick.Unifi_Protect_Child, line 241
                                    } // library marker Mavrrick.Unifi_Protect_Child, line 242
                                    ProcessState( "LastWSSAction", "sensorOpened" ) // library marker Mavrrick.Unifi_Protect_Child, line 243
                                    updates++ // library marker Mavrrick.Unifi_Protect_Child, line 244
                                    break // library marker Mavrrick.Unifi_Protect_Child, line 245
                                case "sensorClosed": // library marker Mavrrick.Unifi_Protect_Child, line 246
                                    ProcessEvent( "contact", "closed" ) // library marker Mavrrick.Unifi_Protect_Child, line 247
                            		if( Data.actionPacket?.action?.toString() == "add" ){ // library marker Mavrrick.Unifi_Protect_Child, line 248
							            ProcessState( "LastWSSAction", "sensorClosed" ) // library marker Mavrrick.Unifi_Protect_Child, line 249
                                    } // library marker Mavrrick.Unifi_Protect_Child, line 250
                                    ProcessState( "LastWSSAction", "sensorClosed" ) // library marker Mavrrick.Unifi_Protect_Child, line 251
                                    updates++ // library marker Mavrrick.Unifi_Protect_Child, line 252
                                    break // library marker Mavrrick.Unifi_Protect_Child, line 253
                                case "ring": // library marker Mavrrick.Unifi_Protect_Child, line 254
                                    push( 1 ) // library marker Mavrrick.Unifi_Protect_Child, line 255
                            		if( Data.actionPacket?.action?.toString() == "add" ){ // library marker Mavrrick.Unifi_Protect_Child, line 256
							            ProcessState( "LastWSSAction", "ring" ) // library marker Mavrrick.Unifi_Protect_Child, line 257
                                    } // library marker Mavrrick.Unifi_Protect_Child, line 258
                                    ProcessState( "${ Device }", "LastWSSAction", "ring" ) // library marker Mavrrick.Unifi_Protect_Child, line 259
                                    updates++ // library marker Mavrrick.Unifi_Protect_Child, line 260
                                    break // library marker Mavrrick.Unifi_Protect_Child, line 261
                                case "smartDetectZone": // library marker Mavrrick.Unifi_Protect_Child, line 262
                                    ProcessEvent( "smartDetectZone", "active" ) // library marker Mavrrick.Unifi_Protect_Child, line 263
                            		if( Data.actionPacket?.action?.toString() == "add" ){ // library marker Mavrrick.Unifi_Protect_Child, line 264
							            ProcessState( "LastWSSAction", "smartDetectZone" ) // library marker Mavrrick.Unifi_Protect_Child, line 265
                                    } // library marker Mavrrick.Unifi_Protect_Child, line 266
                                    ProcessState( "LastWSSAction", "smartDetectZone" ) // library marker Mavrrick.Unifi_Protect_Child, line 267
                                    updates++ // library marker Mavrrick.Unifi_Protect_Child, line 268
                                    break // library marker Mavrrick.Unifi_Protect_Child, line 269
                                case "access": // library marker Mavrrick.Unifi_Protect_Child, line 270
                                    ProcessEvent( "access", "active" ) // library marker Mavrrick.Unifi_Protect_Child, line 271
                            		if( Data.actionPacket?.action?.toString() == "add" ){ // library marker Mavrrick.Unifi_Protect_Child, line 272
							            ProcessState( "LastWSSAction", "access" ) // library marker Mavrrick.Unifi_Protect_Child, line 273
                                    } // library marker Mavrrick.Unifi_Protect_Child, line 274
                                    ProcessState( "LastWSSAction", "access" ) // library marker Mavrrick.Unifi_Protect_Child, line 275
                                    updates++ // library marker Mavrrick.Unifi_Protect_Child, line 276
                                    break // library marker Mavrrick.Unifi_Protect_Child, line 277
                                case "deviceDisconnected": // library marker Mavrrick.Unifi_Protect_Child, line 278
//                                    PostStateToChild( "${ Device }", "DeviceDisconnected", new Date() ) // library marker Mavrrick.Unifi_Protect_Child, line 279
                                    break // library marker Mavrrick.Unifi_Protect_Child, line 280
                                case "fingerprintIdentified": // library marker Mavrrick.Unifi_Protect_Child, line 281
                                    if( Data.dataPacket?.metadata?.fingerprint?.ulpId != null ){ // library marker Mavrrick.Unifi_Protect_Child, line 282
                                        ProcessEvent( "FingerprintUserID", "${ Data.dataPacket.metadata.fingerprint.ulpId }" ) // library marker Mavrrick.Unifi_Protect_Child, line 283
                                    } else { // library marker Mavrrick.Unifi_Protect_Child, line 284
                                        ProcessEvent( "FingerprintUserID", null ) // library marker Mavrrick.Unifi_Protect_Child, line 285
                                        Logging( "No userID found: ${ Data.dataPacket }", 3 ) // library marker Mavrrick.Unifi_Protect_Child, line 286
                                    } // library marker Mavrrick.Unifi_Protect_Child, line 287
                            		if( Data.actionPacket?.action?.toString() == "add" ){ // library marker Mavrrick.Unifi_Protect_Child, line 288
                                        ProcessState( "LastWSSAction", "fingerprint" ) // library marker Mavrrick.Unifi_Protect_Child, line 289
                                    } // library marker Mavrrick.Unifi_Protect_Child, line 290
                                    updates++ // library marker Mavrrick.Unifi_Protect_Child, line 291
                                    break // library marker Mavrrick.Unifi_Protect_Child, line 292
                                case "sensorExtremeValues": // library marker Mavrrick.Unifi_Protect_Child, line 293
//                                	PostStateToChild( "${ Device }", "SensorExtremeValues", "${ it }" ) // library marker Mavrrick.Unifi_Protect_Child, line 294
                            		if( Data.actionPacket?.action?.toString() == "add" ){ // library marker Mavrrick.Unifi_Protect_Child, line 295
							            ProcessState( "LastWSSAction", "sensorExtremeValues" ) // library marker Mavrrick.Unifi_Protect_Child, line 296
                                    } // library marker Mavrrick.Unifi_Protect_Child, line 297
                                    ProcessState( "LastWSSAction", "sensorExtremeValues" ) // library marker Mavrrick.Unifi_Protect_Child, line 298
                                    updates++ // library marker Mavrrick.Unifi_Protect_Child, line 299
                                    break // library marker Mavrrick.Unifi_Protect_Child, line 300
                                case "poorConnection": // library marker Mavrrick.Unifi_Protect_Child, line 301
//                                	PostStateToChild( "${ Device }", "DeviceConnection", "Poor" ) // library marker Mavrrick.Unifi_Protect_Child, line 302
                            		if( Data.actionPacket?.action?.toString() == "add" ){ // library marker Mavrrick.Unifi_Protect_Child, line 303
							            ProcessState( "LastWSSAction", "poorConnection" ) // library marker Mavrrick.Unifi_Protect_Child, line 304
                                    } // library marker Mavrrick.Unifi_Protect_Child, line 305
                                    ProcessState( "LastWSSAction", "poorConnection" ) // library marker Mavrrick.Unifi_Protect_Child, line 306
                                    updates++ // library marker Mavrrick.Unifi_Protect_Child, line 307
                                    break // library marker Mavrrick.Unifi_Protect_Child, line 308
                                case "disconnect": // library marker Mavrrick.Unifi_Protect_Child, line 309
                                	Logging( "${ Device } reported a WebSocket disconnect", 4 ) // library marker Mavrrick.Unifi_Protect_Child, line 310
                            		if( Data.actionPacket?.action?.toString() == "add" ){ // library marker Mavrrick.Unifi_Protect_Child, line 311
							            ProcessState( "LastWSSAction", "disconnect" ) // library marker Mavrrick.Unifi_Protect_Child, line 312
                                    } // library marker Mavrrick.Unifi_Protect_Child, line 313
                                    ProcessState( "LastWSSAction", "disconnect" ) // library marker Mavrrick.Unifi_Protect_Child, line 314
                                    updates++ // library marker Mavrrick.Unifi_Protect_Child, line 315
                                	break // library marker Mavrrick.Unifi_Protect_Child, line 316
                                case "smartAudioDetect": // library marker Mavrrick.Unifi_Protect_Child, line 317
                                	ProcessEvent( "sound ", "detected" ) // library marker Mavrrick.Unifi_Protect_Child, line 318
                            		if( Data.actionPacket?.action?.toString() == "add" ){ // library marker Mavrrick.Unifi_Protect_Child, line 319
							            ProcessState( "LastWSSAction", "smartAudioDetect" ) // library marker Mavrrick.Unifi_Protect_Child, line 320
                                    } // library marker Mavrrick.Unifi_Protect_Child, line 321
                                    ProcessState( "LastWSSAction", "smartAudioDetect" ) // library marker Mavrrick.Unifi_Protect_Child, line 322
                                    updates++ // library marker Mavrrick.Unifi_Protect_Child, line 323
                                	break // library marker Mavrrick.Unifi_Protect_Child, line 324
                                // WebSocket data to do nothing for // library marker Mavrrick.Unifi_Protect_Child, line 325
                                case "lightMotion": // Ignoring as it was seen on NVR in response to floodlight motion being activated // library marker Mavrrick.Unifi_Protect_Child, line 326
                                	break // library marker Mavrrick.Unifi_Protect_Child, line 327
                                case "type": // library marker Mavrrick.Unifi_Protect_Child, line 328
                                	switch( it.value ){ // library marker Mavrrick.Unifi_Protect_Child, line 329
                                        case "sensorButtonPressed": // library marker Mavrrick.Unifi_Protect_Child, line 330
                                        	push( 1 ) // library marker Mavrrick.Unifi_Protect_Child, line 331
                                        	break // library marker Mavrrick.Unifi_Protect_Child, line 332
                                        case "sensorTamper": // library marker Mavrrick.Unifi_Protect_Child, line 333
                                        	ProcessEvent( "tamper ", "detected" ) // library marker Mavrrick.Unifi_Protect_Child, line 334
                                        	break // library marker Mavrrick.Unifi_Protect_Child, line 335
                                        // Things to do nothing with at present // library marker Mavrrick.Unifi_Protect_Child, line 336
                                        case "deviceAdopted": // library marker Mavrrick.Unifi_Protect_Child, line 337
                                        case "deviceUpdatable": // library marker Mavrrick.Unifi_Protect_Child, line 338
                                        	break // library marker Mavrrick.Unifi_Protect_Child, line 339
                                        default: // library marker Mavrrick.Unifi_Protect_Child, line 340
                                        	Logging( "Unhandled WSS Type type for ${ Device }: ${ it }", 3 ) // library marker Mavrrick.Unifi_Protect_Child, line 341
                                        	break // library marker Mavrrick.Unifi_Protect_Child, line 342
                                    } // library marker Mavrrick.Unifi_Protect_Child, line 343
                                    updates++ // library marker Mavrrick.Unifi_Protect_Child, line 344
                                	break // library marker Mavrrick.Unifi_Protect_Child, line 345
                                case "streamRecovery": // library marker Mavrrick.Unifi_Protect_Child, line 346
                                case "fwUpdate": // library marker Mavrrick.Unifi_Protect_Child, line 347
                                case "videoDeleted": // library marker Mavrrick.Unifi_Protect_Child, line 348
                                case "recordingDeleted": // library marker Mavrrick.Unifi_Protect_Child, line 349
                                case "applicationUpdatable": // library marker Mavrrick.Unifi_Protect_Child, line 350
                                case "streamRecovery": // library marker Mavrrick.Unifi_Protect_Child, line 351
                                case "resolutionLowered": // library marker Mavrrick.Unifi_Protect_Child, line 352
                                case "sensorDetectionSettingsChanged": // library marker Mavrrick.Unifi_Protect_Child, line 353
                                case "adminActivity": // library marker Mavrrick.Unifi_Protect_Child, line 354
                                	break // library marker Mavrrick.Unifi_Protect_Child, line 355
                                default: // library marker Mavrrick.Unifi_Protect_Child, line 356
                                    Logging( "Unhandled WSS Type for ${ Device }: ${ it }", 3 ) // library marker Mavrrick.Unifi_Protect_Child, line 357
							        break // library marker Mavrrick.Unifi_Protect_Child, line 358
                            } // library marker Mavrrick.Unifi_Protect_Child, line 359
                            break // library marker Mavrrick.Unifi_Protect_Child, line 360
                        case "metadata": // library marker Mavrrick.Unifi_Protect_Child, line 361
                            it.each(){ // library marker Mavrrick.Unifi_Protect_Child, line 362
                                Logging( "${ Device } Metadata = ${ it }", 4 ) // library marker Mavrrick.Unifi_Protect_Child, line 363
                                switch( it.key ){ // library marker Mavrrick.Unifi_Protect_Child, line 364
                                	case "detectedThumbnails": // library marker Mavrrick.Unifi_Protect_Child, line 365
//                                        PostStateToChild( "${ Device }", "metadata-detectedThumbnails", it.value ) // library marker Mavrrick.Unifi_Protect_Child, line 366
                                    	break // library marker Mavrrick.Unifi_Protect_Child, line 367
                                    case "weather": // library marker Mavrrick.Unifi_Protect_Child, line 368
//                                    	PostStateToChild( "${ Device }", "metadata-weather", it.value ) // library marker Mavrrick.Unifi_Protect_Child, line 369
                                    	break // library marker Mavrrick.Unifi_Protect_Child, line 370
                                    // Ones to ignore // library marker Mavrrick.Unifi_Protect_Child, line 371
                                    case "metadata": // Yes, it provides metadata in metadata and this drove me crazy for a bit... // library marker Mavrrick.Unifi_Protect_Child, line 372
                                    case "clientPlatform": // library marker Mavrrick.Unifi_Protect_Child, line 373
                                    case "ip": // library marker Mavrrick.Unifi_Protect_Child, line 374
                                    case "userName": // library marker Mavrrick.Unifi_Protect_Child, line 375
                                    case "deviceModelKey": // library marker Mavrrick.Unifi_Protect_Child, line 376
                                    case "mountType": // library marker Mavrrick.Unifi_Protect_Child, line 377
                                    case "sensorId": // library marker Mavrrick.Unifi_Protect_Child, line 378
                                    case "sensorName": // library marker Mavrrick.Unifi_Protect_Child, line 379
                                    case "type": // library marker Mavrrick.Unifi_Protect_Child, line 380
                                    	break // library marker Mavrrick.Unifi_Protect_Child, line 381
                                	default: // library marker Mavrrick.Unifi_Protect_Child, line 382
                                        Logging( "Unhandled WSS Metadata on ${ Device } ${ it.key } = ${ it.value }", 3 ) // library marker Mavrrick.Unifi_Protect_Child, line 383
                                        break // library marker Mavrrick.Unifi_Protect_Child, line 384
                                } // library marker Mavrrick.Unifi_Protect_Child, line 385
                            } // library marker Mavrrick.Unifi_Protect_Child, line 386
                            updates++ // library marker Mavrrick.Unifi_Protect_Child, line 387
//                            PostStateToChild( "${ Device }", "metadata", it.value ) // library marker Mavrrick.Unifi_Protect_Child, line 388
                            break // library marker Mavrrick.Unifi_Protect_Child, line 389
                        case "lcdMessage": // library marker Mavrrick.Unifi_Protect_Child, line 390
//                            PostStateToChild( "${ Device }", "lcdMessage", it.value ) // library marker Mavrrick.Unifi_Protect_Child, line 391
                            break // library marker Mavrrick.Unifi_Protect_Child, line 392
                        case "createdAt": // library marker Mavrrick.Unifi_Protect_Child, line 393
                        case "updatedAt": // library marker Mavrrick.Unifi_Protect_Child, line 394
                        	ProcessState( "${ Device }", it.key, it.value ) // library marker Mavrrick.Unifi_Protect_Child, line 395
                            updates++ // library marker Mavrrick.Unifi_Protect_Child, line 396
                            break // library marker Mavrrick.Unifi_Protect_Child, line 397
                        case "motionDetectedAt": // library marker Mavrrick.Unifi_Protect_Child, line 398
 //                       	PostStateToChild( "${ Device }", "motionDetectedAt", ConvertEpochToDate( "${ it.value }" ) ) // library marker Mavrrick.Unifi_Protect_Child, line 399
                            break // library marker Mavrrick.Unifi_Protect_Child, line 400
                        case "functionButtonPressedAt": // library marker Mavrrick.Unifi_Protect_Child, line 401
//                        	PostStateToChild( "${ Device }", "functionButtonPressedAt", ConvertEpochToDate( "${ it.value }" ) ) // library marker Mavrrick.Unifi_Protect_Child, line 402
                            break // library marker Mavrrick.Unifi_Protect_Child, line 403
                        case "batteryStatus": // library marker Mavrrick.Unifi_Protect_Child, line 404
                        	ProcessEvent( "battery", ( it.value.percentage / 100 ) ) // library marker Mavrrick.Unifi_Protect_Child, line 405
                            updates++ // library marker Mavrrick.Unifi_Protect_Child, line 406
                        	break // library marker Mavrrick.Unifi_Protect_Child, line 407
                        case "labels": // library marker Mavrrick.Unifi_Protect_Child, line 408
                        	def Stringy = it.value as String // library marker Mavrrick.Unifi_Protect_Child, line 409
                        	Logging( "WSS labels = ${ Stringy as String }", 4 ) // library marker Mavrrick.Unifi_Protect_Child, line 410
                        	def TempString = "" // library marker Mavrrick.Unifi_Protect_Child, line 411
                        	def Detects = Stringy.count( "smartDetectType" ) // library marker Mavrrick.Unifi_Protect_Child, line 412
                        	def Split1 // library marker Mavrrick.Unifi_Protect_Child, line 413
                        	def Count = 1 // library marker Mavrrick.Unifi_Protect_Child, line 414
                        	def Split2 // library marker Mavrrick.Unifi_Protect_Child, line 415
                        	if( Detects >= 1 ){ // library marker Mavrrick.Unifi_Protect_Child, line 416
                                for( int i = 1; i <= Detects; i++ ){ // library marker Mavrrick.Unifi_Protect_Child, line 417
                                	Split1 = Stringy.split( "smartDetectType:" )[ 1 ] // library marker Mavrrick.Unifi_Protect_Child, line 418
                                	TempString += Split1.split( "," )[ 0 ] // library marker Mavrrick.Unifi_Protect_Child, line 419
                                    Split1 = Stringy.split( "," )[ 1 ] // library marker Mavrrick.Unifi_Protect_Child, line 420
                                    if( i < Detects ){ // library marker Mavrrick.Unifi_Protect_Child, line 421
                                    	TempString += ", " // library marker Mavrrick.Unifi_Protect_Child, line 422
                                	} // library marker Mavrrick.Unifi_Protect_Child, line 423
                                } // library marker Mavrrick.Unifi_Protect_Child, line 424
                            } else { // library marker Mavrrick.Unifi_Protect_Child, line 425
                                TempString = "none" // library marker Mavrrick.Unifi_Protect_Child, line 426
                            } // library marker Mavrrick.Unifi_Protect_Child, line 427
                            Logging( "WSS smartDetectType Final = ${ TempString }", 4 ) // library marker Mavrrick.Unifi_Protect_Child, line 428
                            ProcessEvent( "smartDetectType", TempString ) // library marker Mavrrick.Unifi_Protect_Child, line 429
                        	updates++ // library marker Mavrrick.Unifi_Protect_Child, line 430
                            break // library marker Mavrrick.Unifi_Protect_Child, line 431
                        case "systemInfo": // library marker Mavrrick.Unifi_Protect_Child, line 432
                        	ProcessEvent( "temperature", ConvertTemperature( "C", it.value.cpu.temperature ), location.getTemperatureScale() ) // library marker Mavrrick.Unifi_Protect_Child, line 433
                        	ProcessEvent( "CPULoad", ( it.value.cpu.averageLoad / 100 ), "%" ) // library marker Mavrrick.Unifi_Protect_Child, line 434
                        	updates++ // library marker Mavrrick.Unifi_Protect_Child, line 435
                            break // library marker Mavrrick.Unifi_Protect_Child, line 436
                        case "ptz": // library marker Mavrrick.Unifi_Protect_Child, line 437
                        	ProcessState( "ptz", "${ it.value }" ) // library marker Mavrrick.Unifi_Protect_Child, line 438
                            updates++ // library marker Mavrrick.Unifi_Protect_Child, line 439
                        	break // library marker Mavrrick.Unifi_Protect_Child, line 440
                        case "isOpened": // library marker Mavrrick.Unifi_Protect_Child, line 441
                            if( isOpened ){ // library marker Mavrrick.Unifi_Protect_Child, line 442
                                ProcessEvent( "contact", "open" ) // library marker Mavrrick.Unifi_Protect_Child, line 443
                            } else { // library marker Mavrrick.Unifi_Protect_Child, line 444
                                ProcessEvent( "contact", "closed" ) // library marker Mavrrick.Unifi_Protect_Child, line 445
                            } // library marker Mavrrick.Unifi_Protect_Child, line 446
                            if( Data.actionPacket?.action?.toString() == "add" ){ // library marker Mavrrick.Unifi_Protect_Child, line 447
                                if( isOpened ){ // library marker Mavrrick.Unifi_Protect_Child, line 448
                                    ProcessState( "LastWSSAction", "sensorOpened" ) // library marker Mavrrick.Unifi_Protect_Child, line 449
                                } else { // library marker Mavrrick.Unifi_Protect_Child, line 450
                                    ProcessState( "LastWSSAction", "sensorClosed" ) // library marker Mavrrick.Unifi_Protect_Child, line 451
                                } // library marker Mavrrick.Unifi_Protect_Child, line 452
                            } // library marker Mavrrick.Unifi_Protect_Child, line 453
                            ProcessState( "LastWSSAction", "sensorOpened" ) // library marker Mavrrick.Unifi_Protect_Child, line 454
                            updates++ // library marker Mavrrick.Unifi_Protect_Child, line 455
                        	break // library marker Mavrrick.Unifi_Protect_Child, line 456
                        case "openStatusChangedAt": // library marker Mavrrick.Unifi_Protect_Child, line 457
//                        	PostStateToChild( "${ Device }", it, it.value ) // library marker Mavrrick.Unifi_Protect_Child, line 458
                        	break // library marker Mavrrick.Unifi_Protect_Child, line 459
						// Things to ignore // library marker Mavrrick.Unifi_Protect_Child, line 460
						case "uptime": // library marker Mavrrick.Unifi_Protect_Child, line 461
//                            updates++  // put in for testing // library marker Mavrrick.Unifi_Protect_Child, line 462
						case "upSince": // library marker Mavrrick.Unifi_Protect_Child, line 463
						case "lastSeen": // library marker Mavrrick.Unifi_Protect_Child, line 464
						case "recordingSchedules": // library marker Mavrrick.Unifi_Protect_Child, line 465
						case "wifiConnectionState": // library marker Mavrrick.Unifi_Protect_Child, line 466
                        case "partition": // library marker Mavrrick.Unifi_Protect_Child, line 467
                        case "score": // library marker Mavrrick.Unifi_Protect_Child, line 468
                        case "start": // library marker Mavrrick.Unifi_Protect_Child, line 469
                        case "user": // library marker Mavrrick.Unifi_Protect_Child, line 470
                        case "locked": // library marker Mavrrick.Unifi_Protect_Child, line 471
                        case "isFavorite": // library marker Mavrrick.Unifi_Protect_Child, line 472
                        case "modelKey": // library marker Mavrrick.Unifi_Protect_Child, line 473
                        case "id": // library marker Mavrrick.Unifi_Protect_Child, line 474
                        case "favoriteObjectIds": // library marker Mavrrick.Unifi_Protect_Child, line 475
                        case "device": // library marker Mavrrick.Unifi_Protect_Child, line 476
                        case "camera": // library marker Mavrrick.Unifi_Protect_Child, line 477
                        case "nvrMac": // library marker Mavrrick.Unifi_Protect_Child, line 478
                        case "uplinkDevice": // library marker Mavrrick.Unifi_Protect_Child, line 479
                        case "end": // library marker Mavrrick.Unifi_Protect_Child, line 480
                        case "thumbnailId": // library marker Mavrrick.Unifi_Protect_Child, line 481
                        case "thumbnailFullfovId": // library marker Mavrrick.Unifi_Protect_Child, line 482
                        case "deletionType": // library marker Mavrrick.Unifi_Protect_Child, line 483
                        case "deletedAt": // library marker Mavrrick.Unifi_Protect_Child, line 484
                        case "hqBytesPerDay": // library marker Mavrrick.Unifi_Protect_Child, line 485
                        case "autoRetentionMs": // library marker Mavrrick.Unifi_Protect_Child, line 486
                        case "phyRate": // library marker Mavrrick.Unifi_Protect_Child, line 487
                        case "stats": // library marker Mavrrick.Unifi_Protect_Child, line 488
                        case "wirelessConnectionState": // library marker Mavrrick.Unifi_Protect_Child, line 489
                        case "bridgeCandidates": // library marker Mavrrick.Unifi_Protect_Child, line 490
                        case "bridge": // library marker Mavrrick.Unifi_Protect_Child, line 491
                        case "bluetoothConnectionState": // library marker Mavrrick.Unifi_Protect_Child, line 492
                        case "wanPorts": // library marker Mavrrick.Unifi_Protect_Child, line 493
                        case "portStatus": // library marker Mavrrick.Unifi_Protect_Child, line 494
                        case "clients": // library marker Mavrrick.Unifi_Protect_Child, line 495
                        case "ispSettings": // library marker Mavrrick.Unifi_Protect_Child, line 496
                        case "hasRecordings": // library marker Mavrrick.Unifi_Protect_Child, line 497
                        case "lqBytesPerDay": // library marker Mavrrick.Unifi_Protect_Child, line 498
                        case "autoRetentionLqMs": // library marker Mavrrick.Unifi_Protect_Child, line 499
                        case "videoReconfigurationInProgress": // library marker Mavrrick.Unifi_Protect_Child, line 500
							break // library marker Mavrrick.Unifi_Protect_Child, line 501
						default: // library marker Mavrrick.Unifi_Protect_Child, line 502
							Logging( "Unhandled WSS Update for ${ Device }: ${ it.key } = ${ it.value }", 3 ) // library marker Mavrrick.Unifi_Protect_Child, line 503
							break // library marker Mavrrick.Unifi_Protect_Child, line 504
					} // library marker Mavrrick.Unifi_Protect_Child, line 505
//	                    ProcessState( "LastWSSID", EventID ) // library marker Mavrrick.Unifi_Protect_Child, line 506
                }	 // library marker Mavrrick.Unifi_Protect_Child, line 507
            if (updates > 0 ) { // library marker Mavrrick.Unifi_Protect_Child, line 508
                ProcessState( "LastWSSID", EventID ) // library marker Mavrrick.Unifi_Protect_Child, line 509
            } // library marker Mavrrick.Unifi_Protect_Child, line 510
            } else if( ( Data.actionPacket?.action?.toString() == "update" ) && ( ( Data.dataPacket == null ) || ( Data.dataPacket == "null" ) ) ){ // library marker Mavrrick.Unifi_Protect_Child, line 511
				if( LastActionID == EventID ){ // library marker Mavrrick.Unifi_Protect_Child, line 512
					switch( LastAction ){ // library marker Mavrrick.Unifi_Protect_Child, line 513
						case "Dark": // library marker Mavrrick.Unifi_Protect_Child, line 514
							ProcessEvent( "Dark", "true" ) // library marker Mavrrick.Unifi_Protect_Child, line 515
							break // library marker Mavrrick.Unifi_Protect_Child, line 516
						case "motionDetected": // library marker Mavrrick.Unifi_Protect_Child, line 517
                        	Logging( "LastAction motionDetected on ${ Device }", 4 ) // library marker Mavrrick.Unifi_Protect_Child, line 518
							ProcessEvent( "motion", "inactive" ) // library marker Mavrrick.Unifi_Protect_Child, line 519
							break // library marker Mavrrick.Unifi_Protect_Child, line 520
						case "smartDetectType": // library marker Mavrrick.Unifi_Protect_Child, line 521
                        	Logging( "smartDetectType = ${ it }", 4 ) // library marker Mavrrick.Unifi_Protect_Child, line 522
                            def PreviousType = getChildDevice( "${ Device }" ).ReturnState( "smartDetectType" ) // library marker Mavrrick.Unifi_Protect_Child, line 523
                            if( ( PreviousType != null ) && ( PreviousType != "null" ) ){ // library marker Mavrrick.Unifi_Protect_Child, line 524
							    ProcessEvent( "smartDetectType", null ) // library marker Mavrrick.Unifi_Protect_Child, line 525
                            } // library marker Mavrrick.Unifi_Protect_Child, line 526
							break // library marker Mavrrick.Unifi_Protect_Child, line 527
						case "Light": // library marker Mavrrick.Unifi_Protect_Child, line 528
							ProcessEvent( "${ Device }", "switch", "off" ) // library marker Mavrrick.Unifi_Protect_Child, line 529
							break // library marker Mavrrick.Unifi_Protect_Child, line 530
						case "Recording Now": // library marker Mavrrick.Unifi_Protect_Child, line 531
							ProcessEvent( "${ Device }", "RecordingNow", "false" ) // library marker Mavrrick.Unifi_Protect_Child, line 532
							break // library marker Mavrrick.Unifi_Protect_Child, line 533
                        case "motion": // library marker Mavrrick.Unifi_Protect_Child, line 534
                        	Logging( "LastAction motion on ${ Device }", 4 ) // library marker Mavrrick.Unifi_Protect_Child, line 535
                            ProcessEvent( "${ Device }", "motion", "inactive" ) // library marker Mavrrick.Unifi_Protect_Child, line 536
							break // library marker Mavrrick.Unifi_Protect_Child, line 537
                        case "sensorMotion": // library marker Mavrrick.Unifi_Protect_Child, line 538
                        	Logging( "LastAction sensorMotion on ${ Device }", 4 ) // library marker Mavrrick.Unifi_Protect_Child, line 539
                            ProcessEvent( "${ Device }", "motion", "inactive" ) // library marker Mavrrick.Unifi_Protect_Child, line 540
							break // library marker Mavrrick.Unifi_Protect_Child, line 541
                        case "smartDetectZone": // library marker Mavrrick.Unifi_Protect_Child, line 542
                            ProcessEvent( "${ Device }", "smartDetectZone", "inactive" ) // library marker Mavrrick.Unifi_Protect_Child, line 543
                            break // library marker Mavrrick.Unifi_Protect_Child, line 544
                        case "access": // library marker Mavrrick.Unifi_Protect_Child, line 545
                            ProcessEvent( "${ Device }", "access", "inactive" ) // library marker Mavrrick.Unifi_Protect_Child, line 546
                            break // library marker Mavrrick.Unifi_Protect_Child, line 547
                        case "ring": // library marker Mavrrick.Unifi_Protect_Child, line 548
                            release( 1 ) // library marker Mavrrick.Unifi_Protect_Child, line 549
							break // library marker Mavrrick.Unifi_Protect_Child, line 550
                        case "disconnect": // library marker Mavrrick.Unifi_Protect_Child, line 551
                        	Logging( "${ Device } reconnected for WebSocket", 4 ) // library marker Mavrrick.Unifi_Protect_Child, line 552
                        	break // library marker Mavrrick.Unifi_Protect_Child, line 553
                        case "sensorExtremeValues": // library marker Mavrrick.Unifi_Protect_Child, line 554
//                        	PostStateToChild( "${ Device }", "SensorExtremeValues", null ) // library marker Mavrrick.Unifi_Protect_Child, line 555
                        	break // library marker Mavrrick.Unifi_Protect_Child, line 556
                        case "smartAudioDetect": // library marker Mavrrick.Unifi_Protect_Child, line 557
                            ProcessEvent	( "${ Device }", "sound ", "not detected" ) // library marker Mavrrick.Unifi_Protect_Child, line 558
                            break // library marker Mavrrick.Unifi_Protect_Child, line 559
                        case "poorConnection": // library marker Mavrrick.Unifi_Protect_Child, line 560
//                        	PostStateToChild( "${ Device }", "DeviceConnection", "OK" ) // library marker Mavrrick.Unifi_Protect_Child, line 561
                        	break // library marker Mavrrick.Unifi_Protect_Child, line 562
                        default: // library marker Mavrrick.Unifi_Protect_Child, line 563
                            Logging( "Unhandled LastAction ${ LastAction } for ${ Device }", 3 ) // library marker Mavrrick.Unifi_Protect_Child, line 564
                        	break // library marker Mavrrick.Unifi_Protect_Child, line 565
//					} // library marker Mavrrick.Unifi_Protect_Child, line 566
				} // library marker Mavrrick.Unifi_Protect_Child, line 567
	//		} // library marker Mavrrick.Unifi_Protect_Child, line 568
        }     // library marker Mavrrick.Unifi_Protect_Child, line 569
    } // library marker Mavrrick.Unifi_Protect_Child, line 570
/*    long duration = now() - startTime // library marker Mavrrick.Unifi_Protect_Child, line 571
    procTime += duration // library marker Mavrrick.Unifi_Protect_Child, line 572
    callCount++     // library marker Mavrrick.Unifi_Protect_Child, line 573

    def formattedDuration = formatDuration(duration) // library marker Mavrrick.Unifi_Protect_Child, line 575
    log.info "childPost() ChildPost call Elapse time ${formattedDuration}." */ // library marker Mavrrick.Unifi_Protect_Child, line 576

} // library marker Mavrrick.Unifi_Protect_Child, line 578

def formatDuration(long milliseconds) { // library marker Mavrrick.Unifi_Protect_Child, line 580
    if (milliseconds < 1000) { // library marker Mavrrick.Unifi_Protect_Child, line 581
        return "${milliseconds} ms" // library marker Mavrrick.Unifi_Protect_Child, line 582
    } // library marker Mavrrick.Unifi_Protect_Child, line 583

    long seconds = milliseconds / 1000 // library marker Mavrrick.Unifi_Protect_Child, line 585
    if (seconds < 60) { // library marker Mavrrick.Unifi_Protect_Child, line 586
        return "${seconds} s ${milliseconds % 1000} ms" // library marker Mavrrick.Unifi_Protect_Child, line 587
    } // library marker Mavrrick.Unifi_Protect_Child, line 588

    long minutes = seconds / 60 // library marker Mavrrick.Unifi_Protect_Child, line 590
    if (minutes < 60) { // library marker Mavrrick.Unifi_Protect_Child, line 591
        return "${minutes} min ${seconds % 60} s ${milliseconds % 1000} ms" // library marker Mavrrick.Unifi_Protect_Child, line 592
    } // library marker Mavrrick.Unifi_Protect_Child, line 593

    long hours = minutes / 60 // library marker Mavrrick.Unifi_Protect_Child, line 595
    return "${hours} h ${minutes % 60} min ${seconds % 60} s ${milliseconds % 1000} ms" // library marker Mavrrick.Unifi_Protect_Child, line 596
} // library marker Mavrrick.Unifi_Protect_Child, line 597

// Used to convert epoch values to text dates // library marker Mavrrick.Unifi_Protect_Child, line 599
def String ConvertEpochToDate( String Epoch ){ // library marker Mavrrick.Unifi_Protect_Child, line 600
    def date // library marker Mavrrick.Unifi_Protect_Child, line 601
    if( ( Epoch != null ) && ( Epoch != "" ) && ( Epoch != "null" ) ){ // library marker Mavrrick.Unifi_Protect_Child, line 602
        Long Temp = Epoch.toLong() // library marker Mavrrick.Unifi_Protect_Child, line 603
        if( Temp <= 9999999999 ){ // library marker Mavrrick.Unifi_Protect_Child, line 604
            date = new Date( ( Temp * 1000 ) ).toString() // library marker Mavrrick.Unifi_Protect_Child, line 605
        } else { // library marker Mavrrick.Unifi_Protect_Child, line 606
            date = new Date( Temp ).toString() // library marker Mavrrick.Unifi_Protect_Child, line 607
        } // library marker Mavrrick.Unifi_Protect_Child, line 608
    } else { // library marker Mavrrick.Unifi_Protect_Child, line 609
        date = "Null value provided" // library marker Mavrrick.Unifi_Protect_Child, line 610
    } // library marker Mavrrick.Unifi_Protect_Child, line 611
    return date // library marker Mavrrick.Unifi_Protect_Child, line 612
} // library marker Mavrrick.Unifi_Protect_Child, line 613

// ~~~~~ end include (13) Mavrrick.Unifi_Protect_Child ~~~~~
