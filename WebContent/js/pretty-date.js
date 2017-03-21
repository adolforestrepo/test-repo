/*
 * Based on JavaScript Pretty Date
 * Copyright (c) 2008 John Resig (jquery.com)
 * Licensed under the MIT license.
 */
Date.prototype.pretty = function(settings){
	var settings = {};
	var myDate = this;
	if(typeof myDate == "string") 
		myDate = Date.parse(myDate);
	var nowDate = new Date();
	var nowTime = nowDate.getTime();
	var dateTime = myDate.getTime();
	//get the absolute value so we can just figure out the difference
	var diff = parseInt(Math.abs((nowTime - dateTime) / 1000));
	//find out if today is before the given date
	var isPrior = nowTime > dateTime;
	var isToday = 0;
	//round it down for days
	var day_diff = Math.floor(diff / 86400);
	//however, anything over 1 is not tomorrow or yesterday, so we need to fix that
	if(day_diff == 1) {
		var d1 = myDate.clone();
		var d2 = nowDate.clone();
		d1.clearTime();
		d2.clearTime();
		//if the date is after right now, then make sure we are only 1 day behind
		if(isPrior) {
			if(!d1.equals(d2.addDays(1))) day_diff++;
		} else {
			if(d1.equals(d2)) isToday = true;
			else if(!d1.equals(d2.addDays(-1))) day_diff++;
		}
	}
	//check for an rsuite property, default is 1 day - settings.maxDays will always win out though 
	settings.maxDays = "6";
	if ( isNaN(day_diff) || !settings.maxDays || day_diff > settings.maxDays) {
		return myDate.toString(settings.pattern);
	}
	//maxDays of 1.5 gets special handling, it will only show today/tomorrow/yesterday if within 24 hours, everything else gets a date
	if(settings.maxDays == 1.5) {
		if(isToday) return "Today";
		if(day_diff <= 1) return ((isPrior) ? "Yesterday" : "Tomorrow"  );
		else return myDate.toString(settings.pattern);
	}
	switch(true) {
	case (diff < 60): return "just now";
	case (diff < 120): return Date.doAgo("1 minute",isPrior);
	case (diff < 3600): return Date.doAgo(Math.floor( diff / 60 ) + " minutes",isPrior);
	case (diff < 7000): return Date.doAgo("1 hour",isPrior);
	case (diff < 86400):
		//if it's past 45 minutes of an hour then round up to the next hour 
		var hdiff = Math.floor( diff / 3600 );
		if(diff/3600 - hdiff > 0.45) hdiff++;
		return (hdiff > 1) ? Date.doAgo(hdiff + " hours",isPrior) : Date.doAgo(hdiff + " hour",isPrior);
	case (day_diff == 1): return ((isPrior) ? "Yesterday" : "Tomorrow"  );
	case (day_diff < 7): return Date.doAgo(day_diff + " days",isPrior);
	case (day_diff == 7): return Date.doAgo(Math.ceil( day_diff / 7 ) + " week",isPrior);
	case (day_diff < 31): return Date.doAgo(Math.ceil( day_diff / 7 ) + " weeks",isPrior);
	case (day_diff < 365): return Date.doAgo("about "+(Math.ceil( day_diff / 30 )) + " months",isPrior);
	case (day_diff < 770) : return Date.doAgo("over a year",isPrior);
	default: return Date.doAgo("over "+Math.ceil(day_diff/365)+" years",isPrior);
	
	}
}