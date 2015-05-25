package android.geosvr.dtn.servlib.geohistorydtn.config;

import java.util.Calendar;
import java.util.Timer;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-5-14 上午9:26:17 
 * 说明 ：用来配置和频率向量有关的参数
 */
public class FrequencyConfig {
	
	//实际时间与已有记录时间的倍率。倍率为大于1的整数，已有的记录的时间差除以倍率就可得到现在比较的时间
	private int timeZoom;
	
	//当前时间,真实系统采用当前设备时间，在真是环境模拟的时候采用记录时间
	private long timeNow;
	
	/**
	 * 默认的初始化函数，后续可以引进从配置文件读取相应的值
	 */
	private FrequencyConfig()
	{
		timeZoom=60;
//		Timer t=new Timer();
		//获取当前的系统时间，已毫秒为单位
		timeNow=Calendar.getInstance().getTimeInMillis();
	}
	
//	private static FrequencyConfig instance=null;
	
	private static class SingleFrequencyConfig
	{
		static FrequencyConfig instance=new FrequencyConfig();
	}
	
	public static FrequencyConfig getInstance()
	{
		return SingleFrequencyConfig.instance;
	}
	
	public int getZoom()
	{
		return timeZoom;
	}
	
	public long getConfigTime()
	{
		return timeNow;
	}
}
