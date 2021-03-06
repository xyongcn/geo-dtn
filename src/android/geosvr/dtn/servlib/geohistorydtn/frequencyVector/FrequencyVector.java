package android.geosvr.dtn.servlib.geohistorydtn.frequencyVector;

import java.io.Serializable;

import android.geosvr.dtn.servlib.geohistorydtn.area.AreaInfo;
import android.geosvr.dtn.servlib.geohistorydtn.config.AttenuationConfig;
import android.geosvr.dtn.servlib.geohistorydtn.config.DisplayConfig;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-5-5 上午11:37:50 
 * 说明 ：时间区域频率向量的基类
 * 将向量的增加标志位的清除放在了衰减函数attenuationVector里面
 */
public abstract class FrequencyVector implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 11L;

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
	
	
	
	FrequencyVector(int vectorLevel ,int serviceType)
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
	 * 获取向量等级
	 */
	public int getFrequencyLevel()
	{
		return vectorLevel;
	}
	
	/**
	 * 获取服务的对象
	 */
	public int getServiceType()
	{
		return this.serviceType;
	}
	
	/**
	 * 对更换区域时，处理当前时间段的向量进行处理；因为Info里面存放的包括改变区域时的时间，所以要根据这个info里面的时间来比较需要修改哪一维的频率向量
	 */
	public abstract void changeVector(AreaInfo info);
	
	/**
	 * 对到达计时时间后，处理当前时间段的向量
	 */
	public abstract void changeVector();
	
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
		
		resetChangeFVectorSign();
	}
	
	/**
	 * 重置修改频率向量的修改标志位，这个在区域移动，重置衰减时间
	 */
	public void resetChangeFVectorSign(){

		//清楚修改标志位
		for(int i=0;i<vectorChange.length;i++)
		{
			vectorChange[i]=false;
		}
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		String s=String.valueOf(vector[0]);
		
		for(int i=1;i<vector.length;i++)
		{
			s+=":"+DisplayConfig.decimalFormat.format(vector[i]);
		}
		
		return s;
//		return super.toString();
	}
}
