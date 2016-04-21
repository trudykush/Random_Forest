

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class DecisionTree		//method to take the .csv fle as i/p and pass those values to random forests 
{
	BufferedReader BR = null;
	String path;
	public DecisionTree(String path)
	{
		this.path=path;
	}
	
	public ArrayList<ArrayList<String>> CreateInput(String path)
	{
		ArrayList<ArrayList<String>> DataInput = new ArrayList<ArrayList<String>>();
			try
			{
		 		String sCurrLine;
		 		BR = new BufferedReader(new FileReader(path));
		 			while ((sCurrLine = BR.readLine()) != null) 
		 			{
		 				int i;
		 				ArrayList<Integer> Sp=new ArrayList<Integer>();
		 					if(sCurrLine!=null)
		 					{
		 						if(sCurrLine.indexOf(",")>=0) 	// has comma
		 						{
										sCurrLine=","+sCurrLine+",";
										char[] c =sCurrLine.toCharArray();
											for(i=0;i<sCurrLine.length();i++)
											{
												if(c[i]==',')
													Sp.add(i);
											}ArrayList<String> DataPoint=new ArrayList<String>();
												for(i=0;i<Sp.size()-1;i++)
												{
													DataPoint.add(sCurrLine.substring(Sp.get(i)+1, Sp.get(i+1)).trim());
												}
												DataInput.add(DataPoint);
								}
		 						else if(sCurrLine.indexOf(" ")>=0)		//has spaces
		 						{
		 							sCurrLine=" "+sCurrLine+" ";
		 								for(i=0;i<sCurrLine.length();i++)
		 								{
		 									if(Character.isWhitespace(sCurrLine.charAt(i)))
		 										Sp.add(i);
		 								}
		 								ArrayList<String> DataPoint=new ArrayList<String>();
		 								for(i=0;i<Sp.size()-1;i++)
		 								{
		 									DataPoint.add(sCurrLine.substring(Sp.get(i), Sp.get(i+1)).trim());
		 								}
		 								DataInput.add(DataPoint);
		 						}
		 					}
		 			}
		 		System.out.println("Input generated");

			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			finally 
			{
				try 
				{
					if (BR != null)BR.close();
				}
				catch (IOException ex)
				{
					ex.printStackTrace();
				}
			}
	return DataInput;
	}
}
