package oorg.apache.cordova.accelometer; 

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.Math;
import java.util.List;
import java.util.ArrayList;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.Context;


import android.os.Handler;
import android.os.Looper;


public class Accelometer extends CordovaPlugin implements SensorEventListener{

	//to get the sensor manager running status

	public static int STOPPED = 0;
    public static int STARTING = 1;
    public static int RUNNING = 2;
    public static int FAILED_TO_START = 3;

    public long TIMEOUT = 30000;		//shutdown the listner
    public float NOISE = (float)1.0; 

    int status;					//running status listner
    float x;					//acceleration in x axis
    float y;					//acceleration in y axis
    float z;					//acceleration in z axis
    long timeStamp;				//time of most recent value
    long lastAccessTime;			//time the value was last requested

    float ox,oy,oz;

    private SensorManager aSensorManager; 	//sensor manager
    Sensor aSensor;							//Acceleration sensor

    private CallbackContext callbackContext;
    List<CallbackContext> watchContexts;

    public Accelometer(){
    	this.x = 0;
    	this.y = 0;
    	this.z = 0;
    	this.ox = 0;
    	this.oy = 0;
    	this.oz = 0;
    	this.timeStamp = 0;
    	this.watchContexts = new ArrayList<CallbackContext>();
    	this.setStatus(Accelometer.STOPPED);
    }

    public void onDestroy(){
    	this.stop();
    }

    public void onReset(){
    	this.stop();
    }


    //--------------------------------------------------------------
    // Corodova Plugin Methods
    //--------------------------------------------------------------

    public void initialize(CordovaInterface cordova, CordovaWebView webView){
    	super.initialize(cordova,webView);
        this.asensorManager = (SensorManager) cordova.getActivity().getSystemService(Context.SENSOR_SERVICE);

    }


    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException{
    	if (action.equals("start")){
    		this.start();
    	}

    	else if (action.equals("stop")){
    		this.stop();
    	}

    	else if (action.equals("getStatus")) {
    		int i = this.getStatus();
    		callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,i));
    	}

    	else if (action.equals("getReading")){
    		//this should only call when listner is running
    		if (this.status != Accelometer.RUNNING){
    			int val = this.start();
    			System.out.println("In the function");

    			if(val == Accelometer.FAILED_TO_START){
    				callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.IO_EXCEPTION, Accelometer.FAILED_TO_START));
    				return false;
    			}

    			//set timeout call back on main thread if failed to start
    			Handler handler = new Handler(Looper.getMainLooper());
    			handler.postDelayed(new Runnable() {
    				public void run() {
    					Accelometer.this.timeout();
    				}
    			},2000);
    		}
    		callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,getReading()));

    		
    	}
    	else{
    		// action not defined
    		return false;
    	}

    	return true;
    }



	//--------------------------------------------------------------
	// Local Methods
	//--------------------------------------------------------------


	/*****************************
	 * start to listening to sensor
	 *****************************
	 */

	/**
	 * @return
	 */

	public int start(){

		// if accelerometer is already running
		if((this.status == Accelometer.RUNNING) || (this.status == Accelometer.STARTING)){
			return this.status;
		}

		// get accelorometer from sensor manager
		@SuppressWarnings("deprecation")
		Sensor sensor = aSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		// if sensor found, register as listner
		if (aSensor != null){
			this.aSensor = sensor;
			this.asensorManager.registerListener(this, this.aSensor, SensorManager.SENSOR_DELAY_NORMAL);
			this.lastAccessTime = System.currentTimeMillis();
			this.setStatus(Accelometer.STARTING);
		}

		else{
			this.setStatus(Accelometer.FAILED_TO_START);
		}

		return this.status;
	}


	/**************************************
	 * stop listing to sensor
	 **************************************
	 */

	public void stop(){
		if (this.status != Accelometer.STOPPED){
			this.aSensorManager.unregisterListener(this);
		}
		this.setStatus(Accelometer.STOPPED);
	}



    /**
     * Called after a delay to time out if the listener has not attached fast enough.
     */

	private void timeout() {
        if (this.status == Accelometer.STARTING) {
            this.setStatus(Accelometer.FAILED_TO_START);
            if (this.callbackContext != null) {
                this.callbackContext.error("Magnetometer listener failed to start.");
            }
        }
    }


	//--------------------------------------------------------------
	// SensorEventListner interface
	//--------------------------------------------------------------

   /**
     * Sensor listener event.
     * @param event
     */

	public void onSensorChanged(SensorEvent event){

		this.timeStamp = System.currentTimeMillis();

		float nx = event.values[0];
		float ny = event.values[1];
		float nz = event.values[2];

		this.x = Math.abs(ox-nx);
		this.y = Math.abs(oy-ny);
		this.z = Math.abs(oz-nz);

		if (this.x < NOISE) this.x = (float)0.0;
		if (this.y < NOISE) this.y = (float)0.0;
		if (this.z < NOISE) this.z = (float)0.0;

		ox = nx;
		oy = ny;
		oz = nz;

		// If heading hasn't been read for TIMEOUT time, then turn off compass sensor to save power
        if ((this.timeStamp - this.lastAccessTime) > this.TIMEOUT) {
            this.stop();
        }

	}

	/**
     * Required by SensorEventListener
     * @param sensor
     * @param accuracy
     */

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // DO NOTHING
    }





	//--------------------------------------------------------------
	// JavaScript Interacion
	//--------------------------------------------------------------


    /**
     * get status from accelorometer
     * @return status
     *
     */

    public int getStatus(){
    	return this.status;
    }

    /**
     * set status and send 
     * @param status
     */

    private void setStatus(int status){
    	this.status = status;
    }

    /**
     * generate JSON objecet to return JS
     * @return a accelorometer sensor reading
	 */

    private JSONObject getReading() throws JSONException {
    	JSONObject obj = new JSONObject();

    	obj.put("x", this.x);
    	obj.put("y", this.y);
    	obj.put("z", this.z);

    	return obj;

    }
}





