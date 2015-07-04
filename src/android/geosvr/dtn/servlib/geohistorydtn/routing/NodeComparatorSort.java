package android.geosvr.dtn.servlib.geohistorydtn.routing;

import java.util.Comparator;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-7-4 下午8:30:12 
 * 说明  :对所有的携带节点进行排序，优先级：有机会值的>无机会值；区域层次低>区域层次高；从低层次到高层，机会值分量越大越优先
 */
public class NodeComparatorSort implements Comparator<Node>{

	//要对node节点进行降序排列
	//return为>0时，会交换两个参数
	@Override
	public int compare(Node node1, Node node2) {
		//node1为空时，交换
		if(node1==null)
			return 1;
		
		if(node2==null)
			return -1;
		
		//node2的区域层次更低，所以交换
		if(node2.closedArea.getAreaLevel()<node1.closedArea.getAreaLevel())
			return 1;
		
		if(node2.closedArea.getAreaLevel()>node1.closedArea.getAreaLevel())
			return -1;
		
		//逐个比较机会值
		for(int i=0;i<Math.min(node1.chanceValue.length, node2.chanceValue.length);i++)
		{
			if(node2.chanceValue[i]>node1.chanceValue[i])
			{
				return 1;
			}
			
			if(node2.chanceValue[i]<node1.chanceValue[i])
			{
				return -1;
			}
		}
		
		
		//默认不交换
		return -1;
	}

}
