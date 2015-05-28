package android.geosvr.dtn.servlib.geohistorydtn.area;

import android.geosvr.dtn.servlib.geohistorydtn.timeManager.TimeManager;

/**
 * 作用：用来表示一个区域变化的必要信息
 * @author thedevilking
 *
 */
public class AreaInfo {

	/**
	 * 各个区域层次的id，从底层到底层
	 */
	int[] areaId;
	
	int hour;
	int month;
	int week;
	
	/**
	 * 底层区域的数组位置
	 */
	int baseAreaNum=1;
	
	/**
	 * 底层区域的对应的level
	 */
	int baseAreaLevel=1;
	
	public AreaInfo(int[] areaId)
	{
		this.areaId=areaId;
		
		//获取当前时间
		this.hour=TimeManager.getInstance().getHourNum();
		this.week=TimeManager.getInstance().getWeekDayNum();
		this.month=TimeManager.getInstance().getMonthDayNum();
	}
	
	public AreaInfo(int[] areaId,int hour,int week,int month)
	{
		this.areaId=areaId;
		
		this.hour=hour;
		this.week=week;
		this.month=month;
	}
	
	
	public int[] getAreaId()
	{
		return areaId;
	}
	
	public int getHour()
	{
		return hour;
	}
	
	public int getMonth()
	{
		return month;
	}
	
	public int getWeek()
	{
		return week;
	}
	
	//返回底层的区域的id
	public int getBaseAreaId()
	{
		return areaId[baseAreaNum];
	}
	
	//获取底层区域id所在数组的位置
	public int getBaseAreaNum()
	{
		return baseAreaNum;
	}
	
	//获取底层区域的level等级
	public int getBaseAreaLevel()
	{
		return baseAreaLevel;
	}
}
