package android.geosvr.dtn.servlib.geohistorydtn.area;
/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-5-14 上午9:16:09 
 * 说明 ：对所有的Area进行管理，分配和超出容量删除机制
 */
public class AreaManager 
{
	private static class SingleAreaManager
	{
		static AreaManager instance=new AreaManager();
	}
	
	//线程安全单例模式
	public static AreaManager getInstance()
	{
		return SingleAreaManager.instance;
	}
	
	/**
	 * 默认构造函数
	 */
	public AreaManager()
	{
		
	}
	
	/**
	 * 查询是否含有该底层区域
	 * @param baseArea：要查询的底层区域
	 * @return 查询的结果
	 */
	public boolean hasThisBaseArea(Area baseArea)
	{
		return false;
	}
}
