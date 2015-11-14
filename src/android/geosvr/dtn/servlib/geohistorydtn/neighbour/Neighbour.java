package android.geosvr.dtn.servlib.geohistorydtn.neighbour;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.io.StreamCorruptedException;

import android.geosvr.dtn.servlib.bundling.BundlePayload;
import android.geosvr.dtn.servlib.geohistorydtn.area.Area;
import android.geosvr.dtn.servlib.geohistorydtn.config.NeibhourConfig;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVector;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVectorLevel;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVectorManager;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVectorServiceType;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.HourFrequencyVector;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.MonthFrequencyVector;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.WeekFrequencyVector;
import android.geosvr.dtn.servlib.geohistorydtn.log.GeohistoryLog;
import android.geosvr.dtn.servlib.geohistorydtn.timeManager.TimeManager;
import android.geosvr.dtn.servlib.naming.EndpointID;
import android.util.Log;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-6-22 下午3:10:30 
 * 说明  :邻居类，用来表示DTN总的邻居
 */
public class Neighbour implements Serializable
{
	private static String tag="Neighbour";
	
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
	
	Neighbour(EndpointID eid) throws Exception{
		if(eid==null)
			throw new Exception("the EndpointID is null while generate a new Neighbour");
		
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
	 * 由于暂时不能直接从payload里面获取内存信息，所有打算弃用该方法
	 */
	/*public void generateArea(BundlePayload payload)
	{
		if(neighbourArea==null)
		{
			neighbourArea=new NeighbourArea(neighbourEid);
		}
		else
			neighbourArea.updateArea(payload);
	}*/
	
	/**
	 * 根据邻居的bundle的payload来生成自己的
	 * @param payloadFile
	 */
	public void generateArea()
	{
//		neighbourArea=new NeighbourArea(neighbourEid, payload);
		if(neighbourArea==null)
		{
			neighbourArea=new NeighbourArea(neighbourEid);
		} else{
			try {
				File neighAreaFile=new File(NeibhourConfig.NEIGHBOURAREAFILEDIR+neighbourEid.toString());
				if(!neighAreaFile.exists()){
					GeohistoryLog.w(tag,String.format("没有找到%s的payloadFile来更新邻居区域移动频率",neighbourEid.toString()));
					return;
				}
				neighbourArea.updateArea(neighAreaFile);
			} catch (StreamCorruptedException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 根据本地文件保存的邻居的区域信息来更改区域记录
	 */
	public void init()
	{
		neighbourArea=new NeighbourArea(neighbourEid);
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
	 * 将频率向量加入到计时器中(没有被调用，应该需要添加)
	 * 这里是让计时器来每段时间都改变一下频率向量，改变小时频率向量；星期频率向量；月频率向量
	 */
	public void addTimeCount()
	{
		GeohistoryLog.i(tag, String.format("neighbour(%s) add fvector to timeManager to counting", this.neighbourEid.toString()));
		TimeManager.getInstance().addVectorListen(hourvector);
		TimeManager.getInstance().addVectorListen(weekvector);
		TimeManager.getInstance().addVectorListen(monthvector);
	}
	
	/**
	 * 将Neighbour中的所有频率向量添加到频率向量管理器中，这样从文件中读取邻居时，频率向量的衰减器也可以对其进行频率的衰减了
	 */
	public void addAreaFVector2Manager(){
		FrequencyVectorManager.getInstance().addFVector(hourvector);
		FrequencyVectorManager.getInstance().addFVector(weekvector);
		FrequencyVectorManager.getInstance().addFVector(monthvector);
	}
	
	/**
	 * 将频率向量从计时器中移除
	 */
	public void removeTimeCount()
	{
		GeohistoryLog.i(tag, String.format("neighbour(%s) remove fvector from timeManager", this.neighbourEid.toString()));
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
	
	/**
	 * 获取该邻居的EID
	 * @return
	 */
	public EndpointID getEid()
	{
		return neighbourEid;
	}
	
	/**
	 * 输出保存的区域的星期
	 */
	public void printAreaInfo(){
		GeohistoryLog.i(tag,String.format("打印邻居（%s）的区域移动规律：",neighbourEid.toString()));
		int i=1;
		for(Area area:neighbourArea.areaMap.values()){
			GeohistoryLog.d(tag,String.format("第%d个区域：\n%s",i++,area.toString()));
		}
	}
	
	@Override
	public String toString(){
		if(neighbourEid!=null){
			StringBuilder s=new StringBuilder();
			s.append(String.format("neighbour_eid:%s,\n", neighbourEid.toString()));
			s.append(String.format("hourVector:(%s),\n", hourvector.toString()));
			s.append(String.format("weekVector:(%s),\n", weekvector.toString()));
			s.append(String.format("monthVector:(%s)\n", monthvector.toString()));
			return s.toString();
		}
		else
			return "neighbour_eid:null";
	}
}
