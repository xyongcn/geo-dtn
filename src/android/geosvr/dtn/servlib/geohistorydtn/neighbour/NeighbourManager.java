package android.geosvr.dtn.servlib.geohistorydtn.neighbour;

import java.util.HashMap;
import java.util.Stack;

import android.geosvr.dtn.servlib.geohistorydtn.config.NeibhourConfig;
import android.geosvr.dtn.servlib.naming.EndpointID;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-6-22 下午3:10:12 
 * 说明  ：作为邻居类的管理器
 */
public class NeighbourManager {
	
	
	
	/**
	 * 历史邻居的表
	 */
	HashMap<String, Neighbour> neighbourlist;
	
	private static class SingleNeighbourManager
	{
		public static NeighbourManager instance=new NeighbourManager();
	}
	
	public static NeighbourManager getInstance()
	{
		return SingleNeighbourManager.instance;
	}
	
	private NeighbourManager()
	{
		init();
	}
	
	private void init()
	{
		neighbourlist=new HashMap<String, Neighbour>(NeibhourConfig.NEIGHOURNUM);
	}
	
	/**
	 * 遇到新邻居后进行check，如果没有该邻居将其下载到
	 */
	public Neighbour checkNeighbour(EndpointID eid)
	{
		if(eid==null)
			return null;
		
		Neighbour nei=neighbourlist.get(eid.toString());
		if(nei==null)
		{
			nei=new Neighbour(eid);
			neighbourlist.put(eid.toString(), nei);
		}
		nei.checkVectorChange();
		return nei;
	}
	
	/**
	 * 找到该eid的Neichbour
	 */
	public Neighbour getNeighbour(EndpointID eid)
	{
		if(eid!=null)
			return neighbourlist.get(eid.toString());
		else
			return null;
	}
}
