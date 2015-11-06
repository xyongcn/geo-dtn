package android.geosvr.dtn.servlib.geohistorydtn.routing;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import android.geosvr.dtn.DTNService;
import android.geosvr.dtn.applib.DTNAPIBinder;
import android.geosvr.dtn.applib.DTNAPICode.dtn_api_status_report_code;
import android.geosvr.dtn.applib.DTNAPICode.dtn_bundle_payload_location_t;
import android.geosvr.dtn.applib.types.DTNBundleID;
import android.geosvr.dtn.applib.types.DTNBundlePayload;
import android.geosvr.dtn.applib.types.DTNBundleSpec;
import android.geosvr.dtn.applib.types.DTNEndpointID;
import android.geosvr.dtn.applib.types.DTNHandle;
import android.geosvr.dtn.apps.DTNAPIFailException;
import android.geosvr.dtn.apps.DTNOpenFailException;
import android.geosvr.dtn.apps.DTNSend;
import android.geosvr.dtn.servlib.bundling.Bundle;
import android.geosvr.dtn.servlib.bundling.BundleDaemon;
import android.geosvr.dtn.servlib.bundling.ForwardingInfo;
import android.geosvr.dtn.servlib.bundling.BundlePayload.location_t;
import android.geosvr.dtn.servlib.bundling.event.BundleEvent;
import android.geosvr.dtn.servlib.bundling.event.BundleReceivedEvent;
import android.geosvr.dtn.servlib.bundling.event.ContactDownEvent;
import android.geosvr.dtn.servlib.bundling.event.ContactUpEvent;
import android.geosvr.dtn.servlib.bundling.event.LinkCreatedEvent;
import android.geosvr.dtn.servlib.bundling.event.event_source_t;
import android.geosvr.dtn.servlib.contacts.Link;
import android.geosvr.dtn.servlib.contacts.Link.state_t;
import android.geosvr.dtn.servlib.geohistorydtn.area.Area;
import android.geosvr.dtn.servlib.geohistorydtn.area.AreaInfo;
import android.geosvr.dtn.servlib.geohistorydtn.area.AreaLevel;
import android.geosvr.dtn.servlib.geohistorydtn.area.AreaManager;
import android.geosvr.dtn.servlib.geohistorydtn.areaConnectiveSimulation.CurrentLocationFromSimulator;
import android.geosvr.dtn.servlib.geohistorydtn.areaConnectiveSimulation.questAreaInfo.AreaLayerInfo;
import android.geosvr.dtn.servlib.geohistorydtn.config.BundleConfig;
import android.geosvr.dtn.servlib.geohistorydtn.log.GeohistoryLog;
import android.geosvr.dtn.servlib.geohistorydtn.neighbour.Neighbour;
import android.geosvr.dtn.servlib.geohistorydtn.neighbour.NeighbourArea;
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
		 * bundle之间
		 */
		if(event.bundle().getBundleType()==Bundle.NEI_AREA_BUNDLE)
		{
			//自己想邻居发送的交换历史区域的bundle
			if(event.bundle().source().toString().equals(BundleDaemon.localEid()))
			{
				GeohistoryLog.i(tag,String.format("本节点往%s发送自己的区域移动信息",event.bundle().dest().toString()));
				super.handle_bundle_received(event);
			}
			//保证该邻居交换历史区域的bundle是发给自己的
			else if(event.bundle().dest().toString().equals(BundleDaemon.localEid()))
			{
				GeohistoryLog.i(tag,String.format("收到来自%s的区域移动信息",event.bundle().source().toString()));
				Bundle b=event.bundle();
//				Neighbour nei=NeighbourManager.getInstance().getNeighbour(b.source());//用这个方式可能得到的是没有被创建的邻居
				Neighbour nei=NeighbourManager.getInstance().checkNeighbour(b.source());
				nei.generateArea(b.payload());//用收到的邻居的bundle来
				
				//向该邻居发送队列里面需要发送的bundle（邻居间交换bundle的路由算法）
				//只需要将队列中的bundle向该link重新发送即可
				//reroute();
			}
			else{
				GeohistoryLog.i(tag,String.format("既不是自己发出的，也不是发给自己的邻居信息，源：%s,目的：%s"
						,event.bundle().source().toString(),event.bundle().dest().toString()));
			}
			
		}
		//对其他类型bundle进行
		else if(event.bundle().getBundleType()==Bundle.DATA_BUNDLE)
		{
			super.handle_bundle_received(event);
		}
		else
		{
			GeohistoryLog.e(tag, String.format("bundle_%d 的类型不明确，type：%d", event.bundle().bundleid(),event.bundle().getBundleType()));
		}
		
		/*switch(event.source())
		{
		//收到邻居发来的相关信息
		case EVENTSRC_NEIGHBOUR:
			
			break;
			
		default:
			super.handle_bundle_received(event);
		}*/
		
		
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
//		super.handle_contact_up(event);//无用
		
		GeohistoryLog.i(tag, String.format("geohistory router, handle contact up"));
		
		//获取eid
		Link link=event.contact().link();
		EndpointID eid=link.remote_eid();
//		a)	触发对已有邻居信息的保存
		Neighbour nei=NeighbourManager.getInstance().checkNeighbour(eid);
		nei.addTimeCount();//添加当前邻居的计时器
		
//		b)	触发邻居之间交换信息的bundle发送,发送本节点的历史区域信息
		/*File file=new File("/sdcard/geoHistory_dtn/historyarea");
		try {
			sendMessage(nei.getEid().toString(), file, false, null,Bundle.NEI_AREA_BUNDLE);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			GeohistoryLog.e(tag, String.format("向邻居%s发送自己的区域信息的bundle出错:%s", nei.getEid().toString(),"UnsupportedEncodingException"));
		} catch (DTNOpenFailException e) {
			e.printStackTrace();
			GeohistoryLog.e(tag, String.format("向邻居%s发送自己的区域信息的bundle出错:%s", nei.getEid().toString(),"DTNOpenFailException"));
		} catch (DTNAPIFailException e) {
			e.printStackTrace();
			GeohistoryLog.e(tag, String.format("向邻居%s发送自己的区域信息的bundle出错:%s", nei.getEid().toString(),"DTNAPIFailException"));
		}
		*/
		
		/**
			 * 1.判断是否需要发送本节点的邻居，
			 */

	}
	
	@Override
	protected void handle_contact_down(ContactDownEvent event) {

		GeohistoryLog.i(tag, String.format("geohistory router, handle contact up"));
		
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

	@Override
	protected int route_bundle(Bundle bundle) 
	//对bundle进行route
	{
		RouteEntryVec matches = new RouteEntryVec();

		GeohistoryLog.d(tag, String.format("route_bundle: checking bundle %d", bundle
				.bundleid()));

		// "check to see if forwarding is suppressed to all nodes" [DTN2]
		if (bundle.fwdlog().get_count(EndpointIDPattern.WILDCARD_EID(),
				ForwardingInfo.state_t.SUPPRESSED.getCode(),
				ForwardingInfo.ANY_ACTION) > 0) {
			GeohistoryLog.i(tag, String.format("route_bundle: "
					+ "ignoring bundle %d since forwarding is suppressed",
					bundle.bundleid()));
			return 0;
		}

		//判断 bundle的关于区域的信息是否合法，GeoHistory相关信息不合法时退出发送
		if(!bundle.isGeoHistoryDtnValide())
		{
			GeohistoryLog.e(tag,String.format("bundle_%d 的geohistory相关信息不合法；区域(0-3)：%d.%d.%d.%d",
					bundle.bundleid(),bundle.zeroArea(),bundle.firstArea(),bundle.secondArea(),bundle.thirdArea()));
			return 0;
		}
		
		//如果是邻居间传递各自区域信息的bundle
//		if()
		
		//查找合适的需要转发的bundle
		Area bundleArea=new Area(AreaLevel.FIRSTLEVEL, bundle.firstArea());
		//本节点到达目的地区域
		if(baseArea.equals(bundleArea))
		{
			if(bundle.isFlooding()==0)
			{
				bundle.setIsFlooding(1);
			}
			GeohistoryLog.i(tag, String.format("进入目标区域，执行洪泛转发。bundle_%d,区域(0-3)：%d.%d.%d.%d",
					bundle.bundleid(),bundle.zeroArea(),bundle.firstArea(),bundle.secondArea(),bundle.thirdArea()));
			

			//进行洪泛转发
			get_matching_RouteEntryVec(bundle, matches, -1, null);
			
			sort_routes(matches);

			GeohistoryLog.d(tag, String.format(
					"route_bundle bundle id %d: checking %d route entry matches",
					bundle.bundleid(), matches.size()));

			int count = 0;
			Iterator<RouteEntry> itr = matches.iterator();
			while (itr.hasNext()) 
			{
				RouteEntry route = itr.next();
				GeohistoryLog.d(tag, String.format("checking route entry %s link %s (%s)",
						route.toString(), route.link().name(), route.link()));

				if (!should_fwd(bundle, route)) {
					continue;
				}
				
				//Spray and Wait转发过程中，副本数为1则只对目标节点发送，其余情况下副本对半发送
				if(bundle.floodBundleNum()<=1)
				{
					//当flood的副本数量只有1的时候，只向目的节点发送bundle
					if(!route.dest_pattern().match(bundle.dest()))
						return 0;
				}
				else
				{
					Bundle forwardbundle=null;
					try {
						forwardbundle=(Bundle) bundle.clone();
					} catch (CloneNotSupportedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						GeohistoryLog.d(tag, String.format("clone bundle_%d failed ", bundle.bundleid()));
						return 0;
					}
					
					int bundleCopyNum=bundle.floodBundleNum()/2;
					bundle.setFloodBundleNum(bundle.floodBundleNum()-bundleCopyNum);
					forwardbundle.setFloodBundleNum(bundleCopyNum);
					bundle=forwardbundle;//将bundle的引用改为修改了副本数的bundle引用
				}

				
				if (deferred_list(route.link()).list().contains(bundle)) {
					GeohistoryLog.d(tag, String.format("route_bundle bundle %d: "
							+ "ignoring link %s since already deferred", bundle
							.bundleid(), route.link().name()));
					continue;
				}

				// "because there may be bundles that already have deferred
				// transmission on the link, we first call check_next_hop to
				// get them into the queue before trying to route the new
				// arrival, otherwise it might leapfrog the other deferred
				// bundles" [DTN2]
				check_next_hop(route.link());

				if (!fwd_to_nexthop(bundle, route)) {
					continue;
				}

				++count;
			}

			GeohistoryLog.d(tag, String.format(
					"route_bundle bundle id %d: forwarded on %d links", bundle
							.bundleid(), count));
			return count;
		}
		else
		{
			//本节点进入目的区域后又离开了,这里不进行任何转发
			if(bundle.isFlooding()==1)
			{
				GeohistoryLog.i(tag, String.format("进入目标区域又离开了，不进行任何转发。bundle_%d,目的区域(0-3)：%d.%d.%d.%d",
						bundle.bundleid(),bundle.zeroArea(),bundle.firstArea(),bundle.secondArea(),bundle.thirdArea()));
				//判断是否超时
				//先不管超时，bytewalla应该自己管理好了超时
//				if(bundle.creation_ts().seconds()+bundle.expiration_timer())
				return 0;
			}
			//本节点没有进入目的区域，执行普通转发
			else
			{
				GeohistoryLog.i(tag, String.format("没有进入目标区域，执行普通转发。bundle_%d,目的区域(0-3)：%d.%d.%d.%d",
						bundle.bundleid(),bundle.zeroArea(),bundle.firstArea(),bundle.secondArea(),bundle.thirdArea()));
				
				//普通转发
				//节点Area与link的表
				HashMap<Area,RouteEntry> linkAreaMap=new HashMap<Area,RouteEntry>(route_table_.size());
				//本节点离目的地最近的区域
				Area thisnodeArea=baseArea.checkBundleDestArea(bundle);
				if(thisnodeArea==null)
				{
					GeohistoryLog.i(tag, String.format("执行普通转发时，本节点没有找到与目标节点处在同一层的区域。bundle_%d,目的区域(0-3)：%d.%d.%d.%d",
							bundle.bundleid(),bundle.zeroArea(),bundle.firstArea(),bundle.secondArea(),bundle.thirdArea()));
					return 0;
				}
				//获取符合要求的当前邻居节点
				get_matching_RouteEntryVec(bundle, matches, thisnodeArea.getAreaLevel(), linkAreaMap);
				
				if(linkAreaMap.isEmpty())
				{
					GeohistoryLog.i(tag, String.format("没有找到合适的link"));
					return 0;
				}
				
				Iterator<Area> iterator=linkAreaMap.keySet().iterator();
				List<Area>	avaliableNodeList=new ArrayList<Area>();
				while(iterator.hasNext())
				{
					Area temp=iterator.next();
					avaliableNodeList.add(temp);
				}
				avaliableNodeList.add(thisnodeArea);
				//在当前邻居和历史邻居中，获取比本节点更好的的节点
				avaliableNodeList=ChanceValueSort.getAllAvaliableNodeArea(avaliableNodeList, bundle, thisnodeArea);
				
				//寻找合适的link转发
				int no=1;//标志着该节点的排名
				int count = 0;//原路由算法携带，返回向邻居发送bundle的个数
				for(Area a:avaliableNodeList)
				{
					RouteEntry route=linkAreaMap.get(a);
					if(route!=null)
					{
						GeohistoryLog.d(tag, String.format("checking route entry %s link %s (%s)",
								route.toString(), route.link().name(), route.link()));

						if (!should_fwd(bundle, route)) {
							continue;
						}

						//对bundle副本数目的修改
						//当bundle副本数只有1的时候，只发给目的节点
						if(bundle.deliverBundleNum()<=1)
						{
							if(!route.dest_pattern().match(bundle.dest()))
								return 0;
						}
						else
						{
							Bundle forwardbundle=null;
							try {
								forwardbundle=(Bundle) bundle.clone();
							} catch (CloneNotSupportedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								GeohistoryLog.e(tag, "bundle.clone出现错误");
								return 0;
							}
							
							if(forwardbundle==null)
							{
								GeohistoryLog.e(tag, "bundle.clone出现错误");
								return 0;
							}
							
							//该节点排名小于副本数量，执行2分法发送
							if(no<=forwardbundle.deliverBundleNum())
							{
								int copynum=forwardbundle.deliverBundleNum()/2;
								bundle.setDeliverBundleNum(bundle.deliverBundleNum()-copynum);
								forwardbundle.setDeliverBundleNum(copynum);
								bundle=forwardbundle;
							}
							//该节点排名大于副本数量，只发送1份
							else
							{
								bundle.setDeliverBundleNum(bundle.deliverBundleNum()-1);
								forwardbundle.setDeliverBundleNum(1);
								bundle=forwardbundle;
							}
						}
						
						
						if (deferred_list(route.link()).list().contains(bundle)) {
							GeohistoryLog.d(tag, String.format("route_bundle bundle %d: "
									+ "ignoring link %s since already deferred", bundle
									.bundleid(), route.link().name()));
							continue;
						}

						// "because there may be bundles that already have deferred
						// transmission on the link, we first call check_next_hop to
						// get them into the queue before trying to route the new
						// arrival, otherwise it might leapfrog the other deferred
						// bundles" [DTN2]
						check_next_hop(route.link());

						if (!fwd_to_nexthop(bundle, route)) {
							continue;
						}

						++count;
					}
					
					//排名+1
					++no;
				}
				
				GeohistoryLog.d(tag, String.format(
						"route_bundle bundle id %d: forwarded on %d links", bundle
								.bundleid(), count));
				return count;
				
				
			}
		}
/*
		GeohistoryLog.d(tag, String.format(
				"route_bundle bundle id %d: checking %d route entry matches",
				bundle.bundleid(), matches.size()));

		int count = 0;
		Iterator<RouteEntry> itr = matches.iterator();
		while (itr.hasNext()) {
			RouteEntry route = itr.next();
			GeohistoryLog.d(tag, String.format("checking route entry %s link %s (%s)",
					route.toString(), route.link().name(), route.link()));

			if (!should_fwd(bundle, route)) {
				continue;
			}

			if (deferred_list(route.link()).list().contains(bundle)) {
				GeohistoryLog.d(tag, String.format("route_bundle bundle %d: "
						+ "ignoring link %s since already deferred", bundle
						.bundleid(), route.link().name()));
				continue;
			}

			// "because there may be bundles that already have deferred
			// transmission on the link, we first call check_next_hop to
			// get them into the queue before trying to route the new
			// arrival, otherwise it might leapfrog the other deferred
			// bundles" [DTN2]
			check_next_hop(route.link());

			if (!fwd_to_nexthop(bundle, route)) {
				continue;
			}

			++count;
		}

		GeohistoryLog.d(tag, String.format(
				"route_bundle bundle id %d: forwarded on %d links", bundle
						.bundleid(), count));
		return count;*/
	}
	
	
	/**
	 * 作用： 当sameAreaLevel为-1时，执行的洪泛转发所需要的link表；当sameAreaLevel为合法值时，将返回与目标尽可能接近目的地的邻居的link表。
	 * @param bundle ：需要转发的bundle
	 * @param entry_vec :需要返回的link表
	 * @param sameAreaLevel ：本节点与目标所在的同一区域层次。如果为-1，则表示是执行洪泛的方法；如果是合法的值，则是查找尽可能接近邻居的link
     * @return the number of matching
	 */
	private int get_matching_RouteEntryVec( Bundle bundle,RouteEntryVec entry_vec,int sameAreaLevel,HashMap<Area,RouteEntry> sameLevelAreaMap) 
	{
		route_table_.lock().lock();
		
		//尽可能底层的LEVEL，AreaLevel
		int areaLevel=sameAreaLevel;
		
		try
		{
			int count = 0;
			
			Iterator<RouteEntry> iter = route_table_.route_table().iterator();
			while (iter.hasNext())
			{
				RouteEntry entry = iter.next();
				
				GeohistoryLog.d(tag, String.format("check entry %s", entry.toString()));
				
				//如果为本节点将跳过
				if (entry.dest_pattern().match(BundleDaemon.getInstance().local_eid()))
					continue;
				
				//如果link不该被发送，则
				/*if (!should_fwd(bundle, entry)) {
					continue;
				}*/
				
				//如果是普通转发，获取尽可能接近目的区域的邻居的link
				if(sameAreaLevel!=-1)
				{
//					Neighbour neighbour=NeighbourManager.getInstance().getNeighbour(bundle.dest());//这里不应该是找到历史邻居中的目的节点，而是当前节点的历史邻居位置
					Neighbour neighbour=NeighbourManager.getInstance().getNeighbour(entry.dest_pattern());
					
					if(neighbour==null)
						continue;
					
					NeighbourArea neiArea=neighbour.getNeighbourArea();
					if(neiArea==null)
						continue;
					
					Area area=neiArea.checkBundleDestArea(bundle);
					if(area==null)
						continue;
					
					//本节点离目标节点更接近，因此否决该link
					if(area.getAreaLevel()>areaLevel)
					{
						continue;
					}
					//该邻居比本节点更接近目的地，调整区域等级和已有的link表
					else if(area.getAreaLevel()<areaLevel)
					{
						entry_vec.clear();
						sameLevelAreaMap.clear();
						areaLevel=area.getAreaLevel();
					}
					//该邻居与本节点一样接近目的地，默认操作
					else {}
					
					//将尽可能接近目的地的区域邻居区域加入到
//					sameLevelAreaList.;
					
					//原有算法，将link加到路由项中
					if(!entry_vec.contains(entry) && 
							(entry.link().state() == state_t.OPEN || 
							entry.link().state() == state_t.AVAILABLE ))
					{
						GeohistoryLog.d(tag,String.format("match entry %s", entry.toString() ));
						entry_vec.add(entry);
						++count;
						
						//加入到Map里面
						sameLevelAreaMap.put(area,entry);
					}
					else
					{
						GeohistoryLog.d(tag,String.format("entry %s already in matches... ignoring", entry.toString() ));
					}
				}
				
				
					
			
			}
			
			
			GeohistoryLog.d(tag, String.format("get_matching %s done, %d match(es)",
				bundle.dest().toString().toString(), count));
			
			return count;
		}
		finally 
		{
			route_table_.lock().unlock();
		}
	}
	
	/*private boolean route_to_thisLink(Bundle bundle,Nei)
	{
		return true;
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
					//调用AreaManager，判断是否为新区域；如果区域发生了变动，修改新出现的区域的频率向量
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
			Log.d(tag,String.format("首次加载新区域的频率香料到计时器中（新区域：%d#%d）"
					,newarea.getAreaLevel(),newarea.getAreaId()));
			newarea.addTimeCount();
			changeAreaTimeCount(null,newarea.getFatherArea());
			
			Log.d(tag,"进入新区域的频率向量:"+newarea.toString());
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
			
			Log.d(tag,"原先区域的频率向量:"+oldarea.toString());
			Log.d(tag,"当前区域的频率向量:"+newarea.toString());
		}
		else
			Log.d(tag,String.format("新旧区域为同一层的同一个区域，变动相应的区域频率向量计时停止（旧区域：%d#%d 新区域：%d#%d）"
					, oldarea.getAreaLevel(),oldarea.getAreaId(),newarea.getAreaLevel(),newarea.getAreaId()));
	}

	/*public Area getNowArea()
	{
		return baseArea;
	}*/
	
	/*private void sendBundle(EndpointID eid,File file)
	{
		
	}*/
	
	/**
	 * GeoHistoryDtn发送数据的
	 * @param dest_eid
	 * @param file
	 * @param rctp
	 * @param eventSource :标明该bundle是哪种类型，是APPbundle还是邻居间交换信息的bundle
	 * @param areaid :用来表示目的节点的区域信息
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws DTNOpenFailException
	 * @throws DTNAPIFailException
	 */
	public boolean sendMessage(String dest_eid,File file,boolean rctp,int[] areaid,int bundleType) throws UnsupportedEncodingException, DTNOpenFailException, DTNAPIFailException
	{
		if(!DTNService.is_running())
			return false;
		
		//判断bundle的类型
		if(bundleType==Bundle.DATA_BUNDLE)
		{
			if(areaid==null || areaid.length!=4)
			{
				GeohistoryLog.e(tag, "DTN应用发送bundle时，目的节点的各层次区域信息不明确");
				return false;
			}
		}
		else if(bundleType!=Bundle.NEI_AREA_BUNDLE)
		{
			GeohistoryLog.e(tag, "DTN发送bundle时，event_source不明确，既不是应用的bundle也不是邻居的bundle");
			return false;
		}
		
		DTNAPIBinder dtn_api_binder_=DTNService.getDTNAPIBinder();
		
		double dest_longitude = -1.0;
		double dest_latitude = -1.0;
		
		DTNBundlePayload dtn_payload = new DTNBundlePayload(dtn_bundle_payload_location_t.DTN_PAYLOAD_FILE);
		
		if(file==null || !file.exists())
			return false;
		else
			dtn_payload.set_file(file);//用指定的文件进行发送
//				dtn_payload.set_file(new File("/sdcard/test_0.5M.mp3"));
		   
		// Start the DTN Communication
		DTNHandle dtn_handle = new DTNHandle();
		dtn_api_status_report_code open_status = dtn_api_binder_.dtn_open(dtn_handle);
		if (open_status != dtn_api_status_report_code.DTN_SUCCESS) throw new DTNOpenFailException();
		try
		{
			DTNBundleSpec spec = new DTNBundleSpec();
			
			// set destination from the user input
			spec.set_dest(new DTNEndpointID(dest_eid));
			spec.setDestLongitude(dest_longitude);
			spec.setDestLatitude(dest_latitude);
			// set the source EID from the bundle Daemon
			spec.set_source(new DTNEndpointID(BundleDaemon.getInstance().local_eid().toString()));
				
			// Set expiration in seconds, default to 1 hour
			spec.set_expiration(DTNSend.EXPIRATION_TIME);
			// no option processing for now
			if(rctp)//rctp为true表示执行的是带回复的bundle
				spec.set_dopts(2);
			else
				spec.set_dopts(DTNSend.DELIVERY_OPTIONS);
			// Set prority
			spec.set_priority(DTNSend.PRIORITY);
			
			dtn_api_status_report_code api_send_result ;

//			api_send_result = dtn_api_binder_.dtn_multiple_send(dtn_handle, spec, dtn_payload, 1);
			
			int count=1;//bundle发送的副本数
			if (!dtn_api_binder_.is_handle_valid(dtn_handle))
				api_send_result=dtn_api_status_report_code.DTN_EHANDLE_INVALID;
			DTNBundleID[] dtn_bundle_id = new DTNBundleID[count];
			Bundle[] b = new Bundle[count];
			for (int i=0; i<count; i++) {
				dtn_bundle_id[i] = new DTNBundleID();
				b[i] = new Bundle(location_t.DISK);
				b[i] = dtn_api_binder_.dtn_send_multiple_final(dtn_handle, spec, dtn_payload, dtn_bundle_id[i], b[i]);
				
				//检测添加数据
				/* private long timestamp;
		    	private long invalidtime;//bundle的失效时间
		    	private int zeroArea;//0层区域，最底层区域，也是可达层区域
		    	private int firstArea;//1层区域，区域数字有小到大，依次范围扩大
		    	private int secondArea;//2层区域
		    	private int thirdArea;//3层区域
		    	
		    	private int deliverBundleNum;//传递阶段的bundle数量
		    	private int floodBundleNum;//洪泛扩散阶段bundle的数量
		    	private int isFlooding;//是否进入过了flood阶段
		    	
		    	int bundleType=DATA_BUNDLE;//判断bundle的类型，属于邻居间交换区域信息的bundle，或者是数据bundle
		*/        
				
//				b[i].setZeroArea(33);
//				b[i].setFirstArea(44);
//				b[i].setSecondArea(55);
//				b[i].setThirdArea(66);
//				b[i].setDeliverBundleNum(77);
//				b[i].setFloodBundleNum(88);
//				b[i].setIsFlooding(99);
				//设置目的节点区域信息
				if(bundleType==Bundle.DATA_BUNDLE)
				{
					b[i].setZeroArea(areaid[0]);
					b[i].setFirstArea(areaid[1]);
					b[i].setSecondArea(areaid[2]);
					b[i].setThirdArea(areaid[3]);
				}
				//设置bundle的副本数目
				b[i].setDeliverBundleNum(BundleConfig.DELIVERBUNDLENUM);
				b[i].setFloodBundleNum(BundleConfig.FLOODBUNDLENUM);
				b[i].setIsFlooding(0);
				//设置bundle的类型
				b[i].setBundleType(bundleType);
			}
			
			for (int i = 0; i < count; i++) {
				//向邻居发送区域交换信息的bundle
//				BundleDaemon.getInstance().post(
//						new BundleReceivedEvent(b[i], event_source_t.EVENTSRC_NEIGHBOUR));
				BundleDaemon.getInstance().post(
						new BundleReceivedEvent(b[i], event_source_t.EVENTSRC_APP));
			}
			api_send_result=dtn_api_status_report_code.DTN_SUCCESS;
			
			// If the API fail to execute throw the exception so user interface can catch and notify users
			if (api_send_result != dtn_api_status_report_code.DTN_SUCCESS) {
				throw new DTNAPIFailException();
			}
		
		}
		finally
		{
			dtn_api_binder_.dtn_close(dtn_handle);
		}
		
		return true;
	}
	
}
