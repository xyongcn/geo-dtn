package android.geosvr.dtn.servlib.geohistorydtn.test;

import java.util.Calendar;

import android.geosvr.dtn.servlib.geohistorydtn.area.Area;
import android.geosvr.dtn.servlib.geohistorydtn.config.AttenuationConfig;
import android.geosvr.dtn.servlib.geohistorydtn.config.FrequencyConfig;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVector;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVectorLevel;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVectorManager;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVectorServiceType;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.HourFrequencyVector;
import android.geosvr.dtn.servlib.geohistorydtn.timeManager.TimeManager;
import android.widget.SlidingDrawer;
import junit.framework.TestCase;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-8-7 下午1:56:29 
 * 说明 :单元测试计时功能
 */
public class Test_timeManager extends TestCase{
	
	/**
	 * 对区域的向量计时进行测试
	 * 区域，0.101.10.1 -> 0.102.10.1 -> 0.113.11.1 -> 0.214.21.2 -> 0.101.10.1  
	 */
	/*
	public void testAreaTimeCount()
	{
		//第一层区域
		Area area101=new Area(1, 101);
		Area area102=new Area(1, 102);
		Area area113=new Area(1, 113);
		Area area214=new Area(1, 214);
		
		//第二层区域
		Area area10=new Area(2, 10);
		Area area11=new Area(2, 11);
		Area area21=new Area(2, 21);
		
		//第三层区域
		Area area1=new Area(3, 1);
		Area area2=new Area(3, 2);
		
		//区域移动
		area101.addTimeCount();
//		area101.
		double[] vector=area101.getFrequencyVectorList().get(0).getVector();
		String s="";
		for(int i=0;i<vector.length;i++)
			s+=String.valueOf(vector[i])+",";
		System.out.println("vector:"+s);
		
		try {
			Thread.sleep(50000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		s="";
		vector=area101.getFrequencyVectorList().get(0).getVector();
		for(int i=0;i<vector.length;i++)
			s+=String.valueOf(vector[i])+",";
		System.out.println("vector:"+s);
	}
	*/
	
	/**
	 * 测试小时的计时器和衰减器
	 */
	public void testTimeManager()
	{
		Calendar configTime=Calendar.getInstance();
		configTime.setTimeInMillis(0);
		configTime.set(Calendar.YEAR, 2015);
		configTime.set(Calendar.MONTH, 6);
		configTime.set(Calendar.DAY_OF_MONTH, 31);
		configTime.set(Calendar.HOUR_OF_DAY, 23);
		configTime.set(Calendar.MINUTE, 57);
		FrequencyConfig.getInstance().setConfigTime(configTime.getTimeInMillis());
		
		TimeManager tmanager=TimeManager.getInstance();
		tmanager.resetTime();
		FrequencyVector vector1=FrequencyVectorManager.getInstance().newFVector(FrequencyVectorLevel.hourVector,FrequencyVectorServiceType.AREA);
		FrequencyVector vector2=FrequencyVectorManager.getInstance().newFVector(FrequencyVectorLevel.hourVector,FrequencyVectorServiceType.AREA);
		FrequencyVector weekvector1=FrequencyVectorManager.getInstance().newFVector(FrequencyVectorLevel.weekVector,FrequencyVectorServiceType.AREA);
		FrequencyVector weekvector2=FrequencyVectorManager.getInstance().newFVector(FrequencyVectorLevel.weekVector,FrequencyVectorServiceType.AREA);
		FrequencyVector monthvector1=FrequencyVectorManager.getInstance().newFVector(FrequencyVectorLevel.monthVector,FrequencyVectorServiceType.AREA);
		FrequencyVector monthvector2=FrequencyVectorManager.getInstance().newFVector(FrequencyVectorLevel.monthVector,FrequencyVectorServiceType.AREA);
		
		for(int i=0;i<vector2.getVector().length;i++)
		{
			vector2.getVector()[i]=i;
		}
		
		for(int i=0;i<weekvector2.getVector().length;i++)
		{
			weekvector2.getVector()[i]=i;
		}
		
		for(int i=0;i<monthvector2.getVector().length;i++)
		{
			monthvector2.getVector()[i]=i;
		}
		
		tmanager.addVectorListen(vector1);
		tmanager.addVectorListen(weekvector1);
		tmanager.addVectorListen(monthvector1);
		
//		System.out.println(String.format("计时前：%s", vector1.toString()));
		
		
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		int hournum=tmanager.getHourNum();
		int weeknum=tmanager.getWeekDayNum();
		int monthnum=tmanager.getMonthDayNum();
		
		System.out.println(String.format("hournum:%d , 星期向量：%s", hournum,vector1.toString()));
		System.out.println(String.format("weeknum:%d , 星期向量：%s", weeknum,weekvector1.toString()));
		System.out.println(String.format("monthnum:%d , 月向量：%s", monthnum,monthvector1.toString()));
		
		try {
			Thread.sleep(10*1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		System.out.println(String.format("计时后：%s", vector1.toString()));
		hournum=tmanager.getHourNum();
		double attenuation=AttenuationConfig.getAttenuation(vector1.getFrequencyLevel(), vector1.getServiceType());
		double result=1d*AttenuationConfig.getAttenuation(vector1.getFrequencyLevel(), vector1.getServiceType());
		System.out.println(String.format("attenuation:%f , result:%f , realResult:%f ,hournum:%d", attenuation,result,vector1.getVector()[hournum],hournum));
		assertEquals(vector1.getVector()[hournum], attenuation);//判断计时和衰减
		
		//判断另一个向量的衰减
		assertEquals(vector2.getVector()[hournum], attenuation*hournum);
		
		
		weeknum=tmanager.getWeekDayNum();
		monthnum=tmanager.getMonthDayNum();
		double weekattenuation=AttenuationConfig.getAttenuation(weekvector1.getFrequencyLevel(), weekvector1.getServiceType());
		double monthattenuation=AttenuationConfig.getAttenuation(monthvector1.getFrequencyLevel(), monthvector1.getServiceType());
		
		System.out.println(String.format("hournum:%d , 星期向量：%s", hournum,vector1.toString()));
		System.out.println(String.format("weeknum:%d , 星期向量：%s", weeknum,weekvector1.toString()));
		System.out.println(String.format("monthnum:%d , 月向量：%s", monthnum,monthvector1.toString()));
		assertEquals(weekvector1.getVector()[weeknum], weekattenuation);
		assertEquals(monthvector1.getVector()[monthnum], monthattenuation);
		
		assertEquals(weekvector2.getVector()[weeknum], weekattenuation*weeknum);
		assertEquals(monthvector2.getVector()[monthnum], monthattenuation*monthnum);
	}

}
