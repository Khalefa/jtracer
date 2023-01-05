public class Test {
  public int id;
  static int ttt(Test t) {
    t.id = 44;
    return 5;
  }
  public static void main(String[] args) {
    int a[] = new int[3];
    Test t = new Test();
    t.id = 3;
    Test t2 = t;
    t2.id = 4;
    int o = ttt(t2);
    t = null;
    t2 = null;
  }
}

//------
/*


stdout
stderr

event
loc

stack

globals

heap




*/
