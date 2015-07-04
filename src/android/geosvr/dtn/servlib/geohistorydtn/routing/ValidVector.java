package android.geosvr.dtn.servlib.geohistorydtn.routing;
/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-7-3 下午9:09:10 
 * 说明 
 */
public class ValidVector {

	double[] hourVector;
	double[] weekVector;
	double[] monthVector;
	
	public ValidVector(double[] hourVector,double[] weekVector,double[] monthVector)
	{
		this.hourVector=hourVector;
		this.weekVector=weekVector;
		this.monthVector=monthVector;
	}
	
	public double[] getHourVector()
	{
		return hourVector;
	}
	
	public double[] getWeekVector()
	{
		return weekVector;
	}
	
	public double[] getMonthVector()
	{
		return monthVector;
	}

	
	@Override
	public String toString() {
		String hourstr=String.valueOf(hourVector[0]);
		for(int i=1;i<hourVector.length;i++)
			hourstr+=","+hourVector[i];
		hourstr+=";";
		
		String weekstr=String.valueOf(weekVector[0]);
		for(int i=1;i<weekVector.length;i++)
			weekstr+=","+weekVector[i];
		weekstr+=";";
		
		String monthstr=String.valueOf(monthVector[0]);
		for(int i=1;i<monthVector.length;i++)
			monthstr+=","+monthVector[i];
		monthstr+=";";
		
		String s=String.format("valid vector:hourVector:%s weekVector:%s monthVector:%s", hourstr,weekstr,monthstr);
		
		return null;
	}
}
