
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;

public class Entropy 
{
	private static final int INDEX_SKIP=3; // Instead of checking each index we'll skip every INDEX_SKIP indices unless there's less than MIN_SIZE_TO_CHECK_EACH
	private static final int MIN_SIZE_TO_CHECK_EACH=10;  // we'll check each one, if there's less than MIN_SIZE_TO_CHECK_EACH points  
	private static final int MIN_NODE_SIZE=5;  //  we won't continue splitting, we'll take the majority vote,if the number of data points is less than MIN_NODE_SIZE 
	private int N;  // the number of data records 
	
	// the number of samples left out of the boostrap of all N to test error rate 
	private int testN;
	private int correct; 	// Of the testN, the number that were correctly identified 
	
	// an estimate of the importance of each attribute in the data record
	private int[] importances;
	public ArrayList<String> predictions; // To keep track of the predictions done by this tree 
	private TreeNode root;  	// RootNode of the Decision Tree
	private RandomForestCateg forest;  // This is a pointer to the Random Forest this decision tree belongs to
		public Entropy(ArrayList<ArrayList<String>> data,RandomForestCateg forest, int treenum) 
		{
			this.forest=forest;
			N=data.size();
			importances = new int[RandomForestCateg.M];
		
			ArrayList<ArrayList<String>> train = new ArrayList<ArrayList<String>>(N);
			ArrayList<ArrayList<String>> test = new ArrayList<ArrayList<String>>();
		
			BootStrapSample(data,train,test,treenum);
			testN=test.size();
			correct=0;
		
			root=CreateTree(train,treenum);
			FlushData(root, treenum);
		}
	@SuppressWarnings("unchecked")
	private void BootStrapSample(ArrayList<ArrayList<String>> data,ArrayList<ArrayList<String>> train,ArrayList<ArrayList<String>> test,int numb)
	{
		ArrayList<Integer> indices=new ArrayList<Integer>();
		for (int n=0;n<N;n++)
			indices.add((int)Math.floor(Math.random()*N));
		ArrayList<Boolean> in=new ArrayList<Boolean>();
		for (int n=0;n<N;n++)
			in.add(false);	 //have to initialize it first
		for (int num:indices){
			ArrayList<String> k = data.get(num);
			train.add((ArrayList<String>) k.clone());

			in.set(num,true);
	}
		for (int i=0;i<N;i++)
			if (!in.get(i))
				test.add(data.get(i));
		
	}
	private TreeNode CreateTree(ArrayList<ArrayList<String>> train, int ntree)
	{
		TreeNode root=new TreeNode();
		root.label = "|ROOT|";
		root.data=train;
		RecursiveSplit(root,ntree);
		return root;
	}
	private class TreeNode implements Cloneable{
		public boolean isLeaf;
		public ArrayList<TreeNode> ChildNode ;
		public TreeNode left;
		public TreeNode right;
		public int splitAttributeM;			//which attribute its split on...
		public boolean spiltonCateg = false;
		public String Class;
		public ArrayList<ArrayList<String>> data;
		public String splitValue;			//check this if it return false on splitonCateg
		public String label;				//Label of each node
		public int generation;
		
		public TreeNode()
		{
			splitAttributeM=-99;
			splitValue="-99";
			generation=1;
			label = null;
			spiltonCateg = false;
			isLeaf = false;
			Class = null;
		}
		public TreeNode clone()
		{ 
			TreeNode copy=new TreeNode();  //"data" element always null in clone
			copy.isLeaf=isLeaf;
			for(TreeNode TN : ChildNode)
			{
				if(TN != null)
				{
					copy.ChildNode.add(TN.clone());
				}
			}
			if (left != null) 
				copy.left=left.clone();
			if (right != null)
				copy.right=right.clone();
			copy.splitAttributeM=splitAttributeM;
			copy.Class=Class;
			copy.splitValue=splitValue;
			return copy;
		}
	}
	private class DoubleWrap
	{
		public double d;
		public DoubleWrap(double d)
		{
			this.d=d;
		}		
	}
	
	public ArrayList<String> CalculateClasses(ArrayList<ArrayList<String>> traindata,ArrayList<ArrayList<String>> testdata, int treenumber)
	{
		ArrayList<String> predicts = new ArrayList<String>();
		for(ArrayList<String> record:testdata)
		{
			String Clas = Evaluate(record, traindata);
			if(Clas==null)
				System.out.println("Evaluation Nul hua");
			predicts.add(Clas);
		}
		predictions = predicts;
		return predicts;
	}
	
	public String Evaluate(ArrayList<String> record, ArrayList<ArrayList<String>> tester)
	{
		TreeNode evalNode=root;		
			while (true) {				
				if(evalNode.isLeaf)
					return evalNode.Class;
				else{
					if(evalNode.spiltonCateg)
					{}
					else
					{
						double Compare = Double.parseDouble(evalNode.splitValue);
						double Actual = Double.parseDouble(record.get(evalNode.splitAttributeM));
						if(Actual <= Compare)
						{
							evalNode=evalNode.left;
						}
						else
						{
							evalNode=evalNode.right;
						}
					}
				}
		}
	}
	private String changeNodeLabel(int splitAttributeM, ArrayList<String> record,ArrayList<ArrayList<String>> data)
	{
		String label = record.get(record.size()-1);
		ArrayList<String> ToFind = new ArrayList<String>();
		for(ArrayList<String> DP : data){
			if(DP.get(DP.size()-1).equalsIgnoreCase(label)){
				ToFind.add(DP.get(splitAttributeM));
			}
		}
		String Fill =null;
		Fill = forest.ModeofList(ToFind);
		return Fill;
	}

	private void RecursiveSplit(TreeNode parent, int Ntreenum)
	{
		if (!parent.isLeaf)
		{
			String Class=CheckIfLeaf(parent.data);   			//Step A
			if (Class != null){
				parent.isLeaf=true;
				parent.Class=Class;
				return;
			}
			int Nsub=parent.data.size();						//Step B
			parent.ChildNode = new ArrayList<Entropy.TreeNode>();
			for(TreeNode TN: parent.ChildNode)
			{
				TN = new TreeNode();
				TN.generation = parent.generation+1;
			}
			parent.left = new TreeNode();
			parent.left.generation=parent.generation+1;
			
			parent.right = new TreeNode();
			parent.right.generation = parent.generation+1;
			
			ArrayList<Integer> vars=GetVarsToInclude();
			DoubleWrap lowestE=new DoubleWrap(Double.MAX_VALUE);
			
			for (int m:vars)										//Step C
			{
				SortAtAttribute(parent.data,m);									//sorts on a particular column in the row
				ArrayList<Integer> DataPointToCheck=new ArrayList<Integer>();	 
				for (int n=1;n<Nsub;n++)
				{
					String classA=GetClass(parent.data.get(n-1));
					String classB=GetClass(parent.data.get(n));
					if(!classA.equalsIgnoreCase(classB))
						DataPointToCheck.add(n);
				}
				if (DataPointToCheck.size() == 0)					
				{
					parent.isLeaf=true;
					parent.Class=GetClass(parent.data.get(0));
					continue;
				}
				
				if (DataPointToCheck.size() > MIN_SIZE_TO_CHECK_EACH){
					for (int i=0;i<DataPointToCheck.size();i+=INDEX_SKIP){
						CheckPosition(m, DataPointToCheck.get(i), Nsub, lowestE, parent, Ntreenum);
						if (lowestE.d == 0)							
							break;
					}
				}else{
					for (int k:DataPointToCheck){
						CheckPosition(m,k, Nsub, lowestE, parent, Ntreenum);
						if (lowestE.d == 0)
							break;
					}
				}
				if (lowestE.d == 0)
					break;
			}
			if(!parent.spiltonCateg)   				//Step D
			{
				if (parent.left.data.size() == 1)					//Left Child		
				{
					parent.left.isLeaf=true;
					parent.left.Class=GetClass(parent.left.data.get(0));							
				}
				else if (parent.left.data.size() < MIN_NODE_SIZE)
				{
					parent.left.isLeaf=true;
					parent.left.Class=GetMajorityClass(parent.left.data);	
				}
				else 
				{
					Class=CheckIfLeaf(parent.left.data);
					if (Class == null){
						parent.left.isLeaf=false;
						parent.left.Class=null;
					}
					else {
						parent.left.isLeaf=true;
						parent.left.Class=Class;
					}
				}
					
				if (parent.right.data.size() == 1)		//Right Child
				{
					parent.right.isLeaf=true;
					parent.right.Class=GetClass(parent.right.data.get(0));								
				}
				else if (parent.right.data.size() < MIN_NODE_SIZE){
					parent.right.isLeaf=true;
					parent.right.Class=GetMajorityClass(parent.right.data);	
				}
				else {
					Class=CheckIfLeaf(parent.right.data);
					if (Class == null)		
					{
						parent.right.isLeaf=false;
						parent.right.Class=null;
					}
					else 
					{
						parent.right.isLeaf=true;
						parent.right.Class=Class;
					}
				}
								//Split if necessary
				if (!parent.left.isLeaf)
				{
					RecursiveSplit(parent.left,Ntreenum);
				}
				if (!parent.right.isLeaf)
				{				
					RecursiveSplit(parent.right,Ntreenum);
				}
			}
			else
			{
								//for Categorical Children
				for(TreeNode Child:parent.ChildNode)
				{
					if(Child.label != null)
					{
						if(Child.data.size()==1)
						{
							Child.isLeaf=true;
							Child.Class = GetClass(Child.data.get(0));
						}
						else if(Child.data.size() <= MIN_NODE_SIZE)
						{
							Child.isLeaf = true;
							Child.Class=GetMajorityClass(Child.data);
						}
						else
						{
							if(CheckIfLeaf(Child.data)!=null)
							{
								Child.isLeaf = true;
								Child.Class = CheckIfLeaf(Child.data);
							}
						}
					}
				}
				
				for(TreeNode Child : parent.ChildNode){
					if(!Child.isLeaf){
						System.out.println(" Splitting again from "+parent.label+", with data size "+Child.data.size()+" at gen "+parent.generation);
						RecursiveSplit(Child, Ntreenum);
					}
				}
			}
		}
	}
	
	private String GetMajorityClass(ArrayList<ArrayList<String>> data)
	{
		ArrayList<String> ToFind = new ArrayList<String>();
		for(ArrayList<String> s:data)
		{
			ToFind.add(s.get(s.size()-1));
		}
		String MaxValue = null; int MaxCount = 0;
		for(String s1:ToFind)
		{
			int count =0;
			for(String s2:ToFind)
			{
				if(s2.equalsIgnoreCase(s1))
					count++;
			}
			if(count > MaxCount)
			{
				MaxValue = s1;
				MaxCount = count;
			}
		}return MaxValue;
	}
	
	private double CheckPosition(int m,int n,int Nsub,DoubleWrap lowestE,TreeNode parent, int nTre)
	{
		String real_OR_categ = parent.data.get(n).get(m);
		double entropy =0;
		
		if (n < 1) 					//exit conditions
			return 0;
		if (n > Nsub)
			return 0;
		
		if(isAlphaNumeric(real_OR_categ))
		{		
			ArrayList<Double> pr=getClassProbs(parent.data);
			entropy =CalEntropy(pr);
			if (entropy < lowestE.d){
				lowestE.d=entropy;
				parent.splitAttributeM=m;
				parent.spiltonCateg = true;
				parent.splitValue=parent.data.get(n).get(m);
				
				HashMap<String, Integer> coun = new HashMap<String, Integer>();
				for(ArrayList<String> s:parent.data)
				{
					if(coun.containsKey(s.get(m)))
					{
						coun.put(s.get(m), coun.get(s.get(m))+1);
					}
					else
						coun.put(s.get(m), 1);
				}
				if(coun.size()>parent.ChildNode.size())
				{
					for(Entry<String, Integer> entry : coun.entrySet())
					{}
				}
				parent.ChildNode.clear();						//for removing everything before you add
				
				for(Entry<String, Integer> entry : coun.entrySet())
				{
					ArrayList<ArrayList<String>> child_data = new ArrayList<ArrayList<String>>();
					for(ArrayList<String> s : parent.data)
					{
						if(entry.getKey().equalsIgnoreCase(s.get(m)))
							child_data.add(s);
					}										//adding node and its contents
					TreeNode Child = new TreeNode();
					Child.data = child_data;
					Child.label = entry.getKey();
					parent.ChildNode.add(Child);
				}
			}
		}
		else
		{
			ArrayList<ArrayList<String>> lower=GetLower(parent.data,n);
			ArrayList<ArrayList<String>> upper=GetUpper(parent.data,n);
			ArrayList<Double> pl=getClassProbs(lower);
			ArrayList<Double> pu=getClassProbs(upper);
				double eL=CalEntropy(pl);
				double eU=CalEntropy(pu);
		
				entropy =(eL*lower.size()+eU*upper.size())/((double)Nsub);
			
			if (entropy < lowestE.d)
			{
				lowestE.d=entropy;
				parent.splitAttributeM=m;
				parent.spiltonCateg=false;
				parent.splitValue = parent.data.get(n).get(m).trim();
				
				parent.left.data=lower;
				parent.left.label="Left";
				parent.right.data=upper;
				parent.right.label="Right";
			}
		}
		return entropy;
	}
	private ArrayList<ArrayList<String>> GetLower(ArrayList<ArrayList<String>> data,int nSplit){
		ArrayList<ArrayList<String>> LS = new ArrayList<ArrayList<String>>();
		for(int n=0;n<nSplit;n++){
			LS.add(data.get(n));
		}return LS;
	}
	private ArrayList<ArrayList<String>> GetUpper(ArrayList<ArrayList<String>> data,int nSplit){
		int N=data.size();
		ArrayList<ArrayList<String>> LS = new ArrayList<ArrayList<String>>();
		for(int n=nSplit;n<N;n++){
			LS.add(data.get(n));
		}return LS;
	}
	
	private ArrayList<Double> getClassProbs(ArrayList<ArrayList<String>> record){
		double N=record.size();
		HashMap<String, Integer > counts = new HashMap<String, Integer>();
		for(ArrayList<String> s : record){
			String clas = GetClass(s);
			if(counts.containsKey(clas))
				counts.put(clas, counts.get(clas)+1);
			else
				counts.put(clas, 1);
		}
		ArrayList<Double> probs = new ArrayList<Double>();
		for(Entry<String, Integer> entry : counts.entrySet()){
			double prob = entry.getValue()/N;
			probs.add(prob);
		}return probs;
	}
	
	private static final double logoftwo=Math.log(2);
	
	private double CalEntropy(ArrayList<Double> ps)
	{
		double e=0;		
		for (double p:ps)
		{
			if (p != 0) 
				e+=p*Math.log(p)/logoftwo;
		}
		return -e; 
	}
	
	private boolean isAlphaNumeric(String s)
	{
		char c[]=s.toCharArray();
		for(int j=0;j<c.length;j++){
			if(Character.isLetter(c[j]))
			{
				return true;
			}
		}return false;
	}
	
	private void SortAtAttribute(ArrayList<ArrayList<String>> data,int m)
	{
		if(isAlphaNumeric(data.get(0).get(m)))
			Collections.sort(data,new AttributeComparatorCateg(m));
		else
			Collections.sort(data,new AttributeComparatorReal(m));
	}
	
	private class AttributeComparatorCateg implements Comparator<ArrayList<String>>
	{	
		private int m;
		public AttributeComparatorCateg(int m){
			this.m=m;
		}
		
		public int compare(ArrayList<String> arg1, ArrayList<String> arg2) 
		{
			String a = arg1.get(m);
			String b = arg2.get(m);
			return a.compareToIgnoreCase(b);
		}		
	}
	
	
	private class AttributeComparatorReal implements Comparator<ArrayList<String>>
	{
		private int m;
		public AttributeComparatorReal(int m)
		{
			this.m=m;
		}
			public int compare(ArrayList<String> arg1, ArrayList<String> arg2) 
			{
						double a2 = Double.parseDouble(arg1.get(m));
						double b2 = Double.parseDouble(arg2.get(m));
							if(a2<b2)
								return -1;
							else if(a2>b2)
								return 1;
							else
								return 0;
			}		
	}
	private ArrayList<Integer> GetVarsToInclude() 
	{
		boolean[] whichVarsToInclude=new boolean[RandomForestCateg.M];

		for (int i=0;i<RandomForestCateg.M;i++)
			whichVarsToInclude[i]=false;
		
		while (true){
			int a=(int)Math.floor(Math.random()*RandomForestCateg.M);
			whichVarsToInclude[a]=true;
			int N=0;
			for (int i=0;i<RandomForestCateg.M;i++)
				if (whichVarsToInclude[i])
					N++;
			if (N == RandomForestCateg.Ms)
				break;
		}
		
		ArrayList<Integer> shortRecord=new ArrayList<Integer>(RandomForestCateg.Ms);
		
		for (int i=0;i<RandomForestCateg.M;i++)
			if (whichVarsToInclude[i])
				shortRecord.add(i);
		return shortRecord;
	}
	
	public static String GetClass(ArrayList<String> record){
		return record.get(RandomForestCateg.M).trim();
	}
	private String CheckIfLeaf(ArrayList<ArrayList<String>> data)
	{
		boolean isLeaf=true;
		String ClassA=GetClass(data.get(0));
		for(ArrayList<String> record : data)
		{
			if(!ClassA.equalsIgnoreCase(GetClass(record)))
			{
				isLeaf = false;
				return null;
			}
		}
		return GetClass(data.get(0));
	}
	private void FlushData(TreeNode node, int treenum)
	{
		node.data=null;
		if(node.ChildNode!=null)
		{
			for(TreeNode TN : node.ChildNode)
			{
				if(TN != null)
					FlushData(TN,treenum);
			}
		}
		if (node.left != null)
			FlushData(node.left,treenum);
		if (node.right != null)
			FlushData(node.right,treenum);
	}

}
