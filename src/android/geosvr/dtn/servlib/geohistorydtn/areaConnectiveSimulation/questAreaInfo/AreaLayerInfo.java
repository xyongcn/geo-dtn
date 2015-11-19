package android.geosvr.dtn.servlib.geohistorydtn.areaConnectiveSimulation.questAreaInfo;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * 用来记录这个GPS所在的各个行政区分层的信息
 * 作为收到区域分层信息后返回分层的信息
 * @author wwtao
 *
 */
public class AreaLayerInfo extends ArrayList<Integer> implements Serializable{

	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		
		if(! (o instanceof AreaLayerInfo)){
			return false;
		}
		
		AreaLayerInfo other=(AreaLayerInfo)o;
		if(this.size()!=other.size()){
			return false;
		}
		
		for(int i=0;i<this.size();i++){
			if(this.get(i)!=other.get(i))
				return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder s=new StringBuilder();
		s.append("areainfo（从顶层到底层）:"+String.valueOf(get(0)));
		for(int i=1;i<size();i++){
			s.append(" , "+String.valueOf(get(i)));
		}
		
		return s.toString();
	}
}
