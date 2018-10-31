# TP-Link / Kasa Smart Home Device SmartThings Integration
There is only one version supporting three integrations:

a.  Kasa Account.  Integration via the user's Kasa Account through the Cloud.  This uses a Smart Application for installation, device communications, and device management.

b.  Node Applet.  Smart Application integration via a home wifi Node.JS bridge (pc, fire tablet, android device, Raspberry Pi).  The Smart Application is used (with user entry of bridge IP) to install and manage the devices.  Especially useful in the new SmartThings phone app since it allows entry of user preferences via that app.

c.  Manual Node Installation.  Traditional Hub installation.  Does not use a Smart Application.

# All three versions are compatible with the new SmartThings phone application.

# Installation Prequisites:

SmartThings Classic is required for all original installations.  You can then use the new SmartThings.

a.  Kasa Account.  (1) Kasa Account, (2) TP-Link devices in remote mode

b.  Node Applet.  (1) Node.js Bridge, (2) Static IP address for Bridge (recommended for all devices)

c.  Manual Node Installation.  (1)  Node.js Bridge, (2) Static IP addresses for the bridge and all devices.

# Installation Instructions

(For interaction with the IDE, look in the SmartThings Community Forum.)

1.  Copy device handlers and in

# Installation (Must use the SmartThings Classic App)
Installation instruction can be found in the Documentation Folders.  These are step-by-step and are for users new to SmartThings.  However, they have not been modified from the previous version yet.

a.  Install the relevant installation file(s):
    
    1.  Go to the MyDeviceHandler page on the IDE, select 'Create New Device Handler'  Select 'From Code'.
    
    2.  Copy the contents of the relevant file in the 'Device Handler' folder herein.  Paste into the IDE page.

    3.  Select "Create".  Select 'Publish' 'For Me'.
    
    4.  For 'Kasa Account' or 'Node Applet' installations, go to the My SmartAps page on the IDE.  Select 'New SmartApp'.  Select 'From Code'.
    
    5.  Copy the contents of 'TP-Link SmartThings Manager.groovy' in the 'Service Manager' folder herein.  Paste into the IDE page.
    
    6.  Seleect 'Create'.  Select 'Publish' 'For Me'

b.  Installation of 'Kasa Account' or 'Node Applet' integrations.

    1.  From the SmartThings Classic app on your phone, select 'SmartApps', then 'Add a SmartApp'.  Select 'My Apps' at bottom of next page.  Select 'TP-Link SmartThings Manager' from the 'My Apps' page.
    
    2.  'Select Installation Type'.  Tap for the selection of 'Kasa Account' or 'Node Applet'.  Once you select, the program will land on one of two pages
    
    3.  'Kasa Account'.  Enter your Username and Password.  Once both are entered (right or wrong), you will be directet to select in "Install a Device to Continue.  That will take you to the 'Device Installation Page.  Follow prompts to install the devices.
    
    4.  'Node Applet'. Assure the Node.js Applet is running.  Enter the device IP (example:  192.168.1.199) for your bridge.  You will see an error until the system has time to actually detect devices.  Then follow prompts to add devices.

c.  Installation of Manual Node Installation

  1.  Start the Node.js Applet.  Go to the IDE, "My Devices'.  Select 'New Device'.
  
  2.  Fill-out the form fields 'Name', 'Label', 'Device Network ID'.
  
  3.  From the pull-down 'Type', select the appropriate device type (handler).  Select 'Location' (your hub).
  
  4.  In 'My devices or on the phone (Classic App) for each device, select preferences.  Inter InstallType, Bridge IP, and Device IP and save.
  
  5.  You can now use either phone app to control the device.  Prefernces can only be updated on the IDE or the classic app.

# Version 3.5 Update - 10/31/2018

The Files have been updated to Version 3.5.

Note:  In the Node Applet and Kasa Account installation, DO NOT CHANGE preferences BridgeIP, DeviceIP, nor InstallType.
Device Handler Changes

1.  Single device handler for Hub or Cloud (now 'Node Applet' or 'Kasa Account').  Added preference installType to accommodate.

2.  Update to work with the new SmartThings Application.  Note that this application has some limits:

    a.  No preferences page for devices.
  
    b.  Functionality limited to Samsung selected (no energy monitor display, no Circadian Mode control).
 
Smart Application Update:

1.  Smart App now supports both 'Kasa Account' and 'Node Applet'.

2.  Added Remove Selected Devices and Update Selected Device Preference capabilities.

3.  Better Maintenance functions:  Manage Kasa Acct Settings, Change Bridge IP Address.

4.  Can actually track Device IPs, therefore (although recommended) static deviceIPs are not required.  Static Bridge IP remains required.

# Update.  Unless you need to, not necessary at this time.  Will cause problems later when managing or installing new devices.
Update is possible.  It has not been extensively tested due to the complexity.  If the update fails, try a clean re-installation.

1.  The new device handlers will work with the existing Smart App (Service Manager) except if you try to ADD DEVICES.

2.  The new Smart App requires the new Device Handlers if you are going to perform any functions in the smartapp (it relies on the new naming conventions.
