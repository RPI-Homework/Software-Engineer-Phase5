import soot.*;
import soot.jimple.*;
import java.io.*;
import java.util.*;

public class RtaWriter {

    // directory containing the output files
    private String output_dir;

    // A Hierarchy object providing necessary info for the printing
    private Hierarchy hierarchy;
    
    // A set of all CUT classes
    private HashSet cutClasses = new HashSet();

    // --------------------------------
    public RtaWriter(String dir_name, Hierarchy h) { 

	output_dir = dir_name; 
	hierarchy = h;

	// open files to store info about all calls in non-library methods
	try {
	    // figure out all CUT classes
	    String fclass = output_dir + "/CUT";
	    BufferedReader in = 
		new BufferedReader(new FileReader(fclass));
	    String line;
	    while( (line = in.readLine()) != null ) {
		SootClass c = (SootClass) Scene.v().getSootClass(line);
		cutClasses.add(c);
	    }
	    in.close();
	} catch (Exception e) {
	    System.out.println("Constructor of RtaWriter: " + e);
	    System.exit(1);
	}
    }
    
    // -----------------------------------------------
    public void writeCallGraphInfo(Hashtable reachable) {

	writeMethodInfo(reachable);

	writeEdgeInfo(reachable);
    }

    // -----------------------------------------------
    private void writeMethodInfo(Hashtable reachable) {

	// open files to store the info
	try {
	    BufferedWriter file =
		new BufferedWriter(new FileWriter(output_dir + "/rmethods_all"));
	    BufferedWriter file_nl =
		new BufferedWriter(new FileWriter(output_dir + "/rmethods"));

	    // BufferedWriter file_nl_cut =
	    //	new BufferedWriter(new FileWriter(output_dir + "/rmethods.cut"));

	    // BufferedWriter file_cut =
	    //	new BufferedWriter(new FileWriter(output_dir + "/nmethods.cut"));
	
	    file.write("Total num reachable methods: " + reachable.size() + "\n");
	    for (Iterator it = reachable.keySet().iterator(); it.hasNext();) {
		SootMethod m = (SootMethod) it.next();
		Integer id = (Integer) reachable.get(m);
		file.write(id + ": " + m + "\n");
		if (hierarchy.notLibrary(m)) 
		    file_nl.write(id + ": " + m + "\n");
	    }
	    file.close();
	    file_nl.close();

	    // ok, now let's write info about each CUT method: is is
	    // RTA-reachable?
	    /* Irrelevant 
	    int x = 0, y = 0;
	    for (Iterator cit = cutClasses.iterator(); cit.hasNext();) {
		SootClass c = (SootClass) cit.next();
		for (Iterator it = c.getMethods().iterator(); it.hasNext(); ) {
		    SootMethod m = (SootMethod) it.next();
		    x++;
		    if ( ! reachable.containsKey(m)) {
			file_cut.write(m + "\n");
			y++;
		    } else {
			file_nl_cut.write(reachable.get(m) + ": " + m + "\n");
		    }
		}
	    }
	    file_cut.write("CUT methods: total: " + x + 
			   ", unreachable: " + y + 
			   " [" + percent(y,x) + "]\n");
	    file_cut.close();
	    file_nl_cut.close();
	    */

	} catch (Exception e) {
	    System.out.println("writeMethodInfo: " + e);
	    System.exit(1);
	}

    }

    // -----------------------------------------------
    private void writeEdgeInfo(Hashtable reachable) {

	// open files to store the info
	try {
	    BufferedWriter calls =
		new BufferedWriter(new FileWriter(output_dir + "/calls"));
	    BufferedWriter edges =
		new BufferedWriter(new FileWriter(output_dir + "/edges"));

	    BufferedWriter edges_cut =
		new BufferedWriter(new FileWriter(output_dir + "/edges.annotated"));

	    // go through all methods
	    for (Enumeration mit = reachable.keys(); mit.hasMoreElements();) {
		SootMethod m = (SootMethod) mit.nextElement();
		// only methods for which the id is not zero (i.e.,
		// non-library methods) are interesting
		if (reachable.get(m).equals(new Integer(0))) continue;
		
		calls.write("\n===== Method " + reachable.get(m) + ": " + 
			    m + "\n");

		//		System.err.println("\n===== Method " + reachable.get(m) + ": " + m);

		Hashtable all_calls = (Hashtable) call_info.get(m);
		// some methods don't have calls
		if (all_calls == null) continue;

		for (Enumeration cit = all_calls.keys(); cit.hasMoreElements();) {
		    InvokeExpr call = (InvokeExpr) cit.nextElement();
		    String call_site_id = (String) all_calls.get(call);
		    
		    if (call instanceof StaticInvokeExpr ||
			call instanceof SpecialInvokeExpr) {
			
			// write the call
			calls.write(call_site_id + ": [S] " + call + "\n");
			//			System.err.println(call_site_id + ": [S] " + call);

			// only write edges that go to non-lib methods
			int target_id = ((Integer) reachable.get(call.getMethod())).
			    intValue();
			if (target_id != 0)
			    edges.write(call_site_id + "," + target_id + "\n");

			// write CUT edges: both caller and callee
			// should be in the CUT
			if (inCUT(m) && inCUT(call.getMethod()))
			    edges_cut.write(call_site_id + "," + target_id + "\n");

		    } else { // virtualinvoke or interfaceinvoke

			calls.write(call_site_id + ": [C] " + call);
			//			System.err.println(call_site_id + ": [C] " + call);

			// here also need to figure out the number of
			// receiver classes, the number of target
			// methods, and the actual target methods
			HashSet rcv = (HashSet) receiver_info.get(call);
			//			System.err.println("num rcv = " + rcv.size());
			HashSet targets = new HashSet();
			for (Iterator rcvit = rcv.iterator(); rcvit.hasNext();) {
			    SootClass c = (SootClass) rcvit.next();
			    //			    System.err.println("rcv = " + c);
			    SootMethod t = hierarchy.virtualDispatch(call.getMethod(),c);
			    //			    System.err.println("targ = " + t);
			    targets.add(t);
			    // in case both the caller and the callee
			    // are in the CUT, need to write the edge
			    if (inCUT(m) && inCUT(t)) 
				edges_cut.write(call_site_id + "," + 
						reachable.get(t) + "," +
						c + "\n");
			}

			calls.write("," + rcv.size() + "," + targets.size() + "\n");
			//			System.err.println("," + rcv.size() + "," + targets.size());

			for (Iterator targit = targets.iterator(); targit.hasNext();) {
			    SootMethod targ = (SootMethod) targit.next();
			    calls.write("     " + targ + "\n");
			    //		    System.err.println("targ = " + targ);
			    Integer id = (Integer) reachable.get(targ);
			    if (id == null) 
				throw new RuntimeException
				    ("Method should be reachable but isn't:"+ targ); 
			    // only write edges that go to non-lib methods 
			    int target_id = id.intValue(); 
			    if (target_id != 0) 
				edges.write(call_site_id +"," + target_id + "\n");
			    
			}
		    }
		}
	    }

	    calls.close();
	    edges.close();
	    edges_cut.close();

	} catch (IOException e) {
	    System.out.println("writeEdgeInfo: " + e);
	    System.exit(1);
	}

    }

    // -------------------------------------------
    public String percent(long x, long y) {
	double z = (100.0*x) / ((double)y);
	String res = new String (String.valueOf(Math.round(z)));
	return res + "%";
    }

    public void writeHierarchyInfo() {

	// open a file to store the info
	try {
	    BufferedWriter file =
		new BufferedWriter(new FileWriter(output_dir + "/hier_all"));
	    BufferedWriter file_nl =
		new BufferedWriter(new FileWriter(output_dir + "/hier"));
	
	    Set C = hierarchy.allClasses();
	    file.write("Total num classes: " + C.size() + "\n");
	    for (Iterator it = C.iterator(); it.hasNext();) {
		SootClass c = (SootClass) it.next();
		file.write(c + "," + 
			   hierarchy.possibleReceiverClasses(c).size() + "\n");
		if (hierarchy.notLibrary(c)) 
		    file_nl.write(c + "," + 
				  hierarchy.possibleReceiverClasses(c).size() + 
				  "\n");
	    }
	    file.close();
	    file_nl.close();
	} catch (Exception e) {
	    System.out.println("writeHierarchyInfo " + e);
	    System.exit(1);
	}
    }

    // --------------------------------
    private boolean inCUT(SootClass c ) { return cutClasses.contains(c); }
    private boolean inCUT(SootMethod m) { return inCUT(m.getDeclaringClass()); }


    // ---------------------------------------------------------- 
    // storing and printing info about call edges: files "calls",
    // "edges", "edges.cut"

    // for each non-librarymethod, this table contains a table of
    // pairs (InvokeExpr,call_site_id) for all call sites inside that
    // method
    private Hashtable call_info = new Hashtable();

    // for each "complex" call site inside a non-library method, this
    // table contain a pair (InvokeExpr,Set). The set contains all
    // receiver classes that have been seen at that call site.
    private Hashtable receiver_info = new Hashtable();

    // --------------------------------------------------
    private void registerCall(SootMethod encl_method,
			      InvokeExpr call_expr,
			      String call_site_id) {
	
	// we only care about methods that are non-library
	if ( ! hierarchy.notLibrary(encl_method)) return;

	// do we have a hashtable associated with this method?
	if ( ! call_info.containsKey(encl_method))
	    call_info.put(encl_method, new Hashtable());
	
	Hashtable t = (Hashtable) call_info.get(encl_method);
	t.put(call_expr,call_site_id);
    }

    // --------------------------------------------------
    public void registerSimpleCall(SootMethod encl_method,
				   InvokeExpr call_expr,
				   String call_site_id) {
	registerCall(encl_method,call_expr,call_site_id);
    }
    
    // --------------------------------------------------
    public void registerComplexCall(SootMethod encl_method,
				    InvokeExpr call_expr,
				    String call_site_id) {
	registerCall(encl_method,call_expr,call_site_id);
	// in addition, we need to create the record in receiver_info
	if ( ! hierarchy.notLibrary(encl_method)) return;
	if (receiver_info.containsKey(call_expr)) {
	    System.err.println("Warning: call site " + call_site_id + 
			       ": " + call_expr + 
			       " registered multiple times");
	}
	receiver_info.put(call_expr,new HashSet());
    }

    // --------------------------------------------------
    public void registerReceiverClass(InvokeExpr call,
				      SootClass rcv) {
	if ( ! receiver_info.containsKey(call)) return;
	((HashSet)receiver_info.get(call)).add(rcv);
    }
    
}
