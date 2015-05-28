package android.geosvr.dtn.servlib.geohistorydtn.area;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-5-14 上午9:16:09 
 * 说明 ：对所有的Area进行管理，分配和超出容量删除机制
 */
public class AreaManager 
{
	private static class SingleAreaManager
	{
		static AreaManager instance=new AreaManager();
	}
	
	//线程安全单例模式
	public static AreaManager getInstance()
	{
		return SingleAreaManager.instance;
	}
	
	/**
	 * 默认构造函数
	 */
	public AreaManager()
	{
		init();
	}
	
	private void init()	
	{
		areaMap=new HashMap<String, Area>(500);
	}
	
	/**
	 * 用来存放所有的Area的hashmap
	 */
	HashMap<String,Area> areaMap;
	
	/**
	 * 检查这批新的区域并且修改相应的父类引用
	 * @param areainfo:所在各个层次信息
	 * @return :最底层的区域的引用
	 */
	public Area checkAreaInfo(AreaInfo areainfo)
	{
		int[] areaid=areainfo.getAreaId();
		
		int start=areainfo.getBaseAreaNum();
		int end=areaid.length;
		List<Area> arealist=new ArrayList<Area>(end);
		
		boolean valid=true;
		int level=areainfo.getBaseAreaLevel();
		for(int i=start;i<end;i++)
		{
			int id=areaid[i];
			if(AreaLevel.isLevelValid(level))
			{
				//生成hashmap的标识字符串
				String s=String.valueOf(level)+"#"+String.valueOf(id);
				Area a=areaMap.get(s);
				if(a==null)
				{
					a=new Area(level, id);
					areaMap.put(s, a);
				}
				
				arealist.add(a);
			}
			//如果存在不合格的level，那么久不需要
			else
			{
				valid=false;
				break;
			}
			
			
			++level;
		}
		
		//修改相关父类的引用
		if(valid)
		{
			for(int i=0;i<arealist.size()-1;i++)
			{
				arealist.get(i).setFatherArea(arealist.get(i+1));
			}
			return arealist.get(0);
		}
		else
			return null;
	}
	
	
	//获取位置以及改变位置的流程
	//1.首先由专门获取位置的程序负责获取当前的位置，然后再通过相关程序得到相应的区域编号；一种模拟环境中是直接得到相应的区域编号。
	//2.调用GeoHistoryRouter里面的changeLocation,
	//3.由GeoHistroyRouter来判断是否进行改变区域的事件处理以及调用AreaManager里面对新区域的处理
	//
	//
}
