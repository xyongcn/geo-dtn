package android.geosvr.dtn.apps;

import java.util.HashMap;

import android.app.Activity;
import android.geosvr.dtn.DTNManager;
import android.geosvr.dtn.R;
import android.geosvr.dtn.servlib.geohistorydtn.areaConnectiveSimulation.CurrentLocationFromScript;
import android.geosvr.dtn.servlib.geohistorydtn.areaConnectiveSimulation.CurrentLocationFromScript.AreaDivision;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;

/**
 * 用来选择属于哪个区域的
 * @author wwtao
 *
 */
public class DTNSelectArea extends Activity
{
	static String tag="DTNSelectArea";
	
	
	//控件
	ListView listview=null;
	RadioGroup radiogroup=null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dtnapps_selectarea);
		init();
	}
	
	public void init()	
	{
		//设置单选按钮
		radiogroup=(RadioGroup)findViewById(R.id.dtnapps_selectarea_areachange_radioGrop);
		radiogroup.check(R.id.dtnapps_selectarea_randomchange_radioButton);
		
		//设置列表
		listview=(ListView)findViewById(R.id.dtnapps_selectarea_neighbourarea_listview);
		
		final CurrentLocationFromScript current=(CurrentLocationFromScript)DTNManager.getInstance().currentLocation;
		
		
		final Object[] areaarray=current.getNeighbourArea();
		
		String[] str=new String[areaarray.length];
		for(int i=0;i<str.length;i++)
		{
			str[i]=areaarray[i].toString();
		}
		
		listview.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,str ));
		
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				Log.i(tag,"点击了:"+listview.getItemAtPosition(arg2).toString());
//				arg0.getItemAtPosition(arg2);
				current.changeArea(((AreaDivision)areaarray[arg2]));
				finish();
			}
		});
	}
}
