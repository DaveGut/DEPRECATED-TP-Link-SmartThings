# This integration is DEPRECATED.
SmartThings now has native integration of TP-Link/Kasa products.

https://www.smartthings.com/products/-/filter/brands/tp-link


# TP-Link / Kasa Smart Home Device SmartThings Integration
(Note:  The previous version is still available in the directory "Old Version")

# Current Versions:
1.  Service Manager: 4.0.04
2.  Device Handler: 4.0.01

# 2/21/2019 UPDATE
A significant update to version 4.0.  No added device functionality, so upgrade is purely optional.  Changes:

I.  Cleaned up device handlers to reduce code size and (very slightly) reduce execution cycles.

II.  Added a "upgrade installation" in the smart application to simplify the update process.  This new version is designed to upgrade from Versions 1, 2, 3, 3.5, and 3.6 and has been tested to do so.

III.  Created documentation for installation and upgrade in the Documentation Folder:

    1.	"0 - Upgrade Installation.pdf"
    2.	"1 - Installing DH and SmartApp into SmartThings.pdf"
    3.	"2 - Running the Service Manager for the First Time.pdf"
    4.	"3 - Adding Kasa Devices.pdf"
    5.	"4 - Setting Kasa Device Preferences.pdf"
    6.	"TP-Link SmartThing Implementation.pdf"
    7.  "RE270_370 IFTTT Instructions.txt"

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

# Installation.
    a.  Install the code to your Smart Things IDE per: "1 - Installing DH and SmartApp into SmartThings.pdf".
    b.  Run the Service Manager for the first time per: "2 - Running the Service Manager for the First Time.pdf".
    c.  Add your Kasa Devices per: "3 - Adding Kasa Devices.pdf".
    d.  OPTIONAL.  Set preferences for the devices per: "4 - Setting Kasa Device Preferences.pdf".
    
# Upgrade from previous versions.
See:  "0 - Upgrade Installation.pdf".

    a.  Replace the content of the device handlers and service manager in SmartThings  (NOTE: For the combined plug-switch device handler, use either the plug or the switch device handler.  They are the same except for an icon.)
    b.  Run the Service Manager and select Update Installation Data then "Save" in the right corner on the next page.

