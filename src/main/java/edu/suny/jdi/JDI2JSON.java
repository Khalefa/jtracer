package edu.suny.jdi;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;
import javax.json.*;

public class JDI2JSON {
  private class InputPuller {
    InputStreamReader vm_link;
    StringWriter contents = new java.io.StringWriter();
    String getContents() {
      String toret = contents.toString();
      contents = new java.io.StringWriter(); // this is added to remove the old returned output
      return toret;
    }
    InputPuller(InputStream ir) {
      try {
        vm_link = new InputStreamReader(ir, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException("Encoding error!");
      }
    }
    void pull() {
      int BUFFER_SIZE = 2048;
      char[] cbuf = new char[BUFFER_SIZE];
      int count;
      try {
        while (vm_link.ready() && ((count = vm_link.read(cbuf, 0, BUFFER_SIZE)) >= 0)) {
          contents.write(cbuf, 0, count);
        }
      } catch (IOException e) {
        throw new RuntimeException("I/O Error!");
      }
    }
  }

  private VirtualMachine vm;
  private InputPuller stdout, stderr;
  private JsonObject last_ep = null;

  /*    private ArrayList<Long> frame_stack = new ArrayList<Long>();*/
  private long frame_ticker = 0;

  public List<ReferenceType> staticListable = new ArrayList<>();

  public ReferenceType stdinRT = null;

  public static StringBuilder userlogged;

  public static boolean showVoid = true;

  boolean showStringsAsValues = true;
  boolean showAllFields = false;

  public JDI2JSON(VirtualMachine vm, InputStream vm_stdout, InputStream vm_stderr) {
    stdout = new InputPuller(vm_stdout);
    stderr = new InputPuller(vm_stderr);
    // frame_stack.add(frame_ticker++);
    showStringsAsValues = true;
    showAllFields = true; // optionsObject.getBoolean("showAllFields");
  }

  public static void userlog(String S) {
    if (userlogged == null)
      userlogged = new StringBuilder();
    userlogged.append(S).append("\n");
  }

  public JsonArray getAllFrames(ThreadReference t, JsonValue returnValue) {
    JsonObjectBuilder result = Json.createObjectBuilder();
    JsonArrayBuilder frames = Json.createArrayBuilder();
    StackFrame lastNonUserFrame = null;
    try {
      boolean firstFrame = true;
      for (StackFrame sf : t.frames()) {
        if (!showFramesInLocation(sf.location())) {
          lastNonUserFrame = sf;
          continue;
        }

        if (lastNonUserFrame != null) {
          frame_ticker++;
          frames.add(convertFrameStub(lastNonUserFrame));
          lastNonUserFrame = null;
        }
        frame_ticker++;
        frames.add(convertFrame(sf, returnValue));
        firstFrame = false;
        returnValue = null;
      }
    } catch (IncompatibleThreadStateException ex) {
      // thread was not suspended .. should not normally happen

      throw new RuntimeException("ITSE");
    }
    // result.add("frames", frames);
    return frames.build();
  }

  public JsonObject getlocalvariables_fromframe(ThreadReference t) {
    JsonObjectBuilder frame = Json.createObjectBuilder();
    try {
      StackFrame sf = t.frame(0);
      JsonObject f = convertFrameStub(sf).build();
      frame.add("frame", f);
    } catch (com.sun.jdi.IncompatibleThreadStateException e) {
    }
    return frame.build();
  }

  // returns null when nothing changed since the last time
  // (or when only event type changed and new value is "step_line")

  // add location

  JsonObject addGlobals() {
    JsonObjectBuilder statics = Json.createObjectBuilder();

    for (ReferenceType rt : staticListable)
      if (rt.isInitialized() && !in_builtin_package(rt.name()))
        for (Field f : rt.visibleFields())
          if (f.isStatic()) {
            statics.add(rt.name() + "." + f.name(), convertValue(rt.getValue(f)));
          }
    if (stdinRT != null && stdinRT.isInitialized()) {
      int stdinPosition =
          ((IntegerValue) stdinRT.getValue(stdinRT.fieldByName("position"))).value();
      // result.add("stdinPosition", stdinPosition);
      statics.add("stdin.Position", stdinPosition);
    }

    return statics.build();
  }

  JsonObject getLoc(Location loc) {
    JsonObjectBuilder result = Json.createObjectBuilder();
    try {
      result.add("source", loc.sourceName());
      result.add("name", loc.method().name());
      result.add("lineno", loc.lineNumber());

    } catch (com.sun.jdi.AbsentInformationException e) {
    }

    return result.build();
  }

  TreeMap<Long, ObjectReference> heap;
  TreeSet<Long> heap_done;

  public JsonObject convertExecutionPoint(Event e, Location loc, ThreadReference t) {
    stdout.pull();
    stderr.pull();

    JsonObjectBuilder result = Json.createObjectBuilder();

    if (loc.method().name().indexOf("access$") >= 0)
      return result.build(); // don't visualize synthetic access$000 methods

    heap_done = new TreeSet<Long>();
    heap = new TreeMap<>();

    JsonValue returnValue = null;

    result.add("stdout", stdout.getContents());
    result.add("stderr", stderr.getContents());

    if (e instanceof MethodEntryEvent) {
      result.add("event", "call");
      // frame_stack.add(frame_ticker++);
      result.add("loc", getLoc(loc));
    } else if (e instanceof MethodExitEvent) {
      returnValue = convertValue(((MethodExitEvent) e).returnValue());
      result.add("event", "return");
    } else if (e instanceof BreakpointEvent) {
      result.add("event", "breakpoint");
      result.add("loc", getLoc(loc));
    } else if (e instanceof StepEvent) {
      result.add("event", "step_line");
      result.add("loc", getLoc(loc));
    } else if (e instanceof ExceptionEvent) {
      result.add("event", "exception");
      result.add("loc", getLoc(loc));
    }

    // if (e instanceof StepEvent) {
    // compute the difference between current frame and the previous one
    // we need to make sure we are in the same dataframe
    // we will simply track then umber of

    // for now just get it compile
    result.add("stack", getAllFrames(t, returnValue));
    //}
    // if (e instanceof MethodExitEvent)
    //  frame_stack.remove(frame_stack.size()-1);

    // result.add("globals", addGlobals());

    JsonObjectBuilder heapDescription = Json.createObjectBuilder();
    convertHeap(heapDescription);
    result.add("heap", heapDescription);

    JsonObject this_ep = result.build();
    JsonObject d = diff(last_ep, this_ep);

    last_ep = this_ep;

    return d;
  }

  public static String[] builtin_packages = {
      "java", "javax", "sun", "com.sun", "traceprinter", "jdi"};

  public static String[] PU_stdlib = {
      "BinaryIn", "BinaryOut", "BinaryStdIn", "BinaryStdOut", "Copy", "Draw", "DrawListener", "In",
      "InTest", "Out", "Picture", "Point", "Queue", "ST", "Stack", "StdArrayIO", "StdAudio",
      "StdDraw", "StdDraw3D", "StdIn", "StdInTest", "StdOut", "StdRandom", "StdStats", "Stopwatch"

  };

  // input format: [package.]ClassName:lineno or [package.]ClassName
  public boolean in_builtin_package(String S) {
    S = S.split(":")[0];
    for (String badPrefix : builtin_packages)
      if (S.startsWith(badPrefix + "."))
        return true;
    for (String badClass : PU_stdlib) {
      if (S.equals(badClass))
        return true;
      if (S.startsWith(badClass + "$"))
        return true;
    }
    return false;
  }

  private boolean showFramesInLocation(Location loc) {
    return (!in_builtin_package(loc.toString()) && !loc.method().name().contains("$access"));
    // skip synthetic accessor methods
  }

  private boolean showGuts(ReferenceType rt) {
    return (rt.name().matches("(^|\\.)Point") || !in_builtin_package(rt.name()));
  }

  public boolean reportEventsAtLocation(Location loc) {
    if (in_builtin_package(loc.toString()))
      return false;

    if (loc.toString().contains("$$Lambda$"))
      return false;

    if (loc.lineNumber() <= 0) {
      userlog(loc.toString());
      return true;
    }

    return true;
  }

  private JsonObject createReturnEventFrom(Location loc, JsonObject base_ep, JsonValue returned) {
    try {
      JsonObjectBuilder result = Json.createObjectBuilder();
      result.add("event", "return");
      result.add("line", loc.lineNumber());
      for (Map.Entry<String, JsonValue> me : base_ep.entrySet()) {
        if (me.getKey().equals("event") || me.getKey().equals("line")) {
        } else if (me.getKey().equals("stack_to_render")) {
          JsonArray old_stack_to_render = (JsonArray) me.getValue();
          JsonObject old_top_frame = (JsonObject) (old_stack_to_render.get(0));
          JsonObject old_top_frame_vars = (JsonObject) (old_top_frame.get("locals"));

                                  /*result.add("stack_to_render",
			  jsonModifiedArray(old_stack_to_render, 0,
				  jsonModifiedObject(jsonModifiedObject(old_top_frame, "locals",
					  jsonModifiedObject(old_top_frame_vars, "__return__", returned)))));
		*/} else
                                    result.add(me.getKey(), me.getValue());
      }
      return result.build();
    } catch (IndexOutOfBoundsException exc) {
      return base_ep;
    }
  }

  // list args first
  /* KNOWN ISSUE:
                 .arguments() gets the args which have names in LocalVariableTable,
                 but if there are none, we get an IllegalArgExc, and can use .getArgumentValues()
                 However, sometimes some args have names but not all. Such as within synthetic
                 lambda methods like "lambda$inc$0". For an unknown reason, trying .arguments()
                 causes a JDWP error in such frames. So sadly, those frames are incomplete. */
  private JsonObject getArguments(StackFrame sf) {
    JsonObjectBuilder result = Json.createObjectBuilder();
    boolean JDWPerror = false;
    try {
      sf.getArgumentValues();
    } catch (com.sun.jdi.InternalException e) {
      if (e.toString().contains("Unexpected JDWP Error: 35")) // expect JDWP error 35
        JDWPerror = true;
      else {
        throw e;
      }
    }

    List<LocalVariable> frame_vars = null, frame_args = null;
    boolean completed_args = false;
    try {
      // args make sense to show first
      frame_args = sf.location().method().arguments(); // throwing statement
      completed_args = !JDWPerror && frame_args.size() == sf.getArgumentValues().size();
      for (LocalVariable lv : frame_args) {
        // System.out.println(sf.location().method().getClass());
        if (lv.name().equals("args")) {
          Value v = sf.getValue(lv);
          if (v instanceof ArrayReference && ((ArrayReference) v).length() == 0)
            continue;
        }
        try {
          result.add(lv.name(), convertValue(sf.getValue(lv)));
        } catch (IllegalArgumentException exc) {
          System.out.println("That shouldn't happen!");
        }
      }
    } catch (AbsentInformationException e) {
    }
    // args did not have names, like a functional interface call...
    // although hopefully a future Java version will give them names!
    if (!completed_args && !JDWPerror) {
      try {
        List<Value> anon_args = sf.getArgumentValues();
        for (int i = 0; i < anon_args.size(); i++) {
          result.add("param#" + i, convertValue(anon_args.get(i)));
        }
      } catch (InvalidStackFrameException e) {
      }
    }

    if (JDWPerror) {
      result.add("&hellip;?",
          jsonArray("NUMBER-LITERAL",
              jsonString("&hellip;?"))); // hack since number-literal is just html
    }
    return result.build();
  }

  private JsonObjectBuilder convertFrame(StackFrame sf, JsonValue returnValue) {
    JsonObjectBuilder result = Json.createObjectBuilder();
    if (sf.thisObject() != null) {
      result.add("this", convertValue(sf.thisObject()));
    }

    result.add("args", getArguments(sf));
    List<LocalVariable> frame_vars = null;
    // now non-args
    try {
      /* We're using the fact that the hashCode tells us something
                     about the variable's position (which is subject to change)
                     to compensate for that the natural order of variables()
                     is often different from the declaration order (see LinkedList.java) */
      frame_vars = sf.location().method().variables(); // throwing statement
      TreeMap<Integer, String> orderByHash = null;
      int offset = 0;
      for (LocalVariable lv : frame_vars)
        if (!lv.isArgument())
          if (showAllFields
              || !lv.name().endsWith(
                  "$")) { // skip for-loop synthetics (exists in Java 7, but not 8)
            try {
              result.add(lv.name(), convertValue(sf.getValue(lv)));
              if (orderByHash == null) {
                offset = lv.hashCode();
                orderByHash = new TreeMap<>();
              }
              orderByHash.put(lv.hashCode() - offset, lv.name());
            } catch (IllegalArgumentException exc) {
              // variable not yet defined, don't list it
            }
          }

    } catch (AbsentInformationException ex) {
      // System.out.println("AIE: can't list variables in " + sf.location());
    }
    if (returnValue != null) {
      result.add("__return__", returnValue);
    }
    return Json
        .createObjectBuilder()

        .add("locals", result); // frame_stack.get(level));
  }

  // used to show a single non-user frame when there is
  // non-user code running between two user frames
  private JsonObjectBuilder convertFrameStub(StackFrame sf) {
    return Json.createObjectBuilder()
        .add("func_name",
            "\u22EE\n" + sf.location().declaringType().name() + "." + sf.location().method().name())
        .add("locals", Json.createObjectBuilder()) //.add("...", "..."))
        ;
  }

  void convertHeap(JsonObjectBuilder result) {
    heap_done = new java.util.TreeSet<>();
    while (!heap.isEmpty()) {
      Map.Entry<Long, ObjectReference> first = heap.firstEntry();
      ObjectReference obj = first.getValue();
      long id = first.getKey();
      heap.remove(id);
      if (heap_done.contains(id))
        continue;
      heap_done.add(id);
      result.add("" + id, convertObject(obj, true));
    }
  }

  List<String> wrapperTypes = new ArrayList<String>(
      Arrays.asList("Byte Short Integer Long Float Double Character Boolean".split(" ")));

  private JsonValue convertObject(ObjectReference obj, boolean fullVersion) {
    if (showStringsAsValues && obj.referenceType().name().startsWith("java.lang.")
        && wrapperTypes.contains(obj.referenceType().name().substring(10))) {
      return convertValue(obj.getValue(obj.referenceType().fieldByName("value")));
    }

    JsonArrayBuilder result = Json.createArrayBuilder();
    // TODO try this
    // abbreviated versions are for references to objects
    if (!fullVersion) { //

      result.add("REF").add(obj.uniqueID());
      heap.put(obj.uniqueID(), obj);
      return result.build();
    }

    // full versions are for describing the objects themselves,
    // in the heap

    else if (obj instanceof ArrayReference) {
      ArrayReference ao = (ArrayReference) obj;
      int L = ao.length();

      heap_done.add(obj.uniqueID());

      JsonArrayBuilder array = Json.createArrayBuilder();

      for (int i = 0; i < L; i++) {
        array.add(convertValue(ao.getValue(i)));
      }
      result.add(/*"Array",*/ array);
      return result.build();
    }

    else if (obj instanceof StringReference) {
      return Json.createArrayBuilder()
          .add("HEAP_PRIMITIVE")
          .add("String")
          .add(jsonString(((StringReference) obj).value()))
          .build();
    }

    // now deal with Objects.
    heap_done.add(obj.uniqueID());
    result.add("INSTANCE");
    if (obj.referenceType().name().startsWith("java.lang.")
        && wrapperTypes.contains(obj.referenceType().name().substring(10))) {
      result.add(obj.referenceType().name().substring(10));
      result.add(jsonArray("___NO_LABEL!___", // jsonArray("NO-LABEL"), // don't show a label or
                                              // label cell for wrapper instance field
          convertValue(obj.getValue(obj.referenceType().fieldByName("value")))));
    } else {
      String fullName = obj.referenceType().name();
      if (fullName.indexOf("$") > 0) {
        // inner, local, anonymous or lambda class
        if (fullName.contains("$$Lambda")) {
          fullName =
              "&lambda;" + fullName.substring(fullName.indexOf("$$Lambda") + 9); // skip $$lambda$
          try {
            String interf = ((ClassType) obj.referenceType()).interfaces().get(0).name();
            if (interf.startsWith("java.util.function."))
              interf = interf.substring(19);

            fullName += " [" + interf + "]";
          } catch (Exception e) {
          }
        }
        // more cases here?
        else {
          fullName = fullName.substring(1 + fullName.indexOf('$'));
          if (fullName.matches("[0-9]+"))
            fullName = "anonymous class " + fullName;
          else if (fullName.substring(0, 1).matches("[0-9]+"))
            fullName = "local class " + fullName.substring(1);
        }
      }
      result.add(fullName);
    }
    if (showGuts(obj.referenceType())) {
      // fields: -inherited -hidden +synthetic
      // visibleFields: +inherited -hidden +synthetic
      // allFields: +inherited +hidden +repeated_synthetic
      for (Map.Entry<Field, Value> me :
          obj.getValues(showAllFields ? obj.referenceType().allFields()
                                      : obj.referenceType().visibleFields())
              .entrySet()) {
        if (!me.getKey().isStatic() && (showAllFields || !me.getKey().isSynthetic()))
          result.add(Json.createArrayBuilder()
                         .add((showAllFields ? me.getKey().declaringType().name() + "." : "")
                             + me.getKey().name())
                         .add(convertValue(me.getValue())));
      }
    }
    return result.build();
  }

  private JsonArray jsonArray(Object... args) {
    JsonArrayBuilder result = Json.createArrayBuilder();
    for (Object o : args) {
      if (o instanceof JsonValue)
        result.add((JsonValue) o);
      else if (o instanceof String)
        result.add((String) o);
      else
        throw new RuntimeException("Add more cases to JDI2JSON.jsonArray(Object...)");
    }
    return result.build();
  }

  private JsonObject jsonTypeValue(String t, Object v) {
    JsonObjectBuilder result = Json.createObjectBuilder();
    if (v instanceof JsonValue)

      result.add("type", t).add("value", (JsonValue) v);
    else if (v instanceof String)
      result.add("type", t).add("value", (String) v);
    else if (v == null)
      result.add("type", t);
    else
      throw new RuntimeException("Add more cases to JDI2JSON.jsonArray(Object...)");
    return result.build();
  }

  // return a pair (type, value as string)
  private JsonValue convertValue(Value v) {
    if (v instanceof BooleanValue) {
      if (((BooleanValue) v).value() == true)
        return jsonTypeValue("boolean", JsonValue.TRUE);
      else
        return jsonTypeValue("boolean", JsonValue.FALSE);
    } else if (v instanceof ByteValue)
      return jsonTypeValue("byte", jsonInt(((ByteValue) v).value()));
    else if (v instanceof ShortValue)
      return jsonTypeValue("short", jsonInt(((ShortValue) v).value()));
    else if (v instanceof IntegerValue)
      return jsonTypeValue("int", jsonInt(((IntegerValue) v).value()));
    // some longs can't be represented as doubles, they won't survive the json conversion
    else if (v instanceof LongValue)
      return jsonTypeValue("long", jsonString("" + ((LongValue) v).value()));
    // floats who hold integer values will end up as integers after json conversion
    // also, this lets us pass "Infinity" and other IEEE non-numbers
    else if (v instanceof FloatValue)
      return jsonTypeValue("float", jsonString("" + ((FloatValue) v).value()));
    else if (v instanceof DoubleValue)
      return jsonTypeValue("double", jsonString("" + ((DoubleValue) v).value()));
    else if (v instanceof CharValue)
      return jsonTypeValue("char", jsonString(((CharValue) v).value() + ""));
    else if (v instanceof VoidValue)
      return jsonTypeValue("void", null);
    else if (!(v instanceof ObjectReference))
      return JsonValue.NULL; // not a hack
    else if (showStringsAsValues && v instanceof StringReference)
      return jsonTypeValue("string", jsonString(((StringReference) v).value()));
    else {
      ObjectReference obj = (ObjectReference) v;
      heap.put(obj.uniqueID(), obj);
      return convertObject(obj, false);
    }
  }

  static JsonObject compileErrorOutput(String usercode, String errmsg, long row, long col) {
    return output(usercode,
        Json.createArrayBuilder()
            .add(Json.createObjectBuilder()
                     .add("line", "" + row)
                     .add("event", "uncaught_exception")
                     .add("offset", "" + col)
                     .add("exception_msg", errmsg))
            .build());
  }

  static String fakify(String realcode) {
    String[] x = realcode.split("\n", -1);
    for (int i = 0; i < x.length; i++) {
      int pos = x[i].indexOf("//><");
      if (pos >= 0)
        x[i] = x[i].substring(pos + 4);
    }
    StringBuilder sb = new StringBuilder();
    for (String s : x) {
      sb.append("\n");
      sb.append(s);
    }
    return sb.substring(1);
  }

  static JsonObject output(String usercode, JsonArray trace) {
    JsonObjectBuilder result = Json.createObjectBuilder();
    result.add("code", fakify(usercode)).add("trace", trace);
    if (userlogged != null)
      result.add("userlog", userlogged.toString());
    return result.build();
  }

  String exceptionMessage(ExceptionEvent event) {
    ObjectReference exc = event.exception();
    ReferenceType excType = exc.referenceType();
    try {
      // this is the logical approach, but gives "Unexpected JDWP Error: 502" in invokeMethod
      // even if we suspend-and-resume the thread t
      /*ThreadReference t = event.thread();
      Method mm = excType.methodsByName("getMessage").get(0);
      t.suspend();
      Value v = exc.invokeMethod(t, mm, new ArrayList<Value>(), 0);
      t.resume();
      StringReference sr = (StringReference) v;
      String detail = sr.value();*/

      // so instead we just look for the longest detailMessage
      String detail = "";
      for (Field ff : excType.allFields())
        if (ff.name().equals("detailMessage")) {
          StringReference sr = (StringReference) exc.getValue(ff);
          String thisMsg = sr == null ? null : sr.value();
          if (thisMsg != null && thisMsg.length() > detail.length())
            detail = thisMsg;
        }

      if (detail.equals(""))
        return excType.name(); // NullPointerException has no detail msg

      return excType.name() + ": " + detail;
    } catch (Exception e) {
      System.out.println("Failed to convert exception");
      System.out.println(e);
      e.printStackTrace(System.out);
      for (Field ff : excType.visibleFields()) System.out.println(ff);
      return "fail dynamic message lookup";
    }
  }

  /* JSON utility methods */

  static JsonValue jsonInt(long l) {
    return Json.createArrayBuilder().add(l).build().getJsonNumber(0);
  }

  static JsonValue jsonReal(double d) {
    return Json.createArrayBuilder().add(d).build().getJsonNumber(0);
  }

  static JsonValue jsonString(String S) {
    return Json.createArrayBuilder().add(S).build().getJsonString(0);
  }

  static JsonObject jsonModifiedObject(JsonObject obj, String S, JsonValue v) {
    JsonObjectBuilder result = Json.createObjectBuilder();
    result.add(S, v);
    for (Map.Entry<String, JsonValue> me : obj.entrySet()) {
      if (!S.equals(me.getKey()))
        result.add(me.getKey(), me.getValue());
    }
    return result.build();
  }

  // add at specified position, or end if -1
  static JsonArray jsonModifiedArray(JsonArray arr, int tgt, JsonValue v) {
    JsonArrayBuilder result = Json.createArrayBuilder();
    int i = 0;
    for (JsonValue w : arr) {
      if (i == tgt)
        result.add(v);
      else
        result.add(w);
      i++;
    }
    if (tgt == -1)
      result.add(v);
    return result.build();
  }

  // issue: the frontend uses persistent frame ids but JDI doesn't provide them
  // approach 1, trying to compute them, seems intractable (esp. w/ callbacks)
  // approach 2, using an id based on stack depth, does not work w/ frontend
  // approach 3, just give each frame at each execution point a unique id,
  // is what we do. but we also want to skip animating e.p.'s where nothing changed,
  // and if only the frame ids changed, we should treat it as if nothing changed

  // I am not sure if this would work
  static public Set<String> difference(final Set<String> set1, final Set<String> set2) {
    return set1.stream().filter(n -> !set2.contains(n)).collect(Collectors.toSet());
  }

  static public Set<String> intersect(final Set<String> set1, final Set<String> set2) {
    return set1.stream().filter(n -> set2.contains(n)).collect(Collectors.toSet());
  }

  private JsonValue diff2(JsonValue ost, JsonValue nst) {
    if (ost instanceof JsonObject && nst instanceof JsonObject) {
      return diff2((JsonObject) ost, (JsonObject) nst);
    } else if (ost instanceof JsonArray && nst instanceof JsonArray) {
      return diff2((JsonArray) ost, (JsonArray) nst);
    } else if (ost.getValueType() == nst.getValueType()) {
      if (!ost.equals(nst))
        return Json.createObjectBuilder().add("oldval", ost).add("newval", nst).build();

      // NEED To handle more cases
    }

    System.out.println("Comparing " + ost + " " + nst);
    System.out.println("Comparing " + ost.getValueType() + " " + nst.getValueType());

    return JsonValue.NULL;
  }

  private JsonArray diff2(JsonArray ost, JsonArray nst) {
    JsonArrayBuilder result = Json.createArrayBuilder();
    int size = Math.min(ost.size(), nst.size());
    for (int i = size; i > 0; i--) {
      JsonValue a = ost.get(ost.size() - i);
      JsonValue b = nst.get(nst.size() - i);
      result.add(diff2(a, b));
    }
    return result.build();
  }

  private JsonObject diff2(JsonObject a, JsonObject b) {
    if (b == null)
      return a;

    Set<String> akeys = a.keySet();
    Set<String> bkeys = b.keySet();
    // get those keys in a and b
    Set<String> both = intersect(akeys, bkeys);
    Set<String> del = difference(akeys, bkeys);
    Set<String> add = difference(bkeys, akeys);
    JsonObjectBuilder result = Json.createObjectBuilder();
    JsonArrayBuilder jsonadd = Json.createArrayBuilder();
    JsonArrayBuilder jsonremove = Json.createArrayBuilder();
    JsonArrayBuilder jsonupdate = Json.createArrayBuilder();

    for (String s : add) {
      JsonValue v = b.get(s);
      // System.out.println("add" + s + "\t" + v);

      jsonadd.add(Json.createObjectBuilder().add(s, v));
    }

    for (String s : del) {
      //  System.out.println("del" + s);
      JsonValue v = a.get(s);
      jsonremove.add(Json.createObjectBuilder().add(s, v));
    }

    for (String s : both) {
      JsonValue t1 = a.get(s);
      JsonValue t2 = b.get(s);

      // System.out.println("t1" + t1 + "t2" + t2);

      if (t1.equals(t2))
        continue;
      JsonValue v = diff2(t1, t2);
      jsonupdate.add(Json.createObjectBuilder().add(s, v));
    }

    result.add("add", jsonadd);
    result.add("remove", jsonremove);
    result.add("update", jsonupdate);
    return result.build();
  }

  private JsonObject diff(JsonObject old_ep, JsonObject new_ep) {
    System.out.println("Current " + Util.prettyPrint(new_ep));
    System.out.println(" \n\n\n");

    JsonObjectBuilder result = Json.createObjectBuilder();
    if (new_ep.containsKey("stdout"))
      result.add("stdout", new_ep.getString("stdout"));
    if (new_ep.containsKey("stderr"))
      result.add("stderr", new_ep.getString("stderr"));

    if (new_ep.containsKey("event"))
      result.add("event", new_ep.get("event"));
    if (new_ep.containsKey("loc"))
      result.add("loc", new_ep.get("loc"));

    if (old_ep == null)
      return new_ep;

    JsonArray ost = old_ep.getJsonArray("stack");
    JsonArray nst = new_ep.getJsonArray("stack");

    // try to generalize that
    // now compare the frames
    /*int size = Math.min(ost.size(), nst.size());
    for (int i = size; i > 0; i--) {
      JsonObject a = ost.getJsonObject(ost.size() - i);
      JsonObject b = nst.getJsonObject(nst.size() - i);
      JsonObject aa = a.getJsonObject("locals");
      JsonObject bb = b.getJsonObject("locals");
      result.add("stack.locals", diff2(aa, bb));
    }*/
    result.add("stack.locals", diff2(ost, nst));

    // result.add("globals", diff2(old_ep.get("globals"), new_ep.get("globals")));
    result.add("heap", diff2(old_ep.get("heap"), new_ep.get("heap")));

    JsonObject d = result.build();

    System.out.println("Difference " + Util.prettyPrint(d));
    return result.build(); // TODO
  }
}