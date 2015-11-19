package android.geosvr.dtn.servlib.geohistorydtn.areaConnectiveSimulation.questAreaInfo;

import java.io.Serializable;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-10-21 下午4:36:42 
 * 说明 : 客户端用来请求的当前gps所对应的分层信息的对象块 
 */
public class RequestAreaLayer implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5597011442382291417L;
	
	public int ip;//用一个整形数字来表示ip
	public double longitude;//经度
	public double latitude;
	
	public RequestAreaLayer(int ip,double lon,double lat){
		this.ip=ip;
		this.longitude=lon;
		this.latitude=lat;
	}
	
	@Override
	public String toString() {
		return String.format("ip: %d , longitude: %f , latitude: %f", ip,longitude,latitude);
	}
}
