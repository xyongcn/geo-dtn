package android.geosvr.dtn.servlib.geohistorydtn.areaConnectiveSimulation.questAreaInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import android.geosvr.dtn.servlib.geohistorydtn.log.GeohistoryLog;
import android.util.Log;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-10-21 下午6:44:50 
 * 说明 ： 向地图提供的接口请求获得所在的各层次分层信息 
 */
public class QuestAreaInfo {
	private static class SingleInstance{
		private static QuestAreaInfo instance=new QuestAreaInfo();
	}
	
	//单例
	public static QuestAreaInfo getInstance(){
		return SingleInstance.instance;
	}
	
	private static String tag="QuestAreaInfo";
	
	//提供地图服务的端口号
	private static int serverPort=63301;
	
	//用来发送或者接受的server
	private DatagramSocket server=null;
	
	//接受的数据包
	byte[] b=new byte[1024];
	DatagramPacket receivePacket=new DatagramPacket(b,b.length);
	
	private QuestAreaInfo(){
		try {
			server=new DatagramSocket();
			server.setSoTimeout(5000);//设置超时
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(tag,"QuestAreaInfo 无法生成自己的随机端口的server，因此无法向地图请求区域分层信息");
		}
	}
	
	/**
	 * 请求该(ip,经度,维度)下，经纬度对应的分层信息
	 * @param ip : 用int类型表示的ip地址
	 * @param longitude
	 * @param latitude
	 * @return 返回的分层信息是从上层到底层的形式，最顶层的区域id在第一位
	 * @throws IOException 
	 */
	public AreaLayerInfo getLayerInfo(String ip,double longitude,double latitude) throws SocketTimeoutException,IOException,Exception{

		ByteArrayOutputStream bout=new ByteArrayOutputStream();
		DataOutputStream dout=new DataOutputStream(bout);
		dout.writeUTF(ip);
		dout.writeDouble(longitude);
		dout.writeDouble(latitude);
		byte temp[]=bout.toByteArray();
		
		DatagramPacket questpacket=new DatagramPacket(temp, temp.length, InetAddress.getLocalHost(), serverPort);
		Log.d(tag,String.format("向本地地图接口请求分层信息,quest(ip:%s , longitude:%f , latitude:%f)",ip,longitude,latitude));
		server.send(questpacket);
		dout.close();
		bout.close();
		
		server.receive(receivePacket);
//		Log.d(tag,"get result from map interface");
		temp=receivePacket.getData();
		ByteArrayInputStream bin=new ByteArrayInputStream(temp);
		DataInputStream din=new DataInputStream(bin);
		int len=din.readInt();
		AreaLayerInfo info=new AreaLayerInfo();
		for(int i=0;i<len;i++)
			info.add(din.readInt());
		GeohistoryLog.d(tag, String.format("分层信息(由最顶层到最底层)：%s", info.toString()));
		return info;
	}
}
