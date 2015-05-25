package android.geosvr.dtn.servlib.geohistorydtn.frequencyVector;

public class HourFrequencyVector extends FrequencyVector 
{

	
	public HourFrequencyVector(int vectorLevel, int serviceType) {
		super(FrequencyVectorLevel.hourVector, serviceType);
		
		
	}
	
	public void init()	
	{
		this.vector=new double[24];
		this.vectorChange=new boolean[24];
		this.vectorLength=24;
	}

	@Override
	public void changeVector() {
		// TODO Auto-generated method stub
		
	}

}
