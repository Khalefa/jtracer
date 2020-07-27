package edu.suny.jdi;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ClassType;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.StackFrame;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.StepRequest;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;

public class JavaDebugger {
  private String debugClassName;

  VirtualMachine vm;

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
        "-cp /Users/user/Documents/GitHub/Debugger/JDebugger/examples/g ");

    /*for (Map.Entry<String, Connector.Argument> entry : arguments.entrySet())
      System.out.println(entry.getKey() + " " + entry.getValue());
    */
    vm = launchingConnector.launch(arguments);
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
    // System.out.println(breakPointLines);
    // System.out.println(breakPointLines.length);

    for (int lineNumber : breakPointLines) {
      // System.out.println(lineNumber);

      Location location = classType.locationsOfLine(lineNumber).get(0);
      BreakpointRequest bpReq = vm.eventRequestManager().createBreakpointRequest(location);
      bpReq.enable();
    }
  }

  /**
   * Displays the visible variables
   * @param event
   * @throws IncompatibleThreadStateException
   * @throws AbsentInformationException
   */
  public void displayVariables(LocatableEvent event)
      throws IncompatibleThreadStateException, AbsentInformationException {
    System.out.println("Thread:" + event.thread().name() + " " + event.location());
    StackFrame stackFrame = event.thread().frame(0);
    if (stackFrame.location().toString().contains(debugClassName)) {
      Map<LocalVariable, Value> visibleVariables =
          stackFrame.getValues(stackFrame.visibleVariables());
      System.out.println("Variables at " + stackFrame.location().toString() + " > ");
      for (Map.Entry<LocalVariable, Value> entry : visibleVariables.entrySet()) {
        System.out.println("\t" + entry.getKey().name() + " = " + entry.getValue());
      }
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
    if (event instanceof ClassPrepareEvent) {
      StepRequest stepRequest = vm.eventRequestManager().createStepRequest(
          ((ClassPrepareEvent) (event)).thread(), StepRequest.STEP_LINE, StepRequest.STEP_OVER);
      stepRequest.enable();
    }
    //}
  }

  public static void main(String[] args) throws Exception {
    // prints the local varialbe and stack info after each line
    //

    System.out.println("Debugging" + args[0]);
    JavaDebugger debuggerInstance = new JavaDebugger();
    debuggerInstance.setDebugClass(args[0]);
    int[] breakPoints = {4, 8};
    debuggerInstance.setBreakPointLines(breakPoints);
    VirtualMachine vm = null;

    char[] buf = new char[520];
    InputStreamReader reader = null;
    OutputStreamWriter writer = new OutputStreamWriter(System.out);

    try {
      vm = debuggerInstance.connectAndLaunchVM();
      reader = new InputStreamReader(vm.process().getInputStream());
      debuggerInstance.enableClassPrepareRequest();

      EventSet eventSet = null;
      while ((eventSet = vm.eventQueue().remove()) != null) {
        for (Event event : eventSet) {
          System.out.println(event);
          if (event instanceof ClassPrepareEvent) {
            debuggerInstance.setBreakPoints((ClassPrepareEvent) event);
            debuggerInstance.enableStepRequest(event);
          }

          if (event instanceof BreakpointEvent) {
            event.request().disable();
            debuggerInstance.displayVariables((BreakpointEvent) event);
            debuggerInstance.enableStepRequest((BreakpointEvent) event);
          }

          if (event instanceof StepEvent) {
            debuggerInstance.displayVariables((StepEvent) event);
          }

          vm.resume();
        }
      }
      // while (reader.read(buf) != -1) {
      // writer.write(buf);
      // writer.flush();
      //}
    } catch (VMDisconnectedException e) {
      System.out.println("Virtual Machine is disconnected.");
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      while (reader.read(buf) != -1) {
        writer.write(buf);
        writer.flush();
      }
    }
  }
}