    
        
function next_day()
{
	var d = new Date(global_day);
	d.setTime(d.getTime()+24*60*60*1000);
	
	var y = 1900+d.getYear();
	var m = 1+d.getMonth();
	var d = d.getDate();
	
	if (m<10) m="0"+m;
	if (d<10) d="0"+d;
	
	global_day = y + "-" + m + "-" + d;
	update_content();
}    
    
function prev_day()
{
	var d = new Date(global_day);
	d.setTime(d.getTime()-24*60*60*1000);
	
	var y = 1900+d.getYear();
	var m = 1+d.getMonth();
	var d = d.getDate();
	
	if (m<10) m="0"+m;
	if (d<10) d="0"+d;
	
	global_day = y + "-" + m + "-" + d;
	update_content();
}    
    
function onclick_weather(node)
{
	var date = $(node).find("#idDate").text();
	//alert("NYI: date=" + date);	
}

function create_dom(no, template, item)
{
	var i     = item;    	 
	var id    = i["id"];
	var date  = i["date"];
	var code  = i["code"];
	var text  = i["text"];
	var temp  = i["t"];
	var humi  = i["hum"];
	var visi  = i["visi"];
	var press = i["p"];
	var rise  = i["rise"];
	var upd   = i["upd"];
	// remove date (prefix) and seconds (suffix) 
	var time  = date.replace(/.* /,"").replace(/:..$/,"");

	copy = template.clone();
	copy.removeAttr("id");
	copy.attr("id", id);
	dom = $(copy)

	/*
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
	*/
	
	dom.find("#idTime").text(time);
	dom.find("#idCode").text(code);
	dom.find("#idText").text(text);
	dom.find("#idTemp").text(temp);
	dom.find("#idHumi").text(humi);
	dom.find("#idVisi").text(visi);
	dom.find("#idRise").text(rise);
	var img = dom.find("#idImg")[0];
	img.src = "http://l.yimg.com/a/i/us/we/52/" + code + ".gif";
	
	return dom;
}
 
function update_content()
{
	var template = $("#idTemplate");    	
 	var target   = $("#idVisible");

 	var url = "rest/weather/list?day=" + global_day + "&nonce=" + make_nonce();
	var request = $.getJSON(url, function(data)
	{
	 	var list = data["weather"];
	 	var day  = data["day"];
	 	$("#idDay").text(day);
	 	
	 	var doms = [];
	 	$.each(list, function(i, item)
	 	{
	 		var dom = create_dom(i, template, item);	
	   	 	doms.push(dom);
	   	 	//log(item);
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
		window.setTimeout(update_content,5*60*1000); // every 5 minutes
	});	
}
