/*
 *	  This file is part of the Bytewalla Project
 *    More information can be found at "http://www.tslab.ssvl.kth.se/csd/projects/092106/".
 *    
 *    Copyright 2009 Telecommunication Systems Laboratory (TSLab), Royal Institute of Technology, Sweden.
 *    
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 */

package android.geosvr.dtn.servlib.reg;

import android.geosvr.dtn.servlib.bundling.Bundle;
import android.geosvr.dtn.servlib.bundling.BundleDaemon;
import android.geosvr.dtn.servlib.bundling.event.BundleDeliveredEvent;
import android.geosvr.dtn.servlib.naming.EndpointIDPattern;
import android.geosvr.dtn.servlib.storage.RegistrationStore;
import android.util.Log;


/**
 * This class is for Registration  testing purpose to take the log of bundle's metadata.
 *  */

public class LoggingRegistration extends Registration {

	/**
	 * SerialVersionID to Support Serializable.
	 */
	private static final long serialVersionUID = 4762023883066134693L;

	
	/**
	 * TAG for Android Logging
	 */
	private static String TAG = "LoggingRegistration";

	/**
	 * Constructor to initialize this class.
	 */

	public LoggingRegistration(final  EndpointIDPattern endpoint){
		super(RegistrationStore.getInstance().next_regid(), endpoint, Registration.failure_action_t.DEFER, 0, 0,"");
		set_active(true);
	 }

	/**
	 * Delivery bundle take the log of Bundle and then forward it to BundleDaemon
	 * @param b Bundle to take log of its metadata  
	 */

	@Override
	public void deliver_bundle(Bundle b){
		 
	    StringBuffer buf = new StringBuffer();
	    b.format_verbose(buf);
	    
	    Log.d(TAG, buf.toString());

	    BundleDaemon.getInstance().post(new BundleDeliveredEvent(b, this)); 
	 }	
}
