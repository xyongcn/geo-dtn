package android.geosvr.dtn.servlib.discovery;

/*
 * 1. 路由获取邻居列表（邻居列表10s内有效）
 * 2. 根据邻居列表选出最优下一跳并插入路由表项
 */
import java.io.FileWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.geosvr.dtn.servlib.conv_layers.DTNLocationProvider;
import android.geosvr.dtn.servlib.conv_layers.Netw_layerInteractor;
import android.geosvr.dtn.systemlib.util.ByteHelper;
import android.geosvr.dtn.systemlib.util.IByteBuffer;
import android.geosvr.dtn.systemlib.util.IpHelper;
import android.geosvr.dtn.systemlib.util.SerializableByteBuffer;
import android.geosvr.dtn.systemlib.util.TimeHelper;
import android.util.Log;

public class PASVDiscovery {
	
	private static final String TAG = "PASVDiscovery";
	public static final int reply_wait_time_sec = 8;//s
	public static final int list_live_time_sec = 10;//s
	
	//eid, PASVExtraInfo(ip,..)
	private HashMap<String, PASVExtraInfo> PASVDiscoveries_;
	public long last_set_time_;
	public boolean isTimeout;
//	private static 	
	public class Task extends TimerTask {
		public void run()
		{
		    PASVDiscovery.getInstance().isTimeout = true;
		}
	}
	
	private PASVDiscovery(){
		PASVDiscoveries_ = new HashMap<String, PASVExtraInfo>();
		last_set_time_ = 0;
		isTimeout = false;
	}
	private static PASVDiscovery instance_;
	public static PASVDiscovery getInstance(){
		if (instance_ == null)
			instance_ = new PASVDiscovery();
		return instance_;
	}
	
	public HashMap<String, PASVExtraInfo> getPASVDiscoveriesList(){
//		if (TimeHelper.current_seconds_from_ref() - last_set_time_ < list_live_time_sec)
//			return this.PASVDiscoveries_;
		
		Log.e("testWu","发起获得获得邻居请求");
		
		
		//清空邻居表
		PASVDiscoveries_.clear();
		final int findport = Netw_layerInteractor.DTN_FIND_NEIGHBOUR_PORT;
		final int recvport = Netw_layerInteractor.DTN_RECV_NEIGHBOUR_PORT;
//		PASVExtraInfo extraInfo = new PASVExtraInfo();
		IByteBuffer buf = new SerializableByteBuffer(1024);

		int packet_len = buf.position()+10;
		byte[] data = new byte[packet_len];
		for (int z = 0; z < packet_len; z++) {
			data[z] = buf.get(z);
		}
		isTimeout = false;
		DatagramSocket sock_find_neigh = null;
		DatagramSocket sock_recv_neigh = null;
		
		
		long start_time = TimeHelper.getNanoTime();
		long current_time;
		    
		
		try {
			sock_find_neigh = new DatagramSocket(findport);
			sock_recv_neigh = new DatagramSocket(recvport);
			String host = "127.0.0.1";
			DatagramPacket packet = new DatagramPacket(data, data.length,
					InetAddress.getByName(host),findport);
			sock_find_neigh.send(packet);
			
			FileWriter fw = new FileWriter("/sdcard/dtn_test_data/neighbourtest.txt",true);
			String tmp = "start: "+Long.toString(start_time)+ '\n';
			fw.write(tmp,0,tmp.length());    
	        fw.flush();
	        
			Timer timer = new Timer(); 
		    timer.schedule(new Task(), reply_wait_time_sec * 1000);
			while (!isTimeout) {
				byte[] recvBuf = new byte[40];
				sock_recv_neigh.setSoTimeout(reply_wait_time_sec * 1000);
				DatagramPacket recvPacket = new DatagramPacket(recvBuf,
						recvBuf.length);
				sock_recv_neigh.receive(recvPacket);
				
				
				/*********************test neighbour finding speed***********************/
				
				byte[] neighborip = new byte[5];
				byte[] xb = new byte[5];
				byte[] yb = new byte[5];
				for (int k = 0; k < 4; k++) {
					neighborip[k] = recvBuf[k];
					xb[k] = recvBuf[k + 4];
					yb[k] = recvBuf[k + 8];
					//大小端的问题，加入蔡冰莹的大小端判断
					/*byte caiLongtitude[]=new byte[5];
					byte caiLatitude[]=new byte[5];
					if(ByteHelper.endian_test())//big endian
					{
						for (int tk = 0; tk < 4; tk++) {
							neigh_lat[tk] = recvBuf[tk + 4];
							neigh_lng[tk] = recvBuf[tk + 8];
							dis[tk] = recvBuf[tk +12];
						}
					}
					else {
						
						for (int k = 0; k < 4; k++) {
							srcip[k] = recvBuf[k];
						
							neigh_lat[3-k] = recvBuf[k + 4];
							neigh_lng[3-k] = recvBuf[k + 8];
							dis[3-k] = recvBuf[k +12];
						}
					}*/
				}
				current_time = TimeHelper.getNanoTime();
				String neighboripstr = IpHelper.ipbyte2ipstr(neighborip);
				String increment_time = Long.toString(current_time - start_time);
				String s = neighboripstr +' '+ increment_time + '\n';
		        fw.write(s,0,s.length());    
		        fw.flush(); 
		        /********************************************************/
				insertListFromBuf(recvBuf);
//				System.out.println("22");
			}
			fw.close();
		}
		catch (SocketTimeoutException e){
			Log.w("TAG",String.format("socket time out:%ds", reply_wait_time_sec));
		}
		catch (Exception e) {
			e.printStackTrace();
		} finally {
			sock_find_neigh.close();
			sock_recv_neigh.close();
		}
		
		last_set_time_ = TimeHelper.current_seconds_from_ref();
		
		if(PASVDiscoveries_.size()==0){
			Log.e("testWu","没有或得到周围的DTN邻居");
		}
		for(PASVExtraInfo info:PASVDiscoveries_.values()){
			Log.e("testWu",info.toString());
		}
		Log.e("testWu","获得获得邻居信息结束");
		
		return this.PASVDiscoveries_;
	}
	
	//x,y 要除10^n，n为精确到小数点后位数
	private void insertListFromBuf(byte[] buf){
		if (buf.length < 11 )
			return;
		byte[] neighborip = new byte[5];
		byte[] xb = new byte[4];
		byte[] yb = new byte[4];
		/*for (int k = 0; k < 4; k++) {
			neighborip[k] = buf[k];
			xb[k] = buf[k + 4];
			yb[k] = buf[k + 8];
			
		}*/
		if(ByteHelper.endian_test())//big endian
		{
			for (int tk = 0; tk < 4; tk++) {
				neighborip[tk] = buf[tk];
				xb[tk] = buf[tk + 4];
				yb[tk] = buf[tk + 8];
			}
		}
		else {
			
			for (int tk = 0; tk < 4; tk++) {
				neighborip[tk] = buf[tk];
				xb[3-tk] = buf[tk + 4];
				yb[3-tk] = buf[tk + 8];
			}
		}
		String neighboripstr = IpHelper.ipbyte2ipstr(neighborip);
		int x = ByteHelper.byte_array_to_int(xb);
		int y = ByteHelper.byte_array_to_int(yb);
		PASVExtraInfo info = new PASVExtraInfo();
		info.setNexthop(neighboripstr);
		info.setLongitude(x/(Math.pow(10, DTNLocationProvider.accuracy)));
		info.setLatitude(y/(Math.pow(10, DTNLocationProvider.accuracy)));
		PASVDiscoveries_.put(IpHelper.ipstr2Idstr(neighboripstr), info);
		
		//检测大小端的经纬度判断
		/*byte caiLongtitude[]=new byte[4];
		byte caiLatitude[]=new byte[4];
		if(ByteHelper.endian_test())//big endian
		{
			for (int tk = 0; tk < 4; tk++) {
				caiLongtitude[tk] = buf[tk + 4];
				caiLatitude[tk] = buf[tk + 8];
			}
		}
		else {
			
			for (int tk = 0; tk < 4; tk++) {
				caiLongtitude[3-tk] = buf[tk + 4];
				caiLatitude[3-tk] = buf[tk + 8];
			}
		}
		double dx = ByteHelper.byte_array_to_int(caiLongtitude);
		double dy = ByteHelper.byte_array_to_int(caiLatitude);
		dx/=Math.pow(10,DTNLocationProvider.accuracy);
		dy/=Math.pow(10,DTNLocationProvider.accuracy);
		Log.e("testWu",String.format("置换大小端的经纬度：(%f,%f)",dx,dy));
		
		StringBuilder str0=new StringBuilder();
		StringBuilder str1=new StringBuilder();
		StringBuilder str2=new StringBuilder();
		for(int i=0;i<4;i++){
			str0.append(String.format("%x ", buf[i]));
			str1.append(String.format("%x ", buf[i+4]));
			str2.append(String.format("%x ", buf[i+8]));
		}
		Log.e("testWu",String.format("收到的经纬度数据的二进制：(%s,%s,%s)",str0, str1,str2));
		*/
	}
	
	/**
	 * 在邻居列表中找到与指定坐标最近的点
	 * @param longitude
	 * @param latitude
	 * @return 如果找到，返回详细信息
	 * 		        如果找不到，返回null
	 */
	public static PASVExtraInfo findNearestNode(HashMap<String, PASVExtraInfo> discovlist, double longitude, double latitude){
		Object nearest = null;
		double smallest = 9999999.0;
		for(Object o : discovlist.keySet()){

			PASVExtraInfo info = discovlist.get(o);
			double temp = Math.pow(info.getLongitude() - longitude, 2) 
					+ Math.pow(info.getLatitude() - latitude, 2);
			temp = Math.sqrt(temp);
			if (temp < smallest){
				smallest = temp;
				nearest = o;
			}
		}
		if (nearest != null)
			return discovlist.get(nearest);
		else
			return null;
	}
	
	/**
	 * 在邻居列表中找指定的点
	 * @param eid
	 * @return 如果找到，返回详细信息
	 * 		        如果找不到，返回null
	 */
	public static PASVExtraInfo findNode(HashMap<String, PASVExtraInfo> discovlist, String eid){
		boolean flag = discovlist.containsKey(eid);
		if (flag)
			return discovlist.get(eid);
		else
			return null;
	}
}



//find的时候不需要加额外信息
//EndpointID local = BundleDaemon.getInstance().local_eid(); 
//PASVDiscoveryHeader hdr = new PASVDiscoveryHeader();
//hdr.setInet_addr(IpHelper.getLocalIpAddress().toString());
//hdr.setName_len((short)local.length());
//hdr.setSender_name(local.str());
//hdr.getExtraInfo().setLatitude(latitude);
//hdr.getExtraInfo().setLongitude(longitude);
//hdr.getExtraInfo().setNexthop(IpHelper.getLocalIpAddress().toString());
//
//buf.put(hdr.getInet_addr().getBytes());
//buf.putShort(hdr.getName_len());
//buf.put(hdr.getSender_name().getBytes());
//buf.put(hdr.getExtraInfo().getLongitude().getBytes());
//buf.put(hdr.getExtraInfo().getLatitude().getBytes());
//buf.put(hdr.getExtraInfo().getNexthop().getBytes());
//