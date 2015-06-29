package android.geosvr.dtn.servlib.geohistorydtn.routing;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import android.geosvr.dtn.servlib.bundling.BundleDaemon;
import android.geosvr.dtn.servlib.bundling.ForwardingInfo;
import android.geosvr.dtn.servlib.bundling.event.BundleEvent;
import android.geosvr.dtn.servlib.bundling.event.BundleReceivedEvent;
import android.geosvr.dtn.servlib.bundling.event.ContactDownEvent;
import android.geosvr.dtn.servlib.bundling.event.ContactUpEvent;
import android.geosvr.dtn.servlib.bundling.event.LinkCreatedEvent;
import android.geosvr.dtn.servlib.contacts.Link;
import android.geosvr.dtn.servlib.geohistorydtn.area.Area;
import android.geosvr.dtn.servlib.geohistorydtn.area.AreaInfo;
import android.geosvr.dtn.servlib.geohistorydtn.area.AreaManager;
import android.geosvr.dtn.servlib.geohistorydtn.neighbour.Neighbour;
import android.geosvr.dtn.servlib.geohistorydtn.neighbour.NeighbourManager;
import android.geosvr.dtn.servlib.naming.EndpointID;
import android.geosvr.dtn.servlib.naming.EndpointIDPattern;
import android.geosvr.dtn.servlib.routing.RouteEntry;
import android.geosvr.dtn.servlib.routing.RouteEntryVec;
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
		Log.i(tag,"starts GeoHistoryRouter");
		
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
		
		/**
		 * DTN中的时间，bundle的creation_ts为bundle创建的相对时间，DTNTime.TIMEVAL_CONVERSION就是相对的参数，应该是2000，00：00。
		 * 利用creation_ts和DTNTime.TIMEVAL_CONVERSION即可得到bundle创建时的真实时间
		 * bundle的expirionTime则是指bundle的有效时间
		 */
		
		
		switch(event.source())
		{
		//收到邻居发来的相关信息
		case EVENTSRC_NEIGHBOUR:
			
			break;
			
		default:
			super.handle_bundle_received(event);
		}
		
		
	}

	@Override
	protected void handle_link_created(LinkCreatedEvent event) {
//		super.handle_link_created(event);
		Link link = event.link();
		assert(link != null): "TableBasedRouter: handle_link_created: link is null";
	    assert(!link.isdeleted()): "TableBasedRouter: handle_link_available: link is deleted";

	    link.set_router_info(new DeferredList(link));
	                          
	 // "If we're configured to do so, create a route entry for the eid
 		// specified by the link when it connected, using the
 		// scheme-specific code to transform the URI to wildcard
 		// the service part" [DTN2]
 		EndpointID eid = link.remote_eid();
 		if (config_.add_nexthop_routes() && !eid.equals(EndpointID.NULL_EID())) {
 			EndpointIDPattern eid_pattern = new EndpointIDPattern(link.remote_eid());

 			// "attempt to build a route pattern from link's remote_eid" [DTN2]
 			if (!eid_pattern.append_service_wildcard())
 				// "else assign remote_eid as-is" [DTN2]
 				eid_pattern.assign(link.remote_eid());

 			RouteEntryVec ignored = new RouteEntryVec();
 			if (route_table_.get_matching(eid_pattern, link, ignored) == 0) {
 				RouteEntry entry = new RouteEntry(eid_pattern, link);
 				entry.set_action(ForwardingInfo.action_t.COPY_ACTION);
// 				add_route(entry);//这里面有对所有的bundle进行重新路由的过程
 				route_table_.add_entry(entry);
 			}
 		}
	}
	
	/**
	 * 针对新邻居发现的事件处理
	 */
	@Override
	protected void handle_contact_up(ContactUpEvent event) {
		// TODO Auto-generated method stub
//		super.handle_contact_up(event);//无用
		
		//获取eid
		Link link=event.contact().link();
		EndpointID eid=link.remote_eid();
//		a)	触发对已有邻居信息的保存
		Neighbour nei=NeighbourManager.getInstance().checkNeighbour(eid);
		
//		b)	触发邻居之间交换信息的bundle发送
		
		
//		c)	触发已有bundle队列里面的所有bundle的route事件（会尝试判断是否发送这些bundle）;这个事件应该在收到邻居相应的bundle信息后

	}
	
	@Override
	protected void handle_contact_down(ContactDownEvent event) {
		// TODO Auto-generated method stub
		super.handle_contact_down(event);
		
		//移除该邻居频率的计时器
		Link link=event.contact().link();
		EndpointID eid=link.remote_eid();
		if(eid!=null)
		{
			Neighbour nei=NeighbourManager.getInstance().getNeighbour(eid);
			if(nei!=null)
				nei.removeTimeCount();
		}
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
						newBaseArea.changeFVector(ainfo);
						
						//加入到新的倒数时间列表里面,将原来的从倒数计时里面去掉
						changeAreaTimeCount(baseArea, newBaseArea);
						
						//更改当前的底层区域向量
						baseArea=newBaseArea;
						
						//将移动的记录保存到日志中
						AreaManager.writeAreaLogToFile("move to new area, ",baseArea);
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
		//递归的结束条件
		if(newarea==null)
			return;
		
		//旧区域为空的变更计时器
		if(oldarea==null)
		{
			Log.i(tag,String.format("首次加载新区域的频率香料到计时器中（新区域：%d#%d）"
					,newarea.getAreaLevel(),newarea.getAreaId()));
			newarea.addTimeCount();
			changeAreaTimeCount(null,newarea.getFatherArea());
			
			Log.i(tag,"进入新区域的频率向量:"+newarea.toString());
			return ;
		}
		
		/**
		 * 正常情况的变更计时器
		 */
		if(!oldarea.equals(newarea) && oldarea.getAreaLevel()==newarea.getAreaLevel())
		{
			Log.d(tag,String.format("变动计时器中新旧区域的频率向量（旧区域：%d#%d 新区域：%d#%d）"
					, oldarea.getAreaLevel(),oldarea.getAreaId(),newarea.getAreaLevel(),newarea.getAreaId()));
			oldarea.removeTimeCount();
			newarea.addTimeCount();
			changeAreaTimeCount(oldarea.getFatherArea(),newarea.getFatherArea());
			
			Log.i(tag,"原先区域的频率向量:"+oldarea.toString());
			Log.i(tag,"当前区域的频率向量:"+newarea.toString());
		}
		else
			Log.d(tag,String.format("新旧区域为同一层的同一个区域，变动相应的区域频率向量计时停止（旧区域：%d#%d 新区域：%d#%d）"
					, oldarea.getAreaLevel(),oldarea.getAreaId(),newarea.getAreaLevel(),newarea.getAreaId()));
	}

	private void sendBundle(EndpointID eid,File file)
	{
		
	}
}
