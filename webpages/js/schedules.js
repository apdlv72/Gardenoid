
var schedulesFingerprint = null;
var updateOngoing        = false; 

//action and id set by controller/template engine
var global_action = null; 
var global_id     = null;


function show_dialog_in_viewport()
{
	var dialog = $("#idDialog");
	dialog.show();
	dialog[0].style.top = window.pageYOffset+"px"; 
}
     
function do_add_schedule()
{
	do_update_schedule("add", -1);
	show_dialog_in_viewport();
} 

function do_edit_schedule(dom)
{
	global_action = "edit";
	global_id     = dom.id;
	do_update_schedule(global_action, global_id);
	show_dialog_in_viewport();
}

function do_cancel_dialog()
{
	$("#idDialog").fadeOut();
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

/*
function test_schedules()
{
	var schedule =
	{
		"id"            : "test4711",
		"active"        : true,
		"dayMask"       : 255,
		"strandMask"    : 2+4+8+16,
		"monthMask"     : 8+16+32+64+128,
		"startTime"     : "08:00",
		"endTime"       : "20:00",
		"duration"      : "00:15",
		"interval"      : "01:00",
		"idCondition"   : 0,
		"idException"   : 0,
		"conditionArgs" : null,
		"exceptionArgs" : null,
		"power"         : true
	};
	var template = $("#idScheduleTemplate");    	
	var dom = create_schedules_dom(1, template, schedule);	
 	return dom;
} 
*/   

function clear_schedules()
{
 	var target   = $("#idSchedules");
 	target.empty();    	
}

function update_schedules(action, id)
{
	//log("update_schedules: was called. action=" + action +", id=" + id);    	
	var template = $("#idScheduleTemplate");    	
 	var target   = $("#idSchedules");
	
	updateOngoing = true;
 	
 	if ("delete"==action)
 	{
 		try
 		{	
   	 		sched = $("#"+id);
   	 		sched.hide("slow");
   	 		updateOngoing = false;
   	 		return;
 		}
 		catch (e)
 		{
 			alert("update_schedules: " + e);
 			// fall thru (to have updateOngoing reset)
 		}
 	}
 	else if ("insert"==action)
 	{
 		var url = "rest/schedules/get?id=" + id;   	 		
    	var request = $.getJSON(url, function(data)
    	{
   	 		try
   	 		{
		    	 	var schedule = data["schedule"];
		    	 	var dom = create_schedules_dom(i, template, schedule);
		    	 	// hide the new element before appending it and show afterwards 
		    	 	dom.hide();	  
		    	 	target.append(dom);
		    	 	var appended = $("#" + id);  		
		    	 	appended.show("slow");
		    }
		    catch (e)
	    	{
	    		alert("update_schedules: " + e);
	    	}
    	});
    	request.complete(function() { updateOngoing = false;} );	    	
    	return;
 	}
 	else if ("update"==action)
 	{
 		var updated = $("#" + id);
 		updated.flash(250,1); // -> gardenoid.js
 	}
 	
 	var url = "rest/schedules/list?fingerprint=" + schedulesFingerprint;
	var request = $.getJSON(url, function(data)
	{
	 	var schedules = data["schedules"];
	 	schedulesFingerprint = data["fingerprint"];
	 	
	 	var doms = [];
	 	$.each(schedules, function(i,schedule)
	 	{
	 		var dom = create_schedules_dom(i, template, schedule);	
	   	 	doms.push(dom);
	 	});	    	

   	 	target.empty();
   	 	doms.forEach(function(dom)
   	 	{
   	 		target.append(dom);	
   	 	});	   	 	
	});
	
	request.error(handle_json_error);
	request.complete(function() 
	{
		updateOngoing = false;
		window.setTimeout(update_schedules,5000);
	});
}

function create_schedules_dom(no, template, schedule)
{
	var s  = schedule;    	 
	var id     = s["id"];
	var active = s["active"];
	var power  = s["power"];

	//alert("create_schedules_dom: template: " + template);
	copy = template.clone();
	copy.removeAttr("id");
	copy.attr("id", id);
	dom = $(copy)

	if (power)  
	{
		dom.addClass("power"); 
		//log("schedule " + no + " -> power");
	}    	
	else if (active)
	{ 
		dom.addClass("active");
		//log("schedule " + no + " -> active");
	}
	else if (no%2) 
	{
		dom.addClass("odd");
		//log("schedule " + no + " -> odd");
	}  
	
	mask  = s["strandMask"];
	nodes = dom.find(".strand");
	for (i=1; i<=8; i++)
	{
		name = "strand" + 1;
		nodes.filter("[name=strand" + i + "]")[0].classList.add(mask%2 ? "sx" : "ux");  
		mask = mask>>1;
	}

	mask  = s["dayMask"];
	nodes = dom.find(".day");
	for (i=1; i<=7; i++)
	{
		nodes.filter("[name=day" + i + "]")[0].classList.add(mask%2 ? "sx" : "ux");  
		mask = mask>>1;
	}

	mask  = s["monthMask"];
	nodes = dom.find(".month");
 	for (i=1; i<=12; i++)
	{
		nodes.filter("[name=month" + i + "]")[0].classList.add(mask%2 ? "sx" : "ux");  
		mask = mask>>1;
	}
	
	dom.find(".startTime").text(to_hhmm(s["startTime"]));
	dom.find(".endTime").text(  to_hhmm(s["endTime"]  ));
	dom.find(".duration").text( to_hhmm(s["duration"] ));
	dom.find(".interval").text( to_hhmm(s["interval"] ));
	
	var idCondition   = s["idCondition"];
	var conditionText = ""; 
	try 
	{ 
		conditionText = global_conditionals[idCondition]["name"]; 
	} 
	catch (e) 
	{	
		conditionText = "" + e;
	}    	
	var conditionElem = dom.find(".conditionElem");
	    	
	if (null==idCondition || 0==idCondition)
	{
		conditionElem.hide();
	}
	else
	{    	
		var conditionArgs = s["conditionArgs"];
		conditionText = conditionText.replace("$X", conditionArgs);    		
    	dom.find(".conditionText").text(conditionText);
	}
	
	var idException   = s["idException"];
	var exceptionText = ""; 
	try 
	{ 
		exceptionText = global_conditionals[idException]["name"]; 
	} 
	catch (e) 
	{
		conditionText = "" + e;
	}
	var exceptionElem = dom.find(".exceptionElem");
	
	if (null==idException || 0==idException)
	{
		exceptionElem.hide();
	}
	else
	{
		var exceptionArgs = s["exceptionArgs"];
		log("exceptionArgs: " + exceptionArgs);
		exceptionText = exceptionText.replace("$X", exceptionArgs);
    	dom.find(".exceptionText").text(exceptionText);    	
    }
	return dom;
}

var dialogWindow = null;

///////////////////////////////


function check_time(hhmm)
{
	return hhmm.match(/^[0-2]?[0-9]:[0-5][0-9]$/) || hhmm.match(/^[0-5]?[0-9]$/); 
}
	
	
function show_error(msg, tagNames)
{
	try
	{        		
		$.each(tagNames, function(i,name) { $("[name='" + name + "']")[0].style.background="#ff8080"; });
	}
	catch (e) 
	{
		alert("show_error: " + e);
	}					        	
	alert(msg);        	
	return false;	
}


function check_and_convert(argument) 
{
	try 
	{
    	var errors = "";
    	var errorNames = [];
    	        	  
    	var strandMask = 0;
    	var dayMask    = 0;
    	var monthMask  = 0;
    	var checked    = $("form :checked"); 
    	if (checked)
    	{   	
        	checked.filter(".strand").each(function(i,input){ strandMask |= input.value; });
        	checked.filter(".day"   ).each(function(i,input){ dayMask    |= input.value; });
        	checked.filter(".month" ).each(function(i,input){ monthMask  |= input.value; });        	
        	//alert("strandMask=" + strandMask +", dayMask=" + dayMask + ", monthMask=" + monthMask);
        }

    	if (strandMask<1) { errors += "No strands selected.\n"; errorNames.push('strands'); }
    	if (dayMask<1)    { errors += "No days selected.\n";    errorNames.push('days');    }
    	if (monthMask<1)  { errors += "No months selected.\n";  errorNames.push('months');  }
    	if (""!=errors) return show_error(errors, errorNames);
		    		
		var startTime   = $("input[name='startTime']")[0].value;
		var endTime     = $("input[name='endTime']")[0].value;
		var duration    = $("input[name='duration']")[0].value;
		var interval    = $("input[name='interval']")[0].value;
    	//alert("startTime=" + startTime +", endTime=" + endTime + ", duration=" + duration + ", interval=" + interval);
    	
    	if (!check_time(startTime)) { errors += "Invalid start time.\n"; errorNames.push('startTime'); }
    	if (!check_time(endTime))   { errors += "Invalid end time.\n";   errorNames.push('endTime');   }
    	if (!check_time(duration))  { errors += "Invalid duration.\n";   errorNames.push('duration');  }
    	if (!check_time(interval))  { errors += "Invalid interval.\n";   errorNames.push('interval');  }       
    	if (""!=errors) return show_error(errors, errorNames);
    	
    	startTime = from_hhmm(startTime);
    	endTime   = from_hhmm(endTime);
    	duration  = from_hhmm(duration);
    	interval  = from_hhmm(interval);
    	//alert("startTime=" + startTime +", endTime=" + endTime + ", duration=" + duration + ", interval=" + interval);
    	if (startTime>=endTime) { errors += "Start not before end time.\n"; errorNames.push('startTime','endTime'); }
    	if (startTime>2400    ) { errors += "tart time too high.\n"; errorNames.push('startTime'); }
    	if (endTime>2400      ) { errors += "End time too high.\n";  errorNames.push('endTime');   }
    	if (duration>2400     ) { errors += "Duration too long.\n";  errorNames.push('duration');  }
    	if (""!=errors) return show_error(errors, errorNames);
		
		var idCondition   = $("select[name='idCondition']")[0].value;
		var idException   = $("select[name='idException']")[0].value;
    	var conditionArgs = $("input[name='conditionArgs']")[0].value;
    	var exceptionArgs = $("input[name='exceptionArgs']")[0].value;    		
    	//alert("idCondition=" + idCondition +", idException=" + idException + ", conditionArgs=" + conditionArgs + ", exceptionArgs=" + exceptionArgs);

		if (0!=idCondition && !$.isNumeric(conditionArgs)) {  errors += "Condition arguments not numeric.\n"; errorNames.push('conditionArgs'); }
		if (0!=idException && !$.isNumeric(exceptionArgs)) { errors += "Exception arguments not numeric.\n";  errorNames.push('exceptionArgs'); }
    	if (""!=errors) return show_error(errors, errorNames);        	

		var url = "/rest/schedules/modify" + 
			"?action="      + global_action  + 
			"&id="          + global_id + 
			"&strandMask="  + strandMask +
			"&dayMask="     + dayMask +
			"&monthMask="   + monthMask +
			"&startTime="   + startTime +
			"&endTime="     + endTime +
			"&duration="    + duration +
			"&interval="    + interval +
			"&idCondition=" + idCondition +
			"&idException=" + idException +
			"&conditionArgs=" + conditionArgs +
			"&exceptionArgs=" + exceptionArgs;
			
		//alert("url: " + url);					
		rest_call(url);
	}
	catch (e)
	{
		alert("check_and_convert: Exception: " + e);
	}

	return false; // do not submit (never, since we're doing REST call ourselves)
}


function confirm_deletion()
{
	if (confirm("Do you really want to delete this schedule?"))
	{
		rest_call("/rest/schedules/modify?action=del&id=" + global_id);
	}
}


function rest_success(operation, id)
{
	log("rest_success: operation=" + operation + ", id=" +  id);
	log("rest_success: calling update_schedules(" + operation + "," + id + ")");
	update_schedules(operation, id);
	$("#idDialog").hide();
}


function log(txt)
{
	try { console.log(txt); } catch (e) {}
}


function remote_error(msg)
{
	alert("remote_error: " + msg);
}


function rest_call(url)
{
	//alert("REST URL: " + url)
	$.getJSON(url, function(data) 
	{
		try
		{
			var success = data["success"];
			if (success)
			{
				var op = data["op"];
				var id = data["id"];
				rest_success(op, id);
			}
			else
			{							
				var error = data["error"];
				remote_error(error);
			}
		}
		catch (e)
		{
			rest_error("Exception: " + e);
		}					
	})
	.error(handle_json_error);
}

function do_update_schedule(action, id)
{
	global_action = action;
	global_id     = id;
	//alert("do_update_schedule(" + action + "," + id + ") called, set global_action and global_id");
	update_schedule();
}

function update_schedule()
{
	if ("add"==global_action)
	{
		$("input[name='submit']").val("Add");
		$("#idTitle").text("Add new schedule");
		
		schedule = { 
				"active"        : false,
				"strandMask"    : 1,
				"startTime"     : "0800",
				"endTime"       : "2000",
				"duration"      : "0010",
				"interval"      : "0400",
				"dayMask"       : 255,
				"monthMask"     : 8+16+32,        					 
				"idCondition"   : 0,
				"conditionArgs" : "",  
				"idException"   : 0,
				"exceptionArgs" : "",  
			};
		set_input_values(schedule);		
		
		try
		{
			$("#idDeleteButton").hide();
			//$("#idScheduleDialog").remove();
		}
		catch (e)
		{
			alert(e);
		}
		return;	
	}
	else
	{
		$("#idDeleteButton").show();
		$("#idTitle").text("Edit schedule");
	}
	
	$.getJSON("/rest/schedules/get?id=" + global_id, function(data)
	{
	 	schedule = data["schedule"];   
	 	set_input_values(schedule);
	} );	    	
}    


function set_input_values(schedule)
{
	s = schedule;
	
 	active        = s["active"];    		 	
 	idCondition   = s["idCondition"];
 	conditionArgs = s["conditionArgs"];
 	idException   = s["idException"];
 	exceptionArgs = s["exceptionArgs"];
 	
 	strandBits = mask_to_bitarray(s["strandMask"], 8);
 	dayBits    = mask_to_bitarray(s["dayMask"],    7);
 	monthBits  = mask_to_bitarray(s["monthMask"], 12);    		 	
 	//alert("strandBits=" + strandBits + ", dayBits=" + dayBits + ", monthBits=" + monthBits);

 	for (i=1; i<=8;  i++) $("input[name='strand" + i +"']")[0].checked = strandBits[i-1];
 	for (i=1; i<=7;  i++) $("input[name='day"    + i +"']")[0].checked = dayBits[i-1];
 	for (i=1; i<=12; i++) $("input[name='month"  + i +"']")[0].checked = monthBits[i-1];
 	
 	$.each(["startTime","endTime","duration","interval"], function(i, name)
 	{ 
 		$("input[name='" + name + "']")[0].value = format_hhmm(s[name]);
 	});
 	
 	$("select[name='idCondition']").val(idCondition);
 	$("select[name='idException']").val(idException);    		 	    		 	
	$("input[name='conditionArgs']").val(conditionArgs);
	$("input[name='exceptionArgs']").val(exceptionArgs);    		 	
}    




