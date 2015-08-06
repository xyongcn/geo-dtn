package android.geosvr.dtn.servlib.geohistorydtn.routing;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.geosvr.dtn.servlib.bundling.Bundle;
import android.geosvr.dtn.servlib.bundling.DTNTime;
import android.geosvr.dtn.servlib.geohistorydtn.area.Area;
import android.geosvr.dtn.servlib.geohistorydtn.area.AreaLevel;
import android.geosvr.dtn.servlib.geohistorydtn.config.ChanceComputeConfig;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVector;
import android.geosvr.dtn.servlib.geohistorydtn.frequencyVector.FrequencyVectorLevel;
import android.geosvr.dtn.servlib.geohistorydtn.log.GeohistoryLog;
import android.geosvr.dtn.servlib.geohistorydtn.neighbour.Neighbour;
import android.geosvr.dtn.systemlib.util.TimeHelper;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-7-3 下午7:29:00 
 * 说明  ：用来作为路由选择时计算机会值的类
 * 机会值计算公式： 	1.根据bundle的失效时间和当前时间计算出三个不同尺度的有效时间向量，如果某一时间段bundle有效则对应分量为1，如果无效则为0
 * 				2.将不同尺度的有效时间向量乘以不同尺度的频率向量（邻居或者区域），得到的不同尺度的机会值，
 * 					当所需要的尺度的机会值大于相应的阈值的时候，把各个尺度机会值相加，然后返回.如果期间的有效时间向量超时或者其他的错误，则返回-1;
 */
public class ChanceValueCompute 
{
	private static final String tag="ChanceValueCompute";
	
	/**
	 * 节点携带到达目的地的机会值
	 * @param bundle :为需要发送的bundle
	 * @param area :计算携带到给区域(该区域一般是两个节点)的机会值，包括本区域以及上层区域的机会值。
	 * @return double[] :携带到本层及其父类的区域的机会值；如果存在且合法返回向量；如果不合法返回null 
	 * 在每一层区域的机会值计算中，如果计算出来的机会值向量所需要的分量大于阈值，则返回机会值；否则返回-1；
	 */
	public static double[] carryChance(Bundle bundle,Area area)
	{
		ValidVector validVector=getValidVector(bundle);
		
		List<Double> chanceValue=new ArrayList<Double>(5);
		while(area!=null)
		{
			double temp=carryChance(validVector, area);
			//出现不合法的机会值
			/*if(temp==-1)
				return null;*/
			chanceValue.add(temp);
			area=area.getFatherArea();
		}
		
		double[] result=new double[chanceValue.size()];
		for(int i=0;i<chanceValue.size();i++)
		{
			result[i]=chanceValue.get(i);
		}
		
		return result;
		/*BundleRouter geohistoryRouter=BundleDaemon.getInstance().router();
		//不是GeohistroyRouter,不能计算机会值
		if(!(geohistoryRouter instanceof GeoHistoryRouter))
		{
			GeohistoryLog.e(tag, String.format("路由算法不是GeoHistroyRouter"));
			return -1;
		}
		Area nowArea=((GeoHistoryRouter)geohistoryRouter).getNowArea();*/
		
		
//		double[] hourChanceValue=
	}
	
	/**
	 * 节点通过历史邻居携带到达目的地的机会值
	 * @param bundle :为需要发送的bundle
	 * @param area :计算携带到给区域(该区域一般是两个节点)的机会值，包括本区域以及上层区域的机会值。
	 * @param neighbour :历史邻居的neighbour，用来提供与历史邻居相遇的频率向量
	 * @return double[] :携带到本层及其父类的区域的机会值；如果存在且合法返回向量；如果不合法返回null 
	 * 在每一层区域的机会值计算中，如果计算出来的机会值向量所需要的分量大于阈值，则返回机会值；否则返回-1；
	 */
	public static double[] histroyNeighbourCarryChance(Bundle bundle,Area area,Neighbour neighbour)
	{
		ValidVector validVector=getValidVector(bundle);
		
		List<Double> chanceValue=new ArrayList<Double>(5);
		while(area!=null)
		{
			chanceValue.add(historyNeighbourcarryChance(validVector, area, neighbour));
			area=area.getFatherArea();
		}
		
		double[] result=new double[chanceValue.size()];
		for(int i=0;i<chanceValue.size();i++)
		{
			result[i]=chanceValue.get(i);
		}
		
		return result;
	}
	
	/**
	 * 作用：	计算携带到Area区域的机会值
	 * @param validVector :bundle的时效向量
	 * @param area
	 * @return
	 */
	public static double carryChance(ValidVector validVector,Area area)
	{
		List<FrequencyVector> vectorList=area.getFrequencyVectorList();
		
		//到达area区域的机会值总和
		double chanceValue=0d;
		
		//到达area区域，不同尺度机会值向量
		double[] temp;
		
		for(FrequencyVector vector:vectorList)
		{
			switch(vector.getFrequencyLevel())
			{
			case FrequencyVectorLevel.hourVector:
				//到达Area区域，小时级别的机会值
				temp=vectorMultiplyvector(validVector.getHourVector(), vector.getVector());
				if(temp==null)
					return -1;
				else
				{
					double sum=vectorSum(temp);
					if(sum>ChanceComputeConfig.HOUR_LEVEL_AREA_THRESHOLD)
					{
						chanceValue+=sum;
					}
					else
					{
						//该尺度机会值不满足阈值要求
						return -1;
					}
				}
				break;
				
			case FrequencyVectorLevel.weekVector:
				//到达Area区域，星期级别的机会值
				temp=vectorMultiplyvector(validVector.getWeekVector(), vector.getVector());
				if(temp==null)
					return -1;
				else
				{
					double sum=vectorSum(temp);
					if(sum>ChanceComputeConfig.WEEK_LEVEL_AREA_THREASHOLD)
					{
						chanceValue+=sum;
					}
					else
					{
						//该尺度机会值不满足阈值要求
						return -1;
					}
				}
				break;
				
			case FrequencyVectorLevel.monthVector:
				//到达Area区域，星期级别的机会值
				temp=vectorMultiplyvector(validVector.getMonthVector(), vector.getVector());
				if(temp==null)
					return -1;
				else
				{
					double sum=vectorSum(temp);
					if(sum>ChanceComputeConfig.MONTH_LEVEL_AREA_THRESHOLD)
					{
						chanceValue+=sum;
					}
					else
					{
						//该尺度机会值不满足阈值要求
						return -1;
					}
				}
				break;
				
			default:
				//不合法的频率向量
				return -1;
			}
		}
		return chanceValue;
	}
	
	/**
	 * 作用：	计算历史邻居节点携带到Area区域的机会值
	 * @param validVector :bundle的时效向量
	 * @param area
	 * @param neighbour :历史邻居，可以获取与历史邻居相遇的频率向量
	 * @return
	 */
	public static double historyNeighbourcarryChance(ValidVector validVector,Area area,Neighbour neighbour)
	{
		List<FrequencyVector> vectorList=area.getFrequencyVectorList();
		
		//到达area区域的机会值总和
		double chanceValue=0d;
		
		//到达area区域，不同尺度机会值向量
		double[] temp;
		
		for(FrequencyVector vector:vectorList)
		{
			switch(vector.getFrequencyLevel())
			{
			case FrequencyVectorLevel.hourVector:
				//到达Area区域，小时级别的机会值
				temp=vectorMultiplyvector(validVector.getHourVector(), vector.getVector());
				
				if(temp==null)
					return -1;
				else
				{
					double sum=vectorSum(temp);
					if(sum>ChanceComputeConfig.HOUR_LEVEL_AREA_THRESHOLD)
					{
						//乘以与历史邻居相遇的衰减
						double hourDenominator=ChanceComputeConfig.HOUR_LEVEL_NEIGHBOUR_DENOMINATOR;
						if(validVector.getHourVector().length!=0)
						{
							hourDenominator=hourDenominator*validVector.getHourVector().length;
						}
						double hourTemp=vectorSum(vectorMultiplyvector(validVector.getHourVector(),neighbour.getHourFrequencyVector().getVector()))/hourDenominator;
						sum*=hourTemp;
						
						chanceValue+=sum;
					}
					else
					{
						//该尺度机会值不满足阈值要求
						return -1;
					}
				}
				break;
				
			case FrequencyVectorLevel.weekVector:
				//到达Area区域，星期级别的机会值
				temp=vectorMultiplyvector(validVector.getWeekVector(), vector.getVector());
				if(temp==null)
					return -1;
				else
				{
					double sum=vectorSum(temp);
					if(sum>ChanceComputeConfig.WEEK_LEVEL_AREA_THREASHOLD)
					{
						//乘以与历史邻居相遇的衰减
						double weekDenominator=ChanceComputeConfig.WEEK_LEVEL_NEIGHBOUR_DENOMINATOR;
						if(validVector.getWeekVector().length!=0)
						{
							weekDenominator=weekDenominator*validVector.getWeekVector().length;
						}
						double weekTemp=vectorSum(vectorMultiplyvector(validVector.getWeekVector(),neighbour.getWeekFrequencyVector().getVector()))/weekDenominator;
						sum*=weekTemp;
						
						chanceValue+=sum;
					}
					else
					{
						//该尺度机会值不满足阈值要求
						return -1;
					}
				}
				break;
				
			case FrequencyVectorLevel.monthVector:
				//到达Area区域，星期级别的机会值
				temp=vectorMultiplyvector(validVector.getMonthVector(), vector.getVector());
				if(temp==null)
					return -1;
				else
				{
					double sum=vectorSum(temp);
					if(sum>ChanceComputeConfig.MONTH_LEVEL_AREA_THRESHOLD)
					{
						//乘以与历史邻居相遇的衰减
						double monthDenominator=ChanceComputeConfig.MONTH_LEVEL_NEIGHBOUR_DENOMINATOR;
						if(validVector.getMonthVector().length!=0)
						{
							monthDenominator=monthDenominator*validVector.getMonthVector().length;
						}
						double monthTemp=vectorSum(vectorMultiplyvector(validVector.getMonthVector(),neighbour.getMonthFrequencyVector().getVector()))/monthDenominator;
						sum*=monthTemp;
						
						chanceValue+=sum;
					}
					else
					{
						//该尺度机会值不满足阈值要求
						return -1;
					}
				}
				break;
				
			default:
				//不合法的频率向量
				return -1;
			}
		}
		return chanceValue;
	}
	
	
	/**
	 * 通过从bundle的有效时间和当前时间获取有效时间向量
	 * @return ：获取bundle有效时间向量
	 */
	static ValidVector getValidVector(Bundle bundle)
	{
		long expirationSeconds=bundle.creation_ts().seconds()+bundle.expiration()+DTNTime.TIMEVAL_CONVERSION;
		
		long nowSeconds=TimeHelper.current_seconds_from_ref()+DTNTime.TIMEVAL_CONVERSION;
		
		if(nowSeconds>=expirationSeconds)
		{
			GeohistoryLog.i(tag, String.format("该bundle已经超时却进入了小时机会值计算。bundle_%d,区域(0-3)：%d.%d.%d.%d",
					bundle.bundleid(),bundle.zeroArea(),bundle.firstArea(),bundle.secondArea(),bundle.thirdArea()));
			return null;
		}
		else
		{
			double[] validHourVector=new double[24];
			for(int i=0;i<validHourVector.length;i++)
				validHourVector[i]=0;
			double[] validWeekVector=new double[7];
			for(int i=0;i<validWeekVector.length;i++)
				validWeekVector[i]=0;
			double[] validMonthVector=new double[12];
			for(int i=0;i<validMonthVector.length;i++)
				validMonthVector[i]=0;
			
			Calendar expirationCan=Calendar.getInstance();
			expirationCan.setTimeInMillis(expirationSeconds*1000);
			Calendar nowCan=Calendar.getInstance();
			nowCan.setTimeInMillis(nowSeconds);
			
//			将当前时间和失效时间段内的值赋为1
			for(int i=nowCan.get(Calendar.HOUR_OF_DAY);i<=expirationCan.get(Calendar.HOUR_OF_DAY);i++)
			{
				validHourVector[i]=1;
			}
			
			for(int i=nowCan.get(Calendar.DAY_OF_WEEK);i<=expirationCan.get(Calendar.DAY_OF_WEEK);i++)
			{
				validWeekVector[i]=1;
			}
			
			for(int i=nowCan.get(Calendar.MONTH);i<=expirationCan.get(Calendar.MONTH);i++)
			{
				validMonthVector[i]=1;
			}
			
			ValidVector vector=new ValidVector(validHourVector, validWeekVector, validMonthVector);
			
			GeohistoryLog.d(tag, String.format("通过计算获得bundle_%d的有效时间向量：%s", bundle.bundleid(),vector.toString()));
			
			return vector;
		}
		
	}
	
	/**
	 * 计算向量之间相乘
	 * @return :如果向量为空或者两者长度不等，返回null；否则返回
	 */
	public static double[] vectorMultiplyvector(double[] a,double[] b)
	{
		if(a==null && b==null)
		{
			return null;
		}
		
		//两向量不合法
		if(a.length!=b.length)
			return null;
		
		double[] result=new double[a.length];
		for(int i=0;i<result.length;i++)
		{
			result[i]=a[i]*b[i];
		}
		
		return result;
	}
	
	/**
	 * 计算向量和常数之间的结果
	 */
	public double[] vectorMultiplyConstant(double[] a,double b)
	{
		if(a==null)
			return null;
		
		for(int i=0;i<a.length;i++)
		{
			a[i]=a[i]*b;
		}
		
		return a;
	}
	
	public static double vectorSum(double[] a)
	{
		if(a==null)
			return -1;
		
		double sum=0d;
		for(int i=0;i<a.length;i++)
		{
			sum+=a[i];
		}
		
		return sum;
	}
}
