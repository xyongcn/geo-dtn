package android.geosvr.dtn.servlib.geohistorydtn.routing;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
import android.geosvr.dtn.servlib.bundling.BundleInfoCache;
import android.geosvr.dtn.servlib.bundling.BundleProtocol;
import android.geosvr.dtn.servlib.bundling.BundlePayload.location_t;
import android.geosvr.dtn.servlib.bundling.ForwardingInfo;
import android.geosvr.dtn.servlib.bundling.event.BundleDeleteRequest;
import android.geosvr.dtn.servlib.bundling.event.BundleDeliveredEvent;
import android.geosvr.dtn.servlib.bundling.event.BundleEvent;
import android.geosvr.dtn.servlib.bundling.event.BundleReceivedEvent;
import android.geosvr.dtn.servlib.bundling.event.ContactDownEvent;
import android.geosvr.dtn.servlib.bundling.event.ContactUpEvent;
import android.geosvr.dtn.servlib.bundling.event.LinkCreatedEvent;
import android.geosvr.dtn.servlib.bundling.event.event_source_t;
import android.geosvr.dtn.servlib.bundling.exception.BundleListLockNotHoldByCurrentThread;
import android.geosvr.dtn.servlib.contacts.Link;
import android.geosvr.dtn.servlib.contacts.Link.link_type_t;
import android.geosvr.dtn.servlib.contacts.Link.state_t;
import android.geosvr.dtn.servlib.geohistorydtn.area.Area;
import android.geosvr.dtn.servlib.geohistorydtn.area.AreaInfo;
import android.geosvr.dtn.servlib.geohistorydtn.area.AreaLevel;
import android.geosvr.dtn.servlib.geohistorydtn.area.AreaManager;
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
import android.geosvr.dtn.servlib.storage.BundleStore;
import android.util.Log;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-5-26 下午1:31:44 
 * 说明 
 */
public class GeoHistoryRouter extends TableBasedRouter implements Runnable
{
	static String tag="GeoHistoryRouter";
	static String test="routerTest";
	
	RouteAllBundleMsg routeAllBundle=new RouteAllBundleMsg();
	
	/**
	 *  "Cache to check for duplicates and to implement a simple RPF check" [DTN2]
	 *  用来检测是不是一个重复的dtn bundle
	 */
	private BundleInfoCache reception_cache_;
	
	/**
	 * 用来记录该bundle的副本数目,其实是第一阶段的bundle副本数量
	 */
	private HashMap<String,Integer> Forward1PayloadNumMap;
	/**
	 * 用来记录spray-and-wait过程的bundle副本数目
	 */
	private HashMap<String,Integer>	Forward2PayloadNumMap;
	
	public GeoHistoryRouter() {
		
		super();
//		areaInfoList=new LinkedBlockingDeque<Object>();
		messagequeue=new LinkedBlockingDeque<Object>();
		reception_cache_ = new BundleInfoCache(1024);
		Forward1PayloadNumMap=new HashMap<String, Integer>(1024);
		Forward2PayloadNumMap=new HashMap<String, Integer>(1024);
		init();
	}
	
	private void init(){
		Log.i(tag,"starts GeoHistoryRouter");
		
		(new Thread(this)).start();
		
		(new Thread(new waitOrder())).start();//启动接受命令行发送数据的命令
	}
	
	
	
	/*@Override
	public void handle_event(BundleEvent event) {
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
		GeohistoryLog.i(tag, "GeohistoryRouter 收到一个 BundleReceivedEvent");
		
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
			/*else if(event.bundle().dest().toString().equals(BundleDaemon.localEid()))
			{
				GeohistoryLog.i(tag,String.format("收到来自%s的区域移动信息",event.bundle().source().toString()));
				Bundle b=event.bundle();
//				Neighbour nei=NeighbourManager.getInstance().getNeighbour(b.source());//用这个方式可能得到的是没有被创建的邻居
				Neighbour nei=NeighbourManager.getInstance().checkNeighbour(b.source());
				nei.generateArea(b.payload());//用收到的邻居的bundle来
				
				//向该邻居发送队列里面需要发送的bundle（邻居间交换bundle的路由算法）
				//只需要将队列中的bundle向该link重新发送即可
				if(messagequeue.contains(routeAllBundle)){
					messagequeue.remove(routeAllBundle);
				}
//				messagequeue.add(routeAllBundle);
				//由于已经获取到该邻居的区域移动信息，可以对其进行相应的转发（这里最好重写一下reroute函数，对其中的）
			}*/
			else{
				GeohistoryLog.i(tag,String.format("既不是自己发出的，也不是发给自己的邻居信息，源：%s,目的：%s"
						,event.bundle().source().toString(),event.bundle().dest().toString()));
			}
			
		}
		//对其他类型bundle进行
		else if(event.bundle().getBundleType()==Bundle.DATA_BUNDLE)
		{
			Bundle bundle = event.bundle();
			EndpointID remote_eid = EndpointID.NULL_EID();
			GeohistoryLog.i(test, String.format("收到要发送的DataBundle,des_eid:%s",event.bundle().dest().toString()));
			if (event.link() != null) {
				remote_eid = event.link().remote_eid();
			}

			if (!reception_cache_.add_entry(bundle, remote_eid)) {
				Log.i(test, String.format("ignoring duplicate bundle: bundle %d", bundle.bundleid()));
				BundleDaemon.getInstance().post_at_head(new BundleDeleteRequest(bundle,BundleProtocol.status_report_reason_t.REASON_NO_ADDTL_INFO));
				return;
			}
//			super.handle_bundle_received(event);
			route_bundle(bundle);
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
	protected void handle_bundle_delivered(BundleDeliveredEvent event) {
		Log.i(tag, String.format("handle_bundle_delivered ,dst(%s),ifisNeigh(%b)",
				event.bundle().dest().toString(),event.bundle().getBundleType()==Bundle.NEI_AREA_BUNDLE));
		Log.i(test, String.format("handle_bundle_delivered ,dst(%s),source(%s),ifisNeigh(%b)",
				event.bundle().dest().toString(),event.bundle().source().toString(),event.bundle().getBundleType()==Bundle.NEI_AREA_BUNDLE));
		
		if(event.bundle().getBundleType()==Bundle.NEI_AREA_BUNDLE){
			
			//在bundle还没有被free之前就保存到文件
			NeighbourManager.getInstance().saveNeighbourAreaPayload(event.bundle().payload(), event.bundle().source().toString());
			
			SaveNeighbourArea saveNeighbourArea=new SaveNeighbourArea(event.bundle().source());
			messagequeue.add(saveNeighbourArea);
			
			
			/*GeohistoryLog.i(tag,String.format("收到来自%s的区域移动信息",event.bundle().source().toString()));
			Bundle b=event.bundle();
//			Neighbour nei=NeighbourManager.getInstance().getNeighbour(b.source());//用这个方式可能得到的是没有被创建的邻居
			Neighbour nei=NeighbourManager.getInstance().checkNeighbour(b.source());
			nei.generateArea(b.payload());//用收到的邻居的bundle来
			
			//向该邻居发送队列里面需要发送的bundle（邻居间交换bundle的路由算法）
			//只需要将队列中的bundle向该link重新发送即可
			if(messagequeue.contains(routeAllBundle)){
				messagequeue.remove(routeAllBundle);
			}*/
//			messagequeue.add(routeAllBundle);
			//由于已经获取到该邻居的区域移动信息，可以对其进行相应的转发（这里最好重写一下reroute函数，对其中的）
		}
		super.handle_bundle_delivered(event);
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
		
		GeohistoryLog.i(test, String.format("geohistory router, handle contact up,dst(%s)",eid.toString()));
		
//		a)	触发对已有邻居信息的保存
		Neighbour nei=NeighbourManager.getInstance().checkNeighbour(eid);
		nei.addTimeCount();//添加当前邻居的计时器
		
//		b)	触发邻居之间交换信息的bundle发送,发送本节点的历史区域信息
		File file=new File(AreaManager.historyAreaFilePath);
		SendBundleMsg sendbundle=new SendBundleMsg(nei.getEid().toString(), file, false, AreaInfo.defaultAreaId(), Bundle.NEI_AREA_BUNDLE);
		messagequeue.add(sendbundle);
		/*try {
			GeohistoryLog.i(tag, String.format("准备向邻居%s发送了自己的区域信息的bundle", nei.getEid().toString()));
			sendMessage(nei.getEid().toString(), file, false, AreaInfo.defaultAreaId(),Bundle.NEI_AREA_BUNDLE);
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
		GeohistoryLog.e(tag, String.format("向邻居%s发送了自己的区域信息的bundle成功", nei.getEid().toString()));
		*/
		/**
		 * 1.判断是否需要发送本节点的邻居，
		 */

	}
	
	@Override
	protected void handle_contact_down(ContactDownEvent event) {

		GeohistoryLog.i(tag, String.format("geohistory router, handle contact down"));
		
		GeohistoryLog.i(test, String.format("geohistory router, handle contact down,dst(%s)",event.contact().link().remote_eid().toString()));
		
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
	 * 重写父类，对bundle进行route
	 */
	@Override
	protected int route_bundle(Bundle bundle) {
		
		//如果是对指定邻居发送区域的频率信息的bundle，在这里处理即可
		if(bundle.getBundleType()==Bundle.NEI_AREA_BUNDLE){
			return route_neighbourArea_bundle(bundle);
		}
		
		GeohistoryLog.i(tag,String.format("route_bundle:send data bundle,bundle dst(%s),bundleid(%d),areaId(%d,%d,%d,%d) ",
				bundle.dest().toString(),bundle.bundleid(),bundle.zeroArea(),bundle.firstArea(),bundle.secondArea(),bundle.thirdArea()));
		
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
			GeohistoryLog.e(test,String.format("bundle_%d 的geohistory相关信息不合法；区域(0-3)：%d.%d.%d.%d",
					bundle.bundleid(),bundle.zeroArea(),bundle.firstArea(),bundle.secondArea(),bundle.thirdArea()));
			return 0;
		}
		
		//判断是否遇到了目标节点，是否可以直接发送给目标节点
		if(canDirectDelivery(bundle)){
			return 1;
		}
		
		//查找合适的需要转发的bundle
		Area bundleArea=new Area(AreaLevel.FIRSTLEVEL, bundle.firstArea());
		//本节点到达目的地区域
		if(baseArea.equals(bundleArea))
		{
			if(bundle.isFlooding()==0)
			{
				bundle.setIsFlooding(1);
			}
			GeohistoryLog.i(test, String.format("进入目标区域，执行洪泛转发:dst(%s),copyNum(%d),bundle_%d,区域(0-3)：%d.%d.%d.%d",
					bundle.dest().toString(),bundle.floodBundleNum(),bundle.bundleid(),bundle.zeroArea(),bundle.firstArea(),bundle.secondArea(),bundle.thirdArea()));
			

			//进行洪泛转发
			get_matching_RouteEntryVec(bundle, matches, -1, null);
			
			sort_routes(matches);

			GeohistoryLog.d(test, String.format(
					"route_bundle bundle id %d: checking %d route entry matches",
					bundle.bundleid(), matches.size()));

			int count = 0;
			Iterator<RouteEntry> itr = matches.iterator();
			while (itr.hasNext()) 
			{
				RouteEntry route = itr.next();
				GeohistoryLog.d(test, String.format("checking route entry %s link %s (%s)",
						route.toString(), route.link().name(), route.link()));

				if (!should_fwd(bundle, route)) {
					continue;
				}
				
				
				/*Bundle forwardbundle=null;
				try {
					forwardbundle=(Bundle) bundle.clone();
					forwardbundle.set_bundleid(BundleStore.getInstance().next_id());//重命名Bundle id，防止transmitted之后删除
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
					GeohistoryLog.d(test, String.format("clone bundle_%d failed ", bundle.bundleid()));
					return 0;
				}
				
				int bundleCopyNum=bundle.floodBundleNum()/2;
				bundle.setFloodBundleNum(bundle.floodBundleNum()-bundleCopyNum);
				forwardbundle.setFloodBundleNum(bundleCopyNum);
				bundle=forwardbundle;//将bundle的引用改为修改了副本数的bundle引用
				*/
				
				//更改bundle的副本数量
				String bundle_id=StringOfBundle(bundle);
				int num=bundle.floodBundleNum();
				if(Forward2PayloadNumMap.containsKey(bundle_id)){
					num=Forward2PayloadNumMap.get(bundle_id)/2;
					
//						Forward2PayloadNumMap.put(bundle_id, num);
				}
				
				if(num<1){
					GeohistoryLog.w(test, String.format("bundle(to:%s) don't have flood payload num(%d)",bundle.dest().toString(),num));
					continue;
				}
				
				bundle.setFloodBundleNum(num);
				Forward2PayloadNumMap.put(bundle_id, num);
			

				
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
				
				GeohistoryLog.i(test, String.format("route_bundle,fwd_to_nexthop,洪泛转发完成，发送给  %s,copyNum(%d)"
						,route.link().remote_eid().toString(),bundle.floodBundleNum()));

				++count;
			}

			GeohistoryLog.d(tag, String.format("route_bundle bundle id %d: forwarded on %d links", 
					bundle.bundleid(), count));
			return count;
		}
		else
		{
			//本节点进入目的区域后又离开了,这里不进行任何转发
			if(bundle.isFlooding()==1)
			{
				GeohistoryLog.i(test, String.format("进入目标区域又离开了，不进行任何转发。bundle_%d,目的区域(0-3)：%d.%d.%d.%d",
						bundle.bundleid(),bundle.zeroArea(),bundle.firstArea(),bundle.secondArea(),bundle.thirdArea()));
				//判断是否超时
				//先不管超时，bytewalla应该自己管理好了超时
//				if(bundle.creation_ts().seconds()+bundle.expiration_timer())
				return 0;
			}
			//本节点没有进入目的区域，执行普通转发
			else
			{
				GeohistoryLog.i(test, String.format("没有进入目标区域，执行普通转发:dst(%s),copyNum(%d),bundle_%d,目的区域(0-3)：%d.%d.%d.%d",
						bundle.dest().toString(),bundle.deliverBundleNum(),bundle.bundleid(),bundle.zeroArea(),bundle.firstArea(),bundle.secondArea(),bundle.thirdArea()));
				
				//普通转发
				//节点Area与link的表
				HashMap<Area,RouteEntry> linkAreaMap=new HashMap<Area,RouteEntry>(route_table_.size());
				//本节点离目的地最近的区域
				Area thisnodeArea=baseArea.checkBundleDestArea(bundle);
				if(thisnodeArea==null)
				{
					GeohistoryLog.i(test, String.format("执行普通转发时，本节点没有找到与目标节点处在同一层的区域。bundle_%d,目的区域(0-3)：%d.%d.%d.%d",
							bundle.bundleid(),bundle.zeroArea(),bundle.firstArea(),bundle.secondArea(),bundle.thirdArea()));
					return 0;
				}
				//获取符合要求的当前邻居节点
				get_matching_RouteEntryVec(bundle, matches, thisnodeArea.getAreaLevel(), linkAreaMap);
				
				if(linkAreaMap.isEmpty())
				{
					GeohistoryLog.i(test, String.format("没有找到合适的link"));
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
						GeohistoryLog.d(test, String.format("checking route entry %s link %s (%s)",
								route.toString(), route.link().name(), route.link()));

						if (!should_fwd(bundle, route)) {
							continue;
						}

					
						/*Bundle forwardbundle=null;
						try {
							forwardbundle=(Bundle) bundle.clone();
							forwardbundle.set_bundleid(BundleStore.getInstance().next_id());//重命名Bundle id，防止transmitted之后删除
						} catch (CloneNotSupportedException e) {
							e.printStackTrace();
							GeohistoryLog.e(test, "bundle.clone出现错误");
							return 0;
						}
						
						if(forwardbundle==null)
						{
							GeohistoryLog.e(test, "bundle.clone出现错误");
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
						}*/
						
						//更改bundle的副本数量
						String bundle_id=StringOfBundle(bundle);
						int num=bundle.deliverBundleNum();
						if(Forward1PayloadNumMap.containsKey(bundle_id)){
							num=Forward1PayloadNumMap.get(bundle_id);
							
						}
						
						if(num<=1){
							GeohistoryLog.w(test, String.format("bundle(to:%s) don't have deliver num(%d)",bundle.dest().toString(),num));
							continue;
						}
						
						//该节点排名小于副本数量，执行2分法发送
						if(no<=num)
						{
							bundle.setDeliverBundleNum(num/2);
							num=num-num/2;
						}
						//该节点排名大于副本数量，只发送1份
						else
						{
							--num;
							bundle.setDeliverBundleNum(1);
						}
						Forward1PayloadNumMap.put(bundle_id, num);
					
						
						
						if (deferred_list(route.link()).list().contains(bundle)) {
							GeohistoryLog.d(test, String.format("route_bundle bundle %d: "
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
						GeohistoryLog.i(test, String.format("普通转发完成:dst(%s),copyNum(%d),bundle_%d,目的区域(0-3)：%d.%d.%d.%d",
								bundle.dest().toString(),bundle.deliverBundleNum(),bundle.bundleid(),bundle.zeroArea(),bundle.firstArea(),bundle.secondArea(),bundle.thirdArea()));

						++count;
					}
					
					//排名+1
					++no;
				}
				
				GeohistoryLog.d(test, String.format(
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
	 * @param sameAreaLevel ：本节点与目标所在的同一区域层次。如果为-1(RouteType.Flooding.sameAreaLevel)，则表示是执行洪泛的方法；
	 * 							如果是-2(RouteType.Neighbour.sameAreaLevel)则表示对特定的邻居发送；
	 * 							如果是合法的值，则是查找尽可能接近邻居的link;用路由方式来代替
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
				if (!should_fwd(bundle, entry)) {
					EndpointID prevhop = reception_cache_.lookup(bundle);
					if (prevhop != null) {
						if (prevhop.equals(entry.link().remote_eid())
								&& !prevhop.equals(EndpointID.NULL_EID())) {
							GeohistoryLog.w(tag, String.format("should_fwd: this link(remote:%s) should not be send",entry.dest_pattern().toString()));
							GeohistoryLog.w(test, String.format("should_fwd: this link(remote:%s) should not be send",entry.dest_pattern().toString()));
							continue;
						}
					}
					
				}
				
				//遇到了目标节点
				/*if(entry.dest_pattern().match(bundle.dest())){
					GeohistoryLog.i(test, String.format("this link(remote:%s) is the dest point",entry.dest_pattern().toString()));
					//原有算法，将link加到路由项中
					if(!entry_vec.contains(entry) && 
							(entry.link().state() == state_t.OPEN || 
							entry.link().state() == state_t.AVAILABLE ))
					{
						GeohistoryLog.d(test,String.format("普通转发方式查找路由表中Link,match entry %s", entry.toString() ));
						entry_vec.add(entry);
						++count;
						
						//加入到Map里面
						Neighbour neighbour=NeighbourManager.getInstance().getNeighbour(entry.dest_pattern());
						NeighbourArea neiArea=neighbour.getNeighbourArea();
						Area area=neiArea.checkBundleDestArea(bundle);
						sameLevelAreaMap.put(area,entry);
						break;
					}
				}*/
				
				//如果是flood的转发的方式，那么需要获取所有的link
				if(sameAreaLevel==RouteType.Flooding.sameAreaLevel && sameLevelAreaMap==null && entry_vec!=null){
					GeohistoryLog.d(tag, String.format("洪泛式路由方式查找路由表中的Link,bundle dst:%s",bundle.dest().toString()));
					//原有算法，将link加到路由项中
					if(!entry_vec.contains(entry) && 
							(entry.link().state() == state_t.OPEN || 
							entry.link().state() == state_t.AVAILABLE ))
					{
						GeohistoryLog.d(tag,String.format("match entry %s", entry.toString() ));
						entry_vec.add(entry);
						++count;
					}
					else
					{
						GeohistoryLog.d(tag,String.format("entry %s already in matches... ignoring", entry.toString() ));
					}
				}
				//如果只是转发给特定的邻居，那么只要获取邻居的link就行了
				else if(sameAreaLevel==RouteType.Neighbour.sameAreaLevel && sameLevelAreaMap==null && entry_vec!=null){
					GeohistoryLog.d(tag, String.format("向 特定邻居发送信息  的路由方式查找路由表中的Link,bundle dst:%s",bundle.dest().toString()));
					//检查是否是目的邻居
					if(entry.dest_pattern().match(bundle.dest())){
						//原有算法，将link加到路由项中
						if(!entry_vec.contains(entry) && 
								(entry.link().state() == state_t.OPEN || 
								entry.link().state() == state_t.AVAILABLE ))
						{
							GeohistoryLog.d(tag,String.format("match entry %s", entry.toString() ));
							entry_vec.add(entry);
							++count;
						}
						else
						{
							GeohistoryLog.d(tag,String.format("entry %s already in matches... ignoring", entry.toString() ));
						}
					}
				}
				//如果是普通转发，获取尽可能接近目的区域的邻居的link
				else if(sameAreaLevel>0 && sameLevelAreaMap!=null && entry_vec!=null){
					GeohistoryLog.d(test, String.format("普通转发方式查找路由表中Link,bundle dst:%s,sameAreaLevel:%s",
							bundle.dest().toString(),sameAreaLevel));
//					Neighbour neighbour=NeighbourManager.getInstance().getNeighbour(bundle.dest());//这里不应该是找到历史邻居中的目的节点，而是当前节点的历史邻居位置
					Neighbour neighbour=NeighbourManager.getInstance().getNeighbour(entry.dest_pattern());
					
					if(neighbour==null){
						GeohistoryLog.d(test, String.format("没有邻居%s的记录",neighbour.getEid().toString()));
						continue;
					}
					
					NeighbourArea neiArea=neighbour.getNeighbourArea();
					if(neiArea==null){
						GeohistoryLog.d(test, String.format("没有邻居%s的历史区域移动规律",neighbour.getEid().toString()));
						continue;
					}
					
					Area area=neiArea.checkBundleDestArea(bundle);
					if(area==null){
						GeohistoryLog.d(test, String.format("邻居%s没有去过目标区域",neighbour.getEid().toString()));
						continue;
					}
					
					//本节点离目标节点更接近，因此否决该link
					if(area.getAreaLevel()>areaLevel)
					{
						GeohistoryLog.d(test, String.format("本节点到过的区域比邻居%s更接近目标区域",neighbour.getEid().toString()));
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
					else {
						GeohistoryLog.d(test, String.format("本节点和比邻居%s一样接近目标区域",neighbour.getEid().toString()));
						continue;
					}
					
					//将尽可能接近目的地的区域邻居区域加入到
//					sameLevelAreaList.;
					
					//原有算法，将link加到路由项中
					if(!entry_vec.contains(entry) && 
							(entry.link().state() == state_t.OPEN || 
							entry.link().state() == state_t.AVAILABLE ))
					{
						GeohistoryLog.d(test,String.format("普通转发方式查找路由表中Link,match entry %s", entry.toString() ));
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
				//不合法的查找路由表中link方式
				else{
					GeohistoryLog.d(test, String.format("不合法的请求查找路由表中Link," +
							"bundle dst:%s,sameAreaLevel：%d,sameLevelAreaMap是否为空:%b, entry_vec是否为空:%b",
							bundle.dest().toString(),sameAreaLevel,sameLevelAreaMap==null,entry_vec==null));
				}
				
			}
			
			
			GeohistoryLog.v(tag, String.format("get_matching %s done, %d match(es)",
				bundle.dest().toString().toString(), count));
			
			return count;
		}
		finally 
		{
			route_table_.lock().unlock();
		}
	}
	
	
	/**
	 * 能否直接发送给目标节点，需要对照router表里面的route进行一遍检查，若可以直接交付给目标节点，那么需要直接交付，并且删除该Bundle
	 * @param bundle：检查是否能够直接交付的bundle
	 * @return true则表示可以直接交付，删除本地bundle；false表示不能直接交付，按照正常的情况进行。
	 */
	private boolean canDirectDelivery(Bundle bundle){
		Iterator<RouteEntry> iter = route_table_.route_table().iterator();
		while (iter.hasNext()){
			RouteEntry entry = iter.next();
			if(entry.dest_pattern().match(bundle.dest())){
				
				if((entry.link().state() == state_t.OPEN || 
						entry.link().state() == state_t.AVAILABLE )){
					GeohistoryLog.i(test, String.format("遇到了目的节点，直接将bundle发送给目的节点%s",entry.dest_pattern().toString()));
					

					//原来的转发过程
					if (deferred_list(entry.link()).list().contains(bundle)) {
						GeohistoryLog.d(test, String.format("route_bundle bundle %d: "
								+ "ignoring link %s since already deferred", bundle
								.bundleid(), entry.link().name()));
						continue;
					}

					// "because there may be bundles that already have deferred
					// transmission on the link, we first call check_next_hop to
					// get them into the queue before trying to route the new
					// arrival, otherwise it might leapfrog the other deferred
					// bundles" [DTN2]
					check_next_hop(entry.link());

					if (!fwd_to_nexthop(bundle, entry)) {
						continue;
					}
					GeohistoryLog.i(test, String.format("直接转发给目的节点完成:dst(%s),copyNum(%d),bundle_%d,目的区域(0-3)：%d.%d.%d.%d",
							bundle.dest().toString(),bundle.deliverBundleNum(),bundle.bundleid(),bundle.zeroArea(),bundle.firstArea(),bundle.secondArea(),bundle.thirdArea()));

					
					return true;
				}
			}
		}
		
		return false;
	}
	/*private boolean route_to_thisLink(Bundle bundle,Nei)
	{
		return true;
	}*/
	
	/**
	 * 说明：转发与邻居之间交互邻居信息的bundle的数据
	 * @param bundle
	 * @return
	 */
	private int route_neighbourArea_bundle(Bundle bundle){
		if(bundle.getBundleType()!=Bundle.NEI_AREA_BUNDLE){
			GeohistoryLog.e(tag,String.format("bundle send to %s is not the NEI_AREA_BUNDLE,and it's excute in route_neighbourArea_bundle function"));
			return -1;
		}
		
		RouteEntryVec matches = new RouteEntryVec();
		
		GeohistoryLog.i(tag, String.format("向指定邻居发送各个Area的频率信息"));
		
		//进行洪泛转发
		get_matching_RouteEntryVec(bundle, matches, RouteType.Neighbour.sameAreaLevel, null);
		
		sort_routes(matches);

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
	 * 所有需要在GeoHistoryRouter线程中处理的消息，全部存储在这个队列中
	 * 处理区域变化的列表
	 */
//	BlockingQueue<AreaInfo> areaInfoList;
	BlockingQueue<Object> messagequeue;
	
	/**
	 * 该节点移动时每次获取一下区域信息就加入到该队列
	 * @param:最新区域信息
	 */
	public boolean movetoArea(AreaInfo areainfo)	
	{
		if(areainfo==null)
			return false;
//		return areaInfoList.add(areainfo);
		return messagequeue.add(areainfo);
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

	/**
	 * GeoHistoryRouter 消息处理队列，用来处理区域移动的消息，处理要发送bundle的消息
	 */
	@Override
	public void run() {
		//此处后续需要改为bundleDaemon里面的逻辑标志位
		while(true)
		{
			if(BundleDaemon.getInstance().shutting_down())
				break;
			
			try //防止列表里面去数据的阻塞被打断
			{
//				Object object=areaInfoList.take();
				Object object=messagequeue.take();
				Log.v("geoRouterThread", "geoRouterThread is running");
				if(object instanceof AreaInfo){
					handle_areamoving((AreaInfo)object);//调用处理所在区域改变的代码
					continue;
				}
				else if(object instanceof SendBundleMsg){
					handle_sendBundle((SendBundleMsg)object);//调用发送bundle的处理函数
					continue;
				}
				else if(object instanceof RouteAllBundleMsg){//调用RouteAllBundle的处理函数
					handle_routeAllBundle();
					continue;
				}
				else if(object instanceof SaveNeighbourArea){//调用存储邻居的区域频率信息的处理函数
					handle_saveNeighbourArea((SaveNeighbourArea)object);
					continue;
				}
				else{
					GeohistoryLog.i(tag,"关闭GeoHistoryRouter的消息处理线程");
					break;
				}
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 处理区域移动的消息事件
	 */
	private void handle_areamoving(AreaInfo ainfo){
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
				GeohistoryLog.i(test, String.format("move to a new area : %s",newBaseArea.toString()));
				GeohistoryLog.d(tag,"移动到新区域："+newBaseArea.toString());
				AreaManager.writeAreaLogToFile("move to new area, ",baseArea);
			}
		}
	}
	
	/**
	 * 处理将payload中的区域频率信息，将区域频率信息保存到本地
	 */
	private void handle_saveNeighbourArea(SaveNeighbourArea saveNeighbour){
//		Bundle b=saveNeighbour.bundle;
		EndpointID eid=saveNeighbour.eid;
		GeohistoryLog.i(tag,String.format("收到来自%s的区域移动信息bundle",eid.toString()));
//		Neighbour nei=NeighbourManager.getInstance().getNeighbour(b.source());//用这个方式可能得到的是没有被创建的邻居
		Neighbour nei=NeighbourManager.getInstance().checkNeighbour(eid);
//		nei.generateArea(b.payload());//用收到的邻居的bundle来
		nei.generateArea();//利用本地存下来的payload文件来更新邻居的区域移动频率
		nei.printAreaInfo();//打印出来邻居区域信息
		
		//向该邻居发送队列里面需要发送的bundle（邻居间交换bundle的路由算法）
		//只需要将队列中的bundle向该link重新发送即可
		if(messagequeue.contains(routeAllBundle)){
			messagequeue.remove(routeAllBundle);
		}
		messagequeue.add(routeAllBundle);//用来将队列里面的bundle重新发送
	}
	
	/**
	 * route 所有的bundle的事件处理
	 */
	private void handle_routeAllBundle(){
		
		GeohistoryLog.i(test, String.format("geohistory router, handle reroute all bundle"));
		//reroute all bundle
		pending_bundles_.get_lock().lock();
		try {
			Log.d(tag, String.format(
					"reroute_all_bundles... %d bundles on pending list",
					pending_bundles_.size()));

			ListIterator<Bundle> iter = pending_bundles_.begin();
			while (iter.hasNext()) {
				Bundle bundle = iter.next();
				//对所有本路由算法要发送的bundle进行重新route
				if(bundle.getBundleType()!=Bundle.DATA_BUNDLE){
					continue;
				}
				route_bundle(bundle);
			}

		} catch (BundleListLockNotHoldByCurrentThread e) {
			Log.e(tag, "TableBasedRouter: reroute_all_bundles " + e.toString());
		} finally {
			pending_bundles_.get_lock().unlock();
		}
	}
	
	/**
	 * 处理发送bundle的事件
	 */
	private void handle_sendBundle(SendBundleMsg msg){
		try {
			sendMessage(msg.dest_eid, msg.file, msg.rctp, msg.areaid, msg.bundleType);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			GeohistoryLog.e(tag, String.format("向邻居%s发送自己的区域信息的bundle出错:%s", msg.dest_eid,"UnsupportedEncodingException"));
		} catch (DTNOpenFailException e) {
			e.printStackTrace();
			GeohistoryLog.e(tag, String.format("向邻居%s发送自己的区域信息的bundle出错:%s", msg.dest_eid,"DTNOpenFailException"));
		} catch (DTNAPIFailException e) {
			e.printStackTrace();
			GeohistoryLog.e(tag, String.format("向邻居%s发送自己的区域信息的bundle出错:%s", msg.dest_eid,"DTNAPIFailException"));
		}
		GeohistoryLog.i(tag, String.format("向邻居%s发送了bundle成功", msg.dest_eid));
		
		GeohistoryLog.i(test, String.format("handle_sendBundle ,send Bundle to neighbour ,dst(%s),ifisNeigh(%b)",
				msg.dest_eid,msg.bundleType==Bundle.NEI_AREA_BUNDLE));
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
	 * 保存邻居的区域频率信息
	 * @author wwtao
	 *
	 */
	private class SaveNeighbourArea{
//		Bundle bundle;
		EndpointID eid;
		/*public SaveNeighbourArea(Bundle bundle){
			this.bundle=bundle;
		}*/
		public SaveNeighbourArea(EndpointID eid){
			this.eid=eid;
		}
	}
	
	/**
	 * 对所有的bundle进行发送的事件
	 */
	private class RouteAllBundleMsg{
		public RouteAllBundleMsg(){
			
		}
	}
	
	/**
	 * 说明：针对需要发送bundle的时候，不能通过bundleDaemon的主线程发送，因此需要通过消息传递给本线程来完成发送
	 * @author wwtao
	 *
	 */
	private class SendBundleMsg{
		//sendMessage(String dest_eid,File file,boolean rctp,int[] areaid,int bundleType)
		
		String dest_eid;
		File file;
		boolean rctp;
		int[] areaid;
		int bundleType;
		
		private SendBundleMsg(String dest_eid,File file,boolean rctp,int[] areaid,int bundleType){
			this.dest_eid=dest_eid;
			this.file=file;
			this.rctp=rctp;
			this.areaid=areaid;
			this.bundleType=bundleType;
		}
	}
	
	/**
	 * GeoHistoryDtn发送数据的
	 * @param dest_eid
	 * @param file
	 * @param rctp
	 * @param eventSource :标明该bundle是哪种类型，是APPbundle还是邻居间交换信息的bundle
	 * @param areaid :用来表示目的节点的区域信息,不能为空
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
//			dtn_payload.set_file(new File("/sdcard/test_0.5M.mp3"));

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
	
	/**
	 * 关闭Geohistory的线程，可能不会被调用
	 */
	@Override
	public void shutdown() {
		GeohistoryLog.i(tag, "调用了GeohistoryRouter的shutdown方法");
		super.shutdown();
	}
	
	
	/**
	 * 路由的方式
	 */
	private enum RouteType{
		Flooding(-1),Neighbour(-2);
		
		int sameAreaLevel;
		private RouteType(int num){
			this.sameAreaLevel=num;
		}
	}
	
	/**
	 * 用来测试路由算法，发送测试的路由算法
	 */
	public void sendTestDataBundle(String dest_eid,String areaidstr){
		File file=new File(GeohistoryLog.logfile);
		int areaid[]=new int[4];
		String[] idstr=areaidstr.split(",");
		if(idstr.length!=4){
			GeohistoryLog.w(tag,String.format("准备向邻居发送data数据包出错,areaidstr的数据不对，areaidstr:%s"
					,areaidstr));
		}
		else{
			GeohistoryLog.i(tag, String.format("向邻居发送数据包:dest_eid:%s,areaidStr:%s"
					,dest_eid,areaidstr));
			GeohistoryLog.i(test, String.format("向邻居发送数据包:dest_eid:%s,areaidStr:%s"
					,dest_eid,areaidstr));
		}
		areaid[0]=Integer.valueOf(idstr[0]);
		areaid[1]=Integer.valueOf(idstr[1]);
		areaid[2]=Integer.valueOf(idstr[2]);
		areaid[3]=Integer.valueOf(idstr[3]);
		SendBundleMsg send=new SendBundleMsg(dest_eid, file, false, areaid, Bundle.DATA_BUNDLE);
		messagequeue.add(send);
	}
	
	/**
	 * geoDtn的实验，由192.168.5.11节点进行发送
	 */
	public void geoDtnExpriment(){
		String eid_14="dtn://192.168.5.14.wu.com";
		String eid_13="dtn://192.168.5.13.wu.com";
		String eid_12="dtn://192.168.5.12.wu.com";
		String eid_11="dtn://192.168.5.11.wu.com";
		
		/** 所在的区域的id
		A-B-N-L	2138123745
		B-C-M-N	1854974240
		N-M-I-J	-259923744
		L-N-J-K	-479623740

		C-D-O-M	1645316860
		D-E-F-O	1424349019
		O-F-G-H	-1134001844
		M-O-H-I	278199281
		 */
		int[] areaid_abnl={2138123745,2138123745,1587203,912940};
		int[] areaid_bcmn={1854974240,1854974240,1587203,912940};
		int[] areaid_nmij={-259923744,-259923744,1587203,912940};
		int[] areaid_lnjk={-479623740,-479623740,1587203,912940};
		
		int[] areaid_cdom={1645316860,1645316860,1587203,912940};
		int[] areaid_defo={1424349019,1424349019,1587203,912940};
		int[] areaid_ofgh={-1134001844,-1134001844,1587203,912940};
		int[] areaid_mohi={278199281,278199281,1587203,912940};
		
		File payload1=new File("/sdcard/dtnMessage/dtnMessage_1.txt");
		File payload2=new File("/sdcard/dtnMessage/dtnMessage_2.txt");
		File payload3=new File("/sdcard/dtnMessage/dtnMessage_3.txt");
		
		for(int i=0;i<20;i++){
			SendBundleMsg send0=new SendBundleMsg(eid_14, payload3, false, areaid_defo, Bundle.DATA_BUNDLE);
			messagequeue.add(send0);
			
			/*SendBundleMsg send1=new SendBundleMsg(eid_14, payload2, false, areaid_defo, Bundle.DATA_BUNDLE);
			messagequeue.add(send1);
			SendBundleMsg send2=new SendBundleMsg(eid_13, payload3, false, areaid_nmij, Bundle.DATA_BUNDLE);
			messagequeue.add(send2);*/
		}
	}
	
	//接受控制端的命令
	public class waitOrder implements Runnable{
		@Override
		public void run() {
			
			String tag="GeohistoryRouter.waitOrder";
			
			//数据
			int[] areaid_abnl={2138123745,2138123745,1587203,912940};
			int[] areaid_bcmn={1854974240,1854974240,1587203,912940};
			int[] areaid_nmij={-259923744,-259923744,1587203,912940};
			int[] areaid_lnjk={-479623740,-479623740,1587203,912940};
			
			int[] areaid_cdom={1645316860,1645316860,1587203,912940};
			int[] areaid_defo={1424349019,1424349019,1587203,912940};
			int[] areaid_ofgh={-1134001844,-1134001844,1587203,912940};
			int[] areaid_mohi={278199281,278199281,1587203,912940};
			
			/*HashMap<String,int[]> areaids=new HashMap<String, int[]>();
			areaids.put("abnl", areaid_abnl);
			areaids.put("bcmn", areaid_bcmn);
			areaids.put("nmij", areaid_nmij);
			areaids.put("lnjk", areaid_lnjk);
			
			areaids.put("cdom", areaid_cdom);
			areaids.put("defo", areaid_defo);
			areaids.put("ofgh", areaid_ofgh);
			areaids.put("mohi", areaid_mohi);*/
			
			
			String eid_14="dtn://192.168.5.14.wu.com";
			String eid_13="dtn://192.168.5.13.wu.com";
			String eid_12="dtn://192.168.5.12.wu.com";
			String eid_11="dtn://192.168.5.11.wu.com";
			
			File payload1=new File("/sdcard/dtnMessage/dtnMessage_1.txt");
			File payload2=new File("/sdcard/dtnMessage/dtnMessage_2.txt");
			File payload3=new File("/sdcard/dtnMessage/dtnMessage_3.txt");
			 
			int port=64431;//端口号
			DatagramSocket server;
			byte[] buffer=new byte[1024];
			try {
				Log.i(tag,"start GeohistoryRouter.waitOrder");
				server=new DatagramSocket(port);
				
				while(true){
					if(BundleDaemon.getInstance().shutting_down())
						break;
					
					String eid=null;
					int areaid[]=null;
					File payload=null;
					
					StringBuilder backinfo=new StringBuilder();
					try {
						Log.i(tag,"GeohistoryRouter.waitOrder wait for request");

						DatagramPacket packet=new DatagramPacket(buffer, buffer.length);
						server.receive(packet);
						byte[] temp_byte=packet.getData();
						int length=temp_byte[0];
						String str=new String(temp_byte,1,length-1);
						
						Log.v(tag, String.format("receive cmd %s",str));
						Log.v(tag,String.format("receive cmd(utf):%s",new String(temp_byte, 1, length-1, Charset.forName("utf-8"))));
						
						String s[]=str.split(" ");
						for(int i=0;i<s.length;i++){
							
							String temp[]=s[i].split(":");
							if(temp.length!=2){
								Log.e(tag,String.format("错误的参数：%s",s[i]));
								backinfo.append(String.format("错误的参数：%s",s[i]));
								break;
							}
							String name=temp[0];
							String value=temp[1];
							
							if(name.equals("eid")){
								if(value.equals("11")){
									eid=eid_11;
								}
								else if(value.equals("12")){
									eid=eid_12;
								}
								else if(value.equals("13")){
									eid=eid_13;
								}
								else if(value.equals("14")){
									eid=eid_14;
								}
								else{
									Log.e(tag,String.format("错误的参数：%s",s[i]));
									backinfo.append(String.format("错误的参数：%s",s[i]));
									break;
								}
							}
							else if(name.equals("payload")){
								if(value.equals("1")){
									payload=payload1;
								}
								else if(value.equals("2")){
									payload=payload2;
								}
								else if(value.equals("3")){
									payload=payload3;
								}
								else{
									Log.e(tag,String.format("错误的参数：%s",s[i]));
									backinfo.append(String.format("错误的参数：%s",s[i]));
									break;
								}
							}
							else if(name.equals("areaid")){
								if(value.equals("abnl")){
									areaid=areaid_abnl;
								}
								else if(value.equals("bcmn")){
									areaid=areaid_bcmn;
								}
								else if(value.equals("nmij")){
									areaid=areaid_nmij;
								}
								else if(value.equals("lnjk")){
									areaid=areaid_lnjk;
								}
								else if(value.equals("cdom")){
									areaid=areaid_cdom;
								}
								else if(value.equals("defo")){
									areaid=areaid_defo;
								}
								else if(value.equals("ofgh")){
									areaid=areaid_ofgh;
								}
								else if(value.equals("mohi")){
									areaid=areaid_mohi;
								}
								else{
									Log.e(tag,String.format("错误的参数：%s",s[i]));
									backinfo.append(String.format("错误的参数：%s",s[i]));
									break;
								}
							}
							else{
								Log.e(tag,String.format("错误的参数：%s",s[i]));
								backinfo.append(String.format("错误的参数：%s",s[i]));
								break;
							}
						}
						
						if(eid!=null && payload!=null && areaid!=null){
							SendBundleMsg send0=new SendBundleMsg(eid, payload, false, areaid, Bundle.DATA_BUNDLE);
							messagequeue.add(send0);
							backinfo.append(String.format("send bundle to %s ,with areaid(%d.%d.%d.%d) paylod(%s)",
									eid,areaid[0],areaid[1],areaid[2],areaid[3],payload.getPath()));
						}
						
						Log.v(tag, String.format("backinfo: %s",backinfo.toString()));
						
						//将反馈消息送给客户端
						/*DatagramPacket back=new DatagramPacket(backinfo.toString().getBytes(), backinfo.toString().getBytes().length,
								packet.getAddress(), packet.getPort());
						server.send(back);*/
						
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
				}
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
	}
	
	/**
	 * 提取bundle的唯一标识字符串
	 * @param bundle
	 * @return
	 */
	private static String StringOfBundle(Bundle bundle){
		//bundle转关键字
		String bundlestr=String.format("%s#%s#%d#%d#%b#%d#%d#%d",
				bundle.dest().toString(),bundle.source().toString(),bundle.creation_ts().seconds(),bundle.creation_ts().seqno(),
				bundle.is_fragment(),bundle.frag_offset(),bundle.orig_length(),bundle.payload().length());
		
		return bundlestr;
	}
}
