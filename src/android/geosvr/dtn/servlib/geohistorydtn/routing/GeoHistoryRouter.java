package android.geosvr.dtn.servlib.geohistorydtn.routing;

import android.geosvr.dtn.servlib.bundling.BundleDaemon;
import android.geosvr.dtn.servlib.bundling.event.BundleEvent;
import android.geosvr.dtn.servlib.geohistorydtn.area.Area;
import android.geosvr.dtn.servlib.geohistorydtn.area.AreaManager;
import android.geosvr.dtn.servlib.geohistorydtn.event.ChangeAreaEvent;
import android.geosvr.dtn.servlib.geohistorydtn.event.NewAreaEvent;
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
			//添加新区域
			break;
		
		//区域改变的事件,区域变化后会先出现change_area_event，然后再进行new_area_event
		case CHANGE_AREA_EVENT:
			//更新区域底层
			break;
		}
	}

	/**
	 * 维护当前的数据
	 */
	Area baseArea;//最底层的区域
	
	/**
	 * 公共方法
	 * 判断当前是否离开当前区域
	 */
	public boolean isChangedArea(Area nowBaseArea)
	{
		return !nowBaseArea.equals(baseArea);
		
//		return false;
	}
	
	/**
	 * 
	 * @param now：当前所在的最底层区域
	 */
	public void locationChanged(Area now)
	{
		//当前所在的最底层区域改变了
		if(isChangedArea(now))
		{
			//当前所在最底层区域不在areaManager里面
			if(AreaManager.getInstance().hasThisBaseArea(now))
			{
				BundleDaemon.getInstance().post(new ChangeAreaEvent());
			}
			else
			{
				BundleDaemon.getInstance().post(new NewAreaEvent());
			}
		}
		
	}
}
