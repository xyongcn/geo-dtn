package android.geosvr.dtn.servlib.geohistorydtn;

/**
 * 作用:用来标识区域的对象
 * @author wwtao
 *
 */
public class Area 
{
	
	
	public Area()
	{
		
	}
	
	/**
	 * 用来判断是不是在该区域里面
	 * @param longitude
	 * @param latitude
	 * @return
	 */
	public boolean isIntheArea(double longitude,double latitude)
	{
		return true;
	}
}
