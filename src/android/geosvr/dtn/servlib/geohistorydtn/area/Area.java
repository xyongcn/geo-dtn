package android.geosvr.dtn.servlib.geohistorydtn.area;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVector;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVectorLevel;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVectorManager;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVectorServiceType;

/**
 * 作用:用来描述区域的对象
 * @author wwtao
 *
 */
public class Area 
{
	//区域分层的
	/*private int zeronum;
	private int firstnum;
	private int secondnum;
	private int thirdnum;*/
	
	
	/**
	 * 该端时间内是否被修改过
	 */
//	boolean hasChanged=false;
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
	 * 需要重新蛇者它的father
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
		if(this.equals(o))
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
	
	//对区域有关的频率向量改变相应的频率
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
}
