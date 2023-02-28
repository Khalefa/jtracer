import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

public class Test3 {
	
	public static final int total = 100;
	public static final long seed = 1234567896189L;
	public static int[] totalArray = new int[total];

	public static void main(String[] args) {

		for(int i=0; i<total; i++) {
			int num = i + 1;
			totalArray[i] = num;
		}

		Random rnd = new Random(seed);

		for(int i=0; i<totalArray.length; i++) {
			int rndIndex = rnd.nextInt(totalArray.length);
			int temp = totalArray[rndIndex];
			totalArray[rndIndex] = totalArray[i];
			totalArray[i] = temp;
		}

		SortedMap<Integer, Integer> map  = new TreeMap<>();

		for(int i=0; i<totalArray.length; i++) {
			map.put(totalArray[i],totalArray[i]);
		}

		for(int i=0; i<totalArray.length; i++) {
			totalArray[i] = map.get(i+1);
		}
	}
}