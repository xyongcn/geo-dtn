package android.geosvr.dtn.servlib.geohistorydtn.routing;

import android.geosvr.dtn.servlib.geohistorydtn.area.Area;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-7-4 下午7:12:19 
 * 说明  :用来在排序节点到达目标区域机会值的元素
 */

class Node
{
	/**
	 * 最接近目的区域的区域，可以获得相应的层次
	 */
	Area closedArea;
	
	double[] chanceValue;
	
	public Node(Area closedArea,double[] chanceValue)
	{
		this.closedArea=closedArea;
		this.chanceValue=chanceValue;
	}
}
