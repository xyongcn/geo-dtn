package android.geosvr.dtn.servlib.geohistorydtn.area;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import android.geosvr.dtn.servlib.bundling.Bundle;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVector;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVectorLevel;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVectorManager;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVectorServiceType;
import android.geosvr.dtn.servlib.geohistorydtn.timeManager.TimeManager;

/**
 * 作用:用来描述区域的对象
 * @author wwtao
 *
 */
public class Area implements Serializable
{
	//区域分层的
	/*private int zeronum;
	private int firstnum;
	private int secondnum;
	private int thirdnum;*/
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 111L;

	/**
	 * 该区域是否是是当前所在区域
	 */
	boolean isCurrent=false;
	
	/**
	 * 区域的层次
	 */
	int level;
	
	/**
	 * 区域所在层次的id
	 */
	int id;
	
	/**
	 * 频率向量列表表
	 */
	List<FrequencyVector> vectorlist;
	
	/**
	 * 父区域
	 */
	Area fatherArea;
	
	/**
	 * 子区域
	 * 暂时不启用
	 */
//	List<Area> childrenlist;
	
	/*public Area(int level,int id,Area fatherArea,Collection<Area> childarea)
	{
		this.level=level;
		this.id=id;
		
		this.fatherArea=fatherArea;
		childrenlist=new LinkedList<Area>();
		this.childrenlist.addAll(childarea);
		
		init();
	}*/
	
	public Area(int level,int id,Area fatherArea)
	{
		this.level=level;
		this.id=id;
		
		this.fatherArea=fatherArea;
		
		init();
	}
	
	/**
	 * 需要重新实现它的father
	 * @param level
	 * @param id
	 */
	public Area(int level,int id)
	{
		this.level=level;
		this.id=id;
		
		this.fatherArea=null;
		
		init();
	}
	
	/**
	 * 添加子区域
	 * @param area
	 */
	/*public void addChildArea(Area area)
	{
		this.childrenlist.add(area);
	}*/
	
	/**
	 * 删除子区域
	 * 
	 */
	/*public void removeChildArea(Area area)
	{
		this.childrenlist.remove(area);
	}*/
	
	private void init()	
	{
		vectorlist=new ArrayList<FrequencyVector>();
		
		/**
		 * 加入频率向量，根据区域的层次建立相应的频率向量
		 * 最底层区域只有hourVector
		 * 次底层有hourVector、weekVector
		 * 最顶层有hourVector、weekVector、monthVector
		 */
		int areatype=FrequencyVectorServiceType.AREA;
		//创建基本的小时频率向量
		if(this.level>=AreaLevel.FIRSTLEVEL)
			vectorlist.add(FrequencyVectorManager.getInstance().newFVector(FrequencyVectorLevel.hourVector,areatype));
		//创建周频率向量
		if(this.level>=AreaLevel.SECONDLEVEL)
		{
			vectorlist.add(FrequencyVectorManager.getInstance().newFVector(FrequencyVectorLevel.weekVector, areatype));
		}
		//创建月频率向量
		if(this.level>=AreaLevel.THIRDLEVEL)
		{
			vectorlist.add(FrequencyVectorManager.getInstance().newFVector(FrequencyVectorLevel.monthVector, areatype));
		}
	}
	
	
	
	/**
	 * @author wwtao
	 * 判断是不是同一个area，不只是通过对象引用比较，因为在不同区域间也是要进行比较的，所以应该有每个区域的标识
	 * 
	 */
	@Override
	public boolean equals(Object o) {
		// TODO Auto-generated method stub
//		return super.equals(o);
		if(this==o)
			return true;
		
		if(o instanceof Area)
		{
			Area other=(Area)o;
			if(other.level==this.level && other.id==this.id)
				return true;
			else
				return false;
		}
		else
			return false;
		
	}
	
	public int getAreaId()
	{
		return id;
	}
	
	public int getAreaLevel()
	{
		return this.level;
	}
	
	public Area getFatherArea()
	{
		return fatherArea;
	}
	
	public void setFatherArea(Area area)
	{
		fatherArea=area;
	}
	
	/**
	 * 设置区域为当前所在区域
	 */
	public void setCurrent()
	{
		isCurrent=true;
	}
	
	/**
	 * 获取该区域是否是当前所在区域
	 * @return
	 */
	public boolean isCurrent()	
	{
		return isCurrent;
	}
	
	/**
	 * 将该区域设置为非当前区域
	 */
	public void cancleCurrent()
	{
		isCurrent=false;
	}
	
	/**
	 * 将本区域的频率向量加入到计时器队列
	 */
	public void addTimeCount()
	{
		setCurrent();
		for(FrequencyVector vector:vectorlist)
		{
			TimeManager.getInstance().addVectorListen(vector);
		}
	}
	
	/**
	 * 将本区域的频率向量移除计时器队列
	 * 将本区域的频率向量的修改标识重置，这样即使这个区域再次移动进入了也不会出现不更新频率向量
	 */
	public void removeTimeCount()
	{
		cancleCurrent();
		for(FrequencyVector vector:vectorlist)
		{
			TimeManager.getInstance().removeVectorListen(vector);
			vector.resetChangeFVectorSign();//更新频率向量的修改标志位
		}
	}
	
	/**
	 * 对区域有关的频率向量改变相应的频率
	 * @param info:主要是利用AreaInfo里面的时间来改变频率相当当前时间段的频率
	 */
	public void changeFVector(AreaInfo info)
	{
		for(FrequencyVector vector:vectorlist)
		{
			vector.changeVector(info);
		}
		
		//递归向上调用区域改变频率向量的函数
		if(fatherArea!=null)
			fatherArea.changeFVector(info);
	}
	
	/**
	 * 作用 ：根据bundle的目的节点，查询当前区域及其父类区域中离目的地最近的区域（主要是尽可能低的区域层次）
	 * @param bundle :需要对比目的节点的bundle
	 * @return :如果父类区域中有和bundle的目的地同一层的区域;则返回该区域；如果没有则返回null;
	 */
	public Area checkBundleDestArea(Bundle bundle)
	{
		switch(this.getAreaLevel())
		{
		case AreaLevel.FIRSTLEVEL:
			if(this.getAreaId()==bundle.firstArea())
				return this;
			else
			{
				if(this.getFatherArea()==null)
					return null;
				else
					return this.getFatherArea().checkBundleDestArea(bundle);
			}
			
		case AreaLevel.SECONDLEVEL:
			if(this.getAreaId()==bundle.secondArea())
				return this;
			else
			{
				if(this.getFatherArea()==null)
					return null;
				else
					return this.getFatherArea().checkBundleDestArea(bundle);
			}
			
		case AreaLevel.THIRDLEVEL:
			if(this.getAreaId()==bundle.thirdArea())
				return this;
			else
			{
				if(this.getFatherArea()==null)
					return null;
				else
					return this.getFatherArea().checkBundleDestArea(bundle);
			}
			
		default:
			return null;
		}
		
	}
	
	/**
	 * @return : 获取该区域的频率向量列表
	 */
	public List<FrequencyVector> getFrequencyVectorList()
	{
		return vectorlist;
	}
	
	@Override
	public String toString() {
		String s="";
		s+=String.format("level and id:%s#%d,", level,id);
		
		for(FrequencyVector vector:vectorlist)
		{
			switch(vector.getFrequencyLevel())
			{
			case FrequencyVectorLevel.hourVector:
				s+=String.format("hourVector:(%s),", vector.toString());
				break;
				
			case FrequencyVectorLevel.weekVector:
				s+=String.format("weekVector:(%s),", vector.toString());
				break;
				
			case FrequencyVectorLevel.monthVector:
				s+=String.format("monthVector:(%s),", vector.toString());
				break;
			}
		}
		s+="\n";
		return s;
//		return super.toString();
	}
}
