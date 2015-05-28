package android.geosvr.dtn.servlib.geohistorydtn.frequencyVector;

import android.geosvr.dtn.servlib.geohistorydtn.area.AreaInfo;
import android.geosvr.dtn.servlib.geohistorydtn.timeManager.TimeManager;

public class WeekFrequencyVector extends FrequencyVector 
{

	
	public WeekFrequencyVector(int vectorLevel, int serviceType) {
		super(FrequencyVectorLevel.weekVector, serviceType);
		// TODO Auto-generated constructor stub
	}
	
	public void init()	
	{
		this.vector=new double[7];
		this.vectorChange=new boolean[7];
		this.vectorLength=7;
		
		for(int i=0;i<vectorLength;i++)
		{
			vector[i]=0;
			vectorChange[i]=false;
		}
	}

	@Override
	public void changeVector(AreaInfo info) {
		// TODO Auto-generated method stub
		int week=info.getWeek();
		
		if(week>=0 && week<7)
		{
			if(!vectorChange[week])
			{
				++vector[week];
			}
		}
	}

}
