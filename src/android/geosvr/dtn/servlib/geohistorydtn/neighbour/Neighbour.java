package android.geosvr.dtn.servlib.geohistorydtn.neighbour;

import java.io.Serializable;

import android.geosvr.dtn.servlib.bundling.BundlePayload;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVector;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVectorLevel;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVectorManager;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVectorServiceType;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.HourFrequencyVector;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.MonthFrequencyVector;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.WeekFrequencyVector;
import android.geosvr.dtn.servlib.geohistorydtn.timeManager.TimeManager;
import android.geosvr.dtn.servlib.naming.EndpointID;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-6-22 下午3:10:30 
 * 说明  :邻居类，用来表示DTN总的邻居
 */
public class Neighbour implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 12L;

	//邻居的EID
	EndpointID neighbourEid=null;
	
	//频率向量,与历史邻居相遇的频率向量
	HourFrequencyVector hourvector=null;
	WeekFrequencyVector weekvector=null;
	MonthFrequencyVector monthvector=null;
	
	//区域频率向量管理类
	NeighbourArea neighbourArea=null;
	
	Neighbour(EndpointID eid) {
		// TODO Auto-generated constructor stub
		this.neighbourEid=eid;
		int serviceType=FrequencyVectorServiceType.NEIGHBOUR;
		this.hourvector=(HourFrequencyVector)FrequencyVectorManager.getInstance().newFVector(FrequencyVectorLevel.hourVector, serviceType);
		this.weekvector=(WeekFrequencyVector)FrequencyVectorManager.getInstance().newFVector(FrequencyVectorLevel.weekVector, serviceType);
		this.monthvector=(MonthFrequencyVector)FrequencyVectorManager.getInstance().newFVector(FrequencyVectorLevel.monthVector, serviceType);
		
		//生成该邻居的历史区域向量记录
		init();
	}
	
	/**
	 * 根据收到的邻居的区域信息来更改区域记录
	 */
	public void generateArea(BundlePayload payload)
	{
//		neighbourArea=new NeighbourArea(neighbourEid, payload);
		if(neighbourArea==null)
		{
			neighbourArea=new NeighbourArea(neighbourEid);
		}
		neighbourArea.updateArea(payload);
	}
	
	/**
	 * 根据本地文件保存的邻居的区域信息来更改区域记录
	 */
	public void init()
	{
		neighbourArea=new NeighbourArea(neighbourEid);
//		neighbourArea
	}
	
	public NeighbourArea getNeighbourArea()
	{
		return neighbourArea;
	}
	
	/**
	 * 对向量在该时间段的分量进行处理
	 */
	public void checkVectorChange()
	{
		hourvector.changeVector();
		weekvector.changeVector();
		monthvector.changeVector();
	}
	
	/**
	 * 将频率向量加入到计时器中
	 */
	public void addTimeCount()
	{
		TimeManager.getInstance().addVectorListen(hourvector);
		TimeManager.getInstance().addVectorListen(weekvector);
		TimeManager.getInstance().addVectorListen(monthvector);
	}
	
	/**
	 * 将频率向量从计时器中移除
	 */
	public void removeTimeCount()
	{
		TimeManager.getInstance().removeVectorListen(hourvector);
		TimeManager.getInstance().removeVectorListen(weekvector);
		TimeManager.getInstance().removeVectorListen(monthvector);
	}
	
	/**
	 * 获取与历史邻居相遇的小时级别频率向量
	 */
	public FrequencyVector getHourFrequencyVector()
	{
		return hourvector;
	}
	
	/**
	 * 获取与历史邻居相遇的星期级别频率向量
	 */
	public FrequencyVector getWeekFrequencyVector()
	{
		return weekvector;
	}
	
	/**
	 * 获取与历史邻居相遇的月级别频率向量
	 */
	public FrequencyVector getMonthFrequencyVector()
	{
		return monthvector;
	}
	
/*	@Override
	public String toString() 
	{
		if(neighbourEid!=null)
			return neighbourEid.toString();
		else
			return null;
	}*/
}
