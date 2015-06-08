package android.geosvr.dtn.servlib.geohistorydtn.routing;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import android.geosvr.dtn.servlib.bundling.BundleDaemon;
import android.geosvr.dtn.servlib.bundling.event.BundleEvent;
import android.geosvr.dtn.servlib.bundling.event.BundleReceivedEvent;
import android.geosvr.dtn.servlib.bundling.event.ContactUpEvent;
import android.geosvr.dtn.servlib.geohistorydtn.area.Area;
import android.geosvr.dtn.servlib.geohistorydtn.area.AreaInfo;
import android.geosvr.dtn.servlib.geohistorydtn.area.AreaManager;
import android.geosvr.dtn.servlib.routing.TableBasedRouter;
import android.util.Log;

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
	 * 针对收到要发送的bundle，需要转发的bundle，邻居交互信息的bundle的处理
	 */
	@Override
	protected void handle_bundle_received(BundleReceivedEvent event) {
		// TODO Auto-generated method stub
		super.handle_bundle_received(event);
	}

	/**
	 * 针对新邻居发现的事件处理
	 */
	@Override
	protected void handle_contact_up(ContactUpEvent event) {
		// TODO Auto-generated method stub
//		super.handle_contact_up(event);
		
//		a)	触发对已有邻居信息的保存
		
//		b)	触发邻居之间交换信息的bundle发送
		
//		c)	触发已有bundle队列里面的所有bundle的route事件（会尝试判断是否发送这些bundle）。

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
	/*public void geohisDispatchEvent(BundleEvent event)
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
	}*/

	/**
	 * 维护当前节点所在的最底层区域信息
	 */
	Area baseArea=null;//最底层的区域
	
	/**
	 * 处理区域变化的列表
	 */
	BlockingQueue<AreaInfo> areaInfoList;
	
	/**
	 * 该节点移动时每次获取一下区域信息就加入到该队列
	 * @param:最新区域信息
	 */
	public boolean movetoArea(AreaInfo areainfo)	
	{
		if(areainfo==null)
			return false;
		return areaInfoList.add(areainfo);
	}
	
	/**
	 * 判断当前节点所在区域是否离开原来区域
	 * @param areainfo
	 * @return:返回true则是标志区域发生变化，返回false表示区域没有变化
	 */
	private boolean isChangedArea(AreaInfo areainfo)
	{
		if(areainfo==null)
			return false;
		
		if(baseArea==null)
			return true;
		
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
	/*public void locationChanged(AreaInfo areainfo)
	{
		if(areainfo!=null)
			this.areaInfoList.add(areainfo);
	}*/

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
						//变动相应的频率向量,因为每次计时到了就开始改变FVector，所以在变更区域的时候不需要进行变动频率向量
//						newBaseArea.changeFVector(ainfo);
						
						//加入到新的倒数时间列表里面,将原来的从倒数计时里面去掉
						changeAreaTimeCount(baseArea, newBaseArea);
						
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
	
	/**
	 * 作用：当该节点所在区域变动时，改变它相应的频率向量的时间计时
	 * @param oldarea：旧的底层区域
	 * @param newarea：新的底层区域
	 */
	private void changeAreaTimeCount(Area oldarea,Area newarea)
	{
		if(!oldarea.equals(newarea) && oldarea.getAreaLevel()==newarea.getAreaLevel())
		{
			Log.d(tag,String.format("变动计时器中新旧区域的频率向量（旧区域：%d#%d 新区域：%d#%d）"
					, oldarea.getAreaLevel(),oldarea.getAreaId(),newarea.getAreaLevel(),newarea.getAreaId()));
			oldarea.removeTimeCount();
			newarea.addTimeCount();
			changeAreaTimeCount(oldarea.getFatherArea(),newarea.getFatherArea());
		}
		else
			Log.i(tag,String.format("新旧区域为同一层的同一个区域，变动相应的区域频率向量计时停止（旧区域：%d#%d 新区域：%d#%d）"
					, oldarea.getAreaLevel(),oldarea.getAreaId(),newarea.getAreaLevel(),newarea.getAreaId()));
	}
}
