package android.geosvr.dtn.servlib.geohistorydtn.area;

/**
 * 作用:用来描述区域的对象
 * @author wwtao
 *
 */
public class Area 
{
	//区域分层的
	/*private int zeronum;
	private int firstnum;
	private int secondnum;
	private int thirdnum;*/
	
	
	/**
	 * 该端时间内是否被修改过
	 */
	boolean hasChanged=false;
	
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
	
	/**
	 * @author wwtao
	 * 判断是不是同一个area，不只是通过对象引用比较，因为在不同区域间也是要进行比较的，所以应该有每个区域的标识
	 */
	@Override
	public boolean equals(Object o) {
		// TODO Auto-generated method stub
		return super.equals(o);
	}
}
