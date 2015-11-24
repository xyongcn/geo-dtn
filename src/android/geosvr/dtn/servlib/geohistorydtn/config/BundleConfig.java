package android.geosvr.dtn.servlib.geohistorydtn.config;

import android.geosvr.dtn.applib.DTNAPICode.dtn_bundle_priority_t;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-6-23 下午1:45:56 
 * 说明 
 */
public class BundleConfig {

	//较少副本转发的副本上限
	public static final int DELIVERBUNDLENUM=2;
	
	//受限洪泛算法的副本上限
	public static final int FLOODBUNDLENUM=2;
	
	
	/**
	 * Default expiration time in seconds, set to 1 hour
	 */
	private static final int EXPIRATION_TIME = 1*60*60;
	
	/**
	 * Set delivery options to don't flag at all
	 */
	private static final int DELIVERY_OPTIONS = 0;
	
	/**
	 * Set priority to normal sending
	 */
	private static final dtn_bundle_priority_t PRIORITY = dtn_bundle_priority_t.COS_NORMAL;
}
