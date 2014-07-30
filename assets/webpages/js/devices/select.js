
var isDiscovering   = null;
var isConnected     = null;
var currentPeer     = null;
var currentPower    = null
var currentVersion  = null;
var lastFingerprint = null;

function unknown_status(data)
{
	$("#statusPeerName").text("???");    	
}

function receive_status(data)
{
	try
	{	    	    	
		var newConnected    = data["connected"];
	    var newDiscovering  = data["discovering"];
	    var newPeer         = data["peer"];			    
	    var newPower        = data["power"];			    
	    var newVersion      = data["version"];
	    lastFingerprint    = data["fingerprint"];
	    
	    if (currentVersion!=null && currentVersion!=newVersion)
	    {
	    	log("version changed " + currentVersion + " -> " + newVersion + ", reloading");
	    	location.reload(true);
	    } 		    
	    currentVersion = newVersion;

	    if  (currentPower!=newPower)
	    {
	    	currentPower = newPower;
	    }
	    
	    if (newPeer!=currentPeer)
	    {
	    	currentPeer = newPeer;
	    	$("#statusPeerName"   ).text(null==currentPeer ? "" : currentPeer);
	    }			    
	    
	    if (newDiscovering!=isDiscovering)
	    {
	    	isDiscovering = newDiscovering;
		    // https://www.iconfinder.com/iconsets/30_Free_Black_ToolBar_Icons
		    // http://www.ajaxload.info/
	    	var src = (isDiscovering ? "/img/spinner.gif" : "/img/discover.png");
	    	$("#statusImage")[0].src = src;
		}
		
		if (newConnected!=isConnected)
		{		    	
			isConnected = newConnected;
	    	$("#status")[0].className = (isConnected ? "status_on" : "status_off");
	    }
	}
	catch (e)
	{
		alert("receive_status: " +  e);
	}		    	    	
}    

/*
function update_status()
{
	try
	{	    	    	
    	var url     = "/rest/status?version=" + lastVersion + "&fingerprint=" + lastFingerprint + "&nonce=" + make_nonce();
    	var request = $.getJSON(url, receive_status);	    	
    	request.success(function()      { setTimeout(update_status,  250); });
    	request.error(  function(a,b,c) { handle_json_error(a,b,c); unknown_status(); setTimeout(update_status, 4000); });
    }
    catch (e)
    {
    	alert("update_status: Exception: " + e);	
    }
}
*/

function clear_devices()
{
 	div = $("#divDevices");
 	div.empty();
}

function update_devices()
{
	var request = $.getJSON("/rest/devices/list", function(data)
	{
		try
		{
    	 	devices = data["devices"];
    		text = "";
    	 	    	 	
    	 	$.each(devices, function(i,device)
    	 	{ 
    	 		var paired  = device["paired"];
    	 		var visible = device["visible"];
    	 		var addr    = device["addr"];
    	 		var name    = device["name"] + " (" + (paired ? "P" : "") + (visible ? "V" : "") + ")";
    	 		
    	 		if (i%2)
    	 		{
	    	 		clazz = paired ? "device_even_paired" : "device_even";
	    	 	}
	    	 	else
	    	 	{
	    	 		clazz = paired ? "device_odd_paired" : "device_odd";
	    	 	}	    	 				    	 	
    	 		
				text += 
				    "  <div class=\"" + clazz + "\" id=\"device_" + addr + "\" onClick=\"select_device('" + addr + "')\">\n" +
					"    <div class=\"addr\">" + addr + "</div>\n" + 
					"    <div class=\"name\">" + name + "</div>\n" +  
					"  </div>\n";
    	 	});
    	 	    	 	
    	 	div = $("#divDevices");
    	 	div.empty();
    	 	div.append(text);
    	 }
    	 catch (e)
    	 {
    	 	alert("update_devices: exception: " + e);
    	 }
	});
	 
	request.error(handle_json_error); 
	request.success(function() { setTimeout(update_devices, 4000); });	    	
}

function toggle_discovery()
{
	$.getJSON("/rest/discover/toggle?nonce=" + make_nonce(), function (data) {}).error(handle_json_error);
}

function select_device(addr)
{
	//alert("select_device: " + addr);
	var request = $.getJSON("/rest/device/select?addr=" + addr + "&nonce=" + make_nonce(), function (data) 
	{
		var success = data["success"];
		var error   = data["error"];
		alert(success ? "Device selected" : "Selection failed: " + error);    			
	});    	
	request.error(handle_json_error);
}    

function cancel_dialog()
{
	window.close();
}
