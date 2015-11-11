package android.geosvr.dtn.servlib.geohistorydtn.area;

import android.geosvr.dtn.servlib.geohistorydtn.areaConnectiveSimulation.questAreaInfo.AreaLayerInfo;
import android.geosvr.dtn.servlib.geohistorydtn.timeManager.TimeManager;

/**
 * 作用：用来表示一个区域变化的必要信息
 * @author thedevilking
 * areaId,从0到3，0就是最底层区域的id，从1到3已经是从底层到高层的id
 */
public class AreaInfo {

	/**
	 * 各个区域层次的id，从底层到到高层，范围依次扩大
	 */
	int[] areaId;
	
	int hour;
	int month;
	int week;
	
	/**
	 * 底层区域的数组位置,在默认情况下，区域的向量是四维的，最底层是区域标识id，和最底层区域的唯一id一样，一旦区域标识id不同，则表示一定移动了区域
	 */
	int baseAreaNum=1;
	
	/**
	 * 底层区域的对应的level
	 */
	int baseAreaLevel=AreaLevel.FIRSTLEVEL;
	
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
	
	/**
	 * 由于程序中的分层信息有是否三层的错误校验，所以这里的分层信息只设置三层
	 */
	public AreaInfo(AreaLayerInfo layerinfo){
		//由于layerinfo的区域分层信息是从上层到底层排列，而DTN程序的区域分层信息是从底层到上层排列，所以需要转换
		this.areaId=new int[4];
		for(int i=1;i<4;i++){
			areaId[i]=layerinfo.get(layerinfo.size()-i);
		}
		this.areaId[0]=layerinfo.get(layerinfo.size()-1);
		
		//获取当前时间
		this.hour=TimeManager.getInstance().getHourNum();
		this.week=TimeManager.getInstance().getWeekDayNum();
		this.month=TimeManager.getInstance().getMonthDayNum();
	}
	
	/**
	 * 说明：根据另一个AreaInfo来更新当前类的时间
	 * @param another
	 */
	public void updataTime(AreaInfo another){
		if(another!=null){
			this.hour=another.hour;
			this.week=another.week;
			this.month=another.month;
		}
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
	
	//判断两个AreaInfo所表示的分层信息是否正确
	@Override
	public boolean equals(Object o) {
		if(o==null)
			return false;
		if(this==o)
			return true;
		if(!(o instanceof AreaInfo)){
			return false;
		}
		
		AreaInfo another=(AreaInfo)o;
		boolean isEqual=true;
		
		isEqual=baseAreaLevel==another.baseAreaLevel && baseAreaNum==another.baseAreaNum;
		
		if(!isEqual)
			return false;
		
		for(int i=0;i<areaId.length;i++){
			if(areaId[i]!=another.areaId[i]){
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * 返回默认的区域分层信息，由于是默认的，所以一般作为邻居间交换区域信息时，填充使用
	 * @return
	 */
	public static int[] defaultAreaId(){
		int id[]=new int[4];
		for(int i=0;i<id.length;i++){
			id[i]=-1;
		}
		return  id;
	}
}
