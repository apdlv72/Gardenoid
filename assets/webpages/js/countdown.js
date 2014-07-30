
var strandInDialog = -1;

function hide_dialog()
{
	$("#idDialog").fadeOut("fast");
}

function show_dialog(no)
{
	$("#idDialog").fadeIn("fast");
	var name = $("#idTitle"+no).text();
	$("#idStrandName").text(name);
	strandInDialog = no;
}

function receive_response_set_time(data)
{
	var no     = data["no"];
	var left   = data["left"];
	var end    = data["end"];
	$("#idLeft" + no).text(left);
	var indicator = $("#idActive" + no);
	if (left>0)
	{
		indicator.addClass("on");
	}
	else
	{
		indicator.removeClass("on");
	}
}
   	
function set_time(node)
{
	var time   = node.id;
	var method = ("idStop"==time) ? "stop" : "add"; 
	time = ("idStop"==time) ? 0 : time.replace(/^idTime/,"");
	
	$("#" + node.id).fadeOut(50).fadeIn(50);
	
	var url = "/rest/onetime/" + method + "?no=" + strandInDialog + "&secs=" + time + "&nonce=" + make_nonce();
	//log("url: " + url);  
	
	var r = $.getJSON(url, receive_response_set_time);
	r.error(handle_json_error);
	
	//if ("stop"==method) 
	hide_dialog();	
}

function update_time()
{
	var now = new Date();
	var remoteMillis = now.getTime()+timeSkew;
	var remoteUnixTime = remoteMillis/1000;     	
	now.setTime(remoteMillis);
	var t = now.toTimeString();
	var d = now.toDateString();
	t = t.replace(/ GMT.*/,"");
	d = d.replace(/ 20..$/,""); // skip year
	$("#statusTime").text(d + " " + t);

	lastOnetimeList.forEach(function(elem)
	{
		var no   = elem["no"];
		var left = elem["left"];
		var end  = elem["end"];
		    	
		left = end-remoteUnixTime;	    		
		left = left<0 ? 0 : Math.ceil(left);    		
		var ss = format_seconds(left);
		    		
		$("#idLeft"+no).text(ss);
		$("#idLeft"+no).fadeIn();
			
		if (strandInDialog==no)
		{
			$("#idLeftDialog").text(ss);
		}
		
		if (left>0)
		{
			$("#idActive"+no).addClass("on");
		}
		else
		{
			$("#idActive"+no).removeClass("on");
		}
	});	
}

