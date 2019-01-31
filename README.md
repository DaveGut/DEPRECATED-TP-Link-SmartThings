# TP-Link / Kasa Smart Home Device SmartThings Integration
(Note:  The previous version is still available at the original locations on this site.)

There is only one version supporting three integrations:

a.  Kasa Account.  Integration via the user's Kasa Account through the Cloud.  This uses a Smart Application for installation, device communications, and device management.

b.  Node Applet.  Smart Application integration via a home wifi Node.JS bridge (pc, fire tablet, android device, Raspberry Pi).  The Smart Application is used (with user entry of bridge IP) to install and manage the devices.  Especially useful in the new SmartThings phone app since it allows entry of user preferences via that app.

c.  Manual Node Installation.  Traditional Hub installation.  Does not use a Smart Application.

# Installation Prequisites:

A SmartThings Hub, IDE Account and SmartThings Classic are required for all original installations.  After installation, you may (if desired) transition to the new SmartThings phone app.

    a.	A SmartThing Hub
    b.	Kasa Account.  (1) Kasa Account, (2) TP-Link devices in remote mode.
    c.	Node Applet.  (1) Node.js Bridge, (2) Static IP address for Bridge 
    	(recommended for all devices).
    d.	Manual Node Installation.  (1)  Node.js Bridge, (2) Static IP addresses 
    	for the bridge and all devices.

# Installing the code into the IDE.
    a.  Log onto the IDE and go to the "Locations" tab
    b.  Select your Hub.  A new page will appear with details.
    c.  Select the "My SmartApps" tab.
    d.  Select "+New SmartApp" at the upper right corner.
    e.  Select the "From Code" tab.
    f.  In a separate window, go to the GitHub site "Service Manager" folder 
	and open the appropriate service manager.
    g.  Copy the contents of this file.  Assure you copy ALL of the contents.
    h.  Returning to the IDE window, paste the code you copied.
    i.  Select Create. [note, the paste process is a common error.  If you 
	get a failure, try again. It is likely how you copied the data from 
	GitHub.]
    j.  Select "Publish" then "For Me".
    k.  NOTE:  YOU WILL REPEAT THE BELOW FOR EACH DEVICE TYPE.  DEVICE TYPES 
	versus DEVICE HANDLER files are at the end of these instruction.
    l.  Go to the "My Device Handlers" tab.
    m.  Select "+Create New Device Handler" from upper right of window.
    n.  Select the "From Code" tab.
    o.  In a separate window, go to the GitHub site "Device Handlers" folder 
	and open the appropriate service manager.
    p.  Copy the contents of this file.  Assure you copy ALL of the contents.
    q.  Returning to the IDE window, paste the code you copied into.
    r.  Select Create.
    s.  Select "Publish" then "For Me".

# Running the SmartApp and Installing the Devices

The below MUST be accomplished using the Classic phone application.  If you do not understand what that is, check within the community.

    1.  From the SmartThings Classic phone app, select 'SmartApps', then 
	'Add a SmartApp'.  Select 'My Apps' at bottom of next page.  Select 
	'TP-Link SmartThings Manager' from the 'My Apps' page.
    2.  'Select Installation Type'.  Tap for the selection of 'Kasa Account' 
	or 'Node Applet'.  Once you select, the program will land on one of 
	Kasa Accounts or Node Applet page.
    3.  'Kasa Account'.  Enter your Username and Password.  Once both are 
	entered (right or wrong), you will be directet to select in "Install 
	a Device to Continue.  That will take you to the 'Device Installation 
	Page.  Follow prompts to install the devices.
     4.  'Node Applet'. Assure the Node.js Applet is running.  Enter the 
	device IP (example:  192.168.1.199) for your bridge.  You will see 
	an error until the system has time to actually detect devices.  Then 
	follow prompts to add devices.

# Device Handlers for each product.
	"HS100" : "TP-Link Plug"
	"HS103" : "TP-Link Plug"
	"HS105" : "TP-Link Plug"
	"HS200" : "TP-Link Switch"
	"HS210" : "TP-Link Switch"
	"KP100" : "TP-Link Plug"
	"RE270" " "TP-Link Plug"
	"RE370" " "TP-Link Plug"
	//	Miltiple Outlet Plug
	"HS107" : "TP-Link Multi-Plug"
	"HS300" : "TP-Link Multi-Plug"
	"KP200" : "TP-Link Multi-Plug"
	"KP400" : "TP-Link Multi-Plug"
	//	Dimming Switch Devices
	"HS220" : "TP-Link Dimming Switch"
	//	Energy Monitor Plugs
	"HS110" : "TP-Link Energy Monitor Plug"
	//	Soft White Bulbs
	"KB100" : "TP-Link Soft White Bulb"
	"LB100" : "TP-Link Soft White Bulb"
	"LB110" : "TP-Link Soft White Bulb"
	"KL110" : "TP-Link Soft White Bulb"
	"LB200" : "TP-Link Soft White Bulb"
	//	Tunable White Bulbs
	"LB120" : "TP-Link Tunable White Bulb"
	"KL120" : "TP-Link Tunable White Bulb"
	//	Color Bulbs
	"KB130" : "TP-Link Color Bulb"
	"LB130" : "TP-Link Color Bulb"
	"KL130" : "TP-Link Color Bulb"
	"LB230" : "TP-Link Color Bulb"

# Updates
12-12-2018 - SmartApp 3.5.03.  Fixed SmartApp handling of HS107 and HS300.  After installed, if user unplugs (or is off-line) the device and subsequently tries to add a device, the SmartApp crashed.  Fix will not add the offending device to the currentDevices Array.  Impact:  User may not be able to remove the device using the SmartApp under certain circumstances.  Manual removal via classic Phone App or the IDE My Devices page will work.

01.12.18 - App Version 3.5.04.  Added KP200 (two outlet wall switch) and KP400 (2-outlet outdoor plug) to devices using multi-plug.  Need user confirmation of effectiveness.

12-12-2018 - Device Handlers 3.5.03.  Maintenance release to fix various minor problems; including issues in updating Refresh Rate from the Preferences section of the Smart App.

11-24-2018 - Added device handler and updated Service Manager for HS107 (tested) and HS300.  Issue: Hub installation.  Deferred coding for obtaining the HS107/300 device label until later.  Not a simple issue.

11-24-2018 - Updated node.js applet to eliminate error in device polling causing it to crash.

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

# Update.  Not necessary until you need to install a new device.
To support the new SmartThings phone app icon, the previous plug / switch device handler has been replaced by separate plug and switch device handlers.  If you add a plug or switch, you will need to download these new device handlers (while keeping your current handler.

Update is possible.  It has not been extensively tested due to the complexity.  If the update fails, try a clean re-installation.

1.  The new device handlers will work with the existing Smart App (Service Manager) except if you try to ADD DEVICES.

2.  The new Smart App requires the new Device Handlers if you are going to perform any functions in the smartapp (it relies on the new naming conventions.
