package android.geosvr.dtn.servlib.geohistorydtn.areaConnectiveSimulation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import android.geosvr.dtn.DTNService;
import android.geosvr.dtn.servlib.bundling.BundleDaemon;
import android.geosvr.dtn.servlib.geohistorydtn.area.AreaInfo;
import android.geosvr.dtn.servlib.geohistorydtn.areaConnectiveSimulation.questAreaInfo.AreaLayerInfo;
import android.geosvr.dtn.servlib.geohistorydtn.areaConnectiveSimulation.questAreaInfo.QuestAreaInfo;
import android.geosvr.dtn.servlib.geohistorydtn.log.GeohistoryLog;
import android.geosvr.dtn.servlib.geohistorydtn.routing.GeoHistoryRouter;
import android.geosvr.dtn.servlib.routing.BundleRouter;
import android.geosvr.dtn.systemlib.util.ByteHelper;
import android.geosvr.dtn.systemlib.util.IpHelper;
import android.geosvr.dtn.systemlib.util.LocationHelp;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-10-27 下午2:01:36 
 * 说明  : 从模拟器获取到所需要的地图分层数据
 * 分层数据时由Areainfo保存，如果发生变化调用GeoHistoryRouter里面的movetoArea即可完成
 */
public class CurrentLocationFromSimulator implements Runnable,CurrentLocation{
	private static String tag="CurrentLocationFromSimulator";
	
	/**
	 * 请求分层信息的端口
	 */
	private static final int receivePort=63301;
	/**
	 * 请求经纬的端口
	 */
	public static final int queryLocationPort = 10003;
	
	/**
	 * 当前的位置所在分层信息
	 */
	AreaInfo currentAreaInfo=null;
	
	/**
	 * 用来关闭线程的标志
	 */
	boolean shutdown=false;
	
	/**
	 * 请求的udp服务
	 */
	DatagramSocket server;
	
	private static class SingleInstance{
		private static CurrentLocationFromSimulator instance=new CurrentLocationFromSimulator();
	}
	
	public static CurrentLocationFromSimulator getInstance(){
		return SingleInstance.instance;
	}
	
	private CurrentLocationFromSimulator(){
		try {
			GeohistoryLog.i(tag, "返回单例模式");
			server=new DatagramSocket();
			
//			init();
		} catch (SocketException e) {
			e.printStackTrace();
			GeohistoryLog.e(tag, "无法建立请求分层信息的server");
		}
	}
	
	//初始化
	public void init(){
		shutdown=false;
		(new Thread(this)).start();
	}
	
	@Override
	public void run() {
		GeohistoryLog.i(tag, String.format("请求经纬度和分层信息线程开始"));
		
		ByteArrayOutputStream bout=new ByteArrayOutputStream(512);
		DataOutputStream dout=new DataOutputStream(bout);
		DatagramPacket requestLocationPacket;
		byte[] buff=new byte[512];
		DatagramPacket receivePacket=new DatagramPacket(buff, buff.length);
		
		try{
			dout.writeInt(IpHelper.ipstr2int("127.0.0.1"));
			dout.writeInt(0);
			dout.writeInt(0);
			//请求经纬度的包
			requestLocationPacket=new DatagramPacket(bout.toByteArray(), bout.toByteArray().length,InetAddress.getLocalHost(),queryLocationPort);
			dout.close();
			bout.close();
			bout=new ByteArrayOutputStream(512);
			dout=new DataOutputStream(bout);
		}
		catch(Exception e){
			e.printStackTrace();
			try {
				dout.close();
				bout.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return;
		}
		
		try {
		
//			server.setSoTimeout(5000);
			while(true){
				if(shutdown)
					break;
			
				//请求当前的地理位置,通过经纬度都是0来请求当前的经纬度
				server.send(requestLocationPacket);
				
				GeohistoryLog.v(tag, "向模拟器发出本地位置请求");
				
				//收取经纬度信息
				try{
					server.receive(receivePacket);
				} catch (SocketTimeoutException e){
					e.printStackTrace();
					GeohistoryLog.e(tag, String.format("接受模拟器的本地经纬度超时，可能没有打开模拟器"));
					continue;
				}
//				DataInputStream din=new DataInputStream(new ByteArrayInputStream(buff));
				//此read的方式，不能够完成正常的gps信息的大小端转换
				/*din.readInt();
				double longitude=LocationHelp.gpsint2double(din.readInt());
				double latitude=LocationHelp.gpsint2double(din.readInt());
				GeohistoryLog.i(tag, String.format("获取请求到的经纬度信息：(longitude,latitude):(%f,%f)", longitude,latitude));*/
			
				byte lon[]=new byte[4];
				byte lat[]=new byte[4];
				//根据大小端读取经纬度
				if(ByteHelper.endian_test())//big endian
				{
					for (int k = 0; k < 4; k++) {
						lat[k] = buff[k + 4];
						lon[k] = buff[k + 8];
					}
				}
				else {
					
					for (int k = 0; k < 4; k++) {
						lat[3-k] = buff[k + 4];
						lon[3-k] = buff[k + 8];
					}
				}
				double longitude=LocationHelp.gpsint2double(ByteHelper.byte_array_to_int(lon));
				double latitude=LocationHelp.gpsint2double(ByteHelper.byte_array_to_int(lat));
				GeohistoryLog.d(tag, String.format("获取请求到的经纬度信息(转换大小端的)：(longitude,latitude):(%f,%f)", longitude,latitude));
				try {
					AreaLayerInfo areaLayerInfo=QuestAreaInfo.getInstance().getLayerInfo(InetAddress.getLocalHost().toString(), longitude, latitude);
					//判断DTN服务是否在运行
					if(DTNService.is_running())
					{
						BundleRouter router=BundleDaemon.getInstance().router();
						if(router instanceof GeoHistoryRouter)
						{
							((GeoHistoryRouter)router).movetoArea(new AreaInfo(areaLayerInfo));
						}
					}
				} catch (SocketTimeoutException e){
					e.printStackTrace();
					GeohistoryLog.e(tag, String.format("请求本节点ip(%s)时，超时，可能没有打开分层信息查询服务", InetAddress.getLocalHost().toString()));
				}
				catch (Exception e) {
					e.printStackTrace();
					GeohistoryLog.e(tag, String.format("请求本节点ip(%s)的分层信息出错", InetAddress.getLocalHost().toString()));
				}
				
				//轮询本地分层信息变化的间隔
				Thread.sleep(3000);
			}
		
		
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		finally{

			try {
				dout.close();
				bout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		GeohistoryLog.d(tag, "请求经纬度或者分层信息线程结束运行");
	}


	@Override
	public int getAreaNum() {
		return 0;
	}
	
	/**
	 * 关闭本线程
	 */
	public void shutdown(){
		shutdown=true;
	}
}
