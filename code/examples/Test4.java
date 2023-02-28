import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

public class Test4 {
	
	public static final int total = 100;
	public static final long seed = 1234567896189L;
	public static int[] totalArray = new int[total];

	public static void main(String[] args) {

		System.out.println("Test4.java - line 7 - int total = " + total);
		System.out.println("Test4.java - line 8 - long seed = " + seed);

		for(int i=0; i<total; i++) {
			System.out.println("Test4.java - line 16 - int i = " + i);
			int num = i + 1;
			System.out.println("Test4.java - line 18 - int num = " + num);
			totalArray[i] = num;
			System.out.println("Test4.java - line 20 - int[] totalArray[" + i + "] = " + totalArray[i]);
		}

		Random rnd = new Random(seed);

		for(int i=0; i<totalArray.length; i++) {
			System.out.println("Test4.java - line 26 - int i = " + i);
			int rndIndex = rnd.nextInt(totalArray.length);
			System.out.println("Test4.java - line 28 - int rndIndex = " + rndIndex + " via rnd.nextInt(" + totalArray.length + ") - arg = totalArray.length");
			int temp = totalArray[rndIndex];
			System.out.println("Test4.java - line 30 - int temp = int[] totalArray[" + rndIndex + "] = " + temp);
			totalArray[rndIndex] = totalArray[i];
			System.out.println("Test4.java - line 32 - int[] totalArray[" + rndIndex + "] = totalArray[" + i + "] = " + totalArray[rndIndex]);
			totalArray[i] = temp;
			System.out.println("Test4.java - line 34 - int[] totalArray[" + i + "] = temp = " + totalArray[i]);
		}

		SortedMap<Integer, Integer> map  = new TreeMap<>();

		for(int i=0; i<totalArray.length; i++) {
			System.out.println("Test4.java - line 40 - int i = " + i);
			map.put(totalArray[i],totalArray[i]);
			System.out.println("Test4.java - line 42 - map.get(" + totalArray[i] + ") = int[] totalArray[" + i + "] = " + totalArray[i]);
		}

		for(int i=0; i<totalArray.length; i++) {
			System.out.println("Test4.java - line 46 - int i = " + i);
			totalArray[i] = map.get(i+1);
			System.out.println("Test4.java - line 48 - totalArray[" + i + "] = map.get(" + (i+1) + ") = " + totalArray[i]);
		}
	}
}