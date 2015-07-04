package android.geosvr.dtn.servlib.geohistorydtn.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import android.geosvr.dtn.servlib.bundling.Bundle;
import android.geosvr.dtn.servlib.geohistorydtn.area.Area;
import android.geosvr.dtn.servlib.geohistorydtn.log.GeohistoryLog;
import android.geosvr.dtn.servlib.geohistorydtn.neighbour.Neighbour;
import android.geosvr.dtn.servlib.geohistorydtn.neighbour.NeighbourArea;
import android.geosvr.dtn.servlib.geohistorydtn.neighbour.NeighbourManager;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-7-4 下午6:53:41 
 * 说明  : 用来对尽可能送达目的区域的机会值排序；
 * 		1.如果送达的区域里目的区域层次尽肯能接近则排名越高
 * 		2.在送达相同的层次区域时，比较该层区域开始的机会值向量，向量优先级从底层往高层，机会值越大排名越高
 */
public class ChanceValueSort 
{
	final static String tag="ChanceValueSort";
	
	/**
	 * 从当前邻居，本节点，历史邻居中排序选出比本节点更好的节点
	 * @param nowNeighbour
	 * @param bundle
	 * @return
	 */
	public static List<Area> getAllAvaliableNodeArea(List<Area> nowNeighbour,Bundle bundle,Area thisnode)
	{
		if(!nowNeighbour.contains(thisnode))
		{
			GeohistoryLog.e(tag, "计算比本节点更优节点时，本节点不再列表中");
			nowNeighbour.add(thisnode);
		}
		List<Node> allNodeList=addNeibourAreaNode(nowNeighbour, bundle);
		
		Collections.sort(allNodeList,new NodeComparatorSort());
		
		List<Area> avaliable=nowNeighbour;
		avaliable.clear();
		for(int i=0;i<allNodeList.size();i++)
		{
			if(!allNodeList.get(i).closedArea.equals(thisnode))
			{
				avaliable.add(allNodeList.get(i).closedArea);
			}
			else
				break;
		}
		return avaliable;
	}
	
	/**
	 * 在对所有的节点排序的时候，将历史邻居的也加入进来
	 */
	public static List<Node> addNeibourAreaNode(List<Area> nowNeighbour,Bundle bundle)
	{
		List<Node> nodelist=new ArrayList<Node>();
		
		//加入当前邻居表的node,这个表里面包含有本几点的node
		for(Area area:nowNeighbour)
		{
			Node tempNode=new Node(area, ChanceValueCompute.carryChance(bundle, area));
			nodelist.add(tempNode);
		}
		
		Collection<Neighbour> historyNeighbour=NeighbourManager.getInstance().getAllNeighbour();
//		List<Area> historyNeighbourArea=new ArrayList<Area>(historyNeighbour.size());
		for(Neighbour nei:historyNeighbour)
		{
			//这个邻居没有它的历史区域的记录
			NeighbourArea neiArea=nei.getNeighbourArea();
			if(neiArea==null)
				continue;
			
			//这个邻居的历史区域没有接近目的区域的
			Area area=neiArea.checkBundleDestArea(bundle);
			if(area==null)
				continue;
			
			//这个历史邻居中点在当前邻居中存在
			if(nowNeighbour.contains(area))
				continue;
			
//			historyNeighbourArea.add(area);
			Node tempNode=new Node(area, ChanceValueCompute.histroyNeighbourCarryChance(bundle, area, nei));
			nodelist.add(tempNode);
		}
		
		return nodelist;
	}
}
