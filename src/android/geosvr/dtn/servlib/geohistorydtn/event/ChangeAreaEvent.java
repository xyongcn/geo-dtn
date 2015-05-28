package android.geosvr.dtn.servlib.geohistorydtn.event;

import android.geosvr.dtn.servlib.bundling.event.BundleEvent;
import android.geosvr.dtn.servlib.bundling.event.event_type_t;
import android.geosvr.dtn.servlib.geohistorydtn.area.Area;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-5-26 下午1:30:30 
 * 说明 
 */
public class ChangeAreaEvent extends BundleEvent
{
	//改变到的区域的最底层区域
	private Area baseArea;
	
	
	
	private ChangeAreaEvent() {
		super(event_type_t.CHANGE_AREA_EVENT);
		// TODO Auto-generated constructor stub
	}
	
	public ChangeAreaEvent(Area baseArea)
	{
		super(event_type_t.CHANGE_AREA_EVENT);
		
		this.baseArea=baseArea;
	}

	/**
	 * 获取底层区域
	 */
	public Area getBaseArea()
	{
		return baseArea;
	}
	
	/**
	 * 设置底层区域
	 */
	public void setBaseArea(Area baseArea)
	{
		this.baseArea=baseArea;
	}
}
