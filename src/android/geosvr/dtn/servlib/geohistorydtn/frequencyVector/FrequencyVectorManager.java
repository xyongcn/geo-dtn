package android.geosvr.dtn.servlib.geohistorydtn.frequencyVector;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.geosvr.dtn.servlib.geohistorydtn.log.GeohistoryLog;

public class FrequencyVectorManager {
	private static String tag="FrequencyVectorManager";
	
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
		hourvectorlist=new LinkedList<FrequencyVector>();
		weekvectorlist=new LinkedList<FrequencyVector>();
		monthvectorlist=new LinkedList<FrequencyVector>();
		init();
	}
	
	private void init()
	{
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
		GeohistoryLog.i(tag, "All Frequency Vector Attenuation:"+hourvectorlist.size());
		Iterator<FrequencyVector> iter=hourvectorlist.iterator();
		while(iter.hasNext())
		{
			FrequencyVector vector=iter.next();
			vector.attenuationVector();
			GeohistoryLog.v(tag, "Frequency Vector Attenuation");
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
		GeohistoryLog.i(tag, String.format("FrequencyVectorManager create new Frequency vector,hourlistsize:%d,weeklistsize:%d,monthlistsize:%d"
				,hourvectorlist.size(),weekvectorlist.size(),monthvectorlist.size()));
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
	
	/**
	 * 针对从文件中读取到的区域的频率向量或者邻居频率向量，将其添加到向量列表中
	 */
	public void addFVector(FrequencyVector vector){
		//根据要添加的频率向量的级别来添加
		switch(vector.vectorLevel){
		case FrequencyVectorLevel.hourVector:
			hourvectorlist.add(vector);
			break;
			
		case FrequencyVectorLevel.weekVector:
			weekvectorlist.add(vector);
			break;
			
		case FrequencyVectorLevel.monthVector:
			monthvectorlist.add(vector);
			break;
		}
	}
	
	/**
	 * 对频率向量管理器所存储的频率向量引用清空
	 */
	public void shutdown(){
		
		hourvectorlist.clear();
		weekvectorlist.clear();
		monthvectorlist.clear();
	}
}
