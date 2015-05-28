package android.geosvr.dtn.servlib.geohistorydtn.timeManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import android.geosvr.dtn.servlib.geohistorydtn.config.FrequencyConfig;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVector;
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
//		Locale local=Calendar.getInstance().
//		configTime=Calendar.getInstance().set
		configTime=Calendar.getInstance();
		nextTime=Calendar.getInstance();
		
		//初始化
		init();
	}
	
	public void init()	
	{
		hourFVectorQueue=new LinkedBlockingDeque<FrequencyVector>();
		weekFVectorQueue=new LinkedBlockingDeque<FrequencyVector>();
		monFVectorQueue=new LinkedBlockingDeque<FrequencyVector>();
		/*hourFVectorQueue=new LinkedBlockingDeque<FrequencyVector>();
		weekFVectorQueue=new LinkedBlockingDeque<FrequencyVector>();
		monFVectorQueue=new LinkedBlockingDeque<FrequencyVector>();*/
		
		//初次启用计时代码
		timeTaskRun=true;
		timeCount();
	}
	
	public void destroy()
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
	 * 添加时间向量
	 * @param hourVector
	 */
	public void addHourVectorListen(FrequencyVector hourVector)
	{
		
	}
	
	public void addWeekVetorListen(FrequencyVector weekVector)
	{
		
	}
	
	public void addMonVectorListen(FrequencyVector monVector)
	{
		
	}
	
	//测试使用，获取打印当前事件
	private String getTimeNow()
	{
		Date d=configTime.getTime();
		now=Calendar.getInstance().getTime();
		String s="month:"+monthDayNum+" week:"+weekDayNum+" hour:"+hourNum+"  ";
		String time=s+"路由算法中的时间："+timeformat.format(d)+"实际时间："+timeformat.format(now)+"  ";
		return time;
	}
	
	//每小时需要触发的操作
	public void hourTask()	
	{
		Log.i(tag,getTimeNow()+"\t触发小时操作");
	}
	
	//每星期需要出发的操作
	public void weekTask()	
	{
		Log.i(tag,getTimeNow()+"\t触发周操作");
	}
	
	//每月需要触发的操作
	public void monthTask()
	{
		Log.i(tag,getTimeNow()+"\t触发月操作");
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
	
	
	//time计时器
	/**
	 * 小时向量的time计时器,这是计时的基本单位
	 * @author wwtao
	 *
	 */
	/*TimerTask TimeTask=new TimerTask() {
		
		int minNum=0;
		int hourNum=0;
		int weekDayNum=0;
		int monthDayNum=0;
		
		boolean firstTime=true;
		
		//执行相应的任务
		public void Task()
		{
			//小时级触发任务
			int hourNow=configTime.get(Calendar.HOUR);
			if(hourNum!=hourNow)
			{
				hourNum=hourNow;
				//执行小时级触发的任务
				hourTask();
			}
			
			//星期级触发任务
			int weekDayNow=configTime.get(Calendar.DAY_OF_WEEK);
			if(weekDayNum!=weekDayNow)
			{
				weekDayNum=weekDayNow;
				//对应星期级的任务触发
				weekTask();
			}
			
			//月级触发任务
			int monthDayNow=configTime.get(Calendar.DAY_OF_MONTH);
			if(monthDayNum!=monthDayNow)
			{
				monthDayNum=monthDayNow;
				//对应月级的任务触发
				monthTask();
			}
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(firstTime)
			{
				minNum=nextTime.get(Calendar.MINUTE);
				hourNum=nextTime.get(Calendar.HOUR);
				weekDayNum=nextTime.get(Calendar.DAY_OF_WEEK);
				monthDayNum=nextTime.get(Calendar.MONTH);
				
				firstTime=false;
			}
			
			//判断是否运行
			if(timeTaskRun)
			{
				timeCount();
				Task();
			}
			else
			{
				
			}
		}
	};*/
	
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
			int weekDayNow=configTime.get(Calendar.DAY_OF_WEEK);
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
			// TODO Auto-generated method stub
			if(firstTime)
			{
				minNum=nextTime.get(Calendar.MINUTE);
				hourNum=nextTime.get(Calendar.HOUR_OF_DAY);
				weekDayNum=nextTime.get(Calendar.DAY_OF_WEEK);
				monthDayNum=nextTime.get(Calendar.MONTH);
				
				firstTime=false;
			}
			
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
