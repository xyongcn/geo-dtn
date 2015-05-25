package android.geosvr.dtn.servlib.geohistorydtn.frequencyVector;

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
	}

	@Override
	public void changeVector() {
		// TODO Auto-generated method stub
		
	}

}
