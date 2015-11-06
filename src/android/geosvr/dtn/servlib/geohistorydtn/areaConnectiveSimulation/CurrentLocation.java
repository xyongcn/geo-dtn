package android.geosvr.dtn.servlib.geohistorydtn.areaConnectiveSimulation;

import java.util.HashMap;

import android.geosvr.dtn.servlib.geohistorydtn.areaConnectiveSimulation.CurrentLocationFromScript.AreaDivision;

/**
 * 用来获取当前区域以及变化的接口
 * 可继承实现利用GPS检测当前区域变化
 * 也可继承实现利用脚本实现区域变化
 * @author thedevilking
 *
 */
public interface CurrentLocation 
{
//	public int[] getAreaLayer();
	
	/**
	 * 说明：对于从脚本进行区域的模拟，这里获取的是最底层区域的编码，
	 * 针对模拟器以及地图版本，这里获取是它们最底层的区域编码。由于区域的编码都是唯一的，所以一旦最底层区域编码不一致，那么就一定不再同一个区域
	 * @return
	 */
	public int getAreaNum();
}
