package android.geosvr.dtn.systemlib.util;
/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-10-27 下午5:15:43 
 * 说明 ：将经纬度由int的表示转为double型，或者有double型转为int
 */
public class LocationHelp {
	public static final int accuracy = 6;
	
	public static double gpsint2double(int x){
		return (double)x * 1d / Math.pow(10, accuracy);
	}
	
	public static int gpsdouble2int(double x){
		return (int)(x * Math.pow(10, accuracy));
	}
}
