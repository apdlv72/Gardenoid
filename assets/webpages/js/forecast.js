function show_weather(node)
{
	var day    = $(node).find("#idDay").text();
	var future = $(node).find("#idFuture").text();
	if ("true"==future)
	{
		alert("Date is in future!");
		return;
	}
	var url = "weather.html?desktop=" + global_desktop + "&day=" + day + "&nonce=" + make_nonce();
	document.location = url;
}

function create_forecast_dom(no, template, forecast)
{
	var f    = forecast;    	 
	var id   = f["id"];
	var day  = f["day"];
	var code = f["code"];
	var text = f["text"];
	var low  = f["low"];
	var high = f["high"];
	var upd  = f["upd"];
	var future = f["future"];

	var copy = template.clone();
	copy.removeAttr("id");
	copy.attr("id", id);
	var dom = $(copy)

	var dow = new Date(day).getDay(); // 5 = friday
	if      (dow==0) dow="Sun";
	else if (dow==1) dow="Mon";
	else if (dow==2) dow="Tue";
	else if (dow==3) dow="Wed";
	else if (dow==4) dow="Thu";
	else if (dow==5) dow="Fri";
	else if (dow==6) dow="Sat";
	else if (dow==7) dow="Sun";	
	
	if      ("Sun"==dow) dom.addClass("sunday");
	else if ("Sat"==dow) dom.addClass("saturday");
	else if (no%2)       dom.addClass("odd");
	
	if (future)
	{
		dom.addClass("future");
		// disable n click action (opens weather for historic forecasts only)
		var cont = dom.find("#idContainer")[0];
		cont.onclick = null;
	}
	
	dom.find("#idDay").text(day);
	dom.find("#idDow").text(dow);
	dom.find("#idText").text(text);
	dom.find("#idCode").text(code);
	dom.find("#idLow").text(low);
	dom.find("#idHigh").text(high);
	dom.find("#idFuture").text(future);
	var img = dom.find("#idImg")[0];
	img.src = "http://l.yimg.com/a/i/us/we/52/" + code + ".gif";
	
	return dom;
}
    
function update_forecasts()
{
	var template       = $("#idForecastTemplate");
 	var targetFuture   = $("#idForecastsFuture");
 	var targetHistoric = $("#idForecastsHistoric");

 	var url = "rest/forecast/list";
 	var haveHistoric = false;
 	var haveFuture   = false;
 	
	var request = $.getJSON(url, function(data)
	{
	 	var forecasts = data["forecasts"];
	 	var domsFuture   = [];
	 	var domsHistoric = [];
	 	
	 	$.each(forecasts, function(i,forecast)
	 	{
	 		var dom = create_forecast_dom(i, template, forecast);
	 		if (forecast["future"])
	 		{
	 			domsFuture.push(dom);
	 		}
	 		else
	 		{
	 			domsHistoric.push(dom);
	 		}
	 	});	    	

	 	targetFuture.empty();
	 	domsFuture.forEach(function(dom)
   	 	{
   	 		targetFuture.append(dom);	
 			haveFuture = true;
   	 	});	   	 	
	 	
	 	targetHistoric.empty();
	 	domsHistoric.forEach(function(dom)
   	 	{
	 		targetHistoric.append(dom);	
 			haveHistoric = true;
   	 	});	   	 	

	 	if (haveHistoric)
		{
			$("#idLoading").text("Historic forecasts:");
		}
		else if (!haveFuture)
		{
			$("#idLoading").text("No forecast data.");
		}	
	});
	
	request.error(handle_json_error);
	request.complete(function() 
	{
		updateOngoing = false;
		window.setTimeout(update_forecasts,300*1000); // every 5 minutes
	});		
}
