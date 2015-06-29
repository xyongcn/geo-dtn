/*
 *	  This file is part of the Bytewalla Project
 *    More information can be found at "http://www.tslab.ssvl.kth.se/csd/projects/092106/".
 *    
 *    Copyright 2009 Telecommunication Systems Laboratory (TSLab), Royal Institute of Technology, Sweden.
 *    
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 */

package android.geosvr.dtn.servlib.discovery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.geosvr.dtn.DTNManager;
import android.geosvr.dtn.servlib.bundling.BundleDaemon;
import android.geosvr.dtn.servlib.naming.EndpointID;
import android.geosvr.dtn.systemlib.util.IByteBuffer;
import android.geosvr.dtn.systemlib.util.SerializableByteBuffer;
import android.util.Log;

/**
 * "IPDiscovery is the main thread in IP-based neighbor discovery, configured
 * via config file or command console to listen to a specified UDP port for
 * unicast, broadcast, or multicast beacons from advertising neighbors" [DTN2].
 * 
 * @author Mar�a Jos� Peroza Marval (mjpm@kth.se)
 */

public class IPDiscovery extends Discovery implements Runnable {

	/**
	 * TAG for Android Logging mechanism
	 */
	public static final String TAG = "IPDiscovery";

	/**
	 * Socket receive timeout in milisecs
	 */
	private static final int TIMEOUT_MS = 5000;

	/**
	 * Maximun interval for advertising the announcements
	 */
	private static final int INT_MAX = 500;

	/**
	 * Wifi Manager //
	 */
	// private static WifiManager mWifi = (WifiManager) DTNService.context()
	// .getSystemService(Context.WIFI_SERVICE);

	/**
	 * Internal thread
	 */
	private Thread thread_;

	/**
	 * Constructor
	 */
	IPDiscovery(String name, short port) {

		super(name, "ip");
		// remote_addr_ = null;
		mcast_ttl_ = DEFAULT_MCAST_TTL;
		port_ = port;
		shutdown_ = false;
		persist_ = false;
		thread_ = new Thread(this);
	}

	/**
	 * If no other options are set for multicast TTL, set to 1
	 */
	public static final int DEFAULT_MCAST_TTL = 1;

	/**
	 * Enumerate which type of CL is advertised
	 */
	public enum cl_type_t {
		UNDEFINED(0), TCPCL(1), UDPCL(2); // TCP/UDP Convergence Layer

		private static final Map<Integer, cl_type_t> lookup = new HashMap<Integer, cl_type_t>();

		static {
			for (cl_type_t s : EnumSet.allOf(cl_type_t.class))
				lookup.put(s.getCode(), s);
		}

		private int code;

		private cl_type_t(int code) {
			this.code = code;
		}

		public int getCode() {
			return code;
		}

		public static cl_type_t get(int code) {
			return lookup.get(code);
		}
	}

	public static String type_to_str(cl_type_t t) {
		switch (t) {
		case TCPCL:
			return "tcp";
		case UDPCL:
			return "udp";
		case UNDEFINED:
			return "undefined";
		default:
			return "invalid Converge Layer type";
		}

	}

	public static cl_type_t str_to_type(String cltype) {
		if (cltype.compareTo("tcp") == 0)
			return cl_type_t.TCPCL;
		if (cltype.compareTo("udp") == 0)
			return cl_type_t.UDPCL;
		else
			return cl_type_t.UNDEFINED;
	}

	/**
	 * Close main socket, causing thread to exit
	 */
	@Override
	public void shutdown() {
		shutdown_ = true;
		socket_.close();
	}

	//
	// /**
	// * Get the current ip addres of the mobile. Get the ip assigned by the
	// DHCP
	// * @return the current IPaddress
	// */
	// public static InetAddress getting_my_ip() {
	//
	// InetAddress local_addr_ = null;
	// DhcpInfo dhcp = mWifi.getDhcpInfo();
	// if (dhcp == null) {
	// Log.d(TAG, "Could not get dhcp info");
	// }
	//
	// short[] quads = new short[4];
	// byte[] quads2 = new byte[4];
	// for (int k = 0; k < 4; k++) {
	// quads[k] = (byte) ((dhcp.ipAddress >> k * 8) & 0xFF);
	// quads2[k] = (byte) quads[k];
	// }
	// try {
	// local_addr_ = InetAddress.getByAddress(quads2);
	//
	// } catch (UnknownHostException e) {
	//
	// Log.d(TAG, "error getting_my_ip");
	// }
	//
	// return local_addr_;
	//
	// }

	/**
	 * Get the broadcast IP of the network where the phone is connected
	 * 
	 * @return the broadcast IPaddress
	 */
	// public InetAddress getting_broadcast_ip() {
	//
	// BROADCAST = null;
	// DhcpInfo dhcp = mWifi.getDhcpInfo();
	// if (dhcp == null) {
	// Log.d(TAG, "Could not get dhcp info");
	// }
	//
	// int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
	// byte[] quads = new byte[4];
	// for (int k = 0; k < 4; k++)
	// quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
	// try {
	// BROADCAST = InetAddress.getByAddress(quads);
	// } catch (UnknownHostException e) {
	// Log.d(TAG, "Error calculating Broadcast address");
	// }
	//
	// return BROADCAST;
	// }

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

		if (port_ == 0) {
			Log.e(TAG, "must specify port");
			return false;
		}

		// remote_addr_ = getting_broadcast_ip();

		// "Assume everything is broadcast unless unicast flag is set or if
		// the remote address is a multicast address
		// static IntAddress mcast_mask = 224.0.0.0" [DTN2];
		//
		// if (remote_addr_ == BROADCAST) {
		// Log.d(TAG, "configuring broadcast socket for remote address");
		//
		// } else if (!remote_addr_.isMulticastAddress()) {
		// Log.d(TAG, "configuring unicast socket for remote address");
		// }

		try {
			socket_ = new DatagramSocket(port_);
			socket_.setBroadcast(true);
			socket_.setSoTimeout(TIMEOUT_MS);
		} catch (IOException e) {
			Log.e(TAG, "bind failed " + e.getMessage());
		}

		Log.d(TAG, "starting thread");

		return true;

	}

	/**
	 * Main loop in Discovery
	 */
	public void run() {

		Log.d(TAG, "discovery thread running");
		IByteBuffer buf = new SerializableByteBuffer(1024);

		//由于发送线程和接受线程不能在同一个线程中，将发送线程单独提取出来
		(new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				//自动生成本网段的广播地址
				NetworkInterface netInterface;
				InetAddress broadcastAddr=null;
				try {

					broadcastAddr=InetAddress.getByName("255.255.255.255");
//					Log.i(TAG,"广播地址"+broadcastAddr.getHostAddress());
					netInterface = NetworkInterface.getByName("adhoc0");
					
					if (!netInterface.isLoopback() && netInterface.isUp()) 
				    {
				      List<InterfaceAddress> interfaceAddresses = netInterface.getInterfaceAddresses();
				      Log.i(TAG,"adhoc0网口接口数目："+String.valueOf(interfaceAddresses.size()));
				      for (InterfaceAddress interfaceAddress : interfaceAddresses) {
				        if (interfaceAddress.getBroadcast() != null) {
				        	Log.i(TAG,"广播地址"+interfaceAddress.getBroadcast().getHostAddress());// 输出广播地址
				        	Log.i(TAG,"子网掩码长度："+interfaceAddress.getNetworkPrefixLength());// 输出子网掩码长度，24表示掩码255.255.255.0
				        	broadcastAddr=interfaceAddress.getBroadcast();
				        }
				      }
				    }
					
//					Log.i(TAG,"广播地址"+interfaceAddresses.get(0).getBroadcast().getHostAddress());
				} catch (SocketException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				IByteBuffer buf = new SerializableByteBuffer(1024);
				// TODO Auto-generated method stub
				while(true)
				{
					if(shutdown_)
						break;
					
					/* Send section */
					try 
					{
//						Log.i(TAG, "发送数据");
						
						int min_diff = INT_MAX;
						Iterator<Announce> i = list_.iterator();

						while (i.hasNext()) {
							IPAnnounce announce = (IPAnnounce) i.next();

							int remaining = announce.interval_remaining();

							if (remaining == 0) {
								try {
									// Log.d(TAG, "announce ready for sending");
									hdr = announce.format_advertisement(buf, 1024);

									//buf的position重置为开始
									buf.rewind();
									
									//有用么？
									buf.put(hdr.cl_type());
									buf.put(hdr.interval());
									buf.putShort(hdr.length());
									byte[] ip_address = new byte[] { 0, 0, 0, 0 };
									buf.put(ip_address);
									buf.putShort(hdr.inet_port());
									buf.putShort(hdr.name_len());
									byte[] name = hdr.sender_name().getBytes();
									buf.put(name);

									//加入同一区域的验证信息
									buf.putInt(DTNManager.getInstance().currentLocation.getAreaNum());
									
									int l = hdr.length();
									data = new byte[l];
									int z;
									for (z = 0; z < l; z++) {
										data[z] = buf.get(z);
									}

									DatagramPacket pack = new DatagramPacket(data,
											data.length, broadcastAddr,port_);
									socket_.send(pack);
									min_diff = announce.interval();
								} catch (Exception e) {
									Log.e(TAG, "error sending the packet "
											+ e.getMessage());
								}
							} else {
								// Log.d(TAG, "Could not send discovery request");
								if (remaining < min_diff) {
									min_diff = announce.interval_remaining();
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						Log.e(TAG, "发送部分错误"+e.toString());
					}
				}
			}
		})).start();
		
		while (true) {
			if (shutdown_)
				break;

			/* receive section */
			try {
				buf.rewind();

				byte[] Rdata = new byte[1024];

				DatagramPacket packet = new DatagramPacket(Rdata, Rdata.length);

				//测试接收时间超时是否成功
//				Log.i(TAG, "准备接收数据");
				
				socket_.receive(packet);
				
//				Log.d("B4", "Received beacon: "+packet.getAddress());

				// String s = new String(packet.getData(), 0,
				// packet.getLength());
				// Log.d(TAG, "Received response " + s);

				EndpointID remote_eid = new EndpointID();
				String nexthop = "";// For now

				byte[] b = packet.getData();
				ByteBuffer bb = ByteBuffer.wrap(b);

//				hdr = new DiscoveryHeader();
				hdr = new DiscoveryHeader();
				hdr.set_cl_type(bb.get());

				hdr.set_interval(bb.get());
				hdr.set_length(bb.getShort());

				byte[] addr = new byte[4];
				bb.get(addr);

				hdr.set_inet_addr(InetAddress.getByAddress(addr));
				hdr.set_inet_port(bb.getShort());
				short name_len = bb.getShort();
				hdr.set_name_len(name_len);
				byte[] name = new byte[name_len];
				bb.get(name);
				
				//接受同一区域的验证信息
				int areanum=bb.getInt();
				
				String sender_name = new String(name);
				hdr.set_sender_name(sender_name);
				remote_eid = new EndpointID(hdr.sender_name());
				// if(hdr.inet_addr().toString().equals("0.0.0.0"))
				nexthop = packet.getAddress().toString() + ":"
						+ hdr.inet_port();
				
				
				
				
				// else
				// nexthop = hdr.inet_addr().toString()+":"+hdr.inet_port();

				String Type = IPDiscovery.type_to_str(cl_type_t.get(hdr
						.cl_type()));

				/*Log.i(TAG, "收到"+packet.getAddress()+"发来的数据包");
				Log.i(TAG, "Tye="+Type);
				Log.i(TAG, "nexthop="+nexthop);
				Log.i(TAG, "remote_eid.uri="+remote_eid.uri());*/
				
				Log.v("TESTE","receive from"+remote_eid.toString()+" areanum"+String.valueOf(areanum));
				//用来判断是不是在同一个区域
				if(areanum!=0 && areanum==DTNManager.getInstance().currentLocation.getAreaNum())
				{
					
					BundleDaemon BD = BundleDaemon.getInstance();
					//判断是否是本节点，如果是本节点则不作处理
					if (remote_eid.equals(BD.local_eid())) {
						// Log.d(TAG, "ignoring beacon from self" + remote_eid);
//						Log.i("TESTE","这是本节点");
					} else {
						// distribute to all beacons registered for this CL type
//						Log.i("TESTE","正常通信");
						handle_neighbor_discovered(Type, nexthop, remote_eid);
					}
				}
				else
				{
//					Log.i("TESTE","不在同一区域");
				}

			} catch (Exception e) {
//				e.printStackTrace();
//				Log.e(TAG, "接收错误"+e.toString());
			}

		}
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
	// protected InetAddress remote_addr_; // /< whether unicast, multicast, or
	// broadcast
	protected int mcast_ttl_; // /< TTL hop count for multicast option
	protected DatagramSocket socket_; // /< the socket for beacons in- and
	// out-bound
	protected boolean persist_; // /< whether to exit thread on send/recv
	// failures
	protected DiscoveryHeader hdr; // / Header of the discovery packet
	protected byte[] data; // / the data to be sent
	protected InetAddress BROADCAST; // / Broadcast address of the network where
	// the phone is connected

}
