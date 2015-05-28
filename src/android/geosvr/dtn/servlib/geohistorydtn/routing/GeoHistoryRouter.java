package android.geosvr.dtn.servlib.geohistorydtn.routing;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import android.geosvr.dtn.servlib.bundling.BundleDaemon;
import android.geosvr.dtn.servlib.bundling.event.BundleEvent;
import android.geosvr.dtn.servlib.geohistorydtn.area.Area;
import android.geosvr.dtn.servlib.geohistorydtn.area.AreaInfo;
import android.geosvr.dtn.servlib.geohistorydtn.area.AreaManager;
import android.geosvr.dtn.servlib.geohistorydtn.event.NewAreaEvent;
import android.geosvr.dtn.servlib.routing.TableBasedRouter;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-5-26 下午1:31:44 
 * 说明 
 */
public class GeoHistoryRouter extends TableBasedRouter implements Runnable
{
	static String tag="GeoHistoryRouter";
	
	public GeoHistoryRouter() {
		// TODO Auto-generated constructor stub\
		super();
		
		init();
	}
	
	private void init()	
	{
		areaInfoList=new LinkedBlockingDeque<AreaInfo>();
		
		(new Thread(this)).start();
	}
	
	/*@Override
	public void handle_event(BundleEvent event) {
		// TODO Auto-generated method stub
		//针对该路由算法的事件处理
		geohisDispatchEvent(event);
		
		//调用父类的父类的方法，分发事件
		super.handle_event(event);
	}*/

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
			//更新区域底层以及区域的相关频率向量
//			baseArea
			
			break;
		}
	}

	/**
	 * 维护当前节点所在的最底层区域信息
	 */
	Area baseArea;//最底层的区域
	
	/**
	 * 处理区域变化的列表
	 */
	BlockingQueue<AreaInfo> areaInfoList;
	
	/**
	 * 判断当前节点所在区域是否离开原来区域
	 * @param areainfo
	 * @return:返回true则是标志区域发生变化，返回false表示区域没有变化
	 */
	public boolean isChangedArea(AreaInfo areainfo)
	{
		if(baseArea.getAreaId()==areainfo.getBaseAreaId())
		{
			return false;
		}
		else
			return true;
	}
	
	
	/**
	 * 作用				：由监测节点移动位置的线程负责调用，当探测到节点位置变化或者区域变化时调用
	 * @param areainfo	:节点area变化的信息
	 * @throws InterruptedException :队列已经满了
	 */
	public void locationChanged(AreaInfo areainfo)
	{
		if(areainfo!=null)
			this.areaInfoList.add(areainfo);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		//此处后续需要改为bundleDaemon里面的逻辑标志位
		while(true)
		{
			if(BundleDaemon.getInstance().shutting_down())
				break;
			
			try //防止列表里面去数据的阻塞被打断
			{
				AreaInfo ainfo=areaInfoList.take();
				if( isChangedArea(ainfo))
				{
					//调用AreaManager，判断是否为新区域，
					Area newBaseArea=AreaManager.getInstance().checkAreaInfo(ainfo);
					if(newBaseArea!=null)
					{
						//变动相应的频率向量
						newBaseArea.changeFVector(ainfo);
						
						//加入到新的倒数时间列表里面,将原来的从倒数计时里面去掉
						
						
						//更改当前的底层区域向量
						baseArea=newBaseArea;
					}
				}
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void chageRelatedFVector(Area area)
	{
		
	}
}
