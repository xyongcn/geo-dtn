package android.geosvr.dtn.servlib.geohistorydtn.timeManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import android.geosvr.dtn.servlib.geohistorydtn.area.AreaManager;
import android.geosvr.dtn.servlib.geohistorydtn.config.FrequencyConfig;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVector;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVectorLevel;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVectorManager;
import android.util.Log;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-5-14 上午9:02:49 
 * 说明 :用来对各种倒数计时的事件管理的类，其实本质上是一个计时和相应事件触发的事件，计时线程和事件触发线程应当分离开
 */
public class TimeManager 
{
	private static String tag="TimeManager";
	
	private boolean timeTaskRun=false;
	
	//事件格式化
	SimpleDateFormat timeformat=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); 
	Date now;
	
	//计时器
	private static Timer time=new Timer();
	
	/**
	 * 配置文件中的时间流动，
	 */
	private Calendar configTime;
	/**
	 * 下一个计时事件
	 */
	private Calendar nextTime;
	
	private static class SingleTimeManager
	{
		static TimeManager instance=new TimeManager();
	}
	
	public static TimeManager getInstance()
	{
		return SingleTimeManager.instance;
	}
	
	
	//构造函数
	private TimeManager()	
	{
		configTime=Calendar.getInstance();
		nextTime=Calendar.getInstance();
		
		resetTime();
		
		hourFVectorQueue=new LinkedBlockingDeque<FrequencyVector>();
		weekFVectorQueue=new LinkedBlockingDeque<FrequencyVector>();
		monFVectorQueue=new LinkedBlockingDeque<FrequencyVector>();
		/*hourFVectorQueue=new LinkedBlockingDeque<FrequencyVector>();
		weekFVectorQueue=new LinkedBlockingDeque<FrequencyVector>();
		monFVectorQueue=new LinkedBlockingDeque<FrequencyVector>();*/
		
	}
	
	//每初次运行计时功能时，都要调用的代码
	public void init()	
	{
		
		//初次启用计时代码
		timeTaskRun=true;
		timeCount();
	}
	
	/**
	 * 对照FrequencyConfig里面的时间配置，重置计时器的双时间
	 */
	public void resetTime()
	{
		configTime.setTimeInMillis(FrequencyConfig.getInstance().getConfigTime());
		nextTime.setTimeInMillis(FrequencyConfig.getInstance().getConfigTime());
		
		minNum=configTime.get(Calendar.MINUTE);
		hourNum=configTime.get(Calendar.HOUR_OF_DAY);//时间的范围是0-23
		weekDayNum=configTime.get(Calendar.DAY_OF_WEEK)-1;//星期在日历中范围是1-7
		monthDayNum=configTime.get(Calendar.MONTH);//月的范围是0-12
	}
	
	public void shutdown()
	{
		//关闭计时器的调用
		timeTaskRun=false;
	}
	
	/**
	 * 小时级别的频率向量的通知
	 * FrequencyVectorLevel.hourVector
	 * @author wwtao
	 *
	 */
	BlockingQueue<FrequencyVector> hourFVectorQueue;
	
	/**
	 * 星期级别的频率向量
	 * FrequencyVectorLevel.weekVector
	 * @author wwtao
	 *
	 */
	BlockingQueue<FrequencyVector> weekFVectorQueue;
	
	/**
	 * 月级别的频率向量
	 * FrequencyVectorLevel.monthVector
	 */
	BlockingQueue<FrequencyVector> monFVectorQueue;
	
	/**
	 * 添加频率向量到计时队列
	 * @param vector:频率向量
	 */
	public void addVectorListen(FrequencyVector vector)
	{
		if(vector==null)
			return;
		
		switch(vector.getFrequencyLevel())
		{
		case FrequencyVectorLevel.hourVector:
			hourFVectorQueue.add(vector);
			break;
			
		case FrequencyVectorLevel.weekVector:
			weekFVectorQueue.add(vector);
			break;
			
		case FrequencyVectorLevel.monthVector:
			monFVectorQueue.add(vector);
			break;
		}
	}
	
	/**
	 * 移除计时队列的频率向量
	 * @param vector：频率向量
	 */
	public void removeVectorListen(FrequencyVector vector)
	{
		if(vector==null)
			return;
		
		switch(vector.getFrequencyLevel())
		{
		case FrequencyVectorLevel.hourVector:
			hourFVectorQueue.remove(vector);
			break;
			
		case FrequencyVectorLevel.weekVector:
			weekFVectorQueue.remove(vector);
			break;
			
		case FrequencyVectorLevel.monthVector:
			monFVectorQueue.remove(vector);
			break;
		}
	}
	
	/*public void addHourVectorListen(FrequencyVector hourVector)
	{
		
	}
	
	public void addWeekVetorListen(FrequencyVector weekVector)
	{
		
	}
	
	public void addMonVectorListen(FrequencyVector monVector)
	{
		
	}*/
	
	//测试使用，获取打印当前事件
	private String getTimeNow()
	{
		Date d=configTime.getTime();
		now=Calendar.getInstance().getTime();
		String s="month:"+monthDayNum+" week:"+weekDayNum+" hour:"+hourNum+"  ";
		String time=s+"路由算法中的时间："+timeformat.format(d)+"实际时间："+timeformat.format(now)+"  ";
		return time;
	}
	
	/**
	 * 测试使用，获取简单的时间，用来输出到日志上
	 * @return
	 */
	private String getSimplifyTime()
	{
		Date d=configTime.getTime();
		String s="time changed:"+timeformat.format(d)+"\n";
		return s;
	}
	
	//每小时需要触发的操作
	public void hourTask()	
	{
		//日志输出及提示信息
		Log.i(tag,getTimeNow()+"\t触发小时操作");
		
		for(FrequencyVector vector:hourFVectorQueue)
		{
			vector.changeVector();
		}
		
		//对所有的需要小时衰减的向量进行
		FrequencyVectorManager.getInstance().hourAttenuation();
		
		//写入时间区域频率向量随时间的变化
		AreaManager.writeAreaTimeChange2Log(getSimplifyTime());
		
		//将历史区域向量记录到文件中
		AreaManager.getInstance().wrieteAreaInfoToFile();
	}
	
	//每星期需要出发的操作
	public void weekTask()	
	{
		//日志输出及提示信息
		Log.i(tag,getTimeNow()+"\t触发周操作");
		AreaManager.writeAreaTimeChange2Log(getSimplifyTime());
		
		for(FrequencyVector vector:weekFVectorQueue)
		{
			vector.changeVector();
		}
		
		//对所有的需要小时衰减的向量进行
		FrequencyVectorManager.getInstance().weekAttenuation();
		
	}
	
	//每月需要触发的操作
	public void monthTask()
	{
		//日志输出及提示信息
		Log.i(tag,getTimeNow()+"\t触发月操作");
		AreaManager.writeAreaTimeChange2Log(getSimplifyTime());
		
		for(FrequencyVector vector:monFVectorQueue)
		{
			vector.changeVector();
		}
		
		//对所有的需要小时衰减的向量进行
		FrequencyVectorManager.getInstance().monthAttenuation();
	}
	
	//每隔1分钟倒数计时一次，这样计时器的最小时间就是一分钟
	public void timeCount()
	{
		long t=nextTime.getTimeInMillis();
		
		//同步时间,更新配置时间为计时到达的时间
		configTime.setTimeInMillis(t);
		
		//这里决定间隔时间，用到计时器里面是毫秒为单位
		long interval=TIMECOUNTINTERVAL*1000;
		t+=interval;
		nextTime.setTimeInMillis(t);
		
		//根据缩减倍率生成实际计时时间间隔
		long n=interval/((long)FrequencyConfig.getInstance().getZoom());
		
		mTimerTask timeTask=new mTimerTask();
		if(n>0)
			time.schedule(timeTask, n);
		else
		{
			time.schedule(timeTask, 0);
			Log.e(tag,"倒数计时器出现错误，计时事件为负");
		}
	}
	
	/**
	 * 计时器相关的参数
	 */
	int minNum=0;
	int hourNum=0;//当天的哪个时间
	int weekDayNum=0;//属于周几
	int monthDayNum=0;//当前月份
	
	public int getHourNum()
	{
		return hourNum;
	}
	
	public int getWeekDayNum()
	{
		return weekDayNum;
	}
	
	public int getMonthDayNum()
	{
		return monthDayNum;
	}
	
	boolean firstTime=true;
	/**
	 * 复写timertask计时器
	 */
	private class mTimerTask extends TimerTask
	{
		protected void resetCount()
		{
			
		}
		
		//执行相应的任务
		public void Task()
		{
			//小时级触发任务
			int hourNow=configTime.get(Calendar.HOUR_OF_DAY);
			if(hourNum!=hourNow)
			{
				hourNum=hourNow;
				//执行小时级触发的任务
				hourTask();
			}
			
			//星期级触发任务
			int weekDayNow=configTime.get(Calendar.DAY_OF_WEEK)-1;
			if(weekDayNum!=weekDayNow)
			{
				weekDayNum=weekDayNow;
				//对应星期级的任务触发
				weekTask();
			}
			
			//月级触发任务
			int monthDayNow=configTime.get(Calendar.MONTH);
			if(monthDayNum!=monthDayNow)
			{
				monthDayNum=monthDayNow;
				//对应月级的任务触发
				monthTask();
			}
		}
		
		@Override
		public void run() {
			/*if(firstTime)
			{
				minNum=nextTime.get(Calendar.MINUTE);
				hourNum=nextTime.get(Calendar.HOUR_OF_DAY);
				weekDayNum=nextTime.get(Calendar.DAY_OF_WEEK);
				monthDayNum=nextTime.get(Calendar.MONTH);
				
				firstTime=false;
			}*/
			
			//判断是否运行
			if(timeTaskRun)
			{
				timeCount();
				Task();
				this.cancel();
			}
			else
			{
				
			}
		}
	}
	
	
	/**
	 * 相关常量
	 */
	//没记计时器计时的时间差，单位是秒
	private long TIMECOUNTINTERVAL=60;
}
