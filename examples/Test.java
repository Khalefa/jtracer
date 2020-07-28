public class Test {
  public int id;
  static void ttt(Test t) {
    t.id = 44;
  }
  public static void main(String[] args) {
    int a[] = new int[1000];

    Test t = new Test();
    t.id = 3;
    Test t2 = t;
    t2.id = 4;
    System.out.println(t2.id);
    System.out.println(t.id);

    ttt(t2);
    t = null;
    t2 = null;
  }
}