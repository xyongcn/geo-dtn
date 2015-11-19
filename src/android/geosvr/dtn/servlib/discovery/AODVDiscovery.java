package android.geosvr.dtn.servlib.discovery;

import java.util.HashMap;
import java.util.Iterator;

import android.geosvr.dtn.DTNService;
import android.geosvr.dtn.servlib.conv_layers.Netw_layerInteractor;
import android.geosvr.dtn.servlib.discovery.IPDiscovery.cl_type_t;
import android.geosvr.dtn.servlib.geohistorydtn.log.GeohistoryLog;
import android.geosvr.dtn.servlib.naming.EndpointID;
import android.geosvr.dtn.systemlib.util.IByteBuffer;
import android.geosvr.dtn.systemlib.util.IpHelper;
import android.geosvr.dtn.systemlib.util.SerializableByteBuffer;
import android.util.Log;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-10-19 下午8:49:45 
 * 说明 
 */
public class AODVDiscovery extends Discovery implements Runnable {

	/**
	 * TAG for Android Logging mechanism
	 */
	public static final String TAG = "AODVDiscovery";


	/**
	 * Internal thread
	 */
	private Thread thread_;

	/**
	 * Constructor
	 */
	public AODVDiscovery(String name, short port) {

		super(name, "aodvDiscovery");
		port_ = port;
		shutdown_ = false;
		thread_ = new Thread(this);
	}

	/**
	 * Close main socket, causing thread to exit
	 */
	@Override
	public void shutdown() {
		shutdown_ = true;
	}

	/**
	 * Set internal state using parameter list; return true on success, else
	 * false
	 */
	@Override
	protected boolean configure() {

		if (thread_.isAlive()) {
			Log.w(TAG, "reconfiguration of IPDiscovery not supported");
			return false;
		}

		if (port_ != -1) {
			Log.e(TAG, "AODVDiscovery's port must be -1");
			return false;
		}

		return true;

	}

	/**
	 * Main loop in Discovery
	 */
	public void run() {
		//DTN向aodv注册
		Netw_layerInteractor.getInstace().register2netw_layer();
    	GeohistoryLog.i(TAG,"dtn register2aodv");
		
		IByteBuffer buf = new SerializableByteBuffer(1024);
		GeohistoryLog.i(TAG,String.format("start AODVDiscovery Thread"));
		while(true){
			if(shutdown_)
				break;
			
			//调用aodv进行邻居发现
			HashMap<String, PASVExtraInfo> neighbour_map=PASVDiscovery.getInstance().getPASVDiscoveriesList();
			Log.i(TAG,String.format("receive the aodv's neighour map(size：%d)", neighbour_map.size()));
			for(PASVExtraInfo info:neighbour_map.values()){
				Log.i(TAG,info.toString());
				Iterator<Announce> i = list_.iterator();

				while (i.hasNext()) {
					IPAnnounce announce = (IPAnnounce) i.next();

					try {
						// Log.d(TAG, "announce ready for sending");
						DiscoveryHeader hdr = announce.format_advertisement(buf, 1024);
						String Type = IPDiscovery.type_to_str(cl_type_t.get(hdr
								.cl_type()));
						String nexthop = info.getNexthop() + ":"
								+ hdr.inet_port();
						EndpointID remote_eid = new EndpointID(IpHelper.ipstr2Idstr(info.getNexthop()));
						
						Log.i(TAG,String.format("handle_neighbor_discovered: Type:%s,nexthop:%s,remote_eid:%s",Type,nexthop,remote_eid));
						handle_neighbor_discovered(Type, nexthop, remote_eid);
					}
					catch(Exception e){
						e.printStackTrace();
//						Log.e(TAG,String.format("receive the aodv's neighour map(size：%d)", neighbour_map.size()));
					}
					
				}
				
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		GeohistoryLog.i(TAG,String.format("AODVDiscovery Thread ended"));
	}
	

	/**
	 * Outbound address of advertisements sent by this Discovery instance
	 */
	@Override
	public String to_addr() {
		return to_addr_;
	}

	/**
	 * Local address on which to listen for advertisements
	 */
	@Override
	public String local_addr() {
		return local_;
	}

	/**
	 * Start the IPDiscovery thread
	 */
	@Override
	public void start() {
		thread_.start();
	}

	protected void handle_announce() {

	}

	protected volatile boolean shutdown_; // /< signal to close down thread
	// protected InetAddress local_addr_; // /< address for bind() to receive
	// beacons
	protected short port_; // /< local and remote


}
