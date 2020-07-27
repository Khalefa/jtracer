// https://blog.sqreen.com/building-a-dynamic-instrumentation-agent-for-java/
package edu.suny.jdi;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.*;

public class JDebugger {
  private String debugClassName;

  public VirtualMachine vm;
  public EventRequestManager mgr;

  private int[] breakPointLines;

  public String getDebugClass() {
    return debugClassName;
  }

  public void setDebugClass(String debugClassName) {
    this.debugClassName = debugClassName;
  }

  public int[] getBreakPointLines() {
    return breakPointLines;
  }

  public void setBreakPointLines(int[] breakPointLines) {
    this.breakPointLines = breakPointLines;
  }

  /**
   * Sets the debug class as the main argument in the connector and launches the VM
   * @return VirtualMachine
   * @throws IOException
   * @throws IllegalConnectorArgumentsException
   * @throws VMStartException
   */
  public VirtualMachine connectAndLaunchVM()
      throws IOException, IllegalConnectorArgumentsException, VMStartException {
    LaunchingConnector launchingConnector = Bootstrap.virtualMachineManager().defaultConnector();
    Map<String, Connector.Argument> arguments = launchingConnector.defaultArguments();

    arguments.get("main").setValue(debugClassName);
    arguments.get("options").setValue(
        "-cp /Users/user/Documents/GitHub/Debugger/JDebugger/examples/  ");

    /*for (Map.Entry<String, Connector.Argument> entry : arguments.entrySet())
      System.out.println(entry.getKey() + " " + entry.getValue());
    */
    vm = launchingConnector.launch(arguments);
    mgr = vm.eventRequestManager();
    return vm;
  }

  /**
   * Creates a request to prepare the debug class, add filter as the debug class and enables it
   * @param vm
   */
  public void enableClassPrepareRequest() {
    ClassPrepareRequest classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest();
    classPrepareRequest.addClassFilter(debugClassName);
    classPrepareRequest.enable();
  }

  /**
   * Sets the break points at the line numbers mentioned in breakPointLines array
   * @param vm
   * @param event
   * @throws AbsentInformationException
   */
  public void setBreakPoints(ClassPrepareEvent event) throws AbsentInformationException {
    ClassType classType = (ClassType) event.referenceType();
    for (int lineNumber : breakPointLines) {
      Location location = classType.locationsOfLine(lineNumber).get(0);
      BreakpointRequest bpReq = vm.eventRequestManager().createBreakpointRequest(location);
      bpReq.enable();
    }
  }
  void setEventRequests() {
    mgr = vm.eventRequestManager();
    ExceptionRequest excReq = mgr.createExceptionRequest(null, true, true);
    excReq.setSuspendPolicy(EventRequest.SUSPEND_ALL);

    excReq.enable();

    MethodEntryRequest menr = mgr.createMethodEntryRequest();

    menr.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
    menr.addClassFilter(debugClassName);
    menr.enable();

    MethodExitRequest mexr = mgr.createMethodExitRequest();

    mexr.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
    mexr.addClassFilter(debugClassName);
    mexr.enable();

    ThreadDeathRequest tdr = mgr.createThreadDeathRequest();
    tdr.setSuspendPolicy(EventRequest.SUSPEND_ALL);

    tdr.enable();

    ClassPrepareRequest cpr = mgr.createClassPrepareRequest();

    cpr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
    cpr.addClassFilter(debugClassName);
    cpr.enable();
  }

  StepRequest request = null;

  private ThreadReference handleEvent(Event event) {
    System.out.println("===>>>" + event);
    ThreadReference theThread = null;
    if (event instanceof ClassPrepareEvent) {
      classPrepareEvent((ClassPrepareEvent) event);
    } else if (event instanceof VMDeathEvent) {
      // vmDeathEvent((VMDeathEvent) event);
    } else if (event instanceof VMDisconnectEvent) {
      // vmDisconnectEvent((VMDisconnectEvent) event);
    }

    if (request != null && request.isEnabled()) {
      //  System.out.println("12");
      request.disable();
    }
    if (request != null) {
      // System.out.println("11");
      mgr.deleteEventRequest(request);
      request = null;
    }

    if (event instanceof LocatableEvent) {
      // System.out.println("13");
      request = mgr.createStepRequest(
          ((LocatableEvent) event).thread(), StepRequest.STEP_MIN, StepRequest.STEP_INTO);
      request.addClassFilter(debugClassName);
      request.addCountFilter(1); // next step only
      request.enable();
    }

    if (event instanceof MethodEntryEvent) {
      System.out.println(((MethodEntryEvent) event).method());
    }

    if (event instanceof MethodExitEvent) {
      System.out.println(((MethodExitEvent) event).returnValue());
    }

    if (event instanceof LocatableEvent) {
      // event.request().disable();
      displayVariables((LocatableEvent) event);
    }

    System.out.println("\n\n");

    return theThread;
  }

  /**
   * Displays the visible variables
   * @param event
   * @throws IncompatibleThreadStateException
   * @throws AbsentInformationException
   */
  public void displayVariables(LocatableEvent event)
  // throws IncompatibleThreadStateException, AbsentInformationException
  {
    try {
      System.out.println("Thread:" + event.thread().name() + " " + event.location().method() + " "
          + event.location());
      List<StackFrame> stackFrames = event.thread().frames();
      for (StackFrame stackFrame : stackFrames)
      // StackFrame stackFrame = event.thread().frame(0); // stackFrames[0];
      {
        if (stackFrame.location().toString().contains(debugClassName)) {
          Map<LocalVariable, Value> visibleVariables =
              stackFrame.getValues(stackFrame.visibleVariables());
          System.out.println("Variables at " + stackFrame.location().toString() + " > ");
          for (Map.Entry<LocalVariable, Value> entry : visibleVariables.entrySet()) {
            System.out.println("\t" + entry.getKey().name() + " = " + entry.getValue()
                + "===" + entry.getKey().type());
          }
        }
      }
    } catch (Exception e) {
      System.err.println(e.toString());
    }
  }

  /**
   * Enables step request for a break point
   * @param vm
   * @param event
   */
  public void enableStepRequest(Event event) {
    // enable step request for last break point
    // if (event.location().toString().contains(
    //      debugClassName + ":" + breakPointLines[breakPointLines.length - 1])) {

    System.out.println("14");
    StepRequest stepRequest = vm.eventRequestManager().createStepRequest(
        ((ClassPrepareEvent) (event)).thread(), StepRequest.STEP_LINE, StepRequest.STEP_OVER);
    stepRequest.enable();
  }
  //}

  private void classPrepareEvent(ClassPrepareEvent event) {
    ReferenceType rt = event.referenceType();

    try {
      for (Location loc : rt.allLineLocations()) {
        System.out.println("**\t\t" + loc);
      }
    } catch (AbsentInformationException e) {
      if (!rt.name().contains("$Lambda$"))
        System.out.println("AIE!" + rt.name());
    }
  }

  public static void main(String[] args) throws Exception {
    System.out.println("Debugging" + args[0]);
    JDebugger debuggerInstance = new JDebugger();
    debuggerInstance.setDebugClass(args[0]);

    VirtualMachine vm = null;

    char[] buf = new char[520];
    InputStreamReader reader = null;
    OutputStreamWriter writer = new OutputStreamWriter(System.out);

    try {
      vm = debuggerInstance.connectAndLaunchVM();
      reader = new InputStreamReader(vm.process().getInputStream());

      debuggerInstance.setEventRequests();

      EventSet eventSet = null;

      while ((eventSet = vm.eventQueue().remove()) != null) {
        for (Event event : eventSet) {
          debuggerInstance.handleEvent(event);
        }

        eventSet.resume();

        while (reader.ready()) {
          int l = reader.read(buf);
          if (l > -1)
            System.out.println("-->" + new String(buf, 0, l));
          else
            break;
        }
      }
    } catch (VMDisconnectedException e) {
      System.out.println("Virtual Machine is disconnected.");
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      while (reader.ready()) {
        int l = reader.read(buf);
        if (l > -1)
          System.out.println("-->" + new String(buf, 0, l));
        else
          break;
      }
    }
  }
}