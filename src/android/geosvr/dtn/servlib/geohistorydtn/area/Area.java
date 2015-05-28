package android.geosvr.dtn.servlib.geohistorydtn.area;

import java.util.ArrayList;
import java.util.Collection;
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
	 */
	List<Area> childrenlist;
	
	public Area(int level,int id,Area fatherArea,Collection<Area> childarea)
	{
		this.level=level;
		this.id=id;
		
		this.fatherArea=fatherArea;
		this.childrenlist.addAll(childarea);
		
		init();
	}
	
	private void init()	
	{
		vectorlist=new ArrayList<FrequencyVector>();
		
		/**
		 * 加入频率向量
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
	 * 用来判断是不是在该区域里面
	 * @param longitude
	 * @param latitude
	 * @return
	 */
	public boolean isIntheArea(double longitude,double latitude)
	{
		return true;
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
	
	
}
