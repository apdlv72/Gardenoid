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
	//if (operation=="insert") alert("Schedule added");
	//if (operation=="update") alert("Schedule updated");
	//if (operation=="delete") alert("Schedule deleted");
	log("rest_success: operation=" + operation + ", id=" +  id);
	if (window.opener)
	{
		log("asking window.opener to update schedules"); 
		window.opener.update_schedules(operation, id);
	}
	//if (confirm("Close window?"))
	{
		window.close();
	}					
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


function cancel_dialog()
{
	window.close();
}

function update_schedule()
{
	do_update_schedule(global_action, global_id);
}

function do_update_schedule(action, id)
{
	if ("add"==action)
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
			$("#idDeleteButton").remove();
			$("#idMenu").remove();
		}
		catch (e)
		{
			alert(e);
		}
		return;	
	}
	else
	{
		$("#idTitle").text("Edit schedule");
	}
	
	$.getJSON("/rest/schedules/get?id=" + id, function(data)
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

 	// $("input[name='idCondition2']").val(idCondition);
 	// $("input[name='idException2']").val(idException);    		 	    		 	
	// $("input[name='conditionArgs2']").val(conditionArgs);
	// $("input[name='exceptionArgs2']").val(exceptionArgs);    		 	        	
}    
