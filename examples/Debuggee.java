package test;
public class Debuggee {
  public static void m(int x) {
    int y = x + 2;
    int g = y + y;
    System.out.println("g " + g);
  }
  public static void main(String[] args) {
    /*1*/ String jpda = "--Java Platform Debugger Architecture  ";
    /*2*/ System.out.println("Hi Everyone, Welcome to " + jpda); // add a break point here
    /*3*/ String jdi = "Java Debug Interface"; // add a break point here and also stepping in here
    /*4*/ String text = "Today, we'll dive into " + jdi;
    System.out.println("-" + text);
    m(1);
    m(3);
    for (int i = 0; i < 30; i++) m(i);
  }
}
