package android.geosvr.dtn.servlib.geohistorydtn.test;

import java.util.Random;

import android.geosvr.dtn.servlib.geohistorydtn.routing.ChanceValueCompute;

import junit.framework.TestCase;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-8-3 下午4:39:41 
 * 说明 :测试路由算法中计算机会值的类，routing.ChanceValueCompute
 */
public class Test_routing_ChanceValueCompute extends TestCase
{
	public void test_vectorSum()
	{
		Random rand=new Random();
		double input[]=new double[rand.nextInt(20)];
		double sum=0;
		for(int i=0;i<input.length;i++)
		{
			double temp=rand.nextDouble()/((double)2*input.length);
			sum+=temp;
			input[i]=temp;
		}
		
		double result=ChanceValueCompute.vectorSum(input);
		assertEquals(result, sum);
	}
}
