import java.io.*;
import java.util.*;

// this class is used at run time tor gather information about methods
// and call sites, and to compute the coverage statistics.

public class RuntimeTracker {

    // directory where the output files should be written
    private static String out_dir;

    // --------------------------------------------------------
    // before anything else in RuntimeTracker is called, method
    // 'start' should be invoked. The parameter is a directory in
    // which RuntimeTracker will find the necessary RTA-generated
    // files ("rmethods.cut" and "edges.cut"). In the same directory,
    // RuntimeTracker will write info about coverage statistics.
    public static void start(String io_dir) { 

	System.out.println("\n--- Instrumentation started in " + 
			   io_dir + " ---\n");

	out_dir = io_dir;

    }
    
    // ---------------------------------------------------------
    // this method should be invoked at the end of the execution;
    // basically, it writes the output files to disk
    public static void end() {

	System.out.println("\n--- Instrumentation ended ---\n");
	
	// output file for not-covered methods
	BufferedWriter nc_methods;
	
	// output file for not-covered edges
	BufferedWriter nc_edges; 

	try {
	    nc_methods = 
		new BufferedWriter(new FileWriter(out_dir + "/nmethods"));
	    nc_edges =
		new BufferedWriter(new FileWriter(out_dir + "/nedges"));

	    // ???

	    nc_methods.close();
	    nc_edges.close();

	} catch (Exception e) {
	    System.out.println("OOPS! " + e);
	}
	
    }
    
    // --------------------------------------------------------------
    // if this method is called, it means that the corresponding call
    // site is executed. this is only for call sites that are
    // staticinvoke or specialinvoke. for these, it is enough to
    // remember the call site. 
    public static void beforeCall(String call_site_id) { 
	System.out.println("Call: " + call_site_id);
    }

    // --------------------------------------------------------------
    // if this method is called, it means that the corresponding call
    // site is executed. the second parameter is a pointer to the
    // receiver object. this applies only to virtualinvoke and
    // interfaceinvoke calls; for all others, the alternative
    // beforeCall method is called
    public static void beforeCall(String call_site_id, Object rcv) { 
	String rcv_class = rcv.getClass().getName();
	System.out.println("Call: " + call_site_id + "," + rcv_class);
    }

    // ok, this means that the excution just entered some method. need
    // to remember the method and the incoming call edge.
    public static void methodEntry(int method_id) { 
	System.out.println("Method: " + method_id);
    }

    // -------------------------------------------
    public static String percent(long x, long y) {
	double z = (100.0*x) / ((double)y);
	String res = new String (String.valueOf(Math.round(z)));
	return res + "%";
    }
}
