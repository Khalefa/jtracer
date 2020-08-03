public class Test {
  public int id;
  static int ttt(Test t) {
    t.id = 44;

    return 5;
  }
  static int _g_ = 3;
  public static void main(String[] args) {
    int a[] = new int[10];

    Test t = new Test();
    t.id = 3;
    Test t2 = t;
    t2.id = 4;
    System.out.println(t2.id);
    System.out.println(t.id);
    _g_ = 1;
    int o = ttt(t2);
    t = null;
    t2 = null;
  }
}

//------
/*







*/
