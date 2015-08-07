package android.geosvr.dtn.servlib.geohistorydtn.test;

import android.geosvr.dtn.servlib.geohistorydtn.area.Area;
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
		Area area2=new Area(2, 2);
	}
}
