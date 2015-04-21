package android.geosvr.dtn.servlib.geohistorydtn;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.util.Log;


public class ReadConnectedMatrix 
{
	public static int[][] getMatrix(Context context) throws NotFoundException, IOException
	{
		InputStream in=context.getAssets().open("connectionmatrix.xml");
		int length=in.available();
		byte[] buffer=new byte[length];
		in.read(buffer);
		String content=new String(buffer);//文档内容
		
		
		String[] line=content.split("\n|\0");
		int[][] matrix=new int[line.length][line.length];
		
		for(int i=0;i<line.length;i++)
		{
			String[] s=line[i].split(",");
			if(s.length!=line.length)
				return null;
			
			for(int j=0;j<s.length;j++)
			{
				matrix[i][j]=Integer.valueOf(s[j].trim());
			}
		}
		
		return matrix;
		
	}
}
