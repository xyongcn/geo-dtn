package android.geosvr.dtn.servlib.geohistorydtn.test;

import android.geosvr.dtn.servlib.geohistorydtn.area.Area;
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
	public void testTimeManager()
	{
		TimeManager tmanager=TimeManager.getInstance();
		FrequencyVector vector=FrequencyVectorManager.getInstance().newFVector(FrequencyVectorLevel.hourVector,FrequencyVectorServiceType.AREA);
		tmanager.addVectorListen(vector);
		
		int seconds=0;
		for(;seconds<600;seconds++)
		{
			System.out.println("第"+seconds+"秒，vector:  "+vector.toString());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
