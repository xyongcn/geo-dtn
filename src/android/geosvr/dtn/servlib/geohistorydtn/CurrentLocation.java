package android.geosvr.dtn.servlib.geohistorydtn;

/**
 * 用来获取当前区域以及变化的接口
 * 可继承实现利用GPS检测当前区域变化
 * 也可继承实现利用脚本实现区域变化
 * @author thedevilking
 *
 */
public interface CurrentLocation 
{
	public int[] getAreaLayer();
	
	public int getAreaNum();
}
