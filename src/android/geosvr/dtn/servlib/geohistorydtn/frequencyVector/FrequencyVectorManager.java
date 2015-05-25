package android.geosvr.dtn.servlib.geohistorydtn.frequencyVector;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class FrequencyVectorManager {
	
	private static class SingleFrequencyVectorManager
	{
		static FrequencyVectorManager instance=new FrequencyVectorManager();
	}

	public static FrequencyVectorManager getInstance()
	{
		return SingleFrequencyVectorManager.instance;
	}
	
	public FrequencyVectorManager()
	{
		init();
	}
	
	private void init()
	{
		hourvectorlist=new LinkedList<FrequencyVector>();
		weekvectorlist=new LinkedList<FrequencyVector>();
		monthvectorlist=new LinkedList<FrequencyVector>();
//		hourvectorlist=new LinkedList<FrequencyVector>();
	}
	
	/**
	 * 存储管理时间向量的列表
	 */
	List<FrequencyVector> hourvectorlist;
	List<FrequencyVector> weekvectorlist;
	List<FrequencyVector> monthvectorlist;
//	List<FrequencyVector> hourvectorlist;
	
	/**
	 * 对向量进行衰减
	 */
	public void hourAttenuation()
	{
		Iterator<FrequencyVector> iter=hourvectorlist.iterator();
		while(iter.hasNext())
		{
			FrequencyVector vector=iter.next();
			vector.attenuationVector();
		}
	}
	public void weekAttenuation()
	{
		Iterator<FrequencyVector> iter=weekvectorlist.iterator();
		while(iter.hasNext())
		{
			FrequencyVector vector=iter.next();
			vector.attenuationVector();
		}
	}
	public void monthAttenuation()
	{
		Iterator<FrequencyVector> iter=monthvectorlist.iterator();
		while(iter.hasNext())
		{
			FrequencyVector vector=iter.next();
			vector.attenuationVector();
		}
	}
	
	//对外的公共方法
	/**
	 * 作用：由频率向量管理器来生成相关的向量
	 * @param vectorLevel
	 * @param serviceType
	 * @return
	 */
	public FrequencyVector newFVector(int vectorLevel,int serviceType)
	{
		FrequencyVector vector;
		if(vectorLevel==FrequencyVectorLevel.hourVector)
		{
			vector=new HourFrequencyVector(vectorLevel, serviceType);
			hourvectorlist.add(vector);
		}
		else if(vectorLevel==FrequencyVectorLevel.weekVector)
		{
			vector=new WeekFrequencyVector(vectorLevel, serviceType);
			weekvectorlist.add(vector);
		}
		else if(vectorLevel==FrequencyVectorLevel.monthVector)
		{
			vector=new MonthFrequencyVector(vectorLevel, serviceType);
			monthvectorlist.add(vector);
		}
		else
		{
			vector=null;
		}
		
		return vector;
	}
}
