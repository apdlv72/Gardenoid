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

function format_strand_mask(mask)
{
	mask = parseInt(mask);
	//strands = [ "ME1", "ME2", "HED", "BAM", "VEG", "FRO", "IMM", "ADD" ];
	res  = "";
	for (i=0; i<8; i++) 
	{	
		if (mask&1) res+="▇"; else res+="▁";
		mask=mask>>1;
	}		
	return res;
}

function format_day_mask(mask)
{
	mask = parseInt(mask);
	//days = [ "-", "-", "_", "Th", "Fr", "Sa", "Su" ];
	res  = "";
	for (i=0; i<7; i++) 
	{	
		if (mask&1) res+="▇"; else res+="▁";
		mask=mask>>1;
	}		
	return res;
}

function format_month_mask(mask)
{
	mask = parseInt(mask);
	//mons = [ "J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D" ];
	res  = "";
	for (i=0; i<12; i++) 
	{	
		if (mask&1) res+="▇"; else res+="▁";
		mask=mask>>1;
	}		
	return res;
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
			
	if (console)
	{
		console.log("handle_json_error: " + info);
	}
	    
	var div = $("#jsonErrorLog");
	if (div)
	{
		div.append(info + "<br/>\n");
	}
}    

function show_api()
{
	var win = window.open("/api?" + make_nonce(), "API", "width=300,height=600,resizable=yes");
    win.focus();
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