
var lastVersion     = null;
var lastReconfig    = null;
// (global) transaction ID identifies a reconfig request
var lastReconfigTid = null;    
var lastFingerprint = null;	
var lastOnetimeList = [];
var timeSkew        = 0;
var dayNames        = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat','Sun'];

// stores globally if desktop browser is used
var global_desktop = false;

function log(msg)
{
	try { if (console) console.log(msg); } catch (e) {}    		
}

function select_device()
{
	var url = "/devices.html?action=select&nonce=" + make_nonce();
	window.location = url;
}    

function format_seconds(secs)
{
	if (""==secs || secs<1) return "OFF";
	unit = "s";
	if (secs>3600)
	{
		hh = Math.floor(secs/3600);
		mm = Math.floor((secs%3600)/60);
		if (mm<10) mm="0"+mm;
		return hh+":"+mm+"m";
	}
	else if (secs>60)
	{
		mm = Math.floor(secs/60);
		ss = secs%60;
		if (ss<10) ss="0"+ss;
		return mm+":"+ss+"s";		
	}	
	ss=secs;
	if (ss<10) ss=" "+ss;
	return ss+"s";		
}

function format_hhmm(time)
{
	hhmm = parseInt(time);
	hh = Math.floor(hhmm/100);
	if (hh<10) hh="0"+hh; 
	mm = hhmm%100;
	if (mm<10) mm="0"+mm;
	return hh+":"+mm;
}

function to_hhmm(time)
{
	return format_hhmm(time);
}

function from_hhmm(hhmm)
{
	hhmm = hhmm.replace(/:/,  ""); // 08:00 -> 0800
	hhmm = hhmm.replace(/^0+/,""); // 0800  ->  800
	if (""==hhmm) hhmm="0";
	return parseInt(hhmm);
}

function mask_to_bitarray(mask, bits)
{
	mask = parseInt(mask);
	var a = [];
	for (i=0; i<bits; i++)
	{
		a[i] = mask & 1;
		mask >>= 1;
	}	
	return a;
}

function make_nonce()
{
	var now   = new Date();
	return now.getTime() + "." + now.getMilliseconds();
}

last_json = null;

function handle_json_error(jqXHR, error, msg)    
{
	last_json  = jqXHR;	
	var status = jqXHR.status;
	var json   = jqXHR.responseText
	
	if (0==status) return; // no connection
	var info = "status=" + status + ", error=" + error + ", msg=" + msg + ", json=" + jqXHR.responseText;
			
	log("handle_json_error: " + info);
	    
	var div = $("#jsonErrorLog");
	if (div)
	{
		div.append(info + "<br/>\n");
	}
}    

var apiWindow = null;

function show_api()
{
	if (null!=apiWindow) apiWindow.close(); 
	apiWindow = window.open("/api?" + make_nonce(), "API", "width=300,height=600,resizable=yes");
	apiWindow.focus();
}

//Extend jquery with flashing for elements
$.fn.flash = function(duration, iterations) {
    duration = duration || 1000; // Default to 1 second
    iterations = iterations || 1; // Default to 1 iteration
    var iterationDuration = duration / iterations;

    for (var i = 0; i < iterations; i++) {
        this.fadeOut(iterationDuration).fadeIn(iterationDuration);
    }
}

function do_reload()
{
	if (global_desktop)
	{
		var strandsFrame = parent.window.frames[1];
		log("Passing on reload request to strandsFrame=" + strandsFrame);
		strandsFrame.location.reload(true);
	}
	else
	{
		location.reload(true);
	}
}

function receive_status(data)
{
	try
	{	
		var connected      = data["connected"];
	    var discovering    = data["discovering"];
	    var peer           = data["peer"];
	    var power          = data["power"];
	    var scheduled      = data["scheduled"];
	    var newVersion     = data["version"];
	    var newReconfig    = data["reconfig"];
	    var remoteUnixTime = data["unixtime"];
	    lastFingerprint    = data["fingerprint"];
	    lastOnetimeList    = data["onetimeList"];		    	
	    
	    $("#statusVersion").text(newVersion);
	    
	    if (lastVersion!=null && lastVersion!=newVersion)
	    {
	    	do_reload();
	    } 		    
	    if (lastReconfig!=null && lastReconfig!=newReconfig)
	    {
	    	// do not update if config was issued by this instance
	    	if (lastReconfigTid==null || lastReconfigTid!=newReconfig)
	    	{
		    	do_reload();
	    	}
	    } 		    
	    lastVersion  = newVersion;
	    lastReconfig = newReconfig
	    	
	    var remoteMillis = remoteUnixTime*1000;
	    var localMillis  = (new Date()).getTime();
	    timeSkew = remoteMillis-localMillis;
	    //log("remoteMillis=" + remoteMillis +", localMillis=" + localMillis + ", timeSkew=" + timeSkew);		    
	    
    	$("#statusPower"   ).text(power + "/" + scheduled);
	    $("#statusPeerName").text(null==peer  ? "" : peer);
    	$("#status")[0].className = (connected ? "status_on" : "status_off");
	    
	    // https://www.iconfinder.com/iconsets/30_Free_Black_ToolBar_Icons
	    // http://www.ajaxload.info/
    	//var src = (newDiscovering ? "/img/spinner.gif" : "/img/discover.png");
    	//$("#statusImage")[0].src = src;
	}
	catch (e)
	{
		alert("gardenoid.js: receive_status: " +  e);
	}		    	    	
}

function unknown_status(data)
{
	$("#statusPeerName").text("???");    	
}


function update_status()
{
	try
	{	    	    	
    	var url     = "/rest/status?version=" + lastVersion + "&fingerprint=" + lastFingerprint + "&nonce=" + make_nonce();
    	var request = $.getJSON(url, receive_status);
    		    	
    	request.success(function()
    	{
	    	window.setTimeout(update_status, 500);		    		 
    	});
    		    	
    	request.error(function(a,b,c)
    	{
    		handle_json_error(a,b,c);
	    	window.setTimeout(update_status, 4000);
	    	unknown_status();
	    	$("#status")[0].className = "status_unknown";		    		    		
    	});
    	
    	request.complete(function() { /*log("update_status: complete");*/ });	    	
    }
    catch (e)
    {
    	alert("gardenoid.js: update_status: Exception: " + e);	
    }
}


function start_discovery()
{
	$.getJSON("/rest/discover/start?nonce=" + make_nonce(), function (data) {}).error(handle_json_error);
}

function toggle_discovery()
{
	$.getJSON("/rest/discover/toggle?nonce=" + make_nonce(), function (data) {}).error(handle_json_error);
}

function stop_discovery()
{
	$.getJSON("/rest/discover/stop?nonce=" + make_nonce(), function (data) {}).error(handle_json_error);
}

function disconnect_device()
{
	$.getJSON("rest/connection/stop?nonce=" + make_nonce(), function (data) {}).error(handle_json_error);
}
    
function update_time()
{
	var now = new Date();
	now.setTime(now.getTime()+timeSkew);
	var t = now.toTimeString();
	var d = now.toDateString();
	t = t.replace(/ GMT.*/,"");
	d = d.replace(/ 20..$/,""); // skip year
	$("#statusTime").text(d + " " + t);
}

