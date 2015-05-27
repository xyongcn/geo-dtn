package android.geosvr.dtn.servlib.geohistorydtn.routing;

import android.geosvr.dtn.servlib.bundling.event.BundleEvent;
import android.geosvr.dtn.servlib.bundling.event.event_type_t;
import android.geosvr.dtn.servlib.routing.TableBasedRouter;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-5-26 下午1:31:44 
 * 说明 
 */
public class GeoHistoryRouter extends TableBasedRouter
{

	
	
	@Override
	public void handle_event(BundleEvent event) {
		// TODO Auto-generated method stub
		//针对该路由算法的事件处理
		geohisDispatchEvent(event);
		
		//调用父类的父类的方法，分发事件
		super.handle_event(event);
	}

	/**
	 * 没有被调用
	 */
	@Override
	public void thread_handle_event(BundleEvent event) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 针对该路由算法的事件处理
	 * @param event
	 */
	public void geohisDispatchEvent(BundleEvent event)
	{
		switch(event.type())
		{
		//新区域的事件
		case NEW_AREA_EVENT:
			
			break;
		
		//区域改变的事件
		case CHANGE_AREA_EVENT:
			
			break;
		}
	}

}
