package android.geosvr.dtn.servlib.geohistorydtn.event;

import android.geosvr.dtn.servlib.bundling.event.BundleEvent;
import android.geosvr.dtn.servlib.bundling.event.event_type_t;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-5-26 下午1:30:30 
 * 说明 
 */
public class ChangeAreaEvent extends BundleEvent
{

	public ChangeAreaEvent() {
		super(event_type_t.CHANGE_AREA_EVENT);
		// TODO Auto-generated constructor stub
	}

}
