package android.geosvr.dtn.servlib.geohistorydtn.frequencyVector;

import java.io.Serializable;

import android.geosvr.dtn.servlib.geohistorydtn.area.AreaInfo;
import android.geosvr.dtn.servlib.geohistorydtn.timeManager.TimeManager;

public class MonthFrequencyVector extends FrequencyVector implements Serializable
{

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;

	MonthFrequencyVector(int vectorLevel, int serviceType) {
		super(FrequencyVectorLevel.monthVector, serviceType);
		// TODO Auto-generated constructor stub
		init();
	}
	
	void init()	
	{
		this.vector=new double[12];
		this.vectorChange=new boolean[12];
		this.vectorLength=12;
		
		//初始化向量
		for(int i=0;i<vectorLength;i++)
		{
			vector[i]=0;
			vectorChange[i]=true;
		}
	}

	@Override
	public void changeVector(AreaInfo info) {
		// TODO Auto-generated method stub
		int month=info.getMonth();
		
		if(month>=0 && month<12)
		{
			if(!vectorChange[month])
			{
				++vector[month];
				vectorChange[month]=true;
			}
		}
	}

	@Override
	public void changeVector() {
		// TODO Auto-generated method stub
		int month=TimeManager.getInstance().getMonthDayNum();
		
		if(month>=0 && month<12)
		{
			if(!vectorChange[month])
			{
				++vector[month];
				vectorChange[month]=false;
			}
		}
	}

}
