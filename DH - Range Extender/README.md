# RE270 and RE370 WiFi Range Extender Special Instructions

The RE270 and RE370 range extenders with embedded smart plug do not use the same command structure as other devices.  Until such a time as the on/off command is discovered, the integration uses IFTTT to turn the device on and off and then refreshes the state via the Kasa Cloud or Node Applet.

After installing the device using the Service Manager, the following will have to be accomplished:

    a.	Obtain an free IFTTT account from https://ifttt.com/collections/marketing
    b.	In IFTTT, go to "My Applets" and select the "Services" tab.
    c.	Search for and find the "SmartThings" service.  Follow the instructions 
    	to configure that service.
    d.	Return to the "Services" tab, and do the same thing for Kasa (the 
    	TP-Link service).
    e.	Go to "My Applets", "Applets" tab.
    f.	Select "New Applet".  You will do this twice, once for on and once for off.
    g.	Select "+this" and search for the SmartThings service (and select).
    h.  For turning on, select the "switched on" box.  For off, select the 
    	"switched off" box.
    i.  Select the device, and select "Create Trigger".
    j.	Select "+that" and select the Kasa service on the next page.
    k.  Select "turn on" or "turn off"
    l.	Select the device and then "create Action"
    m.  Press Finish.
