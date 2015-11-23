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

package android.geosvr.dtn.servlib.bundling;

import java.io.Serializable;
import java.util.HashMap;

import android.geosvr.dtn.servlib.naming.EndpointID;


/**
 * Caching of Bundle implementation for detecting duplicated Bundle
 * @author Rerngvit Yanggratoke (rerngvit@kth.se)
 */
public class BundleInfoCache implements Serializable{

	/**
	 * Serial version UID to support Java Serializable 
	 */
	private static final long serialVersionUID = 6810683934464012479L;
	
	/**
	 * Internal HashMap to keep track of the Bundles
	 * 判断该bundle是否向该EndpointID发送过应该采取bundle的关键字来区分，而不是bundle的引用。
	 * ((b.source().equals(b2.source()))
						&& (b.creation_ts().seconds() == b2.creation_ts()
								.seconds())
						&& (b.creation_ts().seqno() == b2.creation_ts().seqno())
						&& (b.is_fragment() == b2.is_fragment())
						&& (b.frag_offset() == b2.frag_offset()) &&
						(b.orig_length() == b2.orig_length()) && 
						(b.payload().length() == b2.payload().length()))
	 * 作用：记录是从哪个link的eid收到了该bundle，之后不再向该link的eid重复发送该bundle
	 */
	private HashMap<String,EndpointID> bundle_info_cache_;
	
	    /**
	     * "Constructor that takes the and the number of entries to
	     * maintain in the cache." [DTN2]
	     */
		public  BundleInfoCache(int capacity)
		{
			bundle_info_cache_ = new HashMap<String, EndpointID>(capacity);
		}

	
	    /**
	     * "Try to add the bundle to the cache. If it already exists in the
	     * cache, adding it again fails, and the method returns false." [DTN2]
	     */
		public boolean add_entry(final Bundle bundle, final EndpointID prevhop)
		{
			//bundle转关键字
			String bundlestr=String.format("%s#%s#%d#%d#%b#%d#%d#%d",
					bundle.dest().toString(),bundle.source().toString(),bundle.creation_ts().seconds(),bundle.creation_ts().seqno(),
					bundle.is_fragment(),bundle.frag_offset(),bundle.orig_length(),bundle.payload().length());
			
			if (bundle_info_cache_.containsKey(bundle)) 
				return false;
			else{
				bundle_info_cache_.put(bundlestr, prevhop);
				return true;
			}
		}
		

	
		
	    /**
	     * "Check if the given bundle is in the cache, returning the EID of
	     * the node from which it arrived (if known).
	     * Calling get after word
	     * Return null if it's not found" [DTN2]
	     */
		public EndpointID lookup(final Bundle bundle)
		{
			//bundle转关键字
			String bundlestr=String.format("%s#%s#%d#%d#%b#%d#%d#%d",
					bundle.dest().toString(),bundle.source().toString(),bundle.creation_ts().seconds(),bundle.creation_ts().seqno(),
					bundle.is_fragment(),bundle.frag_offset(),bundle.orig_length(),bundle.payload().length());
			
			return bundle_info_cache_.get(bundlestr);
		}

	    /**
	     * "Flush the cache." [DTN2]
	     */
		public void evict_all()
		{
			bundle_info_cache_.clear();
		}
	
}
