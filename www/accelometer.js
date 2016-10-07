var argscheck = require('cordova/argscheck');
var utils = require('cordova/utils');
var exec = require('cordova/exec');
var timers = {};


var Accelometer = function(){

}


Accelometer.prototype = {
	getReading: function(onSuccessCallback,onErrorCallback){
		cordova.exec(onSuccessCallback,onErrorCallback,"Accelometer","getReading",[]);

	},

	watchReadings: function(onSuccessCallback, onErrorCallback){
		//start timer to get magnitude
		var accelometer = this;
		var id = utils.createUUID();

		if (cordova.platformId === 'android') {
			timers[id] = window.setInterval(function() {
          		accelometer.getReading(successCallback, errorCallback);
      		}, 40); // every 40 ms (25 fps)
		}
		else {
			cordova.exec(onSuccessCallback,onErrorCallback, "Accelometer","watchReadings",[]);
			return id;
		}
	},

	stop: function(watchID){
		if (cordova.platformId === 'android'){
			// stop a single watch
			window.clearInterval(timers[watchID]);
			delete timers.watchID;
		}
		else{
			// stop all timers
			for (var id in timers) {
				window.clearInterval(timers[id]);
				delete timers.id;
			}
		}
		else cordova.exec(function() {}, function() {throw "Error stopping accelometer"}, "Accelometer","stop",[]);

	}
}

module.exports  = new Accelometer();