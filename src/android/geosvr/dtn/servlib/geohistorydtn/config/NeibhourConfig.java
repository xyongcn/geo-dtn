package android.geosvr.dtn.servlib.geohistorydtn.config;

/**
 * 对邻居相关的配置：系统内保存邻居的数目，系统内保留邻居区域的个数，需要保留区域信息的历史邻居个数
 * @author wwtao
 *
 */
public class NeibhourConfig 
{
	/**
	 * 保留的历史邻居个数
	 */
	public final static int NEIGHOURNUM=100;
	
	/**
	 * 保留的具有区域信息的历史邻居个数，该数目不能超过NEIGHBOURNUM
	 */
	public final static int NEIGHBOURAREANUM=10;
	
	/**
	 * 保留的区域信息的邻居所保留的区域个数（也是邻居之间要交换的区域的个数）
	 */
	public final static int NEIGHBOURKEEPAREANUM=200;
	
	/**
	 * 存放邻居的历史区域信息的path
	 */
	public final static String NEIGHBOURAREAFILEDIR="/sdcard/geoHistory_dtn/neighbour_area/";
}
