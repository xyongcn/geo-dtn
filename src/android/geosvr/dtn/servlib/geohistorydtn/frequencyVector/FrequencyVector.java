package android.geosvr.dtn.servlib.geohistorydtn.frequencyVector;

import android.geosvr.dtn.servlib.geohistorydtn.area.AreaInfo;
import android.geosvr.dtn.servlib.geohistorydtn.config.AttenuationConfig;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-5-5 上午11:37:50 
 * 说明 ：时间区域频率向量的基类
 */
public abstract class FrequencyVector {

	/**
	 * @author wwtao
	 * 时间向量的级别
	 */
	int vectorLevel;
	
	/**
	 * 该事件向量服务的类型，服务类型和向量级别共同决定衰减类型：比如服务于邻居的，或者服务于区域的
	 */
	int serviceType;
	
	
	/**
	 * 时间向量的值
	 */
	double[] vector;
	
	/**
	 * 向量长度
	 */
	int vectorLength;
	
	/**
	 * 向量变动标志位
	 */
	boolean[] vectorChange;
	
	/**
	 * 触发计时事件类型
	 */
//	int 
	
	public FrequencyVector(int vectorLevel ,int serviceType)
	{
		this.vectorLevel=vectorLevel;
		this.serviceType=serviceType;
	}
	
	/**
	 * 获取向量的值
	 */
	public double[] getVector()
	{
		return vector;
	}
	
	/**
	 * 对当前时间段的向量进行处理
	 */
	public abstract void changeVector(AreaInfo info);
	
	/**
	 * 衰减函数
	 */
	public void attenuationVector()
	{
		//衰减
		double parameter=AttenuationConfig.getAttenuation(this.vectorLevel,this.serviceType);
		for(int i=0;i<vector.length;i++)
		{
			vector[i]=vector[i]*parameter;
		}
		
		//清楚修改标志位
		for(int i=0;i<vectorChange.length;i++)
		{
			vectorChange[i]=false;
		}
	}
	
}
