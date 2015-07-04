package android.geosvr.dtn.servlib.geohistorydtn.config;
/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-7-4 上午9:33:39 
 * 说明 
 */
public class ChanceComputeConfig 
{
	//计算区域间机会值
	//小时级别的阈值
	public final static double HOUR_LEVEL_AREA_THRESHOLD=1d;
	//星期级别的阈值
	public final static double WEEK_LEVEL_AREA_THREASHOLD=1d;
	//月级别的阈值
	public final static double MONTH_LEVEL_AREA_THRESHOLD=1d;
	
	/*//计算邻居间相遇的机会值
	//小时级别
	public final static double HOUR_LEVEL_NEIGHBOUR_THRESHOLD=1;
	//星期级别
	public final static double WEEK_LEVEL_NEIGHBOUR_THRESHOLD=1;
	//月级别
	public final static double MONTH_LEVEL_NEIGHBOUR_THRESHOLD=1;*/
	
	//将邻居相遇的机会值化为小于1的值，需要除以分母
	//小时级别
	public final static double HOUR_LEVEL_NEIGHBOUR_DENOMINATOR=24d;
	//星期级别
	public final static double WEEK_LEVEL_NEIGHBOUR_DENOMINATOR=7d;
	//月级别
	public final static double MONTH_LEVEL_NEIGHBOUR_DENOMINATOR=12d;
}
